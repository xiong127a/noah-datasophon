# Redis Sentinel 用户指南

## 基础介绍

本文档是Redis Sentinel的完整用户指南，旨在帮助用户快速理解、部署和配置Redis Sentinel高可用解决方案。Redis Sentinel为Redis提供了自动故障转移功能，确保在主节点故障时系统仍能保持可用。

## 环境准备

在部署Redis Sentinel之前，请确保您的环境满足以下要求：

- **操作系统**：支持Linux、macOS或Windows系统
- **Redis版本**：建议使用Redis 5.0或更高版本
- **服务器要求**：至少3台服务器（或虚拟机）用于部署Redis和Sentinel
- **网络要求**：所有节点之间网络互通，防火墙开放必要端口（默认为6379用于Redis，26379用于Sentinel）
- **存储要求**：根据数据量确定，通常Redis主节点和从节点需要相同的存储空间

## 安装部署

### 安装Redis

在每台需要部署Redis的服务器上执行以下步骤：

#### Linux系统（Ubuntu/Debian）

```bash
# 安装Redis
sudo apt update
sudo apt install redis-server
```

#### CentOS/RHEL

```bash
# 安装Redis
sudo yum install epel-release
sudo yum install redis
```

#### 源码编译安装

如需最新版本，可以从源码编译：

```bash
wget http://download.redis.io/redis-stable.tar.gz
tar xzf redis-stable.tar.gz
cd redis-stable
make
sudo make install
```

### 配置Redis主从复制

Sentinel需要监控Redis主从集群，因此首先需要配置Redis主从复制。假设我们有三台服务器：

- 192.168.1.100 - Redis主服务器
- 192.168.1.101 - Redis从服务器1
- 192.168.1.102 - Redis从服务器2

#### 主服务器配置 (192.168.1.100)

编辑`/etc/redis/redis.conf`或自定义配置文件：

```
# 绑定地址（可以根据实际情况修改）
bind 0.0.0.0
# 服务端口
port 6379
# 设置主服务器密码（可选，强烈推荐）
requirepass "your_redis_password"
# 从服务器连接主服务器的密码（与requirepass相同）
masterauth "your_redis_password"
# 确保日志和数据目录可写
dir /var/lib/redis
# 启用AOF持久化（推荐）
appendonly yes
```

#### 从服务器配置 (192.168.1.101 和 192.168.1.102)

在两台从服务器上，编辑`/etc/redis/redis.conf`：

```
# 绑定地址
bind 0.0.0.0
# 服务端口
port 6379
# 指定主服务器
replicaof 192.168.1.100 6379
# 主服务器密码
masterauth "your_redis_password"
# 设置从服务器密码（推荐与主服务器一致）
requirepass "your_redis_password"
# 数据目录
dir /var/lib/redis
# 启用AOF持久化
appendonly yes
```

#### 启动Redis服务

在所有服务器上启动Redis服务：

```bash
# 如果通过包管理器安装
sudo systemctl start redis-server

# 如果通过源码安装
redis-server /path/to/redis.conf
```

#### 验证主从复制

在任一服务器上执行：

```bash
# 连接Redis
redis-cli -h 192.168.1.100 -a "your_redis_password"

# 检查主从复制状态
info replication
```

正常情况下，应显示一个主服务器和两个从服务器。

### 安装和配置Sentinel

Redis Sentinel通常与Redis服务器分开部署，但也可以部署在同一服务器上。建议至少部署3个Sentinel实例，分布在不同的服务器上。

#### 创建Sentinel配置文件

在每台部署Sentinel的服务器上，创建`sentinel.conf`文件：

