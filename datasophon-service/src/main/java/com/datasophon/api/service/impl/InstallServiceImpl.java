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
import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.service.checker.queue.HostCheckQueueManager;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.service.impl.osinfo.OsInfoCollectorFactory;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.DispatcherHostAgentCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.WorkerServiceMessage;
import com.datasophon.common.model.hardware.GpuInfo;
import com.datasophon.common.model.hardware.NetworkInfo;
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

    @Autowired
    private OsInfoCollectorFactory osInfoCollectorFactory;

    @Autowired
    private OsInfoService osInfoService;

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

            Map<String, HostInfo> hostMap = saveHostInfo(clusterId, ips, sshUser, sshPort, sshPassword, true);

            // 如果没有获取到主机信息,返回错误提示
            if (Objects.isNull(hostMap)) {
                return Result.error("未获取到有效的主机信息");
            }

            List<HostInfo> hostList = new ArrayList<>(hostMap.values());
            hostList.sort(Comparator.comparing(HostInfo::getIp));

            // 计算每个主机的状态
            hostList.forEach(hostInfo -> {
                processHostInfo(hostInfo);
                hostInfo.calculateStatus();
            });

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
     * 处理主机信息，确保各项信息可用于前端展示
     * 
     * @param hostInfo 主机信息对象
     */
    private void processHostInfo(HostInfo hostInfo) {
        // 确保hostsFile信息可用于前端
        if (hostInfo.getHostsFile() == null) {
            hostInfo.setHostsFile(""); // 避免前端收到null
        }

        // 确保错误信息字段不为null
        if (hostInfo.getSshErrorMsg() == null) {
            hostInfo.setSshErrorMsg("");
        }

        if (hostInfo.getErrorMessage() == null) {
            hostInfo.setErrorMessage("");
        }

        if (hostInfo.getOsErrorMsg() == null) {
            hostInfo.setOsErrorMsg("");
        }

        // 确保操作系统信息可用于前端
        if (hostInfo.getOsInfo() != null) {
            if (hostInfo.getOsInfo().getDistribution() == null) {
                hostInfo.getOsInfo().setDistribution(""); // 避免前端收到null
            }

            // 确保网络信息不为null
            if (hostInfo.getOsInfo().getNetworkInfo() == null) {
                hostInfo.getOsInfo().setNetworkInfo(new NetworkInfo());
            }

            // 确保GPU信息不为null
            if (hostInfo.getOsInfo().getGpuInfo() == null) {
                GpuInfo gpuInfo = new GpuInfo();
                gpuInfo.setInfo("未检测到GPU设备");
                gpuInfo.setTotalMemory(0.0);
                gpuInfo.setUsedMemory(0.0);
                hostInfo.getOsInfo().setGpuInfo(gpuInfo);
            }
        }

        // 确保SSH连接状态信息可用于前端
        if (hostInfo.getSshConnectStatus() == null) {
            // 如果osInfoStatus是error，则认为SSH连接失败
            if (OsInfoStatusEnum.ERROR.equals(hostInfo.getOsInfoStatus())) {
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);

                // 设置SSH错误信息（如果尚未设置）
                if (StringUtils.isBlank(hostInfo.getSshErrorMsg())) {
                    hostInfo.setSshErrorMsg("SSH连接失败，无法获取主机信息");
                }
            } else {
                // 默认设为LOADING，让前端显示加载中
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.LOADING);
            }
        }
    }

    /**
     * 保存主机信息到缓存
     *
     * @param clusterId             集群ID
     * @param hosts                 主机列表字符串
     * @param sshUser               SSH用户名
     * @param sshPort               SSH端口
     * @param sshPassword           SSH密码
     * @param startOsInfoCollection 是否开始操作系统信息收集
     */
    private Map<String, HostInfo> saveHostInfo(Integer clusterId, String hosts, String sshUser, Integer sshPort,
            String sshPassword, boolean startOsInfoCollection) {
        String hostsMd5 = SecureUtil.md5(hosts);
        // 1. 检查缓存中是否存在有效的主机列表
        if (isCacheValid(clusterId, hostsMd5)) {
            logger.debug("从缓存获取主机列表");
            return (HashMap<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        }

        logger.info("处理主机列表: 集群ID={}, 主机数量={}, 用户={}, 端口={}",
                clusterId, hosts.split(Constants.COMMA).length, sshUser, sshPort);

        Map<String, HostInfo> hostMap = processHostList(clusterId, hosts, hostsMd5, sshPort, sshUser, sshPassword);

        // 将结果存入缓存
        CacheUtils.put(clusterId + Constants.HOST_MAP, hostMap);
        logger.info("主机列表已存入缓存，共{}台主机", hostMap.size());

        // 如果需要，触发所有主机的操作系统信息收集
        if (startOsInfoCollection) {
            logger.info("开始异步触发所有主机的操作系统信息收集");
            // 创建一个新线程进行主机信息收集，保证主接口立即返回
            Thread thread = new Thread(() -> {
                try {
                    // 排序主机列表，保证按IP地址顺序处理
                    List<HostInfo> sortedHosts = new ArrayList<>(hostMap.values());
                    sortedHosts.sort(Comparator.comparing(HostInfo::getIp));

                    for (HostInfo hostInfo : sortedHosts) {
                        // 设置初始状态
                        hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);

                        try {
                            // 在线程内进行异步验证SSH连接并收集信息
                            osInfoService.getHostOsInfoAsync(hostInfo);

                            logger.info("已触发主机[{}]的信息异步收集", hostInfo.getIp());
                        } catch (Exception e) {
                            logger.error("触发主机[{}]信息收集失败: {}", hostInfo.getIp(), e.getMessage(), e);
                            // 设置错误状态和详细信息
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                            hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                            hostInfo.setSshErrorMsg("SSH连接异常: " + e.getMessage());
                            hostInfo.setErrorMessage("连接主机时发生异常");
                            hostInfo.setOsErrorMsg("由于连接异常，无法获取操作系统信息");
                        }
                    }

                    logger.info("所有主机信息收集任务已异步触发");
                } catch (Exception e) {
                    logger.error("主机信息收集线程异常: {}", e.getMessage(), e);
                }
            });
            thread.setName("Host-OS-Info-Collection-Thread");
            thread.setDaemon(true);
            thread.start();
        }

        return hostMap;
    }

    /**
     * 验证SSH连接
     * 
     * @param hostInfo 主机信息
     * @return 连接是否成功
     */
    private boolean validateSshConnection(HostInfo hostInfo) {
        ClientSession session = null;
        try {
            session = getOrCreateSession(hostInfo);
            if (session != null && session.isOpen()) {
                // 执行一个简单的命令验证连接
                String result = MinaUtils.execCmdWithResult(session, "echo connection_test");
                boolean success = result != null && result.contains("connection_test");

                // 连接成功
                if (success) {
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
                    return true;
                } else {
                    // 命令执行失败
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setSshErrorMsg("SSH连接成功但无法执行命令，请检查用户权限");
                    return false;
                }
            } else {
                // 连接创建失败
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setSshErrorMsg("无法创建SSH连接，请检查IP地址、端口和防火墙设置");
                return false;
            }
        } catch (Exception e) {
            // 处理不同类型的异常，设置更友好的错误信息
            hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);

            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getSimpleName();
            }

            // 根据异常类型和错误消息设置友好的错误提示
            if (errorMsg.contains("Auth fail") || errorMsg.contains("authentication failed")) {
                hostInfo.setSshErrorMsg("SSH认证失败：用户名或密码错误");
            } else if (errorMsg.contains("Connection refused")) {
                hostInfo.setSshErrorMsg("SSH连接被拒绝：SSH服务未启动或端口未开放");
            } else if (errorMsg.contains("connect timed out")) {
                hostInfo.setSshErrorMsg("SSH连接超时：网络不通或防火墙阻止");
            } else if (errorMsg.contains("UnknownHostException")) {
                hostInfo.setSshErrorMsg("无法解析主机名：请检查DNS配置或hosts文件");
            } else if (errorMsg.contains("No route to host")) {
                hostInfo.setSshErrorMsg("无法访问主机：网络不通或主机未启动");
            } else {
                hostInfo.setSshErrorMsg("SSH连接错误：" + errorMsg);
            }

            logger.error("主机[{}]SSH连接验证失败: {}", hostInfo.getIp(), errorMsg, e);
            return false;
        } finally {
            MinaUtils.closeConnection(session);
        }
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

        // 2. 遍历处理每个主机，将结果存入临时Map
        Map<String, HostInfo> tempMap = new HashMap<>();
        for (String host : hostArray) {
            // 只创建主机信息，不执行异步获取操作系统信息
            processHostWithoutOsInfo(host, sshPort, sshUser, sshPassword, clusterId, tempMap);
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
     * 处理单个主机信息（不执行异步获取操作系统信息）
     */
    private void processHostWithoutOsInfo(String host, Integer sshPort, String sshUser, String sshPassword,
            Integer clusterId,
            Map<String, HostInfo> hostInfoMap) {
        // 1. 处理IP范围，如192.168.1.[1-5]
        if (host.contains("[") && host.contains("]")) {
            processIpRangeWithoutOsInfo(host, sshPort, sshUser, sshPassword, clusterId, hostInfoMap);
            return;
        }

        // 2. 普通主机，直接创建主机信息
        HostInfo hostInfo = createHostInfo(host, sshPort, sshUser, sshPassword, clusterId);
        hostInfoMap.put(hostInfo.getIp(), hostInfo);
    }

    /**
     * 处理IP范围（不执行异步获取操作系统信息）
     */
    private void processIpRangeWithoutOsInfo(String host, Integer sshPort, String sshUser, String sshPassword,
            Integer clusterId,
            Map<String, HostInfo> hostInfoMap) {
        int start = host.indexOf("[");
        String prefix = host.substring(0, start);
        String range = host.substring(start + 1, host.length() - 1);
        String[] split = range.split("-");

        // 1. 处理字母范围，如[a-e]
        if (host.matches(Constants.HAS_EN)) {
            processLetterRangeWithoutOsInfo(prefix, split[0], split[1], sshPort, sshUser, sshPassword, clusterId,
                    hostInfoMap);
            return;
        }

        // 2. 处理数字范围，如[1-5]
        processNumberRangeWithoutOsInfo(prefix, split, sshPort, sshUser, sshPassword, clusterId, hostInfoMap);
    }

    /**
     * 处理字母范围的主机名（不执行异步获取操作系统信息）
     */
    private void processLetterRangeWithoutOsInfo(String prefix, String start, String end, Integer sshPort,
            String sshUser,
            String sshPassword, Integer clusterId, Map<String, HostInfo> hostInfoMap) {
        List<String> hostList = PlaceholderUtils.getNewEquipmentNoList(start, end);
        for (String suffix : hostList) {
            HostInfo hostInfo = createHostInfo(prefix + suffix, sshPort, sshUser, sshPassword, clusterId);
            hostInfoMap.put(hostInfo.getIp(), hostInfo);
        }
    }

    /**
     * 处理数字范围的主机名（不执行异步获取操作系统信息）
     */
    private void processNumberRangeWithoutOsInfo(String prefix, String[] range, Integer sshPort, String sshUser,
            String sshPassword,
            Integer clusterId, Map<String, HostInfo> hostInfoMap) {
        int start = Integer.parseInt(range[0]);
        int end = Integer.parseInt(range[1]);
        for (int i = start; i <= end; i++) {
            HostInfo hostInfo = createHostInfo(prefix + i, sshPort, sshUser, sshPassword, clusterId);
            hostInfoMap.put(hostInfo.getIp(), hostInfo);
        }
    }

    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid(Integer clusterId, String hostsMd5) {
        return CacheUtils.constainsKey(clusterId + Constants.HOST_MAP);
    }

    /**
     * 创建主机信息对象
     *
     * @param host        主机地址
     * @param sshPort     SSH端口
     * @param sshUser     SSH用户名
     * @param sshPassword SSH密码
     * @return 主机信息对象
     */
    private HostInfo createHostInfo(String host, Integer sshPort, String sshUser, String sshPassword,
            Integer clusterId) {
        HostInfo hostInfo = new HostInfo();

        // 1. 设置基本信息
        hostInfo.setIp(HostUtils.getIp(host));
        hostInfo.setSshPort(sshPort);
        hostInfo.setSshUser(sshUser);
        hostInfo.setSshPassword(sshPassword);
        hostInfo.setClusterId(clusterId);
        hostInfo.setCreateTime(new Date());

        // 初始化错误信息字段
        hostInfo.setSshErrorMsg("");
        hostInfo.setErrorMessage("");
        hostInfo.setOsErrorMsg("");

        // 2. 检查主机是否已受管
        ClusterHostDO hostEntity = hostService.getClusterHostByHostname(hostInfo.getHostname());
        if (Objects.nonNull(hostEntity)) {
            setManagedHostInfo(hostInfo);
        } else {
            setUnmanagedHostInfo(hostInfo);
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
            // 使用toString()获取状态的字符串表示，兼容前端
            String status = hostInfo.getOsInfoStatus() != null ? hostInfo.getOsInfoStatus().toString() : null;

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
                // 使用toString()获取状态的字符串表示，兼容前端
                statusInfo.put("status",
                        hostInfo.getOsInfoStatus() != null ? hostInfo.getOsInfoStatus().toString() : null);

                statusList.add(statusInfo);
            }

            return Result.success(statusList);
        } catch (Exception e) {
            logger.error("获取集群所有主机操作系统信息状态时出错", e);
            return Result.error("系统异常: " + e.getMessage());
        }
    }

    // 修改方法，使用OsInfoService
    private void getHostOsInfo(HostInfo hostInfo) {
        // 委托给OsInfoService实现

    }

    @Override
    public Result clearHostEnvCheckCache() {
        try {

            logger.debug("删除主机检查项缓存");
            CacheUtils.clear();
            logger.info("主机环境校验缓存清理完成");
            return Result.success();
        } catch (Exception e) {
            logger.error("清理主机环境校验缓存失败", e);
            return Result.error("清理主机环境校验缓存失败: " + e.getMessage());
        }
    }
}
