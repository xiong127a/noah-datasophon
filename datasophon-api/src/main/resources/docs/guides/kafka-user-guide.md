# Kafka 用户指南

## 快速入门

本指南将帮助你在 DataSophon 平台上快速部署、配置和使用 Kafka，以构建高性能、可靠的数据流应用。

## 前置条件

在开始之前，请确保满足以下条件：

- DataSophon 平台已成功安装并正常运行
- 已安装 JDK（建议使用 JDK 8 或更高版本）
- 已安装 ZooKeeper 服务（如使用 KRaft 模式则不需要）
- 集群节点间网络通畅
- 所有节点时间已同步

## 部署流程

### 通过 DataSophon 平台部署

1. 登录 DataSophon 管理控制台
2. 进入【组件管理】页面
3. 点击【添加服务】，在组件列表中选择 Kafka
4. 按照向导完成配置：
   - 选择安装节点
   - 设置角色分配
   - 配置服务参数
   - 设置资源分配
5. 确认配置无误后，点击【部署】
6. 等待部署完成，可以在【服务状态】查看部署进度

### 配置参数说明

DataSophon 平台提供了多种 Kafka 配置参数，以下是常用参数的说明：

#### 基本配置

| 参数名称 | 说明 | 推荐值 |
|---------|------|-------|
| broker.id | Broker 的唯一标识 | 自动生成 |
| zookeeper.connect | ZooKeeper 连接字符串 | zk1:2181,zk2:2181,zk3:2181/kafka |
| listeners | 监听器配置 | PLAINTEXT://host:9092 |
| advertised.listeners | 对外发布的监听器配置 | PLAINTEXT://host:9092 |
| num.network.threads | 网络线程数 | 3-10（根据负载调整） |
| num.io.threads | I/O 线程数 | 8-32（根据负载调整） |
| socket.send.buffer.bytes | Socket 发送缓冲区大小 | 102400 |
| socket.receive.buffer.bytes | Socket 接收缓冲区大小 | 102400 |
| socket.request.max.bytes | 请求最大大小 | 104857600 |

#### 日志配置

| 参数名称 | 说明 | 推荐值 |
|---------|------|-------|
| log.dirs | 日志数据目录 | /data/kafka-logs |
| num.partitions | 默认分区数 | 3-10 |
| log.retention.hours | 日志保留时间 | 168（7天） |
| log.segment.bytes | 日志段大小 | 1073741824（1GB） |
| log.retention.check.interval.ms | 日志检查间隔 | 300000（5分钟） |
| delete.topic.enable | 是否允许删除主题 | true |

#### 复制配置

| 参数名称 | 说明 | 推荐值 |
|---------|------|-------|
| default.replication.factor | 默认副本因子 | 3 |
| min.insync.replicas | 最小同步副本数 | 2 |
| replica.fetch.max.bytes | 副本获取最大字节数 | 1048576 |
| replica.lag.time.max.ms | 副本最大延迟时间 | 10000 |

#### 性能调优

| 参数名称 | 说明 | 推荐值 |
|---------|------|-------|
| compression.type | 压缩类型 | producer（由生产者决定） |
| message.max.bytes | 最大消息大小 | 1000000 |
| queued.max.requests | 最大排队请求数 | 500-1000 |

### KRaft 模式配置（Kafka 2.8+）

如果使用 KRaft 模式（无 ZooKeeper），需要额外配置：

| 参数名称 | 说明 | 推荐值 |
|---------|------|-------|
| process.roles | 节点角色 | broker、controller 或都有 |
| controller.quorum.voters | 控制器仲裁投票者 | 0@host1:9093,1@host2:9093,2@host3:9093 |
| node.id | 节点 ID | 0、1、2（唯一标识） |

## 基本操作指南

### 主题管理

#### 创建主题

```bash
# 创建主题
kafka-topics.sh --bootstrap-server broker1:9092 --create --topic my-topic --partitions 3 --replication-factor 3

# 使用额外配置创建主题
kafka-topics.sh --bootstrap-server broker1:9092 --create --topic my-topic --partitions 3 --replication-factor 3 \
  --config max.message.bytes=1048576 \
  --config retention.ms=604800000
```

#### 列出所有主题

```bash
kafka-topics.sh --bootstrap-server broker1:9092 --list
```

#### 查看主题详情

```bash
kafka-topics.sh --bootstrap-server broker1:9092 --describe --topic my-topic
```

#### 修改主题配置

