# Trino (Formerly PrestoSQL) 组件介绍

## Trino 是什么

Trino (原名 PrestoSQL) 是一个开源的、高性能的分布式 SQL 查询引擎，专为大数据分析而设计。它允许用户使用标准的 SQL 语句对存储在各种数据源中的大规模数据集进行快速、交互式的查询分析。

Trino 的核心设计理念是将计算与存储分离。它本身不存储数据，而是通过连接器 (Connector) 访问底层数据源。这意味着 Trino 可以查询存储在 HDFS、对象存储 (如 S3, GCS, Azure Blob Storage)、关系型数据库 (如 MySQL, PostgreSQL)、NoSQL 数据库 (如 Cassandra, MongoDB) 以及其他各种系统中的数据。

Trino 的目标是提供一个统一的 SQL 接口，让数据分析师和工程师能够轻松地跨越异构数据存储进行联合查询和分析，而无需进行复杂的数据迁移或 ETL 过程。

## 核心概念

理解 Trino 的核心概念对于有效使用它至关重要：

### 查询 (Query)
查询是用户提交给 Trino 的 SQL 语句。Trino 会将 SQL 查询解析、优化并编译成一系列的 Stage 和 Task，然后在集群中分布式执行。

### 连接器 (Connector)
连接器是 Trino 与底层数据源交互的桥梁。每个数据源类型 (如 Hive, MySQL, Kafka) 都需要一个相应的连接器。连接器负责：
*   从数据源获取元数据 (Schema, Table, Column 信息)。
*   将 Trino 查询计划的一部分下推到数据源执行 (如果数据源支持)。
*   从数据源读取数据并将其转换为 Trino 内部的内存格式。
Trino 拥有一个可插拔的连接器架构，用户可以开发自定义连接器来支持新的数据源。

### 目录 (Catalog)
目录对应一个已配置的连接器实例。一个目录可以包含多个 Schema。当用户在 SQL 查询中引用表时，通常会使用三段式名称：`catalog.schema.table`。
例如，如果配置了一个名为 `hive_prod` 的 Hive 连接器目录，以及一个名为 `mysql_sales` 的 MySQL 连接器目录，则可以查询 `hive_prod.web_logs.page_views` 和 `mysql_sales.emea.orders`。

### Schema (模式)
Schema 是在目录中组织表的一种方式，类似于关系型数据库中的数据库或模式概念。一个目录可以包含多个 Schema。

### 表 (Table)
表是数据的逻辑组织形式，由行和列组成。Trino 中的表定义和数据实际存储在底层数据源中。

### Coordinator (协调器)
Coordinator 是 Trino 集群的主节点，负责：
*   接收客户端提交的 SQL 查询。
*   解析、分析和优化查询，并生成分布式查询执行计划。
*   将查询计划分发给 Worker 节点执行。
*   管理 Worker 节点的状态。
*   收集查询结果并返回给客户端。
在一个 Trino 集群中，通常只有一个 Coordinator 节点。为了高可用，可以配置多个 Coordinator，但只有一个是活动的。

### Worker (工作节点)
Worker 是 Trino 集群中的工作节点，负责实际执行查询任务：
*   从 Coordinator 接收任务 (Task)。
*   通过连接器从数据源读取数据。
*   在内存中处理数据 (如过滤、聚合、连接)。
*   将中间结果或最终结果在 Worker 之间或返回给 Coordinator。
一个 Trino 集群通常包含多个 Worker 节点。

### Stage (阶段)
Trino 将一个复杂的 SQL 查询分解为多个执行阶段 (Stage)。Stage 是查询执行计划的一部分，代表一组相关的操作。Stage 之间存在依赖关系，例如一个 Stage 的输出可能是另一个 Stage 的输入。

### Task (任务)
Task 是 Stage 在特定数据片段 (Split) 上的具体执行单元。一个 Stage 会被划分为多个 Task，每个 Task 在一个 Worker 节点上运行。Task 并行执行，以提高查询性能。

### Split (数据分片)
Split 代表了可以被单个 Task 处理的一小部分数据。连接器负责将底层数据源中的数据划分为多个 Split。例如，对于 HDFS 上的文件，一个 Split 可能对应文件的一个块 (Block) 或文件的一部分。

### Exchange (数据交换)
Exchange 是 Trino 在不同 Worker 节点之间或不同 Stage 之间传输数据的机制。当一个 Stage 的输出需要作为另一个 Stage 的输入时，数据会通过 Exchange 进行 Shuffle。

