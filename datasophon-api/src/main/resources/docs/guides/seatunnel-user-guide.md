# SeaTunnel 用户指南

## 概述

本指南将帮助您在大数据平台中部署、配置和使用SeaTunnel组件。SeaTunnel是一个高性能的实时数据集成平台，支持各种数据源的数据同步、转换和处理，可帮助您高效构建数据处理管道。

## 安装与部署

### 环境准备

在安装SeaTunnel之前，请确保您的环境满足以下条件：

* **Java环境**：JDK 8或更高版本（推荐JDK 8或JDK 11）
* **计算引擎**：根据使用需求，准备以下任一引擎
  * Apache Spark 2.x/3.x
  * Apache Flink 1.12.x/1.13.x/1.14.x
  * SeaTunnel Zeta引擎（无需其他依赖）
* **网络环境**：所有节点之间网络互通
* **资源要求**：
  * 最低配置：2核CPU、4GB内存、10GB磁盘空间
  * 推荐配置：4核以上CPU、8GB以上内存、50GB以上磁盘空间

### 通过DataSophon平台部署

DataSophon平台提供了便捷的方式部署SeaTunnel：

1. 登录DataSophon管理界面
2. 导航至"集群管理" > "添加服务"
3. 在可用组件列表中选择"SeaTunnel"
4. 按照向导指引配置相关参数：
   * 选择版本
   * 选择安装节点
   * 配置连接器参数
   * 选择计算引擎
5. 提交并等待部署完成

### 手动安装步骤

如需手动安装SeaTunnel，请按照以下步骤操作：

1. 下载安装包：

```bash
# 下载最新版本的SeaTunnel
wget https://dlcdn.apache.org/seatunnel/2.3.2/apache-seatunnel-2.3.2-bin.tar.gz

# 解压安装包
tar -xzvf apache-seatunnel-2.3.2-bin.tar.gz
cd apache-seatunnel-2.3.2
```

2. 配置环境变量：

```bash
# 编辑~/.bashrc或/etc/profile文件，添加以下内容
export SEATUNNEL_HOME=/path/to/apache-seatunnel-2.3.2
export PATH=$PATH:$SEATUNNEL_HOME/bin
```

3. 应用环境变量：

```bash
source ~/.bashrc  # 或 source /etc/profile
```

4. 验证安装：

```bash
seatunnel --version
```

## 基本配置

### 配置文件结构

SeaTunnel使用HOCON(Human-Optimized Config Object Notation)格式的配置文件来定义数据处理流程，主要包含三部分：env（环境配置）、source（数据源）和sink（数据目标）。

配置文件基本结构：

```hocon
env {
  # 设置执行引擎
  execution.parallelism = 1
  job.mode = "BATCH"  # 或 "STREAMING"
}

source {
  # 数据源配置
  # 可以配置一个或多个source
  SourcePlugin {
    # 具体source插件的配置参数
  }
}

transform {
  # 可选：数据转换配置
  TransformPlugin {
    # 具体transform插件的配置参数
  }
}

sink {
  # 数据目标配置
  # 可以配置一个或多个sink
  SinkPlugin {
    # 具体sink插件的配置参数
  }
}
```

### 环境配置示例

```hocon
env {
  # 批处理作业配置
  execution.parallelism = 5
  job.mode = "BATCH"
  
  # 流处理作业配置
  # job.mode = "STREAMING"
  # checkpoint.interval = 5000
}
```

### 配置计算引擎

SeaTunnel支持多种计算引擎，需要在启动时指定使用的引擎：

* **Spark引擎**：`seatunnel --config config.conf --master local[4] --deploy-mode client --executor-memory 1G`
* **Flink引擎**：`seatunnel --config config.conf -e flink -m yarn-cluster -p 2`
* **Zeta引擎**：`seatunnel --config config.conf -e seatunnel`

## 数据源配置

SeaTunnel支持多种数据源，以下是一些常见数据源的配置示例：

### 文件数据源

#### 本地/HDFS文件：

```hocon
source {
  File {
    path = "/path/to/file.csv"
    format = "csv"
    schema = "name:string,age:int,score:double"
    csv.separator = ","
    csv.header = true
  }
}
```

#### S3文件：

```hocon
source {
  S3File {
    path = "s3a://bucket/path/to/file"
    format = "parquet"
    bucket = "bucket-name"
    access_key = "your-access-key"
    secret_key = "your-secret-key"
    endpoint = "s3.amazonaws.com"
  }
}
```

### 数据库数据源

#### MySQL：

```hocon
source {
  Jdbc {
    url = "jdbc:mysql://localhost:3306/test"
    driver = "com.mysql.cj.jdbc.Driver"
    user = "root"
    password = "password"
    query = "select * from users"
    connection_check_timeout_sec = 100
  }
}
```

