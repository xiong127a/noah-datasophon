# Presto 用户指南

本指南将详细介绍如何在大数据平台中使用 Presto，包括如何连接、查询和优化，以及在开发过程中的最佳实践。

## 准备工作

在开始使用 Presto 之前，您需要了解以下基本信息：

- Presto 服务的访问地址和端口
- 您的数据库目录（Catalog）名称
- 访问权限和凭证信息
- 可以使用的客户端工具

## 连接 Presto

Presto 提供多种连接方式，您可以根据实际需求选择合适的连接方法。

### 使用 Presto CLI

Presto CLI 是官方提供的命令行工具，支持交互式查询和批处理执行。

#### 安装 Presto CLI

1. 下载 Presto CLI 可执行文件：

```bash
wget https://repo1.maven.org/maven2/com/facebook/presto/presto-cli/0.278/presto-cli-0.278-executable.jar -O presto-cli
```

2. 为文件添加执行权限：

```bash
chmod +x presto-cli
```

#### 使用 CLI 连接 Presto

基本连接命令格式如下：

```bash
./presto-cli --server SERVER_ADDRESS:PORT --catalog CATALOG_NAME --schema SCHEMA_NAME
```

例如：

```bash
./presto-cli --server presto.example.com:8080 --catalog hive --schema default
```

#### CLI 常用参数

| 参数 | 描述 | 示例 |
|------|------|------|
| `--server` | Presto 服务器地址和端口 | `--server presto.example.com:8080` |
| `--catalog` | 要使用的数据目录 | `--catalog hive` |
| `--schema` | 要使用的数据库模式 | `--schema default` |
| `--user` | 连接用户名 | `--user admin` |
| `--password` | 使用密码认证 | `--password` |
| `--file` | 执行 SQL 文件 | `--file query.sql` |
| `--output-format` | 输出格式 | `--output-format CSV` |

### 使用 JDBC 驱动

对于 Java 应用程序，Presto 提供了 JDBC 驱动程序，允许您通过标准 JDBC 接口连接 Presto。

#### 添加 JDBC 依赖

在 Maven 项目中，添加以下依赖：

```xml
<dependency>
    <groupId>com.facebook.presto</groupId>
    <artifactId>presto-jdbc</artifactId>
    <version>0.278</version>
</dependency>
```

或在 Gradle 项目中：

```gradle
compile 'com.facebook.presto:presto-jdbc:0.278'
```

#### JDBC 连接示例

```java
import java.sql.*;

public class PrestoJdbcExample {
    public static void main(String[] args) {
        // JDBC URL 格式: jdbc:presto://host:port/catalog/schema
        String url = "jdbc:presto://presto.example.com:8080/hive/default";
        
        try (Connection connection = DriverManager.getConnection(url, "user", null)) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT * FROM orders LIMIT 10")) {
                    while (rs.next()) {
                        System.out.println(rs.getLong("orderkey") + " " + rs.getString("orderstatus"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

#### JDBC URL 格式详解

JDBC URL 遵循以下格式：

```
jdbc:presto://host:port/catalog/schema
```

其中：
- `host:port`: Presto 服务器地址和端口
- `catalog`: 数据目录名称
- `schema`: 数据库模式名称

#### JDBC 连接参数

可以通过 JDBC URL 参数或连接属性设置以下选项：

| 参数 | 描述 | 默认值 |
|------|------|--------|
| `user` | 连接用户名 | 当前操作系统用户 |
| `password` | 连接密码 | 无 |
| `SSL` | 是否使用 SSL 连接 | false |
| `SSLVerification` | SSL 验证模式 | FULL |
| `applicationNamePrefix` | 应用名前缀 | 无 |
| `socksProxy` | SOCKS 代理地址 | 无 |

### 使用 Python 连接

Python 开发者可以使用 `presto-python-client` 库连接 Presto。

#### 安装 Python 客户端

```bash
pip install presto-python-client
```

#### Python 连接示例

```python
import prestodb

# 创建连接
conn = prestodb.dbapi.connect(
    host='presto.example.com',
    port=8080,
    user='user',
    catalog='hive',
    schema='default',
)

# 创建游标
cur = conn.cursor()

# 执行查询
cur.execute('SELECT * FROM orders LIMIT 10')

# 获取结果
rows = cur.fetchall()
for row in rows:
    print(row)
