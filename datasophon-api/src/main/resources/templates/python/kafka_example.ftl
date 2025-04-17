DEPENDENCIES_START
# pip依赖：
kafka-python==2.0.2
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
gssapi==1.7.3  # 用于Kerberos认证
</#if>
DEPENDENCIES_END

#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Kafka Python客户端示例代码
环境要求：Python 3.6+
依赖安装：pip install kafka-python

<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
依赖安装：pip install gssapi
</#if>

<#if data.getSecurityInfoValue('ssl.enabled', 'false') == 'true'>
SSL证书准备：
- 确保您有对应的truststore和keystore文件
</#if>
"""

from kafka import KafkaProducer, KafkaConsumer, KafkaAdminClient
from kafka.admin import NewTopic
from kafka.errors import KafkaError
import json
import logging

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Kafka服务器配置
bootstrap_servers = '${data.getConnectInfoValue('bootstrapServers', 'localhost:9092')}'
# ZooKeeper服务器配置
zookeeper_servers = '${data.getConnectInfoValue('zkConnect', 'localhost:2181')}'

def create_kafka_admin_client():
    """创建Kafka管理客户端"""
    config = {
        'bootstrap_servers': bootstrap_servers,
        'client_id': 'kafka-python-admin'
    }
    
<#if data.getSecurityInfoValue('ssl.enabled', 'false') == 'true'>
    # SSL配置
    config.update({
        'security_protocol': 'SSL',
        'ssl_cafile': '${data.getSecurityInfoValue('ssl.ca.location', '/path/to/ca.pem')}',
        'ssl_certfile': '${data.getSecurityInfoValue('ssl.certificate.location', '/path/to/client.pem')}',
        'ssl_keyfile': '${data.getSecurityInfoValue('ssl.key.location', '/path/to/client.key')}',
        'ssl_password': '${data.getSecurityInfoValue('ssl.key.password', 'key-password')}'
    })
</#if>

<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
    # Kerberos配置
    import os
    os.environ['KRB5_CONFIG'] = '${data.getSecurityInfoValue('krb5.file.path', '/etc/krb5.conf')}'
    
    config.update({
        'security_protocol': 'SASL_PLAINTEXT',
        'sasl_mechanism': 'GSSAPI',
        'sasl_kerberos_service_name': 'kafka'
    })
</#if>

    try:
        admin_client = KafkaAdminClient(**config)
        return admin_client
    except KafkaError as e:
        logger.error(f"创建Kafka管理客户端失败: {e}")
        raise

def create_kafka_producer():
    """创建Kafka生产者"""
    config = {
        'bootstrap_servers': bootstrap_servers,
        'value_serializer': lambda v: json.dumps(v).encode('utf-8'),
        'key_serializer': lambda k: k.encode('utf-8') if k else None,
        'acks': 'all'
    }
    
<#if data.getSecurityInfoValue('ssl.enabled', 'false') == 'true'>
    # SSL配置
    config.update({
        'security_protocol': 'SSL',
        'ssl_cafile': '${data.getSecurityInfoValue('ssl.ca.location', '/path/to/ca.pem')}',
        'ssl_certfile': '${data.getSecurityInfoValue('ssl.certificate.location', '/path/to/client.pem')}',
        'ssl_keyfile': '${data.getSecurityInfoValue('ssl.key.location', '/path/to/client.key')}',
        'ssl_password': '${data.getSecurityInfoValue('ssl.key.password', 'key-password')}'
    })
</#if>

<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
    # Kerberos配置
    import os
    os.environ['KRB5_CONFIG'] = '${data.getSecurityInfoValue('krb5.file.path', '/etc/krb5.conf')}'
    
    config.update({
        'security_protocol': 'SASL_PLAINTEXT',
        'sasl_mechanism': 'GSSAPI',
        'sasl_kerberos_service_name': 'kafka'
    })
</#if>

    try:
        producer = KafkaProducer(**config)
        return producer
    except KafkaError as e:
        logger.error(f"创建Kafka生产者失败: {e}")
        raise

def create_kafka_consumer(topic, group_id='example-group'):
    """创建Kafka消费者"""
    config = {
        'bootstrap_servers': bootstrap_servers,
        'group_id': group_id,
        'auto_offset_reset': 'earliest',
        'value_deserializer': lambda v: json.loads(v.decode('utf-8')),
        'key_deserializer': lambda k: k.decode('utf-8') if k else None
    }
    
<#if data.getSecurityInfoValue('ssl.enabled', 'false') == 'true'>
    # SSL配置
    config.update({
        'security_protocol': 'SSL',
        'ssl_cafile': '${data.getSecurityInfoValue('ssl.ca.location', '/path/to/ca.pem')}',
        'ssl_certfile': '${data.getSecurityInfoValue('ssl.certificate.location', '/path/to/client.pem')}',
        'ssl_keyfile': '${data.getSecurityInfoValue('ssl.key.location', '/path/to/client.key')}',
        'ssl_password': '${data.getSecurityInfoValue('ssl.key.password', 'key-password')}'
    })
</#if>

<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
    # Kerberos配置
    import os
    os.environ['KRB5_CONFIG'] = '${data.getSecurityInfoValue('krb5.file.path', '/etc/krb5.conf')}'
    
    config.update({
        'security_protocol': 'SASL_PLAINTEXT',
        'sasl_mechanism': 'GSSAPI',
        'sasl_kerberos_service_name': 'kafka'
    })
</#if>

    try:
        consumer = KafkaConsumer(topic, **config)
        return consumer
    except KafkaError as e:
        logger.error(f"创建Kafka消费者失败: {e}")
        raise

def create_topic(topic_name, num_partitions=1, replication_factor=1):
    """创建Kafka主题"""
    try:
        admin_client = create_kafka_admin_client()
        topic = NewTopic(name=topic_name, 
                         num_partitions=num_partitions, 
                         replication_factor=replication_factor)
        admin_client.create_topics([topic])
        logger.info(f"主题 {topic_name} 创建成功")
        admin_client.close()
    except KafkaError as e:
        logger.error(f"创建主题失败: {e}")
        raise

def list_topics():
    """列出Kafka所有主题"""
    try:
        admin_client = create_kafka_admin_client()
        topics = admin_client.list_topics()
        logger.info(f"获取到的主题列表: {topics}")
        admin_client.close()
        return topics
    except KafkaError as e:
        logger.error(f"获取主题列表失败: {e}")
        raise

def delete_topic(topic_name):
    """删除Kafka主题"""
    try:
        admin_client = create_kafka_admin_client()
        admin_client.delete_topics([topic_name])
        logger.info(f"主题 {topic_name} 删除成功")
        admin_client.close()
    except KafkaError as e:
        logger.error(f"删除主题失败: {e}")
        raise

def produce_message(topic, key, value):
    """生产消息到Kafka"""
    try:
        producer = create_kafka_producer()
        future = producer.send(topic, key=key, value=value)
        record_metadata = future.get(timeout=10)
        logger.info(f"消息发送成功: topic={record_metadata.topic}, partition={record_metadata.partition}, offset={record_metadata.offset}")
        producer.flush()
        producer.close()
    except KafkaError as e:
        logger.error(f"发送消息失败: {e}")
        raise

def consume_messages(topic, timeout_ms=5000):
    """从Kafka消费消息"""
    try:
        consumer = create_kafka_consumer(topic)
        logger.info(f"开始从主题 {topic} 消费消息...")
        
        # 设置超时时间，避免无限等待
        messages = consumer.poll(timeout_ms=timeout_ms)
        
        if not messages:
            logger.info(f"在 {timeout_ms}ms 内没有收到任何消息")
        
        for tp, records in messages.items():
            logger.info(f"从分区 {tp.partition} 收到 {len(records)} 条消息")
            for record in records:
                logger.info(f"消息: key={record.key}, value={record.value}, offset={record.offset}")
        
        consumer.close()
    except KafkaError as e:
        logger.error(f"消费消息失败: {e}")
        raise

def main():
    """主函数，演示Kafka客户端的基本操作"""
    topic_name = 'example-topic'
    
    try:
        # 打印配置信息
        logger.info(f"Kafka集群地址: {bootstrap_servers}")
        logger.info(f"ZooKeeper集群地址: {zookeeper_servers}")
        
        # 1. 列出现有主题
        logger.info("列出现有主题:")
        existing_topics = list_topics()
        
        # 2. 创建新主题
        if topic_name not in existing_topics:
            logger.info(f"创建主题 {topic_name}")
            create_topic(topic_name)
        
        # 3. 生产消息
        logger.info("生产消息")
        produce_message(topic_name, "key1", {"message": "Hello, Kafka!"})
        
        # 4. 消费消息
        logger.info("消费消息")
        consume_messages(topic_name)
        
        # 5. 删除主题 (谨慎使用)
        # logger.info(f"删除主题 {topic_name}")
        # delete_topic(topic_name)
        
    except Exception as e:
        logger.error(f"运行示例时出错: {e}")

if __name__ == "__main__":
    main() 