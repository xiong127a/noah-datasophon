# Apache Hudi 用户指南

本指南旨在帮助用户理解如何在类似 DataSophon 这样的集群管理平台环境下，结合 Apache Spark 和 Apache Flink 等计算引擎使用 Apache Hudi 构建和管理数据湖。内容将覆盖环境准备、数据写入、数据查询、表管理、性能调优和基本安全考量。

## 环境准备与部署

Apache Hudi 通常不作为一个独立的服务部署，而是作为库集成到 Apache Spark 或 Apache Flink 作业中。DataSophon 平台可以帮助管理所需的计算引擎 (Spark, Flink) 和底层存储 (HDFS)。

### 依赖
*   **计算引擎**: 需要一个已部署并可用的 Apache Spark 或 Apache Flink 集群。
*   **存储系统**: Hudi 表数据通常存储在 HDFS 或对象存储 (如 AWS S3, MinIO) 上。确保计算引擎可以访问这些存储系统。
*   **Hudi Bundles**: 你需要在你的 Spark/Flink 作业中引入 Hudi 对应版本的 bundles。这些 bundles 通常包含了 Hudi 核心库以及与其集成的计算引擎所需的依赖。
    *   **Spark**: `hudi-spark<spark_version>-bundle_2.1x.jar` (例如 `hudi-spark3.3-bundle_2.12.jar`)
    *   **Flink**: `hudi-flink<flink_version>-bundle_2.1x.jar` (例如 `hudi-flink1.17-bundle_2.12.jar`)
    DataSophon 在配置 Spark/Flink 服务时，可能允许配置全局的 Hudi 依赖，或者你需要在提交作业时指定这些 JAR 包。

### 获取 Hudi (Maven/SBT 示例)
如果手动构建 Spark/Flink 作业，可以通过 Maven 或 SBT 添加 Hudi 依赖：

**Maven (pom.xml):**
```xml
<dependency>
    <groupId>org.apache.hudi</groupId>
    <artifactId>hudi-spark3.3-bundle_2.12</artifactId> <!-- 根据 Spark 版本调整 -->
    <version>${hudi.version}</version>
</dependency>
```

**SBT (build.sbt):**
```sbt
libraryDependencies += "org.apache.hudi" % "hudi-spark3.3-bundle" % "${hudi.version}" // 根据 Spark 版本调整
```
请将 `${hudi.version}` 替换为你希望使用的 Hudi 版本 (例如 `0.14.0`)。

### 基本配置
Hudi 的配置项非常多，可以通过 Spark/Flink 作业的参数或配置文件传入。主要分为写配置 (`HoodieWriteConfig`) 和读配置 (`HoodieReadOptions`)。

一些通用的写配置前缀是 `hoodie.`, `hoodie.datasource.write.`, `hoodie.write.`, `hoodie.upsert.` 等。
一些通用的读配置前缀是 `hoodie.datasource.query.`。

建议查阅 Hudi 官方文档以获取特定版本和场景下的详细配置项。

## 写入数据到 Hudi 表 (Write Operations)

Hudi 提供了多种方式将数据写入到表中，最常见的是通过 Spark DataSource API 和 Flink SQL Connector。

### 使用 Spark DataSource API

确保 Spark 作业的 classpath 中包含了 `hudi-spark-bundle.jar`。

