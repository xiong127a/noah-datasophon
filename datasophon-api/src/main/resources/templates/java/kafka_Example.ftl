/**
 * Kafka Java客户端示例代码
 * 适用于Kafka ${data.getBasicInfoValue('version', '2.x')}
 */

// DEPENDENCIES_START
/*
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>2.4.1</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>1.7.36</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.11</version>
</dependency>
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.13</artifactId>
    <version>${data.getBasicInfoValue('version', '2.4.1')}</version>
</dependency>
</#if>
*/
// DEPENDENCIES_END

package com.example.kafka;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.*;
import org.apache.kafka.common.serialization.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Kafka示例类，演示如何使用Java连接Kafka集群
 * 包含了生产者、消费者和管理者的示例代码
 */
public class KafkaExample {
    private static final Logger logger = LoggerFactory.getLogger(KafkaExample.class);
    
    // Kafka配置
    private static final String BOOTSTRAP_SERVERS = "${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')}";
    // ZooKeeper配置
    private static final String ZOOKEEPER_SERVERS = "${data.getConnectInfoValue('zkConnect', 'localhost:2181')}";
    private static final String TOPIC_NAME = "example-topic";
    private static final String CONSUMER_GROUP_ID = "example-consumer-group";
    
    public static void main(String[] args) {
        try {
            // 打印配置信息
            logger.info("Kafka集群地址: {}", BOOTSTRAP_SERVERS);
            logger.info("ZooKeeper集群地址: {}", ZOOKEEPER_SERVERS);
            
            // 1. 创建主题
            createTopic();
            
            // 2. 列出所有主题
            listTopics();
            
            // 3. 发送消息
            produceMessages();
            
            // 4. 消费消息
            consumeMessages();
            
            // 5. 高级消费者示例（从指定偏移量开始消费）
            consumeFromOffset();
            
            logger.info("Kafka示例运行完成");
        } catch (Exception e) {
            logger.error("Kafka示例运行失败", e);
        }
    }
    
    /**
     * 创建Kafka管理客户端
     */
    private static AdminClient createAdminClient() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(AdminClientConfig.CLIENT_ID_CONFIG, "kafka-admin-client");
        
<#if data.getSecurityInfoValue('ssl.enabled', 'false') == 'true'>
        // SSL配置
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put("ssl.truststore.location", "${data.getSecurityInfoValue('ssl.truststore.location', '/path/to/truststore.jks')}");
        props.put("ssl.truststore.password", "${data.getSecurityInfoValue('ssl.truststore.password', 'truststore-password')}");
        props.put("ssl.keystore.location", "${data.getSecurityInfoValue('ssl.keystore.location', '/path/to/keystore.jks')}");
        props.put("ssl.keystore.password", "${data.getSecurityInfoValue('ssl.keystore.password', 'keystore-password')}");
        props.put("ssl.key.password", "${data.getSecurityInfoValue('ssl.key.password', 'key-password')}");
</#if>
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
        // Kerberos配置
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.put("sasl.mechanism", "GSSAPI");
        props.put("sasl.kerberos.service.name", "kafka");

        // Kerberos JAAS配置
        System.setProperty("java.security.auth.login.config", "${data.getSecurityInfoValue('jaas.file.path', '/path/to/jaas.conf')}");
        System.setProperty("java.security.krb5.conf", "${data.getSecurityInfoValue('krb5.file.path', '/etc/krb5.conf')}");
</#if>
        
        return AdminClient.create(props);
    }
    
    /**
     * 创建Kafka生产者
     */
    private static KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "kafka-producer-example");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        