```
# 监听所有网络接口
bind 0.0.0.0
# 端口号
port 26379
# 后台运行
daemonize yes
# 日志文件
logfile "/var/log/redis/sentinel.log"
# 工作目录
dir "/var/lib/redis"
# 设置为1将禁止脚本执行覆盖其他重要脚本或配置
protected-mode no

# sentinel monitor <master-name> <ip> <port> <quorum>
# master-name: 主服务器名称，可自定义
# ip和port: 主服务器地址
# quorum: 判断主服务器客观下线所需的sentinel同意数量
sentinel monitor mymaster 192.168.1.100 6379 2

# 如果主服务器设置了密码，需要配置
sentinel auth-pass mymaster your_redis_password

# 多长时间（毫秒）无法连接主服务器，判定为主观下线
sentinel down-after-milliseconds mymaster 30000

# 故障转移超时时间（毫秒）
sentinel failover-timeout mymaster 180000

# 同时进行同步的从服务器数量
sentinel parallel-syncs mymaster 1
```

**重要说明**：
- `quorum`应该设置为Sentinel总数的大多数（如3个Sentinel设置为2）
- `down-after-milliseconds`设置为30秒或更多，避免网络抖动导致的误判
- `parallel-syncs`设置为1可以减少故障转移过程中的负载

#### 启动Sentinel

在每台需要运行Sentinel的服务器上执行：

```bash
# 通过包管理器安装的redis自带sentinel
redis-sentinel /path/to/sentinel.conf

# 或者使用redis-server启动sentinel模式
redis-server /path/to/sentinel.conf --sentinel
```

#### 验证Sentinel集群

```bash
# 连接到任一Sentinel
redis-cli -h <sentinel_host> -p 26379

# 获取当前主服务器信息
sentinel master mymaster

# 获取所有Sentinel信息
sentinel sentinels mymaster

# 获取所有从服务器信息
sentinel replicas mymaster
```

## 客户端配置

### Java客户端(Jedis)示例

```java
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Jedis;
import java.util.HashSet;
import java.util.Set;

public class RedisSentinelExample {
    public static void main(String[] args) {
        // 设置sentinel地址集合
        Set<String> sentinels = new HashSet<>();
        sentinels.add("192.168.1.100:26379");
        sentinels.add("192.168.1.101:26379");
        sentinels.add("192.168.1.102:26379");
        
        // 创建连接池
        JedisSentinelPool pool = new JedisSentinelPool(
            "mymaster",  // 主服务器名称，与sentinel.conf中定义的一致
            sentinels,
            "your_redis_password"  // 密码
        );
        
        // 获取Jedis实例
        try (Jedis jedis = pool.getResource()) {
            // 执行操作
            jedis.set("key", "value");
            String value = jedis.get("key");
            System.out.println("Value: " + value);
        }
        
        // 关闭连接池
        pool.close();
    }
}
```

### Python客户端示例

```python
from redis.sentinel import Sentinel

# 连接到Sentinel集群
sentinel = Sentinel([
    ('192.168.1.100', 26379),
    ('192.168.1.101', 26379),
    ('192.168.1.102', 26379)
], password='your_redis_password')

# 获取主服务器
master = sentinel.master_for('mymaster', password='your_redis_password')
# 获取从服务器(用于读操作)
slave = sentinel.slave_for('mymaster', password='your_redis_password')

# 写操作使用主服务器
master.set('key', 'value')
# 读操作使用从服务器
value = slave.get('key')
print(f"Value: {value}")
```

### Node.js客户端示例

```javascript
const Redis = require('ioredis');

// 创建Sentinel客户端
const redis = new Redis({
    sentinels: [
        { host: '192.168.1.100', port: 26379 },
        { host: '192.168.1.101', port: 26379 },
        { host: '192.168.1.102', port: 26379 }
    ],
    name: 'mymaster',  // 主服务器名称
    password: 'your_redis_password',
    sentinelPassword: 'your_redis_password' // 如果Sentinel也设置了密码
});

// 使用Redis
async function example() {
    await redis.set('key', 'value');
    const value = await redis.get('key');
    console.log(`Value: ${value}`);
    
    // 关闭连接
    redis.quit();
}

example();
```

