# Elasticsearch 用户指南

## 开始使用

本指南将帮助您入门 Elasticsearch，包括安装概览、基本配置和连接方法。

### 安装概览

Elasticsearch 可以通过多种方式安装，包括下载压缩包、使用包管理器（如 APT, YUM）或运行 Docker 容器。

**基本安装步骤 (以压缩包为例):**

1.  **下载 Elasticsearch**: 从 Elastic 官方网站下载对应您操作系统的 Elasticsearch 压缩包。
2.  **解压文件**: 将下载的 `.tar.gz` 或 `.zip` 文件解压到您选择的目录。
    ```bash
    tar -xzf elasticsearch-X.Y.Z-linux-x86_64.tar.gz
    cd elasticsearch-X.Y.Z/
    ```
3.  **运行 Elasticsearch (单节点开发模式)**:
    ```bash
    ./bin/elasticsearch
    ```
    默认情况下，Elasticsearch 会在前台运行，并将数据存储在安装目录下的 `data` 文件夹，日志在 `logs` 文件夹。它会监听 `localhost:9200` (HTTP) 和 `localhost:9300` (Transport/内部节点通信)。

**集群部署**: 生产环境通常需要部署 Elasticsearch 集群。
-   **配置 `config/elasticsearch.yml`**: 为每个节点配置 `cluster.name`, `node.name`, `network.host`, `discovery.seed_hosts` (用于节点发现) 等。
-   确保节点间网络互通，特别是 9200 和 9300 端口。
-   逐个启动节点，它们会自动发现并组成集群。

**Docker 部署示例 (单节点):**
```bash
docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:X.Y.Z
```
将 `X.Y.Z` 替换为您想要的版本号。

### 基本配置 (`config/elasticsearch.yml`)

一些重要的配置项：

-   `cluster.name`: 集群名称，同一集群内的节点必须具有相同的集群名称。
-   `node.name`: 节点名称，在集群中应唯一。
-   `path.data`: 数据存储路径 (可配置多个路径)。
-   `path.logs`: 日志存储路径。
-   `network.host`: 节点绑定的网络地址 (如 `0.0.0.0` 允许外部访问)。
-   `http.port`: HTTP REST API 监听端口 (默认 9200)。
-   `discovery.seed_hosts`: 集群发现机制中用于初始连接的节点列表 (例如 `["host1:9300", "host2:9300"]`)。
-   `cluster.initial_master_nodes`: 首次启动集群时，指定哪些节点有资格成为 Master 节点。

### 连接到 Elasticsearch

-   **HTTP 客户端 (如 `curl`)**: 由于 Elasticsearch 提供 REST API，任何 HTTP 客户端都可以与其交互。
    ```bash
    # 获取集群健康状态
    curl -X GET "localhost:9200/_cluster/health?pretty"

    # 创建一个索引
    curl -X PUT "localhost:9200/my_index"

    # 索引一个文档
    curl -X POST "localhost:9200/my_index/_doc/1?pretty" -H 'Content-Type: application/json' -d'
    {
      "user": "kimchy",
      "post_date": "2009-11-15T14:12:12",
      "message": "Trying out Elasticsearch"
    }'
    ```

-   **官方语言客户端**: Elastic 为多种编程语言提供了官方客户端，如 Java, Python, JavaScript, Go, Ruby, .NET, PHP 等。这些客户端封装了 API 调用，提供了更便捷的编程接口。
    -   **Python Client (`elasticsearch-py`) 示例**:
        ```python
        from elasticsearch import Elasticsearch

        # 连接到本地 Elasticsearch 实例
        es = Elasticsearch([{'host': 'localhost', 'port': 9200, 'scheme': 'http'}])

        if es.ping():
            print("Connected to Elasticsearch!")
        else:
            print("Could not connect!")

        # 索引文档
        doc = {
            'author': 'jane_doe',
            'text': 'Elasticsearch with Python is fun!',
            'timestamp': '2023-10-27T10:30:00'
        }
        resp = es.index(index="test-index", id=1, document=doc)
        print(resp['result'])

        # 获取文档
        resp = es.get(index="test-index", id=1)
        print(resp['_source'])
        ```

