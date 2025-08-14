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

import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.dao.entity.ClusterHostEntity;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.AbstractActor;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.api.service.ServiceInstallationService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateHostPrometheusConfig;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.StartWorkerMessage;
import com.datasophon.common.model.WorkerServiceMessage;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.common.dto.ClusterGroupDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.dto.ClusterUserDTO;
import com.datasophon.common.enums.ServiceRoleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * Master服务中的Worker启动Actor
 * <p>
 * 主要职责:
 * 1. 接收来自Worker的启动消息和心跳消息
 * 2. 更新主机状态信息
 * 3. 执行服务自动启动/停止
 * 4. 同步集群用户和组信息
 */
public class WorkerStartActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(WorkerStartActor.class);

    /**
     * 接收并处理来自Worker的消息
     * 该方法是Akka Actor的核心方法，用于处理所有接收到的消息
     * 在Noah大数据平台中，主要处理两种消息:
     * 1. StartWorkerMessage: Worker的初始启动消息和周期性心跳消息
     * 2. WorkerServiceMessage: 控制Worker上服务启停的消息
     *
     * @return Receive 消息接收器
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartWorkerMessage.class, this::handleStartWorkerMessage)
                .match(WorkerServiceMessage.class, this::handleWorkerServiceMessage)
                .matchAny(this::unhandled)
                .build();
    }

    /**
     * 处理Worker发送的启动消息或心跳消息
     *
     * @param msg Worker启动消息
     */
    private void handleStartWorkerMessage(StartWorkerMessage msg) {
        try {
            String hostname = msg.getHostname();
            String ip = msg.getIp();
            Long clusterId = msg.getClusterId();
            logger.info("收到Worker首次启动消息,主机名:{},IP地址:{}", hostname, ip);

            ClusterHostService clusterHostService = SpringUtil.getBean(ClusterHostService.class);
            ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);

            ClusterHostEntity hostEntity = clusterHostService.getClusterHostByIp(ip);
            ClusterInfoEntity cluster = clusterInfoService.getById(clusterId);
            logger.info("收到来自主机 {} ({}) 的Worker启动消息,设置主机安装状态为100%", hostname, ip);

            if (CacheUtils.constainsKey(clusterId + Constants.HOST_MAP)) {
                Map<String, HostInfo> map = CacheUtils
                        .getHostMap(clusterId + Constants.HOST_MAP);
                HostInfo hostInfo = map.get(ip);
                if (Objects.nonNull(hostInfo)) {
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setInstallState(InstallState.SUCCESS);
                    hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                    hostInfo.setManaged(true);
                }
            }

            // 如果数据库中不存在该主机,则保存主机信息
            if (ObjectUtil.isNull(hostEntity)) {
                // 保存主机安装信息到数据库
                ServiceInstallationService serviceInstallationService = SpringUtil.getBean(ServiceInstallationService.class);
                serviceInstallationService.saveHostInstallInfo(msg, cluster.getClusterCode());
                logger.info("Host install save to database");
                // 同步集群用户和组
                // syncClusterUserAndGroup(clusterId, hostname);
            } else {
                // 更新现有主机信息
                hostEntity.setCpuArchitecture(msg.getCpuArchitecture());
                hostEntity.setManagementStatus(ManagementStatus.MANAGED);
                clusterHostService.saveHost(hostEntity);
            }

            // 添加到Prometheus监控
            ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,
                    ActorUtils.getActorRefName(PrometheusActor.class));
            GenerateHostPrometheusConfig prometheusConfigCommand = new GenerateHostPrometheusConfig();
            prometheusConfigCommand.setClusterId(cluster.getId());
            prometheusActor.tell(prometheusConfigCommand, getSelf());

            // 告知worker需要启动的服务
        } catch (Exception e) {
            logger.error("处理StartWorkerMessage消息时出错", e);
        }
    }

    /**
     * 处理服务启停消息
     *
     * @param msg 服务启停消息
     */
    private void handleWorkerServiceMessage(WorkerServiceMessage msg) {
        try {
            // 告知worker需要启动/停止的服务
            autoAddServiceOperatorNeeded(msg.getHostname(), msg.getClusterId(), msg.getCommandType());
        } catch (Exception e) {
            logger.error("处理WorkerServiceMessage消息时出错", e);
        }
    }

    /**
     * 自动启动/停止需要操作的服务
     * <p>
     * 该方法会根据命令类型(启动或停止)和主机名查找需要操作的服务角色,
     * 然后生成相应的服务角色命令并执行
     *
     * @param hostname    主机名
     * @param clusterId   集群ID
     * @param commandType 命令类型(START_SERVICE或STOP_SERVICE)
     */
    private void autoAddServiceOperatorNeeded(String hostname, Long clusterId, CommandType commandType) {
        ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        ClusterServiceCommandService serviceCommandService = SpringUtil.getBean(ClusterServiceCommandService.class);

        List<ClusterServiceRoleInstanceDTO> serviceRoleList = null;
        // 启动服务
        if (CommandType.START_SERVICE.equals(commandType)) {
            serviceRoleList = roleInstanceService
                    .listStoppedServiceRoleListByHostnameAndClusterId(hostname, clusterId);
            // 重启时重刷服务配置以支持磁盘故障等问题
            roleInstanceService.updateToNeedRestartByHost(hostname);
        }

        // 停止运行状态的服务
        if (CommandType.STOP_SERVICE.equals(commandType)) {
            serviceRoleList = roleInstanceService
                    .getServiceRoleListByHostnameAndClusterId(hostname, clusterId).stream()
                    .filter(roleInstance -> (ServiceRoleState.STOP.getValue() != roleInstance.serviceRoleState() &&
                            ServiceRoleState.DECOMMISSIONED.getValue() != roleInstance.serviceRoleState()))
                    .collect(toList());
        }

        if (CollectionUtils.isEmpty(serviceRoleList)) {
            logger.info("No services need to start at host {}.", hostname);
            return;
        }

        // 构建服务角色映射(服务ID -> 角色ID列表)
        Map<Long, List<String>> serviceRoleMap = serviceRoleList.stream()
                .collect(
                        groupingBy(
                                ClusterServiceRoleInstanceDTO::serviceId,
                                mapping(i -> String.valueOf(i.id()), toList())));
        // 生成并执行服务角色命令
        serviceCommandService.generateServiceRoleCommands(clusterId, commandType, serviceRoleMap);
        logger.info("Auto-start services command generated for host: {}", hostname);
    }

    /**
     * 同步集群用户和组到指定主机
     *
     * @param clusterId 集群ID
     * @param hostname  主机名
     */
    private void syncClusterUserAndGroup(Long clusterId, String hostname) {
        ClusterGroupService clusterGroupService = SpringUtil.getBean(ClusterGroupService.class);
        ClusterUserService clusterUserService = SpringUtil.getBean(ClusterUserService.class);

        List<ClusterGroupDTO> userGroupList = clusterGroupService.listAllUserGroup(clusterId);
        for (ClusterGroupDTO clusterGroup : userGroupList) {
            String groupName = clusterGroup.groupName();
            clusterGroupService.createUnixGroupOnHost(hostname, groupName);
        }
        List<ClusterUserDTO> userList = clusterUserService.listAllUser(clusterId);
        for (ClusterUserDTO clusterUser : userList) {
            clusterUserService.createUnixUserOnHost(clusterUser, hostname);
        }
    }
}
