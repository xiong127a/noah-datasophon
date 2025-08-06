package com.datasophon.plugins.impl.ssh;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.model.HostInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.sshd.client.session.ClientSession;

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
    private void preparePool() throws Exception {
        int minIdle = config.getMinIdle();
        if (minIdle > 0) {
            log.info("预热SSH连接池: {}, 预创建 {} 个连接", 
                    config.getHostInfo().getIp(), minIdle);
            
            preparePool(minIdle);
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
        poolConfig.setMaxWaitMillis(config.getMaxWaitMillis());
        
        // 测试配置
        poolConfig.setTestOnBorrow(config.isTestOnBorrow());
        poolConfig.setTestOnReturn(config.isTestOnReturn());
        poolConfig.setTestWhileIdle(config.isTestWhileIdle());
        
        // 清理配置
        poolConfig.setTimeBetweenEvictionRunsMillis(config.getTimeBetweenEvictionRunsMillis());
        poolConfig.setMinEvictableIdleTimeMillis(config.getMinEvictableIdleTimeMillis());
        poolConfig.setSoftMinEvictableIdleTimeMillis(config.getSoftMinEvictableIdleTimeMillis());
        
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
                ClientSession session = MinaUtils.openConnectionWithPassword(config.getHostInfo());
                
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
                    String testResult = MinaUtils.execCommand(session, "echo connection_test").getOutput();
                    boolean isValid = testResult != null && testResult.trim().contains("connection_test");
                    
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
    }
}