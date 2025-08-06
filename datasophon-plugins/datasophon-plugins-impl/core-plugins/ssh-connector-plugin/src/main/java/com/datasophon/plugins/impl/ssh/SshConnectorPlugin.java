package com.datasophon.plugins.impl.ssh;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.pf4j.Extension;
import org.pf4j.Plugin;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH连接器插件
 * 提供高性能的SSH连接池服务
 * 
 * @author DataSophon Team
 */
@Slf4j
@Extension
public class SshConnectorPlugin extends Plugin implements HostCheckerPlugin {
    
    private static final String PLUGIN_ID = "ssh-connector";
    private static final String PLUGIN_VERSION = "1.0.0";
    
    // SSH连接池管理器 - 每个主机一个连接池
    private final ConcurrentHashMap<String, HighPerformanceSshPool> connectionPools = new ConcurrentHashMap<>();
    
    @Override
    public Set<OsType> getSupportedOperatingSystems() {
        return Set.of(OsType.LINUX, OsType.WINDOWS, OsType.MACOS); // 支持所有操作系统
    }
    
    @Override
    public int getPriority() {
        return 1; // 最高优先级，因为其他插件都依赖SSH连接
    }
    
    @Override
    public CompletableFuture<CheckResult> executeCheck(HostCheckContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("执行SSH连接池检查，主机: {}", context.getHostInfo().getIp());
                
                String hostKey = generateHostKey(context.getHostInfo());
                HighPerformanceSshPool pool = getOrCreatePool(context.getHostInfo());
                
                // 测试连接池
                SshPoolStatistics stats = pool.getStatistics();
                
                return CheckResult.success(PLUGIN_ID, String.format(
                    "SSH连接池正常: 活跃连接=%d, 空闲连接=%d, 总连接=%d, 命中率=%.1f%%",
                    stats.getActiveCount(),
                    stats.getIdleCount(),
                    stats.getTotalCount(),
                    stats.getHitRate()
                ));
                
            } catch (Exception e) {
                log.error("SSH连接池检查失败", e);
                return CheckResult.error(PLUGIN_ID, "SSH连接池检查失败: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public boolean canExecute(HostCheckContext context) {
        return context.getHostInfo() != null;
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return PluginMetadata.builder()
                .pluginId(PLUGIN_ID)
                .name("SSH连接池")
                .version(PLUGIN_VERSION)
                .description("高性能SSH连接池服务，基于Apache Commons Pool2")
                .author("DataSophon Team")
                .category("infrastructure")
                .supportedOs(Set.of("linux", "windows", "macos"))
                .tags(Set.of("ssh", "connection-pool", "infrastructure"))
                .corePlugin(true)
                .build();
    }
    
    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }
    
    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }
    
    @Override
    public void initialize() {
        log.info("初始化SSH连接池插件...");
        // 在这里可以进行全局初始化
        log.info("SSH连接池插件初始化完成");
    }
    
    @Override
    public void cleanup() {
        log.info("清理SSH连接池插件...");
        
        // 关闭所有连接池
        for (HighPerformanceSshPool pool : connectionPools.values()) {
            try {
                pool.close();
            } catch (Exception e) {
                log.error("关闭SSH连接池失败", e);
            }
        }
        
        connectionPools.clear();
        log.info("SSH连接池插件清理完成");
    }
    
    /**
     * 获取SSH连接（供其他插件调用）
     */
    public ClientSession borrowConnection(HostCheckContext context) throws Exception {
        HighPerformanceSshPool pool = getOrCreatePool(context.getHostInfo());
        return pool.borrowObject();
    }
    
    /**
     * 归还SSH连接（供其他插件调用）
     */
    public void returnConnection(HostCheckContext context, ClientSession session) {
        try {
            String hostKey = generateHostKey(context.getHostInfo());
            HighPerformanceSshPool pool = connectionPools.get(hostKey);
            if (pool != null) {
                pool.returnObject(session);
            }
        } catch (Exception e) {
            log.error("归还SSH连接失败", e);
        }
    }
    
    /**
     * 获取连接池统计信息
     */
    public SshPoolStatistics getPoolStatistics(HostCheckContext context) {
        String hostKey = generateHostKey(context.getHostInfo());
        HighPerformanceSshPool pool = connectionPools.get(hostKey);
        return pool != null ? pool.getStatistics() : new SshPoolStatistics();
    }
    
    /**
     * 获取或创建连接池
     */
    private HighPerformanceSshPool getOrCreatePool(com.datasophon.common.model.HostInfo hostInfo) {
        String hostKey = generateHostKey(hostInfo);
        
        return connectionPools.computeIfAbsent(hostKey, key -> {
            log.info("为主机创建SSH连接池: {}", hostInfo.getIp());
            
            SshPoolConfig config = SshPoolConfig.builder()
                    .hostInfo(hostInfo)
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
                log.error("创建SSH连接池失败: {}", hostInfo.getIp(), e);
                throw new RuntimeException("创建SSH连接池失败", e);
            }
        });
    }
    
    /**
     * 生成主机唯一键
     */
    private String generateHostKey(com.datasophon.common.model.HostInfo hostInfo) {
        return String.format("%s:%d:%s", 
                hostInfo.getIp(), 
                hostInfo.getSshPort(), 
                hostInfo.getSshUser());
    }
}