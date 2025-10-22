/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.plugins.impl.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH连接池管理器
 * 基于Apache Commons Pool2实现高性能SSH连接池
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-22
 */
public class SshConnectionPool {
    
    private static final Logger logger = LoggerFactory.getLogger(SshConnectionPool.class);
    
    // 全局连接池映射：host -> pool
    private static final Map<String, GenericObjectPool<SSHClient>> POOL_MAP = new ConcurrentHashMap<>();
    
    /**
     * SSH连接配置
     */
    public static class SshConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final String privateKey;
        private final int connectionTimeout;
        private final int commandTimeout;
        
        public SshConfig(String host, int port, String username, String password, 
                        String privateKey, int connectionTimeout, int commandTimeout) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.privateKey = privateKey;
            this.connectionTimeout = connectionTimeout;
            this.commandTimeout = commandTimeout;
        }
        
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getPrivateKey() { return privateKey; }
        public int getConnectionTimeout() { return connectionTimeout; }
        public int getCommandTimeout() { return commandTimeout; }
        
        public String getPoolKey() {
            return String.format("%s:%d@%s", username, port, host);
        }
    }
    
    /**
     * SSH连接工厂
     */
    private static class SshClientFactory extends BasePooledObjectFactory<SSHClient> {
        
        private final SshConfig config;
        
        public SshClientFactory(SshConfig config) {
            this.config = config;
        }
        
        @Override
        public SSHClient create() throws Exception {
            logger.debug("创建新的SSH连接: {}", config.getPoolKey());
            
            var client = new SSHClient();
            
            // 配置连接参数
            client.setTimeout(config.getConnectionTimeout());
            client.setConnectTimeout(config.getConnectionTimeout());
            
            // 跳过主机密钥验证（生产环境建议使用已知主机验证）
            client.addHostKeyVerifier(new PromiscuousVerifier());
            
            // 连接到主机
            client.connect(config.getHost(), config.getPort());
            
            // 认证
            if (config.getPrivateKey() != null && !config.getPrivateKey().isEmpty()) {
                // 私钥认证
                logger.debug("使用私钥认证: {}", config.getUsername());
                client.authPassword(config.getUsername(), config.getPassword()); // 先用密码认证，私钥认证需要密钥文件
            } else if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                // 密码认证
                logger.debug("使用密码认证: {}", config.getUsername());
                client.authPassword(config.getUsername(), config.getPassword());
            } else {
                throw new IllegalArgumentException("未提供认证信息");
            }
            
            logger.info("SSH连接创建成功: {}", config.getPoolKey());
            return client;
        }
        
        @Override
        public PooledObject<SSHClient> wrap(SSHClient client) {
            return new DefaultPooledObject<>(client);
        }
        
        @Override
        public void destroyObject(PooledObject<SSHClient> pooledObject) throws Exception {
            var client = pooledObject.getObject();
            if (client != null && client.isConnected()) {
                logger.debug("销毁SSH连接: {}", config.getPoolKey());
                try {
                    client.disconnect();
                } catch (IOException e) {
                    logger.warn("关闭SSH连接时出错: {}", e.getMessage());
                }
            }
        }
        
        @Override
        public boolean validateObject(PooledObject<SSHClient> pooledObject) {
            var client = pooledObject.getObject();
            var isValid = client != null && client.isConnected() && client.isAuthenticated();
            if (!isValid) {
                logger.debug("SSH连接验证失败: {}", config.getPoolKey());
            }
            return isValid;
        }
        
        @Override
        public void activateObject(PooledObject<SSHClient> pooledObject) throws Exception {
            // 从池中取出连接时的操作
            logger.trace("激活SSH连接: {}", config.getPoolKey());
        }
        
        @Override
        public void passivateObject(PooledObject<SSHClient> pooledObject) throws Exception {
            // 返回连接到池时的操作
            logger.trace("归还SSH连接: {}", config.getPoolKey());
        }
    }
    
    /**
     * 获取或创建连接池
     */
    public static GenericObjectPool<SSHClient> getOrCreatePool(SshConfig config) {
        var poolKey = config.getPoolKey();
        return POOL_MAP.computeIfAbsent(poolKey, key -> {
            logger.info("创建新的SSH连接池: {}", poolKey);
            
            var poolConfig = new GenericObjectPoolConfig<SSHClient>();
            poolConfig.setMaxTotal(10); // 最大连接数
            poolConfig.setMaxIdle(5);   // 最大空闲连接
            poolConfig.setMinIdle(2);   // 最小空闲连接
            poolConfig.setMaxWait(Duration.ofSeconds(30)); // 最大等待时间
            poolConfig.setTestOnBorrow(true);  // 借用时测试
            poolConfig.setTestWhileIdle(true); // 空闲时测试
            poolConfig.setTimeBetweenEvictionRuns(Duration.ofMinutes(1)); // 驱逐检查间隔
            poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(5)); // 最小空闲驱逐时间
            poolConfig.setJmxEnabled(false); // 禁用JMX
            
            var factory = new SshClientFactory(config);
            return new GenericObjectPool<>(factory, poolConfig);
        });
    }
    
    /**
     * 从连接池借用连接
     */
    public static SSHClient borrowClient(SshConfig config) throws Exception {
        var pool = getOrCreatePool(config);
        return pool.borrowObject();
    }
    
    /**
     * 归还连接到连接池
     */
    public static void returnClient(SshConfig config, SSHClient client) {
        if (client == null) {
            return;
        }
        
        var poolKey = config.getPoolKey();
        var pool = POOL_MAP.get(poolKey);
        if (pool != null) {
            pool.returnObject(client);
        } else {
            logger.warn("连接池不存在，直接关闭连接: {}", poolKey);
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
            } catch (IOException e) {
                logger.error("关闭SSH连接失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 销毁无效连接
     */
    public static void invalidateClient(SshConfig config, SSHClient client) {
        if (client == null) {
            return;
        }
        
        var poolKey = config.getPoolKey();
        var pool = POOL_MAP.get(poolKey);
        if (pool != null) {
            try {
                pool.invalidateObject(client);
            } catch (Exception e) {
                logger.error("销毁SSH连接失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 关闭指定主机的连接池
     */
    public static void closePool(String poolKey) {
        var pool = POOL_MAP.remove(poolKey);
        if (pool != null) {
            logger.info("关闭SSH连接池: {}", poolKey);
            pool.close();
        }
    }
    
    /**
     * 关闭所有连接池
     */
    public static void closeAllPools() {
        logger.info("关闭所有SSH连接池，总数: {}", POOL_MAP.size());
        POOL_MAP.forEach((key, pool) -> {
            try {
                pool.close();
            } catch (Exception e) {
                logger.error("关闭连接池失败: {}", key, e);
            }
        });
        POOL_MAP.clear();
    }
    
    /**
     * 获取连接池统计信息
     */
    public static Map<String, Object> getPoolStats(String poolKey) {
        var pool = POOL_MAP.get(poolKey);
        if (pool == null) {
            return Map.of("error", "Pool not found");
        }
        
        return Map.of(
            "poolKey", poolKey,
            "active", pool.getNumActive(),
            "idle", pool.getNumIdle(),
            "waiting", pool.getNumWaiters(),
            "created", pool.getCreatedCount(),
            "borrowed", pool.getBorrowedCount(),
            "returned", pool.getReturnedCount(),
            "destroyed", pool.getDestroyedCount()
        );
    }
    
    /**
     * 获取全局统计信息
     */
    public static Map<String, Object> getGlobalStats() {
        var totalActive = POOL_MAP.values().stream().mapToInt(GenericObjectPool::getNumActive).sum();
        var totalIdle = POOL_MAP.values().stream().mapToInt(GenericObjectPool::getNumIdle).sum();
        
        return Map.of(
            "poolCount", POOL_MAP.size(),
            "totalActive", totalActive,
            "totalIdle", totalIdle,
            "totalConnections", totalActive + totalIdle
        );
    }
}

