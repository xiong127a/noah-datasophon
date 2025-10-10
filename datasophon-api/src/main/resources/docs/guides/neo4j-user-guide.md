# Neo4j 用户指南

## 快速入门

本指南将帮助你在 DataSophon 平台上快速部署、配置和使用 Neo4j 图数据库，包括基本管理操作和应用开发最佳实践。

## 前置条件

在开始之前，请确保满足以下条件：

- DataSophon 平台已成功安装并正常运行
- 集群节点之间网络连接正常
- 至少有一台服务器满足 Neo4j 的最低硬件要求：
  - CPU: 4核以上
  - 内存: 8GB以上
  - 存储: 20GB以上SSD存储
- 已安装 JDK 11 或更高版本
- 已开放相关端口（7474, 7687）

## 部署流程

### 通过 DataSophon 平台部署

1. 登录 DataSophon 管理平台
2. 进入【组件管理】页面
3. 点击【添加服务】，在组件列表中选择 Neo4j
4. 按照向导完成配置：
   - 选择安装节点
   - 设置部署目录
   - 配置基本参数
   - 设置管理员账户密码
   - 配置高级参数（可选）
5. 确认配置无误后，点击【部署】
6. 等待部署完成，可在【服务状态】查看部署进度

### 配置参数说明

#### 基本配置

| 参数名称 | 说明 | 推荐值 |
|---------|------|-------|
| dbms.memory.heap.initial_size | 初始堆内存大小 | 1G |
| dbms.memory.heap.max_size | 最大堆内存大小 | 4G |
| dbms.memory.pagecache.size | 页缓存大小 | 2G |
| dbms.default_listen_address | 监听地址 | 0.0.0.0 |
| dbms.default_database | 默认数据库名称 | neo4j |
| dbms.security.auth_enabled | 启用认证 | true |

#### 高级配置

| 参数名称 | 说明 | 推荐值 |
|---------|------|-------|
| dbms.transaction.timeout | 事务超时时间 | 60s |
| dbms.security.procedures.whitelist | 允许执行的过程白名单 | apoc.*, gds.* |
| dbms.directories.logs | 日志文件目录 | /opt/neo4j/logs |
| dbms.directories.data | 数据文件目录 | /opt/neo4j/data |
| dbms.connector.bolt.listen_address | Bolt协议监听地址 | :7687 |
| dbms.connector.http.listen_address | HTTP协议监听地址 | :7474 |

### 部署后初始化

Neo4j 部署完成后，需要进行以下初始化操作：

1. 确认服务状态：
   ```bash
   systemctl status neo4j
   ```

2. 验证服务可访问性，使用浏览器访问 `http://<neo4j_host>:7474`

3. 使用默认凭据登录（如果未更改）：
   - 用户名: neo4j
   - 密码: neo4j
   
4. 首次登录时会要求更改密码

5. 安装常用扩展：
   ```cypher
   CALL dbms.procedures.installPlugin('<plugin-jar-file-url>');
   ```

## 基本操作指南

### 图数据模型设计

在 Neo4j 中设计数据模型时，应遵循以下最佳实践：

#### 节点和关系设计

- **节点代表实体**：使用节点表示域模型中的主要实体（如用户、产品、位置等）
- **关系代表连接**：关系应表示实体之间的连接或交互
- **使用有意义的关系类型**：关系类型应清晰表达实体之间的联系（如 FOLLOWS、PURCHASED、LOCATED_IN）
- **属性存储数据**：节点和关系上的属性用于存储相关数据
- **使用合适的标签**：标签用于对节点分类和分组，便于查询

#### 数据模型示例

社交网络数据模型：

```cypher
// 创建用户节点
CREATE (:Person {name: "张三", age: 28, bio: "工程师"})
CREATE (:Person {name: "李四", age: 32, bio: "设计师"})

// 创建关系
MATCH (a:Person {name: "张三"}), (b:Person {name: "李四"})
CREATE (a)-[:FOLLOWS {since: "2020-01-01"}]->(b)
```

电子商务数据模型：

```cypher
// 创建产品和类别
CREATE (:Category {name: "电子产品"})
CREATE (:Product {name: "智能手机", price: 3999, sku: "SP-001"})

// 建立关系
MATCH (c:Category {name: "电子产品"}), (p:Product {name: "智能手机"})
CREATE (p)-[:BELONGS_TO]->(c)
```

### 数据导入

Neo4j 提供多种数据导入方式，适合不同的数据量和场景：

