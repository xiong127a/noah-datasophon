# Redis 用户指南

## 基本介绍

本文档是Redis的用户指南，提供了Redis的安装、配置、基本命令使用以及常见应用场景的实践指导。通过本指南，您将学会如何正确地设置和使用Redis，充分利用其高性能的内存数据存储能力。

## 环境要求

在安装Redis之前，请确保您的系统满足以下要求：

- 支持的操作系统：Linux、macOS、Windows(通过WSL或官方Windows包)
- 内存要求：取决于数据规模，但建议至少1GB
- 处理器要求：无特殊要求，但多核心可提高性能
- 硬盘空间：至少100MB用于安装，加上数据持久化所需空间
- 网络：如需远程访问，确保防火墙开放6379端口(默认)

## 安装指南

### Linux系统安装

#### 通过包管理器安装(推荐)

**Ubuntu/Debian:**

```bash
sudo apt update
sudo apt install redis-server
```

**CentOS/RHEL:**

```bash
sudo yum install epel-release
sudo yum install redis
```

#### 源码编译安装

如需最新版本或自定义配置，可以从源码编译：

```bash
wget http://download.redis.io/redis-stable.tar.gz
tar xzf redis-stable.tar.gz
cd redis-stable
make
sudo make install
```

### macOS安装

使用Homebrew安装：

```bash
brew install redis
```

### Windows安装

Windows用户可以通过以下方式使用Redis：