-   **Kibana Dev Tools**: Kibana 提供了一个方便的控制台 (Dev Tools)，可以直接发送 REST 请求到 Elasticsearch 并查看结果。

## 索引管理

### 创建索引

使用 `PUT` 请求创建索引。可以同时指定索引的设置 (settings) 和映射 (mappings)。

```json
PUT /my_new_index
{
  "settings": {
    "number_of_shards": 3,      // 主分片数量
    "number_of_replicas": 1   // 每个主分片的副本数量
  },
  "mappings": {
    "properties": {
      "title": { "type": "text" },
      "author": { "type": "keyword" },
      "publish_date": { "type": "date" },
      "views": { "type": "integer" }
    }
  }
}
```

-   `number_of_shards`: 主分片数量在索引创建后不可更改。
-   `number_of_replicas`: 副本数量可以随时动态调整。

### 索引设置 (Index Settings)

索引设置控制索引的行为，如分片数量、副本数量、分析器配置、刷新间隔等。
-   **静态设置 (Static Settings)**: 只能在索引创建时设置或在索引关闭时修改，如 `index.number_of_shards`。
-   **动态设置 (Dynamic Settings)**: 可以随时通过 `PUT /<index_name>/_settings` API 进行修改，如 `index.number_of_replicas`, `index.refresh_interval`。

### 映射 (Mappings)

映射定义了索引中文档的结构以及字段如何被存储和索引。

-   **动态映射 (Dynamic Mapping)**: 如果未显式定义映射，Elasticsearch 会在索引第一个文档时自动推断字段类型并创建映射。这在开发初期很方便，但生产环境建议使用显式映射。
-   **显式映射 (Explicit Mapping)**: 通过 `PUT /<index_name>/_mapping` API 或在创建索引时定义。
    ```json
    PUT /products
    {
      "mappings": {
        "properties": {
          "product_id": { "type": "keyword" },
          "name": {
            "type": "text",
            "analyzer": "standard",
            "fields": { // 多字段特性，name 字段可以同时作为 text 和 keyword 类型
              "keyword": {
                "type": "keyword",
                "ignore_above": 256
              }
            }
          },
          "description": { "type": "text", "analyzer": "english" },
          "price": { "type": "float" },
          "tags": { "type": "keyword" }, // keyword 类型适合精确匹配、排序和聚合
          "available_since": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis" },
          "location": { "type": "geo_point" }
        }
      }
    }
    ```
    **核心数据类型**: `text`, `keyword`, `byte`, `short`, `integer`, `long`, `float`, `double`, `boolean`, `date`, `object`, `nested`, `geo_point`, `ip` 等。

### 分析器 (Analyzers)

分析器用于处理 `text` 类型的字段。可以在索引设置中定义自定义分析器，并在映射中引用它们。

```json
PUT /my_custom_analyzer_index
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_custom_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "char_filter": ["html_strip"],
          "filter": ["lowercase", "asciifolding", "my_stemmer"]
        }
      },
      "filter": {
        "my_stemmer": {
          "type": "stemmer",
          "language": "english"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "content": {
        "type": "text",
        "analyzer": "my_custom_analyzer" // 使用自定义分析器
      }
    }
  }
}
```

### 索引模板 (Index Templates)

索引模板允许您定义一组设置和映射，当新创建的索引名称匹配模板模式时，这些设置和映射会自动应用到新索引上。这对于管理按时间滚动的索引（如日志索引）非常有用。

```json
PUT /_index_template/logs_template
{
  "index_patterns": ["logs-*"], // 匹配以 logs- 开头的索引名
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1
    },
    "mappings": {
      "properties": {
        "timestamp": { "type": "date" },
        "message": { "type": "text", "analyzer": "standard" },
        "level": { "type": "keyword" },
        "hostname": { "type": "keyword" }
      }
    }
  },
  "priority": 200 // 模板优先级，数字越大优先级越高
}
```