#### PostgreSQL：

```hocon
source {
  Jdbc {
    url = "jdbc:postgresql://localhost:5432/test"
    driver = "org.postgresql.Driver"
    user = "postgres"
    password = "password"
    query = "select * from users"
  }
}
```

### 消息队列数据源

#### Kafka：

```hocon
source {
  Kafka {
    bootstrap.servers = "localhost:9092"
    topic = "test-topic"
    consumer.group = "seatunnel-group"
    format = "json"
    schema = "name:string,age:int,score:double"
    start_mode = "earliest"
  }
}
```

#### Pulsar：

```hocon
source {
  Pulsar {
    client.service-url = "pulsar://localhost:6650"
    admin.service-url = "http://localhost:8080"
    topic = "test-topic"
    subscription.name = "seatunnel-subscription"
    format = "json"
  }
}
```

## 数据转换配置

SeaTunnel支持多种数据转换操作，以下是一些常见转换的配置示例：

### SQL转换

```hocon
transform {
  Sql {
    source_table_name = "input_table"
    result_table_name = "output_table"
    query = """
      SELECT 
        name,
        age,
        CASE WHEN score >= 90 THEN 'A'
             WHEN score >= 80 THEN 'B'
             WHEN score >= 70 THEN 'C'
             ELSE 'D'
        END as grade
      FROM input_table
      WHERE age > 18
    """
  }
}
```

### 过滤转换

```hocon
transform {
  Filter {
    fields = ["name", "age", "score"]
    filter_expr = "age > 18 && score >= 60"
  }
}
```

### 替换转换

```hocon
transform {
  Replace {
    fields {
      name = {
        pattern = "^[A-Z].*"
        replacement = "USER_${name}"
      }
    }
  }
}
```

### 分割转换

```hocon
transform {
  Split {
    fields {
      address = {
        separator = ","
        keys = ["province", "city", "district"]
      }
    }
  }
}
```

## 数据目标配置

SeaTunnel支持将处理后的数据写入多种目标系统，以下是一些常见数据目标的配置示例：

### 文件目标

#### HDFS文件：

```hocon
sink {
  File {
    path = "hdfs:///output/data"
    format = "parquet"
    write_mode = "overwrite"
    compress_codec = "snappy"
  }
}
```

#### 本地文件：

```hocon
sink {
  File {
    path = "/path/to/output"
    format = "csv"
    write_mode = "append"
    csv.separator = "|"
    csv.header = true
  }
}
```

### 数据库目标

#### MySQL：

```hocon
sink {
  Jdbc {
    url = "jdbc:mysql://localhost:3306/test"
    driver = "com.mysql.cj.jdbc.Driver"
    user = "root"
    password = "password"
    table = "target_users"
    primary_keys = ["id"]
    max_retries = 3
    batch_size = 1000
    is_exactly_once = true
  }
}
```

#### Elasticsearch：

```hocon
sink {
  Elasticsearch {
    hosts = ["http://localhost:9200"]
    index = "seatunnel-${now()}"
    index_type = "_doc"
    primary_keys = ["id"]
    username = "elastic"
    password = "elastic"
    max_retries = 3
    batch_size = 1000
  }
}
```

### 消息队列目标

#### Kafka：

```hocon
sink {
  Kafka {
    bootstrap.servers = "localhost:9092"
    topic = "output-topic"
    format = "json"
    partition_key_fields = ["id"]
    semantic = "exactly-once"
  }
}
```

#### Redis：

```hocon
sink {
  Redis {
    host = "localhost"
    port = 6379
    auth = "password"
    key_pattern = "user:{id}"
    data_type = "hash"
    format = "json"
    expire_time = 86400
  }
}
```

## 完整示例

以下是一些常见场景的完整配置示例：

### MySQL到Elasticsearch数据同步

```hocon
env {
  execution.parallelism = 1
  job.mode = "BATCH"
}

source {
  Jdbc {
    url = "jdbc:mysql://mysql-server:3306/test"
    driver = "com.mysql.cj.jdbc.Driver"
    user = "root"
    password = "password"
    query = "select * from users where update_time >= '${yesterday()}'"
    connection_check_timeout_sec = 100
  }
}

transform {
  Filter {
    fields = ["id", "name", "age", "email", "update_time"]
    filter_expr = "age > 18"
  }
}

sink {
  Elasticsearch {
    hosts = ["http://elasticsearch-server:9200"]
    index = "users_${now('yyyyMMdd')}"
    index_type = "_doc"
    primary_keys = ["id"]
    username = "elastic"
    password = "elastic"
    max_retries = 3
    batch_size = 1000
  }
}
```

### Kafka流式处理到Redis