#### 使用 Cypher 手动导入

适合小量数据：

```cypher
CREATE (n:Person {name: "王五", age: 25})
```

#### 从 CSV 文件导入

适合中等数据量：

```cypher
// 导入节点
LOAD CSV WITH HEADERS FROM "file:///persons.csv" AS line
CREATE (:Person {id: line.id, name: line.name, age: toInteger(line.age)})

// 导入关系
LOAD CSV WITH HEADERS FROM "file:///friendships.csv" AS line
MATCH (a:Person {id: line.person1_id}), (b:Person {id: line.person2_id})
CREATE (a)-[:FRIEND {since: line.since}]->(b)
```

#### 使用 neo4j-admin 工具批量导入

适合大量数据（数百万到数十亿节点）：

   ```bash
neo4j-admin import --nodes=persons.csv --relationships=friendships.csv
```

#### 使用 APOC 扩展导入

从 JSON、XML、MongoDB 或关系数据库导入：

```cypher
// 从 JSON 导入
CALL apoc.load.json("file:///data.json") YIELD value
MERGE (p:Person {id: value.id})
SET p.name = value.name, p.age = value.age
```

### 索引管理

正确的索引策略对 Neo4j 性能至关重要：

#### 创建索引

```cypher
// 单属性索引
CREATE INDEX person_name FOR (n:Person) ON (n.name)

// 复合索引
CREATE INDEX product_details FOR (n:Product) ON (n.name, n.sku)

// 全文索引
CREATE FULLTEXT INDEX product_search FOR (n:Product) ON EACH [n.name, n.description]
```

#### 查看索引

```cypher
SHOW INDEXES
```

#### 删除索引

   ```cypher
DROP INDEX person_name
```

### 约束管理

约束帮助维护数据完整性：

#### 唯一性约束

```cypher
CREATE CONSTRAINT person_id_unique FOR (n:Person) REQUIRE n.id IS UNIQUE
```

#### 存在性约束

   ```cypher
CREATE CONSTRAINT person_name_exists FOR (n:Person) REQUIRE n.name IS NOT NULL
   ```

#### 节点键约束

```cypher
CREATE CONSTRAINT product_key FOR (n:Product) REQUIRE (n.sku, n.vendor) IS NODE KEY
```

### 基本查询操作

#### 节点查询

```cypher
// 查找所有人
MATCH (p:Person) RETURN p

// 条件过滤
MATCH (p:Person) WHERE p.age > 30 RETURN p.name, p.age

// 查询单个节点
MATCH (p:Person {name: "张三"}) RETURN p
```

#### 关系查询

```cypher
// 查找张三的所有朋友
MATCH (p:Person {name: "张三"})-[:FRIEND]->(friend) RETURN friend.name

// 双向关系
MATCH (p:Person {name: "张三"})-[:FRIEND]-(friend) RETURN friend.name

// 多跳关系
MATCH (p:Person {name: "张三"})-[:FRIEND*1..3]->(fof) RETURN DISTINCT fof.name
```

#### 路径查询

```cypher
// 最短路径
MATCH path = shortestPath((a:Person {name: "张三"})-[*]-(b:Person {name: "王五"}))
RETURN path

// 所有路径
MATCH path = (a:Person {name: "张三"})-[*..3]-(b:Person {name: "王五"})
RETURN path
```

#### 聚合查询

```cypher
// 计数
MATCH (p:Person) RETURN count(p)

// 分组
MATCH (p:Product)-[:BELONGS_TO]->(c:Category)
RETURN c.name, count(p) AS product_count, avg(p.price) AS avg_price
ORDER BY product_count DESC
```

### 数据修改操作

#### 更新节点属性

```cypher
MATCH (p:Person {name: "张三"})
SET p.age = 29, p.title = "高级工程师"
RETURN p
```

#### 添加新关系

```cypher
MATCH (a:Person {name: "张三"}), (b:Person {name: "王五"})
CREATE (a)-[:WORKS_WITH {project: "DataSophon", since: "2023-01-01"}]->(b)
```

#### 删除关系

```cypher
MATCH (a:Person {name: "张三"})-[r:FRIEND]->(b:Person {name: "李四"})
DELETE r
```

#### 删除节点和关系

```cypher
// 删除节点及其所有关系
MATCH (p:Person {name: "王五"})
DETACH DELETE p
```

### 数据备份与恢复

保护 Neo4j 数据安全的关键操作：

#### 创建备份