```scala
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.SaveMode
import org.apache.hudi.DataSourceWriteOptions
import org.apache.hudi.config.HoodieWriteConfig
import org.apache.hudi.keygen.constant.KeyGeneratorOptions

val spark = SparkSession.builder().appName("Hudi Spark Writer").config("spark.serializer", "org.apache.spark.serializer.KryoSerializer").getOrCreate()

// 假设有一个输入 DataFrame: inputDF
// val inputDF = ...

val tableName = "my_hudi_table"
val basePath = "/user/hudi/my_hudi_table" // HDFS 或 S3 路径

inputDF.write.format("hudi")
  // 写操作配置
  .option(DataSourceWriteOptions.TABLE_TYPE_OPT_KEY, "COPY_ON_WRITE") // 或 "MERGE_ON_READ"
  .option(DataSourceWriteOptions.OPERATION_OPT_KEY, "upsert") // "upsert", "insert", "bulk_insert", "delete"
  .option(DataSourceWriteOptions.RECORDKEY_FIELD_OPT_KEY, "uuid") // 记录的唯一键字段名
  .option(DataSourceWriteOptions.PRECOMBINE_FIELD_OPT_KEY, "updated_at") // 数据预合并字段，通常是时间戳，用于处理同一批次中的重复记录键
  .option(DataSourceWriteOptions.PARTITIONPATH_FIELD_OPT_KEY, "event_date") // 分区字段名
  .option(KeyGeneratorOptions.PARTITIONPATH_URL_ENCODE_OPT_KEY, "true") // 分区路径是否 URL 编码
  .option(HoodieWriteConfig.TBL_NAME_OPT_KEY, tableName)
  .option("hoodie.datasource.write.keygenerator.class", "org.apache.hudi.keygen.SimpleKeyGenerator") // 或 ComplexKeyGenerator, TimestampBasedKeyGenerator
  // .option(DataSourceWriteOptions.PAYLOAD_CLASS_OPT_KEY, "org.apache.hudi.common.model.OverwriteWithLatestAvroPayload") // 默认 payload

  // Hive 同步配置 (可选)
  .option(DataSourceWriteOptions.HIVE_STYLE_PARTITIONING_OPT_KEY, "true")
  .option(DataSourceWriteOptions.HIVE_SYNC_ENABLED_OPT_KEY, "true")
  .option(DataSourceWriteOptions.HIVE_DATABASE_OPT_KEY, "default")
  .option(DataSourceWriteOptions.HIVE_TABLE_OPT_KEY, tableName)
  .option(DataSourceWriteOptions.HIVE_PARTITION_FIELDS_OPT_KEY, "event_date")
  .option(DataSourceWriteOptions.HIVE_PARTITION_EXTRACTOR_CLASS_OPT_KEY, "org.apache.hudi.hive.MultiPartKeysValueExtractor")
  // .option(DataSourceWriteOptions.HIVE_USER_OPT_KEY, "hive")
  // .option(DataSourceWriteOptions.HIVE_PASS_OPT_KEY, "hive")
  // .option(DataSourceWriteOptions.HIVE_URL_OPT_KEY, "jdbc:hive2://localhost:10000")

  // MOR 表的 Compaction 配置 (如果 TABLE_TYPE_OPT_KEY = "MERGE_ON_READ")
  // .option("hoodie.compact.inline", "false") // 是否内联执行 compaction，默认为 false (异步)
  // .option("hoodie.compaction.schedule.inline", "true") // 是否内联生成 compaction 计划
  // .option("hoodie.compaction.strategy", "org.apache.hudi.table.action.compact.strategy.LogFileSizeBasedCompactionStrategy") // Compaction 策略
  // .option("hoodie.compaction.trigger.strategy", "NUM_COMMITS") // 触发策略，如 NUM_COMMITS, TIME_ELAPSED
  // .option("hoodie.compaction.delta_commits", "5") // 多少个 delta commits 后触发 compaction

  .mode(SaveMode.Append) // 对于 Hudi 表，通常使用 Append 模式
  .save(basePath)
```

**关键选项说明**: 
*   `TABLE_TYPE_OPT_KEY`: `COPY_ON_WRITE` 或 `MERGE_ON_READ`。
*   `OPERATION_OPT_KEY`: `upsert` (默认), `insert`, `bulk_insert`, `delete`。
*   `RECORDKEY_FIELD_OPT_KEY`: 数据集中作为记录唯一标识的字段。
*   `PRECOMBINE_FIELD_OPT_KEY`: 当同一批写入数据中存在相同记录键的多条记录时，Hudi 会根据此字段的值选择保留哪条记录（通常选择值最大的那条，例如最新的时间戳）。
*   `PARTITIONPATH_FIELD_OPT_KEY`: 用于对表进行分区的字段。Hudi 会根据此字段的值将数据写入到不同的分区目录中。
*   `TBL_NAME_OPT_KEY`: Hudi 表的名称，用于元数据存储和 Hive 同步。
*   `hoodie.datasource.write.keygenerator.class`: 键生成器类，用于从记录中提取记录键和分区路径。`SimpleKeyGenerator` (单字段作键), `ComplexKeyGenerator` (多字段组合键), `TimestampBasedKeyGenerator` (基于时间戳生成分区路径)。
*   `PAYLOAD_CLASS_OPT_KEY`: 定义记录合并逻辑的类。默认是 `OverwriteWithLatestAvroPayload` (新数据覆盖旧数据)。`EmptyHoodieRecordPayload` 用于实现硬删除。
*   **Hive 同步相关选项**: 如果希望 Hudi 表能被 Hive 查询，需要启用 Hive 同步并配置相关参数。