## 运维管理

### 监控Sentinel状态

#### 查看Sentinel信息

```bash
redis-cli -h <sentinel_host> -p 26379 info sentinel
```

#### 查看主服务器状态

```bash
redis-cli -h <sentinel_host> -p 26379 sentinel master mymaster
```

#### 查看从服务器列表

```bash
redis-cli -h <sentinel_host> -p 26379 sentinel replicas mymaster
```

### 故障转移测试

#### 模拟主服务器故障

```bash
# 在主服务器上执行
redis-cli -h 192.168.1.100 -a "your_redis_password" shutdown
```

#### 观察故障转移过程

```bash
# 在Sentinel服务器上观察日志
tail -f /var/log/redis/sentinel.log

# 或者使用redis-cli监控事件
redis-cli -h <sentinel_host> -p 26379 subscribe "+switch-master" "+failover-state-*" "+promoted-slave" "+sdown" "+odown"
```

#### 验证新主服务器

```bash
# 查询新的主服务器信息
redis-cli -h <sentinel_host> -p 26379 sentinel master mymaster
```

### 添加或移除Redis节点

#### 添加新的从服务器

1. 配置新从服务器的`redis.conf`：

   ```
   replicaof <current_master_ip> 6379
   masterauth "your_redis_password"
   ```

2. 启动新的从服务器
3. Sentinel会自动发现新的从服务器，无需额外配置

#### 移除从服务器

1. 停止要移除的从服务器
2. Sentinel会自动更新从服务器列表

### 添加或移除Sentinel节点

#### 添加新的Sentinel

1. 在新服务器上创建与现有Sentinel相同的配置：

   ```
   sentinel monitor mymaster <current_master_ip> 6379 <quorum>
   ```

2. 启动新的Sentinel
3. 新Sentinel会自动发现其他Sentinel和Redis实例，并更新配置

#### 移除Sentinel

1. 停止要移除的Sentinel
2. 执行重置命令：

   ```bash
   redis-cli -h <sentinel_host> -p 26379 sentinel reset mymaster
   ```

## 高级配置

### 通知脚本配置

Sentinel可以在特定事件发生时执行脚本：

```
# 配置通知脚本
sentinel notification-script mymaster /path/to/notification_script.sh

# 配置客户端重配置脚本
sentinel client-reconfig-script mymaster /path/to/reconfig_script.sh
```

脚本接收参数格式如下：

**notification_script.sh参数**：
```
<监控的主服务器名> <事件类型> <事件描述> <时间戳>
```

**client-reconfig-script.sh参数**：
```
<主服务器名> <旧主IP> <旧主端口> <新主IP> <新主端口>
```

### 安全加固配置

#### 启用Sentinel认证

在Redis 6.2及以上版本，可以启用Sentinel认证：

```
# sentinel.conf
requirepass "your_sentinel_password"
sentinel sentinel-user sentinel_username
sentinel sentinel-pass sentinel_password
```

#### 网络安全配置

```
# 绑定特定IP
bind 192.168.1.100
# 禁用保护模式（仅当有其他安全措施如防火墙时）
protected-mode no
```

#### TLS加密(Redis 6.0+)

```
# redis.conf/sentinel.conf
tls-cert-file /path/to/cert.pem
tls-key-file /path/to/key.pem
tls-ca-cert-file /path/to/ca.pem
tls-auth-clients yes
```

### 性能优化

#### 优化超时时间

```
# 调整主观下线判断时间
sentinel down-after-milliseconds mymaster 30000

# 调整故障转移超时时间
sentinel failover-timeout mymaster 180000
```

#### 优化并行同步数

```
# 限制同时进行同步的从服务器数
sentinel parallel-syncs mymaster 1
```

## 故障排除指南

### 常见问题与解决方案

#### Sentinel无法检测主服务器下线