```bash
neo4j-admin dump --database=neo4j --to=/backups/neo4j-backup.dump
```

#### 恢复备份

```bash
neo4j-admin load --from=/backups/neo4j-backup.dump --database=neo4j --force
```

#### 设置定时备份

可以配置 cron 任务来自动执行备份：

```bash
# 每天凌晨 2 点执行备份
0 2 * * * /opt/neo4j/bin/neo4j-admin dump --database=neo4j --to=/backups/neo4j-$(date +\%Y\%m\%d).dump
```

## 开发指南

### 驱动程序连接

Neo4j 支持多种编程语言的官方驱动，以下是常见语言的连接示例：

#### Java 连接示例

```java
import org.neo4j.driver.*;

public class Neo4jExample {
    public static void main(String[] args) {
        Driver driver = GraphDatabase.driver("bolt://neo4j-server:7687", 
                             AuthTokens.basic("neo4j", "password"));
        
        try (Session session = driver.session()) {
            String result = session.writeTransaction(tx -> {
                Result queryResult = tx.run("CREATE (n:Person {name: $name}) RETURN n.name",
                                        Values.parameters("name", "张三"));
                return queryResult.single().get(0).asString();
            });
            
            System.out.println(result);
        }
        
        driver.close();
    }
}
```

#### Python 连接示例

```python
from neo4j import GraphDatabase

class Neo4jExample:
    def __init__(self, uri, user, password):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))
        
    def close(self):
        self.driver.close()
        
    def create_person(self, name):
        with self.driver.session() as session:
            result = session.write_transaction(self._create_person, name)
            return result
            
    @staticmethod
    def _create_person(tx, name):
        result = tx.run("CREATE (n:Person {name: $name}) RETURN n.name", name=name)
        return result.single()[0]
        
if __name__ == "__main__":
    example = Neo4jExample("bolt://neo4j-server:7687", "neo4j", "password")
    name = example.create_person("李四")
    print(name)
    example.close()
```

#### JavaScript (Node.js) 连接示例

```javascript
const neo4j = require('neo4j-driver');

const driver = neo4j.driver(
    'bolt://neo4j-server:7687',
    neo4j.auth.basic('neo4j', 'password')
);

const session = driver.session();

session.run('CREATE (n:Person {name: $name}) RETURN n.name', {name: '王五'})
    .then(result => {
        const singleRecord = result.records[0];
        const name = singleRecord.get(0);
        console.log(name);
    })
    .catch(error => {
        console.error(error);
    })
    .finally(() => {
        session.close();
        driver.close();
    });
```

### 事务管理

在应用程序中正确管理 Neo4j 事务非常重要：

#### 显式事务

```java
// Java 示例
try (Session session = driver.session()) {
    // 开始一个显式事务
    Transaction tx = session.beginTransaction();
    try {
        // 执行多个操作
        tx.run("CREATE (p:Product {name: $name, price: $price})", 
               Values.parameters("name", "笔记本电脑", "price", 5999));
        tx.run("CREATE (c:Category {name: $name})", 
               Values.parameters("name", "电子产品"));
        tx.run("MATCH (p:Product {name: $product}), (c:Category {name: $category}) " +
               "CREATE (p)-[:BELONGS_TO]->(c)",
               Values.parameters("product", "笔记本电脑", "category", "电子产品"));
               
        // 提交事务
        tx.commit();
    } catch (Exception e) {
        // 出现异常时回滚事务
        tx.rollback();
        throw e;
    }
}
```

#### 自动事务函数

```java
// Java 示例
try (Session session = driver.session()) {
    // 写事务
    session.writeTransaction(tx -> {
        tx.run("CREATE (p:Person {name: $name})", 
               Values.parameters("name", "张三"));
        return null;
    });
    
    // 读事务
    List<String> names = session.readTransaction(tx -> {
        Result result = tx.run("MATCH (p:Person) RETURN p.name");
        List<String> nameList = new ArrayList<>();
        while (result.hasNext()) {
            nameList.add(result.next().get(0).asString());
        }
        return nameList;
    });
}
```

### 查询优化

高效的 Cypher 查询对性能至关重要：

#### 使用 EXPLAIN 和 PROFILE

分析查询执行计划：

```cypher
// 查看执行计划
EXPLAIN MATCH (p:Person)-[:FRIEND]->(f) WHERE p.name = "张三" RETURN f.name

// 分析实际执行情况
PROFILE MATCH (p:Person)-[:FRIEND]->(f) WHERE p.name = "张三" RETURN f.name
```

