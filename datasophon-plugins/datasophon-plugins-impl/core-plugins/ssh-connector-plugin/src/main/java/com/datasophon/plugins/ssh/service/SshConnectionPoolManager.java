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

package com.datasophon.plugins.ssh.service;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * SSH连接池管理器（插件内部组件）
 * 
 * 使用Apache SSHJ + Commons Pool2的最佳实践组合
 * 严格限制在插件内部使用，主程序不直接访问
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Slf4j
public class SshConnectionPoolManager {
    
    // 连接池映射 <hostKey, pool>
    private final Map<String, GenericObjectPool<SSHClient>> connectionPools = new ConcurrentHashMap<>();
    
    // 连接池配置
    private GenericObjectPoolConfig<SSHClient> poolConfig;
    
    @PostConstruct
    public void init() {
        initPoolConfig();
        log.info("【插件内部】SSH连接池管理器初始化完成 - 使用Apache SSHJ + Commons Pool2");
    }
    
    @PreDestroy
    public void destroy() {
        log.info("【插件内部】关闭所有SSH连接池...");
        connectionPools.values().forEach(GenericObjectPool::close);
        connectionPools.clear();
        log.info("【插件内部】SSH连接池管理器已关闭");
    }
    
    /**
     * 获取SSH连接（插件内部方法）
     */
    public SSHClient borrowConnection(String hostIp, int port, String user, String password) throws Exception {
        String hostKey = buildHostKey(hostIp, port, user);
        GenericObjectPool<SSHClient> pool = getOrCreatePool(hostKey, hostIp, port, user, password);
        
        SSHClient client = pool.borrowObject();
        log.debug("【插件内部】借用SSH连接: {} (池状态: 活跃={}, 空闲={})", 
                hostKey, pool.getNumActive(), pool.getNumIdle());
        
        return client;
    }
    
    /**
     * 归还SSH连接（插件内部方法）
     */
    public void returnConnection(String hostIp, int port, String user, SSHClient client) {
        String hostKey = buildHostKey(hostIp, port, user);
        GenericObjectPool<SSHClient> pool = connectionPools.get(hostKey);
        
        if (pool != null && client != null) {
            pool.returnObject(client);
            log.debug("【插件内部】归还SSH连接: {} (池状态: 活跃={}, 空闲={})", 
                    hostKey, pool.getNumActive(), pool.getNumIdle());
        }
    }
    
    /**
     * 执行SSH命令 - 自动管理连接（插件内部方法）
     */
    public String executeCommand(String hostIp, int port, String user, String password, String command) throws Exception {
        SSHClient client = null;
        try {
            client = borrowConnection(hostIp, port, user, password);
            
            try (Session session = client.startSession()) {
                Session.Command cmd = session.exec(command);
                
                // 读取输出
                String output = org.apache.commons.io.IOUtils.toString(
                        cmd.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
                
                // 等待命令完成
                cmd.join(30, java.util.concurrent.TimeUnit.SECONDS);
                
                int exitCode = cmd.getExitStatus();
                if (exitCode != 0) {
                    String errorOutput = org.apache.commons.io.IOUtils.toString(
                            cmd.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8);
                    throw new RuntimeException("命令执行失败: 退出码=" + exitCode + ", 错误=" + errorOutput);
                }
                
                log.debug("【插件内部】SSH命令执行成功: {}@{}:{} -> {}", user, hostIp, port, command);
                return output;
            }
            
        } finally {
            if (client != null) {
                returnConnection(hostIp, port, user, client);
            }
        }
    }
    
    /**
     * 获取连接池状态（插件内部方法）
     */
    public Map<String, Object> getPoolStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        int totalActive = 0;
        int totalIdle = 0;
        int totalPools = connectionPools.size();
        
        Map<String, Map<String, Integer>> poolDetails = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, GenericObjectPool<SSHClient>> entry : connectionPools.entrySet()) {
            GenericObjectPool<SSHClient> pool = entry.getValue();
            int active = pool.getNumActive();
            int idle = pool.getNumIdle();
            
            totalActive += active;
            totalIdle += idle;
            
            Map<String, Integer> poolStat = new ConcurrentHashMap<>();
            poolStat.put("active", active);
            poolStat.put("idle", idle);
            poolStat.put("total", active + idle);
            poolDetails.put(entry.getKey(), poolStat);
        }
        
