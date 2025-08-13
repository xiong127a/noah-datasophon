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

package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HBaseHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(HBaseHandlerStrategy.class);

    @Override
    public void handlerConfig(Long clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        boolean enableKerberos = false;
        Map<String, ServiceConfig> map = list.stream()
                .collect(java.util.stream.Collectors.toMap(ServiceConfig::getName, config -> config));
        for (ServiceConfig config : list) {
            if ("enableKerberos".equals(config.getName())) {
                enableKerberos = isEnableKerberos(clusterId, globalVariables, enableKerberos, config, "HBASE");
            }
        }
        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "HBASE" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);
        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
        if (enableKerberos) {
            addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
        } else {
            removeConfigWithKerberos(list, map, configs);
        }
        list.addAll(kbConfigs);
    }

    /**
     * 获取HBase服务特定的连接信息
     *
     * @param clusterId         集群ID
     * @param serviceInstanceId 服务实例ID
     * @param configMap         配置映射
     * @return ConnectionInfo.ConnectionInfoBuilder 包含服务特定信息的构建器
     */
    @Override
    protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
            Long clusterId, Integer serviceInstanceId, Map<String, String> configMap) {
        try {
            logger.info("开始获取HBase服务连接信息，集群ID: {}, 服务实例ID: {}", clusterId, serviceInstanceId);

            // 获取HBase Master和RegionServer节点列表
            List<String> masterList = getRoleHosts(clusterId, serviceInstanceId, "HbaseMaster");
            List<String> regionServerList = getRoleHosts(clusterId, serviceInstanceId, "RegionServer");

            // 如果没有找到Master节点，返回空信息
            if (CollUtil.isEmpty(masterList)) {
                logger.warn("未找到HBase Master节点，集群ID: {}", clusterId);
                return ConnectionInfo.builder();
            }

            logger.info("HBase主节点数量: {}, RegionServer节点数量: {}", masterList.size(), regionServerList.size());

            // 获取第一个Master节点作为主节点
            String masterNode = masterList.getFirst();
            String masterPort = configMap.getOrDefault("hbase.master.port", "16000");
            logger.info("HBase主节点: {}:{}", masterNode, masterPort);

            // 判断是否启用了Kerberos
            boolean enableKerberos = false;
            if (configMap.containsKey("enableKerberos")) {
                enableKerberos = Boolean.parseBoolean(configMap.get("enableKerberos"));
            }

            // 获取ZooKeeper连接信息
            String zkQuorum = configMap.getOrDefault("hbase.zookeeper.quorum",
                    GlobalVariables.get(clusterId).getOrDefault("ZOOKEEPER_NODES", ""));
            String zkPort = configMap.getOrDefault("hbase.zookeeper.property.clientPort", "2181");
            String zkRootNode = configMap.getOrDefault("zookeeper.znode.parent", "/hbase");

            // 判断是否启用了高可用
            boolean isHA;

            // 检查是否启用了分布式模式
            boolean isDistributed = "true"
                    .equalsIgnoreCase(configMap.getOrDefault("hbase.cluster.distributed", "false"));

            // 检查ZooKeeper集群是否配置了多个节点
            boolean hasMultipleZK = zkQuorum.contains(",");

            // 检查是否配置了多个Master节点
            boolean hasMultipleMasters = masterList.size() > 1;

            // 检查是否显式启用了Master高可用
            boolean masterHAEnabled = "true"
                    .equalsIgnoreCase(configMap.getOrDefault("hbase.master.ha.enable", "false"));

            // 检查是否启用了跨集群复制
            boolean replicationEnabled = "true".equalsIgnoreCase(configMap.getOrDefault("hbase.replication", "false"));

            // 综合判断是否高可用
            isHA = isDistributed && (hasMultipleMasters || masterHAEnabled || replicationEnabled);

            // 记录高可用判断的详细信息
            logger.info("HBase高可用判断: 分布式模式={}, 多ZK节点={}, 多Master节点={}, Master高可用={}, 跨集群复制={}, 最终结果={}",
                    isDistributed, hasMultipleZK, hasMultipleMasters, masterHAEnabled, replicationEnabled, isHA);

            // 构建基本信息项列表
            List<InfoItem> basicInfoItems = new ArrayList<>();
            basicInfoItems.add(new InfoItem("host", "主机", masterNode));
            basicInfoItems.add(new InfoItem("port", "端口", masterPort));
            basicInfoItems.add(new InfoItem("zkQuorum", "ZooKeeper集群地址", zkQuorum));
            basicInfoItems.add(new InfoItem("zkPort", "ZooKeeper客户端端口", zkPort));
            basicInfoItems.add(new InfoItem("zkRootNode", "ZooKeeper根节点", zkRootNode));
            basicInfoItems.add(new InfoItem("highAvailability", "高可用", isHA ? "是" : "否"));

            // 如果有多个Master节点，添加Master节点列表
            if (masterList.size() > 1) {
                String masterNodes = String.join(",", masterList);
                basicInfoItems.add(new InfoItem("masterNodes", "Master节点列表", masterNodes));
            }

            // 添加RegionServer节点数量信息
            if (!regionServerList.isEmpty()) {
                basicInfoItems.add(new InfoItem("regionServerCount", "RegionServer节点数量",
                        String.valueOf(regionServerList.size())));
            }

            // 构建安全信息项列表
            List<InfoItem> securityInfoItems = new ArrayList<>();
            securityInfoItems.add(new InfoItem("kerberos", "启用Kerberos", enableKerberos ? "是" : "否"));
            if (enableKerberos) {
                String masterPrincipal = configMap.getOrDefault("hbase.master.kerberos.principal",
                        "hbase/_HOST@EXAMPLE.COM");
                String regionServerPrincipal = configMap.getOrDefault("hbase.regionserver.kerberos.principal",
                        "hbase/_HOST@EXAMPLE.COM");
                securityInfoItems.add(new InfoItem("masterPrincipal", "Master Kerberos主体", masterPrincipal));
                securityInfoItems
                        .add(new InfoItem("regionServerPrincipal", "RegionServer Kerberos主体", regionServerPrincipal));
            }

            // 构建连接信息项列表
            List<InfoItem> connectInfoItems = new ArrayList<>();

            // HBase API 配置 (ZooKeeper连接信息)
            String hbaseAPI = zkQuorum + ":" + zkPort + zkRootNode;
            connectInfoItems.add(new InfoItem("hbaseAPI", "HBase API", hbaseAPI));

            // JDBC URL (Phoenix)
            String jdbcUrl = "jdbc:phoenix:" + zkQuorum + ":" + zkPort + ":" + zkRootNode;
            connectInfoItems.add(new InfoItem("jdbcUrl", "Phoenix JDBC URL", jdbcUrl));

            // 构建重要键列表
            List<String> importantKeys = Arrays.asList("hbaseAPI", "jdbcUrl");

            // 返回连接信息构建器
            logger.info("HBase连接信息生成成功");
            return ConnectionInfo.builder()
                    .basicInfoItems(basicInfoItems)
                    .securityInfoItems(securityInfoItems)
                    .connectInfoItems(connectInfoItems)
                    .hostName(masterNode)
                    .importantKeys(importantKeys);
        } catch (Exception e) {
            logger.error("获取HBase连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder();
        }
    }

}
