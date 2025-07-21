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
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ZkServerHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

        private static final Logger logger = LoggerFactory.getLogger(ZkServerHandlerStrategy.class);

        @Override
        public void handler(Integer clusterId, List<String> hosts) {
                // 保存zkUrls到全局变量
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                String join = String.join(":2181,", hosts);
                String zkUrls = join + ":2181";
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${zkUrls}", zkUrls);
                // 保存hbaseZkUrls到全局变量
                String hbaseZkUrls = String.join(",", hosts);
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${zkHostsUrl}", hbaseZkUrls);
        }

        @Override
        public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
                boolean enableKerberos = false;
                Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);

                for (ServiceConfig config : list) {
                        if ("enableKerberos".equals(config.getName())) {
                                if ((Boolean) config.getValue()) {
                                        enableKerberos = true;
                                        ProcessUtils.generateClusterVariable(globalVariables, clusterId,
                                                        "${enableZOOKEEPERKerberos}",
                                                        "true");
                                } else {
                                        ProcessUtils.generateClusterVariable(globalVariables, clusterId,
                                                        "${enableZOOKEEPERKerberos}",
                                                        "false");
                                }
                        }
                }

                String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "ZOOKEEPER" + Constants.CONFIG;
                List<ServiceConfig> configs = ServiceConfigMap.get(key);
                ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
                if (enableKerberos) {
                        for (ServiceConfig serviceConfig : configs) {
                                if (serviceConfig.isConfigWithKerberos()) {
                                        if (map.containsKey(serviceConfig.getName())) {
                                                ServiceConfig config = map.get(serviceConfig.getName());
                                                config.setRequired(true);
                                                config.setHidden(false);
                                                String value = PlaceholderUtils.replacePlaceholders(
                                                                (String) serviceConfig.getValue(),
                                                                globalVariables, Constants.REGEX_VARIABLE);
                                                logger.info("the value is {}", value);
                                                config.setValue(value);
                                        } else {
                                                serviceConfig.setRequired(true);
                                                serviceConfig.setHidden(false);
                                                String value = PlaceholderUtils.replacePlaceholders(
                                                                (String) serviceConfig.getValue(),
                                                                globalVariables, Constants.REGEX_VARIABLE);
                                                serviceConfig.setValue(value);
                                                kbConfigs.add(serviceConfig);
                                        }
                                }
                        }
                } else {
                        for (ServiceConfig serviceConfig : configs) {
                                if (serviceConfig.isConfigWithKerberos()) {
                                        if (map.containsKey(serviceConfig.getName())) {
                                                list.remove(map.get(serviceConfig.getName()));
                                        }
                                }
                        }
                }
                list.addAll(kbConfigs);
        }

        /**
         * @param clusterId 集群ID
         * @param list      服务配置列表
         */
        @Override
        public void getConfig(Integer clusterId, List<ServiceConfig> list) {
                // add server.x config
                ClusterInfoService clusterInfoService = SpringUtil
                                .getBean(ClusterInfoService.class);
                ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

                String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE
                                + Constants.SERVICE_ROLE_HOST_MAPPING;
                // HashMap<String, List<String>> hostMap = (HashMap<String, List<String>>)
                // CacheOperateUtils.get(hostMapKey);
                HashMap<String, List<String>> hostMap = CacheOperateUtils.getWithType(
                                hostMapKey,
                        new TypeReference<>() {
                        });
                if (Objects.nonNull(hostMap)) {
                        List<String> zkServers = hostMap.get("ZkServer");



                        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);

                        Integer myid = 1;
                        for (String server : zkServers) {
                                ServiceConfig serviceConfig = new ServiceConfig();
                                serviceConfig.setName("server." + myid);
                                serviceConfig.setLabel("server." + myid);
                                // TODO:
                                // 在PVM环境中使用域名通信，在Kubernetes中使用DNS域名通信，避免直接使用IP地址。为了提高系统的灵活性和可维护性，因为直接使用IP地址可能会导致在IP变更时需要大量修改配置，而使用域名可以通过DNS解析动态获取IP，减少维护成本。
                                serviceConfig.setValue(server + ":2888:3888");
                                serviceConfig.setHidden(false);
                                serviceConfig.setRequired(true);
                                serviceConfig.setType("input");
                                serviceConfig.setConfigTargetRoles("ZkServer");
                                serviceConfig.setDefaultValue("");
                                serviceConfig.setConfigType("zkserver");
                                serviceConfig.setConfigCategory("role");
                                serviceConfig.setConfigGroup("ZkServer");
                                serviceConfig.setConfigLevel("advanced");


                                if (map.containsKey("server." + myid)) {
                                        logger.info("set zk server {}", myid);
                                        ServiceConfig config = map.get("server." + myid);
                                        BeanUtils.copyProperties(serviceConfig, config);
                                } else {
                                        logger.info("add zk server.x config");
                                        list.add(serviceConfig);
                                }
                                CacheUtils.put("zkserver_" + server, myid);
                                myid++;
                        }
                }
        }

        /**
         * 获取ZooKeeper服务特定的连接信息
         *
         * @param clusterId         集群ID
         * @param serviceInstanceId 服务实例ID
         * @param configMap         配置映射
         * @return ConnectionInfo.ConnectionInfoBuilder 包含服务特定信息的构建器
         */
        @Override
        protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
                        Integer clusterId, Integer serviceInstanceId, Map<String, String> configMap) {
                try {
                        logger.info("开始获取ZooKeeper服务连接信息，集群ID: {}, 服务实例ID: {}", clusterId, serviceInstanceId);

                        // 获取ZooKeeper服务器列表
                        List<String> zkServerList = getRoleHosts(clusterId, serviceInstanceId, "ZkServer");

                        // 如果没有找到ZooKeeper节点，返回空信息
                        if (CollUtil.isEmpty(zkServerList)) {
                                logger.warn("未找到ZooKeeper节点，集群ID: {}", clusterId);
                                return ConnectionInfo.builder();
                        }

                        logger.info("ZooKeeper节点数量: {}", zkServerList.size());

                        // 获取客户端端口配置
                        String clientPort = configMap.getOrDefault("clientPort", "2181");

                        // 获取第一个ZK服务器作为主主机
                        String masterNode = zkServerList.getFirst();
                        logger.info("ZooKeeper主节点: {}:{}", masterNode, clientPort);

                        // 判断是否启用了Kerberos认证
                        boolean enableKerberos = "true"
                                        .equalsIgnoreCase(configMap.getOrDefault("enableKerberos", "false"));

                        // 构建ZooKeeper连接字符串
                        String zkConnectString = zkServerList.stream()
                                        .map(host -> host + ":" + clientPort)
                                        .collect(Collectors.joining(","));

                        logger.info("ZooKeeper连接字符串: {}", zkConnectString);

                        // 判断是否为集群模式
                        boolean isClusterMode = zkServerList.size() > 1;
                        String deployMode = isClusterMode ? "集群模式" : "单节点模式";

                        // 构建基本信息项列表
                        List<InfoItem> basicInfoItems = new ArrayList<>();
                        basicInfoItems.add(new InfoItem("host", "主机", masterNode));
                        basicInfoItems.add(new InfoItem("port", "端口", clientPort));
                        basicInfoItems.add(new InfoItem("connectString", "连接字符串", zkConnectString));
                        basicInfoItems.add(new InfoItem("deployMode", "部署模式", deployMode));

                        // 添加节点信息（合并为一个条目）
                        if (isClusterMode) {
                                // 使用hutool的CollUtil工具类处理集合，提高代码可维护性
                                List<String> nodeInfoList = new ArrayList<>(zkServerList);
                                // 使用hutool的StrUtil工具类进行字符串拼接
                                String serversInfo = cn.hutool.core.util.StrUtil.join(", ", nodeInfoList);
                                basicInfoItems.add(new InfoItem("servers", "服务器节点列表", serversInfo));
                        }

                        // 构建安全信息项列表
                        List<InfoItem> securityInfoItems = new ArrayList<>();
                        securityInfoItems.add(new InfoItem("kerberos", "启用Kerberos", enableKerberos ? "是" : "否"));

                        if (enableKerberos) {
                                // 如果启用了Kerberos，添加相关安全配置
                                String principal = configMap.getOrDefault("kerberos.service.principal",
                                                "zookeeper/_HOST@EXAMPLE.COM");
                                String keytab = configMap.getOrDefault("kerberos.keytab.file",
                                                "/etc/security/keytabs/zookeeper.keytab");
                                securityInfoItems.add(new InfoItem("principal", "Kerberos主体", principal));
                                securityInfoItems.add(new InfoItem("keytab", "Keytab文件路径", keytab));
                        }

                        // 构建连接信息项列表
                        List<InfoItem> connectInfoItems = new ArrayList<>();

                        // CLI命令
                        String cliCommand = zkServerList.getFirst() + ":" + clientPort + " 2>/dev/null";
                        connectInfoItems.add(new InfoItem("connectString", "ZooKeeper连接字符串", zkConnectString));

                        // 构建重要键列表
                        List<String> importantKeys = Arrays.asList("connectString", "cliCommand");

                        if (enableKerberos) {
                                String principal = configMap.getOrDefault("kerberos.service.principal",
                                                "zookeeper/_HOST@EXAMPLE.COM");
                                String keytab = configMap.getOrDefault("kerberos.keytab.file",
                                                "/etc/security/keytabs/zookeeper.keytab");
                        }

                        // 返回连接信息构建器
                        logger.info("ZooKeeper连接信息生成成功");
                        return ConnectionInfo.builder()
                                        .basicInfoItems(basicInfoItems)
                                        .securityInfoItems(securityInfoItems)
                                        .connectInfoItems(connectInfoItems)
                                        .hostName(masterNode)
                                        .importantKeys(importantKeys);
                } catch (Exception e) {
                        logger.error("获取ZooKeeper连接信息出错: {}", e.getMessage(), e);
                        return ConnectionInfo.builder();
                }
        }
}
