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

package com.datasophon.kubernetes.strategy;

import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubernetesKerberosUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;

import java.util.List;
import java.util.Objects;

import static com.datasophon.common.Constants.UNDERLINE;

public class KubernetesKafkaHandlerStrategy extends KubernetesAbstractHandlerStrategy
        implements KubernetesServiceRoleStrategy {

    public KubernetesKafkaHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        if (command.getEnableKerberos()) {
            logger.info("开始获取Kafka keytab文件");
            String hostname = command.getHostname();
            KubernetesKerberosUtils.createKeytabDir(hostname);
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/kafka.service.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname, "kafka/" + hostname, "kafka.service.keytab");
            }
        }
        return serviceHandler.start(command);
    }

    @Override
    public void getConfig(Integer clusterId, String namespace, List<ServiceConfig> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("配置列表为空，无法更新服务配置");
            return;
        }

        // 动态获取ZK节点数量 - 直接从缓存获取
        int zkNodeCount = 0;
        String zkNodeCountKey = clusterId + UNDERLINE + "zookeeper_node_count";
        Object zkCountObj = CacheUtils.get(zkNodeCountKey);

        if (Objects.nonNull(zkCountObj)) {
            zkNodeCount = (Integer) zkCountObj;
            logger.info("从缓存 zookeeper_node_count 中获取到ZK节点数量为: {}", zkNodeCount);
        } else {
            logger.warn("缓存中未找到 ZK 节点数 (key: {}), ZK quorum 将为空。", zkNodeCountKey);
        }

        logger.info("开始更新Kafka配置，适配Kubernetes服务...");

        // 处理Kafka NodePort特殊绑定
        // processNodePortMappings(clusterId, list);

        // 定义服务名常量
        final String KAFKA_EFAK_SERVICE = "kafka-efak";
        final String ZOOKEEPER_SERVICE = "zookeeper-zkserver";

        // 当前服务角色名称
        // namespace已通过参数传入，无需查询
        // 遍历所有配置
        for (ServiceConfig config : list) {
            String name = config.getName();
            Object value = config.getValue();

            // 处理Kafka相关配置
            switch (name) {
                case "zookeeper.connect": {
                    // 构建ZooKeeper服务地址列表，使用完整的FQDN格式
                    StringBuilder zkServers = new StringBuilder();
                    for (int i = 0; i < zkNodeCount; i++) { // 使用动态获取的zkNodeCount
                        if (i > 0) {
                            zkServers.append(",");
                        }
                        zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                                .append(".").append(ZOOKEEPER_SERVICE).append(".")
                                .append(namespace).append(".")
                                .append(CLUSTER_DOMAIN).append(":2181");
                    }
                    zkServers.append("/kafka");
                    config.setValue(zkServers.toString());
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "cluster1.zk.list": {
                    // EFAK连接的ZooKeeper服务地址列表，使用完整的FQDN格式
                    StringBuilder zkServers = new StringBuilder();
                    for (int i = 0; i < zkNodeCount; i++) { // 使用动态获取的zkNodeCount
                        if (i > 0) {
                            zkServers.append(",");
                        }
                        zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                                .append(".").append(ZOOKEEPER_SERVICE).append(".")
                                .append(namespace).append(".")
                                .append(CLUSTER_DOMAIN).append(":2181");
                    }
                    zkServers.append("/kafka");
                    config.setValue(zkServers.toString());
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "advertised.listeners": {
                    // 对外广播地址使用服务名称
                    String protocol = value != null && value.toString().startsWith("SASL_") ? "SASL_PLAINTEXT"
                            : "PLAINTEXT";

                    // 使用NodePort方式暴露给外部客户端访问
                    // 格式为 Kubernetes宿主机IP:NodePort，这里使用占位符，将在Pod启动时替换为实际IP
                    // 保留9092端口，在Pod启动时会根据实际NodePort进行替换
                    String newValue = protocol + "://${hostname}:9092,EXTERNAL://{{EXTERNAL_IP}}:9093";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "listeners": {
                    String protocol = value != null && value.toString().startsWith("SASL_") ? "SASL_PLAINTEXT"
                            : "PLAINTEXT";
                    String newValue = protocol + "://${hostname}:9092,EXTERNAL://0.0.0.0:9093";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "efak.webui.port": {
                    // EFAK Web UI端口，Kubernetes环境不需要更改，保留原值
                    break;
                }
                case "efak.worknode.master.host": {
                    // EFAK主节点地址，使用服务名称
                    String newValue = KAFKA_EFAK_SERVICE + "." + namespace + "." + CLUSTER_DOMAIN;
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
            }
        }

        logger.info("Kafka配置更新完成，已适配Kubernetes服务，所有服务地址均使用完整FQDN格式");
    }
}
