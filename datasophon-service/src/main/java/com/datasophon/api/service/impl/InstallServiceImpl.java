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

import java.time.temporal.ChronoUnit;

import com.datasophon.common.enums.Status;
import com.datasophon.api.converter.InstallStepConverter;
import com.datasophon.api.service.AgentDistributionService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.InstallService;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.dto.InstallStepDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.mapper.InstallStepMapper;

import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.MessageResolverUtils;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.dto.HostCheckStatusDto;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.exception.ServiceException;
import com.datasophon.common.exception.BusinessException;
import com.datasophon.common.model.HostInfo;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import com.datasophon.common.model.WorkerServiceMessage;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.InstallStepEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.*;

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

    // 依赖注入 - 使用构造器注入
    @Autowired
    private  ClusterInfoService clusterInfoService;
    @Autowired
    private  ClusterHostService hostService;
    @Autowired
    private  AgentDistributionService agentDistributionService;
    // SSH连接服务（延迟初始化，避免Spring上下文未初始化问题）
    private SshConnectionService sshService;
    
    /**
     * 获取SSH连接服务（延迟加载）
     */
    private SshConnectionService getSshService() {
        if (sshService == null) {
            sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
        }
        return sshService;
    }
    @Autowired
    private  InstallStepConverter installStepConverter;

    /**
     * 构建SSH检查上下文
     */
    private HostCheckContext buildHostCheckContext(HostInfo hostInfo) {
        return HostCheckContext.builder()
                .hostIp(hostInfo.getIp())
                .sshPort(hostInfo.getSshPort())
                .sshUser(hostInfo.getSshUser())
                .sshPassword(hostInfo.getSshPassword())
                .build();
    }

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
    public InstallStepDTO getInstallStep(Integer type) {
        try {
            if (type == null) {
                throw new RuntimeException("安装类型不能为空");
            }

            List<InstallStepDTO> steps = getInstallStepsByType(type);
            if (steps == null || steps.isEmpty()) {
                throw new RuntimeException("未找到类型为 " + type + " 的安装步骤");
            }

            // 返回第一个匹配的安装步骤
            return steps.getFirst();
        } catch (Exception e) {
            log.error("根据类型获取安装步骤失败: {}", e.getMessage(), e);
            throw new RuntimeException("根据类型获取安装步骤失败: " + e.getMessage());
        }
    }

    @Override
    public HostCheckStatusDto getHostCheckStatus(Long clusterId, String sshUser, Integer sshPort) {
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
            Long clusterId, Integer installStateCode, Integer page, Integer pageSize) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            String clusterCode = clusterInfo.getClusterCode();
            String distributeAgentKey = clusterCode + Constants.UNDERLINE + Constants.START_DISTRIBUTE_AGENT;
            @SuppressWarnings("unchecked")
            Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
            List<HostInfo> list = map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .filter(e -> e.getCommonResult().getCode() == 10001)
                    .toList();

            // 获取当前Agent分发状态
            var distributionStatus = agentDistributionService.getDistributionStatus(clusterId);
            Map<String, com.datasophon.common.vo.agent.AgentDistributionStatusVO> statusMap = 
                    distributionStatus.stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    com.datasophon.common.vo.agent.AgentDistributionStatusVO::getHostIp, 
                                    s -> s));

            for (HostInfo hostInfo : list) {
                if (hostInfo.isManaged()) {
                    // 已托管的主机直接标记为成功
                    hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.success"));
                    hostInfo.setInstallState(InstallState.SUCCESS);
                } else {
                    // 从新架构获取分发状态
                    var status = statusMap.get(hostInfo.getIp());
                    if (status != null) {
                        // 映射新架构的状态到旧的InstallState
                        switch (status.getStatus()) {
                            case SUCCESS:
                                hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                                hostInfo.setInstallState(InstallState.SUCCESS);
                                hostInfo.setProgress(100);
                                hostInfo.setMessage("Agent安装成功");
                                break;
                            case FAILED:
                                hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                                hostInfo.setInstallState(InstallState.FAILED);
                                hostInfo.setProgress(100);
                                hostInfo.setMessage(status.getMessage());
                                break;
                            case RUNNING:
                                hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
                                hostInfo.setInstallState(InstallState.RUNNING);
                                hostInfo.setProgress(status.getProgress());
                                hostInfo.setMessage("正在分发Agent...");
                                break;
                            default:
                                hostInfo.setInstallStateCode(InstallState.WAITING.getValue());
                                hostInfo.setInstallState(InstallState.WAITING);
                                hostInfo.setProgress(0);
                                hostInfo.setMessage("等待分发");
                                break;
                        }
                    } else if (!CacheUtils.constainsKey(distributeAgentKey + Constants.UNDERLINE + hostInfo.getIp())) {
                        // 如果没有状态且未启动过，则自动启动分发（保持原有逻辑）
                        log.info("自动启动Agent分发: {}", hostInfo.getIp());
                        try {
                            // 准备连接参数
                            Map<String, Object> connectionParams = new java.util.HashMap<>();
                            connectionParams.put("sshUser", hostInfo.getSshUser());
                            connectionParams.put("sshPort", hostInfo.getSshPort());
                            connectionParams.put("sshPassword", hostInfo.getSshPassword());
                            
                            Map<String, String> hostnames = new java.util.HashMap<>();
                            hostnames.put(hostInfo.getIp(), hostInfo.getHostname());
                            connectionParams.put("hostnames", hostnames);
                            
                            // 调用新的分发服务
                            agentDistributionService.startDistribution(
                                    clusterId, 
                                    java.util.Collections.singletonList(hostInfo.getIp()), 
                                    connectionParams);
                            
                            hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
                            hostInfo.setInstallState(InstallState.RUNNING);
                            hostInfo.setCreateTime(LocalDateTime.now());
                            
                            // 标记已启动
                            CacheUtils.put(distributeAgentKey + Constants.UNDERLINE + hostInfo.getIp(), true);
                        } catch (Exception e) {
                            log.error("启动Agent分发失败: {}", hostInfo.getIp(), e);
                            hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                            hostInfo.setInstallState(InstallState.FAILED);
                            hostInfo.setMessage("启动分发失败: " + e.getMessage());
                        }
                    } else {
                        // 已启动但无状态信息（可能正在初始化）
                        hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
                        hostInfo.setInstallState(InstallState.RUNNING);
                        hostInfo.setProgress(0);
                        hostInfo.setMessage("正在初始化...");
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
    public boolean reStartDispatcherHostAgent(Long clusterId, String ips) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
            
            List<String> ipList = java.util.Arrays.asList(ips.split(","));
            Map<String, Object> connectionParams = new java.util.HashMap<>();
            Map<String, String> hostnames = new java.util.HashMap<>();
            
            // 准备连接参数
            for (String ip : ipList) {
                HostInfo hostInfo = null;
                
                // 在缓存map中查找匹配IP的主机信息
                for (Map.Entry<String, HostInfo> entry : map.entrySet()) {
                    HostInfo hi = entry.getValue();
                    if (hi != null && ip.trim().equals(hi.getIp())) {
                        hostInfo = hi;
                        break;
                    }
                }
                
                if (hostInfo != null) {
                    // 使用找到的主机信息
                    connectionParams.put("sshUser", hostInfo.getSshUser());
                    connectionParams.put("sshPort", hostInfo.getSshPort());
                    connectionParams.put("sshPassword", hostInfo.getSshPassword());
                    hostnames.put(ip.trim(), hostInfo.getHostname());
                } else {
                    // 使用默认值
                    log.warn("主机 {} 未在缓存中找到，使用默认SSH配置", ip);
                    connectionParams.put("sshUser", "root");
                    connectionParams.put("sshPort", 22);
                    hostnames.put(ip.trim(), ip.trim());
                }
            }
            
            connectionParams.put("hostnames", hostnames);
            
            // 调用新的Agent分发服务
            log.info("重启Agent分发: 集群={}, 主机={}", clusterId, ips);
            agentDistributionService.startDistribution(clusterId, ipList, connectionParams);
            
            return true;
        } catch (Exception e) {
            log.error("重启主机代理分发失败: 集群={}, 主机={}", clusterId, ips, e);
            throw new BusinessException(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), 
                    "重启主机代理分发失败: " + e.getMessage());
        }
    }

    @Override
    public boolean dispatcherHostAgentCompleted(Long clusterId) {
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo hostInfo = hostInfoEntry.getValue();
            if (hostInfo.getProgress() == 75
                    && ChronoUnit.MINUTES.between(hostInfo.getCreateTime(), LocalDateTime.now()) > 1) {
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
        List<ClusterHostEntity> clusterHostList = hostService.getHostListByIds(clusterHostIdList);

        for (ClusterHostEntity clusterHostEntity : clusterHostList) {
            Map<String, Object> result = new HashMap<>();
            result.put("hostname", clusterHostEntity.getHostname());
            result.put("ip", clusterHostEntity.getIp());
            result.put("command", "service datasophon-worker " + commandType);

            try {
                // 通过SSH插件适配器执行命令
                HostInfo hostInfo = new HostInfo(clusterHostEntity.getIp(), 22, Constants.ROOT);
                String command = "service datasophon-worker " + commandType;
                
                HostCheckContext context = buildHostCheckContext(hostInfo);
                CommandResult cmdResult = getSshService().executeCommand(context, command);
                String commandResult = cmdResult.isSuccess() ? cmdResult.output() : "";
                result.put("success", true);
                result.put("output", commandResult);
                log.info("【安装服务】主机代理命令执行成功: {} -> {}", clusterHostEntity.getIp(), commandResult);
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
                log.error("【安装服务】主机代理命令执行失败: {} -> {}", clusterHostEntity.getIp(), e.getMessage());
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
        List<ClusterHostEntity> clusterHostList = hostService.getHostListByIds(Arrays.asList(clusterHostIdArray));

        CommandType serviceCommandType = "start".equalsIgnoreCase(commandType) ? CommandType.START_SERVICE
                : CommandType.STOP_SERVICE;

        for (ClusterHostEntity clusterHostEntity : clusterHostList) {
            var result = new HashMap<String, Object>();
            result.put("hostname", clusterHostEntity.getHostname());
            result.put("ip", clusterHostEntity.getIp());
            result.put("commandType", serviceCommandType.toString());

            var serviceMessage = new WorkerServiceMessage(clusterHostEntity.getHostname(),
                    clusterHostEntity.getClusterId(), serviceCommandType);
            try {
                // 向 Worker 远程 Actor 发送服务命令
                var workerActorPath = "pekko://datasophon@" + clusterHostEntity.getHostname() + ":2552/user/worker";
                var workerActor = ActorUtils.actorSystem.actorSelection(workerActorPath);
                workerActor.tell(serviceMessage, ActorRef.noSender());
                result.put("success", true);
                result.put("message", "服务命令已发送");
                log.info("Service command sent successfully to {}: {}", clusterHostEntity.getHostname(),
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

    /**
     * 获取主机最近日志
     *
     * @param ip        主机IP
     * @param clusterId 集群ID
     * @return 主机最近日志内容
     */
    @Override
    public String getWorkerLog(String ip, Long clusterId) {
        try {
            // 1. 从缓存中获取主机信息
            Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);

            // 2. 查找指定IP的主机信息
            HostInfo hostInfo = hostMap.get(ip);
            if (hostInfo == null) {
                throw new BusinessException(Status.IP_IS_EMPTY.getCode(), "未找到主机信息");
            }

            // 3. 通过SSH插件适配器执行日志获取命令
            try {
                String command = "tail -n 100 /opt/datasophon/datasophon-worker/logs/datasophon-worker.log";
                
                // 4. 使用SSH插件适配器执行命令并返回日志内容
                HostCheckContext context = buildHostCheckContext(hostInfo);
                CommandResult result = getSshService().executeCommand(context, command);
                String logContent = result.isSuccess() ? result.output() : "";
                
                log.debug("【安装服务】获取主机工作日志成功: {} -> {} 字符", hostInfo.getIp(),
                        logContent.length());
                
                return logContent;
            } catch (Exception sshException) {
                throw new ServiceException("SSH连接失败: " + sshException.getMessage());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取主机日志失败", e);
            throw new ServiceException("获取主机日志失败: " + e.getMessage());
        }
    }

}
