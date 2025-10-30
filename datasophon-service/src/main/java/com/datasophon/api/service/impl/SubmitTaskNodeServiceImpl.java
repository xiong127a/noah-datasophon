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

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.service.CommandExecutionService;
import com.datasophon.api.utils.RollingRestartUtils;
import com.datasophon.common.command.SubmitActiveTaskNodeCommand;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.RollingRestartInfo;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 任务节点提交服务实现
 * 替代SubmitTaskNodeActor，负责管理DAG任务节点的提交和调度
 */
@Service
public class SubmitTaskNodeServiceImpl implements SubmitTaskNodeService {

    private static final Logger logger = LoggerFactory.getLogger(SubmitTaskNodeServiceImpl.class);

    @Autowired
    private CommandExecutionService commandExecutionService;
    
    @Autowired(required = false)
    private WorkerServiceExecutionService workerServiceExecutionService;

    @Override
    @Async("taskExecutor")
    public void submitActiveTaskNode(SubmitActiveTaskNodeCommand submitActiveTaskNodeCommand) {
        try {
            DAGGraph<String, ServiceNode, String> dag = submitActiveTaskNodeCommand.getDag();
            Map<String, ServiceExecuteState> activeTaskList = submitActiveTaskNodeCommand.getActiveTaskList();
            Map<String, String> errorTaskList = submitActiveTaskNodeCommand.getErrorTaskList();
            Map<String, String> readyToSubmitTaskList = submitActiveTaskNodeCommand.getReadyToSubmitTaskList();
            Map<String, String> completeTaskList = submitActiveTaskNodeCommand.getCompleteTaskList();
            
            if (CollUtil.isNotEmpty(readyToSubmitTaskList)) {
                for (String node : readyToSubmitTaskList.keySet()) {
                    if (!canSubmitNode(node, dag, errorTaskList, completeTaskList, activeTaskList)) {
                        continue;
                    }
                    
                    ServiceNode serviceNode = dag.getNode(node);
                    List<ServiceRoleInfo> masterRoles = serviceNode.getMasterRoles();

                    activeTaskList.put(node, ServiceExecuteState.RUNNING);

                    if (CollUtil.isNotEmpty(masterRoles)) {
                        submitMasterRoles(submitActiveTaskNodeCommand, dag, activeTaskList, errorTaskList,
                                readyToSubmitTaskList, completeTaskList, node, masterRoles);
                    } else if (CollUtil.isNotEmpty(serviceNode.getElseRoles())) {
                        submitWorkerRoles(submitActiveTaskNodeCommand, dag, activeTaskList, errorTaskList,
                                readyToSubmitTaskList, completeTaskList, node, serviceNode);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("处理SubmitActiveTaskNodeCommand时出错", e);
        }
    }

    private boolean canSubmitNode(String node, DAGGraph<String, ServiceNode, String> dag,
                                  Map<String, String> errorTaskList,
                                  Map<String, String> completeTaskList,
                                  Map<String, ServiceExecuteState> activeTaskList) {
        Set<String> previousNodes = dag.getPreviousNodes(node);
        for (String previousNode : previousNodes) {
            if (errorTaskList.containsKey(previousNode)) {
                return false;
            }
            if (!completeTaskList.containsKey(previousNode)) {
                return false;
            }
        }
        
        if (activeTaskList.containsKey(node)) {
            return false;
        }
        if (completeTaskList.containsKey(node)) {
            return false;
        }
        return true;
    }

    private void submitMasterRoles(SubmitActiveTaskNodeCommand submitActiveTaskNodeCommand,
                                  DAGGraph<String, ServiceNode, String> dag,
                                  Map<String, ServiceExecuteState> activeTaskList,
                                  Map<String, String> errorTaskList,
                                  Map<String, String> readyToSubmitTaskList,
                                  Map<String, String> completeTaskList,
                                  String node,
                                  List<ServiceRoleInfo> masterRoles) {
        logger.info("开始提交 {} master角色", node);
        
        // TODO: 实现Master角色的实际执行逻辑
        logger.warn("Master角色执行逻辑待实现: {}", node);
    }

    private void submitWorkerRoles(SubmitActiveTaskNodeCommand submitActiveTaskNodeCommand,
                                  DAGGraph<String, ServiceNode, String> dag,
                                  Map<String, ServiceExecuteState> activeTaskList,
                                  Map<String, String> errorTaskList,
                                  Map<String, String> readyToSubmitTaskList,
                                  Map<String, String> completeTaskList,
                                  String node,
                                  ServiceNode serviceNode) {
        logger.info("{} 没有master角色，开始提交worker或client角色", node);

        RollingRestartInfo rollingRestartInfo = submitActiveTaskNodeCommand.getRollingRestartInfo();
        int errorCount = 0;
        List<ServiceRoleInfo> batchList = new ArrayList<>();

        for (ServiceRoleInfo elseRole : serviceNode.getElseRoles()) {
            if (workerServiceExecutionService != null) {
                workerServiceExecutionService.executeWorkerServiceRole(
                        submitActiveTaskNodeCommand.getClusterId(),
                        submitActiveTaskNodeCommand.getCommandType(),
                        submitActiveTaskNodeCommand.getClusterCode(),
                        dag,
                        activeTaskList,
                        errorTaskList,
                        readyToSubmitTaskList,
                        completeTaskList,
                        node,
                        serviceNode.getElseRoles(),
                        elseRole,
                        ServiceRoleType.WORKER);
            } else {
                // 降级处理：使用CommandExecutionService
                commandExecutionService.buildExecuteServiceRoleCommand(
                        submitActiveTaskNodeCommand.getClusterId(),
                        submitActiveTaskNodeCommand.getCommandType(),
                        submitActiveTaskNodeCommand.getClusterCode(),
                        dag,
                        activeTaskList,
                        errorTaskList,
                        readyToSubmitTaskList,
                        completeTaskList,
                        node,
                        serviceNode.getElseRoles(),
                        elseRole,
                        null, // ActorRef已废弃
                        ServiceRoleType.WORKER);
            }

            // 滚动重启控制
            batchList.add(elseRole);
            if (Objects.nonNull(rollingRestartInfo)
                    && batchList.size() == rollingRestartInfo.getBatchCount()) {
                try {
                    // 批量等待
                    for (ServiceRoleInfo serviceRoleInfo : batchList) {
                        RollingRestartUtils.getCountDownLatchByServiceKey(
                                serviceRoleInfo.getHostname() + serviceRoleInfo.getServiceInstanceId())
                                .await();
                        // 错误计数
                        errorCount = errorCount + RollingRestartUtils.getErrorCount(
                                serviceRoleInfo.getHostname() + serviceRoleInfo.getServiceInstanceId());
                    }

                    // 启动失败数量大于阈值 停止后边的任务
                    if (errorCount > rollingRestartInfo.getTaskFailureTolerance()) {
                        return;
                    }
                    
                    logger.info("批次实例滚动重启结束");
                    logger.info("滚动重启批次等待:{} s", rollingRestartInfo.getBatchSeparationInSeconds());
                    Thread.sleep(rollingRestartInfo.getBatchSeparationInSeconds() * 1000);
                    logger.info("滚动重启批次等待结束");

                    batchList.clear();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("滚动重启等待被中断", e);
                    return;
                }
            }
        }
    }
}

