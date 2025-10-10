# StarRocks 用户指南

## 环境准备

在部署StarRocks之前，请确保您的环境满足以下要求：

### 硬件要求

**最低配置**（POC或小规模测试环境）：
- **FE节点**：4核CPU，16GB内存，100GB SSD存储
- **BE节点**：8核CPU，32GB内存，200GB SSD存储
- **节点数量**：1 FE + 3 BE（最小高可用部署）

**生产环境推荐配置**：
- **FE节点**：16核CPU，64GB内存，200GB SSD存储
- **BE节点**：32核CPU，128GB内存，1TB SSD存储（或更大）
- **节点数量**：3 FE（1 Leader + 2 Follower）+ 多个BE节点（根据数据规模）

### 操作系统要求

- CentOS 7.x 或更高版本
- Ubuntu 16.04/18.04/20.04 LTS
- 其他支持的Linux发行版

### 软件依赖

- **Java**：Oracle JDK 1.8+ 或 OpenJDK 1.8+
- **GCC**：5.3.1+ 版本
- **时钟同步**：所有节点需通过NTP保持时钟同步
- **网络**：所有节点间必须网络互通，确保相关端口开放

### 推荐工具

- MySQL客户端：用于连接StarRocks
- 监控工具：Prometheus + Grafana
- 日志收集：Filebeat + ELK

## 安装部署

### 获取安装包

首先，从官方网站或GitHub下载最新版本的StarRocks：

```bash
# 下载二进制包
wget https://github.com/StarRocks/starrocks/releases/download/2.5.0/StarRocks-2.5.0.tar.gz

# 解压安装包
tar -xzf StarRocks-2.5.0.tar.gz
cd StarRocks-2.5.0
```

您也可以选择使用Docker镜像或从源码编译：

```bash
# 使用Docker部署
docker pull starrocks/sr-fe
docker pull starrocks/sr-be
```

### 部署FE节点

1. **修改FE配置**

   编辑`fe/conf/fe.conf`文件，配置关键参数：

   ```properties
   # 元数据目录，确保目录存在且有写权限
   meta_dir = /path/to/starrocks/meta
   
   # FE的HTTP端口，用于管理接口
   http_port = 8030
   
   # FE的RPC端口，用于FE之间通信
   rpc_port = 9020
   
   # FE的Query端口，提供MySQL协议服务
   query_port = 9030
   
   # 集群名称，所有节点必须一致
   cluster_name = my_starrocks_cluster
   
   # JVM参数
   JAVA_OPTS = "-Xmx8g -XX:+UseG1GC"
   ```

2. **启动第一个FE节点（作为Leader）**

   ```bash
   cd fe/bin
   ./start_fe.sh --daemon
   ```

3. **检查FE启动状态**

   ```bash
   tail -f fe/log/fe.log
   ```

   当看到类似以下日志时，表示FE节点已成功启动：
   ```
   StarRocks FE starting...
   ```

4. **添加Follower FE节点**（高可用部署）

   首先，通过MySQL客户端连接到Leader FE：

   ```bash
   mysql -h <fe_host> -P 9030 -u root
   ```

   然后执行以下SQL添加Follower节点：

   ```sql
   ALTER SYSTEM ADD FOLLOWER "follower_fe_host:9010";
   ```

   在Follower节点上启动FE进程：

   ```bash
   ./start_fe.sh --daemon --helper leader_fe_host:9010
   ```

5. **添加Observer节点**（可选，用于扩展查询能力）

   ```sql
   ALTER SYSTEM ADD OBSERVER "observer_fe_host:9010";
   ```

### 部署BE节点

1. **修改BE配置**

   编辑`be/conf/be.conf`文件：

   ```properties
   # 数据存储路径，多个路径用分号分隔
   storage_root_path = /path/to/storage1;/path/to/storage2
   
   # BE监听端口
   be_port = 9060
   
   # BE HTTP端口
   webserver_port = 8040
   
   # BE心跳服务端口
   heartbeat_service_port = 9050
   
   # BE Brpc端口
   brpc_port = 8060
   ```

