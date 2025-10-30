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

import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.RackConfigurationService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.command.GenerateRackPropCommand;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 机架配置服务实现
 * 替代RackActor，负责生成HDFS集群的机架感知配置文件(rack.properties)
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-30
 */
@Service
public class RackConfigurationServiceImpl implements RackConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(RackConfigurationServiceImpl.class);

    // 定义常量，避免魔法值
    private static final String NAME_NODE_ROLE = "NameNode";
    private static final String HDFS_SERVICE = "HDFS";
    private static final String RACK_PROPERTIES = "rack.properties";
    private static final String HADOOP_CONFIG_DIR = "etc/hadoop";
    private static final String PROPERTIES2_FORMAT = "properties2";
    private static final String SLASH = "/";

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Override
    @Async("taskExecutor")
    public void generateRackProperties(GenerateRackPropCommand command) {
        try {
            logger.info("开始生成集群 {} 的机架配置文件", command.getClusterId());

            // 查询集群NameNode角色实例
            List<ClusterServiceRoleInstanceDTO> nameNodes = roleInstanceService
                    .getServiceRoleInstanceListByClusterIdAndRoleName(command.getClusterId(), NAME_NODE_ROLE);

            if (nameNodes.isEmpty()) {
                logger.warn("集群 {} 没有找到NameNode实例，跳过机架配置生成", command.getClusterId());
                return;
            }

            // 获取集群信息
            ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(command.getClusterId());

            // 构建配置文件映射
            Map<Generators, List<ServiceConfig>> configFileMap = buildRackConfigFileMap(
                    hostService.getHostListByClusterIdAndManaged(command.getClusterId()));

            // 为每个NameNode生成rack.properties文件
            for (ClusterServiceRoleInstanceDTO nameNode : nameNodes) {
                generateRackPropertiesForNode(nameNode, configFileMap, clusterInfo);
            }

            logger.info("集群 {} 的机架配置文件生成完成，共为 {} 个NameNode生成配置", 
                    command.getClusterId(), nameNodes.size());
        } catch (Exception e) {
            logger.error("生成集群 {} 的机架配置文件失败", command.getClusterId(), e);
        }
    }

    /**
     * 构建机架配置文件映射
     * 为每个主机创建IP到机架的映射配置
     *
     * @param hostList 主机列表
     * @return 配置文件映射
     */
    private Map<Generators, List<ServiceConfig>> buildRackConfigFileMap(List<ClusterHostEntity> hostList) {
        Map<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

        // 创建配置生成器
        Generators generators = new Generators();
        generators.setFilename(RACK_PROPERTIES);
        generators.setOutputDirectory(HADOOP_CONFIG_DIR);
        generators.setConfigFormat(PROPERTIES2_FORMAT);

        // 为每个主机创建机架配置
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (ClusterHostEntity host : hostList) {
            // 格式: IP=/rack_name
            ServiceConfig config = ProcessUtils.createServiceConfig(
                    host.getIp(),
                    SLASH + host.getRack(),
                    "input");
            serviceConfigs.add(config);
        }

        configFileMap.put(generators, serviceConfigs);
        logger.debug("构建机架配置映射，共 {} 个主机配置", serviceConfigs.size());
        return configFileMap;
    }

    /**
     * 为指定NameNode节点生成rack.properties文件
     *
     * @param nameNode      NameNode实例
     * @param configFileMap 配置文件映射
     * @param clusterInfo   集群信息
     */
    private void generateRackPropertiesForNode(ClusterServiceRoleInstanceDTO nameNode,
                                                Map<Generators, List<ServiceConfig>> configFileMap,
                                                ClusterInfoDTO clusterInfo) {
        try {
            logger.info("为NameNode {} 生成机架配置文件", nameNode.hostname());

            // 构建服务角色信息
            ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
            serviceRoleInfo.setName(NAME_NODE_ROLE);
            serviceRoleInfo.setParentName(HDFS_SERVICE);
            serviceRoleInfo.setConfigFileMap(configFileMap);
            serviceRoleInfo.setDecompressPackageName(
                    PackageUtils.getServiceDcPackageName(clusterInfo.clusterFrame(), HDFS_SERVICE));
            serviceRoleInfo.setHostname(nameNode.hostname());

            // 生成配置
            ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
            ExecResult execResult = configureHandler.handlerRequest(serviceRoleInfo);

            // 记录结果
            if (execResult.getExecResult()) {
                logger.info("成功为NameNode {} 生成机架配置文件 {}", nameNode.hostname(), RACK_PROPERTIES);
            } else {
                logger.error("为NameNode {} 生成机架配置文件 {} 失败: {}", 
                        nameNode.hostname(), RACK_PROPERTIES, execResult.getExecOut());
            }
        } catch (Exception e) {
            logger.error("为NameNode {} 生成机架配置文件时发生异常", nameNode.hostname(), e);
        }
    }
}

