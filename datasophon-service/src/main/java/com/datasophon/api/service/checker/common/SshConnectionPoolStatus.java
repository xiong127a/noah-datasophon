package com.datasophon.api.service.checker.common;

import lombok.Data;

/**
 * SSH连接池状态信息
 */
@Data
public class SshConnectionPoolStatus {

    /**
     * 连接池是否启用
     */
    private boolean enabled;

    /**
     * 连接池大小
     */
    private int connectionPoolSize;

    /**
     * 连接池最大大小
     */
    private int maxPoolSize;

    /**
     * 清理任务是否活动
     */
    private boolean cleanupTaskActive;

    /**
     * 最后清理时间
     */
    private String lastCleanupTime;

    /**
     * 清理间隔（毫秒）
     */
    private long cleanupIntervalMs;

    /**
     * 清理间隔（可读格式）
     */
    private String cleanupInterval;

    /**
     * 空闲连接超时时间（毫秒）
     */
    private long idleTimeoutMs;

    /**
     * 空闲连接超时时间（可读格式）
     */
    private String idleTimeout;

    /**
     * 缓存命中率
     */
    private int sessionCacheHitRate;
}