### SPI (Service Provider Interface)
Trino 提供了强大的 SPI，允许开发者扩展 Trino 的功能，例如：
*   **Connector SPI**: 用于开发新的连接器以支持不同的数据源。
*   **Function SPI**: 用于创建自定义的标量函数、聚合函数或窗口函数。
*   **Access Control SPI**: 用于实现自定义的访问控制策略。
*   **Event Listener SPI**: 用于监听查询生命周期中的事件。

## 核心架构

Trino 采用经典的 MPP (Massively Parallel Processing) 架构：

1.  **客户端 (Client)**: 用户通过 JDBC/ODBC 驱动程序或 Trino CLI (命令行界面) 提交 SQL 查询给 Coordinator。
2.  **Coordinator**: Coordinator 接收查询，进行解析、优化，并生成一个分布式的查询执行计划。它将计划分解为多个 Stage 和 Task。
3.  **Worker 节点**: Coordinator 将 Task 分配给集群中的 Worker 节点。每个 Worker 执行分配给它的 Task，包括通过连接器从数据源读取数据、在内存中处理数据以及与其他 Worker 交换中间数据。
4.  **数据源 (Data Sources)**: Worker 节点通过相应的连接器与底层数据源进行通信，以获取元数据和实际数据。
5.  **结果返回**: Worker 节点将最终结果或中间结果发送回 Coordinator (或直接发送给下一个 Stage 的 Worker)。Coordinator 汇总最终结果并将其返回给客户端。

**关键特点**:
*   **内存计算**: Trino 主要在内存中执行计算，避免了磁盘 I/O 的瓶颈，从而实现高性能。
*   **流水线执行 (Pipelined Execution)**: 数据在不同的操作和 Stage 之间以流水线的方式处理，减少了中间数据的写入和读取开销。
*   **动态代码生成**: Trino 会为每个查询动态生成优化的 Java 字节码，以提高执行效率。

## 关键特性

### 高性能
Trino 专为快速的交互式分析而设计，通过内存计算、MPP 架构、向量化处理和动态代码生成等技术实现低延迟查询。

### 联邦查询 (Federated Queries)
能够通过单个 SQL 查询连接和分析来自多个异构数据源的数据，无需预先进行数据整合。例如，可以将 Hive 表与 MySQL 表进行 JOIN 操作。

### 可扩展性
Trino 集群可以水平扩展，通过增加 Worker 节点的数量来提高查询并发能力和处理大规模数据集的能力。

### SQL 兼容性
支持 ANSI SQL 标准，并提供了丰富的 SQL 函数和操作符，使得熟悉 SQL 的用户可以轻松上手。

### 广泛的数据源支持
通过其连接器架构，支持众多流行的数据存储系统，包括但不限于：
*   **Hadoop Ecosystem**: Hive, HDFS
*   **Object Stores**: S3, Google Cloud Storage, Azure Blob Storage, MinIO
*   **Relational Databases**: MySQL, PostgreSQL, SQL Server, Oracle
*   **NoSQL Databases**: Cassandra, MongoDB, Elasticsearch
*   **Message Queues**: Kafka
*   **Others**: Druid, Pinot, Prometheus

### 易于部署和管理
Trino 的部署相对简单，并且提供了 Web UI 用于监控集群状态、查询执行情况和性能指标。

### 社区活跃和持续发展
Trino (前身为 PrestoSQL，由 PrestoDB 的原始创建者在 Facebook 分裂后继续开发) 拥有一个强大且活跃的开源社区，项目在持续快速发展和演进中。

## 常见用例

Trino 因其高性能和联邦查询能力，被广泛应用于以下场景：

*   **交互式数据探索与分析**: 数据分析师和科学家可以使用 Trino 对海量数据进行快速的 Ad-hoc 查询和探索性分析。
*   **BI 报表与可视化**: 作为 BI 工具 (如 Tableau, Superset, Qlik) 的后端查询引擎，为仪表盘和报表提供实时数据支持。
*   **数据联邦与统一数据访问**: 打破数据孤岛，提供一个统一的 SQL 接口来访问和分析分布在不同系统中的数据。
*   **ETL/ELT 替代或补充**: 在某些场景下，Trino 可以用于替代传统的 ETL 过程，直接对源数据进行转换和加载，或者作为 ETL 流程中的一个查询加速层。
*   **数据湖查询**: 直接查询存储在数据湖 (如基于 HDFS 或对象存储的 Hive 表、Delta Lake, Iceberg, Hudi 表) 中的数据。
*   **A/B 测试分析**: 快速分析 A/B 测试产生的大量用户行为数据。
*   **日志分析**: 对存储在各种系统中的应用日志、服务器日志进行 SQL 查询和分析。

Trino 为企业提供了一个强大、灵活且高效的方式来释放其大数据的价值。 