### 别名 (Aliases)

索引别名是指向一个或多个索引的辅助名称。使用别名可以实现：
-   在不中断服务的情况下重新索引数据（零停机切换）。
-   对多个索引进行分组查询。
-   为索引提供更友好的名称。

```json
POST /_aliases
{
  "actions": [
    { "add": { "index": "my_index_v1", "alias": "my_data" } }
  ]
}

// 零停机切换示例：
// 1. 创建新索引 my_index_v2 并导入数据
// 2. 将别名 my_data 从 my_index_v1 指向 my_index_v2，并移除旧指向
POST /_aliases
{
  "actions": [
    { "remove": { "index": "my_index_v1", "alias": "my_data" } },
    { "add":    { "index": "my_index_v2", "alias": "my_data" } }
  ]
}
```

### 删除索引

```json
DELETE /my_index_to_delete
```
**警告**: 删除索引会永久删除其中的所有数据，请谨慎操作。

## 文档管理

### 索引文档 (Indexing Documents)

索引文档是指将一个 JSON 文档存储到指定的索引中。

-   **`POST /<index_name>/_doc`** (自动生成 ID):
    ```json
    POST /blog/_doc
    {
      "title": "My First Blog Post",
      "author": "John Doe",
      "content": "This is the content of my first post.",
      "tags": ["elasticsearch", "beginner"]
    }
    ```
    Elasticsearch 会自动为文档生成一个唯一的 ID。

-   **`PUT /<index_name>/_doc/<document_id>`** (指定 ID):
    ```json
    PUT /blog/_doc/1
    {
      "title": "Another Blog Post",
      "author": "Jane Smith",
      "content": "Content here...",
      "tags": ["guide", "tips"]
    }
    ```
    如果具有相同 ID 的文档已存在，此操作会更新（覆盖）该文档，并增加版本号。如果 ID 不存在，则创建新文档。

-   **`POST /<index_name>/_create/<document_id>`** 或 **`PUT /<index_name>/_create/<document_id>`** (显式创建):
    此操作只在文档 ID 不存在时才会成功创建文档。如果 ID 已存在，会返回错误。用于确保不会意外覆盖现有文档。

### 更新文档 (Updating Documents)

-   **全量更新 (Full Update)**: 使用 `PUT /<index_name>/_doc/<document_id>` 重新索引整个文档。
-   **部分更新 (Partial Update)**: 使用 `POST /<index_name>/_update/<document_id>` API，可以只更新文档中的特定字段，或使用脚本进行更复杂的更新。
    ```json
    // 更新特定字段
    POST /blog/_update/1
    {
      "doc": {
        "views": 100,
        "status": "published"
      }
    }

    // 使用脚本增加 views 字段的值
    POST /blog/_update/1
    {
      "script": {
        "source": "ctx._source.views += params.count",
        "lang": "painless",
        "params": {
          "count": 5
        }
      }
    }
    ```

### 删除文档 (Deleting Documents)

```json
DELETE /blog/_doc/1
```
删除操作会将文档标记为已删除，实际的物理删除会在后续的段合并（Segment Merging）过程中发生。

### 批量操作 (Bulk API)

Bulk API 允许在一个请求中执行多个索引、创建、更新或删除操作，从而提高效率并减少网络开销。
请求体格式为一系列 JSON 对象，每个操作占两行（操作元数据行 + 可选的文档源数据行）。每行必须以换行符 (`\n`) 结尾。

```json
POST /_bulk
{ "index" : { "_index" : "test", "_id" : "1" } }
{ "field1" : "value1" }
{ "delete" : { "_index" : "test", "_id" : "2" } }
{ "create" : { "_index" : "test", "_id" : "3" } }
{ "field1" : "value3" }
{ "update" : {"_id" : "1", "_index" : "test"} }
{ "doc" : {"field2" : "value2"} }
```
**注意**: Bulk 请求的最后一行也必须有换行符。

