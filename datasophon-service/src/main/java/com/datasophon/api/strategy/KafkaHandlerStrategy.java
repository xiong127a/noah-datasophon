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
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
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

    @Override
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId) {
        try {
            // 1. 获取服务配置
            Pair<String, List<ServiceConfig>> pair = listServiceConfigByServiceInstance(serviceInstanceId);
            List<ServiceConfig> serviceConfigs = pair.getValue();
            // 2. 从配置中解析配置到map，方便快速查询
            Map<String, Object> configMap = new HashMap<>();
            for (ServiceConfig config : serviceConfigs) {
                configMap.put(config.getName(), config.getValue());
            }

            // 3. 获取Kafka Broker和Zookeeper节点列表
            List<String> brokerList = getRoleHosts(clusterId, serviceInstanceId, "KafkaBroker");
            List<String> zkList = getRoleHosts(clusterId,null, "ZkServer");

            // 如果没有找到Broker或ZooKeeper节点，返回空信息
            if (CollectionUtils.isEmpty(brokerList) || CollectionUtils.isEmpty(zkList)) {
                log.warn("未找到Kafka Broker或ZooKeeper节点，集群ID: {}", clusterId);
                return ConnectionInfo.builder().build();
            }

            // 5. 判断是否启用了Kerberos
            boolean enableKerberos = false;
            for (ServiceConfig config : serviceConfigs) {
                if ("enableKerberos".equals(config.getName())) {
                    enableKerberos = isEnableConfig(config);
                    break;
                }
            }

            // 6. 获取Kafka端口，默认为9092
            String kafkaPort = "9092";

            // 7. 构建Kafka Broker连接字符串
            StringBuilder kafkaConnectString = new StringBuilder();
            for (int i = 0; i < brokerList.size(); i++) {
                String broker = brokerList.get(i);
                kafkaConnectString.append(broker).append(":").append(kafkaPort);
                if (i < brokerList.size() - 1) {
                    kafkaConnectString.append(",");
                }
            }

            // 8. 获取ZooKeeper端口和路径
            String zkPort = "2181";
            String zkPath = "/kafka";

            // 从zookeeper.connect配置中提取路径信息（如果有）
            String zkConnect = (String) configMap.get("zookeeper.connect");
            if (StrUtil.isNotBlank(zkConnect)) {
                // 如果配置中包含路径信息，提取出来
                if (zkConnect.contains("/")) {
                    int lastSlashIndex = zkConnect.lastIndexOf('/');
                    zkPath = zkConnect.substring(lastSlashIndex);
                }
            }

            // 9. 构建ZooKeeper连接字符串
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

            // 10. 构建基本连接信息
            Map<String, String> basicInfo = new HashMap<>();
            basicInfo.put("kafka_bootstrap_servers", kafkaConnectString.toString());
            basicInfo.put("zookeeper_connect", zkConnectString.toString());
            basicInfo.put("启用Kerberos", enableKerberos ? "true" : "false");

            // 11. 构建有序的基本连接信息列表（用于前端表格显示）
            List<Map<String, String>> basicInfoList = new ArrayList<>();

            // 按照固定的顺序添加信息
            String[] orderedKeys = {
                    "Kafka集群地址",
                    "ZooKeeper地址",
                    "启用Kerberos"
            };

            // 准备数据
            Map<String, String> infoMap = new HashMap<>();
            infoMap.put("Kafka集群地址", kafkaConnectString.toString());
            infoMap.put("ZooKeeper地址", zkConnectString.toString());
            infoMap.put("启用Kerberos", enableKerberos ? "是" : "否");

            // 按顺序添加到basicInfoList
            for (String key : orderedKeys) {
                if (infoMap.containsKey(key)) {
                    Map<String, String> item = new HashMap<>();
                    item.put("label", key);
                    item.put("value", infoMap.get(key));
                    basicInfoList.add(item);
                }
            }

            // 12. 构建Java代码示例
            String javaCode = generateJavaCode(kafkaConnectString.toString(), enableKerberos);

            // 13. 构建Python代码示例
            String pythonCode = generatePythonCode(kafkaConnectString.toString(), enableKerberos);

            // 14. 构建命令行示例
            List<CommandLineItem> commandLines = generateCommandLines(
                    kafkaConnectString.toString(),
                    pair.getKey());

            // 获取第一个Kafka节点作为主机名
            String primaryHostName = brokerList.isEmpty() ? "localhost" : brokerList.get(0);

            // 15. 返回构建好的连接信息
            return ConnectionInfo.builder()
                    .basicInfo(basicInfo)
                    .basicInfoList(basicInfoList)
                    .javaCode(javaCode)
                    .pythonCode(pythonCode)
                    .commandLines(commandLines)
                    .hostName(primaryHostName)
                    .build();
        } catch (Exception e) {
            log.error("获取Kafka连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder().build();
        }
    }

    /**
     * 生成Java示例代码
     *
     * @param kafkaConnectString Kafka连接字符串
     * @param enableKerberos     是否启用Kerberos
     * @return Java代码示例
     */
    private String generateJavaCode(String kafkaConnectString, boolean enableKerberos) {
        StringBuilder code = new StringBuilder();
        code.append("// Kafka Producer 示例代码\n")
                .append("import org.apache.kafka.clients.producer.*;\n")
                .append("import java.util.Properties;\n\n")
                .append("public class KafkaProducerExample {\n")
                .append("    public static void main(String[] args) {\n")
                .append("        Properties props = new Properties();\n")
                .append("        props.put(\"bootstrap.servers\", \"").append(kafkaConnectString)
                .append("\");\n")
                .append("        props.put(\"key.serializer\", \"org.apache.kafka.common.serialization.StringSerializer\");\n")
                .append("        props.put(\"value.serializer\", \"org.apache.kafka.common.serialization.StringSerializer\");\n");

        // 添加Kerberos配置（如果启用）
        if (enableKerberos) {
            code.append("\n        // Kerberos 认证配置\n")
                    .append("        props.put(\"security.protocol\", \"SASL_PLAINTEXT\");\n")
                    .append("        props.put(\"sasl.mechanism\", \"GSSAPI\");\n")
                    .append("        props.put(\"sasl.kerberos.service.name\", \"kafka\");\n");
        }

        code.append("\n        Producer<String, String> producer = new KafkaProducer<>(props);\n")
                .append("        producer.send(new ProducerRecord<>(\"my-topic\", \"key\", \"value\"));\n\n")
                .append("        producer.close();\n")
                .append("    }\n")
                .append("}\n\n")
                .append("// Kafka Consumer 示例代码\n")
                .append("import org.apache.kafka.clients.consumer.*;\n")
                .append("import java.time.Duration;\n")
                .append("import java.util.Arrays;\n")
                .append("import java.util.Properties;\n\n")
                .append("public class KafkaConsumerExample {\n")
                .append("    public static void main(String[] args) {\n")
                .append("        Properties props = new Properties();\n")
                .append("        props.put(\"bootstrap.servers\", \"").append(kafkaConnectString)
                .append("\");\n")
                .append("        props.put(\"group.id\", \"test-group\");\n")
                .append("        props.put(\"key.deserializer\", \"org.apache.kafka.common.serialization.StringDeserializer\");\n")
                .append("        props.put(\"value.deserializer\", \"org.apache.kafka.common.serialization.StringDeserializer\");\n");

        // 添加Kerberos配置（如果启用）
        if (enableKerberos) {
            code.append("\n        // Kerberos 认证配置\n")
                    .append("        props.put(\"security.protocol\", \"SASL_PLAINTEXT\");\n")
                    .append("        props.put(\"sasl.mechanism\", \"GSSAPI\");\n")
                    .append("        props.put(\"sasl.kerberos.service.name\", \"kafka\");\n");
        }

        code.append("\n        Consumer<String, String> consumer = new KafkaConsumer<>(props);\n")
                .append("        consumer.subscribe(Arrays.asList(\"my-topic\"));\n\n")
                .append("        while (true) {\n")
                .append("            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));\n")
                .append("            for (ConsumerRecord<String, String> record : records) {\n")
                .append("                System.out.printf(\"offset = %d, key = %s, value = %s%n\", record.offset(), record.key(), record.value());\n")
                .append("            }\n")
                .append("        }\n")
                .append("    }\n")
                .append("}");

        return code.toString();
    }

    /**
     * 生成Python示例代码
     *
     * @param kafkaConnectString Kafka连接字符串
     * @param enableKerberos     是否启用Kerberos
     * @return Python代码示例
     */
    private String generatePythonCode(String kafkaConnectString, boolean enableKerberos) {
        StringBuilder code = new StringBuilder();
        code.append("# Kafka Producer 示例代码\n")
                .append("from kafka import KafkaProducer\n\n")
                .append("producer = KafkaProducer(\n")
                .append("    bootstrap_servers=['").append(kafkaConnectString.replace(",", "','"))
                .append("'],\n")
                .append("    value_serializer=lambda x: x.encode('utf-8')");

        // 添加Kerberos配置（如果启用）
        if (enableKerberos) {
            code.append(",\n    # Kerberos 认证配置\n")
                    .append("    security_protocol='SASL_PLAINTEXT',\n")
                    .append("    sasl_mechanism='GSSAPI',\n")
                    .append("    sasl_kerberos_service_name='kafka'");
        }

        code.append("\n)\n\n")
                .append("producer.send('my-topic', b'Hello, Kafka!')\n")
                .append("producer.flush()\n\n")
                .append("# Kafka Consumer 示例代码\n")
                .append("from kafka import KafkaConsumer\n\n")
                .append("consumer = KafkaConsumer(\n")
                .append("    'my-topic',\n")
                .append("    bootstrap_servers=['").append(kafkaConnectString.replace(",", "','"))
                .append("'],\n")
                .append("    auto_offset_reset='earliest',\n")
                .append("    group_id='test-group'");

        // 添加Kerberos配置（如果启用）
        if (enableKerberos) {
            code.append(",\n    # Kerberos 认证配置\n")
                    .append("    security_protocol='SASL_PLAINTEXT',\n")
                    .append("    sasl_mechanism='GSSAPI',\n")
                    .append("    sasl_kerberos_service_name='kafka'");
        }

        code.append("\n)\n\n")
                .append("for message in consumer:\n")
                .append("    print(f\"Topic: {message.topic}, Partition: {message.partition}, Offset: {message.offset}\")\n")
                .append("    print(f\"Key: {message.key}, Value: {message.value.decode('utf-8')}\")");

        return code.toString();
    }

    /**
     * 生成命令行示例
     *
     * @param kafkaConnectString Kafka连接字符串
     * @return 命令行示例列表
     */
    private List<CommandLineItem> generateCommandLines(String kafkaConnectString,
                                                       String serviceName) {
        List<CommandLineItem> commandLines = new ArrayList<>();

        // 生成节点名称 - 假设第一个broker就是运行命令的节点
        String hostname = kafkaConnectString.split(",")[0].split(":")[0];
        // 命令提示符
        String shellPrompt = "[root@" + hostname + " " + serviceName + "]# ";


        // 列出所有主题
        CommandLineItem listTopicsCmd = CommandLineItem.builder()
                .label("列出所有主题")
                .value("bin/kafka-topics.sh --list --bootstrap-server " + kafkaConnectString)
                .commandPrompt(shellPrompt)
                .commandResult("topic1\ntopic2\ntopic3\n__consumer_offsets\n_schemas")
                .build();
        commandLines.add(listTopicsCmd);

        // 创建主题
        CommandLineItem createTopicCmd = CommandLineItem.builder()
                .label("创建主题")
                .value("bin/kafka-topics.sh --create --topic my-topic --bootstrap-server "
                        + kafkaConnectString + " --partitions 3 --replication-factor 2"
                )
                .commandPrompt(shellPrompt)
                .commandResult("Created topic my-topic.")
                .build();
        commandLines.add(createTopicCmd);

        // 查看主题详情
        CommandLineItem describeTopicCmd = CommandLineItem.builder()
                .label("查看主题详情")
                .value("bin/kafka-topics.sh --describe --topic my-topic --bootstrap-server "
                        + kafkaConnectString)
                .commandPrompt(shellPrompt)
                .commandResult("Topic: my-topic\tTopicId: abcdefgh1234\tPartitionCount: 3\tReplicationFactor: 2\n"
                        +
                        "Topic: my-topic\tPartition: 0\tLeader: 1\tReplicas: 1,2\tIsr: 1,2\n" +
                        "Topic: my-topic\tPartition: 1\tLeader: 2\tReplicas: 2,3\tIsr: 2,3\n" +
                        "Topic: my-topic\tPartition: 2\tLeader: 3\tReplicas: 3,1\tIsr: 3,1")
                .build();
        commandLines.add(describeTopicCmd);

        // 删除主题
        CommandLineItem deleteTopicCmd = CommandLineItem.builder()
                .label("删除主题")
                .value("bin/kafka-topics.sh --delete --topic my-topic --bootstrap-server "
                        + kafkaConnectString)
                .commandPrompt(shellPrompt)
                .commandResult("Topic my-topic is marked for deletion.")
                .build();
        commandLines.add(deleteTopicCmd);

        // 生产消息
        CommandLineItem produceCmd = CommandLineItem.builder()
                .label("生产消息")
                .value("bin/kafka-console-producer.sh --broker-list " + kafkaConnectString
                        + " --topic my-topic")
                .commandPrompt(shellPrompt)
                .commandResult(">hello\n>world\n>（按Ctrl+C退出）")
                .build();
        commandLines.add(produceCmd);

        // 消费消息
        CommandLineItem consumeCmd = CommandLineItem.builder()
                .label("消费消息")
                .value("bin/kafka-console-consumer.sh --bootstrap-server " + kafkaConnectString
                        + " --topic my-topic --from-beginning")
                .commandPrompt(shellPrompt)
                .commandResult("hello\nworld\n（消息会持续显示，按Ctrl+C退出）")
                .build();
        commandLines.add(consumeCmd);

        // 查看消费者组
        CommandLineItem listGroupsCmd = CommandLineItem.builder()
                .label("查看消费者组")
                .value("bin/kafka-consumer-groups.sh --list --bootstrap-server "
                        + kafkaConnectString)
                .commandPrompt(shellPrompt)
                .commandResult("console-consumer-12345\nmy-group\ntest-group")
                .build();
        commandLines.add(listGroupsCmd);

        // 查看消费者组详情
        CommandLineItem describeGroupCmd = CommandLineItem.builder()
                .label("查看消费者组详情")
                .value("bin/kafka-consumer-groups.sh --describe --group my-group --bootstrap-server "
                        + kafkaConnectString)
                .commandPrompt(shellPrompt)
                .commandResult(
                        "GROUP        TOPIC    PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG        CONSUMER-ID            HOST            CLIENT-ID\n"
                                +
                                "my-group     my-topic 0          1000            2000            1000       consumer-1-abc123      /127.0.0.1      consumer-1\n"
                                +
                                "my-group     my-topic 1          1500            2500            1000       consumer-2-def456      /127.0.0.2      consumer-2\n"
                                +
                                "my-group     my-topic 2          2000            3000            1000       consumer-3-ghi789      /127.0.0.3      consumer-3")
                .build();
        commandLines.add(describeGroupCmd);

        // 查看消费者组偏移量
        CommandLineItem checkOffsetsCmd = CommandLineItem.builder()
                .label("查看消费者组偏移量")
                .value("bin/kafka-consumer-groups.sh --describe --group my-group --bootstrap-server "
                        + kafkaConnectString + " --offsets")
                .commandPrompt(shellPrompt)
                .commandResult(
                        "GROUP        TOPIC    PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG        CONSUMER-ID            HOST            CLIENT-ID\n"
                                +
                                "my-group     my-topic 0          1000            2000            1000       consumer-1-abc123      /127.0.0.1      consumer-1\n"
                                +
                                "my-group     my-topic 1          1500            2500            1000       consumer-2-def456      /127.0.0.2      consumer-2\n"
                                +
                                "my-group     my-topic 2          2000            3000            1000       consumer-3-ghi789      /127.0.0.3      consumer-3")
                .build();
        commandLines.add(checkOffsetsCmd);

        // 重置消费者组偏移量
        CommandLineItem resetOffsetsCmd = CommandLineItem.builder()
                .label("重置消费者组偏移量")
                .value("bin/kafka-consumer-groups.sh --bootstrap-server " + kafkaConnectString
                        + " --group my-group --reset-offsets --to-earliest --all-topics --execute"
                )
                .commandPrompt(shellPrompt)
                .commandResult("GROUP        TOPIC    PARTITION  NEW-OFFSET\n" +
                        "my-group     my-topic 0          0\n" +
                        "my-group     my-topic 1          0\n" +
                        "my-group     my-topic 2          0")
                .build();
        commandLines.add(resetOffsetsCmd);

        // 查看主题配置
        CommandLineItem describeConfigCmd = CommandLineItem.builder()
                .label("查看主题配置")
                .value("bin/kafka-configs.sh --describe --entity-type topics --entity-name my-topic --bootstrap-server "
                        + kafkaConnectString)
                .commandPrompt(shellPrompt)
                .commandResult("Configs for topic 'my-topic' are:\n" +
                        "  retention.ms=604800000 sensitive=false synonyms={DYNAMIC_TOPIC_CONFIG:retention.ms=604800000, DEFAULT_CONFIG:retention.ms=604800000}\n"
                        +
                        "  cleanup.policy=delete sensitive=false synonyms={DYNAMIC_TOPIC_CONFIG:cleanup.policy=delete, DEFAULT_CONFIG:cleanup.policy=delete}")
                .build();
        commandLines.add(describeConfigCmd);

        // 修改主题配置
        CommandLineItem alterConfigCmd = CommandLineItem.builder()
                .label("修改主题配置")
                .value("bin/kafka-configs.sh --alter --entity-type topics --entity-name my-topic --add-config retention.ms=604800000 --bootstrap-server "
                        + kafkaConnectString)
                .commandPrompt(shellPrompt)
                .commandResult("Completed updating config for topic my-topic.")
                .build();
        commandLines.add(alterConfigCmd);

        // 查看broker配置
        CommandLineItem describeBrokerConfigCmd = CommandLineItem.builder()
                .label("查看broker配置")
                .value("bin/kafka-configs.sh --describe --entity-type brokers --entity-default --bootstrap-server "
                        + kafkaConnectString)
                .commandPrompt(shellPrompt)
                .commandResult("Default configs for brokers in the cluster are:\n" +
                        "  log.retention.hours=168 sensitive=false synonyms={DEFAULT_CONFIG:log.retention.hours=168}\n"
                        +
                        "  log.retention.bytes=-1 sensitive=false synonyms={DEFAULT_CONFIG:log.retention.bytes=-1}")
                .build();
        commandLines.add(describeBrokerConfigCmd);

        // 查看集群信息
        CommandLineItem apiVersionsCmd = CommandLineItem.builder()
                .label("查看集群信息")
                .value("bin/kafka-broker-api-versions.sh --bootstrap-server " + kafkaConnectString
                )
                .commandPrompt(shellPrompt)
                .commandResult(hostname + ":9092 (id: 1 rack: null) -> \n" +
                        "  Produce(0): 0 to 8 [usable: 8]\n" +
                        "  Fetch(1): 0 to 11 [usable: 11]\n" +
                        "  ListOffsets(2): 0 to 5 [usable: 5]\n" +
                        "  Metadata(3): 0 to 9 [usable: 9]\n" +
                        "  ... [更多API版本信息]")
                .build();
        commandLines.add(apiVersionsCmd);

        return addFinalPrompt(commandLines, serviceName, hostname);
    }
}
