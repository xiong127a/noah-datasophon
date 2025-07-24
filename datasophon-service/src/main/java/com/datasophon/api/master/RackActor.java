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

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.command.GenerateRackPropCommand;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RackActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(RackActor.class);

    // 定义常量，避免魔法值
    private static final String NAME_NODE_ROLE = "NameNode";
    private static final String HDFS_SERVICE = "HDFS";
    private static final String RACK_PROPERTIES = "rack.properties";
    private static final String HADOOP_CONFIG_DIR = "etc/hadoop";
    private static final String PROPERTIES2_FORMAT = "properties2";
    private static final String SLASH = "/";

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GenerateRackPropCommand.class, this::generateRackProperties)
                .matchAny(this::unhandled)
                .build();
    }

    /**
     * 生成机架配置文件
     * 
     * @param command 生成机架属性命令
     */
    private void generateRackProperties(GenerateRackPropCommand command) throws Exception {
        // 获取所需服务
        ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        ClusterHostService hostService = SpringUtil.getBean(ClusterHostService.class);
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);

        // 查询集群NameNode角色实例
        List<ClusterServiceRoleInstanceEntity> nameNodes = roleInstanceService
                .getServiceRoleInstanceListByClusterIdAndRoleName(command.getClusterId(), NAME_NODE_ROLE);

        // 获取集群信息
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(command.getClusterId());

        // 构建配置文件映射
        Map<Generators, List<ServiceConfig>> configFileMap = buildRackConfigFileMap(hostService.list());

        // 为每个NameNode生成rack.properties文件
        for (ClusterServiceRoleInstanceEntity nameNode : nameNodes) {
            generateRackPropertiesForNode(nameNode, configFileMap, clusterInfo);
        }
    }

    /**
     * 构建机架配置文件映射
     * 
     * @param hostList 主机列表
     * @return 配置文件映射
     */
    private Map<Generators, List<ServiceConfig>> buildRackConfigFileMap(List<ClusterHostDO> hostList) {
        Map<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

        // 创建配置生成器
        Generators generators = new Generators();
        generators.setFilename(RACK_PROPERTIES);
        generators.setOutputDirectory(HADOOP_CONFIG_DIR);
        generators.setConfigFormat(PROPERTIES2_FORMAT);

        // 为每个主机创建机架配置
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (ClusterHostDO host : hostList) {
            ServiceConfig config = ProcessUtils.createServiceConfig(
                    host.getIp(),
                    SLASH + host.getRack(),
                    "input");
            serviceConfigs.add(config);
        }

        configFileMap.put(generators, serviceConfigs);
        return configFileMap;
    }

    /**
     * 为指定节点生成rack.properties文件
     * 
     * @param nameNode      NameNode实例
     * @param configFileMap 配置文件映射
     * @param clusterInfo   集群信息
     */
    private void generateRackPropertiesForNode(ClusterServiceRoleInstanceEntity nameNode,
            Map<Generators, List<ServiceConfig>> configFileMap,
            ClusterInfoEntity clusterInfo) {
        // 构建服务角色信息
        ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setName(NAME_NODE_ROLE);
        serviceRoleInfo.setParentName(HDFS_SERVICE);
        serviceRoleInfo.setConfigFileMap(configFileMap);
        serviceRoleInfo.setDecompressPackageName(
                PackageUtils.getServiceDcPackageName(clusterInfo.getClusterFrame(), HDFS_SERVICE));
        serviceRoleInfo.setHostname(nameNode.getHostname());

        // 生成配置
        ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
        ExecResult execResult = configureHandler.handlerRequest(serviceRoleInfo);

        // 记录结果
        if (!execResult.getExecResult()) {
            logger.error("生成 {} 文件失败", RACK_PROPERTIES);
        }
    }
}