### 获取文档 (Get Document)

```json
GET /blog/_doc/1?pretty
```
返回文档的 `_source` (原始 JSON 内容) 以及元数据如 `_index`, `_id`, `_version` 等。

### 文档版本控制 (Versioning)

Elasticsearch 会为每个文档维护一个内部版本号 (`_version`)。每次对文档进行更改（索引、更新、删除）时，版本号都会递增。这可以用于实现乐观并发控制 (Optimistic Concurrency Control) ，通过在请求中指定 `if_seq_no` 和 `if_primary_term` 参数来防止并发修改导致的数据丢失。

## 搜索数据

Elasticsearch 提供了强大的 Query DSL (Domain Specific Language) 来执行搜索。

### 基本查询

-   **`GET /<index_name>/_search?q=<query_string>` (URI Search)**: 简单的基于查询字符串的搜索，适合快速测试，但功能有限。
    ```bash
    GET /blog/_search?q=title:Elasticsearch&pretty
    ```

-   **Request Body Search**: 使用 Query DSL，功能更强大。
    ```json
    GET /blog/_search
    {
      "query": {
        "match": {
          "content": "search engine"
        }
      }
    }
    ```

### Query DSL 核心组件

-   **查询子句 (Query Clauses)**: 定义查询条件。分为两种类型：
    -   **叶子查询子句 (Leaf Query Clauses)**: 针对特定字段查找特定值，如 `match`, `term`, `range`。
    -   **复合查询子句 (Compound Query Clauses)**: 包装其他叶子或复合查询子句，以逻辑方式组合它们（如 `bool`）或改变它们的行为。

### 查询上下文 (Query Context) vs 过滤上下文 (Filter Context)