2. **启动BE节点**

   ```bash
   cd be/bin
   ./start_be.sh --daemon
   ```

3. **检查BE启动状态**

   ```bash
   tail -f be/log/be.INFO
   ```

4. **将BE节点加入集群**

   通过MySQL客户端连接到FE，并添加BE节点：

   ```sql
   ALTER SYSTEM ADD BACKEND "be_host:9050";
   ```

### 验证部署

通过以下命令检查集群状态：

```sql
-- 查看FE节点状态
SHOW FRONTEND;

-- 查看BE节点状态
SHOW BACKEND;

-- 查看集群健康状态
SHOW PROC '/cluster_health';
```

## 基本操作指南

### 连接StarRocks

StarRocks兼容MySQL协议，您可以使用MySQL客户端工具连接：

```bash
mysql -h <fe_host> -P 9030 -u root
```

默认用户名是`root`，初始无密码。建议首次登录后立即设置密码：

```sql
SET PASSWORD FOR 'root' = PASSWORD('your_new_password');
```

### 数据库和用户管理

#### 创建数据库

```sql
CREATE DATABASE test_db;
USE test_db;
```

#### 用户管理

```sql
-- 创建新用户
CREATE USER 'test_user' IDENTIFIED BY 'password';

-- 授予权限
GRANT SELECT, INSERT ON test_db.* TO 'test_user'@'%';

-- 撤销权限
REVOKE INSERT ON test_db.* FROM 'test_user'@'%';

-- 删除用户
DROP USER 'test_user'@'%';
```

### 表设计与创建

StarRocks支持多种表模型，根据业务场景选择合适的模型：

#### 明细模型(Duplicate Key)

适用于存储原始明细数据，允许存在重复记录：

```sql
CREATE TABLE orders (
    order_id BIGINT,
    user_id INT,
    product_id INT,
    price DECIMAL(10, 2),
    order_time DATETIME,
    region VARCHAR(50)
) ENGINE = OLAP
DUPLICATE KEY(order_id)
DISTRIBUTED BY HASH(order_id) BUCKETS 10
PROPERTIES (
    "replication_num" = "3",
    "storage_format" = "DEFAULT"
);
```

#### 聚合模型(Aggregate Key)

适用于预聚合数据，减少存储空间和提高聚合查询性能：

```sql
CREATE TABLE sales_summary (
    region VARCHAR(50),
    product_id INT,
    dt DATE,
    total_sales DECIMAL(20, 2) SUM,
    sales_count BIGINT SUM
) ENGINE = OLAP
AGGREGATE KEY(region, product_id, dt)
DISTRIBUTED BY HASH(region, product_id) BUCKETS 10
PROPERTIES (
    "replication_num" = "3"
);
```

#### 更新模型(Unique Key)

适用于需要唯一性约束的场景，如维度表：

```sql
CREATE TABLE users (
    user_id INT,
    username VARCHAR(50),
    email VARCHAR(100),
    register_time DATETIME,
    last_active_time DATETIME,
    status TINYINT
) ENGINE = OLAP
UNIQUE KEY(user_id)
DISTRIBUTED BY HASH(user_id) BUCKETS 10
PROPERTIES (
    "replication_num" = "3"
);
```

#### 主键模型(Primary Key)

提供事务性更新能力，适用于需要频繁更新的场景：

```sql
CREATE TABLE transactions (
    transaction_id BIGINT,
    user_id INT,
    amount DECIMAL(10, 2),
    status TINYINT,
    create_time DATETIME,
    update_time DATETIME
) ENGINE = OLAP
PRIMARY KEY(transaction_id)
DISTRIBUTED BY HASH(transaction_id) BUCKETS 10
PROPERTIES (
    "replication_num" = "3",
    "enable_persistent_index" = "true"
);
```

### 分区与分桶设计

#### 分区设计

分区是StarRocks物理数据组织的主要方式，合理的分区设计对性能至关重要：

