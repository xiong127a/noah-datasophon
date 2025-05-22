# NebulaGraph 用户指南

## 基本概念

在开始使用 NebulaGraph 之前，了解其核心概念是非常重要的：

### 图数据结构

NebulaGraph 采用属性图模型，主要包含以下几个核心概念：

- **点（Vertex）**：图的基本单位，表示实体，如人、地点或事物。
- **边（Edge）**：连接点的线，表示实体之间的关系，如"关注"、"购买"等。
- **属性（Property）**：点和边上可以附加的信息，如姓名、年龄、权重等。
- **标签（Tag）**：点的类型，一个点可以有多个标签。
- **边类型（Edge Type）**：边的类型，定义了边的性质和连接的点类型。

### 数据模型

NebulaGraph 的数据模型具有以下特点：

- **强类型**：每个标签和边类型都有明确定义的属性和类型。
- **有向图**：所有边都是有向的，从源点指向目标点。
- **多关系**：两个点之间可以有多种类型的边，表示不同的关系。
- **属性丰富**：点和边都可以有多个属性，支持多种数据类型。

![数据模型](https://docs.nebula-graph.com.cn/master/figs/property-graph.png)

## 使用流程

### 环境准备

使用 NebulaGraph 前，需要完成以下准备工作：

1. **硬件要求**：
   - 生产环境推荐配置：CPU 16核+，内存 64GB+，SSD 200GB+
   - 测试环境最低配置：CPU 4核，内存 8GB，SSD 100GB

2. **软件要求**：
   - 支持的操作系统：CentOS 7.x、Ubuntu 16.04+、Debian 9.0+
   - 依赖包：GCC 7.5.0+、CMake 3.5.0+、Python 3.6.0+

### 安装部署

NebulaGraph 提供多种安装方式：

#### 使用 RPM/DEB 包安装

```bash
# CentOS/RedHat
$ sudo rpm -ivh nebula-graph-<version>.el7.x86_64.rpm

# Ubuntu/Debian
$ sudo dpkg -i nebula-graph-<version>.ubuntu1804.amd64.deb
```

#### 使用 Docker 安装

```bash
# 拉取镜像
$ docker pull vesoft/nebula-graph:<version>

# 运行容器
$ docker run -it --name nebula-docker -p 9669:9669 -p 19669:19669 -p 19670:19670 vesoft/nebula-graph:<version>
```

#### 使用 Nebula Operator 在 Kubernetes 中部署

```bash
# 添加 Helm 仓库
$ helm repo add nebula-operator https://vesoft-inc.github.io/nebula-operator/charts

# 安装 Nebula Operator
$ helm install nebula-operator nebula-operator/nebula-operator --namespace=<namespace>

# 部署 NebulaGraph 集群
$ helm install nebula nebula-operator/nebula-cluster --namespace=<namespace>
```

### 集群配置

NebulaGraph 集群包含三种服务：Meta 服务、Storage 服务和 Graph 服务。每种服务都有独立的配置文件：

- **Meta 服务配置**：`nebula-metad.conf`
- **Storage 服务配置**：`nebula-storaged.conf`
- **Graph 服务配置**：`nebula-graphd.conf`

主要配置参数包括：

```conf
# 基本配置
--meta_server_addrs=<meta_server_ip1>:9559,<meta_server_ip2>:9559,<meta_server_ip3>:9559
--local_ip=<local_ip>
--port=<service_port>

# 日志配置
--log_dir=<log_dir>
--minloglevel=0  # 0: INFO, 1: WARNING, 2: ERROR, 3: FATAL

# 性能相关配置
--num_accept_threads=<threads_num>
--num_netio_threads=<threads_num>
--num_worker_threads=<threads_num>
```

### 数据库操作

#### 创建和使用空间

```sql
-- 创建空间
CREATE SPACE my_graph(partition_num=15, replica_factor=3, vid_type=FIXED_STRING(30));

-- 使用空间
USE my_graph;
```

#### 创建 Schema

```sql
-- 创建标签
CREATE TAG person(name string, age int);

-- 创建边类型
CREATE EDGE follows(degree int, start_time timestamp);

-- 创建索引
CREATE TAG INDEX person_name_index ON person(name);
```

#### 数据导入

NebulaGraph 提供多种数据导入工具：

1. **INSERT 语句**：适用于小规模数据

```sql
-- 插入点
INSERT VERTEX person(name, age) VALUES "person1":("Tom", 18);

-- 插入边
INSERT EDGE follows(degree, start_time) VALUES "person1"->"person2":(95, "2021-01-01T00:00:00");
```

2. **Nebula Importer**：CSV 文件批量导入工具

```yaml
# 配置文件示例
version: v2
description: example
removeTempFiles: false
clientSettings:
  retry: 3
  concurrency: 10
  channelBufferSize: 128
  space: my_graph
  connection:
    user: root
    password: nebula
    address: 192.168.8.1:9669,192.168.8.2:9669
```

3. **Spark Connector**：从 Hive、Parquet、JSON 等源导入数据

```scala
// Scala 代码示例
import com.vesoft.nebula.connector.NebulaConnectionConfig
import com.vesoft.nebula.connector.writer.NebulaGraphWriter

val config = NebulaConnectionConfig
  .builder()
  .withMetaAddress("192.168.8.1:9559")
  .withGraphAddress("192.168.8.1:9669")
  .withUsername("root")
  .withPassword("nebula")
  .build()

df.write
  .nebula(config, "my_graph")
  .writeVertices("person", Seq("id"), Seq("name", "age"))
```

## 开发指南

### 客户端连接

NebulaGraph 提供多种编程语言的客户端：

#### Java 客户端

```java
// 添加依赖
// <dependency>
//     <groupId>com.vesoft</groupId>
//     <artifactId>client</artifactId>
//     <version>3.0.0</version>
// </dependency>

// 连接代码
NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
List<HostAddress> addresses = Arrays.asList(
    new HostAddress("192.168.8.1", 9669),
    new HostAddress("192.168.8.2", 9669)
);

NebulaPool pool = new NebulaPool();
pool.init(addresses, nebulaPoolConfig);

Session session = pool.getSession("root", "nebula", false);
ResultSet rs = session.execute("USE my_graph; MATCH (v:person) RETURN v LIMIT 10");
```

#### Python 客户端

```python
# 安装依赖
# pip install nebula3-python

# 连接代码
from nebula3.gclient.net import ConnectionPool
from nebula3.Config import Config

config = Config()
connection_pool = ConnectionPool()
connection_pool.init([('192.168.8.1', 9669)], config)

client = connection_pool.get_session('root', 'nebula')
client.execute('USE my_graph')
result = client.execute('MATCH (v:person) RETURN v LIMIT 10')
```

#### Go 客户端

```go
// 安装依赖
// go get -u github.com/vesoft-inc/nebula-go/v3

// 连接代码
import (
    "github.com/vesoft-inc/nebula-go/v3"
)

hostAddress := []nebula.HostAddress{
    {"192.168.8.1", 9669},
    {"192.168.8.2", 9669},
}

config := nebula.PoolConfig{
    MaxConnPoolSize: 10,
}

pool, err := nebula.NewConnectionPool(hostAddress, config)
if err != nil {
    log.Fatal(err)
}

session, err := pool.GetSession("root", "nebula")
if err != nil {
    log.Fatal(err)
}

result, err := session.Execute("USE my_graph; MATCH (v:person) RETURN v LIMIT 10")
```

### 查询语言 nGQL

nGQL 是 NebulaGraph 的查询语言，类似 SQL 但专为图数据设计：

#### 基本查询

```sql
-- 查找点
MATCH (v:person {name: "Tom"}) RETURN v;

-- 查找关系
MATCH (v1:person)-[e:follows]->(v2:person) 
WHERE v1.name = "Tom" 
RETURN v2.name, e.degree;

-- 路径查询
MATCH p = (v1:person {name: "Tom"})-[e:follows*1..3]->(v2:person) 
RETURN p;
```

#### 图算法

```sql
-- 最短路径
FIND SHORTEST PATH FROM "person1" TO "person2" OVER follows;

-- K跳查询
GO 2 STEPS FROM "person1" OVER follows;

-- 社区发现
CALL louvain("my_graph", "follows");
```

## 最佳实践

### 数据建模

设计高效的图数据模型需要遵循以下原则：

1. **明确业务问题**：先确定要解决的问题，再设计数据模型。
2. **选择合适的点和边**：将核心实体设计为点，将关系设计为边。
3. **属性分配**：决定哪些信息作为点/边的属性，哪些作为独立的点。
4. **考虑查询模式**：根据常见查询优化数据模型，如添加冗余边以减少跳数。

![数据建模](https://docs.nebula-graph.com.cn/master/figs/data-modeling.png)

### 性能优化

#### 索引使用

```sql
-- 创建合适的索引
CREATE TAG INDEX person_name_index ON person(name);

-- 重建索引
REBUILD TAG INDEX person_name_index;

-- 使用索引查询
LOOKUP ON person WHERE person.name == "Tom";
```

#### 查询优化

1. **使用 EXPLAIN 分析查询计划**

```sql
EXPLAIN MATCH (v:person)-[e:follows]->(v2:person) WHERE v.name == "Tom" RETURN v2;
```

2. **限制结果集大小**

```sql
-- 使用 LIMIT 子句
MATCH (v:person) RETURN v LIMIT 100;

-- 使用 WHERE 子句过滤
MATCH (v:person) WHERE v.age > 20 RETURN v;
```

3. **避免全图扫描**

```sql
-- 不推荐
MATCH (v) RETURN v;

-- 推荐
MATCH (v:person) WHERE v.name == "Tom" RETURN v;
```

### 高可用部署

为确保 NebulaGraph 的高可用性，建议：

1. **多副本配置**：设置 `replica_factor` 为 3 或更高。
2. **服务分离**：将 Meta、Storage 和 Graph 服务部署在不同的机器上。
3. **负载均衡**：在 Graph 服务前配置负载均衡器。
4. **监控告警**：使用 NebulaGraph Dashboard 监控集群状态。

### 数据备份与恢复

#### 使用 Nebula Backup 工具

```bash
# 全量备份
$ nebula-backup backup --meta=192.168.8.1:9559 --backup_dir=/data/backup

# 增量备份
$ nebula-backup backup --meta=192.168.8.1:9559 --backup_dir=/data/backup --incremental

# 恢复数据
$ nebula-backup restore --meta=192.168.8.1:9559 --backup_dir=/data/backup
```

### 与大数据组件集成

#### Spark 集成

```scala
// 读取 NebulaGraph 数据到 Spark
val config = NebulaConnectionConfig
  .builder()
  .withMetaAddress("192.168.8.1:9559")
  .withGraphAddress("192.168.8.1:9669")
  .build()

val nebulaDF = spark.read.nebula(config, "my_graph")
  .loadVerticesToDF("person")

// 处理数据
val resultDF = nebulaDF.filter("age > 30")

// 写回 NebulaGraph
resultDF.write
  .nebula(config, "my_graph")
  .writeVertices("person_filtered", Seq("id"), Seq("name", "age"))
```

#### Flink 集成

```java
// 配置 NebulaGraph 连接
NebulaConnectionConfig config = NebulaConnectionConfig.builder()
    .withMetaAddress("192.168.8.1:9559")
    .withGraphAddress("192.168.8.1:9669")
    .build();

// 创建 Flink Source
NebulaSourceFunction<Row> source = NebulaSourceFunction.builder()
    .withConfig(config)
    .withSpace("my_graph")
    .withTag("person")
    .withFields(Arrays.asList("name", "age"))
    .build();

// 创建 Flink Sink
NebulaSinkFunction<Row> sink = NebulaSinkFunction.builder()
    .withConfig(config)
    .withSpace("my_graph")
    .withTag("person_processed")
    .withFields(Arrays.asList("name", "age"))
    .build();

// 处理数据
env.addSource(source)
   .map(/* 转换逻辑 */)
   .addSink(sink);
```

## 常见问题解决

### 连接问题

如果无法连接到 NebulaGraph 服务：

1. 检查服务状态：`systemctl status nebula-graphd`
2. 检查防火墙设置：确保端口（9669、9559、9779）已开放
3. 检查配置文件中的 IP 和端口设置
4. 检查客户端版本是否与服务端兼容

### 性能问题

如果查询性能不佳：

1. 检查是否创建了合适的索引
2. 使用 `EXPLAIN` 分析查询计划
3. 优化查询语句，避免全图扫描
4. 检查集群资源使用情况，考虑扩容

### 数据导入失败

如果数据导入失败：

1. 检查数据格式是否符合要求
2. 检查 Schema 是否正确创建
3. 检查空间的 `vid_type` 设置是否与导入数据匹配
4. 增加导入工具的日志级别，查看详细错误信息

## 总结

NebulaGraph 是一款功能强大的分布式图数据库，适用于处理复杂关联数据的场景。通过本指南，您应该已经了解了 NebulaGraph 的基本概念、使用流程、开发方法和最佳实践。在实际应用中，建议根据具体业务需求和数据特点，灵活调整配置和使用策略，以充分发挥 NebulaGraph 的性能优势。

随着对图数据库的深入使用，您可能需要进一步探索 NebulaGraph 的高级特性，如图算法、存储过程、企业级安全特性等。请参考 [NebulaGraph 官方文档](https://docs.nebula-graph.com.cn/) 获取更详细的信息和最新更新。