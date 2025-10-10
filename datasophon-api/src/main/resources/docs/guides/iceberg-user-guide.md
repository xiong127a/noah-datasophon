# Apache Iceberg 用户指南

## 快速开始

Apache Iceberg是一种高性能表格式系统，为数据湖提供可靠的数据管理功能。本指南将帮助您在DataSophon平台上快速入门并高效使用Iceberg。

### 先决条件

在开始使用Iceberg之前，请确保您的环境中已安装并配置以下组件：

- Apache Spark 3.0+
- Apache Hive 3.0+（可选，用于Hive集成）
- Apache Flink 1.12+（可选，用于流处理）
- 存储系统：HDFS、Amazon S3或其他兼容存储

### 基本配置

在DataSophon中，Iceberg组件已预先配置好基本设置。主要配置文件位于：

```
/opt/datasophon/data/iceberg/conf/iceberg-site.xml
```

核心配置参数包括：

| 参数名 | 说明 | 默认值 |
| ----- | ---- | ----- |
| iceberg.catalog-impl | 目录实现类 | org.apache.iceberg.hive.HiveCatalog |
| iceberg.warehouse | 数据仓库位置 | hdfs:///user/hive/warehouse |
| iceberg.catalog.hive.uri | Hive元数据地址 | thrift://metastore-host:9083 |

## 创建与管理表

### 使用Spark SQL创建Iceberg表

在Spark中使用Iceberg非常简单。首先启动Spark，确保包含Iceberg依赖：

```bash
spark-shell --packages org.apache.iceberg:iceberg-spark-runtime-3.2_2.12:0.14.0
```

#### 创建表

```scala
// 设置Spark使用Iceberg
spark.sql("CREATE DATABASE IF NOT EXISTS iceberg_db")
spark.sql("USE iceberg_db")

// 创建Iceberg表
spark.sql("""
  CREATE TABLE customers (
    id bigint,
    name string,
    email string,
    registration_date date,
    active boolean
  ) USING iceberg
  PARTITIONED BY (registration_date)
  TBLPROPERTIES (
    'write.format.default' = 'parquet',
    'write.parquet.compression-codec' = 'snappy'
  )
""")
```

#### 写入数据

```scala
// 通过DataFrame API写入数据
val data = Seq(
  (1L, "张三", "zhangsan@example.com", java.sql.Date.valueOf("2023-01-15"), true),
  (2L, "李四", "lisi@example.com", java.sql.Date.valueOf("2023-01-16"), true),
  (3L, "王五", "wangwu@example.com", java.sql.Date.valueOf("2023-01-17"), false)
).toDF("id", "name", "email", "registration_date", "active")

data.writeTo("iceberg_db.customers").append()

// 通过SQL写入
spark.sql("""
  INSERT INTO iceberg_db.customers
  VALUES
    (4, '赵六', 'zhaoliu@example.com', '2023-01-18', true),
    (5, '钱七', 'qianqi@example.com', '2023-01-19', false)
""")
```

#### 查询数据

```scala
// 基本查询
spark.sql("SELECT * FROM iceberg_db.customers").show()

// 使用时间旅行查询特定版本
spark.sql("""
  SELECT * FROM iceberg_db.customers VERSION AS OF 1
""").show()

// 查询特定时间点的数据
spark.sql("""
  SELECT * FROM iceberg_db.customers TIMESTAMP AS OF '2023-02-01 00:00:00'
""").show()
```

#### 更新和删除

```scala
// 更新记录
spark.sql("""
  UPDATE iceberg_db.customers 
  SET active = false 
  WHERE id = 1
""")

// 删除记录
spark.sql("""
  DELETE FROM iceberg_db.customers 
  WHERE active = false
""")

// MERGE INTO操作
spark.sql("""
  MERGE INTO iceberg_db.customers t
  USING updates s
  ON t.id = s.id
  WHEN MATCHED THEN UPDATE SET t.active = s.active
  WHEN NOT MATCHED THEN INSERT (id, name, email, registration_date, active)
  VALUES (s.id, s.name, s.email, s.registration_date, s.active)
""")
```

