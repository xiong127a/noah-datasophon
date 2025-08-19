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

package com.datasophon.api.master;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.common.Constants;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.common.enums.ClusterState;
import com.datasophon.common.command.StartExecuteCommandCommand;
import com.datasophon.common.command.SubmitActiveTaskNodeCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceCommandDTO;
import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.common.dto.FrameServiceDTO;
import com.datasophon.common.dto.FrameServiceRoleDTO;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.api.converter.FrameServiceConverter;
import com.datasophon.api.converter.FrameServiceRoleConverter;
import com.datasophon.api.service.ClusterServiceCommandService;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DAGBuildActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(DAGBuildActor.class);

    @Override
    public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
        logger.info("restart actor {}", reason.getMessage());
        super.preRestart(reason, message);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(StartExecuteCommandCommand.class, this::handleStartExecuteCommand)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleStartExecuteCommand(StartExecuteCommandCommand executeCommandCommand) {
        try {
            DAGGraph<String, ServiceNode, String> dag = new DAGGraph<>();
            CommandType commandType = executeCommandCommand.getCommandType();
            logger.info("start execute command: commandType={}", commandType);
            
            ClusterServiceCommandHostCommandService hostCommandService = SpringUtil
                    .getBean(ClusterServiceCommandHostCommandService.class);
            FrameServiceRoleService frameServiceRoleService = SpringUtil.getBean(FrameServiceRoleService.class);
            FrameServiceService frameService = SpringUtil.getBean(FrameServiceService.class);
            ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);

            ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(executeCommandCommand.getClusterId());
            
            // 如果是安装服务命令，立即更新主机和集群状态
            if (commandType == CommandType.INSTALL_SERVICE) {
                logger.info("检测到安装服务命令，立即更新集群和主机状态: clusterId={}", clusterInfo.id());
                updateClusterAndHostStatusForInstall(clusterInfo);
            }
            // 获取命令列表
            ClusterServiceCommandService clusterServiceCommandService = SpringUtil.getBean(ClusterServiceCommandService.class);
            List<ClusterServiceCommandDTO> commandList = new ArrayList<>();
            for (Long commandId : executeCommandCommand.getCommandIds()) {
                ClusterServiceCommandDTO command = clusterServiceCommandService.getCommandById(commandId);
                if (command != null) {
                    commandList.add(command);
                }
            }

            ArrayList<FrameServiceEntity> frameServiceList = new ArrayList<>();
            if (ArrayUtil.isNotEmpty(commandList)) {
                for (ClusterServiceCommandDTO command : commandList) {
                    // build dag
                    List<ServiceRoleInfo> masterRoles = new ArrayList<>();
                    List<ServiceRoleInfo> elseRoles = new ArrayList<>();
                    ServiceNode serviceNode = new ServiceNode();

                    List<ClusterServiceCommandHostCommandDTO> hostCommandList = hostCommandService
                            .getHostCommandListByCommandId(command.id());

                    FrameServiceDTO serviceDto = frameService.getServiceByFrameCodeAndServiceName(
                            clusterInfo.clusterFrame(), command.serviceName());
                    // 使用MapStruct Converter进行转换
                    FrameServiceConverter serviceConverter = SpringUtil.getBean(FrameServiceConverter.class);
                    FrameServiceEntity serviceEntity = serviceConverter.dtoToEntity(serviceDto);
                    frameServiceList.add(serviceEntity);

                    serviceNode.setCommandId(command.id());
                    for (ClusterServiceCommandHostCommandDTO hostCommand : hostCommandList) {
                        logger.info("service role is {}", hostCommand.serviceRoleName());
                        FrameServiceRoleDTO frameServiceRoleDto = frameServiceRoleService
                                .getServiceRoleByFrameCodeAndServiceRoleName(
                                        clusterInfo.clusterFrame(), hostCommand.serviceRoleName());
                        // 使用MapStruct Converter进行转换
                        FrameServiceRoleConverter roleConverter = SpringUtil.getBean(FrameServiceRoleConverter.class);
                        FrameServiceRoleEntity frameServiceRoleEntity = roleConverter.dtoToEntity(frameServiceRoleDto);

                        ServiceRoleInfo serviceRoleInfo = JSONObject
                                .parseObject(frameServiceRoleEntity.getServiceRoleJson(), ServiceRoleInfo.class);
                        serviceRoleInfo.setHostname(hostCommand.hostname());
                        serviceRoleInfo.setHostCommandId(hostCommand.hostCommandId());
                        serviceRoleInfo.setClusterId(clusterInfo.id());
                        serviceRoleInfo.setParentName(command.serviceName());
                        serviceRoleInfo.setPackageName(serviceEntity.getPackageName());
                        serviceRoleInfo.setDecompressPackageName(serviceEntity.getDecompressPackageName());
                        serviceRoleInfo.setCommandType(commandType);
                        serviceRoleInfo.setServiceInstanceId(command.serviceInstanceId());

                        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext
                                .getServiceRoleHandler(serviceRoleInfo.getName());
                        if (Objects.nonNull(serviceRoleHandler)) {
                            serviceRoleHandler.handlerServiceRoleInfo(serviceRoleInfo, hostCommand.hostname());
                        }

                        if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                            masterRoles.add(serviceRoleInfo);
                        } else {
                            elseRoles.add(serviceRoleInfo);
                        }
                    }
                    serviceNode.setMasterRoles(masterRoles);
                    serviceNode.setElseRoles(elseRoles);
                    dag.addNode(command.serviceName(), serviceNode);
                }
                // build edge
                for (FrameServiceEntity serviceEntity : frameServiceList) {
                    if (StringUtils.isNotBlank(serviceEntity.getDependencies())) {
                        for (String dependency : serviceEntity.getDependencies().split(Constants.COMMA)) {
                            if (dag.containsNode(dependency)) {
                                dag.addEdge(dependency, serviceEntity.getServiceName(), false);
                            }
                        }
                    }
                }
            }

            if (commandType == CommandType.STOP_SERVICE) {
                logger.info("reverse dag");
                dag = dag.getReverseDagGraph(dag);
            }

            Map<String, String> errorTaskList = new ConcurrentHashMap<>();
            Map<String, ServiceExecuteState> activeTaskList = new ConcurrentHashMap<>();
            Map<String, String> readyToSubmitTaskList = new ConcurrentHashMap<>();
            Map<String, String> completeTaskList = new ConcurrentHashMap<>();

            Collection<String> beginNode = dag.getBeginNode();
            logger.info("beginNode is {}", beginNode.toString());
            for (String node : beginNode) {
                readyToSubmitTaskList.put(node, "");
            }

            SubmitActiveTaskNodeCommand submitActiveTaskNodeCommand = new SubmitActiveTaskNodeCommand();
            submitActiveTaskNodeCommand.setCommandType(executeCommandCommand.getCommandType());
            submitActiveTaskNodeCommand.setDag(dag);
            submitActiveTaskNodeCommand.setClusterId(clusterInfo.id());
            submitActiveTaskNodeCommand.setActiveTaskList(activeTaskList);
            submitActiveTaskNodeCommand.setErrorTaskList(errorTaskList);
            submitActiveTaskNodeCommand.setReadyToSubmitTaskList(readyToSubmitTaskList);
            submitActiveTaskNodeCommand.setCompleteTaskList(completeTaskList);
            submitActiveTaskNodeCommand.setClusterCode(clusterInfo.clusterCode());
            submitActiveTaskNodeCommand.setRollingRestartInfo(executeCommandCommand.getRollingRestartInfo());

            ActorRef submitTaskNodeActor = ActorUtils.getLocalActor(SubmitTaskNodeActor.class,
                    ActorUtils.getActorRefName(SubmitTaskNodeActor.class));
            submitTaskNodeActor.tell(submitActiveTaskNodeCommand, getSelf());
        } catch (Exception e) {
            logger.error("Error handling StartExecuteCommandCommand", e);
        }
    }
    
    /**
     * 为安装服务更新集群和主机状态
     */
    private void updateClusterAndHostStatusForInstall(ClusterInfoDTO clusterInfo) {
        try {
            // 1. 更新集群内所有主机状态为已受管
            try {
                ClusterHostService clusterHostService = SpringUtil.getBean(ClusterHostService.class);
                List<ClusterHostEntity> hosts = clusterHostService.getHostListByClusterId(clusterInfo.id());
                if (!hosts.isEmpty()) {
                    hosts.forEach(host -> {
                        if (host.getManagementStatus() != ManagementStatus.MANAGED) {
                            logger.info("更新主机 {} 管理状态为已受管", host.getHostname());
                            host.setManagementStatus(ManagementStatus.MANAGED);
                        }
                    });
                    clusterHostService.updateBatchHostStatus(hosts);
                    logger.info("成功更新 {} 个主机为已受管状态", hosts.size());
                }
            } catch (Exception e) {
                logger.error("更新主机管理状态失败: clusterId={}", clusterInfo.id(), e);
            }
            
            // 2. 更新集群状态为运行中
            try {
                if (clusterInfo.clusterState() != ClusterState.RUNNING.getValue()) {
                    ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
                    boolean updateResult = clusterInfoService.updateClusterState(clusterInfo.id(), ClusterState.RUNNING.getValue());
                    if (updateResult) {
                        logger.info("成功更新集群状态为运行中: clusterId={}", clusterInfo.id());
                    } else {
                        logger.warn("更新集群状态为运行中失败: clusterId={}", clusterInfo.id());
                    }
                } else {
                    logger.info("集群已处于运行中状态: clusterId={}", clusterInfo.id());
                }
            } catch (Exception e) {
                logger.error("更新集群状态失败: clusterId={}", clusterInfo.id(), e);
            }
        } catch (Exception e) {
            logger.error("更新集群和主机状态失败", e);
        }
    }
}
