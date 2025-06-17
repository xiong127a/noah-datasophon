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

package com.datasophon.k8s.strategy;

import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sKerberosUtils;
import com.datasophon.k8s.util.K8sMinaUtils;

import java.io.IOException;
import java.util.List;

public class K8sKafkaHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sKafkaHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws IOException {
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        if (command.getEnableKerberos()) {
            logger.info("开始获取Kafka keytab文件");
            String hostname = command.getHostname();
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/kafka.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "kafka/" + hostname, "kafka.service.keytab");
            }
        }
        return serviceHandler.start(command);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("配置列表为空，无法更新服务配置");
            return;
        }
        logger.info("开始更新Kafka配置，适配Kubernetes服务...");

        // 处理Kafka NodePort特殊绑定
        processNodePortMappings(clusterId, list);

        // 定义服务名常量
        final String KAFKA_EFAK_SERVICE = "kafka-efak";
        final String ZOOKEEPER_SERVICE = "zookeeper-zkserver";

        // 当前服务角色名称

        // 遍历所有配置
        for (ServiceConfig config : list) {
            String name = config.getName();
            Object value = config.getValue();

            // 处理Kafka相关配置
            switch (name) {
                case "zookeeper.connect": {
                    // 构建ZooKeeper服务地址列表，使用完整的FQDN格式
                    StringBuilder zkServers = new StringBuilder();
                    for (int i = 0; i < 3; i++) { // 假设3个ZooKeeper节点
                        if (i > 0) {
                            zkServers.append(",");
                        }
                        zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                                .append(".").append(ZOOKEEPER_SERVICE).append(".")
                                .append(NAMESPACE).append(".")
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
                    for (int i = 0; i < 3; i++) { // 假设3个ZooKeeper节点
                        if (i > 0) {
                            zkServers.append(",");
                        }
                        zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                                .append(".").append(ZOOKEEPER_SERVICE).append(".")
                                .append(NAMESPACE).append(".")
                                .append(CLUSTER_DOMAIN).append(":2181");
                    }
                    config.setValue(zkServers.toString());
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "advertised.listeners": {
                    // 对外广播地址使用服务名称
                    String protocol = value != null && value.toString().startsWith("SASL_") ? "SASL_PLAINTEXT"
                            : "PLAINTEXT";

                    // 使用NodePort方式暴露给外部客户端访问
                    // 格式为 K8S宿主机IP:NodePort，这里使用占位符，将在Pod启动时替换为实际IP
                    // 保留9092端口，在Pod启动时会根据实际NodePort进行替换
                    String newValue = protocol + "://${hostname}:9092";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "efak.webui.port": {
                    // EFAK Web UI端口，K8S环境不需要更改，保留原值
                    break;
                }
                case "efak.worknode.master.host": {
                    // EFAK主节点地址，使用服务名称
                    String newValue = KAFKA_EFAK_SERVICE + "." + NAMESPACE + "." + CLUSTER_DOMAIN;
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
            }
        }

        logger.info("Kafka配置更新完成，已适配Kubernetes服务，所有服务地址均使用完整FQDN格式");
    }
}
