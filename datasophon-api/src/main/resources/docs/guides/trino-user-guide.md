# Trino (Formerly PrestoSQL) 用户指南

本指南旨在帮助用户理解如何在 DataSophon 平台部署、配置、管理和使用 Trino 服务。

## 1. 服务部署

Trino 是一个分布式 SQL 查询引擎，通常包含一个 Coordinator 节点和多个 Worker 节点。

通过 DataSophon 的服务管理界面：
1.  选择 "添加服务"。
2.  从服务列表中选择 "Trino"。
3.  根据集群规划，选择 Trino Coordinator 和 Trino Worker 角色需要部署的节点。
    *   **Trino Coordinator**: 通常部署1个节点。如果需要高可用，可以配置多个，但只有一个是 Active状态 (需要外部负载均衡配合)。DataSophon 部署时通常配置单个 Coordinator。
    *   **Trino Worker**: 根据查询负载和数据处理需求选择合适的节点数量。
4.  DataSophon 会自动处理依赖关系并推荐配置。用户可以根据实际需求调整配置参数。
5.  确认配置后，点击 "部署"。DataSophon 将自动完成 Trino 服务的安装、配置和启动。

## 2. 服务配置

Trino 的配置主要通过多个属性文件进行管理，这些文件通常位于 Trino 安装目录下的 `etc` 文件夹中。DataSophon 会将这些配置项抽取到其配置管理界面。

### Trino 核心配置文件

#### 1. Coordinator 和 Worker 通用配置 (`etc/config.properties`)
此文件包含 Trino Coordinator 和 Worker 节点的通用配置。
*   **`coordinator`**: (boolean) 如果为 `true`，则该节点作为 Coordinator 运行。如果为 `false`，则作为 Worker 运行。只能有一个活动的 Coordinator。
*   **`node-scheduler.include-coordinator`**: (boolean) 是否允许在 Coordinator 节点上调度 Task。对于大型集群，建议设置为 `false`，让 Coordinator 专职协调。
*   **`http-server.http.port`**: Trino HTTP 服务器监听的端口，用于客户端连接和 Web UI。
    *   默认值: `8080` (Trino 默认，DataSophon 可能会根据端口规划调整)。
*   **`query.max-memory`**: 单个查询在整个集群中可以使用的最大总内存 (例如 `50GB`)。超过此限制查询会失败。
*   **`query.max-memory-per-node`**: 单个查询在单个 Worker 节点上可以使用的最大用户内存 (例如 `1GB`)。
*   **`discovery.uri`**: 服务发现 URI。Coordinator 在此地址注册，Worker 在此地址发现 Coordinator。通常是 Coordinator 的 HTTP 地址 (例如 `http://<coordinator_host>:<http_port>`)。

#### 2. JVM 配置 (`etc/jvm.config`)
此文件用于配置 Trino 进程 (Coordinator 和 Worker) 的 JVM 参数。
*   `-server`: 使用服务器模式 JVM。
*   `-Xmx<size>`: 最大堆大小 (例如 `-Xmx16G`)。根据节点内存和查询负载合理配置。
*   `-XX:+UseG1GC`: 推荐使用 G1 垃圾收集器。
*   `-XX:G1HeapRegionSize=<size>`: G1 区域大小 (例如 `32M`)。
*   `-XX:+ExplicitGCInvokesConcurrent`: 允许并发执行 `System.gc()`。
*   `-XX:+HeapDumpOnOutOfMemoryError`: OOM 时生成堆转储文件。
*   `-XX:HeapDumpPath=<path>`: 堆转储文件路径。

#### 3. 节点属性 (`etc/node.properties`)
此文件包含特定于每个节点的环境配置。
*   **`node.environment`**: 节点所属的环境名称 (例如 `production`, `staging`)。
*   **`node.id`**: 节点的唯一标识符。通常是一个 UUID，由 Trino 自动生成或手动配置。
*   **`node.data-dir`**: 节点存储日志和其他数据 (如 Spooling) 的目录。

#### 4. 连接器配置 (`etc/catalog/<catalog_name>.properties`)
每个数据源连接器都在 `etc/catalog` 目录下有一个独立的属性文件。文件名即为目录名 (Catalog Name)。
例如，配置一个 Hive 连接器，目录名为 `hive`，则配置文件为 `etc/catalog/hive.properties`。

