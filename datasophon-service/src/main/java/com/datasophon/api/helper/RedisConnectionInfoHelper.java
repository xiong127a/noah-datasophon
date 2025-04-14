package com.datasophon.api.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.strategy.ServiceHandlerAbstract;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis连接信息辅助工具类
 * 用于提取和生成Redis连接信息，供Redis相关的策略类调用
 */
@Component
public class RedisConnectionInfoHelper extends ServiceHandlerAbstract {
    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionInfoHelper.class);

    /**
     * 获取Redis连接信息
     *
     * @param clusterId         集群ID
     * @param serviceInstanceId 服务实例ID
     * @param strategy          Redis相关策略类实例
     * @return Redis连接信息
     */
    public ConnectionInfo getRedisConnectionInfo(Integer clusterId, Integer serviceInstanceId,
                                                 ServiceRoleStrategy strategy) {
        try {
            // 1. 获取服务配置
            // 1. 获取服务配置
            Pair<String, List<ServiceConfig>> pair = strategy.listServiceConfigByServiceInstance(serviceInstanceId);
            List<ServiceConfig> serviceConfigs = pair.getValue();
            String serviceHome = pair.getKey();

            // 2. 从配置中解析配置到map，方便快速查询
            Map<String, String> configMap = new HashMap<>();
            for (ServiceConfig config : serviceConfigs) {
                if (config.getValue() != null) {
                    configMap.put(config.getName(), String.valueOf(config.getValue()));
                }
            }

            // 3. 确定服务类型，以便获取正确的角色列表
            boolean isRedisSentinel = strategy.getClass().getSimpleName().contains("Sentinel");

            // 根据服务类型设置角色名称
            String masterRoleName = isRedisSentinel ? "RedisSentinelMaster" : "RedisMaster";
            String slaveRoleName = isRedisSentinel ? "RedisSentinelSlave" : "RedisWorker";
            String sentinelRoleName = "RedisSentinel"; // 仅在哨兵模式有效

            // 4. 获取Redis Master和Slave节点列表
            List<String> masterList = strategy.getRoleHosts(clusterId, serviceInstanceId, masterRoleName);
            List<String> slaveList = strategy.getRoleHosts(clusterId, serviceInstanceId, slaveRoleName);

            // 如果没有找到Master节点，返回空信息
            if (CollUtil.isEmpty(masterList)) {
                logger.warn("未找到Redis Master节点，集群ID: {}", clusterId);
                return ConnectionInfo.builder().build();
            }

            // 5. 获取端口配置
            String masterPort;
            String slavePort;

            if (isRedisSentinel) {
                masterPort = configMap.getOrDefault("redisSentinelMasterPort", "6379");
                slavePort = configMap.getOrDefault("redisSentinelSlavePort", "6379");
            } else {
                masterPort = configMap.getOrDefault("redisMasterPort", "6379");
                slavePort = configMap.getOrDefault("redisSlavePort", "6379");
            }

            // 6. 判断是否启用了密码认证
            String redisPassword = configMap.get("requirepass");
            boolean hasPassword = StrUtil.isNotBlank(redisPassword);

            // 7. 判断是否为哨兵模式
            boolean isSentinelMode = isRedisSentinel
                    || strategy.getClass().getSimpleName().equals("RedisSentinelHandlerStrategy");
            String sentinelPort = "26379"; // 默认哨兵端口
            List<String> sentinelList = null;

            if (isSentinelMode) {
                sentinelList = strategy.getRoleHosts(clusterId, serviceInstanceId, sentinelRoleName);
                sentinelPort = configMap.getOrDefault("redisSentinelPort", "26379");
            }

            // 8. 判断是否为集群模式（主从复制模式也算作单节点模式）
            boolean isClusterMode = "yes".equalsIgnoreCase(configMap.getOrDefault("cluster-enabled", "no"));

            // 封装部署信息
            DeploymentInfo deploymentInfo = new DeploymentInfo(
                    masterList.get(0), masterPort, hasPassword, redisPassword,
                    slaveList, slavePort, isSentinelMode, isClusterMode,
                    sentinelList, sentinelPort);

            // 9. 构建Redis URI
            String redisUri = buildRedisUri(deploymentInfo);

            // 10. 构建基本连接信息
            Map<String, String> basicInfo = buildBasicInfo(deploymentInfo, redisUri);

            // 11. 构建有序的基本连接信息列表（用于前端表格显示）
            List<Map<String, String>> basicInfoList = buildOrderedInfoList(basicInfo);

            // 13. 返回构建好的连接信息
            return ConnectionInfo.builder()
                    .basicInfo(basicInfo)
                    .basicInfoList(basicInfoList)
                    .javaCode(generateJavaCode(deploymentInfo))
                    .pythonCode(generatePythonCode(deploymentInfo))
                    .commandLines(generateCommandLines(serviceHome, deploymentInfo))
                    .hostName(deploymentInfo.masterNode)
                    .build();

        } catch (Exception e) {
            logger.error("获取Redis连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder().build();
        }
    }

    /**
     * 部署信息内部类
     * 用于封装Redis部署相关的参数
     */
    @Data
    private static class DeploymentInfo {
        private final String masterNode;
        private final String masterPort;
        private final boolean hasPassword;
        private final String redisPassword;
        private final List<String> slaveList;
        private final String slavePort;
        private final boolean isSentinelMode;
        private final boolean isClusterMode;
        private final List<String> sentinelList;
        private final String sentinelPort;
    }

    /**
     * 构建Redis URI
     *
     * @param info 部署信息
     * @return Redis连接URI
     */
    private String buildRedisUri(DeploymentInfo info) {
        String redisUri = "redis://";
        if (info.hasPassword) {
            redisUri += ":" + info.redisPassword + "@";
        }
        redisUri += info.masterNode + ":" + info.masterPort;
        return redisUri;
    }

    /**
     * 构建基本连接信息
     *
     * @param info     部署信息
     * @param redisUri Redis URI
     * @return 基本连接信息Map
     */
    private Map<String, String> buildBasicInfo(DeploymentInfo info, String redisUri) {
        Map<String, String> basicInfo = new HashMap<>();
        basicInfo.put("Redis主节点", info.masterNode + ":" + info.masterPort);
        basicInfo.put("连接URI", redisUri);

        if (!info.slaveList.isEmpty()) {
            basicInfo.put("Redis从节点", info.slaveList.stream()
                    .map(node -> node + ":" + info.slavePort)
                    .collect(Collectors.joining(", ")));
        }

        String deployMode = info.isClusterMode ? "集群模式" : (info.isSentinelMode ? "哨兵模式" : "主从模式");
        basicInfo.put("部署模式", deployMode);

        if (info.isSentinelMode) {
            basicInfo.put("哨兵节点", info.sentinelList.stream()
                    .map(node -> node + ":" + info.sentinelPort)
                    .collect(Collectors.joining(", ")));
        }

        basicInfo.put("密码认证", info.hasPassword ? "是" : "否");
        return basicInfo;
    }

    /**
     * 构建有序的基本信息列表（用于前端表格显示）
     *
     * @param basicInfo 基本连接信息Map
     * @return 有序的信息列表
     */
    private List<Map<String, String>> buildOrderedInfoList(Map<String, String> basicInfo) {
        List<Map<String, String>> basicInfoList = new ArrayList<>();

        // 按照固定的顺序添加信息
        String[] orderedKeys = {
                "Redis主节点",
                "连接URI",
                "Redis从节点",
                "部署模式",
                "哨兵节点",
                "密码认证"
        };

        // 按顺序添加到basicInfoList
        for (String key : orderedKeys) {
            if (basicInfo.containsKey(key)) {
                Map<String, String> item = new HashMap<>();
                item.put("label", key);
                item.put("value", basicInfo.get(key));
                basicInfoList.add(item);
            }
        }

        return basicInfoList;
    }

    /**
     * 生成Java代码示例
     *
     * @param info 部署信息
     * @return Java代码示例
     */
    private String generateJavaCode(DeploymentInfo info) {
        StringBuilder code = new StringBuilder();

        // 导入依赖信息
        code.append("// 添加Maven依赖\n");

        if (info.isClusterMode) {
            code.append("// Redis集群模式\n");
            code.append("<dependency>\n");
            code.append("    <groupId>redis.clients</groupId>\n");
            code.append("    <artifactId>jedis</artifactId>\n");
            code.append("    <version>4.4.3</version>\n");
            code.append("</dependency>\n\n");

            code.append("import redis.clients.jedis.HostAndPort;\n");
            code.append("import redis.clients.jedis.JedisCluster;\n");
            code.append("import redis.clients.jedis.JedisPoolConfig;\n");
            code.append("import java.util.HashSet;\n");
            code.append("import java.util.Set;\n\n");

            code.append("// Redis集群模式连接示例\n");
            code.append("public class RedisClusterExample {\n");
            code.append("    public static void main(String[] args) {\n");
            code.append("        // 配置Jedis连接池\n");
            code.append("        JedisPoolConfig poolConfig = new JedisPoolConfig();\n");
            code.append("        poolConfig.setMaxTotal(8);\n");
            code.append("        poolConfig.setMaxIdle(8);\n");
            code.append("        poolConfig.setMinIdle(0);\n\n");

            code.append("        // 设置Redis集群节点\n");
            code.append("        Set<HostAndPort> nodes = new HashSet<>();\n");

            // 添加主节点
            code.append("        nodes.add(new HostAndPort(\"").append(info.masterNode).append("\", ")
                    .append(info.masterPort).append("));\n");

            // 添加从节点
            for (String slave : info.slaveList) {
                code.append("        nodes.add(new HostAndPort(\"").append(slave).append("\", ")
                        .append(info.slavePort).append("));\n");
            }

            code.append("\n        // 创建JedisCluster实例\n");

            if (info.hasPassword) {
                code.append("        JedisCluster jedisCluster = new JedisCluster(nodes, 2000, 2000, 5, \"")
                        .append(info.redisPassword).append("\", poolConfig);\n\n");
            } else {
                code.append("        JedisCluster jedisCluster = new JedisCluster(nodes, poolConfig);\n\n");
            }

            code.append("        try {\n");
            code.append("            // 执行命令示例\n");
            code.append("            String value = jedisCluster.get(\"mykey\");\n");
            code.append("            System.out.println(\"Value for 'mykey': \" + value);\n\n");

            code.append("            // 设置键值对\n");
            code.append("            String result = jedisCluster.set(\"newkey\", \"newvalue\");\n");
            code.append("            System.out.println(\"Set result: \" + result);\n");
            code.append("        } finally {\n");
            code.append("            // 关闭连接\n");
            code.append("            jedisCluster.close();\n");
            code.append("        }\n");
            code.append("    }\n");
            code.append("}\n");
        } else if (info.isSentinelMode) {
            code.append("// Redis哨兵模式\n");
            code.append("<dependency>\n");
            code.append("    <groupId>redis.clients</groupId>\n");
            code.append("    <artifactId>jedis</artifactId>\n");
            code.append("    <version>4.4.3</version>\n");
            code.append("</dependency>\n\n");

            code.append("import redis.clients.jedis.Jedis;\n");
            code.append("import redis.clients.jedis.JedisSentinelPool;\n");
            code.append("import redis.clients.jedis.JedisPoolConfig;\n");
            code.append("import java.util.HashSet;\n");
            code.append("import java.util.Set;\n\n");

            code.append("// Redis哨兵模式连接示例\n");
            code.append("public class RedisSentinelExample {\n");
            code.append("    public static void main(String[] args) {\n");
            code.append("        // 配置Jedis连接池\n");
            code.append("        JedisPoolConfig poolConfig = new JedisPoolConfig();\n");
            code.append("        poolConfig.setMaxTotal(8);\n");
            code.append("        poolConfig.setMaxIdle(8);\n");
            code.append("        poolConfig.setMinIdle(0);\n\n");

            code.append("        // 设置哨兵节点\n");
            code.append("        Set<String> sentinels = new HashSet<>();\n");

            // 添加哨兵节点
            for (String sentinel : info.sentinelList) {
                code.append("        sentinels.add(\"").append(sentinel).append(":")
                        .append(info.sentinelPort).append("\");\n");
            }

            code.append("\n        // 创建哨兵连接池\n");

            if (info.hasPassword) {
                code.append(
                                "        JedisSentinelPool pool = new JedisSentinelPool(\"mymaster\", sentinels, poolConfig, 2000, \"")
                        .append(info.redisPassword).append("\");\n\n");
            } else {
                code.append(
                        "        JedisSentinelPool pool = new JedisSentinelPool(\"mymaster\", sentinels, poolConfig);\n\n");
            }

            code.append("        try (Jedis jedis = pool.getResource()) {\n");
            code.append("            // 执行命令示例\n");
            code.append("            String value = jedis.get(\"mykey\");\n");
            code.append("            System.out.println(\"Value for 'mykey': \" + value);\n\n");

            code.append("            // 设置键值对\n");
            code.append("            String result = jedis.set(\"newkey\", \"newvalue\");\n");
            code.append("            System.out.println(\"Set result: \" + result);\n");
            code.append("        } finally {\n");
            code.append("            // 关闭连接池\n");
            code.append("            pool.close();\n");
            code.append("        }\n");
            code.append("    }\n");
            code.append("}\n");
        } else {
            // 单节点模式
            code.append("// Redis单节点/主从模式\n");
            code.append("<dependency>\n");
            code.append("    <groupId>redis.clients</groupId>\n");
            code.append("    <artifactId>jedis</artifactId>\n");
            code.append("    <version>4.4.3</version>\n");
            code.append("</dependency>\n\n");

            code.append("import redis.clients.jedis.Jedis;\n");
            code.append("import redis.clients.jedis.JedisPool;\n");
            code.append("import redis.clients.jedis.JedisPoolConfig;\n\n");

            code.append("// Redis单节点连接示例\n");
            code.append("public class RedisStandaloneExample {\n");
            code.append("    public static void main(String[] args) {\n");
            code.append("        // 配置Jedis连接池\n");
            code.append("        JedisPoolConfig poolConfig = new JedisPoolConfig();\n");
            code.append("        poolConfig.setMaxTotal(8);\n");
            code.append("        poolConfig.setMaxIdle(8);\n");
            code.append("        poolConfig.setMinIdle(0);\n\n");

            code.append("        // 创建Jedis连接池\n");
            code.append("        JedisPool pool = new JedisPool(poolConfig, \"").append(info.masterNode).append("\", ")
                    .append(info.masterPort);

            if (info.hasPassword) {
                code.append(", 2000, \"").append(info.redisPassword).append("\");\n\n");
            } else {
                code.append(");\n\n");
            }

            code.append("        try (Jedis jedis = pool.getResource()) {\n");
            code.append("            // 执行命令示例\n");
            code.append("            String value = jedis.get(\"mykey\");\n");
            code.append("            System.out.println(\"Value for 'mykey': \" + value);\n\n");

            code.append("            // 设置键值对\n");
            code.append("            String result = jedis.set(\"newkey\", \"newvalue\");\n");
            code.append("            System.out.println(\"Set result: \" + result);\n");
            code.append("        } finally {\n");
            code.append("            // 关闭连接池\n");
            code.append("            pool.close();\n");
            code.append("        }\n");
            code.append("    }\n");
            code.append("}\n");
        }

        return code.toString();
    }

    /**
     * 生成Python代码示例
     *
     * @param info 部署信息
     * @return Python代码示例
     */
    private String generatePythonCode(DeploymentInfo info) {
        StringBuilder code = new StringBuilder();

        // 添加安装依赖信息
        code.append("# 安装依赖\n");
        code.append("# pip install redis\n\n");

        if (info.isClusterMode) {
            code.append("# Redis集群模式\n");
            code.append("from redis.cluster import RedisCluster\n\n");

            code.append("# 定义集群节点\n");
            code.append("startup_nodes = [\n");

            // 添加主节点
            code.append("    {\"host\": \"").append(info.masterNode).append("\", \"port\": ")
                    .append(info.masterPort).append("}\n");

            // 添加从节点
            for (String slave : info.slaveList) {
                code.append("    {\"host\": \"").append(slave).append("\", \"port\": ")
                        .append(info.slavePort).append("}\n");
            }

            code.append("]\n\n");

            code.append("# 创建RedisCluster连接\n");

            if (info.hasPassword) {
                code.append(
                                "redis_client = RedisCluster(startup_nodes=startup_nodes, decode_responses=True, password=\"")
                        .append(info.redisPassword).append("\")\n\n");
            } else {
                code.append("redis_client = RedisCluster(startup_nodes=startup_nodes, decode_responses=True)\n\n");
            }

            code.append("# 执行命令示例\n");
            code.append("# 设置键值对\n");
            code.append("redis_client.set('mykey', 'Hello from Python Cluster client')\n\n");

            code.append("# 获取键值\n");
            code.append("value = redis_client.get('mykey')\n");
            code.append("print(f\"Value for 'mykey': {value}\")\n\n");

            code.append("# 关闭连接\n");
            code.append("redis_client.close()\n");
        } else if (info.isSentinelMode) {
            code.append("# Redis哨兵模式\n");
            code.append("from redis.sentinel import Sentinel\n\n");

            code.append("# 定义哨兵节点\n");
            code.append("sentinel_hosts = [\n");

            // 添加哨兵节点
            for (String sentinel : info.sentinelList) {
                code.append("    (\"").append(sentinel).append("\", ")
                        .append(info.sentinelPort).append("),\n");
            }

            code.append("]\n\n");

            code.append("# 创建Sentinel连接\n");
            code.append("sentinel = Sentinel(sentinel_hosts, socket_timeout=0.5)\n\n");

            code.append("# 获取主节点连接\n");

            if (info.hasPassword) {
                code.append("master = sentinel.master_for('mymaster', socket_timeout=0.5, password=\"")
                        .append(info.redisPassword).append("\", decode_responses=True)\n\n");
            } else {
                code.append("master = sentinel.master_for('mymaster', socket_timeout=0.5, decode_responses=True)\n\n");
            }

            code.append("# 执行命令示例\n");
            code.append("# 设置键值对\n");
            code.append("master.set('mykey', 'Hello from Python Sentinel client')\n\n");

            code.append("# 获取键值\n");
            code.append("value = master.get('mykey')\n");
            code.append("print(f\"Value for 'mykey': {value}\")\n\n");

            code.append("# 获取从节点连接\n");

            if (info.hasPassword) {
                code.append("slave = sentinel.slave_for('mymaster', socket_timeout=0.5, password=\"")
                        .append(info.redisPassword).append("\", decode_responses=True)\n");
            } else {
                code.append("slave = sentinel.slave_for('mymaster', socket_timeout=0.5, decode_responses=True)\n");
            }

            code.append("# 从从节点读取数据\n");
            code.append("slave_value = slave.get('mykey')\n");
            code.append("print(f\"Value from slave for 'mykey': {slave_value}\")\n");
        } else {
            // 单节点模式
            code.append("# Redis单节点/主从模式\n");
            code.append("import redis\n\n");

            code.append("# 创建Redis连接\n");

            if (info.hasPassword) {
                code.append("redis_client = redis.Redis(host=\"").append(info.masterNode).append("\", port=")
                        .append(info.masterPort).append(", password=\"").append(info.redisPassword)
                        .append("\", decode_responses=True)\n\n");
            } else {
                code.append("redis_client = redis.Redis(host=\"").append(info.masterNode).append("\", port=")
                        .append(info.masterPort).append(", decode_responses=True)\n\n");
            }

            code.append("# 执行命令示例\n");
            code.append("# 设置键值对\n");
            code.append("redis_client.set('mykey', 'Hello from Python client')\n\n");

            code.append("# 获取键值\n");
            code.append("value = redis_client.get('mykey')\n");
            code.append("print(f\"Value for 'mykey': {value}\")\n\n");

            code.append("# 关闭连接\n");
            code.append("redis_client.close()\n");
        }

        return code.toString();
    }

    /**
     * 生成命令行示例
     *
     * @param redisHome Redis安装目录
     * @param info      部署信息
     * @return 命令行示例列表
     */
    private List<CommandLineItem> generateCommandLines(String redisHome, DeploymentInfo info) {
        List<CommandLineItem> commands = new ArrayList<>();
        String cliPath = redisHome + "/bin/redis-cli";
        String authParam = info.hasPassword ? " -a " + info.redisPassword : "";
        String hostname = info.masterNode;

        // 添加进入redis目录的提示符
        String redisHomePrompt = "[root@" + hostname + " " + redisHome.substring(redisHome.lastIndexOf('/') + 1)
                + "]# ";
        // Redis客户端命令提示符
        String redisPrompt = hostname + ":" + info.masterPort + "> ";

        // 1. 连接到Redis服务器
        String connectCmd = cliPath +
                " -h " + info.masterNode +
                " -p " + info.masterPort +
                authParam;

        CommandLineItem connectItem = new CommandLineItem();
        connectItem.setLabel("连接到Redis服务器");
        connectItem.setValue(connectCmd);
        connectItem.setCommandResult("Connected to " + hostname + ":" + info.masterPort);
        connectItem.setCommandPrompt(redisHomePrompt);
        commands.add(connectItem);

        // 2. 测试服务器连通性
        String pingCmd = cliPath +
                " -h " + info.masterNode +
                " -p " + info.masterPort +
                authParam +
                " ping";

        CommandLineItem pingItem = new CommandLineItem();
        pingItem.setLabel("测试Redis服务器连通性");
        pingItem.setValue(pingCmd);
        pingItem.setCommandResult("PONG");
        pingItem.setCommandPrompt(redisHomePrompt);
        commands.add(pingItem);

        // 3. 获取服务器信息
        String infoCmd = cliPath +
                " -h " + info.masterNode +
                " -p " + info.masterPort +
                authParam +
                " info";

        CommandLineItem infoItem = new CommandLineItem();
        infoItem.setLabel("获取Redis服务器信息");
        infoItem.setValue(infoCmd);
        infoItem.setCommandResult(
                "# Server\nredis_version:7.0.8\nredis_git_sha1:00000000\nredis_git_dirty:0\nredis_build_id:aabbccddeeff\nredis_mode:standalone\nos:Linux 5.15.0-48-generic x86_64\narch_bits:64\n...");
        infoItem.setCommandPrompt(redisHomePrompt);
        commands.add(infoItem);

        // 4. 设置和获取键值
        String setGetCmd = cliPath +
                " -h " + info.masterNode +
                " -p " + info.masterPort +
                authParam +
                " set mykey \"Hello World\" get mykey";

        CommandLineItem setGetItem = new CommandLineItem();
        setGetItem.setLabel("设置并获取键值");
        setGetItem.setValue(setGetCmd);
        setGetItem.setCommandResult("OK\n\"Hello World\"");
        setGetItem.setCommandPrompt(redisHomePrompt);
        commands.add(setGetItem);

        // 5. 获取所有键名
        String keysCmd = cliPath +
                " -h " + info.masterNode +
                " -p " + info.masterPort +
                authParam +
                " keys *";

        CommandLineItem keysItem = new CommandLineItem();
        keysItem.setLabel("获取所有键名");
        keysItem.setValue(keysCmd);
        keysItem.setCommandResult("1) \"mykey\"\n(可能会显示当前库中的其他键)");
        keysItem.setCommandPrompt(redisHomePrompt);
        commands.add(keysItem);

        // 6. 监控实时请求
        String monitorCmd = cliPath +
                " -h " + info.masterNode +
                " -p " + info.masterPort +
                authParam +
                " monitor";

        CommandLineItem monitorItem = new CommandLineItem();
        monitorItem.setLabel("监控Redis实时请求");
        monitorItem.setValue(monitorCmd);
        monitorItem.setCommandResult("OK\n(实时显示Redis接收到的命令)");
        monitorItem.setCommandPrompt(redisHomePrompt);
        commands.add(monitorItem);

        // 如果是集群模式，添加集群相关命令
        if (info.isClusterMode) {
            // 7. 查看集群节点信息
            String clusterNodesCmd = cliPath +
                    " -h " + info.masterNode +
                    " -p " + info.masterPort +
                    authParam +
                    " cluster nodes";

            CommandLineItem clusterNodesItem = new CommandLineItem();
            clusterNodesItem.setLabel("查看集群节点信息");
            clusterNodesItem.setValue(clusterNodesCmd);
            clusterNodesItem.setCommandResult("显示集群中所有节点的详细信息，包括节点ID、IP地址、端口、角色等");
            clusterNodesItem.setCommandPrompt(redisHomePrompt);
            commands.add(clusterNodesItem);

            // 8. 查看集群状态
            String clusterInfoCmd = cliPath +
                    " -h " + info.masterNode +
                    " -p " + info.masterPort +
                    authParam +
                    " cluster info";

            CommandLineItem clusterInfoItem = new CommandLineItem();
            clusterInfoItem.setLabel("查看集群状态");
            clusterInfoItem.setValue(clusterInfoCmd);
            clusterInfoItem.setCommandResult("显示集群的当前状态信息，包括集群大小、槽分配情况等");
            clusterInfoItem.setCommandPrompt(redisHomePrompt);
            commands.add(clusterInfoItem);
        }

        // 如果是哨兵模式，添加哨兵相关命令
        if (info.isSentinelMode && !info.sentinelList.isEmpty()) {
            // 9. 连接到哨兵并获取主节点信息
            String sentinelCmd = cliPath +
                    " -h " + info.sentinelList.get(0) +
                    " -p " + info.sentinelPort +
                    " sentinel masters";

            CommandLineItem sentinelItem = new CommandLineItem();
            sentinelItem.setLabel("获取哨兵监控的主节点信息");
            sentinelItem.setValue(sentinelCmd);
            sentinelItem.setCommandResult("显示哨兵监控的所有主节点信息");
            sentinelItem.setCommandPrompt(redisHomePrompt);
            commands.add(sentinelItem);

            // 10. 获取指定主节点的从节点信息
            String sentinelSlavesCmd = cliPath +
                    " -h " + info.sentinelList.get(0) +
                    " -p " + info.sentinelPort +
                    " sentinel slaves mymaster";

            CommandLineItem sentinelSlavesItem = new CommandLineItem();
            sentinelSlavesItem.setLabel("获取主节点的从节点信息");
            sentinelSlavesItem.setValue(sentinelSlavesCmd);
            sentinelSlavesItem.setCommandResult("显示指定主节点(mymaster)的所有从节点信息");
            sentinelSlavesItem.setCommandPrompt(redisHomePrompt);
            commands.add(sentinelSlavesItem);
        }

        // 添加最后一个退出命令
        CommandLineItem exitCmd = new CommandLineItem();
        exitCmd.setLabel("退出Redis客户端");
        exitCmd.setValue("exit");
        exitCmd.setCommandResult(null);
        exitCmd.setCommandPrompt(redisPrompt);
        commands.add(exitCmd);

        return addFinalPrompt(commands, redisHome, hostname);
    }


}
