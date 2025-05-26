# Logstash 用户指南

## 环境准备

### 系统要求

在安装 Logstash 之前，请确保您的系统满足以下要求：

- Java 8 或更高版本（推荐使用 OpenJDK）
- 足够的 CPU 和内存资源（建议至少 2GB RAM）
- 充足的磁盘空间（用于持久化队列）

### 安装配置

1. **下载和解压**
   ```bash
   wget https://artifacts.elastic.co/downloads/logstash/logstash-8.x.x.tar.gz
   tar -xzf logstash-8.x.x.tar.gz
   cd logstash-8.x.x
   ```

2. **配置环境变量**
   ```bash
   export LOGSTASH_HOME=/path/to/logstash
   export PATH=$LOGSTASH_HOME/bin:$PATH
   ```

3. **基本配置**
   编辑 `config/logstash.yml`：
   ```yaml
   node.name: logstash-01
   path.data: /var/lib/logstash
   pipeline.workers: 2
   pipeline.batch.size: 125
   pipeline.batch.delay: 50
   queue.type: persisted
   ```

## 管道配置

### 基本语法

Logstash 管道配置包含三个主要部分：input、filter 和 output。

```ruby
input {
  # 输入插件配置
}

filter {
  # 过滤器插件配置
}

output {
  # 输出插件配置
}
```

### 常用输入配置

1. **文件输入**
   ```ruby
   input {
     file {
       path => "/var/log/*.log"
       start_position => "beginning"
       sincedb_path => "/var/lib/logstash/sincedb"
       type => "system-logs"
     }
   }
   ```

2. **Beats 输入**
   ```ruby
   input {
     beats {
       port => 5044
       host => "0.0.0.0"
       client_inactivity_timeout => 60
     }
   }
   ```

3. **Kafka 输入**
   ```ruby
   input {
     kafka {
       bootstrap_servers => "kafka1:9092,kafka2:9092"
       topics => ["logs-topic"]
       group_id => "logstash-group"
       codec => json
     }
   }
   ```

### 过滤器配置

1. **Grok 过滤器**
   ```ruby
   filter {
     grok {
       match => {
         "message" => "%{COMBINEDAPACHELOG}"
       }
     }
   }
   ```

2. **日期过滤器**
   ```ruby
   filter {
     date {
       match => [ "timestamp", "dd/MMM/yyyy:HH:mm:ss Z" ]
       target => "@timestamp"
     }
   }
   ```

3. **JSON 过滤器**
   ```ruby
   filter {
     json {
       source => "message"
       target => "parsed_json"
     }
   }
   ```

### 输出配置

1. **Elasticsearch 输出**
   ```ruby
   output {
     elasticsearch {
       hosts => ["http://elasticsearch:9200"]
       index => "logstash-%{+YYYY.MM.dd}"
       user => "elastic"
       password => "your_password"
     }
   }
   ```

2. **文件输出**
   ```ruby
   output {
     file {
       path => "/var/log/logstash/processed/%{+YYYY-MM-dd}.log"
       codec => line { format => "%{message}"}
     }
   }
   ```

## 插件管理

### 插件安装

1. **安装官方插件**
   ```bash
   bin/logstash-plugin install logstash-input-kafka
   ```

2. **查看已安装插件**
   ```bash
   bin/logstash-plugin list
   ```

3. **更新插件**
   ```bash
   bin/logstash-plugin update logstash-input-kafka
   ```

### 常用插件示例

1. **Mutate 过滤器**
   ```ruby
   filter {
     mutate {
       add_field => { "environment" => "production" }
       remove_field => [ "message" ]
       rename => { "old_field" => "new_field" }
       gsub => [
         "fieldname", "pattern", "replacement"
       ]
     }
   }
   ```

2. **Ruby 过滤器**
   ```ruby
   filter {
     ruby {
       code => '
         event.set("timestamp_ms", 
           event.get("@timestamp").to_i * 1000
         )
       '
     }
   }
   ```

## 高级配置

### 多管道配置

1. **配置文件**
   编辑 `config/pipelines.yml`：
   ```yaml
   - pipeline.id: apache_logs
     path.config: "/etc/logstash/conf.d/apache.conf"
     pipeline.workers: 2
   
   - pipeline.id: mysql_logs
     path.config: "/etc/logstash/conf.d/mysql.conf"
     pipeline.workers: 1
   ```