```sql
-- 按日期范围分区
CREATE TABLE sales (
    order_id BIGINT,
    user_id INT,
    product_id INT,
    price DECIMAL(10, 2),
    order_time DATETIME
) ENGINE = OLAP
DUPLICATE KEY(order_id)
PARTITION BY RANGE(order_time) (
    PARTITION p202201 VALUES LESS THAN ('2022-02-01 00:00:00'),
    PARTITION p202202 VALUES LESS THAN ('2022-03-01 00:00:00'),
    PARTITION p202203 VALUES LESS THAN ('2022-04-01 00:00:00')
)
DISTRIBUTED BY HASH(order_id) BUCKETS 10
PROPERTIES (
    "replication_num" = "3"
);
```

常见分区策略：
- **时间分区**：按天、周、月分区，适合时间序列数据
- **地理分区**：按国家、地区分区，适合地域数据分析
- **业务分区**：按业务线、产品线分区

#### 分桶设计

分桶决定了数据在BE节点间的分布：

```sql
-- 按多列Hash分桶
CREATE TABLE page_visits (
    page_id INT,
    user_id INT,
    visit_time DATETIME,
    stay_time INT,
    source VARCHAR(50)
) ENGINE = OLAP
DUPLICATE KEY(page_id, user_id)
PARTITION BY RANGE(visit_time) (
    PARTITION p202201 VALUES LESS THAN ('2022-02-01 00:00:00'),
    PARTITION p202202 VALUES LESS THAN ('2022-03-01 00:00:00')
)
DISTRIBUTED BY HASH(page_id, user_id) BUCKETS 20
PROPERTIES (
    "replication_num" = "3"
);
```

分桶数量选择原则：
- 单个Bucket大小建议在10GB以内
- Bucket数量 = 数据总量 / 单个Bucket大小
- 分桶数应该是BE节点数的整数倍

### 数据导入方式

StarRocks支持多种数据导入方式，适应不同的场景需求：

#### Stream Load（小批量实时导入）

适合实时性要求高的小批量数据导入：

```bash
curl --location-trusted -u root: -H "label:label123" \
     -H "column_separator:," -T data.csv \
     -XPUT http://fe_host:8030/api/test_db/orders/_stream_load
```

也可以使用MySQL协议导入：

```sql
LOAD DATA LOCAL INFILE '/path/to/data.csv' 
INTO TABLE orders 
COLUMNS TERMINATED BY ',' 
LINES TERMINATED BY '\n';
```

#### Broker Load（大批量导入）

适合从HDFS、S3等外部存储导入大批量数据：

```sql
LOAD LABEL test_db.label_broker
(
    DATA INFILE("hdfs://hdfs_host:port/path/to/data/*.csv")
    INTO TABLE orders
    COLUMNS TERMINATED BY ","
    (order_id, user_id, product_id, price, order_time, region)
)
WITH BROKER
(
    "username" = "hdfs_user",
    "password" = "hdfs_password"
)
PROPERTIES
(
    "timeout" = "3600",
    "max_filter_ratio" = "0.1"
);
```

#### Routine Load（持续数据导入）

适合从Kafka等消息队列持续导入数据：

```sql
CREATE ROUTINE LOAD test_db.kafka_load ON orders
COLUMNS(order_id, user_id, product_id, price, order_time, region),
PROPERTIES
(
    "format" = "csv",
    "column_separator" = ",",
    "jsonpaths" = "[\"$.order_id\",\"$.user_id\"]"
)
FROM KAFKA
(
    "kafka_broker_list" = "kafka_broker1:9092,kafka_broker2:9092",
    "kafka_topic" = "orders_topic",
    "kafka_partitions" = "0,1,2",
    "kafka_offsets" = "OFFSET_BEGINNING,OFFSET_BEGINNING,OFFSET_BEGINNING"
);
```

#### Spark Load（批量ETL导入）

适合需要ETL转换的批量数据导入：

