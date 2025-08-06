package com.datasophon.plugins.impl.ssh;

import com.datasophon.common.model.HostInfo;
import com.datasophon.plugins.api.model.CommandResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高性能SSH连接池
 * 基于Apache Commons Pool2实现
 * 
 * @author DataSophon Team
 */
@Slf4j
public class HighPerformanceSshPool extends GenericObjectPool<ClientSession> {
    
    private final SshPoolConfig config;
    private final AtomicLong borrowCount = new AtomicLong(0);
    private final AtomicLong returnCount = new AtomicLong(0);
    private final AtomicLong createCount = new AtomicLong(0);
    private final AtomicLong destroyCount = new AtomicLong(0);
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    
    public HighPerformanceSshPool(SshPoolConfig config) throws Exception {
        super(new SshConnectionFactory(config), createPoolConfig(config));
        this.config = config;
        
        log.info("创建SSH连接池: {} - 最大连接数={}, 最大空闲={}, 最小空闲={}", 
                config.getHostInfo().getIp(), 
                config.getMaxTotal(), 
                config.getMaxIdle(), 
                config.getMinIdle());
        
        // 预热连接池
        preparePool();
    }
    
    @Override
    public ClientSession borrowObject() throws Exception {
        borrowCount.incrementAndGet();
        
        long startTime = System.currentTimeMillis();
        
        try {
            ClientSession session = super.borrowObject();
            
            if (session != null) {
                hitCount.incrementAndGet();
                long waitTime = System.currentTimeMillis() - startTime;
                
                if (waitTime > 1000) {
                    log.warn("SSH连接获取耗时较长: {}ms, 主机: {}", 
                            waitTime, config.getHostInfo().getIp());
                }
                
                log.debug("成功获取SSH连接: {}, 等待时间: {}ms", 
                        config.getHostInfo().getIp(), waitTime);
            } else {
                missCount.incrementAndGet();
            }
            
            return session;
            
        } catch (Exception e) {
            missCount.incrementAndGet();
            log.error("获取SSH连接失败: {}", config.getHostInfo().getIp(), e);
            throw e;
        }
    }
    
    @Override
    public void returnObject(ClientSession session) {
        returnCount.incrementAndGet();
        
        try {
            super.returnObject(session);
            log.debug("归还SSH连接: {}", config.getHostInfo().getIp());
            
        } catch (Exception e) {
            log.error("归还SSH连接失败: {}", config.getHostInfo().getIp(), e);
        }
    }
    
    /**
     * 获取连接池统计信息
     */
    public SshPoolStatistics getStatistics() {
        return SshPoolStatistics.builder()
                .hostIp(config.getHostInfo().getIp())
                .maxTotal(getMaxTotal())
                .maxIdle(getMaxIdle())
                .minIdle(getMinIdle())
                .activeCount(getNumActive())
                .idleCount(getNumIdle())
                .totalCount(getCreatedCount())
                .borrowCount(borrowCount.get())
                .returnCount(returnCount.get())
                .createCount(createCount.get())
                .destroyCount(destroyCount.get())
                .hitCount(hitCount.get())
                .missCount(missCount.get())
                .hitRate(calculateHitRate())
                .build();
    }
    
