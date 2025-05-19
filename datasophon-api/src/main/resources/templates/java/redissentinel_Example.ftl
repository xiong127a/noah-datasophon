DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>4.4.1</version>
</dependency>
DEPENDENCIES_END

package com.example.redis.sentinel;

/*
 * Redis Sentinel Java连接示例
 * 专注于Redis哨兵模式的连接和操作
 */

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class RedisSentinelClient {

    public static void main(String[] args) {
        // 连接参数
        String masterHost = "${data.getBasicInfoValue('host', 'localhost')}";
        int masterPort = Integer.parseInt("${data.getBasicInfoValue('port', '6379')}");
        
        // 获取哨兵节点信息
        String sentinelNodes = "${data.getBasicInfoValue('sentinelNodes', '')}";
        int sentinelPort = Integer.parseInt("${data.getBasicInfoValue('sentinelPort', '26379')}");
        
        // 安全认证配置
        boolean authEnabled = "${data.getSecurityInfoValue('authEnabled', '否')}".equals("是");
        String password = "${data.getSecurityInfoValue('password', '')}";
        
        // 主节点名称 (哨兵配置中设置的名称)
        String masterName = "mymaster";  // 默认值，实际应从配置中获取
        
        System.out.println("===== Redis哨兵模式连接示例 =====");
        System.out.println("主节点: " + masterHost + ":" + masterPort);
        System.out.println("哨兵节点: " + sentinelNodes);
        
        try {
            // 如果未配置哨兵节点信息，使用默认连接方式
            if (sentinelNodes == null || sentinelNodes.isEmpty()) {
                System.out.println("警告: 未配置哨兵节点，将使用单节点模式连接主节点");
                connectToMaster(masterHost, masterPort, authEnabled, password);
                return;
            }
            
            // 解析哨兵节点列表
            Set<String> sentinels = parseSentinelNodes(sentinelNodes, sentinelPort);
            System.out.println("已配置 " + sentinels.size() + " 个哨兵节点");
            
            // 使用哨兵模式连接
            connectWithSentinel(sentinels, masterName, authEnabled, password);
            
        } catch (Exception e) {
            System.err.println("Redis哨兵连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 解析哨兵节点信息
     */
    private static Set<String> parseSentinelNodes(String sentinelNodes, int defaultPort) {
        Set<String> sentinels = new HashSet<>();
        
        for (String node : sentinelNodes.split(",")) {
            if (node.contains(":")) {
                // 节点包含端口信息
                sentinels.add(node);
            } else {
                // 节点不包含端口，使用默认端口
                sentinels.add(node + ":" + defaultPort);
            }
        }
        
        return sentinels;
    }
    
    /**
     * 直接连接到主节点 (当哨兵节点配置不可用时)
     */
    private static void connectToMaster(String host, int port, boolean authEnabled, String password) {
        System.out.println("\n----- 使用单节点模式连接到主节点 -----");
        
        // 创建连接池配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWait(Duration.ofSeconds(3));
        poolConfig.setTestOnBorrow(true);
        
        // 创建Jedis实例
        try (Jedis jedis = authEnabled 
                ? new Jedis(host, port, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, password)
                : new Jedis(host, port)) {
                
            // 测试连接
            String pingResponse = jedis.ping();
            System.out.println("连接成功! 服务器响应: " + pingResponse);
            
            // 执行基本操作示例
            basicOperationsExample(jedis);
        }
    }
    
    /**
     * 使用哨兵模式连接
     */
    private static void connectWithSentinel(Set<String> sentinels, String masterName,
                                        boolean authEnabled, String password) {
        System.out.println("\n----- 使用哨兵模式连接 -----");
        
        // 创建连接池配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWait(Duration.ofSeconds(3));
        poolConfig.setTestOnBorrow(true);
        
        // 创建哨兵连接池
        try (JedisSentinelPool sentinelPool = authEnabled
                ? new JedisSentinelPool(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password)
                : new JedisSentinelPool(masterName, sentinels, poolConfig)) {
                
            System.out.println("哨兵连接池创建成功");
            System.out.println("当前主节点: " + sentinelPool.getCurrentHostMaster());
            
            // 获取连接
            try (Jedis jedis = sentinelPool.getResource()) {
                // 测试连接
                String pingResponse = jedis.ping();
                System.out.println("连接成功! 服务器响应: " + pingResponse);
                
                // 获取主节点信息
                displayMasterInfo(jedis);
                
                // 执行基本操作示例
                basicOperationsExample(jedis);
                
                // 哨兵特有操作
                sentinelSpecificOperations(jedis);
            }
        }
    }
    
    /**
     * 显示主节点信息
     */
    private static void displayMasterInfo(Jedis jedis) {
        System.out.println("\n----- 主节点信息 -----");
        
        // 获取当前主节点信息
        String info = jedis.info("replication");
        
        // 打印主要信息
        System.out.println("复制信息:\n" + info);
        
        // 获取服务器角色信息
        String role = jedis.info("server").contains("role:master") ? "主节点" : "从节点";
        System.out.println("当前连接到的是: " + role);
    }
    
    /**
     * 哨兵模式特有操作示例
     */
    private static void sentinelSpecificOperations(Jedis jedis) {
        System.out.println("\n----- 哨兵模式特有操作 -----");
        System.out.println("注意: 以下操作通常需要直接连接到哨兵节点执行");
        System.out.println("通过以下命令可以在哨兵节点上查询信息:");
        System.out.println("SENTINEL masters        # 查看所有主节点");
        System.out.println("SENTINEL slaves <master-name>     # 查看指定主节点的所有从节点");
        System.out.println("SENTINEL sentinels <master-name>  # 查看指定主节点的所有哨兵");
        System.out.println("SENTINEL get-master-addr-by-name <master-name>  # 获取主节点地址");
    }
    
    /**
     * 基本操作示例
     */
    private static void basicOperationsExample(Jedis jedis) {
        System.out.println("\n===== 基本操作示例 =====");
        
        // 字符串操作
        jedis.set("sentinel:key", "哨兵模式测试");
        System.out.println("设置 sentinel:key = '哨兵模式测试'");
        
        String value = jedis.get("sentinel:key");
        System.out.println("获取 sentinel:key = " + value);
        
        // 过期时间设置
        jedis.setex("sentinel:temp-key", 60, "一分钟后过期");
        Long ttl = jedis.ttl("sentinel:temp-key");
        System.out.println("sentinel:temp-key 过期时间: " + ttl + " 秒");
        
        // 哈希操作
        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", "张三");
        userMap.put("age", "30");
        userMap.put("city", "北京");
        jedis.hset("sentinel:user:1", userMap);
        System.out.println("设置 sentinel:user:1 哈希数据");
        
        Map<String, String> userData = jedis.hgetAll("sentinel:user:1");
        System.out.println("获取 sentinel:user:1 数据: " + userData);
        
        // 列表操作
        jedis.del("sentinel:list");  // 确保列表为空
        jedis.lpush("sentinel:list", "哨兵", "模式");
        jedis.rpush("sentinel:list", "示例");
        System.out.println("列表操作: 添加数据到列表");
        
        List<String> listData = jedis.lrange("sentinel:list", 0, -1);
        System.out.println("列表内容: " + listData);
        
        // 查看数据分布情况
        System.out.println("\n----- 数据分布情况 -----");
        System.out.println("注: 在哨兵模式下，所有写操作都会发送到主节点");
        System.out.println("  从节点提供读操作支持，实现读写分离");
        System.out.println("  当主节点故障时，哨兵会自动将一个从节点提升为新的主节点");
    }
} 