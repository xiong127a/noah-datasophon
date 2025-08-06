package com.datasophon.plugins.impl.ssh;

import com.datasophon.common.model.HostInfo;
import lombok.Builder;
import lombok.Data;

/**
 * SSH连接池配置
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class SshPoolConfig {
    
    /**
     * 主机信息
     */
    private HostInfo hostInfo;
    
    /**
     * 连接池最大总连接数
     */
    @Builder.Default
    private int maxTotal = 10;
    
    /**
     * 最大空闲连接数
     */
    @Builder.Default
    private int maxIdle = 5;
    
    /**
     * 最小空闲连接数
     */
    @Builder.Default
    private int minIdle = 2;
    
    /**
     * 获取连接时的最大等待时间（毫秒）
     */
    @Builder.Default
    private long maxWaitMillis = 30000;
    
    /**
     * 借用连接时是否测试
     */
    @Builder.Default
    private boolean testOnBorrow = true;
    
    /**
     * 归还连接时是否测试
     */
    @Builder.Default
    private boolean testOnReturn = true;
    
    /**
     * 空闲时是否测试连接
     */
    @Builder.Default
    private boolean testWhileIdle = true;
    
    /**
     * 空闲连接清理任务运行间隔（毫秒）
     */
    @Builder.Default
    private long timeBetweenEvictionRunsMillis = 30000;
    
    /**
     * 连接空闲多长时间后可被清理（毫秒）
     */
    @Builder.Default
    private long minEvictableIdleTimeMillis = 300000; // 5分钟
    
    /**
     * 软最小空闲时间（毫秒）
     */
    @Builder.Default
    private long softMinEvictableIdleTimeMillis = 180000; // 3分钟
    
    /**
     * 连接超时时间（毫秒）
     */
    @Builder.Default
    private int connectTimeout = 10000;
    
    /**
     * 认证超时时间（毫秒）
     */
    @Builder.Default
    private int authTimeout = 10000;
    
    /**
     * 心跳间隔（毫秒）
     */
    @Builder.Default
    private long heartbeatInterval = 60000; // 1分钟
    
    /**
     * 是否启用压缩
     */
    @Builder.Default
    private boolean compressionEnabled = true;
    
    /**
     * Keep-Alive设置
     */
    @Builder.Default
    private boolean keepAliveEnabled = true;
    
    /**
     * Keep-Alive间隔（秒）
     */
    @Builder.Default
    private int keepAliveInterval = 60;
    
    /**
     * Keep-Alive计数
     */
    @Builder.Default
    private int keepAliveCountMax = 3;
}