#!/bin/bash

# Kafka命令行操作示例
# 依赖: Kafka命令行工具
# 使用说明: 本文档展示了常用的Kafka命令行操作，默认使用Kafka自带的脚本工具

# 主题管理命令
TIP> 列出所有主题
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-topics.sh --list --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')}<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> __consumer_offsets
RES> example-topic
RES> my-topic
<--->

TIP> 创建新主题
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-topics.sh --create --topic example-topic --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')} --partitions 3 --replication-factor 1<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> Created topic example-topic.
<--->

TIP> 查看主题详情
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-topics.sh --describe --topic example-topic --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')}<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> Topic: example-topic	TopicId: abcdefgh1234	PartitionCount: 3	ReplicationFactor: 1	Configs: 
RES> 	Topic: example-topic	Partition: 0	Leader: 1	Replicas: 1	Isr: 1
RES> 	Topic: example-topic	Partition: 1	Leader: 1	Replicas: 1	Isr: 1
RES> 	Topic: example-topic	Partition: 2	Leader: 1	Replicas: 1	Isr: 1
<--->

TIP> 修改主题配置
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-configs.sh --alter --entity-type topics --entity-name example-topic --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')} --add-config retention.ms=86400000<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> Completed updating config for topic example-topic.
<--->

TIP> 查看主题配置
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-configs.sh --describe --entity-type topics --entity-name example-topic --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')}<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> Dynamic configs for topic example-topic are:
RES>   retention.ms=86400000 sensitive=false synonyms={DYNAMIC_TOPIC_CONFIG:retention.ms=86400000, DEFAULT_CONFIG:retention.ms=604800000}
<--->

# 生产消息
TIP> 生产消息到主题
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-console-producer.sh --broker-list ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')} --topic example-topic<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --producer.config config/kerberos-client.properties</#if>
RES> >Hello, Kafka!
RES> >这是第二条消息
RES> >{"name": "JSON消息", "value": 123}
RES> >^C  (按Ctrl+C退出)
<--->

# 消费消息
TIP> 从头开始消费主题消息
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-console-consumer.sh --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')} --topic example-topic --from-beginning<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --consumer.config config/kerberos-client.properties</#if>
RES> Hello, Kafka!
RES> 这是第二条消息
RES> {"name": "JSON消息", "value": 123}
RES> ^C  (按Ctrl+C退出)
<--->

TIP> 使用消费者组消费消息
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-console-consumer.sh --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')} --topic example-topic --group example-group<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --consumer.config config/kerberos-client.properties</#if>
RES> (此处会显示新消息，按Ctrl+C退出)
<--->

# 消费者组管理
TIP> 列出所有消费者组
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-consumer-groups.sh --list --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')}<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> example-group
RES> console-consumer-12345
<--->

TIP> 查看消费者组详情
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-consumer-groups.sh --describe --group example-group --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')}<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                 HOST            CLIENT-ID
RES> example-group   example-topic   0          10              10              0               consumer-1-abcdef1234567890-1              /192.168.1.101  consumer-1
RES> example-group   example-topic   1          8               8               0               consumer-1-abcdef1234567890-1              /192.168.1.101  consumer-1
RES> example-group   example-topic   2          5               5               0               consumer-1-abcdef1234567890-1              /192.168.1.101  consumer-1
<--->

TIP> 重置消费者组偏移量
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-consumer-groups.sh --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')} --group example-group --reset-offsets --to-earliest --all-topics --execute<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> GROUP                          TOPIC                          PARTITION  NEW-OFFSET     
RES> example-group                  example-topic                  0          0              
RES> example-group                  example-topic                  1          0              
RES> example-group                  example-topic                  2          0              
<--->

# 查看Broker信息
TIP> 查看Broker版本信息
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-broker-api-versions.sh --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')}<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '9092')} (id: 0 rack: null) -> 
RES> 	Produce(0): 0 to 8 [usable: 8]
RES> 	Fetch(1): 0 to 11 [usable: 11]
RES> 	(显示更多API版本信息...)
<--->

# 数据保留策略检查
TIP> 查看日志段信息
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-log-dirs.sh --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')} --describe<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> {
RES>   "brokers": [
RES>     {
RES>       "broker": 0,
RES>       "logDirs": [
RES>         {
RES>           "logDir": "/tmp/kafka-logs",
RES>           "error": null,
RES>           "partitions": [
RES>             {
RES>               "partition": "example-topic-0",
RES>               "size": 12345,
RES>               "offsetLag": 0,
RES>               "isFuture": false
RES>             }
RES>           ]
RES>         }
RES>       ]
RES>     }
RES>   ]
RES> }
<--->

<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
# Kerberos相关操作
TIP> 查看当前Kerberos票据
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> klist
RES> Ticket cache: FILE:/tmp/krb5cc_1000
RES> Default principal: kafka/${data.getBasicInfoValue('host', 'localhost')}@EXAMPLE.COM
RES> 
RES> Valid starting       Expires              Service principal
RES> 01/01/2023 00:00:00  01/02/2023 00:00:00  krbtgt/EXAMPLE.COM@EXAMPLE.COM
<--->

TIP> 获取新的Kerberos票据
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> kinit -kt /etc/security/keytabs/kafka.keytab kafka/${data.getBasicInfoValue('host', 'localhost')}
<--->
</#if>

# 性能测试工具
TIP> 生产者性能测试
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-producer-perf-test.sh --topic example-topic --num-records 100000 --record-size 1024 --throughput 10000 --producer-props bootstrap.servers=${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')}<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> security.protocol=SASL_PLAINTEXT sasl.mechanism=GSSAPI sasl.kerberos.service.name=kafka</#if>
RES> 10000 records sent, 2450.980 records/sec (2.39 MB/sec), 25.40 ms avg latency, 389.00 ms max latency.
RES> 20000 records sent, 3401.361 records/sec (3.32 MB/sec), 12.30 ms avg latency, 52.00 ms max latency.
<--->

TIP> 消费者性能测试
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-consumer-perf-test.sh --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')} --topic example-topic --messages 100000<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --consumer.config config/kerberos-client.properties</#if>
RES> start.time, end.time, data.consumed.in.MB, MB.sec, data.consumed.in.nMsg, nMsg.sec, rebalance.time.ms, fetch.time.ms, fetch.MB.sec, fetch.nMsg.sec
RES> 2023-01-01 00:00:00:000, 2023-01-01 00:00:10:000, 97.6562, 9.7656, 100000, 10000.0000, 20, 9980, 9.7851, 10020.0401
<--->

# 分区管理
TIP> 分区扩容
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'kafka'}]# 
CMD> bin/kafka-topics.sh --bootstrap-server ${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')} --alter --topic example-topic --partitions 6<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'> --command-config config/kerberos-client.properties</#if>
RES> Adding partitions succeeded!
<---> 