### 使用Flink SQL操作Iceberg

Flink提供了对Iceberg的原生支持，适合流处理场景：

#### 设置Flink环境

```bash
flink-sql-client embedded \
  -j iceberg-flink-runtime-1.16-0.14.0.jar \
  -c org.apache.flink.table.catalog.hive.HiveCatalog \
  -s key1=value1
```

在SQL客户端中配置Iceberg目录：

```sql
CREATE CATALOG iceberg_catalog WITH (
  'type'='iceberg',
  'catalog-type'='hive',
  'uri'='thrift://metastore-host:9083',
  'warehouse'='hdfs:///user/hive/warehouse'
);

USE CATALOG iceberg_catalog;
```

#### 流式读取和写入

```sql
-- 创建流式表
CREATE TABLE customer_events (
  id BIGINT,
  name STRING,
  event_time TIMESTAMP(3),
  event_type STRING,
  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
  'connector' = 'kafka',
  'topic' = 'customer_events',
  'properties.bootstrap.servers' = 'kafka:9092',
  'properties.group.id' = 'iceberg-consumer',
  'format' = 'json',
  'scan.startup.mode' = 'latest-offset'
);

-- 写入Iceberg表
INSERT INTO iceberg_db.customers
SELECT id, name, email, CAST(event_time AS DATE) AS registration_date, true AS active
FROM customer_events
WHERE event_type = 'REGISTER';
```

### 使用Trino/Presto查询Iceberg

Trino提供出色的Iceberg支持，特别适合交互式查询：

```sql
-- 在Trino中查询Iceberg表
SELECT * FROM iceberg.iceberg_db.customers
WHERE registration_date > date '2023-01-15';

-- 使用时间旅行
SELECT * FROM iceberg.iceberg_db.customers FOR TIMESTAMP AS OF timestamp '2023-01-20 00:00:00';

-- 检查表历史
SELECT * FROM iceberg.iceberg_db."customers$history";
```

## 高级特性

### 模式演化

Iceberg支持无中断的模式演化，可以轻松添加、重命名或删除列：

```sql
-- 添加新列
ALTER TABLE iceberg_db.customers
ADD COLUMN loyalty_points INT;

-- 重命名列
ALTER TABLE iceberg_db.customers
RENAME COLUMN loyalty_points TO member_points;

-- 更改列类型（需要兼容）
ALTER TABLE iceberg_db.customers
ALTER COLUMN member_points TYPE BIGINT;
```

### 分区演化

Iceberg允许动态更改分区方案，无需重写数据：

```sql
-- 更改分区方案
ALTER TABLE iceberg_db.customers
REPLACE PARTITION FIELD registration_date
WITH months(registration_date);
```

### 表维护操作

#### 压缩小文件

```sql
-- 使用Spark SQL执行压缩操作
CALL iceberg_db.system.rewrite_data_files(table => 'iceberg_db.customers')
```

#### 设置和过期快照

```scala
// 在Spark中过期快照
import org.apache.iceberg.spark.actions.SparkActions

// 保留最近5个快照，并过期比30天更早的快照
SparkActions
  .get()
  .expireSnapshots()
  .table("iceberg_db.customers")
  .retainLast(5)
  .expireOlderThan(System.currentTimeMillis() - 30 * 86400 * 1000L)
  .execute()
```

#### 检查表历史和元数据

```scala
// 查看表历史
spark.sql("SELECT * FROM iceberg_db.customers.history").show()

// 查看快照
spark.sql("SELECT * FROM iceberg_db.customers.snapshots").show()

// 查看清单
spark.sql("SELECT * FROM iceberg_db.customers.manifests").show()

// 查看数据文件
spark.sql("SELECT * FROM iceberg_db.customers.files").show()
```

## 性能优化

### 数据分布与排序

优化Iceberg表性能的关键是合理的分区策略和排序：

```sql
-- 创建带有排序的表
CREATE TABLE orders (
  order_id bigint,
  customer_id bigint,
  order_date date,
  total_amount decimal(10,2),
  status string
) USING iceberg
PARTITIONED BY (days(order_date))
TBLPROPERTIES (
  'write.distribution-mode' = 'hash',
  'write.distribution.hash.field' = 'customer_id',
  'write.sort.mode' = 'full-sort',
  'write.sort.field' = 'customer_id, order_id'
)
```