**Hive 连接器 (`hive.properties`) 示例:**
```properties
connector.name=hive
hive.metastore.uri=thrift://<hive_metastore_host>:<port>
# hive.config.resources=/path/to/core-site.xml,/path/to/hdfs-site.xml (如果需要访问 HDFS)
hive.allow-drop-table=true 
# hive.s3.aws-access-key=YOUR_ACCESS_KEY (如果使用S3且需要认证)
# hive.s3.aws-secret-key=YOUR_SECRET_KEY
```
*   **`connector.name`**: 指定连接器的类型 (例如 `hive`, `mysql`, `postgresql`, `kafka` 等)。这个名称必须与 Trino 插件中的连接器名称匹配。
*   后续属性特定于每个连接器。例如，Hive 连接器需要 `hive.metastore.uri`。

DataSophon 会提供常用连接器 (如 Hive, MySQL, PostgreSQL, Kafka) 的配置模板，并允许用户添加自定义连接器配置。

### 通过 DataSophon 修改配置
1.  进入 "服务管理" -> "Trino" -> "配置"。
2.  DataSophon 会将上述不同配置文件的参数分类展示。
3.  修改需要的参数。对于连接器配置，通常需要先选择或创建一个目录 (Catalog)，然后配置该目录的属性。
4.  修改完成后，点击 "保存配置"。
5.  根据提示，可能需要重启 Trino 服务 (Coordinator 和/或 Workers) 使配置生效。

## 3. 服务管理与监控

### 服务启停
通过 DataSophon 的服务管理界面，可以方便地启动、停止 Trino Coordinator 和 Trino Worker 节点。

### Trino Web UI
Trino Coordinator 提供了一个 Web UI，用于监控集群状态、查询执行情况和性能指标。
*   **访问地址**: `http://<coordinator_host>:<http_port>` (例如 `http://trino-coordinator:8080`)
*   **UI 功能**:
    *   **集群概览**: 显示活动的 Worker 数量、排队查询数、正在运行查询数、已完成查询数、失败查询数等。
    *   **查询列表**: 可以查看当前运行、已完成、失败的查询。点击查询 ID 可以查看详细信息。
    *   **查询详情**: 显示查询的 SQL 语句、状态、执行时间、处理的数据量、CPU 时间、生成的 Stage 和 Task、查询计划 (文本和 JSON 格式)、Live Plan (实时执行图) 等。
    *   **Worker 状态**: 列出所有 Worker 节点及其状态、地址、内存使用情况等。
    *   **性能图表**: 提供一些集群级别的性能图表。

DataSophon 通常会在 Trino 服务页面提供 Trino Web UI 的快捷链接。

### 监控指标
DataSophon 会集成 Trino 的关键监控指标，并在仪表盘中展示。这些指标通常通过 JMX 或 Trino 的 HTTP API 获取。
关键指标包括：
*   **Coordinator 指标**:
    *   活动/失败 Worker 数量。
    *   查询队列长度、运行中查询数。
    *   查询成功率、失败率。
    *   查询执行时间 (平均、P90、P99)。
*   **Worker 指标**:
    *   每个 Worker 的 CPU 使用率、内存使用率。
    *   输入数据速率、输出数据速率。
    *   活动 Task 数量。
*   **JVM 指标**: Coordinator 和 Worker 的堆内存使用、GC 次数和时间。
*   **连接器指标**: 特定连接器的操作计数、错误率、数据读取/写入量等。

## 4. Trino CLI 使用

Trino 提供了一个可执行 JAR 文件作为命令行界面 (CLI)，用于连接到 Trino 集群并执行 SQL 查询。

### 下载和运行 CLI
1.  从 Trino 官方网站或 Maven Central 下载与 Trino 服务器版本兼容的 CLI JAR 文件 (例如 `trino-cli-<version>-executable.jar`)。
2.  重命名为 `trino` 并赋予执行权限：
    ```bash
    mv trino-cli-*-executable.jar trino
    chmod +x trino
    ```

### 连接到集群并执行查询
```bash
./trino --server <coordinator_host>:<http_port> --catalog <catalog_name> --schema <schema_name>

# 示例:
./trino --server trino-coord:8080 --catalog hive --schema default
```
参数说明:
*   `--server`: Trino Coordinator 的地址和端口。
*   `--catalog`: (可选) 默认连接的目录名。
*   `--schema`: (可选) 默认连接的 Schema 名。
*   `--user`: (可选) 连接用户名 (如果 Trino 配置了认证)。
*   `--execute <sql_query>`: (可选) 直接执行 SQL 查询并退出。
*   `--file <file_path>`: (可选) 执行指定文件中的 SQL 查询并退出。

