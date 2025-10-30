/*
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
 */

package com.datasophon.api.service.impl;

import com.datasophon.api.service.CommandExecutionService;
import com.datasophon.api.service.ServiceInstallationService;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Worker服务执行服务实现
 * 替代WorkerServiceActor，处理Worker节点的服务执行
 */
@Service
public class WorkerServiceExecutionServiceImpl implements WorkerServiceExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerServiceExecutionServiceImpl.class);

    @Autowired
    private CommandExecutionService commandExecutionService;
    
    @Autowired
    private ServiceInstallationService serviceInstallationService;

    @Override
    @Async("taskExecutor")
    public void executeWorkerServiceRole(
            Long clusterId,
            CommandType commandType,
            String clusterCode,
            DAGGraph<String, ServiceNode, String> dag,
            Map<String, ServiceExecuteState> activeTaskList,
            Map<String, String> errorTaskList,
            Map<String, String> readyToSubmitTaskList,
            Map<String, String> completeTaskList,
            String node,
            List<ServiceRoleInfo> elseRoles,
            ServiceRoleInfo elseRole,
            ServiceRoleType serviceRoleType) {
        
        logger.info("执行Worker服务角色: {}, 主机: {}", node, elseRole.getHostname());
        
        try {
            // 执行Worker角色的命令
            ExecResult execResult = executeWorkerCommand(elseRole, commandType);
            
            // 通知执行结果
            if (execResult != null && execResult.getExecResult()) {
                logger.info("Worker角色执行成功: {}, 主机: {}", node, elseRole.getHostname());
                commandExecutionService.tellCommandActorResult(
                        elseRole.getParentName(),
                        buildExecuteCommand(clusterId, commandType, clusterCode, dag, activeTaskList,
                                errorTaskList, readyToSubmitTaskList, completeTaskList, node, elseRoles, elseRole),
                        ServiceExecuteState.SUCCESS);
            } else {
                logger.error("Worker角色执行失败: {}, 主机: {}, 错误: {}", 
                        node, elseRole.getHostname(), execResult != null ? execResult.getExecOut() : "Unknown error");
                commandExecutionService.tellCommandActorResult(
                        elseRole.getParentName(),
                        buildExecuteCommand(clusterId, commandType, clusterCode, dag, activeTaskList,
                                errorTaskList, readyToSubmitTaskList, completeTaskList, node, elseRoles, elseRole),
                        ServiceExecuteState.ERROR);
            }
        } catch (Exception e) {
            logger.error("Worker角色执行异常: {}, 主机: {}", node, elseRole.getHostname(), e);
            commandExecutionService.tellCommandActorResult(
                    elseRole.getParentName(),
                    buildExecuteCommand(clusterId, commandType, clusterCode, dag, activeTaskList,
                            errorTaskList, readyToSubmitTaskList, completeTaskList, node, elseRoles, elseRole),
                    ServiceExecuteState.ERROR);
        }
    }
    
    /**
     * 执行Worker命令
     */
    private ExecResult executeWorkerCommand(ServiceRoleInfo serviceRoleInfo, CommandType commandType) {
        // 检查是否需要重新配置
        boolean needReConfig = serviceRoleInfo.getConfigFileMap() != null 
                && !serviceRoleInfo.getConfigFileMap().isEmpty();
        
        return switch (commandType) {
            case INSTALL_SERVICE -> {
                logger.info("开始安装 {} 在主机 {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                yield serviceInstallationService.startInstallService(serviceRoleInfo);
            }
            case START_SERVICE -> {
                logger.info("开始启动 {} 在主机 {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                yield serviceInstallationService.startService(serviceRoleInfo, needReConfig);
            }
            case STOP_SERVICE -> {
                logger.info("开始停止 {} 在主机 {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                yield serviceInstallationService.stopService(serviceRoleInfo);
            }
            case RESTART_SERVICE -> {
                logger.info("开始重启 {} 在主机 {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                yield serviceInstallationService.restartService(serviceRoleInfo, needReConfig);
            }
            default -> {
                logger.warn("未知的命令类型: {}", commandType);
                yield null;
            }
        };
    }
    
    /**
     * 构建ExecuteServiceRoleCommand
     */
    private ExecuteServiceRoleCommand buildExecuteCommand(
            Long clusterId,
            CommandType commandType,
            String clusterCode,
            DAGGraph<String, ServiceNode, String> dag,
            Map<String, ServiceExecuteState> activeTaskList,
            Map<String, String> errorTaskList,
            Map<String, String> readyToSubmitTaskList,
            Map<String, String> completeTaskList,
            String node,
            List<ServiceRoleInfo> elseRoles,
            ServiceRoleInfo elseRole) {
        
        ExecuteServiceRoleCommand command = new ExecuteServiceRoleCommand(clusterId, node, elseRoles);
        command.setServiceRoleType(ServiceRoleType.WORKER);
        command.setCommandType(commandType);
        command.setDag(dag);
        command.setClusterCode(clusterCode);
        command.setActiveTaskList(activeTaskList);
        command.setErrorTaskList(errorTaskList);
        command.setReadyToSubmitTaskList(readyToSubmitTaskList);
        command.setCompleteTaskList(completeTaskList);
        command.setWorkerRole(elseRole);
        
        return command;
    }
}