### 使用Z-Order优化

Z-Order排序可以提高多维过滤的性能：

```scala
// 使用Z-Order重写数据
import org.apache.spark.sql.functions.lit

spark.sql("""
  CALL iceberg_db.system.rewrite_data_files(
    table => 'iceberg_db.customers', 
    strategy => 'sort', 
    sort_order => 'zorder(id, registration_date)'
  )
""")
```

### 优化读取性能

为提高查询性能，可以利用Iceberg的统计信息和向量化读取：

```scala
// 设置向量化读取
spark.conf.set("spark.sql.parquet.enableVectorizedReader", "true")
spark.conf.set("spark.sql.iceberg.vectorization.enabled", "true")

// 设置统计信息过滤
spark.conf.set("spark.sql.iceberg.filter-pushdown.enabled", "true")
```

## 集成与互操作性

### Hive集成

要在Hive中使用Iceberg表，需要配置HiveCatalog：

```xml
<property>
  <name>iceberg.catalog.hive.type</name>
  <value>hive</value>
</property>
<property>
  <name>iceberg.catalog.hive.uri</name>
  <value>thrift://metastore-host:9083</value>
</property>
```

然后可以在Hive中查询Iceberg表：

```sql
SELECT * FROM iceberg_db.customers;
```

### REST API集成

Iceberg提供REST目录服务，用于跨平台管理表：

```bash
# 启动REST服务
java -jar iceberg-rest-service.jar

# 配置客户端使用REST目录
spark.conf.set("spark.sql.catalog.rest", "org.apache.iceberg.spark.SparkCatalog")
spark.conf.set("spark.sql.catalog.rest.catalog-impl", "org.apache.iceberg.rest.RESTCatalog")
spark.conf.set("spark.sql.catalog.rest.uri", "http://localhost:8181")
```

## 故障排查与最佳实践

### 常见问题解决

1. **元数据锁定问题**
   - 症状：提交失败，报告锁冲突
   - 解决：检查并清理过期的锁
   ```scala
   import org.apache.iceberg.catalog.TableIdentifier
   import org.apache.iceberg.hive.HiveCatalog

   val catalog = new HiveCatalog()
   catalog.initialize("hive", Map("uri" -> "thrift://metastore-host:9083").asJava)
   val table = catalog.loadTable(TableIdentifier.of("iceberg_db", "customers"))
   table.refresh()
   ```

2. **性能下降**
   - 症状：查询变慢，尤其在表增长后
   - 解决：检查分区策略，运行压缩，设置合理的清理策略
   ```sql
   CALL iceberg_db.system.rewrite_data_files(table => 'iceberg_db.customers')
   ```

3. **版本兼容性问题**
   - 症状：使用新版Spark时出现格式错误
   - 解决：确保使用兼容的Iceberg和引擎版本，可能需要迁移表格式

### 最佳实践

1. **分区策略**
   - 选择适当的分区粒度，避免过度分区
   - 对于时间序列数据，考虑使用月或日级别分区
   - 使用Iceberg的转换分区避免分区裂变

2. **写入优化**
   - 对于批量写入，使用排序提高写入速度
   - 设置合理的文件大小目标 (128-512MB)
   - 使用分布式写入平衡工作负载

3. **读取优化**
   - 启用向量化读取和统计信息过滤
   - 对频繁查询的列使用Z-Order排序
   - 定期运行压缩和清理任务

4. **管理和监控**
   - 监控表的快照大小和增长
   - 设置自动清理策略
   - 备份关键元数据

## 实战案例

### 案例1：电子商务数据平台

本案例展示如何构建一个使用Iceberg的电子商务数据平台：