```sql
LOAD LABEL test_db.label_spark
(
    DATA INFILE("hdfs://hdfs_host:port/path/to/data/*.parquet")
    INTO TABLE orders
    FORMAT AS "parquet"
)
WITH SPARK
(
    "spark.executor.memory" = "4g",
    "spark.shuffle.partitions" = "32"
)
PROPERTIES
(
    "timeout" = "3600"
);
```

#### 通过INSERT导入

从其他表或查询结果导入数据：

```sql
INSERT INTO target_table
SELECT * FROM source_table WHERE condition;
```

### 数据查询优化

#### 基本查询语法

StarRocks支持标准SQL语法：

```sql
-- 基本查询
SELECT order_id, user_id, product_id, price
FROM orders
WHERE region = 'Asia'
  AND order_time >= '2022-01-01'
  AND order_time < '2022-02-01'
ORDER BY price DESC
LIMIT 100;

-- 聚合查询
SELECT region, 
       COUNT(*) AS order_count, 
       SUM(price) AS total_sales,
       AVG(price) AS avg_price
FROM orders
WHERE order_time >= '2022-01-01'
GROUP BY region
ORDER BY total_sales DESC;

-- JOIN查询
SELECT o.order_id, o.price, u.username
FROM orders o
JOIN users u ON o.user_id = u.user_id
WHERE o.order_time >= '2022-01-01';
```

#### 查询优化技巧

1. **利用分区裁剪**：在查询条件中包含分区列

   ```sql
   -- 高效：匹配分区条件
   SELECT * FROM sales WHERE order_time >= '2022-01-01' AND order_time < '2022-02-01';
   
   -- 低效：无法使用分区裁剪
   SELECT * FROM sales WHERE MONTH(order_time) = 1;
   ```

2. **合理选择JOIN策略**

   ```sql
   -- 广播JOIN（小表JOIN大表）
   SELECT /*+ broadcast(dim_table) */ *
   FROM fact_table f
   JOIN dim_table d ON f.dim_id = d.id;
   
   -- Shuffle JOIN（大表JOIN大表）
   SELECT /*+ shuffle(t1, t2) */ *
   FROM large_table1 t1
   JOIN large_table2 t2 ON t1.id = t2.id;
   ```

3. **使用物化视图**

   创建物化视图加速常见查询：

   ```sql
   CREATE MATERIALIZED VIEW mv_sales_by_region
   DISTRIBUTED BY HASH(region) BUCKETS 10
   AS SELECT region, 
             product_id, 
             COUNT(*) AS order_count, 
             SUM(price) AS total_sales
      FROM orders
      GROUP BY region, product_id;
   ```

4. **使用索引**

   为高频查询条件创建索引：

   ```sql
   -- 创建Bitmap索引
   ALTER TABLE orders ADD INDEX idx_region (region) USING BITMAP;
   ```

5. **合理使用缓存**

   ```sql
   -- 查询启用缓存
   SELECT /*+ SET_VAR(enable_query_cache = true) */ *
   FROM orders
   WHERE condition;
   ```

### 数据更新与删除

#### 数据更新

根据表模型，StarRocks支持不同的更新方式：

```sql
-- Unique Key表更新（自动去重）
INSERT INTO users VALUES
(101, 'user101', 'user101@example.com', '2022-01-01', '2022-06-15', 1);

-- 当主键已存在时会更新其他列
INSERT INTO users VALUES
(101, 'user101_new', 'new_email@example.com', '2022-01-01', '2022-06-20', 1);

-- Primary Key表支持标准UPDATE语法
UPDATE transactions
SET status = 2, update_time = NOW()
WHERE transaction_id = 12345;
```

#### 数据删除

```sql
-- 按条件删除数据
DELETE FROM users WHERE user_id = 101;

-- 按分区删除数据
ALTER TABLE sales DROP PARTITION p202201;

-- 清空表数据
TRUNCATE TABLE temp_data;
```

## 高级功能

### 物化视图

物化视图可以预计算和存储常用的聚合结果，显著提升查询性能：

