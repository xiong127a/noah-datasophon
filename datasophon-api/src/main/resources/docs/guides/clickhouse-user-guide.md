# ClickHouse 用户指南

## 开始使用

本指南将帮助您开始使用 ClickHouse，包括安装概览和连接到数据库。

### 安装概览

ClickHouse 可以在多种 Linux 发行版上安装，也支持 Docker 容器化部署。官方推荐使用预编译的 `deb` 或 `rpm` 包进行安装。

**基本步骤（以 DEB 包为例）：**

1.  **添加 ClickHouse 仓库**：
    ```bash
    sudo apt-get install apt-transport-https ca-certificates dirmngr
    sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv E0C56BD4    # 对于较新版本，可能使用不同的 key

    echo "deb https://packages.clickhouse.com/deb stable main" | sudo tee \
        /etc/apt/sources.list.d/clickhouse.list
    sudo apt-get update
    ```

2.  **安装 ClickHouse 服务器和客户端**：
    ```bash
    sudo apt-get install -y clickhouse-server clickhouse-client
    ```

3.  **启动 ClickHouse 服务器**：
    ```bash
    sudo service clickhouse-server start
    ```
    您可以使用 `sudo service clickhouse-server status` 检查服务状态。

4.  **验证安装**：
    连接到服务器：
    ```bash
    clickhouse-client
    ```
    如果成功，您将看到 ClickHouse 的命令行提示符。

**Docker 部署示例：**
```bash
docker run -d --name some-clickhouse-server --ulimit nofile=262144:262144 -p 8123:8123 -p 9000:9000 yandex/clickhouse-server
```
这将启动一个 ClickHouse 服务器实例，并将 HTTP 接口映射到主机的 8123 端口，TCP 接口映射到 9000 端口。

### 连接到 ClickHouse

您可以通过多种方式连接到 ClickHouse：

-   **`clickhouse-client`**：本地命令行客户端。
    ```bash
    clickhouse-client # 默认连接到 localhost:9000
    clickhouse-client --host <hostname> --port <port> --user <username> --password <password>
    ```

-   **HTTP/HTTPS 接口**：ClickHouse 默认在 `8123` 端口提供 HTTP 接口。您可以使用 `curl` 或任何 HTTP 客户端发送查询。
    ```bash
    curl 'http://localhost:8123/' --data-binary "SELECT 1"
    ```

-   **JDBC/ODBC 驱动程序**：用于 Java、Python 等编程语言的应用程序集成。官方和社区提供了多种驱动程序。

-   **第三方 GUI 工具**：如 DBeaver, Tabix, DataGrip 等，提供了图形化界面来管理和查询 ClickHouse。

## 数据库和表操作

### 创建数据库

使用 `CREATE DATABASE` 语句创建新的数据库：

```sql
CREATE DATABASE IF NOT EXISTS my_database;
```
`IF NOT EXISTS` 可以防止在数据库已存在时报错。

切换到数据库：
```sql
USE my_database;
```

### 创建表

创建表是 ClickHouse 中最核心的操作之一。选择合适的表引擎至关重要。`MergeTree` 家族是最常用的引擎，专为 OLAP 设计。

**MergeTree 表引擎基础：**

`MergeTree` 引擎的表需要一个主键（`PRIMARY KEY`）和一个排序键（`ORDER BY`，通常与主键相同或为其前缀）。数据按排序键排序存储，并按月进行分区（默认）。

```sql
CREATE TABLE IF NOT EXISTS my_database.my_table (
    event_date Date,
    user_id UInt32,
    event_type String,
    value Float64,
    timestamp DateTime
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(event_date) -- 按年月分区
ORDER BY (user_id, event_type, event_date) -- 按用户ID、事件类型、事件日期排序
PRIMARY KEY (user_id, event_type); -- 主键，用于稀疏索引
```

**常用 MergeTree 家族引擎：**