        stats.put("totalPools", totalPools);
        stats.put("totalActive", totalActive);
        stats.put("totalIdle", totalIdle);
        stats.put("totalConnections", totalActive + totalIdle);
        stats.put("poolDetails", poolDetails);
        
        return stats;
    }
    
    /**
     * 关闭指定主机的连接池（插件内部方法）
     */
    public void closePool(String hostIp, int port, String user) {
        String hostKey = buildHostKey(hostIp, port, user);
        GenericObjectPool<SSHClient> pool = connectionPools.remove(hostKey);
        
        if (pool != null) {
            pool.close();
            log.info("【插件内部】关闭SSH连接池: {}", hostKey);
        }
    }
    
    // ================== 私有方法 ==================
    
    private void initPoolConfig() {
        poolConfig = new GenericObjectPoolConfig<>();
        
        // 连接池大小配置
        poolConfig.setMaxTotal(50);              // 最大连接数
        poolConfig.setMaxIdle(10);               // 最大空闲连接数
        poolConfig.setMinIdle(2);                // 最小空闲连接数
        
        // 连接获取配置
        poolConfig.setBlockWhenExhausted(true);  // 连接耗尽时是否阻塞
        poolConfig.setMaxWait(Duration.ofSeconds(10)); // 最大等待时间
        
        // 连接验证配置
        poolConfig.setTestOnBorrow(true);        // 借用时验证
        poolConfig.setTestOnReturn(false);       // 归还时验证
        poolConfig.setTestWhileIdle(true);       // 空闲时验证
        
        // 空闲连接清理配置
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMinutes(1));  // 清理间隔
        poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(5));     // 最小空闲时间
        poolConfig.setNumTestsPerEvictionRun(3); // 每次清理测试连接数
        
        // 注意：连接泄漏检测功能在新版本中已移除或改变API
        // 在生产环境中可以考虑使用监控工具来检测连接泄漏
        
        log.info("【插件内部】SSH连接池配置完成: 最大连接={}, 最大空闲={}, 最小空闲={}", 
                poolConfig.getMaxTotal(), poolConfig.getMaxIdle(), poolConfig.getMinIdle());
    }
    
    private GenericObjectPool<SSHClient> getOrCreatePool(String hostKey, String hostIp, int port, String user, String password) {
        return connectionPools.computeIfAbsent(hostKey, key -> {
            SshConnectionFactory factory = new SshConnectionFactory(hostIp, port, user, password);
            GenericObjectPool<SSHClient> pool = new GenericObjectPool<>(factory, poolConfig);
            
            log.info("【插件内部】创建SSH连接池: {} (目标: {}@{}:{})", key, user, hostIp, port);
            return pool;
        });
    }
    
    private String buildHostKey(String hostIp, int port, String user) {
        return String.format("%s@%s:%d", user, hostIp, port);
    }
    
    // ================== 连接操作模板接口 ==================
    
    /**
     * SSH操作接口
     */
    @FunctionalInterface
    public interface SshOperation<T> {
        T execute(SSHClient sshClient) throws Exception;
    }
    
    /**
     * 执行SSH操作（模板方法）
     * 自动管理连接的借用和归还
     */
    public <T> T executeWithConnection(String hostIp, int port, String user, String password, 
                                     SshOperation<T> operation) throws Exception {
        SSHClient client = null;
        try {
            client = borrowConnection(hostIp, port, user, password);
            return operation.execute(client);
        } finally {
            if (client != null) {
                returnConnection(hostIp, port, user, client);
            }
        }
    }


}