```sql
-- 创建物化视图
CREATE MATERIALIZED VIEW mv_daily_sales
DISTRIBUTED BY HASH(dt) BUCKETS 10
REFRESH ASYNC
AS SELECT product_id, 
          DATE(order_time) AS dt, 
          COUNT(*) AS sales_count, 
          SUM(price) AS total_sales,
          MIN(price) AS min_price,
          MAX(price) AS max_price
   FROM orders
   GROUP BY product_id, DATE(order_time);

-- 查看物化视图
SHOW MATERIALIZED VIEW ON orders;

-- 删除物化视图
DROP MATERIALIZED VIEW mv_daily_sales;
```

StarRocks的查询优化器会自动选择是否使用物化视图。

### 外部表

StarRocks支持直接查询外部数据源，无需导入数据：

#### Hive外部表

```sql
-- 创建Hive Catalog
CREATE EXTERNAL CATALOG hive_catalog
PROPERTIES (
    "type" = "hive",
    "hive.metastore.uris" = "thrift://metastore_host:9083"
);

-- 使用Hive表
SELECT * FROM hive_catalog.hive_db.hive_table LIMIT 100;
```

#### JDBC外部表

```sql
-- 创建MySQL外部表
CREATE EXTERNAL RESOURCE mysql_resource 
PROPERTIES (
    "type" = "jdbc",
    "user" = "mysql_user",
    "password" = "mysql_password",
    "jdbc_url" = "jdbc:mysql://mysql_host:3306",
    "driver_url" = "jdbc:mysql://mysql_host:3306/database",
    "driver_class" = "com.mysql.jdbc.Driver"
);

CREATE EXTERNAL TABLE ext_mysql_table
(
    id INT,
    name VARCHAR(50),
    age INT
)
ENGINE=JDBC
PROPERTIES (
    "resource" = "mysql_resource",
    "table" = "original_mysql_table"
);
```

#### Elasticsearch外部表

```sql
CREATE EXTERNAL TABLE es_table
(
    id INT,
    name VARCHAR(50),
    age INT
)
ENGINE=ELASTICSEARCH
PROPERTIES (
    "hosts" = "http://es_host:9200",
    "index" = "my_index",
    "user" = "elastic_user",
    "password" = "elastic_password"
);
```

### 数据湖分析

StarRocks支持直接查询Hudi、Iceberg等数据湖格式：

```sql
-- 创建Hudi Catalog
CREATE EXTERNAL CATALOG hudi_catalog
PROPERTIES (
    "type" = "hudi",
    "hive.metastore.uris" = "thrift://metastore_host:9083"
);

-- 创建Iceberg Catalog
CREATE EXTERNAL CATALOG iceberg_catalog
PROPERTIES (
    "type" = "iceberg",
    "iceberg.catalog.type" = "hms",
    "hive.metastore.uris" = "thrift://metastore_host:9083"
);
```

### 资源隔离与管理

StarRocks支持资源组功能，实现多租户资源隔离：

```sql
-- 创建资源组
CREATE RESOURCE GROUP group_etl 
PROPERTIES (
    "cpu_core_limit" = "10",
    "mem_limit" = "20%",
    "type" = "normal"
);

-- 分配用户到资源组
ALTER RESOURCE GROUP group_etl ADD USER user1;

-- 在查询中指定资源组
SELECT /*+ SET_VAR(resource_group = 'group_etl') */ * FROM table;
```

### 数据加密与安全

```sql
-- 创建SSL证书
CREATE CERTIFICATE ssl_cert EXPIRE_TIME '2025-01-01';

-- 配置数据传输加密
ALTER SYSTEM PROPERTY SET 'ssl_encryption_enabled' = 'true';

-- 行级别访问控制
CREATE ROW POLICY row_policy_orders ON orders
AS (region = 'user_region') TO USER 'sales_user';
```

## 监控与运维

### 系统监控

StarRocks提供多种方式监控系统状态：

#### 系统视图查询

```sql
-- 查看FE节点状态
SHOW FRONTENDS;

-- 查看BE节点状态
SHOW BACKENDS;

-- 查看Tablet分布
SHOW TABLETS FROM orders;

-- 查看数据导入状态
SHOW LOAD WHERE LABEL = 'label123';

-- 查看查询状态
SHOW PROCESSLIST;

-- 取消长时间运行的查询
KILL QUERY queryId;
```

