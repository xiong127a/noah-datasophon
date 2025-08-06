/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.service.impl;

import cn.hutool.core.util.IdUtil;
// 移除未使用的import
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.CancelCommandMap;
import com.datasophon.api.master.ServiceCommandActor;
import com.datasophon.api.master.ServiceExecuteResultActor;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.CommandExecutionService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceExecuteResultMessage;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.model.UpdateCommandHostMessage;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.dao.enums.RoleType;
import com.datasophon.common.enums.ServiceRoleType;
import org.apache.pekko.actor.ActorRef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import scala.concurrent.duration.FiniteDuration;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 命令执行管理服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service
public class CommandExecutionServiceImpl implements CommandExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutionServiceImpl.class);

    @Autowired
    private ClusterServiceCommandHostCommandService hostCommandService;

    // hostCommandConverter已移除，因为现在直接使用DTO操作

    @Override
    public void updateCommandStateToFailed(List<String> commandIds) {
        for (String commandId : commandIds) {
            logger.info("command id is {}", commandId);
            // cancel worker and sub node
            ActorRef commandActor = ActorUtils.getLocalActor(ServiceCommandActor.class, "commandActor");
            List<ClusterServiceCommandHostCommandDTO> hostCommandList = hostCommandService
                    .getHostCommandListByCommandId(commandId);
            for (ClusterServiceCommandHostCommandDTO hostCommandDTO : hostCommandList) {
                if (hostCommandDTO.commandState() == CommandState.RUNNING.getValue()) {
                    logger.info("{} host command  set to cancel", hostCommandDTO.commandName());
                    CancelCommandMap.put(hostCommandDTO.hostCommandId(), hostCommandDTO.commandName());

                    // 创建更新后的DTO - JDK21 Record特性
                    var updatedDTO = new ClusterServiceCommandHostCommandDTO(
                        hostCommandDTO.hostCommandId(),
                        hostCommandDTO.commandName(),
                        CommandState.CANCEL.getValue(), // 设置为取消状态
                        CommandState.CANCEL.getValue(),
                        100, // 设置进度为100
                        hostCommandDTO.commandHostId(),
                        hostCommandDTO.commandId(),
                        hostCommandDTO.hostname(),
                        hostCommandDTO.serviceRoleName(),
                        hostCommandDTO.serviceRoleType(),
                        hostCommandDTO.resultMsg(),
                        hostCommandDTO.createTime(),
                        hostCommandDTO.commandType()
                    );
                    hostCommandService.updateByHostCommandId(updatedDTO); // 传递DTO而不是Entity

                    var message = new UpdateCommandHostMessage(); // JDK21特性
                    message.setCommandId(commandId);
                    message.setCommandHostId(hostCommandDTO.hostCommandId());
                    message.setHostname(hostCommandDTO.hostname());
                    if (hostCommandDTO.serviceRoleType() == RoleType.MASTER.getValue()) {
                        message.setServiceRoleType(ServiceRoleType.MASTER);
                    } else {
                        message.setServiceRoleType(ServiceRoleType.WORKER);
                    }
                    ActorUtils.actorSystem.scheduler().scheduleOnce(
                            FiniteDuration.apply(3L, TimeUnit.SECONDS),
                            commandActor,
                            message,
                            ActorUtils.actorSystem.dispatcher(),
                            ActorRef.noSender());
                }
            }
        }
    }

    @Override
    public void handleCommandResult(String hostCommandId, Boolean execResult, String execOut) {
        var hostCommandDTO = hostCommandService.getByHostCommandId(hostCommandId); // JDK21特性

        // 创建更新后的DTO - JDK21 Record特性
        var updatedDTO = new ClusterServiceCommandHostCommandDTO(
            hostCommandDTO.hostCommandId(),
            hostCommandDTO.commandName(),
            execResult ? CommandState.SUCCESS.getValue() : CommandState.FAILED.getValue(),
            execResult ? CommandState.SUCCESS.getValue() : CommandState.FAILED.getValue(),
            100, // 设置进度为100
            hostCommandDTO.commandHostId(),
            hostCommandDTO.commandId(),
            hostCommandDTO.hostname(),
            hostCommandDTO.serviceRoleName(),
            hostCommandDTO.serviceRoleType(),
            execResult ? "success" : execOut, // 设置结果消息
            hostCommandDTO.createTime(),
            hostCommandDTO.commandType()
        );
        
        if (execResult) {
            logger.info("{} in {} success", updatedDTO.commandName(), updatedDTO.hostname());
        } else {
            logger.info("{} in {} failed", updatedDTO.commandName(), updatedDTO.hostname());
        }
        
        hostCommandService.updateByHostCommandId(updatedDTO); // 传递DTO而不是Entity

        // 更新command host进度
        // 更新command进度
        var message = new UpdateCommandHostMessage(); // JDK21特性
        message.setExecResult(execResult);
        message.setCommandId(updatedDTO.commandId());
        message.setCommandHostId(updatedDTO.commandHostId());
        message.setHostname(updatedDTO.hostname());
        if (updatedDTO.serviceRoleType() == RoleType.MASTER.getValue()) {
            message.setServiceRoleType(ServiceRoleType.MASTER);
        } else {
            message.setServiceRoleType(ServiceRoleType.WORKER);
        }

        ActorRef commandActor = ActorUtils.getLocalActor(ServiceCommandActor.class, "commandActor");
        ActorUtils.actorSystem.scheduler().scheduleOnce(FiniteDuration.apply(
                1L, TimeUnit.SECONDS),
                commandActor, message,
                ActorUtils.actorSystem.dispatcher(),
                ActorRef.noSender());
    }

    @Override
    public void tellCommandActorResult(String serviceName, ExecuteServiceRoleCommand executeServiceRoleCommand,
            ServiceExecuteState state) {
        ActorRef serviceExecuteResultActor = ActorUtils.getLocalActor(ServiceExecuteResultActor.class,
                ActorUtils.getActorRefName(ServiceExecuteResultActor.class));

        ServiceExecuteResultMessage serviceExecuteResultMessage = new ServiceExecuteResultMessage();
        serviceExecuteResultMessage.setServiceExecuteState(state);
        serviceExecuteResultMessage.setDag(executeServiceRoleCommand.getDag());
        serviceExecuteResultMessage.setServiceName(serviceName);
        serviceExecuteResultMessage.setClusterCode(executeServiceRoleCommand.getClusterCode());
        serviceExecuteResultMessage.setServiceRoleType(executeServiceRoleCommand.getServiceRoleType());
        serviceExecuteResultMessage.setCommandType(executeServiceRoleCommand.getCommandType());
        serviceExecuteResultMessage.setDag(executeServiceRoleCommand.getDag());
        serviceExecuteResultMessage.setClusterId(executeServiceRoleCommand.getClusterId());
        serviceExecuteResultMessage.setActiveTaskList(executeServiceRoleCommand.getActiveTaskList());
        serviceExecuteResultMessage.setErrorTaskList(executeServiceRoleCommand.getErrorTaskList());
        serviceExecuteResultMessage.setReadyToSubmitTaskList(executeServiceRoleCommand.getReadyToSubmitTaskList());
        serviceExecuteResultMessage.setCompleteTaskList(executeServiceRoleCommand.getCompleteTaskList());

        serviceExecuteResultActor.tell(serviceExecuteResultMessage, ActorRef.noSender());
    }

    @Override
    public void buildExecuteServiceRoleCommand(
            Integer clusterId,
            CommandType commandType,
            String clusterCode,
            DAGGraph<String, ServiceNode, String> dag,
            Map<String, ServiceExecuteState> activeTaskList,
            Map<String, String> errorTaskList,
            Map<String, String> readyToSubmitTaskList,
            Map<String, String> completeTaskList,
            String node,
            List<ServiceRoleInfo> masterRoles,
            ServiceRoleInfo workerRole,
            ActorRef serviceActor,
            ServiceRoleType serviceRoleType) {
        ExecuteServiceRoleCommand executeServiceRoleCommand = new ExecuteServiceRoleCommand(clusterId, node,
                masterRoles);
        executeServiceRoleCommand.setServiceRoleType(serviceRoleType);
        executeServiceRoleCommand.setCommandType(commandType);
        executeServiceRoleCommand.setDag(dag);
        executeServiceRoleCommand.setClusterCode(clusterCode);
        executeServiceRoleCommand.setClusterId(clusterId);
        executeServiceRoleCommand.setActiveTaskList(activeTaskList);
        executeServiceRoleCommand.setErrorTaskList(errorTaskList);
        executeServiceRoleCommand.setReadyToSubmitTaskList(readyToSubmitTaskList);
        executeServiceRoleCommand.setCompleteTaskList(completeTaskList);
        executeServiceRoleCommand.setWorkerRole(workerRole);
        serviceActor.tell(executeServiceRoleCommand, ActorRef.noSender());
    }

    @Override
    public ClusterServiceCommandEntity generateCommandEntity(Integer clusterId, CommandType commandType,
            String serviceName) {
        ClusterServiceCommandEntity commandEntity = new ClusterServiceCommandEntity();
        String commandId = IdUtil.simpleUUID();
        commandEntity.setCommandId(commandId);
        commandEntity.setClusterId(clusterId);
        commandEntity.setCommandName(commandType.getCommandName(PropertyUtils.getString(Constants.LOCALE_LANGUAGE))
                + Constants.SPACE + serviceName);
        commandEntity.setCommandProgress(0L);
        commandEntity.setCommandState(CommandState.RUNNING);
        commandEntity.setCommandType(commandType.getValue());
        commandEntity.setCreateTime(new Date());
        commandEntity.setCreateBy("admin");
        commandEntity.setServiceName(serviceName);
        return commandEntity;
    }

    @Override
    public ClusterServiceCommandHostEntity generateCommandHostEntity(String commandId, String hostname) {
        ClusterServiceCommandHostEntity commandHost = new ClusterServiceCommandHostEntity();
        String commandHostId = IdUtil.simpleUUID();
        commandHost.setCommandHostId(commandHostId);
        commandHost.setCommandId(commandId);
        commandHost.setHostname(hostname);
        commandHost.setCommandState(CommandState.RUNNING);
        commandHost.setCommandProgress(0L);
        commandHost.setCreateTime(new Date());

        return commandHost;
    }

    @Override
    public ClusterServiceCommandHostCommandEntity generateCommandHostCommandEntity(CommandType commandType,
            String commandId,
            String serviceRoleName,
            RoleType serviceRoleType,
            ClusterServiceCommandHostEntity commandHost) {
        ClusterServiceCommandHostCommandEntity hostCommand = new ClusterServiceCommandHostCommandEntity();
        String hostCommandId = IdUtil.simpleUUID();
        hostCommand.setHostCommandId(hostCommandId);
        hostCommand.setServiceRoleName(serviceRoleName);
        hostCommand.setCommandHostId(commandHost.getCommandHostId());
        hostCommand.setCommandState(CommandState.RUNNING);
        hostCommand.setCommandProgress(0);
        hostCommand.setHostname(commandHost.getHostname());
        hostCommand.setCommandName(commandType.getCommandName(PropertyUtils.getString(Constants.LOCALE_LANGUAGE))
                + Constants.SPACE + serviceRoleName);
        hostCommand.setCommandId(commandId);
        hostCommand.setCommandType(commandType.getValue());
        hostCommand.setServiceRoleType(serviceRoleType);
        hostCommand.setCreateTime(new Date());
        return hostCommand;
    }
}