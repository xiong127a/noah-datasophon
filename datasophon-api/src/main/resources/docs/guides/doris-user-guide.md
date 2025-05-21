# Apache Doris 用户指南

## 开始使用

本指南旨在帮助用户快速上手 Apache Doris，包括安装概览和连接到 Doris 集群。

### 安装概览

Apache Doris 支持多种部署方式，包括单节点部署（用于测试和开发）和集群部署（用于生产环境）。官方推荐使用源码编译或下载预编译的二进制包进行部署。

**部署模式：**

-   **单节点部署 (Standalone)**：将 FE 和 BE 进程部署在同一台机器上。适合快速体验和功能测试。
-   **集群部署 (Cluster)**：
    -   **FE 节点**：建议部署奇数个（例如1、3、5个），以实现高可用。其中一个为 Master，其余为 Follower。至少需要1个 FE。
    -   **BE 节点**：根据数据量和查询负载进行部署，至少需要1个 BE。生产环境通常部署3个或更多 BE 节点。

**基本安装步骤 (以预编译包为例):**

1.  **下载预编译包**：从 Apache Doris 官方网站下载最新的稳定版二进制包。
2.  **环境准备**：
    -   JDK 1.8 或更高版本 (FE 进程需要)。
    -   GCC 7.1+ (BE 编译或运行可能需要，具体看版本要求)。
    -   配置免密登录（如果多机部署）。
    -   关闭防火墙或开放所需端口（FE HTTP 端口默认为 8030，RPC 端口为 9020，查询端口为 9030；BE Thrift 端口默认为 9060，心跳端口为 9050）。
3.  **部署 FE**：
    -   解压 FE 安装包到指定目录。
    -   修改 `fe/conf/fe.conf` 配置文件，主要配置 `meta_dir` (元数据存储目录) 和 `priority_networks` (FE 通信的IP/CIDR)。
    -   如果是多 FE 部署，需要配置 `master_auth_code` (已废弃，通过 `ALTER SYSTEM ADD FOLLOWER` 或 `ALTER SYSTEM ADD OBSERVER` 命令添加) 或确保 helper 节点信息正确。
    -   启动 FE 进程：`sh fe/bin/start_fe.sh --daemon`。
    -   第一次启动 Master FE 后，通过 MySQL 客户端连接 FE，添加其他 Follower FE 和所有 BE 节点信息。
4.  **部署 BE**：
    -   解压 BE 安装包到指定目录。
    -   修改 `be/conf/be.conf` 配置文件，主要配置 `storage_root_path` (数据存储目录，可配置多个，用分号分隔) 和 `priority_networks`。
    -   启动 BE 进程：`sh be/bin/start_be.sh --daemon`。
5.  **将 BE 节点添加到集群**：
    通过 MySQL 客户端连接到 Master FE，执行以下 SQL 将 BE 节点注册到集群：
    ```sql
    ALTER SYSTEM ADD BACKEND "be_host_ip:heartbeat_service_port";
    -- 例如: ALTER SYSTEM ADD BACKEND "192.168.1.10:9050";
    ```
6.  **验证集群状态**：
    通过 `SHOW PROC '/frontends';` 和 `SHOW PROC '/backends';` 查看 FE 和 BE 节点状态。

### 连接到 Doris

Doris 兼容 MySQL 协议，因此可以使用任何支持 MySQL 协议的客户端或驱动程序连接到 Doris FE。

-   **MySQL 客户端 (`mysql-client`)**：
    ```bash
    mysql -h <fe_host_ip> -P <fe_query_port> -u <username> -p<password>
    # 默认端口: 9030
    # 默认用户: root (无密码)
    # 例如:
    mysql -h 127.0.0.1 -P 9030 -u root
    ```

-   **JDBC/ODBC 驱动**：
    -   **JDBC URL 格式**：`jdbc:mysql://<fe_host_ip>:<fe_query_port>/<database_name>?user=<username>&password=<password>`
    -   与连接 MySQL 数据库的配置方式类似。

-   **HTTP API**：
    FE 也提供了 HTTP API（默认端口 8030）用于集群管理、SQL 查询等，但不推荐用于高频查询。

## 数据库和表操作

### 创建数据库