#### 监控指标

使用Prometheus + Grafana监控StarRocks：

1. **配置FE监控**

   编辑`fe.conf`：
   ```properties
   enable_prometheus_exporter = true
   prometheus_exporter_port = 9181
   ```

2. **配置BE监控**

   编辑`be.conf`：
   ```properties
   enable_prometheus_exporter = true
   prometheus_exporter_port = 9182
   ```

3. **Prometheus配置**

   ```yaml
   scrape_configs:
     - job_name: 'starrocks_fe'
       static_configs:
         - targets: ['fe_host:9181']
     - job_name: 'starrocks_be'
       static_configs:
         - targets: ['be_host1:9182', 'be_host2:9182']
   ```

### 数据备份与恢复

#### 备份数据

```sql
-- 创建备份任务
CREATE REPOSITORY repo_name
WITH BROKER
(
    "fs.defaultFS" = "hdfs://hdfs_host:port",
    "hadoop.username" = "hdfs_user"
);

-- 执行备份
BACKUP DATABASE test_db TO repo_name
ON (orders, users)
PROPERTIES
(
    "type" = "full"
);
```

#### 恢复数据

```sql
-- 查看可用备份
SHOW BACKUP FROM repo_name;

-- 执行恢复
RESTORE DATABASE test_db FROM repo_name
ON (orders, users)
PROPERTIES
(
    "backup_id" = "123456"
);
```

### 集群扩容与缩容

#### 扩容集群

1. **添加BE节点**

   ```sql
   -- 添加新BE节点
   ALTER SYSTEM ADD BACKEND "new_be_host:9050";
   ```

2. **添加FE节点**

   ```sql
   -- 添加Follower节点
   ALTER SYSTEM ADD FOLLOWER "new_fe_host:9010";
   
   -- 添加Observer节点
   ALTER SYSTEM ADD OBSERVER "new_observer_host:9010";
   ```

#### 缩容集群

```sql
-- 下线BE节点
ALTER SYSTEM DECOMMISSION BACKEND "be_host:9050";

-- 删除BE节点（当节点已经失效时）
ALTER SYSTEM DROP BACKEND "be_host:9050";

-- 删除FE节点
ALTER SYSTEM DROP FOLLOWER "fe_host:9010";
```

### 日常维护任务

#### 表优化

```sql
-- 表压缩
ALTER TABLE orders COMPACT;

-- 统计信息收集
ANALYZE TABLE orders;

-- 数据回收
ALTER SYSTEM ADD TRASH EXPIRE "3d";
```

#### 数据均衡

```sql
-- 手动触发数据均衡
ALTER SYSTEM TRIGGER BALANCE;

-- 查看均衡任务
SHOW PROC '/cluster_balance';
```

## 性能调优指南

### SQL优化建议

1. **选择合适的表模型**
   - 明细数据：Duplicate Key模型
   - 需要唯一性：Unique Key或Primary Key模型
   - 需要预聚合：Aggregate Key模型

2. **分区策略优化**
   - 热数据分区粒度更细
   - 冷数据分区可以合并
   - 避免分区过多或过少

3. **JOIN优化**
   - 使用JOIN Hint指定JOIN策略
   - 小表广播JOIN大表
   - 预先过滤数据再JOIN
   - 使用等值JOIN而非范围JOIN

4. **数据倾斜处理**
   - 调整分桶键避免数据倾斜
   - 使用预聚合减轻压力
   - 合理设置并行度

### 系统参数调优

1. **查询并发控制**

   ```sql
   -- 设置全局并发
   SET GLOBAL parallel_fragment_exec_instance_num = 8;
   
   -- 单查询并行度
   SELECT /*+ SET_VAR(parallel_fragment_exec_instance_num=4) */ * FROM orders;
   ```

