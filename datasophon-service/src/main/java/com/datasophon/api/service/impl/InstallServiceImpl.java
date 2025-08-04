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

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import com.datasophon.common.enums.Status;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.DispatcherWorkerActor;
import com.datasophon.api.master.WorkerStartActor;
import com.datasophon.api.converter.InstallStepConverter;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.InstallService;
import com.datasophon.api.service.OsInfoService;
import com.datasophon.common.dto.InstallStepDTO;
import com.datasophon.dao.mapper.InstallStepMapper;
import com.datasophon.api.service.checker.common.CommandResult;

import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.DispatcherHostAgentCommand;
import com.datasophon.common.dto.HostCheckStatusDto;
import com.datasophon.common.dto.PageResult;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.exception.ServiceException;
import com.datasophon.common.exception.BusinessException;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.WorkerServiceMessage;
import com.datasophon.common.model.hardware.GpuInfo;
import com.datasophon.common.model.hardware.NetworkInfo;
import com.datasophon.common.utils.HostUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.InstallStepEntity;
import com.datasophon.kubernetes.util.KubeUtil;
import com.datasophon.kubernetes.model.K8sNodeInfo;
import com.datasophon.api.converter.K8sToClusterHostConverter;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.sshd.client.session.ClientSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 安装服务实现类
 * 负责处理集群安装流程、主机检查、Agent分发等核心业务逻辑
 * 支持传统和Kubernetes两种部署模式
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Slf4j
@Service("installService")
@RequiredArgsConstructor
public class InstallServiceImpl extends ServiceImpl<InstallStepMapper, InstallStepEntity> implements InstallService {

    // 添加一个原子计数器，用于控制日志打印频率
    private static final AtomicInteger logCounter = new AtomicInteger(0);
    private static final int LOG_PRINT_INTERVAL = 10;

    // 依赖注入 - 使用构造器注入
    private final ClusterInfoService clusterInfoService;
    private final ClusterHostService hostService;
    private final OsInfoService osInfoService;
    private final K8sToClusterHostConverter k8sToClusterHostConverter;
    private final InstallStepConverter installStepConverter;

    // 线程池需要特殊处理，因为有@Qualifier注解，使用字段注入
    @Qualifier("osInfoExecutor")
    private final ExecutorService osInfoExecutor;

    @Qualifier("hardwareInfoExecutor")
    private final ExecutorService hardwareInfoExecutor;

