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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KafkaHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

        private static final Logger log = LoggerFactory.getLogger(KafkaHandlerStrategy.class);

        @Override
        public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
                boolean enableKerberos = false;
                boolean enableAcl = false;
                boolean enableDistributed = false;
                boolean enableJmxAcl = false;
                boolean enableSasl;
                Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
                for (ServiceConfig config : list) {
                        if ("enableKerberos".equals(config.getName())) {
                                enableKerberos = isEnableKerberos(clusterId, globalVariables, enableKerberos, config,
                                                "KAFKA");
                        }
                        if ("zookeeper.connect".equals(config.getName())) {
                                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${kafkaZkAddr}",
                                                Convert.toStr(config.getValue()));
                        }
                        if ("cluster1.zk.acl.enable".equals(config.getName())) {
                                enableAcl = isEnableConfig(config);
                        }
                        if ("efak.distributed.enable".equals(config.getName())) {
                                enableDistributed = isEnableConfig(config);
                        }
                        if ("cluster1.efak.jmx.acl".equals(config.getName())) {
                                enableJmxAcl = isEnableConfig(config);
                        }
                        if ("cluster1.efak.sasl.enable".equals(config.getName())) {
                                enableSasl = isEnableConfig(config);
                        }
                        /*
                         * if ("JMX_PORT".equals(config.getName())) {
                         * if (ObjectUtil.isNotEmpty(config.getValue())){
                         * config.setRequired(true);
                         * }
                         * }
                         */
                }

                String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "KAFKA" + Constants.CONFIG;
                List<ServiceConfig> configs = ServiceConfigMap.get(key);
                ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();

                if (enableKerberos) {
                        addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
                        // TODO 当kafka开启kerberos认证时，efak也要开启
                        enableSasl = true;
                        for (ServiceConfig config : list) {
                                if ("cluster1.efak.sasl.enable".equals(config.getName())) {
                                        config.setValue(enableSasl);
                                }
                        }
                } else {
                        removeConfigWithKerberos(list, map, configs);
                        // TODO 当kafka关闭kerberos认证时，efak也要关闭
                        enableSasl = false;
                        for (ServiceConfig config : list) {
                                if ("cluster1.efak.sasl.enable".equals(config.getName())) {
                                        config.setValue(enableSasl);
                                }
                        }
                }

                handleConfig(list, enableAcl, globalVariables, map, configs, "acl");
                handleConfig(list, enableDistributed, globalVariables, map, configs, "efak-ha");
                handleConfig(list, enableJmxAcl, globalVariables, map, configs, "jmx-acl");
                handleConfig(list, enableSasl, globalVariables, map, configs, "sasl");

                list.addAll(kbConfigs);
        }

        private void handleConfig(List<ServiceConfig> list, boolean enableAcl, Map<String, String> globalVariables,
                        Map<String, ServiceConfig> map, List<ServiceConfig> configs, String configType) {
                List<ServiceConfig> toProcessConfigs = new ArrayList<>();
                if (enableAcl) {
                        addConfigWithConfigType(globalVariables, map, configs, toProcessConfigs, configType);
                } else {
                        removeConfigWithConfigType(list, map, configs, configType);
                }
                list.addAll(toProcessConfigs);
        }

        public boolean isEnableConfig(ServiceConfig config) {
                return BooleanUtil.toBoolean(StrUtil.toString(config.getValue()));
        }

        /**
         * 将所有service_ddl.json中configType是acl的配置项加入到当前配置列表
         * isConfigWithAcl判定条件在 service_ddl.json 中设置 cluster1.zk.acl.enable = true
         *
         * @param globalVariables 全局变量
         * @param map             当前前端传入的配置项
         * @param configs         所有service_ddl.json中设置的所有配置项
         * @param aclConfigs      需要添加到当前的配置项
         */
        public void addConfigWithConfigType(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
                        List<ServiceConfig> configs, List<ServiceConfig> aclConfigs, String configType) {
                for (ServiceConfig serviceConfig : configs) {
                        if (StrUtil.equals(serviceConfig.getConfigType(), configType)) {
                                addConfig(globalVariables, map, aclConfigs, serviceConfig);
                        }
                }
        }

        public void removeConfigWithConfigType(List<ServiceConfig> list, Map<String, ServiceConfig> map,
                        List<ServiceConfig> configs, String configType) {
                for (ServiceConfig serviceConfig : configs) {
                        if (StrUtil.equals(serviceConfig.getConfigType(), configType)) {
                                if (map.containsKey(serviceConfig.getName())) {
                                        list.remove(map.get(serviceConfig.getName()));
                                }
                        }
                }
        }

        /**
         * 获取Kafka服务特定的连接信息
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
                        log.info("开始获取Kafka服务连接信息，集群ID: {}, 服务实例ID: {}", clusterId, serviceInstanceId);

                        // 获取Kafka Broker和Zookeeper节点列表
                        List<String> brokerList = getRoleHosts(clusterId, serviceInstanceId, "KafkaBroker");
                        List<String> zkList = getRoleHosts(clusterId, null, "ZkServer");

                        // 如果没有找到Broker或ZooKeeper节点，返回空信息
                        if (CollectionUtils.isEmpty(brokerList) || CollectionUtils.isEmpty(zkList)) {
                                log.warn("未找到Kafka Broker或ZooKeeper节点，集群ID: {}", clusterId);
                                return ConnectionInfo.builder();
                        }

                        log.info("Kafka Broker节点数量: {}, ZooKeeper节点数量: {}", brokerList.size(), zkList.size());

                        // 判断是否启用了Kerberos
                        boolean enableKerberos = false;
                        // 从配置映射中获取Kerberos启用状态
                        if (configMap.containsKey("enableKerberos")) {
                                enableKerberos = Boolean.parseBoolean(configMap.get("enableKerberos"));
                        }

                        // 获取Kafka端口，默认为9092
                        String kafkaPort = configMap.getOrDefault("port", "9092");

                        // 构建Kafka Broker连接字符串
                        StringBuilder kafkaConnectString = new StringBuilder();
                        for (int i = 0; i < brokerList.size(); i++) {
                                String broker = brokerList.get(i);
                                kafkaConnectString.append(broker).append(":").append(kafkaPort);
                                if (i < brokerList.size() - 1) {
                                        kafkaConnectString.append(",");
                                }
                        }

                        // 获取ZooKeeper端口和路径
                        String zkPort = "2181";
                        String zkPath = "/kafka";

                        // 从zookeeper.connect配置中提取路径信息（如果有）
                        String zkConnect = configMap.getOrDefault("zookeeper.connect", "");
                        if (StrUtil.isNotBlank(zkConnect)) {
                                // 如果配置中包含路径信息，提取出来
                                if (zkConnect.contains("/")) {
                                        int lastSlashIndex = zkConnect.lastIndexOf('/');
                                        zkPath = zkConnect.substring(lastSlashIndex);
                                }
                        }

                        // 构建ZooKeeper连接字符串
                        StringBuilder zkConnectString = new StringBuilder();
                        for (int i = 0; i < zkList.size(); i++) {
                                String zk = zkList.get(i);
                                zkConnectString.append(zk).append(":").append(zkPort);
                                if (i < zkList.size() - 1) {
                                        zkConnectString.append(",");
                                }
                        }
                        // 添加路径
                        zkConnectString.append(zkPath);

                        // 获取第一个Kafka节点作为主机名
                        String primaryHostName = brokerList.isEmpty() ? "localhost" : brokerList.getFirst();

                        // 构建基本信息项列表
                        List<InfoItem> basicInfoItems = new ArrayList<>();
                        basicInfoItems.add(new InfoItem("host", "主机", primaryHostName));
                        basicInfoItems.add(new InfoItem("port", "端口", kafkaPort));

                        // 构建安全信息项列表
                        List<InfoItem> securityInfoItems = new ArrayList<>();
                        securityInfoItems.add(new InfoItem("kerberos", "启用Kerberos", enableKerberos ? "是" : "否"));

                        if (enableKerberos) {
                                // 如果启用了Kerberos，添加相关安全配置
                                String saslJaasConfig = configMap.getOrDefault("sasl.jaas.config", "");
                                String securityProtocol = configMap.getOrDefault("security.protocol", "SASL_PLAINTEXT");
                                String saslMechanism = configMap.getOrDefault("sasl.mechanism", "GSSAPI");

                                securityInfoItems.add(new InfoItem("securityProtocol", "安全协议", securityProtocol));
                                securityInfoItems.add(new InfoItem("saslMechanism", "SASL机制", saslMechanism));
                                if (!saslJaasConfig.isEmpty()) {
                                        securityInfoItems.add(new InfoItem("saslJaasConfig", "JAAS配置", saslJaasConfig));
                                }
                        }

                        // 构建连接信息项列表
                        List<InfoItem> connectInfoItems = new ArrayList<>();
                        connectInfoItems.add(
                                        new InfoItem("bootstrapServers", "Kafka集群地址", kafkaConnectString.toString()));
                        connectInfoItems.add(new InfoItem("zkConnect", "ZooKeeper地址", zkConnectString.toString()));

                        // 构建重要键列表
                        List<String> importantKeys = Arrays.asList("bootstrapServers", "zkConnect");

                        // 返回连接信息构建器
                        log.info("Kafka连接信息生成成功");
                        return ConnectionInfo.builder()
                                        .basicInfoItems(basicInfoItems)
                                        .securityInfoItems(securityInfoItems)
                                        .connectInfoItems(connectInfoItems)
                                        .hostName(primaryHostName)
                                        .importantKeys(importantKeys);
                } catch (Exception e) {
                        log.error("获取Kafka连接信息出错: {}", e.getMessage(), e);
                        return ConnectionInfo.builder();
                }
        }

}