### 使用 Flink SQL Connector

确保 Flink 作业的 classpath 中包含了 `hudi-flink<flink_version>-bundle.jar`。

**DDL 示例 (创建 Hudi 表):**
```sql
CREATE TABLE my_flink_hudi_table (
  uuid STRING PRIMARY KEY NOT ENFORCED, -- 记录键
  name STRING,
  amount DECIMAL(10, 2),
  event_date STRING, -- 分区字段
  updated_at TIMESTAMP(3), -- precombine 字段
  WATERMARK FOR updated_at AS updated_at - INTERVAL '5' SECOND -- 可选，用于流式处理
)
PARTITIONED BY (event_date) -- 定义分区
WITH (
  'connector' = 'hudi',
  'path' = 'hdfs:///user/hudi/my_flink_hudi_table', -- HDFS 或 S3 路径
  'table.type' = 'MERGE_ON_READ', -- 'COPY_ON_WRITE' 或 'MERGE_ON_READ'
  'hoodie.datasource.write.recordkey.field' = 'uuid', -- 如果 PKEY 已定义则可省略
  'hoodie.datasource.write.precombine.field' = 'updated_at', -- precombine 字段
  -- 'write.operation' = 'upsert', -- 'insert', 'upsert', 'bulk_insert', 'delete'
  'compaction.async.enabled' = 'true', -- MOR表: 是否异步 compaction
  'compaction.tasks' = '2', -- MOR表: compaction 并行度
  'compaction.trigger.strategy' = 'num_commits', -- MOR表: compaction 触发策略 (num_commits, time_elapsed)
  'compaction.delta_commits' = '5', -- MOR表: 5个 delta commits 后触发 compaction
  'read.streaming.enabled' = 'true', -- 是否启用流式读取
  'read.streaming.check-interval' = '60', -- 流式读取检查新数据的间隔 (秒)
  'hive_sync.enabled' = 'true',          -- 是否启用 Hive 同步
  'hive_sync.mode' = 'hms',              -- Hive 同步模式 (hms, jdbc, glue)
  'hive_sync.metastore.uris' = 'thrift://your-hive-metastore:9083',
  'hive_sync.db' = 'default',
  'hive_sync.table' = 'my_flink_hudi_table_hive',
  'hive_sync.partition_fields' = 'event_date',
  'hive_sync.partition_extractor_class' = 'org.apache.hudi.hive.MultiPartKeysValueExtractor'
);
```

**DML 示例 (写入数据):**
```sql
-- 批处理写入
INSERT INTO my_flink_hudi_table
SELECT uuid, name, amount, DATE_FORMAT(updated_at, 'yyyy-MM-dd') as event_date, updated_at
FROM source_table;

-- 流式写入 (假设 source_stream_table 是一个流式源)
INSERT INTO my_flink_hudi_table /*+ OPTIONS('write.operation'='upsert') */
SELECT uuid, name, amount, DATE_FORMAT(updated_at, 'yyyy-MM-dd') as event_date, updated_at
FROM source_stream_table;
```

**关键 `WITH` 选项**: 许多 Spark DataSource API 中的选项在 Flink SQL 中有对应的 `WITH` 子句配置项，前缀通常是 `hoodie.datasource.write.` 或直接是相关功能名 (如 `compaction.*`, `hive_sync.*`)。

### 使用 HoodieDeltaStreamer 工具

`HoodieDeltaStreamer` 是 Hudi 提供的一个独立的命令行工具 (通常与 Spark 一起运行)，用于从各种数据源 (如 Kafka, DFS 上的 JSON/Avro 文件, JDBC 源) 持续或批量地将数据摄取到 Hudi 表中。