使用 `CREATE DATABASE` 语句创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS my_database
PROPERTIES ("replication_num" = "3"); -- 可选，指定默认副本数，默认为1，生产通常为3
```
切换到数据库：
```sql
USE my_database;
```

### 创建表 (OLAP 表)

创建表是 Doris 中核心的操作。OLAP 表是 Doris 内部管理数据的主要方式。创建表时需要指定数据模型、列定义、分区和分桶策略等。

**数据模型 (Data Models)：**

-   **Duplicate Key Model (明细模型)**：
    -   所有列构成排序键 (`DUPLICATE KEY(...)`)。
    -   完全保留导入的每一行数据，适用于存储原始日志、流水等。
    ```sql
    CREATE TABLE my_database.log_detail (
        event_time DATETIME,
        user_id INT,
        event_type VARCHAR(20),
        message STRING
    )
    DUPLICATE KEY(event_time, user_id)
    PARTITION BY RANGE(event_time) (
        PARTITION p202310 VALUES LESS THAN ("2023-11-01 00:00:00"),
        PARTITION p202311 VALUES LESS THAN ("2023-12-01 00:00:00")
    )
    DISTRIBUTED BY HASH(user_id) BUCKETS 10 -- 按 user_id 哈希分桶，10个桶
    PROPERTIES (
        "replication_num" = "3"
    );
    ```

-   **Aggregate Key Model (聚合模型)**：
    -   指定的列为聚合键 (`AGGREGATE KEY(...)`)，其余为指标列。
    -   聚合键相同的行会被聚合，指标列根据指定的聚合函数（`SUM`, `MIN`, `MAX`, `REPLACE`, `REPLACE_IF_NOT_NULL`, `HLL_UNION`, `BITMAP_UNION`）进行聚合。
    -   适用于预聚合报表数据。
    ```sql
    CREATE TABLE my_database.user_activity_summary (
        event_date DATE,
        user_id INT,
        action_type VARCHAR(50),
        pv BIGINT SUM DEFAULT "0", -- 指标列，使用 SUM 聚合
        uv HLL HLL_UNION DEFAULT "0" -- 指标列，使用 HLL_UNION 聚合 HyperLogLog
    )
    AGGREGATE KEY(event_date, user_id, action_type)
    PARTITION BY RANGE(event_date) (
        PARTITION p202310 VALUES LESS THAN ("2023-11-01"),
        PARTITION p202311 VALUES LESS THAN ("2023-12-01")
    )
    DISTRIBUTED BY HASH(user_id) BUCKETS 16
    PROPERTIES (
        "replication_num" = "3"
    );
    ```

-   **Unique Key Model (唯一键模型)**：
    -   指定的列为唯一键 (`UNIQUE KEY(...)`)，其余为指标列。
    -   保证唯一键的唯一性，后导入的数据会覆盖先导入的具有相同唯一键的数据。
    -   适用于需要按主键更新的业务数据，如用户画像、商品信息等。
    -   底层实现为聚合模型的 `REPLACE` 聚合类型。
    ```sql
    CREATE TABLE my_database.user_profiles (
        user_id INT,
        user_name VARCHAR(100),
        email VARCHAR(255),
        last_updated_time DATETIME
    )
    UNIQUE KEY(user_id)
    DISTRIBUTED BY HASH(user_id) BUCKETS 8
    PROPERTIES (
        "replication_num" = "3",
        "enable_unique_key_merge_on_write" = "true" -- 可选，开启写时合并（Merge-on-Write）优化
    );
    ```

**常用数据类型：**

-   **数值型**: `TINYINT`, `SMALLINT`, `INT`, `BIGINT`, `LARGEINT` (128位整数)
-   **浮点型**: `FLOAT`, `DOUBLE`
-   **高精度**: `DECIMAL(P,S)`, `DECIMALV3(P,S)`
-   **字符串型**: `CHAR(N)`, `VARCHAR(N)`, `STRING` (变长，最大65533字节)
-   **日期时间型**: `DATE`, `DATETIME`, `DATEV2`, `DATETIMEV2`
-   **布尔型**: `BOOLEAN`
-   **半结构化**: `JSONB` (自 Doris 2.1 版本起)
-   **复杂类型**: `ARRAY<T>`, `MAP<K,V>`, `STRUCT<field_name:field_type, ...>`
-   **特殊聚合类型**: `BITMAP`, `HLL` (HyperLogLog)

**分区 (Partitioning)：**

-   主要使用 `RANGE` 分区，通常基于时间列。
-   可以动态增删分区：`ALTER TABLE ... ADD PARTITION ...`, `ALTER TABLE ... DROP PARTITION ...`。
-   Doris 支持动态分区功能，可以自动创建和删除分区。

**分桶 (Bucketing / Distribution)：**

-   使用 `DISTRIBUTED BY HASH(col1, col2, ...)` 将数据哈希分布到指定的 `BUCKETS` 数量中。
-   选择合适的分桶键（高基数列）和分桶数量对于数据均衡和查询性能非常重要。
-   Doris 支持 `RANDOM` 分桶（`DISTRIBUTED BY RANDOM BUCKETS N`），数据会随机分配到桶中，适用于无明显高基数列的场景。

### 创建 Rollup 和物化视图

-   **Rollup (上卷索引)**：
    Rollup 是基表的物理聚合，包含基表的部分列，并可以有不同的排序键和聚合定义。查询时优化器会自动选择最优的 Rollup。
    ```sql
    ALTER TABLE my_database.log_detail ADD ROLLUP event_type_rollup (event_time, event_type);
    ```

-   **物化视图 (Materialized View)**：
    物化视图存储预计算的查询结果。Doris 的物化视图是与基表同步更新的（通常在导入数据后异步构建）。
    ```sql
    CREATE MATERIALIZED VIEW my_database.daily_sales_mv AS
    SELECT
        sale_date,
        product_id,
        SUM(amount) as total_amount,
        COUNT(DISTINCT order_id) as distinct_orders
    FROM my_database.sales_records
    GROUP BY sale_date, product_id;
    ```

## 数据导入

Doris 提供多种数据导入方式，以适应不同的数据源和导入需求。

### Stream Load

-   通过 HTTP 协议将本地文件或数据流导入 Doris。同步执行，返回导入结果。
-   适合导入几GB以内的数据，常用于实时或小批量数据导入。
-   支持 CSV、JSON 格式。
    ```bash
    curl --location-trusted -u user:passwd -H "label:mylabel123" \
        -H "column_separator:|" -H "columns:col1,col2,tmp_col3" \
        -T data.txt http://fe_host:http_port/api/db_name/table_name/_stream_load
    ```

### Broker Load

-   通过 Broker 进程读取外部存储系统（如 HDFS, S3）上的数据文件导入 Doris。异步执行。
-   适合导入大数据量的文件，支持 Parquet, ORC, CSV, JSON 等格式。
    ```sql
    LOAD LABEL my_database.label_load_hdfs
    (
        DATA INFILE("hdfs://namenode:port/user/doris/data/my_table/*")
        INTO TABLE my_table
        COLUMNS TERMINATED BY ","
        (col1, col2, col3)
    )
    WITH BROKER "my_hdfs_broker"
    (
        "username"="hdfs_user",
        "password"="hdfs_password"
    )
    PROPERTIES
    (
        "timeout"="3600",
        "max_filter_ratio"="0.1"
    );
    ```

### Routine Load

-   持续消费 Kafka 中的数据并自动导入 Doris。用于流式数据导入。
    ```sql
    CREATE ROUTINE LOAD my_database.routine_load_kafka ON my_table
    COLUMNS TERMINATED BY ",",
    PROPERTIES
    (
        "desired_concurrent_number"="3",
        "max_batch_interval" = "20",
        "max_batch_rows" = "300000",
        "max_batch_size" = "209715200",
        "strict_mode" = "false"
    )
    FROM KAFKA
    (
        "kafka_broker_list" = "kafka1:9092,kafka2:9092",
        "kafka_topic" = "my_topic",
        "kafka_partitions" = "0,1,2",
        "property.kafka_group_id" = "my_group"
    );
    ```

### Spark Load

-   通过外部的 Spark 集群进行数据转换和预处理后，将数据导入 Doris。适合ETL后的大规模数据导入。
-   需要部署 `doris-spark-connector`。

### INSERT INTO

-   标准的 SQL `INSERT INTO ... VALUES ...` 或 `INSERT INTO ... SELECT ...`。
-   `VALUES` 方式适合小批量数据写入或测试。
-   `SELECT` 方式可以将 Doris 表或其他外部表（通过联邦查询）的数据导入目标表。
    ```sql
    INSERT INTO my_database.target_table (col1, col2) VALUES (1, 'A'), (2, 'B');

    INSERT INTO my_database.summary_table (dt, user_count)
    SELECT event_date, COUNT(DISTINCT user_id) FROM my_database.log_detail GROUP BY event_date;
    ```

## 查询数据

Doris 使用标准 SQL 进行数据查询。

### 基本 SELECT 查询

```sql
SELECT column1, column2
FROM my_database.my_table
WHERE column3 = 'some_value' AND column4 > 100
ORDER BY column1 DESC
LIMIT 100;
```

### 过滤数据 (WHERE)

利用 `WHERE` 子句进行高效过滤。对于分区表，基于分区列的过滤可以实现分区剪枝。

### 数据聚合 (GROUP BY)

配合聚合函数（`COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `COUNT(DISTINCT ...)`）进行数据聚合。
```sql
SELECT
    country,
    platform,
    SUM(revenue) AS total_revenue,
    COUNT(DISTINCT user_id) AS active_users
FROM my_database.sales_data
WHERE event_date >= '2023-01-01'
GROUP BY country, platform
HAVING total_revenue > 10000
ORDER BY total_revenue DESC;
```