-   `MergeTree`: 基础引擎。
-   `ReplacingMergeTree`: 在合并数据时会删除具有相同排序键的重复行，只保留最后一条（或指定版本列的最新版本）。
-   `SummingMergeTree`: 在合并时对具有相同排序键的行进行汇总，对指定的数值列求和。
-   `AggregatingMergeTree`: 在合并时对具有相同排序键的行进行聚合，使用 `AggregateFunction` 类型存储中间状态。
-   `CollapsingMergeTree`: 需要一个额外的 `Sign` 列（1 表示状态，-1 表示取消状态），成对的行（相同排序键，一个 Sign=1，一个 Sign=-1）会在合并时被删除。用于记录对象状态变化。
-   `VersionedCollapsingMergeTree`: `CollapsingMergeTree` 的增强版，增加了版本列，处理乱序数据更可靠。
-   `ReplicatedMergeTree`: `MergeTree` 的复制版本，用于在集群中实现数据复制和高可用。例如 `ReplicatedReplacingMergeTree`。

**分布式表 (Distributed Tables)：**

`Distributed` 表引擎本身不存储数据，它充当一个视图，将查询路由到集群中的一个或多个分片上的本地表。写入 `Distributed` 表的数据也会被分发到各个分片。

```sql
CREATE TABLE IF NOT EXISTS my_database.my_distributed_table AS my_database.my_table
ENGINE = Distributed(cluster_name, my_database, my_table, rand());
-- cluster_name: 在服务器配置文件中定义的集群名称
-- my_database: 远程分片上的数据库名
-- my_table: 远程分片上的本地表名
-- rand(): 分片键，用于决定数据写入哪个分片，rand() 表示随机分发
```

### ClickHouse 数据类型

ClickHouse 支持丰富的数据类型：

-   **整数型**: `UInt8`, `UInt16`, `UInt32`, `UInt64`, `Int8`, `Int16`, `Int32`, `Int64`, `UInt128`, `Int128`, `UInt256`, `Int256`
-   **浮点型**: `Float32`, `Float64`
-   **Decimal 型**: `Decimal(P, S)`，高精度定点数
-   **布尔型**: `Bool` (底层使用 `UInt8`，取值为 0 或 1)
-   **字符串型**: `String` (任意长度), `FixedString(N)` (固定长度)
-   **日期时间型**: `Date`, `Date32`, `DateTime`, `DateTime64(S, [timezone])`
-   **枚举型**: `Enum8('value1' = 1, 'value2' = 2, ...)` , `Enum16`
-   **数组型**: `Array(T)`，例如 `Array(String)`
-   **元组型**: `Tuple(T1, T2, ...)`，例如 `Tuple(String, Int32)`
-   **Nullable(T)**: 允许存储 `NULL` 值，例如 `Nullable(Int32)`
-   **LowCardinality(T)**: 对低基数列（不同值较少）进行字典编码优化存储和查询，例如 `LowCardinality(String)`
-   **UUID**: 用于存储 UUID 值
-   **IPv4, IPv6**: 用于存储 IP 地址
-   **AggregateFunction(name, types_of_arguments...)**: 用于 `AggregatingMergeTree`，存储聚合函数的中间状态。
-   **Map(key_type, value_type)**: 键值对类型。

## 数据导入

将数据加载到 ClickHouse 有多种方法。

### INSERT INTO 语句

标准的 SQL `INSERT INTO` 语句可用于插入数据。对于大量数据，逐条插入效率低下，应使用批处理方式。

```sql
INSERT INTO my_database.my_table (event_date, user_id, event_type, value, timestamp)
VALUES ('2023-10-26', 1001, 'view', 1.0, '2023-10-26 10:00:00');

-- 批量插入
INSERT INTO my_database.my_table (event_date, user_id, event_type, value, timestamp)
VALUES
    ('2023-10-26', 1002, 'click', 0.0, '2023-10-26 10:01:00'),
    ('2023-10-26', 1003, 'purchase', 19.99, '2023-10-26 10:05:00');
```

### 从 SELECT 查询结果插入

可以将一个查询的结果直接插入到另一个表中：

```sql
INSERT INTO my_database.archive_table
SELECT * FROM my_database.my_table WHERE event_date < '2023-01-01';
```

### 支持的数据格式

ClickHouse 在通过 `INSERT` 语句、`clickhouse-client` 或 HTTP 接口导入数据时支持多种格式。常用的有：