1. **WSL(Windows Subsystem for Linux)**：在WSL中按Linux方式安装
2. **官方Windows版**：从[https://github.com/microsoftarchive/redis/releases](https://github.com/microsoftarchive/redis/releases)下载MSI安装包

### Docker容器安装

使用Docker可以快速部署Redis：

```bash
docker pull redis
docker run --name my-redis -d -p 6379:6379 redis
```

## 基本配置

Redis的配置文件通常位于`/etc/redis/redis.conf`(Linux)或`/usr/local/etc/redis.conf`(macOS)。以下是一些重要的配置项：

### 网络配置

```
# 绑定地址，默认只允许本地访问
bind 127.0.0.1
# 端口号
port 6379
# 客户端空闲多久后关闭连接(秒)
timeout 300
# TCP keepalive检测时间
tcp-keepalive 60
```

### 内存管理

```
# 最大内存限制
maxmemory 2gb
# 内存策略：当达到最大内存时如何处理
# noeviction(返回错误), allkeys-lru(淘汰最久未使用), volatile-lru(淘汰设置了过期时间的最久未使用)等
maxmemory-policy allkeys-lru
```

### 持久化配置

**RDB配置:**

```
# 900秒内如果有1个key变更，则保存
save 900 1
# 300秒内如果有10个key变更，则保存
save 300 10
# 60秒内如果有10000个key变更，则保存
save 60 10000
# RDB文件名
dbfilename dump.rdb
# RDB文件保存目录
dir /var/lib/redis
```

**AOF配置:**

```
# 是否开启AOF
appendonly yes
# AOF文件名
appendfilename "appendonly.aof"
# 同步策略: always(每次写入), everysec(每秒), no(由操作系统决定)
appendfsync everysec
```

### 安全配置

```
# 设置访问密码
requirepass yourpassword
# 禁用危险命令
rename-command FLUSHALL ""
rename-command FLUSHDB ""
rename-command CONFIG ""
```

## 启动与停止

### 启动Redis服务

**Linux系统服务方式:**

```bash
sudo systemctl start redis   # 使用systemd
sudo service redis-server start   # 使用SysV init
```

**直接启动:**

```bash
redis-server /path/to/redis.conf
```

**后台运行:**

```bash
redis-server --daemonize yes
```

### 停止Redis服务

**系统服务方式:**

```bash
sudo systemctl stop redis
sudo service redis-server stop
```

**客户端方式:**

```bash
redis-cli shutdown
```

### 验证服务状态

```bash
redis-cli ping
```

如果返回`PONG`，表示服务已成功启动。

## 基本操作指南

### 连接Redis

**本地连接:**

```bash
redis-cli
```

**远程连接:**

```bash
redis-cli -h host -p port -a password
```

### 数据类型与基本命令

#### 字符串操作

```
# 设置键值
SET key value
# 获取值
GET key
# 删除键
DEL key
# 检查键是否存在
EXISTS key
# 设置过期时间(秒)
EXPIRE key seconds
# 获取剩余过期时间
TTL key
```

#### 哈希表操作

```
# 设置哈希表字段
HSET key field value
# 获取哈希表字段
HGET key field
# 获取所有字段和值
HGETALL key
# 删除字段
HDEL key field
# 增加字段值(数字)
HINCRBY key field increment
```

#### 列表操作

```
# 左侧添加元素
LPUSH key value [value ...]
# 右侧添加元素
RPUSH key value [value ...]
# 左侧弹出元素
LPOP key
# 获取范围内元素
LRANGE key start stop
# 获取列表长度
LLEN key
```

#### 集合操作

```
# 添加成员
SADD key member [member ...]
# 移除成员
SREM key member [member ...]
# 判断是否为成员
SISMEMBER key member
# 获取所有成员
SMEMBERS key
# 获取集合交集
SINTER key [key ...]
# 获取集合并集
SUNION key [key ...]
```

#### 有序集合操作

```
# 添加成员和分数
ZADD key score member [score member ...]
# 获取排名范围的成员
ZRANGE key start stop [WITHSCORES]
# 获取分数范围的成员
ZRANGEBYSCORE key min max [WITHSCORES]
# 增加成员分数
ZINCRBY key increment member
# 获取成员排名
ZRANK key member
```

### 事务操作

Redis事务允许一次执行多个命令：

```
# 开始事务
MULTI
# 命令入队
SET key1 value1
SET key2 value2
# 执行事务
EXEC
# 取消事务
DISCARD
```

### 发布/订阅

```
# 订阅频道
SUBSCRIBE channel [channel ...]
# 发布消息
PUBLISH channel message
# 取消订阅
UNSUBSCRIBE [channel [channel ...]]
# 按模式订阅
PSUBSCRIBE pattern [pattern ...]
```

## 进阶应用

### 使用Redis实现缓存

以下是用Redis实现简单缓存的模式：

```
# 检查缓存是否存在
EXISTS cache:user:123
# 如果不存在，从数据库获取并缓存
SET cache:user:123 '{"name":"John","email":"john@example.com"}'
# 设置过期时间(3600秒)
EXPIRE cache:user:123 3600
```

### 实现计数器

使用Redis实现访问计数器：

```
# 增加计数
INCR page:visits
# 获取计数
GET page:visits
# 增加指定值
INCRBY user:123:credits 50
```

### 限速器实现

API请求限速器示例：

```
# 记录用户请求
INCR user:123:requests
# 设置过期时间(60秒后重置)
EXPIRE user:123:requests 60
# 检查是否超限
GET user:123:requests
```

### 会话存储

Redis适合存储会话数据：

```
# 存储会话
HMSET session:abc123 user_id 456 login_time 1634567890 data '{"cart":[1,2,3]}'
# 设置会话过期
EXPIRE session:abc123 1800
# 获取会话数据
HGETALL session:abc123
```

### 任务队列

使用列表实现简单任务队列：

```
# 生产者：添加任务
LPUSH tasks '{"id":1,"action":"process_order","data":{"order_id":12345}}'
# 消费者：获取任务(阻塞式等待)
BRPOP tasks 30
```

### 排行榜

使用有序集合实现排行榜：

```
# 更新分数
ZADD leaderboard 1000 user:123
# 获取前10名
ZREVRANGE leaderboard 0 9 WITHSCORES
# 获取用户排名
ZREVRANK leaderboard user:123
```

## 高可用配置

### 配置主从复制

1. 配置从服务器(在redis.conf中)：

```
# 指定主服务器
replicaof 192.168.1.100 6379
# 如果主服务器有密码
masterauth password
```

2. 或者通过命令行动态配置：

```
SLAVEOF 192.168.1.100 6379
```

3. 检查复制状态：

```
INFO replication
```

### 配置Sentinel

1. 创建sentinel.conf：

```
sentinel monitor mymaster 192.168.1.100 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
sentinel auth-pass mymaster password
```

2. 启动Sentinel：

```bash
redis-sentinel /path/to/sentinel.conf
```

### 配置Redis Cluster

1. 配置各节点的redis.conf：

```
port 7000
cluster-enabled yes
cluster-config-file nodes-7000.conf
cluster-node-timeout 5000
appendonly yes
```

2. 启动各节点：

```bash
redis-server /path/to/redis-7000.conf
redis-server /path/to/redis-7001.conf
# ... 更多节点
```

3. 创建集群：

```bash
redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 --cluster-replicas 1
```

## 客户端开发

### 常用客户端库

不同编程语言的Redis客户端：

- **Java**: Jedis, Lettuce, Redisson
- **Python**: redis-py
- **Node.js**: node-redis, ioredis
- **PHP**: phpredis, predis
- **Ruby**: redis-rb
- **Go**: go-redis, redigo
- **C#/.NET**: StackExchange.Redis

### Java应用示例

使用Jedis连接Redis并执行基本操作：

```java
import redis.clients.jedis.Jedis;

public class RedisExample {
    public static void main(String[] args) {
        // 连接Redis
        Jedis jedis = new Jedis("localhost", 6379);
        
        // 可选：如果设置了密码
        // jedis.auth("password");
        
        // 设置键值
        jedis.set("key", "value");
        
        // 获取值
        String value = jedis.get("key");
        System.out.println(value);
        
        // 使用哈希表
        jedis.hset("user:123", "name", "John");
        jedis.hset("user:123", "email", "john@example.com");
        
        // 获取哈希表字段
        String name = jedis.hget("user:123", "name");
        System.out.println(name);
        
        // 关闭连接
        jedis.close();
    }
}
```

### Python应用示例

使用redis-py操作Redis：

```python
import redis

# 连接Redis
r = redis.Redis(host='localhost', port=6379, decode_responses=True)

# 设置值
r.set('key', 'value')

# 获取值
value = r.get('key')
print(value)

# 列表操作
r.lpush('tasks', 'task1', 'task2', 'task3')
tasks = r.lrange('tasks', 0, -1)
print(tasks)

# 哈希表操作
r.hset('user:123', mapping={
    'name': 'John',
    'email': 'john@example.com',
    'age': 30
})

user = r.hgetall('user:123')
print(user)
```

### Node.js应用示例

使用node-redis操作Redis：

```javascript
const redis = require('redis');

async function redisExample() {
    // 创建客户端
    const client = redis.createClient({
        url: 'redis://localhost:6379'
    });
    
    // 连接事件处理
    client.on('error', (err) => console.log('Redis Client Error', err));
    await client.connect();
    
    // 基本操作
    await client.set('key', 'value');
    const value = await client.get('key');
    console.log(value);
    
    // 哈希表操作
    await client.hSet('user:123', 'name', 'John');
    await client.hSet('user:123', 'email', 'john@example.com');
    
    const userData = await client.hGetAll('user:123');
    console.log(userData);
    
    // 断开连接
    await client.quit();
}

redisExample();
```

## 监控与维护

### 性能监控

1. **使用INFO命令**：提供服务器状态信息

```
redis-cli info
```

2. **监控特定指标**：

```
redis-cli info memory
redis-cli info stats
redis-cli info clients
```

3. **实时监控命令执行**：

```
redis-cli monitor
```

4. **慢查询日志**：

```
# 配置慢查询
CONFIG SET slowlog-log-slower-than 10000  # 微秒
CONFIG SET slowlog-max-len 128

# 获取慢查询
SLOWLOG GET 10
```

### 备份与恢复

**创建RDB备份**：

```bash
# 触发保存
redis-cli SAVE
# 后台保存
redis-cli BGSAVE
```

**备份文件**：

```bash
cp /var/lib/redis/dump.rdb /backup/redis_backup_$(date +%Y%m%d).rdb
```

**从RDB文件恢复**：

```bash
# 停止Redis
sudo systemctl stop redis

# 复制RDB文件到数据目录
cp /backup/redis_backup_file.rdb /var/lib/redis/dump.rdb

# 确保文件权限正确
chown redis:redis /var/lib/redis/dump.rdb

# 启动Redis
sudo systemctl start redis
```

### 日常维护任务

1. **清理过期键**：

```
# 检查过期键数量
redis-cli info keyspace

# 手动触发过期键清除
redis-cli SCAN 0 COUNT 1000
```

2. **优化内存使用**：

```
# 分析内存使用
redis-cli memory stats
redis-cli memory usage key
```

3. **大键扫描**：

```bash
redis-cli --bigkeys
```

4. **压缩AOF文件**：

```
redis-cli BGREWRITEAOF
```

## 最佳实践

### 性能优化

1. **合理使用数据结构**
   - 对于简单键值对，直接使用字符串
   - 对象数据使用哈希表，更节省内存
   - 大量成员的集合考虑使用整型ID而非字符串

2. **批量操作**
   - 使用MGET/MSET替代多个GET/SET
   - 使用管道(pipeline)减少网络往返
   - 合理使用事务减少操作次数

3. **键名设计**
   - 采用冒号分隔的命名约定，如`user:1000:profile`
   - 避免过长的键名
   - 根据数据类型使用统一前缀，如`cache:`、`session:`

4. **避免大键值**
   - 拆分大哈希表为多个小哈希表
   - 使用HSCAN代替HGETALL
   - 列表过大时考虑分片

### 安全建议

1. **网络安全**
   - 绑定特定IP地址
   - 使用防火墙限制访问
   - 启用加密传输(Redis 6.0+)

2. **访问控制**
   - 设置强密码
   - 使用ACL(Redis 6.0+)限制命令
   - 定期更改密码

3. **配置安全**
   - 禁用或重命名危险命令
   - 限制最大内存
   - 定期审计配置

### 生产环境部署

1. **高可用部署**
   - 使用主从复制保证数据安全
   - 配置Sentinel实现自动故障转移
   - 大规模场景使用Redis Cluster

2. **资源分配**
   - 为Redis分配足够内存
   - 考虑使用专用服务器
   - 设置合理的maxmemory-policy

3. **监控告警**
   - 监控内存使用率
   - 监控连接数
   - 监控命令执行延迟
   - 设置关键指标告警

## 常见问题与解决方案

### 内存问题

**问题**：Redis内存使用过高

**解决方案**：
- 配置最大内存限制
- 设置合适的淘汰策略
- 定期清理过期键
- 使用更内存高效的数据结构

### 性能问题

**问题**：Redis响应变慢

**解决方案**：
- 检查慢查询日志
- 避免使用耗时命令(如KEYS)
- 优化大键访问(使用SCAN代替)
- 检查持久化配置影响
- 配置合理的系统参数(vm.overcommit_memory)

### 连接问题

**问题**：无法连接Redis服务

**解决方案**：
- 检查Redis服务是否运行
- 验证IP绑定设置
- 检查密码配置
- 确认防火墙设置
- 验证最大客户端连接数

### 数据丢失问题

**问题**：Redis数据丢失

**解决方案**：
- 配置合适的持久化策略
- 使用主从复制保障数据安全
- 定期备份数据
- 调整AOF fsync策略
- 避免使用FLUSHALL/FLUSHDB命令

## 参考资源

- [Redis官方文档](https://redis.io/documentation)
- [Redis命令参考](https://redis.io/commands)
- [Redis最佳实践](https://redis.io/topics/clients)
- [Redis GitHub仓库](https://github.com/redis/redis)
- [Redis Stack介绍](https://redis.io/docs/stack/) 