2. **管道配置示例**
   ```ruby
   # apache.conf
   input {
     file {
       path => "/var/log/apache2/access.log"
     }
   }
   filter {
     grok {
       match => { "message" => "%{COMBINEDAPACHELOG}" }
     }
   }
   output {
     elasticsearch {
       index => "apache-logs-%{+YYYY.MM.dd}"
     }
   }
   ```

### 持久化队列

1. **启用持久化队列**
   ```yaml
   queue.type: persisted
   path.queue: "/path/to/queue"
   queue.max_bytes: 4gb
   ```

2. **队列参数调优**
   ```yaml
   queue.checkpoint.writes: 1024
   queue.checkpoint.acks: true
   queue.drain: false
   ```

### 监控配置

1. **启用监控**
   ```yaml
   xpack.monitoring.enabled: true
   xpack.monitoring.elasticsearch.hosts: ["http://elasticsearch:9200"]
   ```

2. **指标收集**
   ```yaml
   xpack.monitoring.collection.interval: 10s
   xpack.monitoring.collection.pipeline.details.enabled: true
   ```

## 性能优化

### 内存配置

1. **JVM 设置**
   编辑 `config/jvm.options`：
   ```
   -Xms2g
   -Xmx2g
   -XX:+UseG1GC
   -XX:G1ReservePercent=25
   ```

2. **批处理设置**
   ```yaml
   pipeline.batch.size: 125
   pipeline.batch.delay: 50
   ```

### 工作线程优化

1. **工作线程配置**
   ```yaml
   pipeline.workers: 4
   pipeline.ordered: false
   ```

2. **过滤器工作线程**
   ```yaml
   pipeline.filter.workers: 4
   ```

## 故障排除

### 常见问题

1. **内存问题**
   - 检查 JVM 堆大小设置
   - 监控垃圾回收情况
   - 调整批处理参数

2. **性能问题**
   - 检查管道配置
   - 优化过滤器逻辑
   - 调整工作线程数

### 日志分析

1. **查看日志**
   ```bash
   tail -f logs/logstash-plain.log
   ```

2. **调试模式**
   ```bash
   bin/logstash -e 'input { stdin { } } output { stdout { codec => rubydebug } }'
   ```

## 最佳实践

### 配置管理

1. **配置文件组织**
   ```
   /etc/logstash/
   ├── conf.d/
   │   ├── inputs/
   │   ├── filters/
   │   └── outputs/
   ├── patterns/
   └── templates/
   ```

2. **配置测试**
   ```bash
   bin/logstash -f config/test.conf --config.test_and_exit
   ```

### Grok 模式

1. **自定义模式**
   ```ruby
   filter {
     grok {
       patterns_dir => ["/etc/logstash/patterns"]
       match => { "message" => "%{CUSTOM_PATTERN}" }
     }
   }
   ```

2. **常用模式示例**
   ```
   # /etc/logstash/patterns/custom
   CUSTOM_LOG %{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}
   ```

### 数据处理

1. **字段处理**
   ```ruby
   filter {
     mutate {
       convert => {
         "bytes" => "integer"
         "duration" => "float"
       }
     }
   }
   ```

2. **条件处理**
   ```ruby
   filter {
     if [status] =~ /^5\d\d/ {
       mutate {
         add_tag => ["error"]
       }
     }
   }
   ```

### 安全配置

1. **SSL/TLS 配置**
   ```ruby
   input {
     beats {
       port => 5044
       ssl => true
       ssl_certificate => "/etc/pki/tls/certs/server.crt"
       ssl_key => "/etc/pki/tls/private/server.key"
     }
   }
   ```

2. **认证配置**
   ```ruby
   output {
     elasticsearch {
       user => "logstash_writer"
       password => "${ES_PWD}"
       ssl => true
       cacert => "/etc/pki/tls/certs/ca.crt"
     }
   }
   ```

本指南涵盖了 Logstash 的主要使用方法和最佳实践。随着 Logstash 的持续发展，建议定期查看官方文档以获取最新信息和更新。在实际使用中，请根据具体需求和环境调整配置参数。