连接成功后，会进入交互式 SQL Shell：
```
trino> SELECT * FROM nation LIMIT 10;
trino:default> SHOW TABLES;
trino:default> USE my_catalog.my_schema;
trino:my_schema> SELECT count(*) FROM orders;
```

### 常用 CLI 命令
*   `quit` 或 `exit`: 退出 CLI。
*   `clear`: 清屏。
*   `history`: 显示命令历史。
*   `USE <catalog_name>.<schema_name>` 或 `USE <schema_name>` (如果在当前 catalog 下): 切换默认的目录和模式。
*   `SHOW CATALOGS;`
*   `SHOW SCHEMAS [FROM <catalog_name>];`
*   `SHOW TABLES [FROM <catalog_name>.<schema_name>];`
*   `DESCRIBE <table_name>;` 或 `DESC <table_name>;`

## 5. 连接器配置示例 (简要)

DataSophon 在部署 Trino 时，通常会引导用户配置一些常用的连接器。这里列举几个关键点。

### Hive Connector (`etc/catalog/hive.properties`)
用于查询存储在 HDFS 或兼容对象存储 (如 S3) 上的 Hive 表。
```properties
connector.name=hive
hive.metastore.uri=thrift://<hive_metastore_host>:<hive_metastore_port>
# 如果Hive Metastore启用了Kerberos
# hive.metastore.authentication.type=KERBEROS
# hive.metastore.kerberos.principal=hive/_HOST@YOUR_REALM.COM
# hive.metastore.client.keytab=/path/to/trino.keytab
# hive.metastore.service.principal=hive/_HOST@YOUR_REALM.COM

# 如果HDFS启用了Kerberos
# hive.hdfs.authentication.type=KERBEROS
# hive.hdfs.trino.principal=trino/_HOST@YOUR_REALM.COM
# hive.hdfs.trino.keytab=/path/to/trino.keytab
```

### MySQL Connector (`etc/catalog/mysql.properties`)
```properties
connector.name=mysql
connection-url=jdbc:mysql://<mysql_host>:<port>
connection-user=<user>
connection-password=<password>
```

### PostgreSQL Connector (`etc/catalog/postgresql.properties`)
```properties
connector.name=postgresql
connection-url=jdbc:postgresql://<pg_host>:<port>/<database_name>
connection-user=<user>
connection-password=<password>
```

### Kafka Connector (`etc/catalog/kafka.properties`)
用于查询 Kafka Topic 中的消息 (通常是 JSON, Avro, CSV 格式)。
```properties
connector.name=kafka
kafka.nodes=<kafka_broker_host1>:<port1>,<kafka_broker_host2>:<port2>
kafka.table-names=topic1,topic2.schema_name.table_name_for_topic2
kafka.hide-internal-columns=false 
# kafka.message-decoder.name=json (如果消息是JSON格式)
```

配置连接器后，需要在 Trino Coordinator 和所有 Worker 节点的 `etc/catalog` 目录下都放置对应的属性文件，并重启 Trino 服务。
DataSophon 会自动分发这些配置文件。

## 6. 性能调优与最佳实践

### 查询优化
*   **使用 `EXPLAIN (TYPE DISTRIBUTED) <SQL_QUERY>` 查看执行计划**：分析查询的各个阶段、操作符、数据分布和预计开销，找出潜在瓶颈。
*   **谓词下推 (Predicate Pushdown)**: 确保过滤条件尽可能早地在数据源或查询的早期阶段应用，以减少需要处理和传输的数据量。检查连接器是否支持谓词下推以及是否生效。
*   **Join 优化**: 
    *   确保 Join key 的数据类型一致。
    *   将大表放在 Probe Side (通常是 Join 的右侧)，小表放在 Build Side (通常是 Join 的左侧，用于构建哈希表)。Trino 通常会自动优化，但了解原理有帮助。
    *   考虑使用 `BROADCAST` Join Hint (如果一个小表可以完全加载到每个 Worker 的内存中)。
*   **`GROUP BY` 优化**: 避免在 `GROUP BY` 子句中使用高基数 (high-cardinality) 的列，这会导致大量的分组和内存消耗。
*   **使用近似查询函数**: 对于大型数据集，如果不需要精确结果，可以使用如 `approx_distinct()`, `approx_percentile()` 等近似聚合函数，它们通常更快且消耗更少资源。
*   **数据分区和分桶**: 对于 Hive 等数据源，合理的分区和分桶可以极大提高查询性能，因为 Trino 可以利用这些信息进行查询剪枝 (pruning)。