```bash
# 修改主题配置
kafka-configs.sh --bootstrap-server broker1:9092 --entity-type topics --entity-name my-topic --alter \
  --add-config max.message.bytes=2097152,retention.ms=1209600000
  
# 查看主题配置
kafka-configs.sh --bootstrap-server broker1:9092 --entity-type topics --entity-name my-topic --describe
```

#### 删除主题

```bash
kafka-topics.sh --bootstrap-server broker1:9092 --delete --topic my-topic
```

### 消息生产与消费

#### 使用控制台生产者发送消息

```bash
# 发送消息到主题
kafka-console-producer.sh --bootstrap-server broker1:9092 --topic my-topic

# 包含键的消息发送
kafka-console-producer.sh --bootstrap-server broker1:9092 --topic my-topic \
  --property "parse.key=true" --property "key.separator=:"
```

输入示例：
```
This is a test message
Another message
```

带键消息输入示例：
```
key1:This is a message with key1
key2:This is a message with key2
```

#### 使用控制台消费者接收消息

```bash
# 从头开始消费主题消息
kafka-console-consumer.sh --bootstrap-server broker1:9092 --topic my-topic --from-beginning

# 消费最新消息
kafka-console-consumer.sh --bootstrap-server broker1:9092 --topic my-topic

# 显示消息键和值
kafka-console-consumer.sh --bootstrap-server broker1:9092 --topic my-topic --property print.key=true --property key.separator=:

# 指定消费者组
kafka-console-consumer.sh --bootstrap-server broker1:9092 --topic my-topic --group my-group
```

#### 使用 Kafka 性能测试工具

生产者性能测试：

```bash
# 测试生产性能
kafka-producer-perf-test.sh --topic my-topic --num-records 1000000 --record-size 1024 \
  --throughput 100000 --producer-props bootstrap.servers=broker1:9092,broker2:9092,broker3:9092
```

消费者性能测试：

```bash
# 测试消费性能
kafka-consumer-perf-test.sh --bootstrap-server broker1:9092 --topic my-topic \
  --messages 1000000 --threads 1
```

### 消费者组管理

#### 列出所有消费者组

```bash
kafka-consumer-groups.sh --bootstrap-server broker1:9092 --list
```

#### 查看消费者组详情

```bash
kafka-consumer-groups.sh --bootstrap-server broker1:9092 --describe --group my-group
```

输出示例：
```
GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
my-group        my-topic        0          1000            1000            0               consumer-1      localhost       consumer-1
my-group        my-topic        1          985             985             0               consumer-2      localhost       consumer-2
my-group        my-topic        2          1020            1020            0               consumer-1      localhost       consumer-1
```

#### 重置消费者组偏移量

```bash
# 重置到开头
kafka-consumer-groups.sh --bootstrap-server broker1:9092 --group my-group --reset-offsets --to-earliest --execute --topic my-topic

# 重置到末尾
kafka-consumer-groups.sh --bootstrap-server broker1:9092 --group my-group --reset-offsets --to-latest --execute --topic my-topic

# 向前移动偏移量
kafka-consumer-groups.sh --bootstrap-server broker1:9092 --group my-group --reset-offsets --shift-by -10 --execute --topic my-topic
```

#### 删除消费者组

```bash
kafka-consumer-groups.sh --bootstrap-server broker1:9092 --delete --group my-group
```

## 开发指南

### Java 客户端使用

#### 添加 Maven 依赖

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.3.1</version>  <!-- 使用与服务器兼容的版本 -->
</dependency>
```

#### 生产者代码示例

```java
import org.apache.kafka.clients.producer.*;
import java.util.Properties;
import java.util.concurrent.Future;