-   **查询上下文 (Query Context)**: 在 `query` 参数中使用的子句。这些子句不仅判断文档是否匹配，还会计算相关性得分 (`_score`)。
-   **过滤上下文 (Filter Context)**: 在 `bool` 查询的 `filter` 参数中使用的子句。这些子句只判断文档是否匹配（是/否），不计算得分。过滤上下文通常更快，并且结果可以被缓存。

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must": [ // 文档必须匹配此查询，会计入得分
        { "match": { "name": "Laptop" } }
      ],
      "filter": [ // 文档必须匹配此过滤器，不计入得分，可缓存
        { "term": { "brand.keyword": "BrandX" } },
        { "range": { "price": { "gte": 1000, "lte": 2000 } } }
      ]
    }
  }
}
```

### 常用查询类型

-   **`match` query**: 标准的全文搜索查询，会对查询文本进行分析。
    ```json
    GET /blog/_search
    {
      "query": {
        "match": { "content": "powerful search analytics" }
      }
    }
    ```
-   **`match_phrase` query**: 匹配短语，要求查询文本中的词项按顺序出现。
    ```json
    GET /blog/_search
    {
      "query": {
        "match_phrase": { "content": "distributed search engine" }
      }
    }
    ```
-   **`multi_match` query**: 在多个字段上执行相同的 `match` 查询。
    ```json
    GET /blog/_search
    {
      "query": {
        "multi_match": {
          "query": "elasticsearch guide",
          "fields": ["title", "content"]
        }
      }
    }
    ```
-   **`term` query**: 精确匹配，不对查询文本进行分析。通常用于 `keyword`, `numeric`, `date`, `boolean` 等类型的字段。
    ```json
    GET /products/_search
    {
      "query": {
        "term": { "tags.keyword": "electronics" } // 假设 tags 是 keyword 类型或 text 类型的 keyword 子字段
      }
    }
    ```
-   **`terms` query**: 匹配字段值在给定列表中的文档。
    ```json
    GET /products/_search
    {
      "query": {
        "terms": { "product_id": ["P123", "P456"] }
      }
    }
    ```
-   **`range` query**: 匹配字段值在指定范围内的文档 (数字、日期、字符串)。
    ```json
    GET /orders/_search
    {
      "query": {
        "range": {
          "order_date": {
            "gte": "2023-01-01",
            "lte": "2023-12-31",
            "format": "yyyy-MM-dd"
          }
        }
      }
    }
    ```
-   **`bool` query (Boolean Query)**: 复合查询，用于组合多个查询子句。包含以下操作符：
    -   `must`: 子句必须匹配，并贡献得分。
    -   `filter`: 子句必须匹配，但不贡献得分（在过滤上下文中使用）。
    -   `should`: 子句应该匹配。如果用在 `bool` 查询中且没有 `must` 或 `filter`，则至少一个 `should` 子句必须匹配。如果存在 `must` 或 `filter`，则 `should` 子句用于调整相关性得分。
    -   `must_not`: 子句绝不能匹配，不贡献得分（在过滤上下文中使用）。

### 排序 (Sorting)

默认情况下，结果按相关性得分 (`_score`) 降序排列。可以按一个或多个字段排序。

```json
GET /blog/_search
{
  "query": { "match_all": {} },
  "sort": [
    { "publish_date": { "order": "desc" } }, // 按发布日期降序
    { "_score": { "order": "desc" } }      // 再按得分降序
  ]
}
```
对于 `text` 字段排序，通常需要使用其 `keyword` 类型的子字段。

### 分页 (Pagination)

使用 `from` 和 `size` 参数进行分页：
-   `from`: 起始文档的偏移量 (默认为 0)。
-   `size`: 每页返回的文档数量 (默认为 10)。

```json
GET /blog/_search
{
  "query": { "match_all": {} },
  "from": 10, // 从第11条开始 (0-indexed)
  "size": 5   // 返回5条
}
```
对于深度分页，推荐使用 `search_after` 参数或 Scroll API，因为 `from`/`size` 方式在页数很深时性能会下降。

### 高亮 (Highlighting)

高亮显示搜索结果中与查询匹配的词项。

```json
GET /blog/_search
{
  "query": {
    "match": { "content": "elasticsearch" }
  },
  "highlight": {
    "fields": {
      "content": {}
    }
  }
}
```
结果中每个匹配的文档会包含一个 `highlight` 字段，其中包含高亮片段。

## 聚合 (Aggregations)

聚合框架允许从数据中生成分析信息。聚合可以嵌套以构建复杂的数据摘要。

### 聚合类型

-   **桶聚合 (Bucket Aggregations)**: 根据标准将文档分组到不同的桶中。每个桶关联一个键和其中的文档数量。
-   **指标聚合 (Metric Aggregations)**: 计算桶内文档的统计指标，如平均值、总和、最大值、最小值等。
-   **管道聚合 (Pipeline Aggregations)**: 对其他聚合的输出或其产生的指标进行聚合。
-   **矩阵聚合 (Matrix Aggregations)**: 对多个字段进行操作，并根据这些字段的值生成一个矩阵结果。

### 常用桶聚合

-   **`terms` aggregation**: 基于字段的词项进行分组。
    ```json
    GET /products/_search
    {
      "size": 0, // 通常不关心搜索结果本身，只关心聚合结果
      "aggs": {
        "group_by_tags": {
          "terms": { "field": "tags.keyword", "size": 10 } // 按 tags.keyword 字段分组，返回前10个桶
        }
      }
    }
    ```
-   **`histogram` / `date_histogram` aggregation**: 基于数值或日期字段的间隔进行分组。
    ```json
    GET /sales/_search
    {
      "size": 0,
      "aggs": {
        "sales_over_time": {
          "date_histogram": {
            "field": "order_date",
            "calendar_interval": "month", // 按月分组
            "format": "yyyy-MM"
          }
        }
      }
    }
    ```
-   **`range` / `date_range` / `ip_range` aggregation**: 基于用户定义的范围进行分组。
-   **`filter` / `filters` aggregation**: 将匹配特定过滤器的文档分到一个桶中。

### 常用指标聚合

通常嵌套在桶聚合内部，对每个桶内的数据进行计算。

-   `sum`, `avg`, `min`, `max`: 计算总和、平均值、最小值、最大值。
-   `stats`: 一次性计算 `count`, `min`, `max`, `avg`, `sum`。
-   `extended_stats`: 包含更多统计信息，如方差、标准差等。
-   `cardinality`: 计算字段的近似唯一值数量 (基于 HyperLogLog++)。
-   `percentiles`: 计算百分位数。
-   `top_hits`: 返回每个桶中最相关的文档样本。

```json
GET /sales/_search
{
  "size": 0,
  "aggs": {
    "group_by_category": {
      "terms": { "field": "category.keyword", "size": 5 },
      "aggs": { // 嵌套指标聚合
        "total_revenue": { "sum": { "field": "price" } },
        "average_quantity": { "avg": { "field": "quantity" } }
      }
    }
  }
}
```

## 集群管理与监控

Elasticsearch 提供了丰富的 API 来监控和管理集群。

### 集群健康 (Cluster Health API)

```json
GET /_cluster/health?pretty
```
返回集群状态 (`green`, `yellow`, `red`)、节点数、分片数、活动分片百分比等信息。
-   `green`: 所有主分片和副本分片都已分配。
-   `yellow`: 所有主分片都已分配，但至少有一个副本分片未分配。
-   `red`: 至少有一个主分片未分配。集群部分功能不可用。

### 节点统计 (Nodes Stats API)

```json
GET /_nodes/stats?pretty
GET /_nodes/node_id1,node_id2/stats?pretty // 特定节点
```
提供关于集群中一个或多个节点的详细统计信息，包括 JVM、OS、文件系统、网络、线程池、索引等。

### 索引统计 (Index Stats API)

```json
GET /my_index/_stats?pretty
GET /_all/_stats?pretty // 所有索引
```
提供关于一个或多个索引的统计信息，如文档数量、存储大小、段信息、合并统计、刷新统计等。

### 常见集群问题

-   **Unassigned Shards**: 副本分片未分配（Yellow 状态）或主分片未分配（Red 状态）。可能原因包括节点不足、磁盘空间不足、配置错误等。使用 `GET /_cluster/allocation/explain` API 诊断原因。
-   **High JVM Heap Usage**: JVM 堆内存使用过高可能导致性能下降或 OutOfMemoryError。需要分析堆转储 (Heap Dump) 或调整 JVM 设置、优化查询/聚合。
-   **High CPU Usage**: 可能由重量级查询、聚合、索引操作或配置不当引起。

### 备份与快照 (Backup and Snapshot)

Snapshot API 用于创建集群或特定索引的快照，并将其存储到远程仓库（如共享文件系统、S3、HDFS、Azure Blob Storage、GCS）。

1.  **注册快照仓库 (Repository)**:
    ```json
    PUT /_snapshot/my_backup_repository
    {
      "type": "fs", // 文件系统类型仓库
      "settings": {
        "location": "/mnt/backups/elasticsearch_backup" // 共享文件系统路径
      }
    }
    ```
    对于云存储，需要安装相应的插件 (e.g., `repository-s3`) 并配置凭证。

2.  **创建快照**:
    ```json
    PUT /_snapshot/my_backup_repository/snapshot_1?wait_for_completion=true
    {
      "indices": "index_1,index_2", // 可选，指定要备份的索引，默认全部
      "ignore_unavailable": true,    // 如果指定索引不存在则忽略
      "include_global_state": false  // 是否备份集群全局状态
    }
    ```

3.  **查看快照**:
    ```json
    GET /_snapshot/my_backup_repository/snapshot_1?pretty
    ```

4.  **恢复快照**:
    ```json
    POST /_snapshot/my_backup_repository/snapshot_1/_restore?wait_for_completion=true
    {
      "indices": "index_1", // 指定要恢复的索引
      "rename_pattern": "index_(.+)",
      "rename_replacement": "restored_index_$1" // 恢复时重命名索引
    }
    ```

## 性能调优与最佳实践

### 分片策略与大小

-   避免过多的小分片 (Over-sharding)：每个分片都有资源开销。过多分片会增加集群元数据负担和管理复杂性。
-   避免过大的分片：大分片在恢复、迁移或分配时可能耗时较长。
-   理想的分片大小通常在几 GB 到几十 GB 之间，具体取决于用例和硬件。
-   主分片数量在索引创建后无法更改，需提前规划。

### 映射最佳实践

-   **显式映射**: 为生产环境定义明确的映射，避免动态映射带来的不确定性。
-   **禁用不需要的特性**: 如果字段不需要全文搜索，将其映射为 `keyword` 类型或禁用索引 (`index: false`)。
-   **禁用 `_source`**: 如果不需要访问原始文档内容（例如，仅用于聚合），可以禁用 `_source` 字段以节省存储空间，但这会限制某些功能（如高亮、重新索引）。
-   **禁用 `_all` 字段 (旧版本)**: 在 Elasticsearch 6.0 之前，`_all` 字段会索引所有字段的内容。如果不需要，应禁用它以节省资源。从 6.0 开始，`_all` 字段默认禁用。
-   **使用 `ignore_above`**: 对于 `keyword` 字段，使用 `ignore_above` 限制索引词项的长度，避免过长的词项导致的问题。

### 查询优化

-   **使用过滤上下文 (Filter Context)**: 对于不需要评分的精确匹配条件，尽量使用 `bool` 查询的 `filter` 子句，它们可以被缓存且性能更好。
-   **避免脚本查询 (Script Queries)**: 脚本查询通常比原生查询慢，谨慎使用。
-   **避免前导通配符查询**: 如 `*text` 或 `?text`，这类查询非常耗费资源，因为它们无法利用倒排索引的优势。
-   **合理使用分页**: 对于深度分页，使用 `search_after` 或 Scroll API。
-   **Profile API**: 使用 `GET /<index>/_search { "profile": true, ... }` 分析查询的各个阶段耗时，找出瓶颈。

### JVM 调优

-   **堆大小 (Heap Size)**: 为 Elasticsearch JVM 分配足够的堆内存，但不应超过物理内存的 50%，且最大不超过 30-32GB (以利用压缩对象指针 Compressed Oops)。
-   在 `config/jvm.options` (或 `jvm.options.d/` 目录下的文件) 中设置 `-Xms` (初始堆大小) 和 `-Xmx` (最大堆大小) 为相同的值，以避免运行时堆大小调整带来的开销。
-   监控 JVM 堆使用情况和垃圾回收 (GC) 活动。

### 缓存

Elasticsearch 有多种缓存机制：
-   **节点查询缓存 (Node Query Cache)**: 缓存过滤上下文中使用的查询结果 (位集)。
-   **分片请求缓存 (Shard Request Cache)**: 缓存聚合结果、建议结果和 `hits.total`。默认情况下，只缓存 `size: 0` 的请求。
-   **文件系统缓存 (OS Page Cache)**: Elasticsearch 严重依赖操作系统的文件系统缓存来加速对索引文件的访问。确保操作系统有足够的空闲内存用于页缓存。

## Kibana 简介 (可视化与探索)

Kibana 是 Elastic Stack 的可视化组件，与 Elasticsearch 紧密集成。

-   **Discover**: 交互式地探索和搜索 Elasticsearch 中的数据，查看原始文档，过滤数据。
-   **Visualize**: 创建各种图表（如折线图、柱状图、饼图、地图等）来可视化 Elasticsearch 中的数据和聚合结果。
-   **Dashboard**: 将多个可视化组件组合到一个仪表盘中，提供数据的统一视图和监控界面。
-   **Dev Tools**: 提供一个控制台，可以直接向 Elasticsearch 发送 REST API 请求并查看响应。
-   **Stack Management**: 管理索引模式、索引生命周期策略 (ILM)、快照与恢复、用户角色等。

本指南涵盖了 Elasticsearch 的核心概念和基本用法。Elasticsearch 功能非常丰富，深入学习请参考官方文档和社区资源。 