-   **`Values`**: `INSERT INTO t VALUES (v1, v2), (v3, v4)` 这种形式。
-   **`TabSeparated` (TSV)**: Tab 分隔值，每行一条记录，字段间用制表符分隔。
-   **`CSV`**: 逗号分隔值。
-   **`JSONEachRow`**: 每行一个 JSON 对象。
    ```json
    {"event_date":"2023-10-26", "user_id":1004, "event_type":"login"}
    {"event_date":"2023-10-26", "user_id":1005, "event_type":"view"}
    ```
-   **`Parquet`**, **`ORC`**, **`Arrow`**: 高效的列式存储格式，常用于大数据生态。

**使用 `clickhouse-client` 导入文件示例：**

```bash
# 导入 TSV 文件
cat data.tsv | clickhouse-client --query="INSERT INTO my_database.my_table FORMAT TabSeparated"

# 导入 JSONEachRow 文件
cat data.jsonl | clickhouse-client --query="INSERT INTO my_database.my_table FORMAT JSONEachRow"
```

**使用 HTTP 接口导入文件示例：**

```bash
curl -X POST 'http://localhost:8123/?query=INSERT%20INTO%20my_database.my_table%20FORMAT%20CSV' --data-binary @data.csv
```

### 异步插入

对于需要高吞吐量写入的场景，可以启用异步插入 (`async_insert=1`)。数据首先写入内存缓冲区，然后批量刷写到磁盘。这可以提高写入性能，但如果服务器在数据刷写前崩溃，可能会有少量数据丢失的风险。

```sql
INSERT INTO my_database.my_table SETTINGS async_insert=1 VALUES (...);
```

## 查询数据

ClickHouse 使用 SQL 进行数据查询，并针对分析查询进行了大量优化。

### 基本 SELECT 查询

```sql
SELECT user_id, event_type, value
FROM my_database.my_table
WHERE event_date = '2023-10-26'
LIMIT 10;
```

### 过滤数据 (WHERE)

使用 `WHERE` 子句进行数据过滤。由于 ClickHouse 的列式存储和稀疏主键索引，针对主键列和分区键的过滤非常高效。

```sql
SELECT count()
FROM my_database.my_table
WHERE event_type = 'purchase' AND value > 100;
```

### 数据聚合 (GROUP BY)

`GROUP BY` 子句用于数据聚合，配合聚合函数使用，如 `count()`, `sum()`, `avg()`, `min()`, `max()`, `uniq()` (计算近似唯一值), `groupArray()` (将分组内的值聚合为数组) 等。

```sql
SELECT
    event_type,
    count() AS event_count,
    sum(value) AS total_value,
    avg(value) AS average_value
FROM my_database.my_table
WHERE event_date >= '2023-10-01' AND event_date <= '2023-10-31'
GROUP BY event_type
ORDER BY event_count DESC;
```

### 排序数据 (ORDER BY)

`ORDER BY` 子句用于对结果集进行排序。如果排序键与表的 `ORDER BY` 键一致，查询性能会更好。

```sql
SELECT user_id, timestamp, value
FROM my_database.my_table
WHERE event_type = 'click'
ORDER BY timestamp DESC
LIMIT 100;
```

### 连接表 (JOIN)

ClickHouse 支持多种 `JOIN` 类型，包括 `INNER JOIN`, `LEFT JOIN`, `RIGHT JOIN`, `FULL JOIN`, `CROSS JOIN`。对于大规模分布式 `JOIN`，ClickHouse 提供了 `GLOBAL JOIN` 优化。

```sql
CREATE TABLE my_database.user_profiles (
    user_id UInt32,
    name String,
    registration_date Date
)
ENGINE = MergeTree()
ORDER BY user_id;

INSERT INTO my_database.user_profiles VALUES (1001, 'Alice', '2023-01-15'), (1002, 'Bob', '2023-02-20');

SELECT
    t.user_id,
    p.name,
    t.event_type,
    t.value
FROM my_database.my_table AS t
LEFT JOIN my_database.user_profiles AS p ON t.user_id = p.user_id
WHERE t.event_date = '2023-10-26'
LIMIT 5;
```