### 连接表 (JOIN)

Doris 支持 `INNER JOIN`, `LEFT JOIN`, `RIGHT JOIN`, `FULL OUTER JOIN`, `CROSS JOIN`。
-   Doris 的查询优化器支持多种 JOIN 算法，如 Hash Join, Broadcast Join, Shuffle Join。
-   对于大表 JOIN，确保连接键的数据类型一致，并考虑分桶键对 JOIN 性能的影响。

```sql
SELECT
    o.order_id,
    o.order_amount,
    c.customer_name,
    c.region
FROM my_database.orders AS o
INNER JOIN my_database.customers AS c ON o.customer_id = c.customer_id
WHERE o.order_date = '2023-10-26';
```

### 子查询

支持在 `SELECT`, `FROM`, `WHERE`, `IN` 子句中使用子查询。

```sql
SELECT product_name, sales
FROM my_database.product_sales
WHERE sales > (SELECT AVG(sales) FROM my_database.product_sales WHERE category = 'Electronics');
```

### 窗口函数

Doris 支持 SQL 标准窗口函数，如 `ROW_NUMBER()`, `RANK()`, `DENSE_RANK()`, `LAG()`, `LEAD()`, 以及聚合函数的窗口版本。

```sql
SELECT
    user_id,
    order_date,
    amount,
    SUM(amount) OVER (PARTITION BY user_id ORDER BY order_date ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumulative_amount
FROM my_database.user_orders
LIMIT 100;
```