    @Override
    public List<InstallStepDTO> getInstallStepsByType(Integer installType) {
        try {
            if (installType == null) {
                throw new RuntimeException("安装类型不能为空");
            }

            List<InstallStepEntity> entities = getMapper().selectByInstallType(installType);
            return installStepConverter.entityListToDtoList(entities);
        } catch (Exception e) {
            log.error("根据类型获取安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("根据类型获取安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public InstallStepDTO getInstallStepById(Integer id) {
        try {
            if (id == null) {
                throw new RuntimeException("安装步骤ID不能为空");
            }

            InstallStepEntity entity = getById(id);
            if (entity == null) {
                throw new RuntimeException("未找到ID为 " + id + " 的安装步骤");
            }

            return installStepConverter.entityToDto(entity);
        } catch (Exception e) {
            log.error("根据ID获取安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("根据ID获取安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public InstallStepDTO saveInstallStep(InstallStepDTO installStepDTO) {
        try {
            if (installStepDTO == null) {
                throw new RuntimeException("安装步骤信息不能为空");
            }

            InstallStepEntity entity = installStepConverter.dtoToEntity(installStepDTO);
            boolean result = save(entity);

            if (!result) {
                throw new RuntimeException("保存安装步骤失败");
            }

            return installStepConverter.entityToDto(entity);
        } catch (Exception e) {
            log.error("保存安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public InstallStepDTO updateInstallStep(InstallStepDTO installStepDTO) {
        try {
            if (installStepDTO == null || installStepDTO.id() == null) {
                throw new RuntimeException("安装步骤信息或ID不能为空");
            }

            // 检查记录是否存在
            InstallStepEntity existingEntity = getById(installStepDTO.id());
            if (existingEntity == null) {
                throw new RuntimeException("未找到ID为 " + installStepDTO.id() + " 的安装步骤");
            }

            InstallStepEntity entity = installStepConverter.dtoToEntity(installStepDTO);
            boolean result = updateById(entity);

            if (!result) {
                throw new RuntimeException("更新安装步骤失败");
            }

            return installStepConverter.entityToDto(entity);
        } catch (Exception e) {
            log.error("更新安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public boolean removeInstallStepByIds(List<Integer> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                throw new RuntimeException("删除的ID列表不能为空");
            }

            return removeByIds(ids);
        } catch (Exception e) {
            log.error("批量删除安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量删除安装步骤失败: " + e.getMessage());
        }
    }

    /**
     * 解析主机列表并进行环境检测
     * <p>
     * 处理流程：
     * 1. 获取集群信息，判断集群类型（K8S或传统）
     * 2. K8S模式：从K8S API获取节点列表和架构信息
     * 3. 传统模式：解析用户输入的主机列表并进行SSH检测
     * 4. 分页返回结果
     *
     * @param clusterId   集群ID
     * @param ips         主机列表字符串（K8S模式下可为空）
     * @param sshUser     SSH用户名（K8S模式下可为空）
     * @param sshPort     SSH端口（K8S模式下可为空）
     * @param sshPassword SSH密码（K8S模式下可为空）
     * @param page        当前页码
     * @param pageSize    每页大小
     * @return 分页后的主机列表结果
     */
    @Override
    public PageResult<HostInfo> analysisHostList(Integer clusterId, String ips, String sshUser, Integer sshPort,
            String sshPassword, String kubeConfigContent, Integer page, Integer pageSize) {
        try {
            // 获取集群信息以判断集群类型
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            if (clusterInfo == null) {
                throw new ServiceException("集群不存在");
            }

            String depType = clusterInfo.getDepType();
            log.info("集群ID: {}, 部署类型: {}", clusterId, depType);

            // 根据部署类型路由到不同的处理方法
            if ("Kubernetes".equals(depType)) {
                log.info("检测到Kubernetes集群，使用K8S API获取节点列表");
                return analysisHostListForKubernetes(clusterId, kubeConfigContent, page, pageSize);
            } else {
                log.info("检测到传统集群，使用SSH方式获取主机列表");
                return analysisHostListForTraditional(clusterId, ips, sshUser, sshPort, sshPassword, page, pageSize);
            }

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析主机列表失败: {}", ExceptionUtil.getSimpleMessage(e));
            throw new ServiceException("解析主机列表失败: " + e.getMessage());
        }
    }

    /**
     * 传统集群模式的主机列表解析
     */
    private PageResult<HostInfo> analysisHostListForTraditional(Integer clusterId, String ips, String sshUser,
            Integer sshPort,
            String sshPassword,
            Integer page, Integer pageSize) {
        try {
            if (StringUtils.isBlank(ips)) {
                throw new ServiceException("主机列表不能为空");
            }

            // 修改：每次调用都应该触发saveHostInfo，但用标记控制是否重新收集
            Map<String, HostInfo> hostMap = saveHostInfo(clusterId, ips, sshUser, sshPort, sshPassword, page,
                    pageSize);

            // 如果没有获取到主机信息,返回错误提示
            List<HostInfo> hostList = new ArrayList<>(hostMap.values());

            // 使用HostUtils的统一排序方法对IP进行排序
            List<String> sortedIps = HostUtils.sortIpAddresses(hostList.stream()
                    .map(HostInfo::getIp)
                    .collect(Collectors.toList()));

            // 按照排序后的IP顺序重新组织主机列表
            List<HostInfo> sortedHostList = new ArrayList<>();
            for (String ip : sortedIps) {
                hostList.stream()
                        .filter(host -> ip.equals(host.getIp()))
                        .findFirst()
                        .ifPresent(sortedHostList::add);
            }

            // 使用排序后的主机列表
            hostList = sortedHostList;

            // 计算每个主机的状态
            hostList.forEach(hostInfo -> {
                processHostInfo(hostInfo);
                hostInfo.calculateStatus();
            });

            // 分页处理
            int offset = (page - 1) * pageSize;
            int end = Math.min(offset + pageSize, hostList.size());
            List<HostInfo> pagedHosts = hostList.subList(offset, end);

            return PageResult.of(pagedHosts, hostList.size(), page, pageSize);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("传统集群主机列表解析失败: {}", ExceptionUtil.getSimpleMessage(e));
            throw new ServiceException("传统集群主机列表解析失败: " + e.getMessage());
        }
    }

    /**
     * 处理主机信息，确保各项信息可用于前端展示
     *
     * @param hostInfo 主机信息对象
     */
    private void processHostInfo(HostInfo hostInfo) {
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

        // 确保主机名状态字段初始化，使用与OS状态相同的加载逻辑
        if (hostInfo.getHostnameStatus() == null) {
            // 如果SSH连接是成功的，但主机名尚未收集，设置为LOADING
            if (StringUtils.isBlank(hostInfo.getHostname())) {
                hostInfo.setHostnameStatus(OsInfoStatusEnum.LOADING);
            } else {
                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
            }
        } else if (OsInfoStatusEnum.ERROR.equals(hostInfo.getHostnameStatus())) {
            // 如果已经获取到主机名，但状态仍为ERROR，修正为SUCCESS
            if (StringUtils.isNotBlank(hostInfo.getHostname()) && !hostInfo.getHostname().equals(hostInfo.getIp())) {
                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
            }
        }

        // 确保操作系统信息可用于前端
        if (hostInfo.getOsInfo() != null) {
            // 确保distribution字段有值
            if (hostInfo.getOsInfo().getDistribution() == null || hostInfo.getOsInfo().getDistribution().isEmpty()) {
                // 如果fullName有值，从fullName中提取distribution
                String fullName = hostInfo.getOsInfo().getFullName();
                if (fullName != null && !fullName.isEmpty()) {
                    String lowerFullName = fullName.toLowerCase();
                    if (lowerFullName.contains("centos")) {
                        hostInfo.getOsInfo().setDistribution("CentOS");
                    } else if (lowerFullName.contains("ubuntu")) {
                        hostInfo.getOsInfo().setDistribution("Ubuntu");
                    } else if (lowerFullName.contains("debian")) {
                        hostInfo.getOsInfo().setDistribution("Debian");
                    } else if (lowerFullName.contains("red hat") || lowerFullName.contains("redhat")) {
                        hostInfo.getOsInfo().setDistribution("RedHat");
                    } else if (lowerFullName.contains("kylin")) {
                        hostInfo.getOsInfo().setDistribution("Kylin");
                    } else if (lowerFullName.contains("alpine")) {
                        hostInfo.getOsInfo().setDistribution("Alpine");
                    } else {
                        hostInfo.getOsInfo().setDistribution("Linux");
                    }
                } else {
                    hostInfo.getOsInfo().setDistribution(""); // 避免前端收到null
                }
            }

            // 确保osDistribution字段正确设置
            if (hostInfo.getOsInfo().getDistribution() != null && !hostInfo.getOsInfo().getDistribution().isEmpty()) {
                // 更新操作系统发行版枚举
                hostInfo.getOsInfo().updateOsDistribution();
                // 如果更新后仍为其他类型，则强制使用distribution字段设置
                if ("linux".equals(hostInfo.getOsInfo().getDistributionId())) {
                    hostInfo.getOsInfo().forceUpdateDistribution();
                }
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
            // 检查是否有主机名或系统信息（如果有任何一个，说明SSH连接是成功的）
            boolean hasSshSuccess = StringUtils.isNotBlank(hostInfo.getHostname()) &&
                    !hostInfo.getHostname().equals(hostInfo.getIp());

            // 如果已经获取到主机名（非空且不等于IP），则SSH连接成功

            // 如果已经获取到操作系统信息，则SSH连接成功
            if (hostInfo.getOsInfo() != null &&
                    hostInfo.getOsInfo().isValid() &&
                    OsInfoStatusEnum.SUCCESS.equals(hostInfo.getOsInfoStatus())) {
                hasSshSuccess = true;
            }

            if (hasSshSuccess) {
                // 如果有任何成功的SSH交互，则SSH连接是成功的
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
                // 清除可能存在的错误信息
                if (StringUtils.isNotBlank(hostInfo.getSshErrorMsg())) {
                    hostInfo.setSshErrorMsg("");
                }
            } else if (OsInfoStatusEnum.ERROR.equals(hostInfo.getOsInfoStatus())) {
                // 只有在没有成功的SSH交互且操作系统信息获取失败时，才可能是SSH连接失败
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);

                // 设置SSH错误信息（如果尚未设置）
                if (StringUtils.isBlank(hostInfo.getSshErrorMsg())) {
                    hostInfo.setSshErrorMsg("SSH连接失败，无法获取主机信息");
                }
            } else {
                // 默认设为LOADING，让前端显示加载中
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.LOADING);
            }
        } else if (OsInfoStatusEnum.ERROR.equals(hostInfo.getSshConnectStatus())) {
            // 即使状态被设置为ERROR，但如果有主机名或系统信息，仍应该纠正为SUCCESS
            boolean hasSshSuccess = StringUtils.isNotBlank(hostInfo.getHostname()) &&
                    !hostInfo.getHostname().equals(hostInfo.getIp());

            // 如果已经获取到主机名（非空且不等于IP），则SSH连接成功

            // 如果已经获取到操作系统信息，则SSH连接成功
            if (hostInfo.getOsInfo() != null &&
                    hostInfo.getOsInfo().isValid() &&
                    OsInfoStatusEnum.SUCCESS.equals(hostInfo.getOsInfoStatus())) {
                hasSshSuccess = true;
            }

            if (hasSshSuccess) {
                // 纠正错误的SSH连接状态
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
                // 清除错误信息
                hostInfo.setSshErrorMsg("");
                log.info("主机[{}]的SSH连接状态被纠正为SUCCESS，因为已成功获取主机信息", hostInfo.getIp());
            }
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
     * @param page        当前页码
     * @param pageSize    每页大小
     */
    private Map<String, HostInfo> saveHostInfo(Integer clusterId, String hosts, String sshUser, Integer sshPort,
            String sshPassword, Integer page, Integer pageSize) {
        // 定义已收集主机集合的缓存键
        String collectedHostsKey = clusterId + "_COLLECTED_HOSTS";
        // 定义正在收集中的主机集合的缓存键（防止并发重复收集）
        String collectingHostsKey = clusterId + "_COLLECTING_HOSTS";
        Map<String, HostInfo> hostMap;
        Set<String> collectedHosts;
        Set<String> collectingHosts;

        // 1. 检查缓存中是否存在有效的主机列表
        if (isCacheValid(clusterId)) {
            log.debug("从缓存获取主机列表");
            hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

            // 获取已收集主机集合，如果不存在则创建新的
            if (CacheUtils.constainsKey(collectedHostsKey)) {
                @SuppressWarnings("unchecked")
                Set<String> cachedCollectedHosts = (Set<String>) CacheUtils.get(collectedHostsKey);
                collectedHosts = Collections.synchronizedSet(cachedCollectedHosts);
                log.debug("从缓存获取已收集主机列表，已收集{}台主机", collectedHosts.size());
            } else {
                collectedHosts = Collections.synchronizedSet(new HashSet<>());
                CacheUtils.put(collectedHostsKey, collectedHosts);
                log.debug("创建新的已收集主机列表缓存");
            }

            // 获取正在收集的主机集合，如果不存在则创建新的
            if (CacheUtils.constainsKey(collectingHostsKey)) {
                @SuppressWarnings("unchecked")
                Set<String> cachedCollectingHosts = (Set<String>) CacheUtils.get(collectingHostsKey);
                collectingHosts = Collections.synchronizedSet(cachedCollectingHosts);
                log.debug("从缓存获取正在收集的主机列表，共{}台主机", collectingHosts.size());
            } else {
                collectingHosts = Collections.synchronizedSet(new HashSet<>());
                CacheUtils.put(collectingHostsKey, collectingHosts);
                log.debug("创建新的正在收集主机列表缓存");
            }
        } else {
            log.info("处理主机列表: 集群ID={}, 主机数量={}, 用户={}, 端口={}",
                    clusterId, hosts.split(Constants.COMMA).length, sshUser, sshPort);

            hostMap = processHostList(clusterId, hosts, sshPort, sshUser, sshPassword);

            // 将结果存入缓存
            CacheUtils.put(clusterId + Constants.HOST_MAP, hostMap);
            log.info("主机列表已存入缓存，共{}台主机", hostMap.size());

            // 创建新的已收集主机集合 - 使用线程安全集合
            collectedHosts = Collections.synchronizedSet(new HashSet<>());
            CacheUtils.put(collectedHostsKey, collectedHosts);
            log.debug("创建新的已收集主机列表缓存");

            // 创建新的正在收集主机集合 - 使用线程安全集合
            collectingHosts = Collections.synchronizedSet(new HashSet<>());
            CacheUtils.put(collectingHostsKey, collectingHosts);
            log.debug("创建新的正在收集主机列表缓存");
        }

        // 如果需要，触发当前分页主机的操作系统信息收集
        // 每10次请求只打印一次日志
        int currentCount = logCounter.incrementAndGet();
        if (currentCount % LOG_PRINT_INTERVAL == 1) {
            log.info("开始异步触发当前分页未收集主机的SSH验证和操作系统信息收集");
        }
        // 使用线程池进行主机信息收集，保证主接口立即返回
        osInfoExecutor.execute(() -> {
            try {
                // 使用与返回给前端相同的排序逻辑，确保一致性
                List<HostInfo> tempList = new ArrayList<>(hostMap.values());

                // 使用完全相同的排序代码，确保与前端展示的顺序完全一致
                List<String> sortedIps = HostUtils.sortIpAddresses(tempList.stream()
                        .map(HostInfo::getIp)
                        .collect(Collectors.toList()));

                List<HostInfo> allSortedHosts = new ArrayList<>();
                for (String ip : sortedIps) {
                    tempList.stream()
                            .filter(host -> ip.equals(host.getIp()))
                            .findFirst()
                            .ifPresent(allSortedHosts::add);
                }

                // 只处理当前页的主机
                List<HostInfo> sortedHosts;
                if (page != null && pageSize != null) {
                    int offset = (page - 1) * pageSize;
                    int end = Math.min(offset + pageSize, allSortedHosts.size());
                    // 确保参数有效
                    if (offset >= 0 && offset < allSortedHosts.size()) {
                        sortedHosts = allSortedHosts.subList(offset, end);
                        // 每10次请求只打印一次日志
                        if (currentCount % LOG_PRINT_INTERVAL == 1) {
                            log.info("检查当前页({}/{})的主机信息，范围: {}-{}, 共{}台主机",
                                    page, (int) Math.ceil(allSortedHosts.size() / (double) pageSize),
                                    offset + 1,
                                    end,
                                    sortedHosts.size());
                        }
                    } else {
                        // 参数无效，使用所有主机
                        sortedHosts = allSortedHosts;
                        log.warn("分页参数无效(offset={}, size={}), 将收集所有主机信息", offset, allSortedHosts.size());
                    }
                } else {
                    // 未提供分页参数，使用所有主机
                    sortedHosts = allSortedHosts;
                    log.info("未提供分页参数，将收集所有{}台主机的信息", sortedHosts.size());
                }

                // 过滤出未收集且当前不在收集过程中的主机
                List<HostInfo> pendingHosts = sortedHosts.stream()
                        .filter(host -> {
                            synchronized (collectedHosts) {
                                synchronized (collectingHosts) {
                                    return !collectedHosts.contains(host.getIp())
                                            && !collectingHosts.contains(host.getIp());
                                }
                            }
                        })
                        .toList();

                if (pendingHosts.isEmpty()) {
                    // 每10次请求只打印一次日志
                    if (currentCount % LOG_PRINT_INTERVAL == 1) {
                        log.info("当前页所有主机均已收集过信息或正在收集中，无需再次收集");
                    }
                    return;
                }

                log.info("当前页有{}台主机等待收集信息，开始收集: {}",
                        pendingHosts.size(),
                        pendingHosts.stream().map(HostInfo::getIp).collect(Collectors.joining(", ")));

                // 将所有待收集主机标记为"正在收集"状态
                synchronized (collectingHosts) {
                    for (HostInfo hostInfo : pendingHosts) {
                        collectingHosts.add(hostInfo.getIp());
                    }
                    // 更新缓存
                    CacheUtils.put(collectingHostsKey, collectingHosts);
                }

                // ==================== 第一阶段：并行收集所有主机的基本信息 ====================
                log.info("【第一阶段开始】首先为所有{}台主机并行收集基本信息（主机名和操作系统类型）", pendingHosts.size());

                // 用于跟踪第一阶段成功的主机
                List<HostInfo> firstPhaseSuccessHosts = Collections.synchronizedList(new ArrayList<>());

                // 创建一个并行任务列表，为每台主机创建一个独立的任务
                List<CompletableFuture<Void>> firstPhaseFutures = new ArrayList<>();

                // 并行为所有主机执行SSH验证和基本信息收集
                for (HostInfo hostInfo : pendingHosts) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        // 设置更有意义的线程名，包含主机IP
                        Thread thread = Thread.currentThread();
                        String threadOriginalName = thread.getName();
                        thread.setName("os-info-executor-" + hostInfo.getIp());

                        try {
                            log.info("开始为主机[{}]收集基本信息", hostInfo.getIp());
                            boolean sshSuccess = validateSshConnection(hostInfo);

                            // 如果SSH连接失败，设置相关错误状态并跳过后续操作
                            if (!sshSuccess) {
                                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                                hostInfo.setErrorMessage("SSH连接失败，无法获取主机信息");
                                hostInfo.setOsErrorMsg("由于SSH连接失败，无法获取操作系统信息");
                                hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                                hostInfo.setMessage("SSH连接失败：" + hostInfo.getSshErrorMsg());

                                // 即使失败也标记为已收集，避免反复尝试失败的主机
                                synchronized (collectedHosts) {
                                    collectedHosts.add(hostInfo.getIp());
                                    // 更新缓存
                                    CacheUtils.put(collectedHostsKey, collectedHosts);
                                }

                                // 从正在收集的列表中移除
                                synchronized (collectingHosts) {
                                    collectingHosts.remove(hostInfo.getIp());
                                    CacheUtils.put(collectingHostsKey, collectingHosts);
                                }

                                log.warn("主机[{}]SSH连接失败，标记为已处理", hostInfo.getIp());
                                return;
                            }

                            // SSH连接成功，开始收集操作系统信息第一阶段
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
                            hostInfo.setMessage("正在收集主机信息...");

                            try {
                                // 使用第一阶段收集方法
                                osInfoService.collectPhaseOneInfo(hostInfo);
                                log.info("主机[{}]的基本信息收集完成", hostInfo.getIp());

                                // 将成功的主机添加到列表，用于第二阶段处理
                                firstPhaseSuccessHosts.add(hostInfo);
                            } catch (Exception e) {
                                log.error("收集主机[{}]基本信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
                                // 异常情况仍然标记为已收集，避免重复尝试
                                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                                hostInfo.setOsErrorMsg("收集基本信息异常: " + e.getMessage());

                                // 即使失败也标记为已收集，避免反复尝试
                                synchronized (collectedHosts) {
                                    collectedHosts.add(hostInfo.getIp());
                                    // 更新缓存
                                    CacheUtils.put(collectedHostsKey, collectedHosts);
                                }

                                // 从正在收集的列表中移除
                                synchronized (collectingHosts) {
                                    collectingHosts.remove(hostInfo.getIp());
                                    CacheUtils.put(collectingHostsKey, collectingHosts);
                                }
                            }
                        } catch (Exception e) {
                            log.error("为主机[{}]执行基本信息收集失败: {}", hostInfo.getIp(), e.getMessage(), e);
                            // 设置错误状态和详细信息
                            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                            hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                            hostInfo.setSshErrorMsg("SSH连接异常: " + formatSshErrorMessage(e));
                            hostInfo.setErrorMessage("连接主机时发生异常");
                            hostInfo.setOsErrorMsg("由于连接异常，无法获取操作系统信息");
                            hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                            hostInfo.setMessage("SSH连接失败：" + formatSshErrorMessage(e));

                            // 即使失败也标记为已收集，避免反复尝试
                            synchronized (collectedHosts) {
                                collectedHosts.add(hostInfo.getIp());
                                // 更新缓存
                                CacheUtils.put(collectedHostsKey, collectedHosts);
                            }

                            // 从正在收集的列表中移除
                            synchronized (collectingHosts) {
                                collectingHosts.remove(hostInfo.getIp());
                                CacheUtils.put(collectingHostsKey, collectingHosts);
                            }
                        } finally {
                            // 恢复线程原始名称
                            thread.setName(threadOriginalName);
                        }
                    }, osInfoExecutor);

                    firstPhaseFutures.add(future);
                }

                // 等待所有第一阶段任务完成
                try {
                    CompletableFuture.allOf(firstPhaseFutures.toArray(new CompletableFuture[0])).get();
                } catch (Exception e) {
                    log.error("等待第一阶段任务完成时发生异常: {}", e.getMessage(), e);
                }

                log.info("【第一阶段完成】所有主机基本信息收集完毕，成功收集{}台主机的基本信息", firstPhaseSuccessHosts.size());

                // ==================== 第二阶段：并行收集详细信息 ====================
                if (!firstPhaseSuccessHosts.isEmpty()) {
                    log.info("【第二阶段开始】开始并行收集{}台主机的详细硬件信息", firstPhaseSuccessHosts.size());

                    // 创建第二阶段任务列表
                    List<CompletableFuture<Void>> secondPhaseFutures = new ArrayList<>();

                    // 并行为第一阶段成功的主机收集详细信息
                    for (HostInfo hostInfo : firstPhaseSuccessHosts) {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            // 设置更有意义的线程名，包含主机IP
                            Thread thread = Thread.currentThread();
                            String threadOriginalName = thread.getName();
                            thread.setName("hardware-info-executor-" + hostInfo.getIp());

                            try {
                                log.info("开始为主机[{}]收集详细硬件信息", hostInfo.getIp());

                                // 使用第二阶段收集方法
                                osInfoService.collectPhaseTwoInfo(hostInfo);
                                log.info("主机[{}]的详细信息收集完成", hostInfo.getIp());

                                // 标记为已完全收集
                                synchronized (collectedHosts) {
                                    collectedHosts.add(hostInfo.getIp());
                                    // 更新缓存
                                    CacheUtils.put(collectedHostsKey, collectedHosts);
                                }

                                // 从正在收集的列表中移除
                                synchronized (collectingHosts) {
                                    collectingHosts.remove(hostInfo.getIp());
                                    CacheUtils.put(collectingHostsKey, collectingHosts);
                                }

                                log.info("主机[{}]所有信息收集完成，已更新收集状态", hostInfo.getIp());
                            } catch (Exception e) {
                                log.error("收集主机[{}]详细信息时发生异常: {}", hostInfo.getIp(), e.getMessage());
                                // 即使第二阶段失败，也标记为已收集，因为基本信息已经收集完成
                                synchronized (collectedHosts) {
                                    collectedHosts.add(hostInfo.getIp());
                                    // 更新缓存
                                    CacheUtils.put(collectedHostsKey, collectedHosts);
                                }

                                // 从正在收集的列表中移除
                                synchronized (collectingHosts) {
                                    collectingHosts.remove(hostInfo.getIp());
                                    CacheUtils.put(collectingHostsKey, collectingHosts);
                                }

                                log.warn("主机[{}]详细信息收集失败，但基本信息已收集完成", hostInfo.getIp());
                            } finally {
                                // 恢复线程原始名称
                                thread.setName(threadOriginalName);
                            }
                        }, hardwareInfoExecutor);

                        secondPhaseFutures.add(future);
                    }

                    // 等待所有第二阶段任务完成
                    try {
                        CompletableFuture.allOf(secondPhaseFutures.toArray(new CompletableFuture[0])).get();
                    } catch (Exception e) {
                        log.error("等待第二阶段任务完成时发生异常: {}", e.getMessage(), e);
                    }

                    log.info("【第二阶段完成】所有主机详细信息收集完毕");
                } else {
                    log.info("【第二阶段跳过】没有主机成功通过第一阶段，跳过详细信息收集");
                }

                log.info("当前页所有主机的信息收集任务已全部完成，共处理{}台主机", pendingHosts.size());
            } catch (Exception e) {
                log.error("主机信息收集线程异常: {}", e.getMessage(), e);
            }
        });

        return hostMap;
    }

    /**
     * 格式化SSH错误消息，使其更用户友好
     */
    private String formatSshErrorMessage(Exception e) {
        String errorMsg = e.getMessage();
        if (errorMsg == null) {
            errorMsg = e.getClass().getSimpleName();
        }

        // 根据异常类型和错误消息设置友好的错误提示
        String friendlyMessage;
        String errorCode;
        String solution;

        if (errorMsg.contains("Auth fail") || errorMsg.contains("authentication failed")) {
            friendlyMessage = "用户名或密码错误";
            errorCode = "SSH_AUTH_ERROR";
            solution = "请检查SSH用户名和密码是否正确";
        } else if (errorMsg.contains("Connection refused")) {
            friendlyMessage = "SSH服务未启动或端口未开放";
            errorCode = "SSH_CONNECTION_REFUSED";
            solution = "请确认SSH服务已启动，端口(默认22)已开放，并检查防火墙设置";
        } else if (errorMsg.contains("connect timed out")) {
            friendlyMessage = "连接超时，网络不通或防火墙阻止";
            errorCode = "SSH_TIMEOUT";
            solution = "请检查网络连接和防火墙设置，确保主机可访问";
        } else if (errorMsg.contains("UnknownHostException")) {
            friendlyMessage = "无法解析主机名";
            errorCode = "SSH_UNKNOWN_HOST";
            solution = "请检查DNS配置或hosts文件，确保主机名可解析";
        } else if (errorMsg.contains("No route to host")) {
            friendlyMessage = "无法访问主机";
            errorCode = "SSH_NO_ROUTE";
            solution = "请检查网络连接，确保主机已启动且网络可达";
        } else if (errorMsg.contains("Too many authentication failures")) {
            friendlyMessage = "认证失败次数过多";
            errorCode = "SSH_TOO_MANY_AUTH_FAILURES";
            solution = "请等待一段时间后重试，或尝试使用密钥认证";
        } else if (errorMsg.contains("Permission denied")) {
            friendlyMessage = "权限被拒绝";
            errorCode = "SSH_PERMISSION_DENIED";
            solution = "请检查用户权限或尝试使用sudo权限的用户";
        } else if (errorMsg.contains("Host key verification failed")) {
            friendlyMessage = "主机密钥验证失败";
            errorCode = "SSH_HOST_KEY_VERIFICATION_FAILED";
            solution = "主机密钥已更改，请更新known_hosts文件";
        } else {
            friendlyMessage = errorMsg;
            errorCode = "SSH_UNKNOWN_ERROR";
            solution = "请检查SSH服务配置和网络连接";
        }

        // 返回完整原始错误信息，便于调试
        String originalError = errorMsg;

        // 构建结构化的错误信息
        return String.format("%s [%s] - %s (原始错误: %s)",
                friendlyMessage,
                errorCode,
                solution,
                originalError);
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
            // getOrCreateSession内部会调用openConnectionWithPassword
            // openConnectionWithPassword如果异常会设置hostInfo的错误信息
            session = getOrCreateSession(hostInfo);

            // 如果session为null，表示连接失败
            // 错误信息已经在openConnectionWithPassword中设置到hostInfo
            if (session == null) {
                // 确保SSH连接状态为ERROR
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);

                // 如果没有错误信息，则设置一个默认值
                if (StringUtils.isBlank(hostInfo.getSshErrorMsg())) {
                    hostInfo.setSshErrorMsg("无法创建SSH连接，请检查网络连接和SSH配置");
                }

                if (StringUtils.isBlank(hostInfo.getErrorMessage())) {
                    hostInfo.setErrorMessage("SSH连接失败：无法创建连接");
                }

                return false;
            }

            // 连接成功，执行测试命令
            CommandResult connectionTestResult = MinaUtils.execCmdWithResultObject(session,
                    "echo connection_test");
            String result = connectionTestResult.isSuccess() ? connectionTestResult.getOutput()
                    : "EXIT_CODE_" + connectionTestResult.getExitCode() + ": " + connectionTestResult.getError();

            boolean success = result != null && result.contains("connection_test");

            // 连接成功
            if (success) {
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
                return true;
            } else {
                // 命令执行失败
                hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setSshErrorMsg("SSH连接成功但无法执行命令，请检查用户权限");
                hostInfo.setErrorMessage("SSH连接成功但无法执行命令，请检查用户权限");
                return false;
            }
        } catch (Exception e) {
            // 处理不同类型的异常，设置更友好的错误信息
            hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);

            String formattedErrorMsg = formatSshErrorMessage(e);
            hostInfo.setSshErrorMsg(formattedErrorMsg);
            hostInfo.setErrorMessage("SSH连接失败：" + formattedErrorMsg);

            log.error("主机[{}]SSH连接验证失败: {}", hostInfo.getIp(), formattedErrorMsg, e);
            return false;
        } finally {
            // 安全关闭会话，避免关闭异常影响验证结果
            if (session != null) {
                try {
                    MinaUtils.closeConnection(session);
                } catch (Exception e) {
                    // 仅记录关闭连接时的异常，不影响验证结果
                    log.warn("关闭主机[{}]的SSH连接时发生异常: {}", hostInfo.getIp(), e.getMessage());
                }
            }
        }
    }

    /**
     * 处理主机列表
     *
     * @param clusterId   集群ID
     * @param hosts       主机列表字符串
     * @param sshPort     SSH端口
     * @param sshUser     SSH用户名
     * @param sshPassword SSH密码
     * @return 主机信息映射，IP为键
     */
    private Map<String, HostInfo> processHostList(Integer clusterId, String hosts, Integer sshPort,
            String sshUser, String sshPassword) {
        HashMap<String, HostInfo> hostInfoMap = new HashMap<>();

        log.info("解析主机列表");
        String[] hostArray = hosts.split(Constants.COMMA);

        // 2. 遍历处理每个主机，将结果存入临时Map
        Map<String, HostInfo> tempMap = new HashMap<>();
        for (String host : hostArray) {
            // 只创建主机信息，不执行异步获取操作系统信息
            processHostWithoutOsInfo(host, sshPort, sshUser, sshPassword, clusterId, tempMap);
        }

        // List<CheckItem> checkItems = hostCheckService.getHostCheckItems();
        List<CheckItem> checkItems = null;
        // 3. 将所有主机信息添加到返回结果中，以IP为键
        for (HostInfo hostInfo : tempMap.values()) {
            // 获取主机检查项列表
            hostInfo.setCheckItems(checkItems);

            // 初始化SSH连接状态为loading，而不是同步验证
            hostInfo.setSshConnectStatus(OsInfoStatusEnum.LOADING);

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
    private boolean isCacheValid(Integer clusterId) {
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

        hostInfo.setHostname(HostUtils.getHostName(host));
        hostInfo.setIp(HostUtils.getIpByHost(host));
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
        log.info("创建主机 {} 的新SSH连接", ip);
        ClientSession newSession = MinaUtils.openConnectionWithPassword(hostInfo);
        if (newSession != null) {
            // 将新会话添加到Map中
            log.info("成功创建主机 {} 的SSH连接", ip);
        } else {
            log.warn("无法创建主机 {} 的SSH连接", ip);
        }
        return newSession;
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
    public HostCheckStatusDto getHostCheckStatus(Integer clusterId, String sshUser, Integer sshPort) {
        try {
            // 获取检查结果列表
            Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
            List<HostInfo> list = map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .toList();

            // 统计检查状态
            int totalHosts = list.size();
            int completedHosts = 0;
            int failedHosts = 0;
            int successHosts = 0;
            boolean allCompleted = true;

            for (HostInfo hostInfo : list) {
                CheckItem.Status status = hostInfo.getStatus();
                if (status == CheckItem.Status.SUCCESS) {
                    completedHosts++;
                    successHosts++;
                } else if (status == CheckItem.Status.FAILED) {
                    completedHosts++;
                    failedHosts++;
                } else if (status == CheckItem.Status.CHECKING || status == CheckItem.Status.WAITING) {
                    allCompleted = false;
                }
            }

            return HostCheckStatusDto.builder()
                    .hosts(list)
                    .completed(allCompleted)
                    .totalHosts(totalHosts)
                    .completedHosts(completedHosts)
                    .failedHosts(failedHosts)
                    .successHosts(successHosts)
                    .build();
        } catch (Exception e) {
            log.error("获取主机检查状态失败", e);
            throw new ServiceException("获取主机检查状态失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<HostInfo> dispatcherHostAgentList(
            Integer clusterId, Integer installStateCode, Integer page, Integer pageSize) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            String clusterCode = clusterInfo.getClusterCode();
            String distributeAgentKey = clusterCode + Constants.UNDERLINE + Constants.START_DISTRIBUTE_AGENT;
            @SuppressWarnings("unchecked")
            Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
            List<HostInfo> list = map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .filter(e -> e.getCheckResult().getCode() == 10001)
                    .toList();

            for (HostInfo hostInfo : list) {
                if (hostInfo.isManaged()) {
                    hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.success"));
                    hostInfo.setInstallState(InstallState.SUCCESS);
                } else if (!CacheUtils.constainsKey(distributeAgentKey + Constants.UNDERLINE + hostInfo.getIp())) {
                    log.info("start to dispatcher host agent to {}", hostInfo.getIp());
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
            return PageResult.of(result, list.size(), page, pageSize);
        } catch (Exception e) {
            log.error("获取主机代理分发列表失败", e);
            throw new ServiceException("获取主机代理分发列表失败: " + e.getMessage());
        }
    }

    @Override
    public boolean reStartDispatcherHostAgent(Integer clusterId, String ips) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

            for (String ip : ips.split(",")) {
                // 不使用不存在的getClusterHostByIp方法
                HostInfo hostInfo = new HostInfo();
                boolean foundInMap = false;

                // 在缓存map中查找匹配IP的主机信息
                for (Map.Entry<String, HostInfo> entry : map.entrySet()) {
                    HostInfo hi = entry.getValue();
                    if (hi != null && ip.equals(hi.getIp())) {
                        hostInfo = hi;
                        foundInMap = true;
                        break;
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
            return true;
        } catch (Exception e) {
            log.error("重启主机代理分发失败", e);
            throw new BusinessException(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), "重启主机代理分发失败: " + e.getMessage());
        }
    }

    @Override
    public boolean hostCheckCompleted(Integer clusterId) {
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

        // 收集未通过检查的主机信息
        List<Map<String, Object>> failedHosts = new ArrayList<>();
        Map<String, List<String>> hostToFailedItems = new HashMap<>();

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

                log.info("主机 {} 的整体检查状态未完成，主机检查不通过", hostInfo.getIp());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean cleanupHostCheckResources(Integer clusterId) {
        try {
            if (clusterId == null) {
                log.error("集群ID为空，无法清理主机检查资源");
                throw new BusinessException(Status.REQUEST_PARAMS_NOT_VALID_ERROR.getCode(), "集群ID不能为空");
            }

            log.info("开始清理集群[{}]的主机检查资源", clusterId);

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
                log.info("清理前缀为[{}]的日志缓存", prefix);
                // 注意：这里无法遍历所有键，所以我们只能在后续操作中处理相关键
            }

            // 通用日志缓存键
            String logCacheKey = clusterId + "_HOST_CHECK_LOG";
            if (CacheUtils.constainsKey(logCacheKey)) {
                CacheUtils.removeKey(logCacheKey);
                log.info("已清理集群[{}]的主机检查日志缓存", clusterId);
            }

            // 2. 清理其他与检查相关的缓存
            String hostMapKey = clusterId + Constants.HOST_MAP;
            if (CacheUtils.constainsKey(hostMapKey)) {
                // 在清理前，获取所有主机信息，用于清理特定主机的日志
                Map<String, HostInfo> hostMap = CacheUtils.getHostMap(hostMapKey);
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
                                    log.debug("已清理日志: {}", itemLogKey);
                                }
                            }
                        }
                    }
                }
            }
        } catch (BusinessException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public boolean cancelDispatcherHostAgent(Integer clusterId, String ip, Integer installStateCode) {
        // 此方法虽然定义但实际未使用
        log.warn("cancelDispatcherHostAgent方法暂未实现具体逻辑");
        return false;
    }

    @Override
    public boolean dispatcherHostAgentCompleted(Integer clusterId) {
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo hostInfo = hostInfoEntry.getValue();
            if (hostInfo.getProgress() == 75
                    && DateUtil.between(hostInfo.getCreateTime(), new Date(), DateUnit.MINUTE) > 1) {
                log.info("dispatcher host agent timeout");
                hostInfo.setInstallState(InstallState.FAILED);
                hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                hostInfo.setErrMsg("dispatcher host agent timeout");
            }
            if (hostInfo.getInstallState() != InstallState.SUCCESS) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Map<String, Object>> generateHostAgentCommand(String clusterHostIds, String commandType) {
        if (StringUtils.isBlank(clusterHostIds)) {
            throw new BusinessException(Status.SELECT_LEAST_ONE_HOST.getCode(), Status.SELECT_LEAST_ONE_HOST.getMsg());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        String[] clusterHostIdArray = clusterHostIds.split(Constants.COMMA);
        List<String> clusterHostIdList = Arrays.asList(clusterHostIdArray);
        List<ClusterHostDO> clusterHostList = hostService.getHostListByIds(clusterHostIdList);

        for (ClusterHostDO clusterHostDO : clusterHostList) {
            Map<String, Object> result = new HashMap<>();
            result.put("hostname", clusterHostDO.getHostname());
            result.put("ip", clusterHostDO.getIp());
            result.put("command", "service datasophon-worker " + commandType);

            ClientSession session = null;
            try {
                session = MinaUtils.openConnectionWithPassword(new HostInfo(clusterHostDO.getIp(), 22, Constants.ROOT));
                String commandResult = MinaUtils.execCmdWithResult(session, "service datasophon-worker " + commandType);
                result.put("success", true);
                result.put("output", commandResult);
                log.info("hostAgent command executed successfully on {}: {}", clusterHostDO.getIp(), commandResult);
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
                log.error("Failed to execute hostAgent command on {}: {}", clusterHostDO.getIp(), e.getMessage());
            } finally {
                if (session != null) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.warn("Failed to close SSH session for {}: {}", clusterHostDO.getIp(), e.getMessage());
                    }
                }
            }
            results.add(result);
        }
        return results;
    }

    /**
     * 一键 启动 主机上安装的服务
     *
     */
    @Override
    public List<Map<String, Object>> generateHostServiceCommand(String clusterHostIds, String commandType) {
        if (StringUtils.isBlank(clusterHostIds)) {
            throw new BusinessException(Status.SELECT_LEAST_ONE_HOST.getCode(), Status.SELECT_LEAST_ONE_HOST.getMsg());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        String[] clusterHostIdArray = clusterHostIds.split(Constants.COMMA);
        List<ClusterHostDO> clusterHostList = hostService.getHostListByIds(Arrays.asList(clusterHostIdArray));

        CommandType serviceCommandType = "start".equalsIgnoreCase(commandType) ? CommandType.START_SERVICE
                : CommandType.STOP_SERVICE;

        for (ClusterHostDO clusterHostDO : clusterHostList) {
            Map<String, Object> result = new HashMap<>();
            result.put("hostname", clusterHostDO.getHostname());
            result.put("ip", clusterHostDO.getIp());
            result.put("commandType", serviceCommandType.toString());

            WorkerServiceMessage serviceMessage = new WorkerServiceMessage(clusterHostDO.getHostname(),
                    clusterHostDO.getClusterId(), serviceCommandType);
            try {
                ActorRef actor = ActorUtils.getLocalActor(WorkerStartActor.class, "workerStartActor");
                actor.tell(serviceMessage, ActorRef.noSender());
                result.put("success", true);
                result.put("message", "服务命令已发送");
                log.info("Service command sent successfully to {}: {}", clusterHostDO.getHostname(),
                        serviceCommandType);
            } catch (Exception e) {
                log.error("launcher worker service error!", e);
                result.put("success", false);
                result.put("error", "启动服务异常，Cause: " + e.getMessage());
            }
            results.add(result);
        }
        return results;
    }

    /**
     * Kubernetes集群模式的主机列表解析
     * 从K8S API获取节点信息，包括CPU架构
     */
    private PageResult<HostInfo> analysisHostListForKubernetes(Integer clusterId, String kubeConfig, Integer page,
            Integer pageSize) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

            if (kubeConfig == null || kubeConfig.trim().isEmpty()) {
                throw new ServiceException("Kubernetes配置不能为空，请先完成集群配置");
            }

            log.info("开始从Kubernetes API获取节点列表，集群ID: {}", clusterId);

            // 从Kubernetes集群获取节点列表
            List<K8sNodeInfo> k8sNodeInfoList = KubeUtil.getHostListByConfig(kubeConfig);
            List<ClusterHostDO> kubernetesHosts = k8sToClusterHostConverter.convertToClusterHostList(k8sNodeInfoList,
                    clusterId);

            if (kubernetesHosts.isEmpty()) {
                log.warn("未从Kubernetes集群获取到任何节点");
                throw new ServiceException("未找到任何Kubernetes节点，请检查集群配置");
            }

            log.info("从Kubernetes API获取到 {} 个节点", kubernetesHosts.size());

            // 转换为HostInfo格式
            List<HostInfo> hostInfoList = new ArrayList<>();
            String clusterCode = clusterInfo.getClusterCode();

            // 获取检查项列表（K8S模式下也需要检查项用于环境验证）
            // List<CheckItem> checkItems = hostCheckService.getHostCheckItems();
            List<CheckItem> checkItems = null;
            // 保存从K8S API获取的完整节点信息，用于后续保存
            List<ClusterHostDO> kubernetesHostsForSave = new ArrayList<>();

            for (ClusterHostDO kubernetesHost : kubernetesHosts) {
                HostInfo hostInfo = new HostInfo();
                hostInfo.setHostname(kubernetesHost.getHostname());
                hostInfo.setIp(kubernetesHost.getIp());
                hostInfo.setSshPort(22); // Kubernetes模式默认SSH端口
                hostInfo.setSshUser("root"); // Kubernetes模式默认用户
                hostInfo.setClusterCode(clusterCode);
                hostInfo.setClusterId(clusterId);
                hostInfo.setCreateTime(new Date());

                // 重要：从Kubernetes API获取的CPU架构信息
                String cpuArchitecture = kubernetesHost.getCpuArchitecture();
                hostInfo.setCpuArchitecture(cpuArchitecture);

                log.info("节点 {} (IP: {}) 的完整信息: 核心数={}, 总内存={}GB, 总磁盘={}GB, 架构={}",
                        kubernetesHost.getHostname(), kubernetesHost.getIp(),
                        kubernetesHost.getCoreNum(), kubernetesHost.getTotalMem(),
                        kubernetesHost.getTotalDisk(), cpuArchitecture);

                // 设置检查项列表
                hostInfo.setCheckItems(checkItems);

                // Kubernetes模式下检查主机受管状态
                ClusterHostDO existingHost = hostService.getClusterHostByHostname(kubernetesHost.getHostname());

                if (existingHost != null && existingHost.getClusterId().equals(clusterId)) {
                    // 主机已在当前集群中受管 - 重复添加
                    hostInfo.setManaged(true);
                    hostInfo.setInstallState(InstallState.FAILED);
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setCheckResult(
                            new CheckResult(Status.CONNECTION_FAILED.getCode(), "主机已在当前集群中受管，请勿重复添加"));
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("主机已受管");
                    log.info("Host {} is already managed in current Kubernetes cluster {}",
                            kubernetesHost.getHostname(), clusterId);
                } else if (existingHost != null) {
                    // 主机已在其他集群中受管
                    hostInfo.setManaged(true);
                    hostInfo.setInstallState(InstallState.FAILED);
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setCheckResult(
                            new CheckResult(Status.CONNECTION_FAILED.getCode(), "主机已在其他集群中受管"));
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("主机已在其他集群受管");
                    log.info("Host {} is already managed in another cluster {}",
                            kubernetesHost.getHostname(), existingHost.getClusterId());
                } else {
                    // 主机未受管，Kubernetes模式下校验成功，可以添加
                    hostInfo.setManaged(false);
                    hostInfo.setInstallState(InstallState.SUCCESS);
                    hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setCheckResult(
                            new CheckResult(Status.CHECK_HOST_SUCCESS.getCode(), Status.CHECK_HOST_SUCCESS.getMsg()));
                    hostInfo.setSshConnectStatus(OsInfoStatusEnum.SUCCESS);
                    hostInfo.setMessage("K8S节点验证成功，可以添加");
                    log.info("Host {} is not managed in Kubernetes mode, can be added",
                            kubernetesHost.getHostname());

                    // 将可以添加的主机信息保存到列表中，用于后续保存
                    kubernetesHostsForSave.add(kubernetesHost);
                }

                // 初始化检查项状态（K8S模式下某些检查项可能不适用）
                if (hostInfo.getCheckItems() != null) {
                    for (CheckItem checkItem : hostInfo.getCheckItems()) {
                        // K8S模式下，某些检查项设置为成功（如SSH连接等）
                        if ("SSH连接检查".equals(checkItem.getItemName()) ||
                                "主机名检查".equals(checkItem.getItemName()) ||
                                "操作系统检查".equals(checkItem.getItemName())) {
                            checkItem.setStatus(CheckItem.Status.SUCCESS);
                            checkItem.setMessage("K8S模式自动通过");
                        }
                    }
                }

                hostInfoList.add(hostInfo);
            }

            // 缓存K8S主机信息，用于后续保存
            CacheUtils.put(clusterId + "_K8S_HOSTS_FOR_SAVE", kubernetesHostsForSave);

            // 使用HostUtils的统一排序方法对IP进行排序
            List<String> sortedIps = HostUtils.sortIpAddresses(hostInfoList.stream()
                    .map(HostInfo::getIp)
                    .collect(Collectors.toList()));

            // 按照排序后的IP顺序重新组织主机列表
            List<HostInfo> sortedHostList = new ArrayList<>();
            for (String ip : sortedIps) {
                hostInfoList.stream()
                        .filter(host -> ip.equals(host.getIp()))
                        .findFirst()
                        .ifPresent(sortedHostList::add);
            }

            // 缓存主机列表
            HashMap<String, HostInfo> hostMap = new HashMap<>();
            for (HostInfo hostInfo : sortedHostList) {
                hostMap.put(hostInfo.getIp(), hostInfo);
            }
            CacheUtils.put(clusterId + Constants.HOST_MAP, hostMap);

            log.info("已缓存K8S集群主机列表，共{}台主机", hostMap.size());
            log.info("已缓存K8S完整硬件信息，共{}台主机", kubernetesHostsForSave.size());

            // 分页处理
            int offset = (page - 1) * pageSize;
            int end = Math.min(offset + pageSize, sortedHostList.size());
            List<HostInfo> pagedResult = sortedHostList.subList(offset, end);

            return PageResult.of(pagedResult, sortedHostList.size(), page, pageSize);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取Kubernetes节点列表失败", e);
            throw new ServiceException("获取Kubernetes节点列表失败: " + e.getMessage());
        }
    }

    private List<HostInfo> getListPage(List<HostInfo> list, Integer offset, Integer pageSize) {
        List<HostInfo> result = new ArrayList<>();
        int limit = offset + pageSize;
        if (list.size() < offset + pageSize) {
            limit = list.size();
        }
        for (int i = offset; i < limit; i++) {
            result.add(list.get(i));
        }
        return result;
    }

    @Override
    public boolean clearHostEnvCheckCache() {
        try {
            log.debug("删除主机检查项缓存");
            CacheUtils.clear();
            log.info("主机环境校验缓存清理完成");
            return true;
        } catch (Exception e) {
            log.error("清理主机环境校验缓存失败", e);
            throw new ServiceException("清理主机环境校验缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取主机最近日志
     *
     * @param ip        主机IP
     * @param clusterId 集群ID
     * @return 主机最近日志内容
     */
    @Override
    public String getWorkerLog(String ip, Integer clusterId) {
        try {
            // 1. 从缓存中获取主机信息
            Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

            // 2. 查找指定IP的主机信息
            HostInfo hostInfo = hostMap.get(ip);
            if (hostInfo == null) {
                throw new BusinessException(Status.IP_IS_EMPTY.getCode(), "未找到主机信息");
            }

            // 3. 建立SSH连接
            ClientSession session = MinaUtils.openConnectionWithPassword(hostInfo);
            if (session == null) {
                throw new ServiceException("SSH连接失败: " + hostInfo.getSshErrorMsg());
            }

            try {
                // 4. 执行tail命令查看运行日志
                String command = "tail -n 100 /opt/datasophon/datasophon-worker/logs/datasophon-worker.log";

                // 5. 返回日志内容
                return MinaUtils.execCmdWithResult(session, command);
            } finally {
                // 6. 关闭SSH连接
                MinaUtils.closeConnection(session);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取主机日志失败", e);
            throw new ServiceException("获取主机日志失败: " + e.getMessage());
        }
    }
}