**用途**:
*   构建从上游系统到数据湖的增量数据管道。
*   支持 Schema 演进 (通过 Schema Provider)。
*   支持自定义转换逻辑 (Transformer)。

**命令行示例 (从 Kafka 摄取 Avro 数据):**
```bash
spark-submit --class org.apache.hudi.utilities.deltastreamer.HoodieDeltaStreamer \
hudi-utilities-bundle_2.12-0.14.0.jar \
  --props /path/to/kafka-source.properties \
  --schemaprovider-class org.apache.hudi.utilities.schema.SchemaRegistryProvider \
  --source-class org.apache.hudi.utilities.sources.AvroKafkaSource \
  --source-ordering-field <timestamp_field_in_kafka_message> \
  --target-base-path hdfs:///user/hudi/target_hudi_table \
  --target-table target_hudi_table_name \
  --op UPSERT \
  --continuous # 可选，用于连续模式运行
```

**`kafka-source.properties` 文件示例:**
```properties
include=base.properties # 可以包含通用配置

# Kafka Source
hoodie.deltastreamer.source.kafka.topic=my_kafka_topic
bootstrap.servers=kafka_broker1:9092,kafka_broker2:9092
auto.offset.reset=earliest
group.id=hudi_deltastreamer_group

# Schema Registry
schema.registry.url=http://schema_registry_host:8081

# Hoodie Table Configs (DeltaStreamer 会传递给 HoodieWriteClient)
hoodie.datasource.write.recordkey.field=id
hoodie.datasource.write.partitionpath.field=event_date_partition
hoodie.datasource.write.precombine.field=updated_ts
hoodie.table.name=target_hudi_table_name
hoodie.datasource.hive_sync.enable=true
# ... 其他 Hive Sync 和 Hudi 写配置
```
`HoodieDeltaStreamer` 非常灵活，支持多种源和自定义逻辑，是构建数据湖摄取层的重要工具。

### Schema 演进
Hudi 支持 Schema 演进。当写入的数据包含新的列或列类型发生变化时，Hudi 可以处理这些变更。
*   通常依赖于 Avro Schema。Hudi 表的 Schema（以及数据的 Schema）通常以 Avro 格式定义和存储。
*   与 Hive Sync 结合使用时，Hudi 会尝试将 Avro Schema 转换为 Hive Metastore 中的表结构。确保 Hive Metastore 也支持或能够兼容 Schema 演进（例如，对于某些类型的变更）。
*   配置如 `hoodie.datasource.write.schema.on.read` (Flink) 可以控制 Schema 推断行为。

## 查询 Hudi 表 (Query Operations)

Hudi 表创建后，可以通过多种计算引擎进行查询。

### 使用 Spark SQL

```scala
val spark = SparkSession.builder().appName("Hudi Spark Reader").getOrCreate()

val hudiTablePath = "/user/hudi/my_hudi_table"

// 1. 快照查询 (Snapshot Query) - 查询最新提交的数据
val snapshotDF = spark.read.format("hudi").load(hudiTablePath)
snapshotDF.show()
// 或者，如果已同步到 Hive Metastore
// spark.sql("SELECT * FROM default.my_hudi_table_hive LIMIT 10").show()

// 2. 读优化查询 (Read Optimized Query) - 仅 MOR 表，查询最新的已压缩数据
// 对于 COW 表，此查询与快照查询相同
val readOptimizedDF = spark.read.format("hudi")
  .option(DataSourceReadOptions.QUERY_TYPE_OPT_KEY, DataSourceReadOptions.QUERY_TYPE_READ_OPTIMIZED_OPT_KEY)
  .load(hudiTablePath)
readOptimizedDF.show()

// 3. 增量查询 (Incremental Query) - 查询自某个时间点以来的变更数据
val incrementalDF = spark.read.format("hudi")
  .option(DataSourceReadOptions.QUERY_TYPE_OPT_KEY, DataSourceReadOptions.QUERY_TYPE_INCREMENTAL_OPT_KEY)
  .option(DataSourceReadOptions.BEGIN_INSTANTTIME_OPT_KEY, "<commit_timestamp_or_earliest>") // 例如 "20230101100000" 或 "earliest"
  // .option(DataSourceReadOptions.END_INSTANTTIME_OPT_KEY, "<commit_timestamp>") // 可选，指定结束时间
  .load(hudiTablePath)

// 增量查询会包含一些 Hudi 元数据列，如 _hoodie_commit_time, _hoodie_record_key 等
incrementalDF.printSchema()
incrementalDF.show()

// 4. 时间旅行查询 (Time Travel Query) - 查询特定时间点的快照
// 需要该时间点的数据未被 Cleaning 清理
val timeTravelDF = spark.read.format("hudi")
  .option(DataSourceReadOptions.TIME_TRAVEL_AS_OF_INSTANT_OPT_KEY, "<commit_timestamp_of_past_snapshot>") // 例如 "20230101000000"
  .load(hudiTablePath)
timeTravelDF.show()
```