public class KafkaProducerExample {
    public static void main(String[] args) {
        // 配置生产者属性
        Properties props = new Properties();
        props.put("bootstrap.servers", "broker1:9092,broker2:9092,broker3:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("retries", 3);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        
        // 创建生产者
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            // 同步发送
            Future<RecordMetadata> future = producer.send(new ProducerRecord<>("my-topic", "key", "value"));
            RecordMetadata metadata = future.get();
            System.out.println("Message sent to partition " + metadata.partition() + " with offset " + metadata.offset());
            
            // 异步发送带回调
            producer.send(new ProducerRecord<>("my-topic", "key", "async-value"), 
                (recordMetadata, e) -> {
                    if (e != null) {
                        e.printStackTrace();
                    } else {
                        System.out.println("Message sent to partition " + recordMetadata.partition() + 
                                          " with offset " + recordMetadata.offset());
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 消费者代码示例

```java
import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerExample {
    public static void main(String[] args) {
        // 配置消费者属性
        Properties props = new Properties();
        props.put("bootstrap.servers", "broker1:9092,broker2:9092,broker3:9092");
        props.put("group.id", "my-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        
        // 创建消费者
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            // 订阅主题
            consumer.subscribe(Collections.singletonList("my-topic"));
            
            // 持续消费消息
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("Received message: topic = %s, partition = %d, offset = %d, key = %s, value = %s%n",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value());
                    
                    // 处理消息...
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 事务示例

```java
import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class KafkaTransactionalProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "broker1:9092,broker2:9092,broker3:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("transactional.id", "my-transactional-id");  // 启用事务
        
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            // 初始化事务
            producer.initTransactions();
            
            try {
                // 开始事务
                producer.beginTransaction();
                
                // 发送消息
                producer.send(new ProducerRecord<>("topic1", "key1", "value1"));
                producer.send(new ProducerRecord<>("topic2", "key2", "value2"));
                
                // 提交事务
                producer.commitTransaction();
            } catch (Exception e) {
                // 出现异常时中止事务
                producer.abortTransaction();
                throw e;
            }
        }
    }
}
```

### 流处理示例（Kafka Streams）

#### 添加 Maven 依赖

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-streams</artifactId>
    <version>3.3.1</version>
</dependency>
```

#### Kafka Streams 应用示例

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class WordCountExample {
    public static void main(String[] args) {
        // 配置 Streams 属性
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-application");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "broker1:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // 创建流构建器
        StreamsBuilder builder = new StreamsBuilder();
        
        // 创建源流
        KStream<String, String> source = builder.stream("input-topic");
        
        // 单词计数逻辑
        KTable<String, Long> counts = source
            .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
            .groupBy((key, word) -> word)
            .count();
        
        // 将结果发送到输出主题
        counts.toStream().to("output-topic", Produced.with(Serdes.String(), Serdes.Long()));
        
        // 构建并启动流
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close();
            latch.countDown();
        }));

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
```

## 监控与运维

### JMX 监控配置

启用 JMX 监控，在 Kafka 启动脚本中添加：

```bash
export JMX_PORT=9999
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote=true 
                       -Dcom.sun.management.jmxremote.authenticate=false 
                       -Dcom.sun.management.jmxremote.ssl=false 
                       -Djava.rmi.server.hostname=<broker-host>"
```

### 重要监控指标

| 类别 | 指标名称 | 说明 |
|------|----------|------|
| **Broker** | BytesInPerSec | 每秒入站字节数 |
| | BytesOutPerSec | 每秒出站字节数 |
| | RequestsPerSec | 每秒请求数 |
| | ActiveControllerCount | 当前控制器状态 |
| **Topic** | MessagesInPerSec | 每秒接收消息数 |
| | BytesInPerSec | 主题每秒接收字节数 |
| | BytesOutPerSec | 主题每秒发送字节数 |
| **Producer** | request-latency-avg | 平均请求延迟 |
| | request-rate | 请求速率 |
| | response-rate | 响应速率 |
| **Consumer** | records-consumed-rate | 消息消费速率 |
| | bytes-consumed-rate | 字节消费速率 |
| | fetch-latency-avg | 平均获取延迟 |

### 使用 Prometheus 和 Grafana 监控

1. 添加 JMX Exporter 代理

   下载 JMX Exporter:
   ```bash
   wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.16.1/jmx_prometheus_javaagent-0.16.1.jar -O jmx_prometheus_javaagent.jar
   ```

2. 创建 Kafka JMX 配置文件 `kafka-jmx-config.yml`:

   ```yaml
   lowercaseOutputName: true
   lowercaseOutputLabelNames: true
   rules:
   - pattern: ".*"
   ```

3. 配置 Kafka 启动脚本，添加 JVM 选项:

   ```bash
   export KAFKA_OPTS="-javaagent:/path/to/jmx_prometheus_javaagent.jar=8080:/path/to/kafka-jmx-config.yml"
   ```

4. 在 Prometheus 中配置目标:

   ```yaml
   scrape_configs:
     - job_name: 'kafka'
       static_configs:
         - targets: ['kafka-broker1:8080', 'kafka-broker2:8080', 'kafka-broker3:8080']
   ```

5. 导入 Grafana 的 Kafka 仪表板

### 常见管理任务

#### 滚动重启 Broker

1. 确定要重启的 Broker ID
2. 查看 Broker 的分区状态：
   ```bash
   kafka-topics.sh --bootstrap-server broker1:9092 --describe
   ```
3. 关闭目标 Broker：
   ```bash
   kafka-server-stop.sh
   ```
4. 等待所有分区副本重新分配
5. 启动 Broker：
   ```bash
   kafka-server-start.sh -daemon /path/to/server.properties
   ```
6. 验证 Broker 已正常加入集群

#### 扩展 Kafka 集群

1. 为新 Broker 准备配置文件 `server.properties`，确保 `broker.id` 唯一
2. 启动新 Broker：
   ```bash
   kafka-server-start.sh -daemon /path/to/server.properties
   ```
3. 验证新 Broker 已加入集群：
   ```bash
   kafka-topics.sh --bootstrap-server broker1:9092 --describe
   ```
4. 重新平衡现有分区（可选）：
   ```bash
   # 创建重新分配 JSON 文件
   kafka-reassign-partitions.sh --bootstrap-server broker1:9092 --generate \
     --topics-to-move-json-file topics-to-move.json \
     --broker-list "1,2,3,4" > reassignment-plan.json
   
   # 执行重新分配
   kafka-reassign-partitions.sh --bootstrap-server broker1:9092 \
     --reassignment-json-file reassignment-plan.json \
     --execute
   ```

#### 分区重新分配

```bash
# 创建 topics-to-move.json
echo '{"topics": [{"topic": "my-topic"}], "version": 1}' > topics-to-move.json

# 生成重新分配计划
kafka-reassign-partitions.sh --bootstrap-server broker1:9092 --generate \
  --topics-to-move-json-file topics-to-move.json \
  --broker-list "1,2,3" > reassignment-plan.json

# 执行重新分配
kafka-reassign-partitions.sh --bootstrap-server broker1:9092 \
  --reassignment-json-file reassignment-plan.json \
  --execute

# 验证重新分配进度
kafka-reassign-partitions.sh --bootstrap-server broker1:9092 \
  --reassignment-json-file reassignment-plan.json \
  --verify
```

### 故障排除

#### 常见问题及解决方案

1. **Broker 无法启动**

   问题症状:
   - Broker 进程启动失败
   - 日志中出现绑定地址或端口错误

   排查步骤:
   1. 检查端口是否被占用：`netstat -anp | grep 9092`
   2. 检查服务器资源：`top`, `df -h`
   3. 查看详细日志：`cat /var/log/kafka/server.log`

   解决方案:
   - 修改冲突的端口配置
   - 确保有足够的磁盘空间和内存
   - 确保配置文件中的路径存在且有正确权限

2. **副本同步问题**

   问题症状:
   - 主题有 Under-replicated 分区
   - ISR 列表变小

   排查步骤:
   1. 检查副本状态：`kafka-topics.sh --bootstrap-server broker1:9092 --describe`
   2. 查看 Broker 日志
   3. 检查网络延迟和带宽

   解决方案:
   - 增大 `replica.fetch.max.bytes` 或 `replica.fetch.wait.max.ms`
   - 优化网络配置
   - 调整 `replica.lag.time.max.ms`

3. **生产者/消费者连接问题**

   问题症状:
   - 客户端无法连接到 Kafka
   - 连接超时或中断

   排查步骤:
   1. 确认防火墙配置：`iptables -L`
   2. 测试网络连接：`telnet broker-host 9092`
   3. 检查监听器配置

   解决方案:
   - 确保防火墙允许 Kafka 端口
   - 正确配置 `advertised.listeners`
   - 检查客户端和服务器的兼容性

4. **性能问题**

   问题症状:
   - 高延迟
   - 低吞吐量
   - 消费者滞后

   排查步骤:
   1. 检查资源使用情况：`top`, `iostat`, `netstat`
   2. 查看 JVM GC 日志
   3. 监控 JMX 指标

   解决方案:
   - 调整 JVM 堆大小
   - 优化磁盘 I/O（使用多个磁盘或更快的存储）
   - 增加分区数量
   - 调整批处理大小、压缩和缓冲区设置

#### 常用日志位置

- Broker 日志: `/var/log/kafka/server.log`
- 控制器日志: `/var/log/kafka/controller.log`
- 状态更改日志: `/var/log/kafka/state-change.log`
- 请求日志: `/var/log/kafka/kafka-request.log`
- 日志压缩器日志: `/var/log/kafka/log-cleaner.log`

## 最佳实践

### 分区和副本策略

- 分区数量建议：每 GB/s 吞吐量约 10 个分区
- 副本因子选择：生产环境建议至少 3 个副本
- 分区均衡：避免数据倾斜，使用自定义分区器
- 主题设计：相关数据分组到同一主题，使用分区键确保顺序

### 性能优化

- **生产者优化**:
  - 增加批量大小（`batch.size`）
  - 适当增加延迟时间（`linger.ms`）以提高吞吐量
  - 使用压缩（`compression.type=snappy` 或 `lz4`）
  - 考虑非阻塞模式与回调

- **消费者优化**:
  - 增加获取大小（`fetch.min.bytes`）
  - 调整最大拉取记录数量（`max.poll.records`）
  - 优化消息处理逻辑
  - 适当设置消费者数量（通常与分区数相同或更少）

- **Broker 优化**:
  - 使用多个磁盘目录分散 I/O
  - 优化 JVM GC 参数
  - 合理配置网络和 I/O 线程数
  - 正确设置页缓存大小

### 安全性配置

#### 启用 SSL/TLS

1. 为每个 Broker 创建密钥库：

   ```bash
   keytool -keystore kafka.server.keystore.jks -alias localhost -validity 365 -genkey -keyalg RSA
   ```

2. 创建 CA：

   ```bash
   openssl req -new -x509 -keyout ca-key -out ca-cert -days 365
   ```

3. 将 CA 导入信任库：

   ```bash
   keytool -keystore kafka.server.truststore.jks -alias CARoot -import -file ca-cert
   ```

4. 配置 Broker `server.properties`：

   ```properties
   listeners=SSL://host.name:9093
   advertised.listeners=SSL://host.name:9093
   ssl.keystore.location=/path/to/kafka.server.keystore.jks
   ssl.keystore.password=keystore-password
   ssl.key.password=key-password
   ssl.truststore.location=/path/to/kafka.server.truststore.jks
   ssl.truststore.password=truststore-password
   ssl.client.auth=required
   ssl.enabled.protocols=TLSv1.2,TLSv1.3
   ssl.cipher.suites=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
   ```

5. 配置客户端：

   ```properties
   security.protocol=SSL
   ssl.truststore.location=/path/to/client.truststore.jks
   ssl.truststore.password=truststore-password
   ssl.keystore.location=/path/to/client.keystore.jks
   ssl.keystore.password=keystore-password
   ssl.key.password=key-password
   ```

#### 启用 SASL 认证

1. 创建 JAAS 配置文件 `kafka_server_jaas.conf`：

   ```
   KafkaServer {
       org.apache.kafka.common.security.plain.PlainLoginModule required
       username="admin"
       password="admin-secret"
       user_admin="admin-secret"
       user_alice="alice-secret"
       user_bob="bob-secret";
   };
   ```

2. 配置 Broker `server.properties`：

   ```properties
   listeners=SASL_PLAINTEXT://host.name:9092
   advertised.listeners=SASL_PLAINTEXT://host.name:9092
   security.inter.broker.protocol=SASL_PLAINTEXT
   sasl.enabled.mechanisms=PLAIN
   sasl.mechanism.inter.broker.protocol=PLAIN
   ```

3. 启动 Broker 时指定 JAAS 文件：

   ```bash
   export KAFKA_OPTS="-Djava.security.auth.login.config=/path/to/kafka_server_jaas.conf"
   kafka-server-start.sh server.properties
   ```

4. 配置客户端 JAAS 文件 `kafka_client_jaas.conf`：

   ```
   KafkaClient {
       org.apache.kafka.common.security.plain.PlainLoginModule required
       username="alice"
       password="alice-secret";
   };
   ```

5. 配置客户端属性：

   ```properties
   security.protocol=SASL_PLAINTEXT
   sasl.mechanism=PLAIN
   ```

#### ACL 访问控制配置

1. 启用 ACL 在 `server.properties`：

   ```properties
   authorizer.class.name=kafka.security.authorizer.AclAuthorizer
   super.users=User:admin
   ```

2. 设置 ACL 规则：

   ```bash
   # 允许 Alice 生产者写入 my-topic
   kafka-acls.sh --bootstrap-server broker1:9092 \
     --add --allow-principal User:alice \
     --producer --topic my-topic
   
   # 允许 Bob 消费者组从 my-topic 读取
   kafka-acls.sh --bootstrap-server broker1:9092 \
     --add --allow-principal User:bob \
     --consumer --topic my-topic --group my-group
   ```

3. 查看现有 ACL：

   ```bash
   kafka-acls.sh --bootstrap-server broker1:9092 --list
   ```

## 总结

本指南介绍了在 DataSophon 平台上使用 Kafka 的关键内容，包括部署配置、基本操作、开发示例、监控运维和最佳实践。Kafka 作为企业级消息系统和流处理平台，提供了高吞吐量、低延迟和可扩展的事件流处理能力。

遵循本指南的建议和最佳实践，可以帮助你构建稳定、高效的 Kafka 实现，支持各种实时数据流场景。根据具体业务需求和数据规模，合理调整配置参数和架构设计，以获得最佳性能和可靠性。 