2. **内存管理**

   ```properties
   # BE配置
   mem_limit = 80%              # BE可使用的最大内存比例
   query_cache_size = 536870912 # 查询缓存大小(512MB)
   
   # FE配置
   metadata_journal_cache_memory_mb = 2048 # 元数据缓存(2GB)
   ```

3. **网络与I/O**

   ```properties
   # BE配置
   compress_rowbatches = true    # 压缩传输数据
   brpc_socket_timeout_ms = 3000 # RPC超时时间
   
   # 存储配置
   storage_page_cache_limit = 40% # 页缓存限制
   ```

### 常见性能问题排查

1. **慢查询分析**

   ```sql
   -- 开启查询分析
   SET enable_profile = true;
   
   -- 执行查询
   SELECT * FROM orders WHERE ...;
   
   -- 获取查询剖析
   SHOW QUERY PROFILE;
   ```

2. **执行计划分析**

   ```sql
   -- 查看执行计划
   EXPLAIN SELECT * FROM orders WHERE ...;
   
   -- 详细执行计划
   EXPLAIN VERBOSE SELECT * FROM orders WHERE ...;
   ```

3. **表状态检查**

   ```sql
   -- 查看表状态
   SHOW TABLET FROM orders;
   
   -- 查看分片分布
   SHOW DATA FROM orders;
   
   -- 查看表统计信息
   SHOW STATS FROM orders;
   ```

## 最佳实践

### 设计原则

1. **数据模型设计**
   - 按照查询模式设计表结构
   - 冷热数据分离存储
   - 合理使用字段类型和长度

2. **数据导入策略**
   - 小批量高频数据：Stream Load
   - 大批量数据：Broker Load
   - 持续导入：Routine Load
   - 数据变更同步：INSERT + Primary Key表

3. **高可用配置**
   - 最少3个FE节点（1 Leader + 2 Follower）
   - BE节点分布在不同机器，至少3个
   - 数据三副本存储

### 运维建议

1. **定期维护任务**
   - 定期备份元数据
   - 定期收集统计信息
   - 监控磁盘使用率，防止空间不足
   - 根据数据增长调整分区策略

2. **资源规划**
   - 为不同业务创建资源组
   - FE节点资源分配偏重内存
   - BE节点均衡分配CPU、内存和存储
   - 冷热数据使用不同存储介质

3. **故障恢复**
   - 建立完整的备份恢复流程
   - 定期演练故障恢复
   - 保持操作系统、Java运行环境稳定

## 常见问题排查

### 导入问题

**问题**：Stream Load导入失败
**排查**：
- 检查数据格式是否正确
- 查看导入详情`SHOW LOAD WHERE LABEL = 'xxx'`
- 检查BE日志中的错误信息

**问题**：Routine Load卡住
**排查**：
- 查看任务状态`SHOW ROUTINE LOAD`
- 检查Kafka连接是否正常
- 查看BE日志是否有错误

### 查询问题

**问题**：查询非常慢
**排查**：
- 分析执行计划是否合理
- 检查是否使用了分区裁剪
- 查看是否存在数据倾斜
- 确认索引和物化视图配置

**问题**：内存不足错误
**排查**：
- 调整查询并行度
- 优化复杂JOIN操作
- 增加BE节点内存配置
- 检查是否有太多并发查询

### 集群问题

**问题**：BE节点频繁掉线
**排查**：
- 检查网络连接稳定性
- 查看BE节点磁盘空间
- 检查系统资源使用情况(CPU/内存)
- 检查BE日志中的错误信息

**问题**：元数据操作失败
**排查**：
- 确认FE Leader节点状态
- 检查FE节点间通信
- 查看FE日志中的错误信息

## 参考资源

- [StarRocks官方文档](https://docs.starrocks.io/)
- [StarRocks GitHub仓库](https://github.com/StarRocks/starrocks)
- [StarRocks官方论坛](https://forum.starrocks.io/)
- [StarRocks常见问题解答](https://docs.starrocks.io/docs/faq/sql_faq/)
- [StarRocks性能调优](https://docs.starrocks.io/docs/administration/Tune_query_performance/) 