### 使用 Flink SQL

如果已通过 DDL 创建了 Hudi 表 (如上文示例)，可以直接使用 Flink SQL 查询：

```sql
-- 快照查询 (默认行为对于批模式，或流模式下的 lookup join)
SELECT * FROM my_flink_hudi_table WHERE event_date = '2023-10-26';

-- 流式读取 (如果 'read.streaming.enabled' = 'true')
-- Flink 作业会持续消费 Hudi 表中的新数据和更新
SELECT uuid, name, amount, updated_at
FROM my_flink_hudi_table
/*+ OPTIONS('read.streaming.enabled'='true', 'read.start-commit'='earliest') */;

-- Flink 也支持特定版本的 Hudi 表进行时间旅行查询，通常通过 'read.end-commit' 等选项在批模式下实现，
-- 或者在创建表时定义特定的版本快照读取。
```
Flink 对 Hudi 的支持在不断发展，具体查询类型和选项请参考对应 Flink 和 Hudi 版本的官方文档。

### 使用 Hive (通过 Hive Sync)

在 Hudi 写作业中启用了 Hive Sync (`hoodie.datasource.hive_sync.enable=true`) 后，Hudi 表的元数据会被同步到 Hive Metastore。之后，你可以使用 Hive 客户端 (如 Beeline) 或任何连接到 Hive Metastore 的工具 (如 Hue, Superset) 来查询 Hudi 表。

```sql
-- 连接到 Hive (Beeline)
-- !connect jdbc:hive2://your-hiveserver2:10000

USE default;

-- 查询 COW 表 (通常只有一个视图或表，例如 my_hudi_table_hive)
SELECT * FROM my_hudi_table_hive WHERE event_date = '2023-10-26' LIMIT 10;

-- 查询 MOR 表 (通常会同步两个视图/表)
-- 1. 读优化视图 (Read Optimized View - `<table_name>_ro`):
--    查询最新的已压缩数据，性能较好，数据可能有延迟。
SELECT * FROM my_hudi_table_hive_ro WHERE event_date = '2023-10-26' LIMIT 10;

-- 2. 实时视图 (Real-time View - `<table_name>_rt`):
--    查询最新的数据 (合并基础文件和日志文件)，数据新鲜度高，查询延迟可能较高。
SELECT * FROM my_hudi_table_hive_rt WHERE event_date = '2023-10-26' LIMIT 10;
```
注意：视图名称 (`_ro`, `_rt`) 的约定可能会随 Hudi 版本变化。较新版本倾向于简化，可能只暴露一个表，其行为由查询引擎和 Hudi InputFormat 决定。

### 使用 Presto/Trino
Presto 和 Trino 都有 Hudi Connector，允许直接查询存储在 HDFS 或对象存储上的 Hudi 表。
1.  **配置 Connector**: 在 Presto/Trino 的 catalog 目录下创建一个 Hudi catalog 配置文件 (例如 `hudi.properties`)。
    ```properties
    connector.name=hudi
    hive.metastore.uri=thrift://your-hive-metastore:9083 # Hudi Connector 通常依赖 Hive Metastore 获取元数据
    # hive.s3.aws-access-key=xxx (如果数据在S3且需要认证)
    # hive.s3.aws-secret-key=xxx
    ```