```

## 编写 Presto SQL 查询

Presto 支持标准 ANSI SQL 以及一些扩展特性，下面介绍常用的查询操作。

### 基本查询

#### 简单查询

```sql
SELECT * FROM hive.sales.orders LIMIT 10;
```

#### 聚合查询

```sql
SELECT customer_id, 
       COUNT(*) as order_count, 
       SUM(order_total) as total_spent
FROM hive.sales.orders
GROUP BY customer_id
ORDER BY total_spent DESC
LIMIT 20;
```

#### 联合查询

```sql
SELECT o.order_id, o.order_date, c.customer_name, p.product_name
FROM hive.sales.orders o
JOIN mysql.customers.customer_data c ON o.customer_id = c.id
JOIN hive.products.items p ON o.product_id = p.id
WHERE o.order_date >= DATE '2023-01-01'
ORDER BY o.order_date DESC;
```

### 高级查询功能

#### 窗口函数

```sql
SELECT 
    order_id,
    order_date,
    order_amount,
    SUM(order_amount) OVER (PARTITION BY customer_id ORDER BY order_date) as customer_running_total
FROM 
    orders;
```

#### 复杂数据类型

Presto 支持复杂数据类型如 ARRAY、MAP 和 ROW：

```sql
-- 数组操作
SELECT 
    user_id,
    interests,
    interests[1] as primary_interest,
    array_contains(interests, 'sports') as likes_sports
FROM 
    user_profiles;

-- MAP 操作
SELECT 
    product_id,
    properties,
    properties['color'] as product_color,
    map_keys(properties) as available_properties
FROM 
    products;
```

#### JSON 处理

```sql
-- 从 JSON 字段提取数据
SELECT 
    id,
    JSON_EXTRACT(properties, '$.dimensions.width') as width,
    JSON_EXTRACT(properties, '$.dimensions.height') as height
FROM 
    products;
```

#### 正则表达式

```sql
-- 使用正则表达式匹配
SELECT 
    user_id, 
    email
FROM 
    users
WHERE 
    regexp_like(email, '.*@example\.com');
```

## 跨数据源查询

Presto 的一大优势是支持跨多种数据源的查询。

### 跨源联合查询

您可以在一个查询中联合不同数据源的表：

```sql
-- 关联 Hive 数据和 MySQL 数据
SELECT 
    o.order_id,
    o.order_date,
    c.customer_name,
    o.total_amount
FROM 
    hive.sales.orders o
JOIN 
    mysql.crm.customers c
ON 
    o.customer_id = c.id
WHERE 
    o.order_date > date '2023-01-01';
```

### 使用跨源视图

创建跨源视图简化复杂查询：

```sql
-- 创建跨源视图
CREATE VIEW hive.analytics.order_details AS
SELECT 
    o.order_id,
    o.order_date,
    c.customer_name,
    c.customer_email,
    p.product_name,
    o.quantity,
    o.unit_price,
    o.quantity * o.unit_price as total_price
FROM 
    hive.sales.orders o
JOIN 
    mysql.crm.customers c ON o.customer_id = c.id
JOIN 
    postgresql.inventory.products p ON o.product_id = p.id;

-- 使用视图
SELECT * FROM hive.analytics.order_details
WHERE order_date BETWEEN date '2023-01-01' AND date '2023-01-31'
ORDER BY total_price DESC;
```

## 性能优化

使用 Presto 时，以下性能优化技巧可以帮助您获得更好的查询性能。

### 查询优化

#### 过滤条件下推

尽早应用过滤条件，减少处理的数据量：

```sql
-- 优化前
SELECT customer_id, SUM(order_total) 
FROM orders 
WHERE order_date > date '2023-01-01'
GROUP BY customer_id;

-- 优化后（确保过滤条件在索引列上）
SELECT customer_id, SUM(order_total) 
FROM orders 
WHERE order_date > date '2023-01-01'
  AND order_status = 'COMPLETED'  -- 添加更多过滤条件
GROUP BY customer_id;
```

#### 限制查询字段

只选择需要的列，避免 SELECT *：

```sql
-- 优化前
SELECT * FROM large_table;

-- 优化后
SELECT id, name, status FROM large_table;
```

#### 使用近似函数

对于大数据集的聚合，考虑使用近似函数：

```sql
-- 精确计数（较慢）
SELECT COUNT(DISTINCT user_id) FROM events;

