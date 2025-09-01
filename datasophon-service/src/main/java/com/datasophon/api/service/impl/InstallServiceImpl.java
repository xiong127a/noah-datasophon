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
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.DispatcherWorkerActor;
import com.datasophon.api.master.WorkerStartActor;
import com.datasophon.api.converter.InstallStepConverter;
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
import com.datasophon.common.command.DispatcherHostAgentCommand;
import com.datasophon.common.dto.HostCheckStatusDto;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.exception.ServiceException;
import com.datasophon.common.exception.BusinessException;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.WorkerServiceMessage;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.InstallStepEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import com.datasophon.api.service.SshPluginAdapterService;
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
    private  SshPluginAdapterService sshPluginAdapter;
    @Autowired
    private  InstallStepConverter installStepConverter;

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
                    hostInfo.setCreateTime(LocalDateTime.now());
                    hostActor.tell(new DispatcherHostAgentCommand(hostInfo, clusterId, clusterInfo.getClusterFrame()),
                            ActorRef.noSender());
                    // 保存主机agent分发历史
                    CacheUtils.put(distributeAgentKey + Constants.UNDERLINE + hostInfo.getIp(), true);

                } else {
                    long timeout = ChronoUnit.MINUTES.between(hostInfo.getCreateTime(), LocalDateTime.now());
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
    public boolean reStartDispatcherHostAgent(Long clusterId, String ips) {
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
    public boolean cleanupHostCheckResources(Long clusterId) {
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
    public boolean cancelDispatcherHostAgent(Long clusterId, String ip, Integer installStateCode) {
        // 此方法虽然定义但实际未使用
        log.warn("cancelDispatcherHostAgent方法暂未实现具体逻辑");
        return false;
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
                
                String commandResult = sshPluginAdapter.executeCommand(hostInfo, command);
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
            Map<String, Object> result = new HashMap<>();
            result.put("hostname", clusterHostEntity.getHostname());
            result.put("ip", clusterHostEntity.getIp());
            result.put("commandType", serviceCommandType.toString());

            WorkerServiceMessage serviceMessage = new WorkerServiceMessage(clusterHostEntity.getHostname(),
                    clusterHostEntity.getClusterId(), serviceCommandType);
            try {
                ActorRef actor = ActorUtils.getLocalActor(WorkerStartActor.class, "workerStartActor");
                actor.tell(serviceMessage, ActorRef.noSender());
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
                String logContent = sshPluginAdapter.executeCommand(hostInfo, command);
                
                log.debug("【安装服务】获取主机工作日志成功: {} -> {} 字符", hostInfo.getIp(), 
                        logContent != null ? logContent.length() : 0);
                
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