### 常用 SQL 函数

Doris 提供了丰富的内置函数，包括数学函数、字符串函数、日期时间函数、类型转换函数、条件函数、聚合函数、位图函数、HLL 函数、JSON 函数等。
-   **位图函数**: `BITMAP_UNION()`, `BITMAP_COUNT()`, `TO_BITMAP()`
-   **HLL 函数**: `HLL_UNION()`, `HLL_CARDINALITY()`, `TO_HLL()`

## 数据管理

### Schema Change (在线表结构变更)

Doris 支持在线修改表结构（Schema Change），操作期间数据仍可读写。常见的变更包括：
-   增加列、删除列、修改列类型、修改列注释
-   调整列顺序
-   增加、删除 Rollup 索引

```sql
-- 增加列
ALTER TABLE my_database.my_table ADD COLUMN new_col INT DEFAULT 0 AFTER existing_col;

-- 修改列类型 (有一定限制，需兼容)
ALTER TABLE my_database.my_table MODIFY COLUMN col_to_change VARCHAR(200);

-- 删除 Rollup
ALTER TABLE my_database.my_table DROP ROLLUP my_rollup_name;
```
Schema Change 是一个异步操作，可以通过 `SHOW ALTER TABLE COLUMN;` 查看进度。

### 备份与恢复 (Backup and Restore)