```hocon
env {
  execution.parallelism = 2
  job.mode = "STREAMING"
  checkpoint.interval = 5000
}

source {
  Kafka {
    bootstrap.servers = "kafka-server:9092"
    topic = "user-events"
    consumer.group = "seatunnel-group"
    format = "json"
    schema = "id:string,event:string,timestamp:long,data:string"
    start_mode = "latest"
  }
}

transform {
  Sql {
    source_table_name = "kafka_input"
    result_table_name = "processed_data"
    query = """
      SELECT 
        id,
        event,
        FROM_UNIXTIME(timestamp/1000) as event_time,
        data
      FROM kafka_input
      WHERE event IN ('LOGIN', 'PURCHASE', 'LOGOUT')
    """
  }
}

sink {
  Redis {
    host = "redis-server"
    port = 6379
    auth = "password"
    key_pattern = "user:{id}:{event}"
    data_type = "string"
    format = "json"
    expire_time = 3600
    max_retries = 3
  }
}
```

### CSV文件处理后写入多个目标

```hocon
env {
  execution.parallelism = 3
  job.mode = "BATCH"
}

source {
  File {
    path = "/data/input/sales.csv"
    format = "csv"
    schema = "date:string,region:string,product:string,amount:double"
    csv.separator = ","
    csv.header = true
  }
}

transform {
  Sql {
    source_table_name = "sales"
    result_table_name = "sales_summary"
    query = """
      SELECT 
        region,
        product,
        SUM(amount) as total_sales,
        COUNT(*) as sales_count,
        AVG(amount) as avg_sale
      FROM sales
      GROUP BY region, product
    """
  }
}

sink {
  # 写入MySQL
  Jdbc {
    url = "jdbc:mysql://mysql-server:3306/analytics"
    driver = "com.mysql.cj.jdbc.Driver"
    user = "root"
    password = "password"
    table = "sales_summary"
    primary_keys = ["region", "product"]
    max_retries = 3
    batch_size = 1000
  }
  
  # 同时写入Elasticsearch
  Elasticsearch {
    hosts = ["http://elasticsearch-server:9200"]
    index = "sales_summary"
    index_type = "_doc"
    primary_keys = ["region", "product"]
    username = "elastic"
    password = "elastic"
  }
  
  # 同时保存为文件
  File {
    path = "/data/output/summary"
    format = "parquet"
    write_mode = "overwrite"
    compress_codec = "snappy"
  }
}
```

## 运行SeaTunnel作业

### 使用Spark引擎

```bash
$SEATUNNEL_HOME/bin/seatunnel.sh --config config.conf --master local[4] --deploy-mode client

# 提交到YARN集群
$SEATUNNEL_HOME/bin/seatunnel.sh --config config.conf --master yarn --deploy-mode cluster
```

### 使用Flink引擎

```bash
$SEATUNNEL_HOME/bin/seatunnel.sh --config config.conf -e flink -m local

# 提交到YARN集群
$SEATUNNEL_HOME/bin/seatunnel.sh --config config.conf -e flink -m yarn-cluster -p 2
```

### 使用SeaTunnel Zeta引擎

```bash
$SEATUNNEL_HOME/bin/seatunnel.sh --config config.conf -e seatunnel
```

## 监控与管理

### 日志管理

SeaTunnel的日志文件默认保存在`$SEATUNNEL_HOME/logs`目录下：

* **应用日志**：记录SeaTunnel自身的运行日志
* **引擎日志**：Spark或Flink引擎的执行日志
* **任务日志**：具体任务的执行日志

可通过以下命令查看日志：

```bash
# 查看最新的SeaTunnel日志
tail -f $SEATUNNEL_HOME/logs/seatunnel.log

# 查看特定任务的日志
find $SEATUNNEL_HOME/logs -name "*job_id*" | xargs tail -f
```

### 性能调优

#### 内存配置

* **Spark引擎**：通过`--driver-memory`和`--executor-memory`选项调整内存
* **Flink引擎**：通过`-yjm`和`-ytm`选项调整JobManager和TaskManager内存

```bash
# Spark内存配置示例
$SEATUNNEL_HOME/bin/seatunnel.sh --config config.conf --master yarn --deploy-mode cluster --driver-memory 4G --executor-memory 8G

# Flink内存配置示例
$SEATUNNEL_HOME/bin/seatunnel.sh --config config.conf -e flink -m yarn-cluster -yjm 1024m -ytm 4096m
```

#### 并行度配置

在配置文件的`env`部分或命令行参数中调整并行度：

```hocon
env {
  execution.parallelism = 10  # 调整并行度
}
```

```bash
# 命令行指定并行度（Flink）
$SEATUNNEL_HOME/bin/seatunnel.sh --config config.conf -e flink -p 10
```

