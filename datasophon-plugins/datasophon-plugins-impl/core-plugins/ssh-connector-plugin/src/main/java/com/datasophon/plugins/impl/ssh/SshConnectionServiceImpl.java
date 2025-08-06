package com.datasophon.plugins.impl.ssh;

import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH连接服务实现
 * 
 * @author DataSophon Team
 */
@Service
@Slf4j
public class SshConnectionServiceImpl implements SshConnectionService {
    
    // SSH连接池管理器 - 每个主机一个连接池
    private final ConcurrentHashMap<String, HighPerformanceSshPool> connectionPools = new ConcurrentHashMap<>();
    
    @Override
    public ClientSession borrowConnection(HostCheckContext context) throws Exception {
        HighPerformanceSshPool pool = getOrCreatePool(context);
        return pool.borrowObject();
    }
    
    @Override
    public void returnConnection(HostCheckContext context, ClientSession session) {
        try {
            String hostKey = generateHostKey(context);
            HighPerformanceSshPool pool = connectionPools.get(hostKey);
            if (pool != null) {
                pool.returnObject(session);
            }
        } catch (Exception e) {
            log.error("归还SSH连接失败", e);
        }
    }
    
    @Override
    public String getPoolStatistics(HostCheckContext context) {
        String hostKey = generateHostKey(context);
        HighPerformanceSshPool pool = connectionPools.get(hostKey);
        if (pool != null) {
            SshPoolStatistics stats = pool.getStatistics();
            return stats.toString();
        }
        return "连接池不存在";
    }
    
    @Override
    public boolean isPoolHealthy(HostCheckContext context) {
        String hostKey = generateHostKey(context);
        HighPerformanceSshPool pool = connectionPools.get(hostKey);
        if (pool != null) {
            SshPoolStatistics stats = pool.getStatistics();
            return stats.isHealthy();
        }
        return false;
    }
    
    /**
     * 获取或创建连接池
     */
    private HighPerformanceSshPool getOrCreatePool(HostCheckContext context) throws Exception {
        String hostKey = generateHostKey(context);
        
        return connectionPools.computeIfAbsent(hostKey, key -> {
            log.info("为主机创建SSH连接池: {}", context.getHostInfo().getIp());
            
            SshPoolConfig config = SshPoolConfig.builder()
                    .hostInfo(context.getHostInfo())
                    .maxTotal(10)           // 最大连接数
                    .maxIdle(5)             // 最大空闲连接
                    .minIdle(2)             // 最小空闲连接
                    .maxWaitMillis(30000)   // 最大等待时间
                    .testOnBorrow(true)     // 借用时测试
                    .testOnReturn(true)     // 归还时测试
                    .testWhileIdle(true)    // 空闲时测试
                    .timeBetweenEvictionRunsMillis(30000)  // 清理任务间隔
                    .minEvictableIdleTimeMillis(300000)    // 最小空闲时间
                    .softMinEvictableIdleTimeMillis(180000) // 软最小空闲时间
                    .build();
            
            try {
                return new HighPerformanceSshPool(config);
            } catch (Exception e) {
                log.error("创建SSH连接池失败: {}", context.getHostInfo().getIp(), e);
                throw new RuntimeException("创建SSH连接池失败", e);
            }
        });
    }
    
    /**
     * 生成主机唯一键
     */
    private String generateHostKey(HostCheckContext context) {
        com.datasophon.common.model.HostInfo hostInfo = context.getHostInfo();
        return String.format("%s:%d:%s", 
                hostInfo.getIp(), 
                hostInfo.getSshPort(), 
                hostInfo.getSshUser());
    }
    
    /**
     * 清理所有连接池
     */
    public void shutdown() {
        log.info("关闭所有SSH连接池...");
        
        for (HighPerformanceSshPool pool : connectionPools.values()) {
            try {
                pool.close();
            } catch (Exception e) {
                log.error("关闭SSH连接池失败", e);
            }
        }
        
        connectionPools.clear();
        log.info("所有SSH连接池已关闭");
    }
}