Doris 支持对表或分区级别的数据进行快照式备份，并将备份数据存储到远端存储系统（如 HDFS, S3）。

```sql
-- 创建远程仓库
CREATE REPOSITORY my_backup_repo
WITH BROKER "my_hdfs_broker"
ON LOCATION "hdfs://namenode:port/doris_backup_repo"
PROPERTIES (
    "username" = "hdfs_user",
    "password" = "hdfs_password",
    "read_only" = "false"
);

-- 备份表
BACKUP SNAPSHOT my_database.snapshot_20231027
TO my_backup_repo
ON (my_table1, my_table2 PARTITION (p202310, p202309))
PROPERTIES ("type" = "FULL");

-- 查看备份
SHOW SNAPSHOT ON my_backup_repo WHERE SNAPSHOT = "snapshot_20231027";

-- 恢复表
RESTORE SNAPSHOT my_database.snapshot_20231027
FROM my_backup_repo
ON (my_table1_restored AS my_table1)
PROPERTIES ("backup_timestamp"="2023-10-27-08-00-00");
```

### Compaction (数据合并)

BE 节点会自动执行 Compaction 操作，合并小的数据版本，回收已删除数据，整理数据存储，以提高查询性能和空间利用率。用户可以通过配置调整 Compaction 策略。

### 监控

-   **FE/BE Metrics**: Doris FE 和 BE 都会暴露大量的 Metrics 信息，可以通过 HTTP API (`/metrics`) 获取，方便集成到 Prometheus + Grafana 等监控系统。
-   **Audit Log**: 记录所有通过 FE 执行的 SQL 请求，包括用户、时间、耗时、状态等。可以通过 FE 的 HTTP API (`/api/audit_log`) 或直接访问 FE 日志目录下的 `fe.audit.log` 文件获取。
-   **`SHOW PROC` 命令**: 提供查看集群内部状态的多种路径，如 `SHOW PROC '/frontends'`, `SHOW PROC '/backends'`, `SHOW PROC '/cluster_balance'`, `SHOW PROC '/statistic'` 等。
-   **Information Schema**: 提供了 `tables`, `columns`, `partitions` 等标准元数据视图。

## 最佳实践

### Schema 设计

-   **选择合适的数据模型**：根据业务需求选择 Duplicate, Aggregate, 或 Unique Key 模型。
-   **合理设计排序键/聚合键/唯一键**：将常用于过滤和等值查询的列放在前面。
-   **分区策略**：合理设置分区粒度，避免分区过多或过少。利用动态分区管理数据生命周期。
-   **分桶策略**：选择高基数、分布均匀的列作为分桶键。分桶数一般设置为 BE 节点数的整数倍，单个 Tablet 大小建议在 1GB - 10GB。
-   **使用 Rollup/物化视图**：为高频查询或复杂聚合创建预聚合结构。
-   **数据类型选择**：使用最精确且占用空间最小的数据类型。善用 `BITMAP` 和 `HLL` 进行去重计数。

### 查询优化

-   **`EXPLAIN <SQL>`**：查看查询的执行计划，分析瓶颈。
-   **精确选择列**：避免 `SELECT *`。
-   **有效利用谓词下推**：确保过滤条件尽可能作用在数据扫描阶段。
-   **JOIN 优化**：小表在右，大表在左（对于 Broadcast Join）；确保连接键类型一致；利用分桶键进行 Colocation Join。
-   **使用近似查询函数**：如 `APPROX_COUNT_DISTINCT` 替代 `COUNT(DISTINCT ...)`。
-   **管理查询并发和内存**：通过用户属性或查询变量控制资源使用。

### 数据导入

-   **选择合适的导入方式**：Stream Load 适合小批量实时，Broker Load 适合大批量离线，Routine Load 适合 Kafka 流式数据。
-   **控制导入频率和批次大小**：避免过于频繁或过小的导入，以减少 Compaction 压力。
-   **处理导入错误**：关注导入任务的状态和错误信息，设置合理的 `max_filter_ratio`。

Apache Doris 功能强大且在持续快速发展中。本指南提供了基础入门，更详细和最新的信息请参考 Apache Doris 官方文档。 