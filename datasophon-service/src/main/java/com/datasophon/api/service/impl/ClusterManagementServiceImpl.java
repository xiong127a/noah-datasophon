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

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONArray;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.api.service.*;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ClusterCommand;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.ClusterCommandType;
import com.datasophon.common.enums.ClusterState;
import com.datasophon.common.enums.ServiceRoleState;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 集群管理服务实现
 * 替代ClusterActor，负责集群状态检查和集群删除操作
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-30
 */
@Service
public class ClusterManagementServiceImpl implements ClusterManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterManagementServiceImpl.class);

    private static final String DEPRECATED = "Deprecated";

    @Autowired
    @Lazy
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterHostService clusterHostService;

    @Autowired
    private ClusterServiceInstanceService clusterServiceInstanceService;

    @Autowired
    private ClusterServiceRoleGroupConfigService clusterServiceRoleGroupConfigService;

    @Autowired
    private ClusterServiceRoleGroupConfigConverter configConverter;

    @Override
    @Async("taskExecutor")
    public void handleClusterCommand(ClusterCommand command) {
        try {
            logger.info("ClusterManagementService 接收到命令: commandType={}", command.getCommandType());

            if (ClusterCommandType.CHECK.equals(command.getCommandType())) {
                checkAllClustersState();
            } else if (ClusterCommandType.DELETE.equals(command.getCommandType())) {
                deleteCluster(command.getClusterId());
            } else {
                logger.warn("未知的集群命令类型: {}", command.getCommandType());
            }

        } catch (Exception e) {
            logger.error("处理集群命令时发生错误", e);
        }
    }

    /**
     * 检查所有集群的状态
     * 根据服务角色实例的状态更新集群状态（RUNNING/STOP）
     */
    private void checkAllClustersState() {
        try {
            logger.debug("开始检查所有集群状态");

            // 获取所有集群
            List<ClusterInfoDTO> clusterList = clusterInfoService.getClusterList();

            for (ClusterInfoDTO clusterInfo : clusterList) {
                Long clusterId = clusterInfo.id();

                // 跳过待配置状态的集群
                if (ClusterState.NEED_CONFIG.getValue() == clusterInfo.clusterState()) {
                    continue;
                }

                // 获取集群上正在运行的服务角色实例
                List<ClusterServiceRoleInstanceDTO> roleInstanceList = 
                        roleInstanceService.getServiceRoleInstanceListByClusterId(clusterId);

                if (roleInstanceList.isEmpty()) {
                    continue;
                }

                // 检查所有服务角色实例的状态
                boolean allStopped = roleInstanceList.stream()
                        .allMatch(roleInstance -> Objects.equals(
                                ServiceRoleState.STOP.getValue(), 
                                roleInstance.serviceRoleState()));

                if (allStopped) {
                    // 所有服务都停止了，更新集群状态为STOP
                    boolean result = clusterInfoService.updateClusterState(clusterId, ClusterState.STOP.getValue());
                    if (result) {
                        logger.info("集群 {} 状态已更新为 STOP", clusterId);
                    } else {
                        logger.warn("更新集群 {} 状态为 STOP 失败", clusterId);
                    }
                } else {
                    // 至少有一个服务在运行，更新集群状态为RUNNING
                    boolean result = clusterInfoService.updateClusterState(clusterId, ClusterState.RUNNING.getValue());
                    if (result) {
                        logger.debug("集群 {} 状态已更新为 RUNNING", clusterId);
                    } else {
                        logger.warn("更新集群 {} 状态为 RUNNING 失败", clusterId);
                    }
                }
            }

            logger.debug("所有集群状态检查完成");

        } catch (Exception e) {
            logger.error("检查集群状态时发生错误", e);
        }
    }

    /**
     * 删除集群
     * 1. 重命名所有服务实例的目录（添加DEPRECATED后缀）
     * 2. 删除所有服务实例
     * 3. 删除所有主机
     * 4. 删除集群信息
     */
    private void deleteCluster(Long clusterId) {
        try {
            if (clusterId == null) {
                logger.warn("集群ID为空，无法删除");
                return;
            }

            logger.info("开始删除集群: {}", clusterId);

            // 获取集群信息
            ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(clusterId);
            if (clusterInfo == null) {
                logger.warn("集群 {} 不存在", clusterId);
                return;
            }

            // 处理所有服务角色实例的目录重命名
            List<ClusterServiceRoleInstanceDTO> roleInstanceList = 
                    roleInstanceService.getServiceRoleInstanceListByClusterId(clusterId);

            for (ClusterServiceRoleInstanceDTO roleInstance : roleInstanceList) {
                try {
                    processServiceRoleUninstall(roleInstance, clusterId);
                } catch (Exception e) {
                    logger.error("处理服务角色 {} 卸载失败: {}", 
                            roleInstance.serviceRoleName(), e.getMessage(), e);
                    // 继续处理其他服务角色
                }
            }

            // 删除所有服务实例
            List<ClusterServiceInstanceDTO> serviceInstanceList = 
                    clusterServiceInstanceService.listAll(clusterId);

            boolean allInstancesDeleted = true;
            for (ClusterServiceInstanceDTO instance : serviceInstanceList) {
                try {
                    clusterServiceInstanceService.delServiceInstance(instance.id());
                    logger.info("成功删除服务实例: {}", instance.id());
                } catch (Exception e) {
                    logger.error("删除服务实例 {} 失败", instance.id(), e);
                    allInstancesDeleted = false;
                    break;
                }
            }

            // 只有在所有服务实例都删除成功后，才删除主机和集群
            if (allInstancesDeleted) {
                // 删除所有受管主机
                List<ClusterHostEntity> hostList = clusterHostService.getHostListByClusterIdAndManaged(clusterId);
                if (!hostList.isEmpty()) {
                    String hostIds = hostList.stream()
                            .map(h -> String.valueOf(h.getId()))
                            .collect(Collectors.joining(Constants.COMMA));
                    clusterHostService.deleteHosts(hostIds);
                    logger.info("成功删除集群 {} 的所有主机", clusterId);
                }

                // 删除集群信息
                try {
                    clusterInfoService.deleteCluster(java.util.Collections.singletonList(clusterId));
                    logger.info("成功删除集群: {}", clusterId);
                } catch (Exception e) {
                    logger.error("删除集群 {} 失败", clusterId, e);
                }
            } else {
                logger.warn("部分服务实例删除失败，中止集群 {} 删除操作", clusterId);
            }

        } catch (Exception e) {
            logger.error("删除集群 {} 时发生错误", clusterId, e);
        }
    }

    /**
     * 处理服务角色卸载（目录重命名）
     */
    private void processServiceRoleUninstall(ClusterServiceRoleInstanceDTO roleInstance, Long clusterId) {
        String roleName = roleInstance.serviceRoleName();
        String hostname = roleInstance.hostname();

        // 获取角色组配置
        ClusterServiceRoleGroupConfigDTO configDto = 
                clusterServiceRoleGroupConfigService.getConfigByRoleGroupId(roleInstance.roleGroupId());

        if (configDto == null) {
            logger.warn("服务角色 {} 的配置不存在，跳过卸载", roleName);
            return;
        }

        // 转换配置
        ClusterServiceRoleGroupConfigEntity config = configConverter.dtoToEntity(configDto);
        Map<Generators, List<ServiceConfig>> configFileMap = new ConcurrentHashMap<>();
        ConfigGroupUtils.generateConfigFileMap(configFileMap, config, clusterId);

        // 处理路径配置：添加DEPRECATED后缀
        for (Map.Entry<Generators, List<ServiceConfig>> configFile : configFileMap.entrySet()) {
            List<ServiceConfig> serviceConfigs = configFile.getValue().stream()
                    .filter(c -> Constants.PATH.equals(c.getConfigType()))
                    .peek(c -> {
                        if (Constants.INPUT.equals(c.getType())) {
                            // 单个路径
                            String oldPath = (String) c.getValue();
                            if (!oldPath.contains(DEPRECATED)) {
                                String newPath = String.format("%s_%s_%s_%s", 
                                        oldPath, DEPRECATED, clusterId, DateUtil.today());
                                c.setValue(newPath);
                                c.setConfigType(Constants.MV_PATH);
                            }
                        } else if (Constants.MULTIPLE.equals(c.getType())) {
                            // 多个路径
                            JSONArray value = (JSONArray) c.getValue();
                            List<String> oldPaths = value.toJavaList(String.class);
                            List<String> newPaths = oldPaths.stream()
                                    .map(path -> !path.contains(DEPRECATED)
                                            ? String.format("%s_%s_%s_%s", 
                                                    path, DEPRECATED, clusterId, DateUtil.today())
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
            logger.info("开始卸载 {} 在主机 {}，共 {} 个配置文件需要处理", 
                    roleName, hostname, configFileMap.size());

            // TODO: 需要通过HTTP REST API分发重命名命令到Worker
            // 目前记录卸载信息，等待HTTP Worker实现完成后补充

            logger.info("{} 卸载完成在 {}", roleName, hostname);
        }
    }
}