**GLOBAL JOIN**:
当右表较小，且需要与左表（可能是分布式表）进行连接时，`GLOBAL ... JOIN` 会将右表的数据广播到所有参与查询的节点，每个节点上构建哈希表进行连接。这避免了跨节点的数据 shuffle。

```sql
SELECT ...
FROM distributed_table AS dt
GLOBAL LEFT JOIN small_local_table AS slt ON dt.key = slt.key;
```

### 常用函数

ClickHouse 提供了极其丰富的内置函数，涵盖数学、字符串、日期时间、数组、URL、JSON、条件、类型转换、聚合等。

-   **日期时间函数**: `toDate()`, `toDateTime()`, `toYYYYMM()`, `toStartOfDay()`, `now()`, `yesterday()`, `subtractDays()`
-   **字符串函数**: `length()`, `substring()`, `concat()`, `lower()`, `upper()`, `replaceOne()`, `splitByString()`, `like`, `notLike`, `match()` (regexp)
-   **聚合函数**: `count()`, `sum()`, `avg()`, `min()`, `max()`, `uniq()`, `uniqExact()`, `groupArray()`, `groupUniqArray()`, `topK()`
-   **条件函数**: `if(cond, then, else)`, `multiIf()`
-   **数组函数**: `array()`, `length(array)`, `empty(array)`, `has()`, `indexOf()`, `arrayMap()`, `arrayFilter()`, `arrayJoin()` (将数组展开为多行)
-   **URL 函数**: `protocol()`, `domain()`, `path()`
-   **JSON 函数**: `JSONExtractString()`, `JSONHas()`, `isValidJSON()`

### 子查询

支持在 `FROM`, `WHERE`, `IN` 子句中使用子查询。

```sql
SELECT user_id, count()
FROM my_database.my_table
WHERE user_id IN (SELECT user_id FROM my_database.user_profiles WHERE registration_date > '2023-06-01')
GROUP BY user_id;
```

### 窗口函数

ClickHouse 从较新版本开始支持 SQL 标准窗口函数，如 `ROW_NUMBER()`, `RANK()`, `LAG()`, `LEAD()`, 以及聚合函数的窗口版本（例如 `SUM(...) OVER (...)`）。

```sql
SELECT
    user_id,
    event_date,
    value,
    SUM(value) OVER (PARTITION BY user_id ORDER BY event_date) AS cumulative_value
FROM my_database.my_table
LIMIT 10;
```

## 数据管理

### 备份与恢复

ClickHouse 本身不提供像传统数据库那样完整的在线备份和时间点恢复 (PITR) 工具。备份通常通过以下几种方式进行：

1.  **`ALTER TABLE ... FREEZE PARTITION ...`**：
    该命令会为指定表或分区创建一个硬链接副本（快照）在 `shadow/` 目录下。然后可以将这些文件手动拷贝到备份存储。
    ```sql
    ALTER TABLE my_database.my_table FREEZE PARTITION '202310';
    -- 文件会出现在 /var/lib/clickhouse/shadow/N/... 目录下
    -- N 是一个递增的数字
    ```
    恢复时，将数据文件放回对应表的 `detached/` 目录，然后执行 `ALTER TABLE ... ATTACH PARTITION ...`。

2.  **`clickhouse-copier`**：
    一个官方工具，用于在 ClickHouse 集群之间复制数据。可以用于迁移数据或创建集群副本。

3.  **第三方备份工具**：
    -   `clickhouse-backup` (by Alexey Milovidov, ClickHouse 创始人之一，后独立发展)：一个流行的开源工具，支持向 S3、GCS 等云存储备份和恢复，支持增量备份。

4.  **文件系统级别快照**：
    如果使用支持快照的文件系统（如 ZFS）或云存储卷（如 EBS 快照），可以进行块级别的备份。

### 监控

监控 ClickHouse 的健康状况和性能至关重要。

-   **系统表**：ClickHouse 提供了大量的 `system.*` 表，用于查询服务器状态、查询历史、性能指标、副本状态等。
    -   `system.metrics`: 当前指标值（如活跃查询数、内存使用）。
    -   `system.events`: 事件计数器（如执行的 SELECT 查询数）。
    -   `system.query_log`: 记录已执行查询的详细信息（需启用）。
    -   `system.processes`: 当前正在执行的查询列表。
    -   `system.replicas`: `ReplicatedMergeTree` 表的副本状态。
    -   `system.parts`: 表的数据片段信息。

