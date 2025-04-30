DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>4.4.1</version>
</dependency>
DEPENDENCIES_END

package com.example.redis;

/*
 * Redis Java连接示例
 */

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Connection;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.util.Pool;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

public class RedisExample {

    public static void main(String[] args) {
        // 连接参数
        String host = "${data.getBasicInfoValue('host', 'localhost')}";
        int port = Integer.parseInt("${data.getBasicInfoValue('port', '6379')}");
        String deployMode = "${data.getBasicInfoValue('deployMode', '主从模式')}";
        
        // 安全认证配置
        boolean authEnabled = "${data.getSecurityInfoValue('authEnabled', '否')}".equals("是");
        String password = "${data.getSecurityInfoValue('password', '')}";
        
        // 构建Redis URI
        String redisUri = "redis://";
        if (authEnabled && !password.isEmpty()) {
            redisUri += ":" + password + "@";
        }
        redisUri += host + ":" + port;
        
        System.out.println("Redis连接URI: " + redisUri);
        
        try {
            // 根据部署模式选择连接方式
            if (deployMode.equals("哨兵模式")) {
                System.out.println("\n----- 使用哨兵模式连接 -----");
                sentinelModeExample(host, port, authEnabled, password);
            } else if (deployMode.equals("集群模式")) {
                System.out.println("\n----- 使用集群模式连接 -----");
                clusterModeExample(host, port, authEnabled, password);
            } else {
                System.out.println("\n----- 使用单节点/主从模式连接 -----");
                // 1. 传统连接方式
                System.out.println("\n=== 1. 传统连接方式 ===");
                singleNodeExample(host, port, authEnabled, password);
                
                // 2. 使用Redis URI连接
                System.out.println("\n=== 2. 使用Redis URI连接 ===");
                uriConnectionExample(redisUri);
                
                // 3. 使用连接工厂模式
                System.out.println("\n=== 3. 使用JedisPooled连接 ===");
                jedisPooledExample(host, port, authEnabled, password);
            }
            
        } catch (Exception e) {
            System.err.println("Redis连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 单节点/主从模式连接示例
     */
    private static void singleNodeExample(String host, int port, boolean authEnabled, String password) {
        System.out.println("连接到Redis单节点/主从模式...");
        
        // 使用连接池配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);           // 最大连接数
        poolConfig.setMaxIdle(5);             // 最大空闲连接数
        poolConfig.setMinIdle(1);             // 最小空闲连接数
        poolConfig.setMaxWait(Duration.ofSeconds(3)); // 最大等待时间
        poolConfig.setTestOnBorrow(true);     // 借用连接时测试
        poolConfig.setTestOnReturn(true);     // 归还连接时测试
        
        // 创建连接池
        try (JedisPool jedisPool = authEnabled 
                ? new JedisPool(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, password) 
                : new JedisPool(poolConfig, host, port)) {
                
            // 获取连接
            try (Jedis jedis = jedisPool.getResource()) {
                // 测试连接
                String pingResponse = jedis.ping();
                System.out.println("连接成功! 服务器响应: " + pingResponse);
                
                // 基本操作示例
                basicOperationsExample(jedis);
            }
        }
    }
    
    /**
     * 使用Redis URI连接示例
     */
    private static void uriConnectionExample(String redisUri) {
        System.out.println("使用Redis URI连接: " + redisUri);
        
        try {
            URI uri = URI.create(redisUri);
            
            // 使用URI创建Jedis连接
            try (Jedis jedis = new Jedis(uri)) {
                // 测试连接
                String pingResponse = jedis.ping();
                System.out.println("URI连接成功! 服务器响应: " + pingResponse);
                
                // 执行简单操作
                jedis.set("uri-test", "通过URI连接成功");
                String value = jedis.get("uri-test");
                System.out.println("获取 uri-test = " + value);
            }
        } catch (Exception e) {
            System.err.println("URI连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 使用JedisPooled连接示例 (更现代的API)
     */
    private static void jedisPooledExample(String host, int port, boolean authEnabled, String password) {
        System.out.println("使用JedisPooled连接...");
        
        // 连接池配置
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        
        // 客户端配置
        DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder();
        configBuilder.connectionTimeoutMillis(5000);
        configBuilder.socketTimeoutMillis(5000);
        
        if (authEnabled && !password.isEmpty()) {
            configBuilder.password(password);
        }
        
        try (JedisPooled jedisPooled = new JedisPooled(poolConfig, host, port, configBuilder.build())) {
            // 测试连接
            String pingResponse = jedisPooled.ping();
            System.out.println("JedisPooled连接成功! 服务器响应: " + pingResponse);
            
            // 执行事务示例
            transactionExample(jedisPooled);
            
            // 执行管道示例
            pipelineExample(jedisPooled);
        }
    }
    
    /**
     * 哨兵模式连接示例
     */
    private static void sentinelModeExample(String host, int port, boolean authEnabled, String password) {
        System.out.println("连接到Redis哨兵模式...");
        
        try {
            // 哨兵节点集合
            String sentinelNodes = "${data.getBasicInfoValue('sentinelNodes', '')}";
            String masterName = "mymaster"; // 默认主节点名称，实际应从配置中获取
            
            if (sentinelNodes.isEmpty()) {
                System.out.println("未配置哨兵节点，使用单节点模式连接");
                singleNodeExample(host, port, authEnabled, password);
                return;
            }
            
            // 创建哨兵节点集合
            Set<String> sentinels = new HashSet<>();
            for (String node : sentinelNodes.split(",")) {
                sentinels.add(node);
            }
            
            // 创建连接池配置
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            
            // 创建哨兵连接池
            try (JedisSentinelPool sentinelPool = authEnabled
                    ? new JedisSentinelPool(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password)
                    : new JedisSentinelPool(masterName, sentinels, poolConfig)) {
                
                // 获取连接
                try (Jedis jedis = sentinelPool.getResource()) {
                    // 测试连接
                    String pingResponse = jedis.ping();
                    System.out.println("连接成功! 服务器响应: " + pingResponse);
                    
                    // 获取当前主节点信息
                    String currentMaster = jedis.info("replication");
                    System.out.println("当前主节点信息: \n" + currentMaster);
                    
                    // 基本操作示例
                    basicOperationsExample(jedis);
                }
            }
        } catch (Exception e) {
            System.err.println("Redis哨兵模式连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 集群模式连接示例
     */
    private static void clusterModeExample(String host, int port, boolean authEnabled, String password) {
        System.out.println("连接到Redis集群模式...");
        
        try {
            // 创建集群节点
            Set<HostAndPort> clusterNodes = new HashSet<>();
            clusterNodes.add(new HostAndPort(host, port));
            
            // 添加从节点（如果有）
            String slaveNodes = "${data.getBasicInfoValue('slaveNodes', '')}";
            if (!slaveNodes.isEmpty()) {
                for (String node : slaveNodes.split(",")) {
                    String[] hostPort = node.split(":");
                    if (hostPort.length == 2) {
                        clusterNodes.add(new HostAndPort(hostPort[0], Integer.parseInt(hostPort[1])));
                    }
                }
            }
            
            // 集群连接配置
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            
            // 创建集群连接
            try (JedisCluster jedisCluster = authEnabled
                    ? new JedisCluster(clusterNodes, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, 
                        5, password, poolConfig)
                    : new JedisCluster(clusterNodes, poolConfig)) {
                
                // 测试连接
                String pingResponse = jedisCluster.ping();
                System.out.println("连接成功! 集群响应: " + pingResponse);
                
                // 集群操作示例
                clusterOperationsExample(jedisCluster);
            }
        } catch (Exception e) {
            System.err.println("Redis集群模式连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 事务操作示例
     */
    private static void transactionExample(JedisCommands jedis) {
        System.out.println("\n===== 事务操作示例 =====");
        
        if (jedis instanceof JedisPooled) {
            JedisPooled pooled = (JedisPooled) jedis;
            
            // 开始事务
            pooled.multi();
            
            // 执行多个命令
            pooled.set("tx-key1", "事务值1");
            pooled.set("tx-key2", "事务值2");
            pooled.incr("tx-counter");
            
            // 执行事务
            List<Object> results = pooled.exec();
            
            System.out.println("事务执行结果: " + results);
            System.out.println("事务后key1值: " + pooled.get("tx-key1"));
            System.out.println("事务后key2值: " + pooled.get("tx-key2"));
            System.out.println("事务后计数器值: " + pooled.get("tx-counter"));
        } else {
            System.out.println("当前连接不支持事务操作");
        }
    }
    
    /**
     * 管道操作示例
     */
    private static void pipelineExample(JedisPooled jedis) {
        System.out.println("\n===== 管道操作示例 =====");
        
        // 使用管道批量执行命令
        List<Object> results = jedis.pipelined(pipeline -> {
            pipeline.set("pipe-key1", "管道值1");
            pipeline.set("pipe-key2", "管道值2");
            pipeline.incr("pipe-counter");
            pipeline.get("pipe-key1");
            pipeline.get("pipe-key2");
            pipeline.get("pipe-counter");
        });
        
        System.out.println("管道执行结果: " + results);
        System.out.println("管道后key1值: " + jedis.get("pipe-key1"));
        System.out.println("管道后key2值: " + jedis.get("pipe-key2"));
        System.out.println("管道后计数器值: " + jedis.get("pipe-counter"));
    }
    
    /**
     * 基本操作示例（适用于单节点和哨兵模式）
     */
    private static void basicOperationsExample(Jedis jedis) {
        System.out.println("\n===== 基本操作示例 =====");
        
        // 字符串操作
        jedis.set("mykey", "Hello Redis");
        System.out.println("设置 mykey = 'Hello Redis'");
        
        String value = jedis.get("mykey");
        System.out.println("获取 mykey = " + value);
        
        // 过期时间设置
        jedis.setex("temp-key", 60, "一分钟后过期");
        Long ttl = jedis.ttl("temp-key");
        System.out.println("temp-key 过期时间: " + ttl + " 秒");
        
        // 哈希操作
        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", "张三");
        userMap.put("age", "30");
        userMap.put("city", "北京");
        jedis.hset("user:1", userMap);
        System.out.println("设置 user:1 哈希数据");
        
        Map<String, String> userData = jedis.hgetAll("user:1");
        System.out.println("获取 user:1 数据: " + userData);
        
        // 列表操作
        jedis.del("mylist"); // 确保列表为空
        jedis.lpush("mylist", "世界", "你好");
        jedis.rpush("mylist", "Redis");
        System.out.println("列表操作: 左侧添加'你好'和'世界'，右侧添加'Redis'");
        
        List<String> listData = jedis.lrange("mylist", 0, -1);
        System.out.println("列表内容: " + listData);
        
        // 集合操作
        jedis.sadd("myset", "A", "B", "C");
        System.out.println("集合操作: 添加元素 A, B, C");
        
        Set<String> setData = jedis.smembers("myset");
        System.out.println("集合内容: " + setData);
        
        // 有序集合操作
        Map<String, Double> scores = new HashMap<>();
        scores.put("张三", 100.0);
        scores.put("李四", 85.0);
        scores.put("王五", 95.0);
        jedis.zadd("ranking", scores);
        System.out.println("有序集合操作: 添加成绩排名");
        
        List<String> topScores = jedis.zrevrange("ranking", 0, 2);
        System.out.println("前三名: " + topScores);
        
        // 键操作
        System.out.println("\n----- 键操作示例 -----");
        Set<String> keys = jedis.keys("*");
        System.out.println("所有键: " + keys);
        
        boolean exists = jedis.exists("mykey");
        System.out.println("mykey 是否存在: " + exists);
        
        String type = jedis.type("user:1");
        System.out.println("user:1 类型: " + type);
    }
    
    /**
     * 集群操作示例
     */
    private static void clusterOperationsExample(JedisCluster jedisCluster) {
        System.out.println("\n===== 集群操作示例 =====");
        
        // 字符串操作
        jedisCluster.set("cluster:key1", "集群值1");
        System.out.println("设置 cluster:key1 = '集群值1'");
        
        String value = jedisCluster.get("cluster:key1");
        System.out.println("获取 cluster:key1 = " + value);
        
        // 哈希操作
        Map<String, String> clusterMap = new HashMap<>();
        clusterMap.put("field1", "值1");
        clusterMap.put("field2", "值2");
        jedisCluster.hset("cluster:hash", clusterMap);
        System.out.println("设置 cluster:hash 哈希数据");
        
        Map<String, String> hashData = jedisCluster.hgetAll("cluster:hash");
        System.out.println("获取 cluster:hash 数据: " + hashData);
        
        // 列表操作
        jedisCluster.lpush("cluster:list", "集群", "列表");
        jedisCluster.rpush("cluster:list", "示例");
        System.out.println("列表操作: 添加数据到集群列表");
        
        List<String> listData = jedisCluster.lrange("cluster:list", 0, -1);
        System.out.println("列表内容: " + listData);
        
        // 注: 集群模式下，某些跨槽位的操作不支持(如keys命令)
        // 管道和事务也会受到限制，只能在同一个槽内执行
    }
} 