#### 优化技巧

- **使用参数化查询**：防止 Cypher 注入并允许查询计划缓存
- **正确使用索引**：确保查询条件使用已建立索引的属性
- **限制结果集大小**：使用 LIMIT 子句避免返回过多结果
- **避免 OPTIONAL MATCH 滥用**：可能导致笛卡尔积爆炸
- **使用 WHERE 子句代替 AND**：提高可读性和优化机会
- **合理设置查询超时**：防止长时间运行的查询消耗资源

   ```cypher
// 优化前
MATCH (p:Person)
WHERE p.name = "张三" AND p.age > 25
RETURN p

// 优化后
MATCH (p:Person {name: "张三"})
WHERE p.age > 25
RETURN p
```

### 常用扩展库

Neo4j 生态系统包含强大的扩展库：

#### APOC (Awesome Procedures On Cypher)

安装 APOC：

1. 下载与 Neo4j 版本匹配的 APOC 库
2. 将 JAR 文件放入 Neo4j 插件目录 `$NEO4J_HOME/plugins/`
3. 在 `neo4j.conf` 中添加配置：
   ```
   dbms.security.procedures.unrestricted=apoc.*
   ```
4. 重启 Neo4j

常用 APOC 过程：

   ```cypher
// 数据导入
CALL apoc.load.json("https://api.example.com/data")

// 图算法
CALL apoc.path.expandConfig()

// 数据转换
CALL apoc.convert.toJson()

// 触发器
CALL apoc.trigger.add()
```

#### 图数据科学库 (GDS)

安装图数据科学库：

1. 下载与 Neo4j 版本匹配的 GDS 库
2. 将 JAR 文件放入 Neo4j 插件目录
3. 在 `neo4j.conf` 中添加配置：
   ```
   dbms.security.procedures.unrestricted=gds.*
   ```
4. 重启 Neo4j

常用图算法：

```cypher
// 社区检测
CALL gds.louvain.stream('myGraph')

// 中心性算法
CALL gds.pageRank.stream('myGraph')

// 路径查找
CALL gds.shortestPath.dijkstra.stream('myGraph')

// 相似度计算
CALL gds.nodeSimilarity.stream('myGraph')
```

## 最佳实践

### 数据模型设计最佳实践

- **避免过度规范化**：图数据库中，适当的数据冗余有时比严格规范化更高效
- **正确选择节点与关系**：不是所有概念都应建模为节点，有时使用属性或关系更合适
- **使用有意义的标签体系**：设计清晰的标签层次，便于查询和索引
- **关系方向要一致**：保持关系方向的一致性，避免查询中频繁使用双向关系
- **属性命名规范**：采用一致的属性命名方案，如驼峰命名法或下划线分隔

### 性能优化最佳实践

- **合理配置内存**：根据数据规模和查询模式调整堆内存和页缓存大小
- **定期更新统计信息**：执行 `CALL db.stats.clear()` 刷新查询优化器统计信息
- **使用批处理**：处理大量数据时使用批处理，避免单个大事务
- **查询预热**：对关键查询进行预热，确保缓存包含常用数据
- **定期维护索引**：重建性能下降的索引，删除不再使用的索引

### 安全最佳实践

- **启用身份验证**：永远不要在生产环境中禁用认证
- **实施细粒度访问控制**：为不同用户组创建角色和权限
- **定期修改密码**：遵循安全密码策略，定期更改管理员密码
- **限制网络访问**：使用防火墙规则限制 Neo4j 端口访问
- **保护密钥表文件**：安全存储用于 TLS 的证书和密钥

### 备份与恢复最佳实践

- **定时自动备份**：配置自动化备份流程
- **异地存储备份**：将备份副本存储在不同物理位置
- **测试恢复过程**：定期测试备份恢复流程，确保可用性
- **监控备份状态**：设置备份失败告警
- **记录版本信息**：备份时保存 Neo4j 版本信息，避免兼容性问题

## 故障排除

### 常见问题及解决方案

#### 连接问题

问题：无法连接到 Neo4j 服务器

排查步骤：
1. 检查 Neo4j 服务状态：`systemctl status neo4j`
2. 确认网络连接：`telnet neo4j-server 7687`
3. 查看配置文件监听设置：检查 `neo4j.conf` 中的 `dbms.default_listen_address`
4. 检查防火墙规则：`iptables -L`