2.  **查询 Hudi 表**:
    ```sql
    SELECT * FROM hudi.default.my_hudi_table_hive WHERE event_date = '2023-10-26' LIMIT 10;
    ```
    Presto/Trino 的 Hudi Connector 支持查询 COW 表和 MOR 表 (通常默认查询快照视图，可能提供配置项选择读优化视图)。具体支持的查询类型和特性请参考相应 Presto/Trino 版本的 Hudi Connector 文档。

## 表管理与维护 (Table Services)

Hudi 提供了一系列后台服务来自动或手动维护表的健康和性能。

### Compaction (压缩) - 仅 MOR 表
*   **目的**: 将 MOR 表中累积的日志文件合并到基础 Parquet/ORC 文件中，生成新的基础文件版本。这能提高读优化查询的性能，并控制日志文件的数量。
*   **配置 (Spark DataSource 示例)**:
    *   `hoodie.compact.inline=false` (默认): 异步执行 Compaction。写入作业完成后，Compaction 会独立运行。
    *   `hoodie.compaction.schedule.inline=true` (默认): 写入作业完成后，立即调度 Compaction 计划。
    *   `hoodie.compaction.strategy`: Compaction 计划选择策略，例如 `org.apache.hudi.table.action.compact.strategy.LogFileSizeBasedCompactionStrategy` (基于日志文件大小)。
    *   `hoodie.compaction.trigger.strategy`: 触发 Compaction 的策略，例如 `NUM_COMMITS` (N 个 delta commits 后触发), `TIME_ELAPSED` (固定时间间隔后触发)。
    *   `hoodie.compaction.delta_commits=5`: 如果触发策略是 `NUM_COMMITS`，表示每 5 个 delta commit 触发一次 Compaction。
    *   `hoodie.compaction.tasks`: Compaction 使用的 Spark 并行任务数。
*   **配置 (Flink SQL 示例)**:
    *   `compaction.async.enabled = 'true'`
    *   `compaction.schedule.enabled = 'true'` (Flink 通常在 checkpoint 完成后检查是否需要调度compaction)
    *   `compaction.trigger.strategy = 'num_commits'`
    *   `compaction.delta_commits = '5'`
    *   `compaction.tasks = '2'`
*   **手动触发**: 可以通过 Hudi CLI 或专门的 Spark/Flink 作业 (如 `HoodieCompactor` 工具类) 来手动触发 Compaction。

### Cleaning (清理)
*   **目的**: 根据保留策略删除 Hudi 表中旧的文件版本 (文件切片)，以回收存储空间。
*   **配置 (Spark DataSource 示例)**:
    *   `hoodie.clean.automatic=true` (默认): 自动清理。
    *   `hoodie.clean.async=true` (默认): 异步执行清理。
    *   `hoodie.cleaner.policy=KEEP_LATEST_COMMITS` (默认): 清理策略。其他选项如 `KEEP_LATEST_FILE_VERSIONS`。
    *   `hoodie.cleaner.commits.retained=10` (默认): 如果策略是 `KEEP_LATEST_COMMITS`，保留最近 10 个提交相关的文件版本。
    *   `hoodie.cleaner.hours.retained=24` (按小时保留) 等。
*   **配置 (Flink SQL 示例)**:
    *   `clean.async.enabled = 'true'`
    *   `clean.retain_commits = '10'`
    *   `clean.policy = 'KEEP_LATEST_COMMITS'`
*   **手动触发**: 可以通过 Hudi CLI 或 Spark/Flink 作业 (如 `HoodieCleaner` 工具类) 手动触发。

### Clustering (聚类)
*   **目的**: 通过重写数据文件来优化数据在存储上的物理布局，例如按特定列排序或合并小文件。这可以提高查询性能，特别是范围查询和需要特定顺序的查询。
*   **配置 (Spark DataSource 示例)**:
    *   `hoodie.clustering.inline=false` (默认): 异步执行聚类。
    *   `hoodie.clustering.schedule.inline=true` (默认): 内联调度聚类计划。
    *   `hoodie.clustering.plan.strategy.class`: 定义聚类计划生成的策略，例如 `org.apache.hudi.client.clustering.plan.strategy.SparkSizeBasedClusteringPlanStrategy` (基于文件大小)。
    *   `hoodie.clustering.execution.strategy.class`: 定义聚类执行策略，例如 `org.apache.hudi.client.clustering.run.strategy.SparkSortAndSizeExecutionStrategy` (排序并调整文件大小)。
    *   `hoodie.clustering.plan.strategy.sort.columns`: 指定用于排序的列。