<#if data.getSecurityInfoValue('ssl.enabled', 'false') == 'true'>
        // SSL配置
        props.put(ProducerConfig.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put("ssl.truststore.location", "${data.getSecurityInfoValue('ssl.truststore.location', '/path/to/truststore.jks')}");
        props.put("ssl.truststore.password", "${data.getSecurityInfoValue('ssl.truststore.password', 'truststore-password')}");
        props.put("ssl.keystore.location", "${data.getSecurityInfoValue('ssl.keystore.location', '/path/to/keystore.jks')}");
        props.put("ssl.keystore.password", "${data.getSecurityInfoValue('ssl.keystore.password', 'keystore-password')}");
        props.put("ssl.key.password", "${data.getSecurityInfoValue('ssl.key.password', 'key-password')}");
</#if>
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
        // Kerberos配置
        props.put(ProducerConfig.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.put("sasl.mechanism", "GSSAPI");
        props.put("sasl.kerberos.service.name", "kafka");

        // Kerberos JAAS配置
        System.setProperty("java.security.auth.login.config", "${data.getSecurityInfoValue('jaas.file.path', '/path/to/jaas.conf')}");
        System.setProperty("java.security.krb5.conf", "${data.getSecurityInfoValue('krb5.file.path', '/etc/krb5.conf')}");
</#if>
        
        return new KafkaProducer<>(props);
    }
    
    /**
     * 创建Kafka消费者
     */
    private static KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-consumer-example");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
<#if data.getSecurityInfoValue('ssl.enabled', 'false') == 'true'>
        // SSL配置
        props.put(ConsumerConfig.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put("ssl.truststore.location", "${data.getSecurityInfoValue('ssl.truststore.location', '/path/to/truststore.jks')}");
        props.put("ssl.truststore.password", "${data.getSecurityInfoValue('ssl.truststore.password', 'truststore-password')}");
        props.put("ssl.keystore.location", "${data.getSecurityInfoValue('ssl.keystore.location', '/path/to/keystore.jks')}");
        props.put("ssl.keystore.password", "${data.getSecurityInfoValue('ssl.keystore.password', 'keystore-password')}");
        props.put("ssl.key.password", "${data.getSecurityInfoValue('ssl.key.password', 'key-password')}");
</#if>
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
        // Kerberos配置
        props.put(ConsumerConfig.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.put("sasl.mechanism", "GSSAPI");
        props.put("sasl.kerberos.service.name", "kafka");

        // Kerberos JAAS配置
        System.setProperty("java.security.auth.login.config", "${data.getSecurityInfoValue('jaas.file.path', '/path/to/jaas.conf')}");
        System.setProperty("java.security.krb5.conf", "${data.getSecurityInfoValue('krb5.file.path', '/etc/krb5.conf')}");
</#if>
        
        return new KafkaConsumer<>(props);
    }
    
    /**
     * 创建主题
     */
    private static void createTopic() {
        try (AdminClient adminClient = createAdminClient()) {
            // 检查主题是否已存在
            boolean topicExists = adminClient.listTopics().names().get().contains(TOPIC_NAME);
            
            if (topicExists) {
                logger.info("主题 {} 已存在", TOPIC_NAME);
            } else {
                // 创建主题配置
                int partitions = 3;
                short replicationFactor = 1;
                NewTopic newTopic = new NewTopic(TOPIC_NAME, partitions, replicationFactor);
                
                // 创建主题
                CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));
                result.all().get(); // 等待操作完成
                logger.info("主题 {} 创建成功", TOPIC_NAME);
            }
        } catch (Exception e) {
            logger.error("创建主题失败", e);
        }
    }
    
    /**
     * 列出所有主题
     */
    private static void listTopics() {
        try (AdminClient adminClient = createAdminClient()) {
            // 获取所有主题
            ListTopicsResult topicsResult = adminClient.listTopics();
            Set<String> topicNames = topicsResult.names().get();
            
            logger.info("集群中的主题列表: {}", topicNames);
        } catch (Exception e) {
            logger.error("获取主题列表失败", e);
        }
    }
    
    /**
     * 生产消息示例
     */
    private static void produceMessages() {
        try (KafkaProducer<String, String> producer = createProducer()) {
            // 发送10条消息
            for (int i = 0; i < 10; i++) {
                String key = "key-" + i;
                String value = "消息内容-" + i + " (时间戳: " + System.currentTimeMillis() + ")";
                
                // 创建消息记录
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, key, value);
                
                // 发送消息（异步）
                producer.send(record, (metadata, exception) -> {
                    if (exception == null) {
                        logger.info("消息发送成功: 主题={}, 分区={}, 偏移量={}, 键={}, 值={}",
                                metadata.topic(), metadata.partition(), metadata.offset(), key, value);
                    } else {
                        logger.error("消息发送失败", exception);
                    }
                });
            }
            
            // 确保所有消息都已发送
            producer.flush();
            logger.info("所有消息已发送完成");
        } catch (Exception e) {
            logger.error("生产消息失败", e);
        }
    }
    
    /**
     * 消费消息示例
     */
    private static void consumeMessages() {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            // 订阅主题
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));
            logger.info("已订阅主题: {}", TOPIC_NAME);
            
            // 设置轮询超时时间
            final int timeoutSeconds = 5;
            final long startTime = System.currentTimeMillis();
            final long timeoutMs = timeoutSeconds * 1000;
            
            // 开始消费
            int messageCount = 0;
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                // 拉取记录
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                
                // 处理记录
                for (ConsumerRecord<String, String> record : records) {
                    messageCount++;
                    logger.info("收到消息: 主题={}, 分区={}, 偏移量={}, 键={}, 值={}",
                            record.topic(), record.partition(), record.offset(), record.key(), record.value());
                }
            }
            
            // 取消订阅
            consumer.unsubscribe();
            
            if (messageCount > 0) {
                logger.info("共消费了 {} 条消息", messageCount);
            } else {
                logger.info("在{}秒内没有收到任何消息", timeoutSeconds);
            }
        } catch (Exception e) {
            logger.error("消费消息失败", e);
        }
    }
    
    /**
     * 高级消费者示例 - 从指定偏移量开始消费
     */
    private static void consumeFromOffset() {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            // 获取主题中的分区信息
            List<PartitionInfo> partitionInfos = consumer.partitionsFor(TOPIC_NAME);
            if (partitionInfos == null || partitionInfos.isEmpty()) {
                logger.warn("主题 {} 没有可用的分区", TOPIC_NAME);
                return;
            }
            
            // 创建TopicPartition列表
            List<TopicPartition> partitions = new ArrayList<>();
            for (PartitionInfo partitionInfo : partitionInfos) {
                partitions.add(new TopicPartition(TOPIC_NAME, partitionInfo.partition()));
            }
            
            // 手动分配分区
            consumer.assign(partitions);
            
            // 获取每个分区的开始和结束偏移量
            Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
            
            // 打印每个分区的偏移量范围
            for (TopicPartition partition : partitions) {
                Long beginOffset = beginningOffsets.get(partition);
                Long endOffset = endOffsets.get(partition);
                logger.info("分区 {}: 开始偏移量={}, 结束偏移量={}, 消息数量={}",
                        partition.partition(), beginOffset, endOffset, endOffset - beginOffset);
                
                if (endOffset > beginOffset) {
                    // 从分区的一半位置开始消费
                    long startOffset = beginOffset + (endOffset - beginOffset) / 2;
                    consumer.seek(partition, startOffset);
                    logger.info("将从分区 {} 的偏移量 {} 开始消费", partition.partition(), startOffset);
                }
            }
            
            // 设置轮询超时
            final int timeoutSeconds = 5;
            final long startTime = System.currentTimeMillis();
            final long timeoutMs = timeoutSeconds * 1000;
            
            // 从指定位置消费
            int messageCount = 0;
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    messageCount++;
                    logger.info("高级消费: 主题={}, 分区={}, 偏移量={}, 键={}, 值={}",
                            record.topic(), record.partition(), record.offset(), record.key(), record.value());
                }
            }
            
            if (messageCount > 0) {
                logger.info("高级消费: 共读取了 {} 条消息", messageCount);
            } else {
                logger.info("高级消费: 在{}秒内没有收到任何消息", timeoutSeconds);
            }
        } catch (Exception e) {
            logger.error("高级消费失败", e);
        }
    }
} 