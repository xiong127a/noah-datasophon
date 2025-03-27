/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.enums.Status;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.DispatcherWorkerActor;
import com.datasophon.api.master.WorkerStartActor;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.api.service.InstallService;
import com.datasophon.api.service.checker.queue.HostCheckQueueManager;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.DispatcherHostAgentCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import com.datasophon.common.model.WorkerServiceMessage;
import com.datasophon.common.utils.HostUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.InstallStepEntity;
import com.datasophon.dao.mapper.InstallStepMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service("installService")
public class InstallServiceImpl implements InstallService {

    private static final Logger logger = LoggerFactory.getLogger(InstallServiceImpl.class);

    @Autowired
    InstallStepMapper stepMapper;

    @Autowired
    ClusterInfoService clusterInfoService;

    @Autowired
    ClusterHostService hostService;

    @Autowired
    private HostCheckService hostCheckService;

    @Autowired
    private HostCheckQueueManager hostCheckQueueManager;

    @Autowired
    private ApplicationContext applicationContext;

    private static final String SSHUSER = "SSHUSER";

    @Override
    public Result getInstallStep(Integer type) {
        List<InstallStepEntity> list = stepMapper
                .selectList(new QueryWrapper<InstallStepEntity>().eq(Constants.INSTALL_TYPE, type));
        return Result.success(list);
    }