**问题**：主服务器已经不可用，但Sentinel没有触发故障转移

**解决方案**：
- 检查Sentinel配置中的quorum值是否合理
- 确认Sentinel与主服务器之间的网络连接
- 查看Sentinel日志是否有报错信息
- 验证防火墙设置是否允许Sentinel访问Redis

#### 故障转移不生效

**问题**：Sentinel检测到主服务器下线，但故障转移未执行

**解决方案**：
- 检查是否有足够数量的Sentinel达成共识
- 确认所有从服务器配置正确且可连接
- 验证从服务器复制状态正常
- 检查权限设置，确保Sentinel有权限执行命令

#### 脑裂问题

**问题**：网络分区导致多个主服务器同时存在

**解决方案**：
- 在主服务器配置中添加：

  ```
  min-replicas-to-write 1
  min-replicas-max-lag 10
  ```

- 确保quorum值设置合理
- 将Sentinel部署在不同的网络段

### 日志分析

#### 重要日志信息解读

```
+sdown           # 主观下线事件
+odown           # 客观下线事件
+vote-for-leader # 领导者选举投票
+elected-leader  # 当选领导者
+failover-state  # 故障转移状态变更
+switch-master   # 主服务器切换
+promoted-slave  # 从服务器被提升为主服务器
```

#### 监控重要的日志模式

```bash
# 监控故障转移相关事件
grep "failover" /var/log/redis/sentinel.log

# 监控选举过程
grep "vote\|elected\|leader" /var/log/redis/sentinel.log

# 监控状态变更
grep "sdown\|odown" /var/log/redis/sentinel.log
```

### 诊断命令

```bash
# 查看Sentinel状态
redis-cli -h <sentinel_host> -p 26379 info sentinel

# 获取主服务器详情
redis-cli -h <sentinel_host> -p 26379 sentinel master mymaster

# 检查Sentinel日志
redis-cli -h <sentinel_host> -p 26379 sentinel pending-scripts

# 测试与主服务器连接
redis-cli -h <master_host> -p 6379 -a "your_redis_password" ping
```

## 最佳实践

### 部署建议

1. **至少3个Sentinel实例**
   - 部署在不同的物理机器上
   - 使用奇数个Sentinel避免选票平分

2. **合理的网络规划**
   - Sentinel实例应部署在不同的网络故障域
   - 避免所有Sentinel实例在同一网段

3. **配置文件管理**
   - 使用版本控制系统管理配置文件
   - 部署前验证配置文件格式

4. **数据持久化**
   - 在所有Redis实例上启用AOF持久化
   - 定期备份RDB文件

### 安全建议

1. **访问控制**
   - 所有Redis和Sentinel实例设置强密码
   - 使用防火墙限制访问IP

2. **最小权限原则**
   - 运行Redis和Sentinel的用户权限最小化
   - 使用专用账户，避免使用root

3. **敏感命令保护**
   - 禁用或重命名危险命令（如FLUSHALL）
   
   ```
   rename-command FLUSHALL ""
   rename-command CONFIG ""
   ```

4. **监控审计**
   - 启用操作日志
   - 收集并分析安全事件

### 运维策略

1. **定期演练故障转移**
   - 定期进行主服务器故障模拟
   - 验证客户端连接和数据完整性

2. **监控系统整合**
   - 将Sentinel监控整合到监控系统
   - 设置关键指标告警

3. **扩容考量**
   - 提前规划容量增长
   - 制定清晰的扩容流程

4. **文档维护**
   - 维护完整的架构文档
   - 记录所有配置变更和维护活动

## 参考资源

- [Redis Sentinel官方文档](https://redis.io/topics/sentinel)
- [Redis高可用性文章](https://redis.io/topics/cluster-tutorial)
- [Redis安全指南](https://redis.io/topics/security)
- [Redis Sentinel客户端支持](https://redis.io/topics/clients)
- [Redis社区支持](https://redis.io/community) 