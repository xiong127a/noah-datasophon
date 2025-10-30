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
import com.datasophon.api.service.ServiceExecuteResultService;
import com.datasophon.common.command.SubmitActiveTaskNodeCommand;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceExecuteResultMessage;
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
import java.util.Set;

/**
 * 服务执行结果处理服务实现
 * 替代ServiceExecuteResultActor，使用Spring Service实现
 */
@Service
public class ServiceExecuteResultServiceImpl implements ServiceExecuteResultService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceExecuteResultServiceImpl.class);

    @Autowired
    private CommandExecutionService commandExecutionService;
    
    @Autowired(required = false)
    private SubmitTaskNodeService submitTaskNodeService;
    
    @Autowired(required = false)
    private WorkerServiceExecutionService workerServiceExecutionService;

    @Override
    @Async("taskExecutor")
    public void handleServiceExecuteResult(ServiceExecuteResultMessage result) {
        try {
            DAGGraph<String, ServiceNode, String> dag = result.getDag();
            Map<String, ServiceExecuteState> activeTaskList = result.getActiveTaskList();
            Map<String, String> errorTaskList = result.getErrorTaskList();
            Map<String, String> readyToSubmitTaskList = result.getReadyToSubmitTaskList();
            Map<String, String> completeTaskList = result.getCompleteTaskList();
            
            String node = result.getServiceName();
            ServiceNode servicNode = dag.getNode(node);
            
            if (result.getServiceRoleType().equals(ServiceRoleType.MASTER)) {
                if (result.getServiceExecuteState().equals(ServiceExecuteState.ERROR)) {
                    handleMasterRoleError(result, dag, activeTaskList, errorTaskList, 
                            readyToSubmitTaskList, completeTaskList, node, servicNode);
                } else if (result.getServiceExecuteState().equals(ServiceExecuteState.SUCCESS)) {
                    handleMasterRoleSuccess(result, dag, activeTaskList, errorTaskList, 
                            readyToSubmitTaskList, completeTaskList, node, servicNode);
                }
            }
        } catch (Exception e) {
            logger.error("处理ServiceExecuteResultMessage时出错", e);
        }
    }

    private void handleMasterRoleError(ServiceExecuteResultMessage result,
                                      DAGGraph<String, ServiceNode, String> dag,
                                      Map<String, ServiceExecuteState> activeTaskList,
                                      Map<String, String> errorTaskList,
                                      Map<String, String> readyToSubmitTaskList,
                                      Map<String, String> completeTaskList,
                                      String node,
                                      ServiceNode servicNode) {
        // 移动到错误列表
        errorTaskList.put(node, "");
        activeTaskList.remove(node);
        readyToSubmitTaskList.remove(node);
        completeTaskList.put(node, "");
        
        // 取消所有后续节点
        logger.info("{} master角色失败，取消所有后续节点，commandId: {}", node, servicNode.getCommandId());
        List<Long> commandIds = new ArrayList<>();
        commandIds.add(servicNode.getCommandId());
        listCancelCommand(dag, node, commandIds);
        commandExecutionService.updateCommandStateToFailed(commandIds);
    }

    private void handleMasterRoleSuccess(ServiceExecuteResultMessage result,
                                        DAGGraph<String, ServiceNode, String> dag,
                                        Map<String, ServiceExecuteState> activeTaskList,
                                        Map<String, String> errorTaskList,
                                        Map<String, String> readyToSubmitTaskList,
                                        Map<String, String> completeTaskList,
                                        String node,
                                        ServiceNode serviceNode) {
        // 提交worker节点
        List<ServiceRoleInfo> elseRoles = serviceNode.getElseRoles();
        if (CollUtil.isNotEmpty(elseRoles)) {
            logger.info("开始提交worker/client角色");
            for (ServiceRoleInfo elseRole : serviceNode.getElseRoles()) {
                if (workerServiceExecutionService != null) {
                    workerServiceExecutionService.executeWorkerServiceRole(
                            result.getClusterId(),
                            result.getCommandType(),
                            result.getClusterCode(),
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
                    // 降级处理：直接调用CommandExecutionService
                    commandExecutionService.buildExecuteServiceRoleCommand(
                            result.getClusterId(),
                            result.getCommandType(),
                            result.getClusterCode(),
                            dag,
                            activeTaskList,
                            errorTaskList,
                            readyToSubmitTaskList,
                            completeTaskList,
                            node,
                            serviceNode.getElseRoles(),
                            elseRole,
                            null, // ActorRef不再需要
                            ServiceRoleType.WORKER);
                }
            }
        } else {
            activeTaskList.remove(node);
            readyToSubmitTaskList.remove(node);
        }
        
        logger.info("开始提交下一个节点");
        submitNextActiveTaskNode(result, dag, activeTaskList, errorTaskList, 
                readyToSubmitTaskList, completeTaskList, node);
    }

    private void listCancelCommand(DAGGraph<String, ServiceNode, String> dag, 
                                   String node, List<Long> commandIds) {
        if (dag.getSubsequentNodes(node).isEmpty()) {
            return;
        }
        Set<String> subsequentNodes = dag.getSubsequentNodes(node);
        for (String subsequentNode : subsequentNodes) {
            commandIds.add(dag.getNode(subsequentNode).getCommandId());
            listCancelCommand(dag, subsequentNode, commandIds);
        }
    }

    private void submitNextActiveTaskNode(ServiceExecuteResultMessage result,
                                         DAGGraph<String, ServiceNode, String> dag,
                                         Map<String, ServiceExecuteState> activeTaskList,
                                         Map<String, String> errorTaskList,
                                         Map<String, String> readyToSubmitTaskList,
                                         Map<String, String> completeTaskList,
                                         String node) {
        if (dag.getSubsequentNodes(node).isEmpty()) {
            logger.info("所有节点已完成");
            return;
        }
        
        Set<String> subsequentNodes = dag.getSubsequentNodes(node);
        for (String subsequentNode : subsequentNodes) {
            Boolean canSubmit = dag.getNode(subsequentNode).getCanSubmit();
            if (canSubmit) {
                continue;
            }
            
            List<String> startNodes = new ArrayList<>();
            Set<String> previousNodes = dag.getPreviousNodes(subsequentNode);
            if (previousNodes.isEmpty()) {
                startNodes.add(subsequentNode);
            } else {
                for (String previousNode : previousNodes) {
                    if (completeTaskList.containsKey(previousNode)) {
                        startNodes.add(subsequentNode);
                    }
                }
            }
            
            if (!startNodes.isEmpty()) {
                dag.getNode(subsequentNode).setCanSubmit(true);
                if (submitTaskNodeService != null) {
                    SubmitActiveTaskNodeCommand submitCommand = new SubmitActiveTaskNodeCommand();
                    submitCommand.setClusterId(result.getClusterId());
                    submitCommand.setCommandType(result.getCommandType());
                    submitCommand.setClusterCode(result.getClusterCode());
                    submitCommand.setDag(dag);
                    submitCommand.setActiveTaskList(activeTaskList);
                    submitCommand.setErrorTaskList(errorTaskList);
                    submitCommand.setReadyToSubmitTaskList(readyToSubmitTaskList);
                    submitCommand.setCompleteTaskList(completeTaskList);
                    submitCommand.setStartNodes(startNodes);
                    
                    submitTaskNodeService.submitActiveTaskNode(submitCommand);
                }
            }
        }
    }
}