*   **配置 (Flink SQL 示例)**:
    *   `clustering.async.enabled = 'true'`
    *   `clustering.schedule.enabled = 'true'`
    *   `clustering.plan.strategy.class = 'org.apache.hudi.table.action.cluster.strategy.FlinkSizeBasedClusteringPlanStrategy'`
    *   `clustering.plan.strategy.sort.columns = 'col_a,col_b'`
*   **触发方式**: 内联 (写入时同步执行，不推荐)、调度 (写入后异步调度)，或手动通过 Hudi CLI / Spark/Flink 作业 (`HoodieClusteringJob`) 触发。

### Archiving (归档)
*   **目的**: 将时间轴上较旧的、已完成的 Instant 元数据从主时间轴 (`.hoodie/*.commit`, `.hoodie/*.deltacommit` 等) 移动到归档文件 (`.hoodie/archived/`) 中。这有助于保持主时间轴元数据文件数量可控，提高元数据读取性能。
*   **配置 (Spark DataSource 示例)**:
    *   `hoodie.archive.automatic=true` (默认): 自动归档。
    *   `hoodie.archive.min.commits=20` (默认): 至少保留 20 个未归档的 commit 文件。
    *   `hoodie.archive.max.commits=30` (默认): 最多保留 30 个未归档的 commit 文件。
*   **配置 (Flink SQL 示例)**:
    *   `archive.automatic = 'true'`
    *   `archive.min_commits = '20'`
    *   `archive.max_commits = '30'`

### Hudi CLI 工具

Hudi 提供了一个命令行工具 (`hudi-cli`)，可以用于检查表状态、时间轴信息、修复某些元数据问题等。
*   **启动 CLI**: `hudi-cli` 或通过 `spark-submit` 运行 `org.apache.hudi.cli.HoodieCLI`。
*   **连接到 Hudi 表**: `connect --path hdfs:///user/hudi/my_hudi_table`
*   **常用命令**:
    *   `show instants` / `show commits`
    *   `show archived timeline`
    *   `show partitions`
    *   `show files --partition <partition_path>`
    *   `stats filesizes --partition <partition_path>`
    *   `repair addpartitionmeta --path <table_path>` (修复分区元数据)

### Savepoint 与 Restore
*   **Savepoint**: 一种特殊的 Commit，标记了某个时间点的文件版本是"安全的"，不会被 Cleaning 服务删除。用于灾难恢复或回滚到特定状态。
    *   **创建 (Spark)**: `hoodie.datasource.write.savepoint.before.write=true` (写入前自动创建)，或通过 Hudi CLI 手动创建。
*   **Restore**: 将表恢复到之前创建的某个 Savepoint 状态。此操作会创建一个新的 `RESTORE` Instant。
    *   通过 Hudi CLI (`savepoint restore --savepoint <commit_time>`) 或相关 API 执行。

### Bootstrap
*   用于将一个现有的、非 Hudi 格式的 Parquet 或 ORC 数据集（例如一个普通的 Hive 表）"引导"成为一个 Hudi 表。
*   它会生成 Hudi 元数据，而无需重写原始数据文件。之后，新数据可以通过 Hudi 写入，旧数据也可以被查询。
*   通过 `HoodieBootstrapClient` (Spark) 或相关工具执行。

## 性能调优

优化 Hudi 表的性能涉及多个方面，从写入配置到表服务，再到查询引擎的设置。

*   **文件大小**: 合理配置基础文件和日志文件的目标大小，避免过多小文件。
    *   Spark: `hoodie.parquet.max.file.size` (默认 120MB), `hoodie.logfile.max.size` (默认 1GB)。
    *   Flink: `write.parquet.max.file.size`, `write.log.max.size`。