```scala
// 创建订单表
spark.sql("""
  CREATE TABLE e_commerce.orders (
    order_id BIGINT,
    user_id BIGINT,
    order_date TIMESTAMP,
    status STRING,
    total_amount DECIMAL(10,2)
  ) USING iceberg
  PARTITIONED BY (days(order_date))
  TBLPROPERTIES (
    'write.format.default' = 'parquet',
    'write.parquet.compression-codec' = 'snappy'
  )
""")

// 创建订单明细表
spark.sql("""
  CREATE TABLE e_commerce.order_items (
    item_id BIGINT,
    order_id BIGINT,
    product_id BIGINT,
    quantity INT,
    unit_price DECIMAL(10,2)
  ) USING iceberg
""")

// 增量ETL示例
import org.apache.spark.sql.functions._

// 读取上次处理后的新订单
val fromTimestamp = "2023-01-01T00:00:00"
spark.read
  .format("iceberg")
  .option("snapshot-id", 10)
  .load("e_commerce.orders")
  .filter(col("order_date") > lit(fromTimestamp))
  .createOrReplaceTempView("new_orders")

// 处理并写入聚合表
spark.sql("""
  INSERT INTO e_commerce.daily_sales
  SELECT 
    date_trunc('day', order_date) as sale_date,
    count(order_id) as order_count,
    sum(total_amount) as total_sales
  FROM new_orders
  WHERE status = 'COMPLETED'
  GROUP BY date_trunc('day', order_date)
""")
```

### 案例2：流批一体处理

本案例展示如何构建一个集成Kafka、Flink和Iceberg的实时数据管道：

```java
// Flink Java代码示例
// 创建Flink表环境
TableEnvironment tEnv = TableEnvironment.create(EnvironmentSettings.newInstance().build());

// 注册Iceberg目录
tEnv.executeSql(
    "CREATE CATALOG iceberg_catalog WITH ("
    + "'type'='iceberg',"
    + "'catalog-type'='hive',"
    + "'uri'='thrift://metastore-host:9083',"
    + "'warehouse'='hdfs:///user/hive/warehouse'"
    + ")");

// 注册Kafka表
tEnv.executeSql(
    "CREATE TABLE kafka_events ("
    + "  user_id BIGINT,"
    + "  event_type STRING,"
    + "  product_id BIGINT,"
    + "  event_time TIMESTAMP(3),"
    + "  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECONDS"
    + ") WITH ("
    + "  'connector' = 'kafka',"
    + "  'topic' = 'user_events',"
    + "  'properties.bootstrap.servers' = 'kafka:9092',"
    + "  'properties.group.id' = 'flink-iceberg-demo',"
    + "  'format' = 'json'"
    + ")");

// 写入Iceberg表
tEnv.executeSql(
    "INSERT INTO iceberg_catalog.analytics.user_product_events "
    + "SELECT "
    + "  user_id, product_id, event_type, "
    + "  DATE_FORMAT(event_time, 'yyyy-MM-dd') AS event_date, "
    + "  event_time "
    + "FROM kafka_events");

// 查询聚合后写入另一个Iceberg表
tEnv.executeSql(
    "INSERT INTO iceberg_catalog.analytics.product_popularity "
    + "SELECT "
    + "  product_id, "
    + "  COUNT(DISTINCT user_id) AS unique_users, "
    + "  COUNT(*) AS total_events, "
    + "  DATE_FORMAT(event_time, 'yyyy-MM-dd') AS event_date "
    + "FROM kafka_events "
    + "GROUP BY product_id, DATE_FORMAT(event_time, 'yyyy-MM-dd')");
```

## 参考资料

- [Apache Iceberg官方文档](https://iceberg.apache.org/)
- [Iceberg Spark集成指南](https://iceberg.apache.org/spark-quickstart/)
- [Iceberg Flink集成指南](https://iceberg.apache.org/flink/)
- [Iceberg表规范](https://iceberg.apache.org/spec/)

---

通过本指南，您已经掌握了在DataSophon平台上使用Apache Iceberg的基础知识和高级技术。Iceberg作为现代数据湖的表格式，能够帮助您构建可靠、高性能的数据系统。随着对数据需求的增长，您可以进一步利用Iceberg提供的强大功能，如ACID事务、模式演化和时间旅行，以满足复杂的业务场景和分析需求。 