解决方案：
- 启动服务：`systemctl start neo4j`
- 修改监听地址：设置 `dbms.default_listen_address=0.0.0.0`
- 开放防火墙端口：`firewall-cmd --permanent --add-port=7474/tcp`
- 检查日志文件：`cat /var/log/neo4j/neo4j.log`

#### 内存问题

问题：服务启动失败，日志显示内存不足

排查步骤：
1. 检查系统内存：`free -h`
2. 查看 Neo4j 内存配置：检查 `neo4j.conf` 中的堆内存和页缓存设置
3. 检查日志中的 OOM 错误：`grep "OutOfMemoryError" /var/log/neo4j/neo4j.log`

解决方案：
- 减小内存配置：降低 `dbms.memory.heap.max_size` 和 `dbms.memory.pagecache.size`
- 增加系统内存：升级服务器内存或添加交换空间
- 优化查询：减少高内存消耗的查询

#### 查询超时

问题：长时间运行的查询超时失败

排查步骤：
1. 查看查询超时设置：检查 `neo4j.conf` 中的 `dbms.transaction.timeout`
2. 使用 PROFILE 分析查询：找出耗时操作
3. 检查索引使用情况：确认查询使用了合适的索引

解决方案：
- 增加超时设置：调整 `dbms.transaction.timeout`
- 添加必要索引：为查询条件创建索引
- 重写查询：分解复杂查询为多个简单查询
- 使用 LIMIT 子句：限制中间结果集大小

#### 磁盘空间不足

问题：Neo4j 报错磁盘空间不足

排查步骤：
1. 检查磁盘空间：`df -h`
2. 查看数据目录大小：`du -sh /opt/neo4j/data`
3. 检查日志目录大小：`du -sh /opt/neo4j/logs`
4. 检查事务日志：`du -sh /opt/neo4j/data/databases/*/transactions`

解决方案：
- 清理旧备份：删除不需要的备份文件
- 配置日志轮转：设置日志文件自动轮转和删除
- 扩展磁盘空间：增加存储容量
- 清理事务日志：执行数据库检查点或备份后清理

### 日志分析

Neo4j 日志位置和重要日志说明：

- 默认日志路径：`/opt/neo4j/logs/`
- 主要日志文件：`neo4j.log`
- 查询日志：`query.log`（需要单独启用）
- 安全日志：`security.log`（需要单独启用）

启用详细查询日志：

1. 编辑 `neo4j.conf`：
   ```
   dbms.logs.query.enabled=true
   dbms.logs.query.threshold=0 # 记录所有查询
   dbms.logs.query.time_logging_enabled=true # 记录查询时间
   dbms.logs.query.parameter_logging_enabled=true # 记录参数
   ```

2. 重启 Neo4j 服务

分析慢查询：

   ```bash
grep "ms: " /opt/neo4j/logs/query.log | sort -k5 -n -r | head -10
```

### 性能监控

监控 Neo4j 性能的工具和方法：

#### 内置监控

查看数据库状态：

   ```cypher
CALL dbms.queryJmx("org.neo4j:*")
   ```

查看长时间运行的查询：

   ```cypher
CALL dbms.listQueries()
```

终止长时间运行的查询：

   ```cypher
CALL dbms.killQuery("query-id")
```

#### 第三方监控

- **Prometheus + Grafana**：设置 Neo4j Prometheus 导出器监控指标
- **Neo4j 监控与管理（企业版功能）**：提供图形化监控界面
- **Halin**：开源 Neo4j 监控工具

#### 关键监控指标

- **Page Cache 命中率**：理想值 > 95%
- **GC 暂停时间**：应该最小化
- **查询执行时间**：监控慢查询趋势
- **内存使用情况**：堆内存和页缓存使用率
- **事务吞吐量**：每秒事务数
- **连接数**：活动连接数

## 总结

本指南详细介绍了在 DataSophon 平台上部署、配置和使用 Neo4j 图数据库的关键步骤和最佳实践。Neo4j 作为领先的图数据库，为处理高度互联数据提供了强大的解决方案，特别适合关系密集型应用场景。

通过遵循本指南中的建议，你可以充分利用 Neo4j 的强大功能，构建高效的图数据应用。从基本的 CRUD 操作到高级的图算法和优化技术，Neo4j 提供了丰富的工具集，帮助开发者解决复杂的数据关系问题。

随着数据关系复杂性的不断增加，Neo4j 在大数据平台中的作用将越来越重要。持续学习和探索 Neo4j 的新特性和最佳实践，将帮助你在数据密集型应用开发中保持竞争优势。