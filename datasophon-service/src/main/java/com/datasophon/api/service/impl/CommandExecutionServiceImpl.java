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
import com.datasophon.api.converter.ClusterServiceCommandHostCommandConverter;
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
import com.datasophon.common.enums.CommandState;
import com.datasophon.common.enums.RoleType;
import com.datasophon.common.enums.ServiceRoleType;
import org.apache.pekko.actor.ActorRef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import scala.concurrent.duration.FiniteDuration;

import java.time.LocalDateTime;
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

    @Autowired
    private ClusterServiceCommandHostCommandConverter hostCommandConverter;

    @Override
    public void updateCommandStateToFailed(List<Long> commandIds) {
        for (Long commandId : commandIds) {
            logger.info("command id is {}", commandId);
            // cancel worker and sub node
            ActorRef commandActor = ActorUtils.getLocalActor(ServiceCommandActor.class, "commandActor");
            List<ClusterServiceCommandHostCommandDTO> hostCommandList = hostCommandService
                    .getHostCommandListByCommandId(commandId);
            for (ClusterServiceCommandHostCommandDTO hostCommandDTO : hostCommandList) {
                if (hostCommandDTO.commandState() == CommandState.RUNNING.getValue()) {
                    logger.info("{} host command  set to cancel", hostCommandDTO.commandName());
                    CancelCommandMap.put(hostCommandDTO.hostCommandId(), hostCommandDTO.commandName());

                    // 使用MapStruct转换器创建更新后的DTO - JDK21特性
                    var hostCommandEntity = hostCommandConverter.dtoToEntity(hostCommandDTO);
                    hostCommandEntity.setCommandState(CommandState.CANCEL); // 设置为取消状态
                    hostCommandEntity.setCommandProgress(100); // 设置进度为100
                    var updatedDTO = hostCommandConverter.entityToDto(hostCommandEntity);
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
    public void handleCommandResult(Long hostCommandId, Boolean execResult, String execOut) {
        var hostCommandDTO = hostCommandService.getByHostCommandId(hostCommandId); // JDK21特性

        // 使用MapStruct转换器创建更新后的DTO - JDK21特性
        var hostCommandEntity = hostCommandConverter.dtoToEntity(hostCommandDTO);
        hostCommandEntity.setCommandState(execResult ? CommandState.SUCCESS : CommandState.FAILED);
        hostCommandEntity.setCommandProgress(100); // 设置进度为100
        hostCommandEntity.setResultMsg(execResult ? "success" : execOut); // 设置结果消息
        var updatedDTO = hostCommandConverter.entityToDto(hostCommandEntity);
        
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
            Long clusterId,
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
    public ClusterServiceCommandEntity generateCommandEntity(Long clusterId, CommandType commandType,
            String serviceName) {
        ClusterServiceCommandEntity commandEntity = new ClusterServiceCommandEntity();
        Long commandId = IdUtil.getSnowflakeNextId();
        commandEntity.setId(commandId);
        commandEntity.setClusterId(clusterId);
        commandEntity.setCommandName(commandType.getCommandName(PropertyUtils.getString(Constants.LOCALE_LANGUAGE))
                + Constants.SPACE + serviceName);
        commandEntity.setCommandProgress(0L);
        commandEntity.setCommandState(CommandState.RUNNING);
        commandEntity.setCommandType(commandType.getValue());
        commandEntity.setCreateTime(LocalDateTime.now());
        commandEntity.setCreateBy("admin");
        commandEntity.setServiceName(serviceName);
        return commandEntity;
    }

    @Override
    public ClusterServiceCommandHostEntity generateCommandHostEntity(Long commandId, String hostname) {
        ClusterServiceCommandHostEntity commandHost = new ClusterServiceCommandHostEntity();
        Long commandHostId = IdUtil.getSnowflakeNextId();
        commandHost.setId(commandHostId);
        commandHost.setCommandId(commandId);
        commandHost.setHostname(hostname);
        commandHost.setCommandState(CommandState.RUNNING);
        commandHost.setCommandProgress(0L);
        commandHost.setCreateTime(LocalDateTime.now());

        return commandHost;
    }

    @Override
    public ClusterServiceCommandHostCommandEntity generateCommandHostCommandEntity(CommandType commandType,
                                                                                   Long commandId,
            String serviceRoleName,
            RoleType serviceRoleType,
            ClusterServiceCommandHostEntity commandHost) {
        ClusterServiceCommandHostCommandEntity hostCommand = new ClusterServiceCommandHostCommandEntity();
        Long hostCommandId = IdUtil.getSnowflakeNextId();
        hostCommand.setId(hostCommandId);
        hostCommand.setServiceRoleName(serviceRoleName);
        hostCommand.setCommandHostId(commandHost.getId());
        hostCommand.setCommandState(CommandState.RUNNING);
        hostCommand.setCommandProgress(0);
        hostCommand.setHostname(commandHost.getHostname());
        hostCommand.setCommandName(commandType.getCommandName(PropertyUtils.getString(Constants.LOCALE_LANGUAGE))
                + Constants.SPACE + serviceRoleName);
        hostCommand.setCommandId(commandId);
        hostCommand.setCommandType(commandType.getValue());
        hostCommand.setServiceRoleType(serviceRoleType);
        hostCommand.setCreateTime(LocalDateTime.now());
        return hostCommand;
    }
}