### 配置调优
*   **内存配置**: 
    *   `query.max-memory` 和 `query.max-memory-per-node` 是最重要的内存配置。根据集群总内存和并发查询数合理设置。
    *   JVM 堆大小 (`-Xmx`) 要为 Trino 进程和查询执行提供足够的内存。
*   **并发控制**: 
    *   `query.max-concurrent-queries`: Coordinator 接受的最大并发查询数。
    *   `query.max-queued-queries`: 最大排队查询数。
*   **Task 并发**: `task.concurrency` (每个 Worker 上并行处理的 Task 数量)。
*   **数据溢写 (Spilling)**: Trino 支持将内存不足的中间数据溢写到磁盘。通过 `experimental.spill-enabled=true` 和相关参数 (`experimental.spiller-spill-path`, `experimental.max-spill-per-node`) 启用和配置。这可以防止 OOM，但会降低性能。

### Trino 版本
*   保持 Trino 版本更新，新版本通常会带来性能改进、新功能和 Bug 修复。

### 数据源优化
*   确保底层数据源 (如 Hive Metastore, HDFS, RDBMS) 本身是健康的并且性能良好。
*   使用合适的文件格式 (如 ORC, Parquet) 和压缩方式 (如 Snappy, ZSTD) 来存储数据，这些列式格式对分析查询非常友好。

## 7. 安全配置

### 网络安全
*   使用防火墙限制对 Trino Coordinator 和 Worker 端口的访问。
*   考虑在 Trino 节点之间以及客户端与 Coordinator 之间启用 SSL/TLS 加密通信 (`http-server.https.enabled=true` 和相关 keystore/truststore 配置)。

### 认证 (Authentication)
Trino 支持多种认证机制，如 LDAP, Kerberos, Password file 等。
*   通过 `http-server.authentication.type` 选择认证类型。
*   例如，使用 LDAP:
    ```properties
    # etc/config.properties
    http-server.authentication.type=LDAP
    # etc/password-authenticator.properties 或特定认证配置文件
    ldap.url=ldaps://ldap-server:636
    ldap.user-bind-dn=cn=trino-user,ou=users,dc=example,dc=com
    ldap.user-bind-password=bind_password
    ldap.user-base-dn=ou=users,dc=example,dc=com
    ```

### 授权 (Authorization)
一旦用户通过认证，授权机制决定用户可以访问哪些数据以及执行哪些操作。
*   Trino 提供了基于文件的访问控制 (`access-control.name=file`，并在 `etc/access-control.properties` 和 `etc/rules.json` 中配置规则)。
*   也可以集成 Apache Ranger 或其他自定义访问控制插件。

DataSophon 在部署时如果集成了安全环境 (如 Kerberos)，可能会自动处理 Trino 的部分安全配置。

## 8. 故障排查

### 查询失败
*   **查看 Trino Web UI**: 查询详情页面通常会显示错误信息和失败的 Stage/Task。
*   **查看 Coordinator 和 Worker 日志**: 日志文件 (`node.data-dir`/var/log/server.log) 包含详细的错误堆栈和调试信息。
*   **常见原因**: OOM (OutOfMemoryError), 超时，连接器错误 (无法连接数据源、数据源权限问题)，SQL 语法错误，节点故障。

### 性能问题
*   **使用 Web UI 分析查询计划和 Live Plan**: 找出耗时长的 Stage 或 Task。
*   **检查资源使用**: CPU、内存、网络、磁盘 I/O (如果启用了 Spilling)。
*   **检查数据倾斜 (Data Skew)**: 如果某些 Task 处理的数据量远大于其他 Task，会导致性能瓶颈。
*   **调整配置参数**: 参考性能调优部分。

### Coordinator 或 Worker 无法启动/加入集群
*   检查 `discovery.uri` 配置是否正确，Coordinator 和 Worker 是否能互相访问。
*   检查端口是否被占用。
*   检查 JVM 配置和内存是否足够。
*   查看节点日志获取启动错误信息。

通过本指南，您应该能够更好地在 DataSophon 平台上管理和使用 Trino 服务。Trino 是一个强大的工具，理解其架构和配置对于发挥其最大潜力至关重要。对于更高级的用例和深入调优，请参考 Trino 官方文档。 