    /**
     * 计算命中率
     */
    private double calculateHitRate() {
        long total = hitCount.get() + missCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) hitCount.get() / total * 100.0;
    }
    
    /**
     * 预热连接池
     */
    public void preparePool() throws Exception {
        int minIdle = config.getMinIdle();
        if (minIdle > 0) {
            log.info("预热SSH连接池: {}, 预创建 {} 个连接", 
                    config.getHostInfo().getIp(), minIdle);
            
            preparePool();
        }
    }
    
    /**
     * 创建池配置
     */
    private static GenericObjectPoolConfig<ClientSession> createPoolConfig(SshPoolConfig config) {
        GenericObjectPoolConfig<ClientSession> poolConfig = new GenericObjectPoolConfig<>();
        
        // 基本配置
        poolConfig.setMaxTotal(config.getMaxTotal());
        poolConfig.setMaxIdle(config.getMaxIdle());
        poolConfig.setMinIdle(config.getMinIdle());
        poolConfig.setMaxWait(java.time.Duration.ofMillis(config.getMaxWaitMillis()));
        
        // 测试配置
        poolConfig.setTestOnBorrow(config.isTestOnBorrow());
        poolConfig.setTestOnReturn(config.isTestOnReturn());
        poolConfig.setTestWhileIdle(config.isTestWhileIdle());
        
        // 清理配置
        poolConfig.setTimeBetweenEvictionRuns(java.time.Duration.ofMillis(config.getTimeBetweenEvictionRunsMillis()));
        poolConfig.setMinEvictableIdleTime(java.time.Duration.ofMillis(config.getMinEvictableIdleTimeMillis()));
        poolConfig.setSoftMinEvictableIdleTime(java.time.Duration.ofMillis(config.getSoftMinEvictableIdleTimeMillis()));
        
        // 阻塞配置
        poolConfig.setBlockWhenExhausted(true);
        
        // LIFO配置 - 后进先出，提高缓存命中率
        poolConfig.setLifo(true);
        
        return poolConfig;
    }
    
    /**
     * SSH连接工厂
     */
    private static class SshConnectionFactory extends BasePooledObjectFactory<ClientSession> {
        
        private final SshPoolConfig config;
        private final AtomicLong createCounter = new AtomicLong(0);
        
        public SshConnectionFactory(SshPoolConfig config) {
            this.config = config;
        }
        
        @Override
        public ClientSession create() throws Exception {
            long startTime = System.currentTimeMillis();
            
            try {
                ClientSession session = createSshConnection(config.getHostInfo());
                
                if (session == null || !session.isOpen()) {
                    throw new RuntimeException("SSH连接创建失败");
                }
                
                long createTime = System.currentTimeMillis() - startTime;
                long count = createCounter.incrementAndGet();
                
                log.info("创建SSH连接成功: {} [{}], 耗时: {}ms", 
                        config.getHostInfo().getIp(), count, createTime);
                
                return session;
                
            } catch (Exception e) {
                log.error("创建SSH连接失败: {}", config.getHostInfo().getIp(), e);
                throw e;
            }
        }
        
        @Override
        public PooledObject<ClientSession> wrap(ClientSession session) {
            return new DefaultPooledObject<>(session);
        }
        
        @Override
        public boolean validateObject(PooledObject<ClientSession> pooledObject) {
            ClientSession session = pooledObject.getObject();
            
            try {
                // 检查连接是否有效
                if (session == null || !session.isOpen()) {
                    log.debug("SSH连接无效: {} - 连接已关闭", config.getHostInfo().getIp());
                    return false;
                }
                
                // 发送心跳测试连接
                try {
                    CommandResult testResult = executeCommand(session, "echo connection_test");
                    boolean isValid = testResult.exitCode() == 0 && 
                                     testResult.output() != null && 
                                     testResult.output().trim().contains("connection_test");
                    
                    if (!isValid) {
                        log.debug("SSH连接心跳测试失败: {}", config.getHostInfo().getIp());
                    }
                    
                    return isValid;
                    
                } catch (Exception e) {
                    log.debug("SSH连接验证异常: {} - {}", config.getHostInfo().getIp(), e.getMessage());
                    return false;
                }
                
            } catch (Exception e) {
                log.warn("验证SSH连接时发生异常: {}", config.getHostInfo().getIp(), e);
                return false;
            }
        }
        
        @Override
        public void destroyObject(PooledObject<ClientSession> pooledObject) throws Exception {
            ClientSession session = pooledObject.getObject();
            
            try {
                if (session != null && session.isOpen()) {
                    session.close();
                    log.debug("销毁SSH连接: {}", config.getHostInfo().getIp());
                }
            } catch (Exception e) {
                log.warn("销毁SSH连接时发生异常: {}", config.getHostInfo().getIp(), e);
            }
        }
        
        @Override
        public void passivateObject(PooledObject<ClientSession> pooledObject) throws Exception {
            // 归还连接时的处理（可选）
            log.trace("钝化SSH连接: {}", config.getHostInfo().getIp());
        }
        
        @Override
        public void activateObject(PooledObject<ClientSession> pooledObject) throws Exception {
            // 激活连接时的处理（可选）
            log.trace("激活SSH连接: {}", config.getHostInfo().getIp());
        }
        
        /**
         * 创建SSH连接
         */
        private ClientSession createSshConnection(HostInfo hostInfo) throws IOException {
            if (hostInfo == null) {
                throw new IllegalArgumentException("主机信息不能为空");
            }

            String ip = hostInfo.getIp();
            Integer port = hostInfo.getSshPort();
            String username = hostInfo.getSshUser();
            String password = hostInfo.getSshPassword();

            if (ip == null || port == null || username == null || password == null) {
                throw new IllegalArgumentException("SSH连接信息不完整");
            }

            SshClient client = SshClient.setUpDefaultClient();
            client.start();

            try {
                ConnectFuture connectFuture = client.connect(username, ip, port);
                ClientSession session = connectFuture.verify(config.getConnectTimeout()).getSession();

                AuthFuture authFuture = session.auth();
                session.addPasswordIdentity(password);
                
                authFuture.verify(config.getAuthTimeout());

                if (session.isAuthenticated()) {
                    return session;
                } else {
                    session.close();
                    throw new IOException("SSH认证失败");
                }
            } catch (Exception e) {
                client.stop();
                throw new IOException("SSH连接失败: " + e.getMessage(), e);
            }
        }
        
        /**
         * 执行SSH命令
         */
        private CommandResult executeCommand(ClientSession session, String command) throws IOException {
            if (session == null || !session.isOpen()) {
                return new CommandResult(command, -1, "", "SSH会话无效");
            }

            try (ChannelExec channel = session.createExecChannel(command)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayOutputStream err = new ByteArrayOutputStream();
                
                channel.setOut(out);
                channel.setErr(err);
                
                channel.open();
                
                // 等待命令执行完成
                channel.waitFor(java.util.EnumSet.of(
                    org.apache.sshd.client.channel.ClientChannelEvent.CLOSED), 
                    TimeUnit.SECONDS.toMillis(30));
                
                Integer exitCode = channel.getExitStatus();
                String output = out.toString(StandardCharsets.UTF_8);
                String error = err.toString(StandardCharsets.UTF_8);
                
                return new CommandResult(command, 
                    exitCode != null ? exitCode : -1, 
                    output != null ? output : "", 
                    error != null ? error : "");
                    
            } catch (Exception e) {
                return new CommandResult(command, -1, "", "命令执行异常: " + e.getMessage());
            }
        }
    }
}