*   **索引选择与调优**: 根据场景选择合适的索引类型。
    *   Bloom Index: 调整 `hoodie.bloom.index.num.entries`, `hoodie.bloom.index.fpp`。
    *   调整索引并行度 `hoodie.index.bloom.parallelism`。
*   **写操作并行度**: 调整 Spark/Flink 作业的并行度，以匹配数据量和集群资源。
    *   `hoodie.insert.shuffle.parallelism`, `hoodie.upsert.shuffle.parallelism`。
*   **Payload 类选择**: 如果有复杂的合并逻辑，自定义 Payload 可能影响性能。
*   **Compaction/Clustering 调优**:
    *   合理配置触发频率、执行并行度和策略，避免对写操作或集群资源造成过大压力。
    *   监控这些服务的执行时间和资源消耗。
*   **Schema 设计**: 避免过宽的表，合理使用分区。
*   **查询引擎优化**: 针对 Spark, Flink, Hive, Presto/Trino 等查询引擎，利用它们各自的优化特性 (如谓词下推、列裁剪、Join 优化等)。
*   **并发控制**: `hoodie.write.concurrency.mode` (optimistic_concurrency_control), `hoodie.cleaner.policy.failed.writes` 等冲突处理配置。
*   **Metrics 监控**: 监控 Hudi 提供的 Metrics (通过 JMX, Prometheus 等)，识别瓶颈。

## 安全配置

Hudi 本身不直接提供复杂的认证和授权机制，它依赖于底层计算引擎和存储系统的安全特性。

*   **与 Kerberos 集成**:
    *   如果 Hadoop 集群 (HDFS, YARN) 和 Hive Metastore 启用了 Kerberos，Hudi 作业 (Spark/Flink) 需要正确配置 Kerberos principal 和 keytab 才能访问这些安全服务。
    *   Spark: 通过 `--principal` 和 `--keytab` 提交作业，或在代码中配置。
    *   Flink: 配置 `security.kerberos.login.principal`, `security.kerberos.login.keytab` 等。
*   **HDFS ACLs / S3 Bucket Policies**: 确保运行 Hudi 作业的用户/角色具有读写 Hudi 表数据和 `.hoodie` 元数据目录的权限。
*   **Hive Metastore 安全**: 如果启用了 Hive Sync，Hudi 作业需要有权限连接到 Hive Metastore 并修改其元数据。如果 Metastore 启用了认证 (如 Kerberos) 或授权 (如 Ranger, Sentry)，需要相应配置。
*   **加密**: Hudi 支持 HDFS 透明加密。对于 S3 等对象存储，可以使用服务端加密 (SSE-S3, SSE-KMS)。

## 故障排查

*   **查看日志**: 这是最主要的排查手段。
    *   **Spark 作业**: Driver 日志, Executor 日志 (通过 YARN UI 或 Spark History Server 查看)。
    *   **Flink 作业**: JobManager 日志, TaskManager 日志 (通过 Flink Web UI 或日志文件查看)。
    *   **Hudi 时间轴**: `.hoodie` 目录下的 Instant 文件可以提供操作的详细元数据和状态。
*   **Hudi CLI**: 使用 `hudi-cli` 检查表状态、文件列表、时间轴等。
*   **常见问题**:
    *   **作业失败**: OOM (内存不足，调整 Spark/Flink 内存配置、并行度), 索引查找超时, 写入冲突 (OCC 失败), Compaction/Cleaning 失败。
    *   **性能低下**: 小文件过多, 索引选择不当, Compaction 滞后, 数据倾斜, 查询未优化。
    *   **数据不一致或丢失**: 检查 Cleaning 策略是否过于激进，Savepoint 是否正确创建和使用，源数据是否有问题。
    *   **Hive Sync 问题**: Metastore 连接失败, Schema 冲突, 权限不足。
*   **社区资源**: Apache Hudi 官方文档、邮件列表、Slack 频道、GitHub Issues 是获取帮助和查找解决方案的好地方。

本指南提供了一个在 DataSophon 类平台下使用 Apache Hudi 的概览。具体操作和配置可能因 Hudi、Spark、Flink 的版本以及平台特性而有所不同。强烈建议参考相应版本的官方文档以获取最准确和详细的信息。 