-- 近似计数（较快）
SELECT approx_distinct(user_id) FROM events;
```

### 表格式优化

#### 使用列式存储格式

对于 Hive 表，使用 ORC 或 Parquet 格式获得更好的性能：

```sql
-- 创建 ORC 格式表
CREATE TABLE orders_orc
WITH (format = 'ORC')
AS SELECT * FROM orders;
```

#### 合理分区

根据查询模式合理分区表数据：

```sql
-- 按日期分区的表
CREATE TABLE events (
    event_id BIGINT,
    event_type VARCHAR,
    event_data VARCHAR
)
WITH (
    partitioned_by = ARRAY['event_date'],
    format = 'ORC'
);
```

#### 使用压缩

启用数据压缩减少 I/O：

```sql
-- 设置会话压缩选项
SET SESSION hive.compression_codec = 'SNAPPY';
```

### 会话参数优化

可以通过设置会话参数提升特定查询性能：

```sql
-- 增加内存限制
SET SESSION memory_limit = '8GB';

-- 调整查询优化器级别
SET SESSION optimizer.optimize_hash_generation = true;

-- 启用动态过滤
SET SESSION enable_dynamic_filtering = true;
```

## 监控和故障排除

### 查询监控

#### 查看活跃查询

查看当前正在运行的查询：

```sql
SELECT 
    query_id, 
    user, 
    state, 
    queued_time_ms,
    elapsed_time_ms,
    query_type,
    query_text
FROM 
    system.runtime.queries
WHERE 
    state != 'FINISHED';
```

#### 查看查询历史

查看已完成的查询历史：

```sql
SELECT 
    query_id, 
    user, 
    state, 
    queued_time_ms,
    elapsed_time_ms,
    total_cpu_time_ms,
    total_scheduled_time_ms,
    query_text
FROM 
    system.runtime.completed_queries
ORDER BY 
    end_time DESC
LIMIT 100;
```

### 常见问题排查

#### 内存不足错误

如果遇到 "Query exceeded per-node memory limit" 错误：

1. 检查查询是否可以进一步优化
2. 考虑调整会话内存限制：`SET SESSION memory_limit = '10GB';`
3. 减少查询并行度：`SET SESSION query_max_stage_count = 10;`

#### 超时错误

处理 "Query exceeded maximum time limit" 错误：

1. 优化查询以提高效率
2. 考虑增加查询超时限制：`SET SESSION query_max_execution_time = '30m';`

#### 连接问题

如果无法连接 Presto 服务：

1. 验证服务器地址和端口是否正确
2. 检查网络连接和防火墙设置
3. 确认用户名和权限设置

## 系统集成

### 与 BI 工具集成

Presto 可以与多种 BI 工具集成，例如：

#### Tableau 集成

1. 安装 Presto ODBC 驱动
2. 在 Tableau 中添加新的 Presto 数据源连接
3. 配置连接参数（服务器、端口、目录、模式）
4. 测试连接并开始创建可视化

#### PowerBI 集成

1. 使用 ODBC 驱动连接 Presto
2. 在 PowerBI 中选择"获取数据" > "ODBC"
3. 选择已配置的 Presto DSN
4. 设置凭据并导入数据

### 与大数据平台集成

#### 与 Apache Airflow 集成

在 Airflow 中使用 Presto 执行查询：

```python
from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from datetime import datetime, timedelta
import prestodb

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'start_date': datetime(2023, 1, 1),
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

def run_presto_query():
    conn = prestodb.dbapi.connect(
        host='presto.example.com',
        port=8080,
        user='airflow',
        catalog='hive',
        schema='default',
    )
    cur = conn.cursor()
    cur.execute('SELECT COUNT(*) FROM orders')
    result = cur.fetchone()[0]
    print(f"Total orders: {result}")
    return result

dag = DAG(
    'presto_query_dag',
    default_args=default_args,
    schedule_interval=timedelta(days=1),
)

query_task = PythonOperator(
    task_id='run_presto_query',
    python_callable=run_presto_query,
    dag=dag,
)
```

#### 与 Apache Spark 集成

在 Spark 中读取 Presto 查询结果：

```scala
import org.apache.spark.sql.SparkSession

val spark = SparkSession.builder()
  .appName("Presto-Spark Integration")
  .getOrCreate()

// 使用 JDBC 连接 Presto
val prestoDF = spark.read
  .format("jdbc")
  .option("driver", "com.facebook.presto.jdbc.PrestoDriver")
  .option("url", "jdbc:presto://presto.example.com:8080/hive/default")
  .option("dbtable", "orders")
  .option("user", "spark")
  .load()

