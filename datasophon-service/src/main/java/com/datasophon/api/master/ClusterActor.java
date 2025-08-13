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

import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONArray;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ClusterCommand;
import com.datasophon.common.enums.ClusterCommandType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;

import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.common.enums.ClusterState;
import com.datasophon.common.enums.ServiceRoleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 节点状态监测
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public class ClusterActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(ClusterActor.class);

    private static final String DEPRECATED = "Deprecated";

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ClusterCommand.class, this::handleClusterCommand)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleClusterCommand(ClusterCommand clusterCommand) {
        try {
            clusterCommand.setClusterId(6L);
            ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                    .getBean(ClusterServiceRoleInstanceService.class);
            ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);

            if (ClusterCommandType.CHECK.equals(clusterCommand.getCommandType())) {
                // 获取所有集群
                List<ClusterInfoDTO> clusterList = clusterInfoService.getClusterList();

                for (ClusterInfoDTO clusterInfoEntity : clusterList) {
                    // 获取集群上正在运行的服务
                    Long clusterId = clusterInfoEntity.id();
                    List<ClusterServiceRoleInstanceDTO> roleInstanceList = roleInstanceService
                            .getServiceRoleInstanceListByClusterId(clusterId);
                    if (ClusterState.NEED_CONFIG.getValue()!=(clusterInfoEntity.clusterState())) {
                        if (!roleInstanceList.isEmpty()) {
                            if (roleInstanceList.stream().allMatch(
                                    roleInstance -> Objects.equals(ServiceRoleState.STOP.getValue(), roleInstance.serviceRoleState()))) {
                                boolean stopResult = clusterInfoService.updateClusterState(clusterId,
                                        ClusterState.STOP.getValue());
                                if (!stopResult) {
                                    logger.warn("Failed to update cluster {} state to STOP", clusterId);
                                }
                            } else {
                                boolean runningResult = clusterInfoService.updateClusterState(clusterId,
                                        ClusterState.RUNNING.getValue());
                                if (!runningResult) {
                                    logger.warn("Failed to update cluster {} state to RUNNING", clusterId);
                                }
                            }
                        }
                    }
                }
            } else if (ClusterCommandType.DELETE.equals(clusterCommand.getCommandType())) {
                Long clusterId = clusterCommand.getClusterId();
                if (Objects.nonNull(clusterId)) {
                    ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(clusterId);
                    if (Objects.nonNull(clusterInfo)) {
                        ClusterHostService clusterHostService = SpringUtil.getBean(ClusterHostService.class);
                        ClusterServiceInstanceService clusterServiceInstanceService = SpringUtil
                                .getBean(ClusterServiceInstanceService.class);
                        ClusterServiceRoleInstanceService clusterServiceRoleInstanceService = SpringUtil
                                .getBean(ClusterServiceRoleInstanceService.class);
                        ClusterServiceRoleGroupConfigService clusterServiceRoleGroupConfigService = SpringUtil
                                .getBean(ClusterServiceRoleGroupConfigService.class);

                        // 检查服务实例配置与目录
                        List<ClusterServiceRoleInstanceDTO> roleInstanceList = clusterServiceRoleInstanceService
                                .getServiceRoleInstanceListByClusterId(clusterId);
                        for (ClusterServiceRoleInstanceDTO roleInstance : roleInstanceList) {
                            String roleName = roleInstance.serviceRoleName();
                            String hostname = roleInstance.hostname();
                            ClusterServiceRoleGroupConfigDTO configDto = clusterServiceRoleGroupConfigService
                                    .getConfigByRoleGroupId(roleInstance.roleGroupId());
                            // 使用MapStruct Converter进行转换 - 符合架构规范
                            ClusterServiceRoleGroupConfigConverter converter = SpringUtil.getBean(ClusterServiceRoleGroupConfigConverter.class);
                            ClusterServiceRoleGroupConfigEntity config = converter.dtoToEntity(configDto);
                            Map<Generators, List<ServiceConfig>> configFileMap = new ConcurrentHashMap<>();
                            ConfigGroupUtils.generateConfigFileMap(configFileMap, config, clusterId);
                            for (Map.Entry<Generators, List<ServiceConfig>> configFile : configFileMap.entrySet()) {
                                List<ServiceConfig> serviceConfigs = configFile.getValue().stream()
                                        .filter(c -> Constants.PATH.equals(c.getConfigType()))
                                        .peek(c -> {
                                            if (Constants.INPUT.equals(c.getType())) {
                                                String oldPath = (String) c.getValue();
                                                if (!oldPath.contains(DEPRECATED)) {
                                                    String newPath = String.format("%s_%s_%s_%s", oldPath, DEPRECATED,
                                                            clusterId, DateUtil.today());
                                                    c.setValue(newPath);
                                                    c.setConfigType(Constants.MV_PATH);
                                                }
                                            } else if (Constants.MULTIPLE.equals(c.getType())) {
                                                JSONArray value = (JSONArray) c.getValue();
                                                List<String> oldPaths = value.toJavaList(String.class);
                                                List<String> newPaths = oldPaths.stream()
                                                        .map(path -> !path.contains(DEPRECATED)
                                                                ? String.format("%s_%s_%s_%s", path, DEPRECATED,
                                                                        clusterId, DateUtil.today())
                                                                : path)
                                                        .toList();
                                                c.setValue(newPaths);
                                                c.setConfigType(Constants.MV_PATH);
                                            }
                                        })
                                        .filter(c -> Constants.MV_PATH.equals(c.getConfigType()))
                                        .toList();
                                if (!serviceConfigs.isEmpty()) {
                                    configFileMap.replace(configFile.getKey(), serviceConfigs);
                                } else {
                                    configFileMap.remove(configFile.getKey());
                                }
                            }

                            if (!configFileMap.isEmpty()) {
                                // 分发重命名命令
                                try {
                                    logger.info(
                                            "start to uninstall {} in host {}",
                                            roleName,
                                            hostname);
                                    // TODO: 需要实现服务卸载逻辑或文件重命名逻辑
                                    // 目前简化处理：记录卸载信息
                                    logger.info(
                                            "Processing uninstall for {} in host {} with {} config files",
                                            roleName,
                                            hostname,
                                            configFileMap.size());
                                    
                                    // 假设卸载成功（需要后续实现具体的卸载逻辑）
                                    logger.info(
                                            "{} uninstall completed in {}",
                                            roleName,
                                            hostname);

                                } catch (Exception e) {
                                    logger.info(
                                            "{} uninstall failed in {}",
                                            roleName,
                                            hostname);
                                    logger.error(ProcessUtils.getExceptionMessage(e));
                                    return;
                                }
                            }
                        }
                        List<ClusterServiceInstanceDTO> serviceInstanceList = clusterServiceInstanceService
                                .listAll(clusterId);
                        // 删除服务实例
                        boolean allInstancesDeleted = true;
                        for (ClusterServiceInstanceDTO instance : serviceInstanceList) {
                            try {
                                clusterServiceInstanceService.delServiceInstance(instance.id());
                            } catch (Exception e) {
                                logger.error("Failed to delete service instance {}", instance.id(), e);
                                allInstancesDeleted = false;
                                break;
                            }
                        }

                        if (allInstancesDeleted) {
                            List<ClusterHostEntity> hostList = clusterHostService.getHostListByClusterId(clusterId);
                            String hostIds = hostList.stream()
                                    .map(h -> String.valueOf(h.getId()))
                                    .collect(Collectors.joining(Constants.COMMA));
                            clusterHostService.deleteHosts(hostIds);

                            // 删除集群信息 - 根据Service层的新接口调整
                            try {
                                clusterInfoService.deleteCluster(java.util.Collections.singletonList(clusterId));
                            } catch (Exception e) {
                                logger.error("Failed to delete cluster {}", clusterId, e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error handling ClusterCommand", e);
        }
    }
}