    /**
     * 解析主机列表并进行环境检测
     * <p>
     * 处理流程：
     * 1. 获取全局变量并设置SSH用户
     * 2. 检查缓存中是否存在主机列表
     * 3. 解析主机列表（支持IP、主机名、IP域格式）
     * 4. 对未受管主机进行环境检测
     * 5. 分页返回结果
     *
     * @param clusterId   集群ID
     * @param ips         主机列表字符串，支持格式：单个IP/主机名，逗号分隔的列表，IP域如[1-5]
     * @param sshUser     SSH用户名
     * @param sshPort     SSH端口
     * @param sshPassword SSH密码
     * @param page        当前页码
     * @param pageSize    每页大小
     * @return 分页后的主机列表结果
     */
    @Override
    public Result analysisHostList(Integer clusterId, String ips, String sshUser, Integer sshPort, String sshPassword,
            Integer page, Integer pageSize) {
        try {
            if (StringUtils.isBlank(ips)) {
                return Result.error("主机列表不能为空");
            }

            Map<String, HostInfo> hostMap = saveHostInfo(clusterId, ips, sshUser, sshPort, sshPassword);

            // 如果没有获取到主机信息,返回错误提示
            if (Objects.isNull(hostMap)) {
                return Result.error("未获取到有效的主机信息");
            }

            List<HostInfo> hostList = new ArrayList<>(hostMap.values());
            hostList.sort(Comparator.comparing(HostInfo::getIp));

            // 计算每个主机的状态
            hostList.forEach(HostInfo::calculateStatus);

            // 分页处理
            int offset = (page - 1) * pageSize;
            int end = Math.min(offset + pageSize, hostList.size());
            List<HostInfo> pagedHosts = hostList.subList(offset, end);

            // 添加队列状态信息
            Map<String, Object> queueStatus = new HashMap<>();
            if (hostCheckQueueManager != null) {
                Map<String, Object> statusMap = hostCheckQueueManager.getQueueStatus();
                // 获取必要的队列状态信息
                queueStatus.put("queueSize", statusMap.getOrDefault("queueSize", 0));
                queueStatus.put("runningTasks", statusMap.getOrDefault("runningTasks", 0));
                queueStatus.put("processorThreadAlive", statusMap.getOrDefault("processorThreadAlive", true));
            }

            // 直接返回主机列表和队列状态
            Result success = Result.success();
            success.put("data", pagedHosts);
            success.put(Constants.TOTAL, (long) hostList.size());
            success.put("queueStatus", queueStatus);
            return success;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getSimpleMessage(e));
            Result success = Result.success();
            success.put("data", Collections.emptyList());
            success.put(Constants.TOTAL, 0L);
            return success;
        }
    }

    /**
     * 保存主机信息到缓存
     *
     * @param clusterId   集群ID
     * @param hosts       主机列表字符串
     * @param sshUser     SSH用户名
     * @param sshPort     SSH端口
     * @param sshPassword SSH密码
     */
    private Map<String, HostInfo> saveHostInfo(Integer clusterId, String hosts, String sshUser, Integer sshPort,
            String sshPassword) {
        String hostsMd5 = SecureUtil.md5(hosts);
        // 1. 检查缓存中是否存在有效的主机列表
        if (isCacheValid(clusterId, hostsMd5)) {
            logger.debug("从缓存获取主机列表");
            return (HashMap<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        }

        Map<String, HostInfo> hostMap = processHostList(clusterId, hosts, hostsMd5, sshPort, sshUser, sshPassword);

        // 将结果存入缓存
        CacheUtils.put(clusterId + Constants.HOST_MAP, hostMap);
        CacheUtils.put(clusterId + Constants.HOST_MD5, hostsMd5);
        logger.info("主机列表已存入缓存");

        return hostMap;
    }

    /**
     * 处理主机列表
     *
     * @param clusterId   集群ID
     * @param hosts       主机列表字符串
     * @param hostsMd5    主机列表MD5值
     * @param sshPort     SSH端口
     * @param sshUser     SSH用户名
     * @param sshPassword SSH密码
     * @return 主机信息映射，IP为键
     */
    private Map<String, HostInfo> processHostList(Integer clusterId, String hosts, String hostsMd5, Integer sshPort,
            String sshUser, String sshPassword) {
        HashMap<String, HostInfo> hostInfoMap = new HashMap<>();

        logger.info("解析主机列表");
        String[] hostArray = hosts.split(Constants.COMMA);

        // 获取集群信息
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        if (Objects.isNull(clusterInfo)) {
            logger.error("集群信息不存在, clusterId: {}", clusterId);
            return hostInfoMap;
        }
        String clusterCode = clusterInfo.getClusterCode();

        // 2. 遍历处理每个主机，将结果存入临时Map
        Map<String, HostInfo> tempMap = new HashMap<>();
        for (String host : hostArray) {
            processHost(host, sshPort, sshUser, sshPassword, clusterCode, tempMap);
        }

        List<CheckItem> checkItems = hostCheckService.getHostCheckItems();
        // 3. 将所有主机信息添加到返回结果中，以IP为键
        for (HostInfo hostInfo : tempMap.values()) {
            // 获取主机检查项列表
            hostInfo.setCheckItems(checkItems);
            // 使用IP作为键
            hostInfoMap.put(hostInfo.getIp(), hostInfo);
        }

        return hostInfoMap;
    }

    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid(Integer clusterId, String hostsMd5) {
        return CacheUtils.constainsKey(clusterId + Constants.HOST_MAP)
                && CacheUtils.constainsKey(clusterId + Constants.HOST_MD5)
                && hostsMd5.equals(CacheUtils.getString(clusterId + Constants.HOST_MD5));
    }

    /**
     * 处理单个主机信息
     */
    private void processHost(String host, Integer sshPort, String sshUser, String sshPassword, String clusterCode,
            Map<String, HostInfo> hostInfoMap) {
        // 添加参数日志
        logger.info("处理主机连接参数: host={}, sshPort={}, sshUser={}, sshPassword={}, clusterCode={}", host, sshPort, sshUser,
                StringUtils.isNotBlank(sshPassword) ? "******" : "null", clusterCode);

        // 1. 处理IP域格式 [x-y]
        if (host.contains("[") && host.contains("-")) {
            processIpRange(host, sshPort, sshUser, sshPassword, clusterCode, hostInfoMap);
            return;
        }

        // 2. 处理单个主机
        HostInfo hostInfo = createHostInfo(host, sshPort, sshUser, sshPassword, clusterCode);
        // 使用IP作为临时Map的键
        hostInfoMap.put(hostInfo.getIp(), hostInfo);

        // 设置初始状态
        hostInfo.setOsInfoStatus("loading");

        // 异步获取主机信息
        getHostOsInfoAsync(hostInfo);
    }

    /**
     * 处理IP范围
     */
    private void processIpRange(String host, Integer sshPort, String sshUser, String sshPassword, String clusterCode,
            Map<String, HostInfo> hostInfoMap) {
        int start = host.indexOf("[");
        String prefix = host.substring(0, start);
        String range = host.substring(start + 1, host.length() - 1);
        String[] split = range.split("-");

        // 1. 处理字母范围，如[a-e]
        if (host.matches(Constants.HAS_EN)) {
            processLetterRange(prefix, split[0], split[1], sshPort, sshUser, sshPassword, clusterCode, hostInfoMap);
            return;
        }

        // 2. 处理数字范围，如[1-5]
        processNumberRange(prefix, split, sshPort, sshUser, sshPassword, clusterCode, hostInfoMap);
    }

    /**
     * 处理字母范围的主机名
     */
    private void processLetterRange(String prefix, String start, String end, Integer sshPort, String sshUser,
            String sshPassword, String clusterCode, Map<String, HostInfo> hostInfoMap) {
        List<String> hostList = PlaceholderUtils.getNewEquipmentNoList(start, end);
        for (String suffix : hostList) {
            HostInfo hostInfo = createHostInfo(prefix + suffix, sshPort, sshUser, sshPassword, clusterCode);
            hostInfoMap.put(hostInfo.getIp(), hostInfo);
        }
    }

    /**
     * 处理数字范围的主机名
     */
    private void processNumberRange(String prefix, String[] range, Integer sshPort, String sshUser, String sshPassword,
            String clusterCode, Map<String, HostInfo> hostInfoMap) {
        int start = Integer.parseInt(range[0]);
        int end = Integer.parseInt(range[1]);
        for (int i = start; i <= end; i++) {
            HostInfo hostInfo = createHostInfo(prefix + i, sshPort, sshUser, sshPassword, clusterCode);
            hostInfoMap.put(hostInfo.getIp(), hostInfo);
        }
    }

    /**
     * 创建主机信息对象
     *
     * @param host        主机地址
     * @param sshPort     SSH端口
     * @param sshUser     SSH用户名
     * @param sshPassword SSH密码
     * @param clusterCode 集群编码
     * @return 主机信息对象
     */
    private HostInfo createHostInfo(String host, Integer sshPort, String sshUser, String sshPassword,
            String clusterCode) {
        HostInfo hostInfo = new HostInfo();

        // 1. 设置基本信息
        hostInfo.setIp(HostUtils.getIp(host));
        hostInfo.setSshPort(sshPort);
        hostInfo.setSshUser(sshUser);
        hostInfo.setSshPassword(sshPassword);
        hostInfo.setClusterCode(clusterCode);
        hostInfo.setCreateTime(new Date());

        // 2. 检查主机是否已受管
        ClusterHostDO hostEntity = hostService.getClusterHostByHostname(hostInfo.getHostname());
        if (Objects.nonNull(hostEntity)) {
            setManagedHostInfo(hostInfo);
        } else {
            setUnmanagedHostInfo(hostInfo);
        }

        // 3. 尝试获取操作系统信息
        try {
            if (sshUser != null && sshPort != null) {
                OsInfo osInfo = getHostOsInfo(hostInfo);
                hostInfo.setOsInfo(osInfo);
            }
        } catch (Exception e) {
            logger.warn("获取主机 {} 的操作系统信息失败: {}", host, e.getMessage());
        }

        return hostInfo;
    }

    /**
     * 获取或创建SSH会话
     *
     * @return SSH会话
     */
    private ClientSession getOrCreateSession(HostInfo hostInfo) {
        // 使用host作为连接池的键
        String ip = hostInfo.getIp();
        // 创建新会话
        try {
            logger.info("创建主机 {} 的新SSH连接", ip);
            ClientSession newSession = MinaUtils.openConnectionWithPassword(hostInfo);
            if (newSession != null) {
                // 将新会话添加到Map中
                logger.info("成功创建主机 {} 的SSH连接", ip);
            } else {
                logger.warn("无法创建主机 {} 的SSH连接", ip);
            }
            return newSession;
        } catch (Exception e) {
            logger.error("创建主机 {} 的SSH连接时发生异常: {}", ip, e.getMessage());
            return null;
        }
    }

    /**
     * 异步获取主机的操作系统信息
     * 
     * @param hostInfo 主机信息
     */
    private void getHostOsInfoAsync(HostInfo hostInfo) {
        // 设置初始状态，表示正在获取主机信息
        hostInfo.setOsInfoStatus("loading");

        // 使用CompletableFuture异步执行获取操作系统信息的任务
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始异步获取主机 {} 的操作系统信息", hostInfo.getIp());
                OsInfo osInfo = getHostOsInfoInternal(hostInfo);

                // 更新主机信息和缓存
                hostInfo.setOsInfo(osInfo);
                hostInfo.setOsInfoStatus("ready");

                // 更新缓存
                updateHostInfoCache(hostInfo);

                logger.info("成功获取主机 {} 的操作系统信息", hostInfo.getIp());
            } catch (Exception e) {
                // 设置异常状态
                hostInfo.setOsInfoStatus("error");
                logger.error("异步获取主机 {} 的操作系统信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);

                // 即使出错也要更新缓存，前端可以根据状态显示错误信息
                updateHostInfoCache(hostInfo);
            }
        });
    }

    /**
     * 更新缓存中的主机信息
     */
    private void updateHostInfoCache(HostInfo hostInfo) {
        try {
            // 获取缓存中的主机信息
            Integer clusterId = hostInfo.getClusterId();
            if (clusterId == null) {
                logger.warn("主机 {} 未关联集群ID，无法更新缓存", hostInfo.getIp());
                return;
            }

            String cacheKey = clusterId + Constants.HOST_MAP;
            if (!CacheUtils.constainsKey(cacheKey)) {
                logger.warn("找不到集群 {} 的主机缓存，无法更新", clusterId);
                return;
            }

            Map<String, HostInfo> hostMap = (Map<String, HostInfo>) CacheUtils.get(cacheKey);
            if (hostMap != null) {
                // 更新缓存中的主机信息
                hostMap.put(hostInfo.getIp(), hostInfo);
                CacheUtils.put(cacheKey, hostMap);
                logger.debug("已更新集群 {} 中主机 {} 的缓存信息", clusterId, hostInfo.getIp());
            }
        } catch (Exception e) {
            logger.error("更新主机 {} 的缓存信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);
        }
    }

    /**
     * 获取主机的操作系统信息（内部实现）
     * 
     * @param hostInfo 主机信息
     * @return 操作系统信息
     */
    private OsInfo getHostOsInfoInternal(HostInfo hostInfo) {
        OsInfo osInfo = new OsInfo();

        // 获取SSH会话
        ClientSession session = getOrCreateSession(hostInfo);

        if (session == null) {
            logger.warn("无法连接到主机: {}", hostInfo.getIp());
            return osInfo;
        }

        // 获取主机名（兼容Windows和Linux系统）
        // 首先用简单的hostname命令（两个系统都支持）
        String hostname = MinaUtils.execCmdWithResult(session, "hostname");
        if (hostname != null && !hostname.isEmpty()) {
            hostname = hostname.trim();

            // 尝试获取完整主机名（FQDN）
            // 对于Linux使用hostname -f, 对于Windows使用PowerShell命令
            try {
                // 检测操作系统类型（简单的方法，检查是否存在/etc目录）
                String osCheck = MinaUtils.execCmdWithResult(session,
                        "if [ -d /etc ]; then echo 'linux'; else echo 'windows'; fi");
                if ("linux".equalsIgnoreCase(osCheck.trim())) {
                    // Linux系统，尝试获取FQDN
                    String fqdn = MinaUtils.execCmdWithResult(session, "hostname -f 2>/dev/null");
                    if (fqdn != null && !fqdn.isEmpty() && !fqdn.equals(hostname)) {
                        hostname = fqdn.trim();
                        logger.debug("获取到Linux完整主机名(FQDN): {}", hostname);
                    }
                } else {
                    // Windows系统，尝试使用PowerShell获取DNS主机名
                    String winFqdn = MinaUtils.execCmdWithResult(session,
                            "powershell -command \"(Get-WmiObject -Class Win32_ComputerSystem).DNSHostName + '.' + (Get-WmiObject -Class Win32_ComputerSystem).Domain\" 2>NUL");
                    if (winFqdn != null && !winFqdn.isEmpty() && !winFqdn.equals(hostname)
                            && !winFqdn.contains("'.'")) {
                        hostname = winFqdn.trim();
                        logger.debug("获取到Windows完整主机名(FQDN): {}", hostname);
                    }
                }
            } catch (Exception e) {
                logger.warn("尝试获取完整主机名时出错，将使用简单主机名: {}", e.getMessage());
            }

            logger.debug("最终使用的主机名: {}", hostname);
            // 将主机名设置到hostInfo对象中
            hostInfo.setHostname(hostname);
        } else {
            logger.warn("无法获取主机 {} 的主机名，将使用IP代替", hostInfo.getIp());
            // 如果获取主机名失败，则使用IP地址作为主机名
            hostInfo.setHostname(hostInfo.getIp());
        }

        // 获取/etc/os-release文件内容
        String osReleaseCommand = "cat /etc/os-release 2>/dev/null || echo 'Not Found'";
        String osRelease = MinaUtils.execCmdWithResult(session, osReleaseCommand);

        if (osRelease != null && !osRelease.contains("Not Found")) {
            // 解析/etc/os-release文件内容
            String distroId = extractValue(osRelease, "ID=");
            osInfo.setDistributionId(distroId);

            // 获取名称
            String name = extractValue(osRelease, "NAME=");
            osInfo.setDistribution(name);

            // 获取版本ID
            String versionId = extractValue(osRelease, "VERSION_ID=");
            osInfo.setVersionId(versionId);

            // 获取完整名称
            String prettyName = extractValue(osRelease, "PRETTY_NAME=");
            osInfo.setFullName(prettyName);

            // 获取内核版本
            String kernelVersion = MinaUtils.execCmdWithResult(session, "uname -r");
            if (kernelVersion != null) {
                osInfo.setKernelVersion(kernelVersion.trim());
            }

            // 获取系统架构
            String architecture = MinaUtils.execCmdWithResult(session, "uname -m");
            if (architecture != null) {
                osInfo.setArchitecture(architecture.trim());
                hostInfo.setCpuArchitecture(architecture.trim());
            }

            // 获取CPU信息
            String cpuInfoCmd = "lscpu | grep 'Model name' | sed 's/Model name://g' | sed 's/^[ \t]*//g'";
            String cpuInfo = MinaUtils.execCmdWithResult(session, cpuInfoCmd);
            if (cpuInfo != null && !cpuInfo.isEmpty()) {
                osInfo.setCpuInfo(cpuInfo.trim());
            } else {
                // 备用方法
                cpuInfo = MinaUtils.execCmdWithResult(session,
                        "cat /proc/cpuinfo | grep 'model name' | head -n 1 | sed 's/model name.*: //g'");
                if (cpuInfo != null && !cpuInfo.isEmpty()) {
                    osInfo.setCpuInfo(cpuInfo.trim());
                }
            }

            // 获取CPU核心数
            String cpuCoresCmd = "nproc --all";
            String cpuCores = MinaUtils.execCmdWithResult(session, cpuCoresCmd);
            if (cpuCores != null && !cpuCores.isEmpty()) {
                try {
                    osInfo.setCpuCores(Integer.parseInt(cpuCores.trim()));
                } catch (NumberFormatException e) {
                    logger.warn("解析CPU核心数失败: {}", cpuCores);
                }
            }

            // 获取内存信息
            String memInfoCmd = "free -m | grep 'Mem:' | awk '{print $2 \" \" $7}'";
            String memInfo = MinaUtils.execCmdWithResult(session, memInfoCmd);
            if (memInfo != null && !memInfo.isEmpty()) {
                String[] memParts = memInfo.trim().split("\\s+");
                if (memParts.length >= 2) {
                    try {
                        // 转换MB到GB并保留1位小数
                        double totalMemoryMB = Double.parseDouble(memParts[0]);
                        double availableMemoryMB = Double.parseDouble(memParts[1]);
                        osInfo.setTotalMemory(Math.round(totalMemoryMB / 1024 * 10) / 10.0);
                        osInfo.setAvailableMemory(Math.round(availableMemoryMB / 1024 * 10) / 10.0);
                    } catch (NumberFormatException e) {
                        logger.warn("解析内存信息失败: {}", memInfo);
                    }
                }
            }

            // 获取交换空间信息
            String swapInfoCmd = "free -m | grep 'Swap:' | awk '{print $2 \" \" $4}'";
            String swapInfo = MinaUtils.execCmdWithResult(session, swapInfoCmd);
            if (swapInfo != null && !swapInfo.isEmpty()) {
                String[] swapParts = swapInfo.trim().split("\\s+");
                if (swapParts.length >= 2) {
                    try {
                        // 转换MB到GB并保留1位小数
                        double totalSwapMB = Double.parseDouble(swapParts[0]);
                        double availableSwapMB = Double.parseDouble(swapParts[1]);
                        osInfo.setTotalSwap(Math.round(totalSwapMB / 1024 * 10) / 10.0);
                        osInfo.setAvailableSwap(Math.round(availableSwapMB / 1024 * 10) / 10.0);
                    } catch (NumberFormatException e) {
                        logger.warn("解析交换空间信息失败: {}", swapInfo);
                    }
                }
            }

            // 获取磁盘信息
            String diskInfoCmd = "df -h / | tail -n 1 | awk '{print $2 \" \" $4}'";
            String diskInfo = MinaUtils.execCmdWithResult(session, diskInfoCmd);
            if (diskInfo != null && !diskInfo.isEmpty()) {
                String[] diskParts = diskInfo.trim().split("\\s+");
                if (diskParts.length >= 2) {
                    try {
                        // 移除单位并转换为GB
                        String totalDiskStr = diskParts[0].replaceAll("[^0-9.]", "");
                        String availableDiskStr = diskParts[1].replaceAll("[^0-9.]", "");

                        // 处理单位换算
                        double totalDiskMultiplier = 1;
                        double availableDiskMultiplier = 1;

                        if (diskParts[0].endsWith("T")) {
                            totalDiskMultiplier = 1024;
                        }

                        if (diskParts[1].endsWith("T")) {
                            availableDiskMultiplier = 1024;
                        }

                        osInfo.setTotalDisk(Double.parseDouble(totalDiskStr) * totalDiskMultiplier);
                        osInfo.setAvailableDisk(Double.parseDouble(availableDiskStr) * availableDiskMultiplier);
                    } catch (NumberFormatException e) {
                        logger.warn("解析磁盘信息失败: {}", diskInfo);
                    }
                }
            }

            // 尝试获取GPU信息
            String gpuInfoCmd = "lspci | grep -i 'vga\\|3d\\|2d' | cut -d ':' -f3";
            String gpuInfo = MinaUtils.execCmdWithResult(session, gpuInfoCmd);
            if (gpuInfo != null && !gpuInfo.isEmpty()) {
                osInfo.setGpuInfo(gpuInfo.trim());
            } else {
                // 尝试使用nvidia-smi查询NVIDIA GPU
                gpuInfo = MinaUtils.execCmdWithResult(session,
                        "which nvidia-smi && nvidia-smi --query-gpu=name --format=csv,noheader 2>/dev/null || echo ''");
                if (gpuInfo != null && !gpuInfo.isEmpty() && !gpuInfo.contains("which")) {
                    osInfo.setGpuInfo(gpuInfo.trim());
                }
            }

            osInfo.setValid(true);
        } else {
            // 尝试替代方法获取操作系统信息
            tryAlternativeOsDetection(osInfo, session);
        }

        // 收集硬件信息
        collectHardwareInfo(osInfo, session);

        return osInfo;
    }

    // 修改原有的方法调用，将同步调用改为异步调用
    // 注意你需要修改所有调用getHostOsInfo的地方，改为调用getHostOsInfoAsync
    private OsInfo getHostOsInfo(HostInfo hostInfo) {
        // 立即启动异步任务获取完整信息
        getHostOsInfoAsync(hostInfo);

        // 返回一个空的OsInfo对象，表示信息正在获取中
        OsInfo pendingInfo = new OsInfo();
        pendingInfo.setValid(false);
        pendingInfo.setFullName("获取中...");
        return pendingInfo;
    }

    // 在HostInfo类中添加osInfoStatus字段，用于标识操作系统信息的获取状态
    // 你需要编辑HostInfo类，添加以下字段：
    // private String osInfoStatus; // 可能的值: "loading", "ready", "error"

    /**
     * 尝试替代方法获取操作系统信息
     */
    private void tryAlternativeOsDetection(OsInfo osInfo, ClientSession session) {
        try {
            // 检查是否是CentOS/RHEL (查看/etc/redhat-release)
            String redhatCmd = "cat /etc/redhat-release 2>/dev/null || echo 'Not Found'";
            String redhatRelease = MinaUtils.execCmdWithResult(session, redhatCmd);
            if (redhatRelease != null && !redhatRelease.contains("Not Found")) {
                String release = redhatRelease.trim();
                osInfo.setFullName(release);

                if (release.toLowerCase().contains("centos")) {
                    osInfo.setDistribution("CentOS");
                    osInfo.setDistributionId("centos");
                } else if (release.toLowerCase().contains("red hat")) {
                    osInfo.setDistribution("Red Hat Enterprise Linux");
                    osInfo.setDistributionId("rhel");
                }

                // 提取版本号
                String version = extractVersionFromRelease(release);
                osInfo.setVersionId(version);

                osInfo.setValid(true);
                logger.info("从redhat-release获取到操作系统信息: {}, 版本: {}", release, version);
                return;
            }

            // 使用lsb_release命令
            String lsbCmd = "lsb_release -a 2>/dev/null || echo 'Not Found'";
            String lsbOutput = MinaUtils.execCmdWithResult(session, lsbCmd);
            if (lsbOutput != null && !lsbOutput.contains("Not Found")) {
                String distro = extractFromLsb(lsbOutput, "Distributor ID:");
                String version = extractFromLsb(lsbOutput, "Release:");
                String description = extractFromLsb(lsbOutput, "Description:");

                osInfo.setFullName(description);
                osInfo.setDistribution(distro);
                osInfo.setDistributionId(distro.toLowerCase());
                osInfo.setVersionId(version);
                osInfo.setValid(true);

                logger.info("从lsb_release获取到操作系统信息: {}, 版本: {}", description, version);
                return;
            }

            // 最后尝试使用uname
            String unameCmd = "uname -a";
            String unameOutput = MinaUtils.execCmdWithResult(session, unameCmd);
            if (unameOutput != null) {
                osInfo.setFullName(unameOutput.trim());
                osInfo.setDistribution("Unknown Linux");
                osInfo.setDistributionId("unknown");
                osInfo.setValid(true);

                logger.info("从uname获取到操作系统信息: {}", unameOutput.trim());
            }
        } catch (Exception e) {
            logger.error("尝试替代方法获取操作系统信息时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 从LSB Release输出中提取信息
     */
    private String extractFromLsb(String content, String prefix) {
        if (content == null || prefix == null) {
            return "";
        }

        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    /**
     * 从发行版字符串中提取版本号
     */
    private String extractVersionFromRelease(String release) {
        if (release == null) {
            return "";
        }

        // 尝试提取类似 "7.9"、"8.3" 这样的版本号
        String versionPattern = "\\d+(\\.\\d+)+";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(versionPattern);
        java.util.regex.Matcher matcher = pattern.matcher(release);
        if (matcher.find()) {
            return matcher.group();
        }

        // 尝试提取单个数字版本
        String singleVersionPattern = "\\s\\d+\\s";
        pattern = java.util.regex.Pattern.compile(singleVersionPattern);
        matcher = pattern.matcher(release);
        if (matcher.find()) {
            return matcher.group().trim();
        }

        return "";
    }

    /**
     * 设置已受管主机的信息
     */
    private void setManagedHostInfo(HostInfo hostInfo) {
        hostInfo.setManaged(true);
        hostInfo.setInstallState(InstallState.SUCCESS);
        hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
        hostInfo.setProgress(Constants.ONE_HUNDRRD);
        hostInfo.setCheckResult(
                new CheckResult(Status.CHECK_HOST_SUCCESS.getCode(), Status.CHECK_HOST_SUCCESS.getMsg()));
    }

    /**
     * 设置未受管主机的信息
     */
    private void setUnmanagedHostInfo(HostInfo hostInfo) {
        hostInfo.setManaged(false);
        hostInfo.setInstallState(InstallState.RUNNING);
        hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
        hostInfo.setProgress(0);
        hostInfo.setCheckResult(new CheckResult(Status.START_CHECK_HOST.getCode(), Status.START_CHECK_HOST.getMsg()));
    }

    @Override
    public Result getHostCheckStatus(Integer clusterId, String sshUser, Integer sshPort) {
        // 获取检查结果列表
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        List<HostInfo> list = new ArrayList<>(map.values());
        return Result.success(list);
    }

    @Override
    public Result dispatcherHostAgentList(Integer clusterId, Integer installStateCode, Integer page, Integer pageSize) {

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String distributeAgentKey = clusterId + Constants.UNDERLINE + Constants.START_DISTRIBUTE_AGENT;
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        List<HostInfo> list = map.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).filter(e -> e.getCheckResult().getCode() == 10001)
                .collect(Collectors.toList());

        for (HostInfo hostInfo : list) {
            if (hostInfo.isManaged()) {
                hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                hostInfo.setProgress(Constants.ONE_HUNDRRD);
                hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.success"));
                hostInfo.setInstallState(InstallState.SUCCESS);
            } else if (!CacheUtils.constainsKey(distributeAgentKey + Constants.UNDERLINE + hostInfo.getIp())) {
                logger.info("start to dispatcher host agent to {}", hostInfo.getIp());
                ActorRef hostActor = ActorUtils.getLocalActor(DispatcherWorkerActor.class,
                        "dispatcherWorkerActor-" + hostInfo.getIp());
                hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
                hostInfo.setCreateTime(new Date());
                hostActor.tell(new DispatcherHostAgentCommand(hostInfo, clusterId, clusterInfo.getClusterFrame()),
                        ActorRef.noSender());
                // 保存主机agent分发历史
                CacheUtils.put(distributeAgentKey + Constants.UNDERLINE + hostInfo.getIp(), true);

            } else {
                long timeout = DateUtil.between(hostInfo.getCreateTime(), new Date(), DateUnit.MINUTE);
                long timeOutPeriodOne = PropertyUtils.getLong("timeOutPeriodOne");
                long timeOutPeriodTwo = PropertyUtils.getLong("timeOutPeriodTwo");
                Integer progress = hostInfo.getProgress();
                if ("75".equals(String.valueOf(progress)) && timeout > timeOutPeriodOne) {
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.fail.tips.one"));
                    hostInfo.setInstallState(InstallState.FAILED);
                }
                if (timeout > timeOutPeriodTwo) {
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setInstallState(InstallState.FAILED);
                }
            }
        }
        // list分页
        Integer offset = (page - 1) * pageSize;
        List<HostInfo> result = getListPage(list, offset, pageSize);
        return Result.success(result).put(Constants.TOTAL, list.size());
    }

    @Override
    public Result reStartDispatcherHostAgent(Integer clusterId, String ips) {

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);

        for (String ip : ips.split(",")) {
            // 不使用不存在的getClusterHostByIp方法
            HostInfo hostInfo = new HostInfo();
            boolean foundInMap = false;

            // 在缓存map中查找匹配IP的主机信息
            if (Objects.nonNull(map)) {
                for (Map.Entry<String, HostInfo> entry : map.entrySet()) {
                    HostInfo hi = entry.getValue();
                    if (hi != null && ip.equals(hi.getIp())) {
                        hostInfo = hi;
                        foundInMap = true;
                        break;
                    }
                }
            }

            // 如果在缓存中没找到，则使用提供的IP构建基本信息
            if (!foundInMap) {
                hostInfo.setIp(ip);
                hostInfo.setSshUser("root");
                hostInfo.setSshPort(22);
                hostInfo.setHostname(ip); // 使用IP作为hostname
            }

            ActorRef hostActor = ActorUtils.getLocalActor(DispatcherWorkerActor.class,
                    "dispatcherWorkerActor-" + hostInfo.getHostname());

            hostInfo.setInstallState(InstallState.RUNNING);
            hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
            hostInfo.setErrMsg("");
            hostInfo.setProgress(0);

            hostActor.tell(new DispatcherHostAgentCommand(hostInfo, clusterId, clusterInfo.getClusterFrame()),
                    ActorRef.noSender());
        }
        return Result.success();
    }

    @Override
    public Result hostCheckCompleted(Integer clusterId) {
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);

        // 收集未通过检查的主机信息
        List<Map<String, Object>> failedHosts = new ArrayList<>();
        Map<String, List<String>> hostToFailedItems = new HashMap<>();
        int totalFailedItems = 0;

        // 检查是否存在未完成的主机
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo hostInfo = hostInfoEntry.getValue();

            // 检查主机整体状态
            CheckResult checkResult = hostInfo.getCheckResult();
            if (checkResult == null) {
                Map<String, Object> failInfo = new HashMap<>();
                failInfo.put("hostname", hostInfo.getIp());
                failInfo.put("reason", "主机整体检查状态未完成");
                failInfo.put("code",
                        Objects.nonNull(hostInfo.getCheckResult()) ? hostInfo.getCheckResult().getCode() : "未知");
                failedHosts.add(failInfo);

                // 添加到错误映射
                List<String> failedItemsList = new ArrayList<>();
                failedItemsList.add("检查未完成");
                hostToFailedItems.put(hostInfo.getIp(), failedItemsList);
                totalFailedItems++;

                logger.info("主机 {} 的整体检查状态未完成，主机检查不通过", hostInfo.getIp());
                continue;
            }

            // 检查所有检查项状态
            List<CheckItem> checkItems = hostInfo.getCheckItems();
            if (checkItems != null) {
                List<Map<String, Object>> failedItems = new ArrayList<>();
                List<String> failedItemNames = new ArrayList<>();

                // 先统计该主机上未通过的检查项
                for (CheckItem item : checkItems) {
                    // 只有 SUCCESS 或 SKIPPED 状态被视为通过
                    if (item.getStatus() != CheckItem.Status.SUCCESS &&
                            item.getStatus() != CheckItem.Status.SKIPPED) {
                        Map<String, Object> itemInfo = new HashMap<>();
                        itemInfo.put("itemId", item.getId());
                        itemInfo.put("itemName", item.getItemName());
                        itemInfo.put("status", item.getStatus());
                        itemInfo.put("message", item.getMessage());
                        failedItems.add(itemInfo);

                        // 收集失败的检查项名称
                        String statusText = "";
                        if (item.getStatus() == CheckItem.Status.FAILED) {
                            statusText = "未通过";
                        } else if (item.getStatus() == CheckItem.Status.CHECKING) {
                            statusText = "检查中";
                        } else if (item.getStatus() == CheckItem.Status.WAITING) {
                            statusText = "待检查";
                        } else {
                            statusText = item.getStatus().toString();
                        }
                        failedItemNames.add(item.getItemName() + "(" + statusText + ")");

                        logger.info("主机 {} 的检查项 {} 状态为 {}，主机检查不通过",
                                hostInfo.getIp(), item.getItemName(), item.getStatus());
                    }
                }

                if (!failedItems.isEmpty()) {
                    Map<String, Object> failInfo = new HashMap<>();
                    failInfo.put("hostname", hostInfo.getIp());
                    failInfo.put("reason", "存在未通过的检查项");
                    failInfo.put("failedItems", failedItems);
                    failedHosts.add(failInfo);

                    // 添加到错误映射
                    hostToFailedItems.put(hostInfo.getIp(), failedItemNames);
                    // 将该主机的未通过项数量加到总数中
                    totalFailedItems += failedItemNames.size();
                }
            } else {
                // 如果检查项列表为空，也视为未完成
                Map<String, Object> failInfo = new HashMap<>();
                failInfo.put("hostname", hostInfo.getIp());
                failInfo.put("reason", "没有检查项");
                failedHosts.add(failInfo);

                // 添加到错误映射
                List<String> noItemsList = new ArrayList<>();
                noItemsList.add("没有检查项");
                hostToFailedItems.put(hostInfo.getIp(), noItemsList);
                totalFailedItems++;

                logger.info("主机 {} 没有检查项，主机检查不通过", hostInfo.getIp());
            }
        }

        if (!failedHosts.isEmpty()) {
            // 生成简洁的错误消息
            StringBuilder msgBuilder = new StringBuilder();

            // 格式化错误信息
            int hostCount = hostToFailedItems.size();
            msgBuilder.append("共有 ").append(hostCount).append(" 台主机检查未通过，").append(totalFailedItems)
                    .append(" 项未通过检查\n\n");

            // 限制显示的主机数量
            int hostDisplayLimit = 3;
            int hostDisplayCount = 0;

            for (Map.Entry<String, List<String>> entry : hostToFailedItems.entrySet()) {
                if (hostDisplayCount >= hostDisplayLimit && hostCount > hostDisplayLimit) {
                    msgBuilder.append("\n还有 ").append(hostCount - hostDisplayLimit).append(" 台主机存在问题...");
                    break;
                }

                String hostname = entry.getKey();
                List<String> items = entry.getValue();

                msgBuilder.append("• ").append(hostname).append("：")
                        .append(items.size()).append("项未通过 - ");

                // 限制每台主机显示的检查项数量
                int itemLimit = 2;
                if (items.size() <= itemLimit) {
                    msgBuilder.append(String.join("、", items));
                } else {
                    List<String> displayItems = items.subList(0, itemLimit);
                    msgBuilder.append(String.join("、", displayItems))
                            .append(" 等 ").append(items.size()).append(" 项");
                }

                msgBuilder.append("\n");
                hostDisplayCount++;
            }

            String errorMsg = msgBuilder.toString().trim();
            logger.info("存在未通过检查的主机，总数: {}, 失败项总数: {}, 错误信息: {}",
                    failedHosts.size(), totalFailedItems, errorMsg);

            return Result.success(errorMsg) // 将错误信息放在msg字段
                    .put("hostCheckCompleted", false)
                    .put("failedHosts", failedHosts);
        }

        logger.info("所有主机的所有检查项均已通过检查");

        return Result.success("所有主机检查项通过").put("hostCheckCompleted", true);
    }

    @Override
    public Result cleanupHostCheckResources(Integer clusterId) {
        try {
            if (clusterId == null) {
                logger.error("集群ID为空，无法清理主机检查资源");
                return Result.error("集群ID不能为空");
            }

            logger.info("开始清理集群[{}]的主机检查资源", clusterId);

            // 1. 删除缓存中当前集群ID的所有日志
            // 主要的日志前缀模式
            String[] logPrefixes = {
                    "CHECK_ITEM_LOG_" + clusterId + "_", // 检查项日志
                    "CHECK_LOG_" + clusterId + "_", // 检查操作日志
                    "FIX_LOG_" + clusterId + "_", // 修复操作日志
                    "CHECK_TASK_STATUS_" + clusterId + "_" // 任务状态
            };

            // 获取所有的缓存键
            // 由于没有直接获取所有键的方法，我们需要基于前缀前缀模式清理
            for (String prefix : logPrefixes) {
                logger.info("清理前缀为[{}]的日志缓存", prefix);
                // 注意：这里无法遍历所有键，所以我们只能在后续操作中处理相关键
            }

            // 通用日志缓存键
            String logCacheKey = clusterId + "_HOST_CHECK_LOG";
            if (CacheUtils.constainsKey(logCacheKey)) {
                CacheUtils.removeKey(logCacheKey);
                logger.info("已清理集群[{}]的主机检查日志缓存", clusterId);
            }

            // 2. 清理其他与检查相关的缓存
            String hostMapKey = clusterId + Constants.HOST_MAP;
            if (CacheUtils.constainsKey(hostMapKey)) {
                // 在清理前，获取所有主机信息，用于清理特定主机的日志
                Map<String, HostInfo> hostMap = (Map<String, HostInfo>) CacheUtils.get(hostMapKey);
                if (hostMap != null) {
                    for (Map.Entry<String, HostInfo> entry : hostMap.entrySet()) {
                        String hostname = entry.getKey();
                        HostInfo hostInfo = entry.getValue();

                        // 清理该主机的所有检查项日志
                        if (hostInfo.getCheckItems() != null) {
                            for (CheckItem item : hostInfo.getCheckItems()) {
                                // 删除该主机该检查项的所有日志
                                for (String prefix : logPrefixes) {
                                    String itemLogKey = prefix + hostname + "_" + item.getId();
                                    if (CacheUtils.constainsKey(itemLogKey)) {
                                        CacheUtils.removeKey(itemLogKey);
                                        logger.debug("已清理日志: {}", itemLogKey);
                                    }
                                }
                            }
                        }
                    }
                }

            }

            // 3. 关闭任务清理和连接清理
            // 如果有特定于集群ID的检查任务，可以取消它们
            if (hostCheckQueueManager != null) {
                logger.info("开始清理集群[{}]的检查任务和监控", clusterId);

                // 关闭队列健康监控
                hostCheckQueueManager.stopQueueHealthMonitor();
                logger.info("已关闭队列健康监控");

                // 关闭任务超时监控
                hostCheckQueueManager.stopTaskTimeoutMonitor();
                logger.info("已关闭任务超时监控");

                // 取消所有与该集群相关的任务
                try {
                    // cancelTask方法没有返回值，所以我们不能统计取消的任务数
                    hostCheckQueueManager.cancelTask(clusterId, null); // 取消所有主机的任务
                    logger.info("已取消集群[{}]的所有检查任务", clusterId);
                } catch (Exception e) {
                    logger.warn("取消集群[{}]的检查任务时发生异常: {}", clusterId, e.getMessage());
                }

                // 关闭定时任务（停止监控）
                hostCheckQueueManager.stopScheduledTasks();
                logger.info("已关闭所有定时监控任务");
            } else {
                logger.warn("hostCheckQueueManager为空，无法关闭相关任务");
            }

            // 4. 尝试获取并关闭AsyncCheckService的SSH连接清理
            try {
                // 通过Spring获取AsyncCheckService实例 - 考虑添加@Autowired注入AsyncCheckService
                Object asyncCheckService = applicationContext.getBean("asyncCheckService");
                if (asyncCheckService.getClass().getName().contains("AsyncCheckService")) {
                    // 反射调用stopConnectionCleanup方法停止SSH连接清理
                    Method stopConnectionCleanupMethod = asyncCheckService.getClass()
                            .getMethod("stopConnectionCleanup");
                    stopConnectionCleanupMethod.invoke(asyncCheckService);
                    logger.info("已停止AsyncCheckService的SSH连接清理任务");

                    // 反射调用stopTaskCleanup方法停止任务清理
                    Method stopTaskCleanupMethod = asyncCheckService.getClass()
                            .getMethod("stopTaskCleanup");
                    stopTaskCleanupMethod.invoke(asyncCheckService);
                    logger.info("已停止AsyncCheckService的任务清理任务");

                    // 尝试调用stopScheduledTasks方法停止所有定时任务
                    try {
                        Method stopScheduledTasksMethod = asyncCheckService.getClass()
                                .getMethod("stopScheduledTasks");
                        stopScheduledTasksMethod.invoke(asyncCheckService);
                        logger.info("已停止AsyncCheckService的所有定时任务");
                    } catch (NoSuchMethodException nsme) {
                        logger.info("AsyncCheckService没有stopScheduledTasks方法，已单独停止各项定时任务");
                    }

                    // 尝试调用disableScheduledTasks方法禁用所有定时任务
                    try {
                        Method disableScheduledTasksMethod = asyncCheckService.getClass()
                                .getMethod("disableScheduledTasks");
                        disableScheduledTasksMethod.invoke(asyncCheckService);
                        logger.info("已禁用AsyncCheckService的所有定时任务");
                    } catch (NoSuchMethodException nsme) {
                        logger.info("AsyncCheckService没有disableScheduledTasks方法");
                    }
                }
            } catch (NoSuchBeanDefinitionException e) {
                logger.warn("无法找到AsyncCheckService实例，跳过任务清理的停止");
            } catch (Exception e) {
                logger.warn("关闭AsyncCheckService的任务时发生异常: {}", e.getMessage());
            }

            logger.info("集群[{}]的主机检查资源清理完成", clusterId);
            return Result.success("主机检查资源清理完成，已清理缓存和取消检查任务");
        } catch (Exception e) {
            logger.error("清理主机检查资源时发生错误", e);
            return Result.error("清理主机检查资源失败: " + e.getMessage());
        }
    }

    @Override
    public Result cancelDispatcherHostAgent(Integer clusterId, String ip, Integer installStateCode) {
        // 此方法虽然定义但实际未使用，返回null
        return null;
    }

    @Override
    public Result dispatcherHostAgentCompleted(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo hostInfo = hostInfoEntry.getValue();
            if (hostInfo.getProgress() == 75
                    && DateUtil.between(hostInfo.getCreateTime(), new Date(), DateUnit.MINUTE) > 1) {
                logger.info("dispatcher host agent timeout");
                hostInfo.setInstallState(InstallState.FAILED);
                hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                hostInfo.setErrMsg("dispatcher host agent timeout");
            }
            if (hostInfo.getInstallState() != InstallState.SUCCESS) {
                return Result.success().put("dispatcherHostAgentCompleted", false);
            }
        }
        return Result.success().put("dispatcherHostAgentCompleted", true);
    }

    @Override
    public Result generateHostAgentCommand(String clusterHostIds, String commandType) throws Exception {
        if (StringUtils.isBlank(clusterHostIds)) {
            return Result.error(Status.SELECT_LEAST_ONE_HOST.getMsg());
        }
        String[] clusterHostIdArray = clusterHostIds.split(Constants.COMMA);
        List<String> clusterHostIdList = Arrays.asList(clusterHostIdArray);
        List<ClusterHostDO> clusterHostList = hostService.getHostListByIds(clusterHostIdList);
        for (ClusterHostDO clusterHostDO : clusterHostList) {
            ClientSession session = MinaUtils.openConnection(new HostInfo(clusterHostDO.getIp(), 22, Constants.ROOT));
            MinaUtils.execCmdWithResult(session, "service datasophon-worker " + commandType);
            logger.info("hostAgent command:{}", "service datasophon-worker " + commandType);
            if (ObjectUtil.isNotEmpty(session)) {
                if (session != null) {
                    session.close();
                }
            }
        }
        return Result.success();
    }

    /**
     * 一键 启动 主机上安装的服务
     *
     * @param clusterHostIds
     * @param commandType
     * @return
     * @throws Exception
     */
    @Override
    public Result generateHostServiceCommand(String clusterHostIds, String commandType) throws Exception {
        if (StringUtils.isBlank(clusterHostIds)) {
            return Result.error(Status.SELECT_LEAST_ONE_HOST.getMsg());
        }
        String[] clusterHostIdArray = clusterHostIds.split(Constants.COMMA);
        List<ClusterHostDO> clusterHostList = hostService.getHostListByIds(Arrays.asList(clusterHostIdArray));
        Result result = null;

        CommandType serviceCommandType = "start".equalsIgnoreCase(commandType) ? CommandType.START_SERVICE
                : CommandType.STOP_SERVICE;
        for (ClusterHostDO clusterHostDO : clusterHostList) {
            WorkerServiceMessage serviceMessage = new WorkerServiceMessage(clusterHostDO.getHostname(),
                    clusterHostDO.getClusterId(), serviceCommandType);
            try {
                ActorRef actor = ActorUtils.getLocalActor(WorkerStartActor.class, "workerStartActor");
                actor.tell(serviceMessage, ActorRef.noSender());
            } catch (Exception e) {
                logger.error("launcher worker service error!", e);
                result = Result.error("启动服务异常，Cause: " + e.getMessage());
            }
        }
        return result == null ? Result.success() : result;
    }

    private List<HostInfo> getListPage(List<HostInfo> list, Integer offset, Integer pageSize) {
        List<HostInfo> result = new ArrayList<>();
        Integer limit = offset + pageSize;
        if (list.size() < offset + pageSize) {
            limit = list.size();
        }
        for (int i = offset; i < limit; i++) {
            result.add(list.get(i));
        }
        return result;
    }

    @Override
    public Result fixCheckItem(Integer clusterId, String ip, Integer itemId) {
        return hostCheckService.fixCheckItem(clusterId, ip, itemId);
    }

    @Override
    public Result fixCheckItem(Integer clusterId, String ip, Integer itemId, Boolean skipConfirm) {
        return hostCheckService.fixCheckItem(clusterId, ip, itemId, skipConfirm);
    }

    @Override
    public Result fixSelectedCheckItems(Integer clusterId, String ip, String itemIds) {
        return hostCheckService.fixSelectedCheckItems(clusterId, ip, itemIds);
    }

    @Override
    public Result fixAllCheckItems(Integer clusterId, String ip) {
        return hostCheckService.fixAllCheckItems(clusterId, ip);
    }

    /**
     * 执行检查
     */
    private Result executeCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            ClientSession session = MinaUtils.openConnection(hostInfo);

            if (Objects.isNull(session)) {
                return Result.error("无法连接到主机");
            }

            boolean success = false;
            String message = "";
            String result;

            switch (checkItem.getId()) {
                case 1: // 主机免密检查
                    MinaUtils.CheckResult checkResult = MinaUtils.checkPasswordlessStatus(session);
                    success = checkResult.isSuccess();
                    message = checkResult.getMessage();
                    break;

                case 2: // Java环境检查
                    result = MinaUtils.execCmdWithResult(session, "which java");
                    success = result != null && !result.isEmpty();
                    message = success ? "Java环境已安装" : "未安装Java环境";
                    break;

                case 3: // 最大文件句柄数检查
                    result = MinaUtils.execCmdWithResult(session, "ulimit -n");
                    try {
                        int limit = Integer.parseInt(result.trim());
                        success = limit >= 65535;
                        message = success ? "文件句柄数配置正确" : "文件句柄数配置过低";
                    } catch (NumberFormatException e) {
                        message = "无法获取文件句柄数配置";
                    }
                    break;

                case 4: // 防火墙检查
                    result = MinaUtils.execCmdWithResult(session, "systemctl status firewalld | grep Active");
                    success = result.contains("inactive") || result.contains("dead");
                    message = success ? "防火墙已关闭" : "防火墙未关闭";
                    break;

                case 5: // SELinux检查
                    result = MinaUtils.execCmdWithResult(session, "getenforce");
                    success = "Disabled".equalsIgnoreCase(result.trim());
                    message = success ? "SELinux已禁用" : "SELinux未禁用";
                    break;

                case 6: // 时间同步检查
                    result = MinaUtils.execCmdWithResult(session, "systemctl status chronyd | grep Active");
                    success = result.contains("active");
                    message = success ? "时间同步服务运行正常" : "时间同步服务未运行";
                    break;

                default:
                    return Result.error("未知的检查项");
            }

            MinaUtils.closeConnection(session);

            checkItem.setStatus(success ? CheckItem.Status.SUCCESS : CheckItem.Status.FAILED);
            checkItem.setMessage(message);

            return Result.success();
        } catch (Exception e) {
            logger.error("执行检查失败", e);
            return Result.error("检查失败: " + e.getMessage());
        }
    }

    /**
     * 收集硬件信息
     */
    private void collectHardwareInfo(OsInfo osInfo, ClientSession session) {
        try {
            // 获取CPU型号
            String cpuInfoCmd = "lscpu | grep 'Model name' | sed 's/Model name://g' | sed 's/^[ \t]*//g'";
            String cpuInfo = MinaUtils.execCmdWithResult(session, cpuInfoCmd);
            if (cpuInfo != null && !cpuInfo.isEmpty()) {
                osInfo.setCpuInfo(cpuInfo.trim());
                logger.debug("获取到CPU信息: {}", cpuInfo.trim());
            } else {
                // 备用方法
                cpuInfo = MinaUtils.execCmdWithResult(session,
                        "cat /proc/cpuinfo | grep 'model name' | head -n 1 | sed 's/model name.*: //g'");
                if (cpuInfo != null && !cpuInfo.isEmpty()) {
                    osInfo.setCpuInfo(cpuInfo.trim());
                    logger.debug("通过备用方法获取到CPU信息: {}", cpuInfo.trim());
                }
            }

            // 获取CPU核心数
            String cpuCoresCmd = "nproc --all";
            String cpuCores = MinaUtils.execCmdWithResult(session, cpuCoresCmd);
            if (cpuCores != null && !cpuCores.isEmpty()) {
                try {
                    osInfo.setCpuCores(Integer.parseInt(cpuCores.trim()));
                    logger.debug("获取到CPU核心数: {}", cpuCores.trim());
                } catch (NumberFormatException e) {
                    logger.warn("解析CPU核心数失败: {}", cpuCores);
                }
            }

            // 获取内存信息
            String memInfoCmd = "free -m | grep 'Mem:' | awk '{print $2 \" \" $7}'";
            String memInfo = MinaUtils.execCmdWithResult(session, memInfoCmd);
            if (memInfo != null && !memInfo.isEmpty()) {
                String[] memParts = memInfo.trim().split("\\s+");
                if (memParts.length >= 2) {
                    try {
                        // 转换MB到GB并保留1位小数
                        double totalMemoryMB = Double.parseDouble(memParts[0]);
                        double availableMemoryMB = Double.parseDouble(memParts[1]);
                        osInfo.setTotalMemory(Math.round(totalMemoryMB / 1024 * 10) / 10.0);
                        osInfo.setAvailableMemory(Math.round(availableMemoryMB / 1024 * 10) / 10.0);
                    } catch (NumberFormatException e) {
                        logger.warn("解析内存信息失败: {}", memInfo);
                    }
                }
            }

            // 获取磁盘信息
            String diskInfoCmd = "df -h / | tail -n 1 | awk '{print $2 \" \" $4}'";
            String diskInfo = MinaUtils.execCmdWithResult(session, diskInfoCmd);
            if (diskInfo != null && !diskInfo.isEmpty()) {
                String[] diskParts = diskInfo.trim().split("\\s+");
                if (diskParts.length >= 2) {
                    try {
                        // 提取数字部分并确定单位
                        String totalDiskStr = diskParts[0].replaceAll("[^0-9.]", "");
                        String availableDiskStr = diskParts[1].replaceAll("[^0-9.]", "");

                        double totalDiskMultiplier = 1;
                        double availableDiskMultiplier = 1;

                        if (diskParts[0].toUpperCase().endsWith("T")) {
                            totalDiskMultiplier = 1024;
                        }

                        if (diskParts[1].toUpperCase().endsWith("T")) {
                            availableDiskMultiplier = 1024;
                        }

                        double totalDisk = Double.parseDouble(totalDiskStr) * totalDiskMultiplier;
                        double availableDisk = Double.parseDouble(availableDiskStr) * availableDiskMultiplier;

                        osInfo.setTotalDisk(totalDisk);
                        osInfo.setAvailableDisk(availableDisk);
                        logger.debug("获取到磁盘信息: 总共 {}GB, 可用 {}GB", totalDisk, availableDisk);
                    } catch (NumberFormatException e) {
                        logger.warn("解析磁盘信息失败: {}", diskInfo);
                    }
                }
            }

            // 收集系统负载信息
            String loadInfoCmd = "cat /proc/loadavg";
            String loadInfo = MinaUtils.execCmdWithResult(session, loadInfoCmd);
            if (loadInfo != null && !loadInfo.isEmpty()) {
                String[] loadParts = loadInfo.trim().split("\\s+");
                if (loadParts.length >= 3) {
                    try {
                        double load1 = Double.parseDouble(loadParts[0]);
                        double load5 = Double.parseDouble(loadParts[1]);
                        double load15 = Double.parseDouble(loadParts[2]);

                        // 存储负载信息（如果OsInfo类有对应字段）
                        osInfo.getClass().getDeclaredMethod("setLoad1Min", Double.class).invoke(osInfo, load1);
                        osInfo.getClass().getDeclaredMethod("setLoad5Min", Double.class).invoke(osInfo, load5);
                        osInfo.getClass().getDeclaredMethod("setLoad15Min", Double.class).invoke(osInfo, load15);
                        logger.debug("获取到系统负载: 1分钟 {}, 5分钟 {}, 15分钟 {}", load1, load5, load15);
                    } catch (Exception e) {
                        logger.warn("设置系统负载信息失败: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("收集硬件信息时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 从lscpu输出中提取特定字段的值
     */
    private String extractValueFromLscpu(String lscpuOutput, String fieldName) {
        if (lscpuOutput == null || fieldName == null) {
            return null;
        }

        String[] lines = lscpuOutput.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith(fieldName)) {
                return line.substring(fieldName.length()).trim();
            }
        }

        return null;
    }

    /**
     * 从/etc/os-release文件内容中提取指定键的值
     *
     * @param content 文件内容
     * @param key     要查找的键（如ID=、NAME=等）
     * @return 找到的值，如果未找到则返回空字符串
     */
    private String extractValue(String content, String key) {
        if (content == null || key == null) {
            return "";
        }

        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(key)) {
                String value = line.substring(key.length()).trim();
                // 移除引号（如果有）
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }

        return "";
    }

    /**
     * 新增一个方法，用于获取主机的操作系统信息状态
     * 供前端查询主机信息是否已准备好
     */
    public Result getHostOsInfoStatus(Integer clusterId, String ip) {
        try {
            Map<String, HostInfo> hostMap = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
            if (hostMap == null || !hostMap.containsKey(ip)) {
                return Result.error("找不到指定主机信息");
            }

            HostInfo hostInfo = hostMap.get(ip);
            String status = hostInfo.getOsInfoStatus();

            return Result.success().put("status", status)
                    .put("hostname", hostInfo.getHostname())
                    .put("ip", hostInfo.getIp());
        } catch (Exception e) {
            logger.error("获取主机操作系统信息状态时出错", e);
            return Result.error("系统异常: " + e.getMessage());
        }
    }

    /**
     * 新增一个方法，用于获取集群中所有主机的操作系统信息状态
     * 供前端批量查询主机信息是否已准备好
     */
    public Result getAllHostOsInfoStatus(Integer clusterId) {
        try {
            Map<String, HostInfo> hostMap = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
            if (hostMap == null) {
                return Result.error("找不到集群主机信息");
            }

            List<Map<String, Object>> statusList = new ArrayList<>();
            for (Map.Entry<String, HostInfo> entry : hostMap.entrySet()) {
                HostInfo hostInfo = entry.getValue();
                Map<String, Object> statusInfo = new HashMap<>();
                statusInfo.put("ip", hostInfo.getIp());
                statusInfo.put("hostname", hostInfo.getHostname());
                statusInfo.put("status", hostInfo.getOsInfoStatus());

                statusList.add(statusInfo);
            }

            return Result.success(statusList);
        } catch (Exception e) {
            logger.error("获取集群所有主机操作系统信息状态时出错", e);
            return Result.error("系统异常: " + e.getMessage());
        }
    }
}