// 处理数据
prestoDF.createOrReplaceTempView("orders")
val resultDF = spark.sql("SELECT order_date, COUNT(*) as order_count FROM orders GROUP BY order_date")
resultDF.show()
```

## 安全与权限管理

### 用户认证

Presto 支持多种认证机制，包括：

#### 基于密码的认证

设置基于密码的认证需要在服务器配置中启用：

```properties
# config.properties
http-server.authentication.type=PASSWORD
```

客户端连接示例：

```bash
./presto-cli --server presto.example.com:8080 --user admin --password
```

#### LDAP 认证

集成 LDAP 认证：

```properties
# config.properties
http-server.authentication.type=LDAP
authentication.ldap.url=ldaps://ldap.example.com
authentication.ldap.user-bind-pattern=uid=${USER},ou=org,dc=example,dc=com
```

### 权限控制

Presto 可以通过配置实现细粒度的访问控制。

#### 文件访问控制

```properties
# access-control.properties
access-control.name=file
security.config-file=/etc/presto/rules.json
```

规则文件示例：

```json
{
  "catalogs": [
    {
      "user": "admin",
      "catalog": "hive",
      "allow": true
    },
    {
      "group": "analysts",
      "catalog": "hive",
      "schema": "sales",
      "allow": true
    }
  ]
}
```

#### SQL 标准访问控制

```properties
# access-control.properties
access-control.name=sql-standard
security.refresh-period=1m
```

创建角色和授权：

```sql
-- 创建角色
CREATE ROLE analyst;

-- 授予权限
GRANT SELECT ON hive.sales.orders TO ROLE analyst;
GRANT ROLE analyst TO USER bob;
```

## 高级主题

### 用户定义函数

Presto 允许开发自定义函数（UDF）以扩展功能。

#### 开发自定义函数

1. 创建 Maven 项目，添加依赖：

```xml
<dependency>
    <groupId>com.facebook.presto</groupId>
    <artifactId>presto-spi</artifactId>
    <version>${presto.version}</version>
    <scope>provided</scope>
</dependency>
```

2. 创建函数实现：

```java
package com.example.udf;

import com.facebook.presto.spi.function.Description;
import com.facebook.presto.spi.function.ScalarFunction;
import com.facebook.presto.spi.function.SqlType;
import com.facebook.presto.spi.type.StandardTypes;

public class ExampleUDF {
    @ScalarFunction("example_udf")
    @Description("Example user defined function")
    @SqlType(StandardTypes.VARCHAR)
    public static String exampleUdf(
            @SqlType(StandardTypes.VARCHAR) String input) {
        return "Example UDF: " + input;
    }
}
```

3. 创建扩展类：

```java
package com.example.udf;

import com.facebook.presto.spi.Plugin;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class ExamplePlugin implements Plugin {
    @Override
    public Set<Class<?>> getFunctions() {
        return ImmutableSet.<Class<?>>builder()
                .add(ExampleUDF.class)
                .build();
    }
}
```

4. 打包并部署到 Presto 插件目录。

### 使用外部函数

连接到外部系统功能：

```sql
-- 使用外部函数示例
SELECT hive.default.geocode(customer_address) AS coordinates
FROM customers;
```

## 最佳实践

### 查询编写

1. **始终指定列**：避免使用 `SELECT *`，只选择必要的列
2. **使用合适的数据类型**：选择最适合数据的类型，避免不必要的转换
3. **合理使用过滤条件**：尽早过滤数据，减少处理量
4. **限制结果集**：使用 LIMIT 限制大型结果集的大小
5. **使用参数化查询**：在应用程序中使用预处理语句和参数绑定

### 数据建模

1. **选择合适的表格式**：使用列式存储格式如 ORC 或 Parquet
2. **合理分区**：根据查询模式选择适当的分区策略
3. **定期维护统计信息**：确保优化器使用最新的统计信息
4. **使用物化视图**：对于频繁的复杂查询，考虑预计算结果

### 资源管理

1. **合理设置会话参数**：根据查询特点调整内存和并行度
2. **使用资源组**：对不同类型的查询分配不同的资源限制
3. **避免长时间运行的查询**：拆分大型查询，或使用增量处理
4. **监控系统资源**：持续监控内存、CPU 使用情况，及时调整

## 总结

Presto 是一个强大的分布式 SQL 查询引擎，适用于大数据环境下的交互式分析。通过本指南的内容，您应该能够：

- 连接 Presto 并编写高效的查询
- 执行跨数据源的复杂分析
- 优化查询性能和资源使用
- 将 Presto 集成到您的数据分析工作流中

随着对 Presto 的深入了解和使用，您将能够充分发挥其强大的数据分析能力，为您的组织提供快速、灵活的数据洞察。 