-   **HTTP 监控端点**：
    -   `/ping`: 检查服务器是否存活。
    -   `/metrics`: Prometheus 格式的指标暴露。

-   **日志文件**：
    默认路径：`/var/log/clickhouse-server/clickhouse-server.log` 和 `clickhouse-server.err.log`。

-   **集成 Prometheus 和 Grafana**：
    ClickHouse 可以通过 `system.metrics` 表或 HTTP `/metrics` 端点与 Prometheus 集成，使用 Grafana 创建监控仪表盘。

## 性能优化与最佳实践

### Schema 设计

-   **选择合适的表引擎**：`MergeTree` 家族是分析查询的首选。根据需求选择 `ReplacingMergeTree`, `SummingMergeTree` 等。
-   **合理定义排序键 (`ORDER BY`)**：将最常用于过滤和聚合的列放在排序键的前面。排序键决定了数据的物理存储顺序和主键稀疏索引的构建。
-   **分区 (`PARTITION BY`)**：通常按月或按天分区。过细或过粗的分区都可能影响性能。分区有助于管理数据生命周期（如删除旧数据）。
-   **主键 (`PRIMARY KEY`)**：ClickHouse 的主键是稀疏的，用于加速数据查找。通常设置为主排序键的前缀。
-   **数据类型选择**：使用最小且能满足需求的类型（如 `UInt32` 而非 `String` 存储用户 ID）。使用 `LowCardinality(String)` 优化低基数字符串列。
-   **避免 `Nullable`**：除非确实需要 `NULL` 值，否则尽量避免使用 `Nullable` 类型，因为它会带来一些性能开销。
-   **归一化 vs 反归一化**：ClickHouse 倾向于反归一化（宽表），以减少查询时的 `JOIN` 操作。但过度反归一化也可能导致数据冗余和更新复杂。

### 查询优化

-   **`SELECT` 特定列**：只选择查询所需的列，避免 `SELECT *`。
-   **利用 `WHERE` 子句过滤**：尽可能早地过滤数据，尤其是在分区键和排序键上的过滤。
-   **避免在 `WHERE` 子句的左侧对列使用函数**：这会使索引失效。例如，用 `date_col >= '2023-01-01'` 代替 `toYYYYMM(date_col) = 202301`（如果 `date_col` 可以直接比较）。
-   **使用 `PREWHERE`**：对于 `MergeTree` 表，`PREWHERE` 是一个更早的过滤阶段，它在读取完整数据列之前执行。如果过滤条件涉及的列与主查询选择的列不同，且过滤能显著减少数据量，`PREWHERE` 能提高效率。
-   **`JOIN` 优化**：
    -   小表在右侧，大表在左侧。
    -   使用 `GLOBAL JOIN` 优化分布式连接中小表广播。
    -   确保连接键类型一致。
-   **使用近似计算函数**：对于允许一定误差的场景（如统计独立访客数），使用 `uniq()` (近似) 而非 `uniqExact()` (精确) 可以大幅提升性能。
-   **批量操作**：对于数据插入和删除，始终使用批量操作。
-   **理解 `GROUP BY` 行为**：ClickHouse 的 `GROUP BY` 对内存使用敏感，如果基数过高，可能会消耗大量内存。考虑使用 `GROUP BY ... WITH TOTALS` 或分阶段聚合。
-   **使用 `MATERIALIZED VIEW`**：对于频繁执行的复杂聚合查询，可以创建物化视图来预计算结果。
-   **查询 `system.query_log`**：分析慢查询，找出性能瓶颈。

### 系统配置

-   调整 `config.xml` 和 `users.xml` 中的参数，如最大内存使用、并发查询数、网络设置等。
-   确保有足够的内存、快速的磁盘 I/O 和足够的 CPU 核心。

本指南提供了 ClickHouse 的基本用法和一些高级主题的入门。要深入了解特定功能，请参阅 ClickHouse 官方文档。 