#### 批处理大小

对于数据库操作，可以调整批处理大小以提高性能：

```hocon
sink {
  Jdbc {
    # 其他配置...
    batch_size = 5000  # 调整批处理大小
  }
}
```

### 错误处理

SeaTunnel提供了多种错误处理机制：

#### 重试机制

```hocon
sink {
  Jdbc {
    # 其他配置...
    max_retries = 3  # 最大重试次数
    retry_backoff_multiplier_ms = 100  # 重试间隔乘数(ms)
  }
}
```

#### 错误容忍

```hocon
source {
  File {
    # 其他配置...
    parse_error_handle_way = "skip_row"  # 解析错误时跳过该行
    parse_error_max_rows = 100  # 最大容忍错误行数
  }
}
```

## 最佳实践

### 性能优化建议

1. **适当设置并行度**：根据数据量和集群资源调整`execution.parallelism`
2. **使用高效文件格式**：使用列式存储格式如Parquet或ORC以提高性能
3. **批处理调优**：适当增加`batch_size`可以减少网络往返，提高吞吐量
4. **合理使用分区**：对大数据集使用分区可以提高处理和查询效率
5. **SQL优化**：在`Sql`转换中优化SQL查询，避免全表扫描和笛卡尔积

### 配置管理建议

1. **参数化配置**：使用变量替代硬编码的日期、路径等
   ```hocon
   path = "/data/${date}/logs"
   ```

2. **配置模板化**：为不同环境（开发、测试、生产）创建配置模板

3. **配置版本控制**：将配置文件纳入版本控制系统

4. **敏感信息保护**：避免在配置文件中明文存储密码等敏感信息，考虑使用环境变量或密钥管理系统

### 监控与维护建议

1. **任务监控**：集成Prometheus和Grafana监控SeaTunnel任务
2. **资源监控**：监控CPU、内存、磁盘I/O等资源使用情况
3. **定期维护**：定期清理临时文件和过期日志
4. **备份配置**：定期备份关键配置文件

## 故障排查

### 常见问题与解决方案

1. **连接超时**：
   * 检查网络连通性和防火墙设置
   * 增加连接超时参数：`connection_check_timeout_sec = 300`

2. **内存不足**：
   * 增加JVM内存：`--driver-memory 4G --executor-memory 8G`
   * 减少批处理大小或并行度

3. **数据类型不匹配**：
   * 明确定义源和目标的schema
   * 使用转换插件进行类型转换

4. **性能问题**：
   * 检查数据倾斜情况
   * 优化并行度和批处理大小
   * 考虑增加集群资源

### 日志分析

当遇到问题时，查看以下日志以定位问题：

```bash
# 检查SeaTunnel日志
cat $SEATUNNEL_HOME/logs/seatunnel.log | grep ERROR

# 检查Spark日志
yarn logs -applicationId <application_id>

# 检查Flink日志
yarn logs -applicationId <application_id> | grep -i exception
```

## 进阶功能

### 变量和表达式

SeaTunnel支持在配置中使用变量和表达式：

```hocon
env {
  # 定义变量
  custom_variables = {
    db_name = "test"
    table_name = "users"
  }
}

source {
  Jdbc {
    url = "jdbc:mysql://localhost:3306/${db_name}"
    query = "select * from ${table_name} where create_date='${date.format('yyyy-MM-dd', '-1d')}'"
  }
}
```

### 插件开发

如需开发自定义插件，可以参考以下步骤：

1. 创建Maven项目并添加依赖：

```xml
<dependency>
    <groupId>org.apache.seatunnel</groupId>
    <artifactId>seatunnel-api</artifactId>
    <version>${seatunnel.version}</version>
</dependency>
```

2. 实现相应接口：
   * 源插件实现`Source`接口
   * 转换插件实现`Transform`接口
   * 目标插件实现`Sink`接口

3. 打包并部署：
   * 构建JAR包并放入`$SEATUNNEL_HOME/connectors/`目录
   * 重启SeaTunnel服务

### REST API集成

SeaTunnel提供REST API，可以通过HTTP请求提交和管理作业：

1. 启动API服务：

```bash
$SEATUNNEL_HOME/bin/seatunnel-api-server.sh
```

2. 提交作业：

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"config": "env {...}"}'
```

3. 查询作业状态：

```bash
curl -X GET http://localhost:8080/api/v1/jobs/{jobId}
```

## 参考资料

* [SeaTunnel官方文档](https://seatunnel.apache.org/docs/introduction)
* [SeaTunnel GitHub仓库](https://github.com/apache/seatunnel)
* [SeaTunnel连接器列表](https://seatunnel.apache.org/docs/category/connector)
* [SeaTunnel社区](https://seatunnel.apache.org/community/contribution_guide/contribute) 