package com.datasophon.plugins.impl.ssh;

import lombok.Builder;
import lombok.Data;

/**
 * SSH连接池统计信息
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class SshPoolStatistics {
    
    /**
     * 主机IP
     */
    private String hostIp;
    
    /**
     * 最大总连接数
     */
    private int maxTotal;
    
    /**
     * 最大空闲连接数
     */
    private int maxIdle;
    
    /**
     * 最小空闲连接数
     */
    private int minIdle;
    
    /**
     * 当前活跃连接数
     */
    private int activeCount;
    
    /**
     * 当前空闲连接数
     */
    private int idleCount;
    
    /**
     * 总创建连接数
     */
    private long totalCount;
    
    /**
     * 借用连接次数
     */
    private long borrowCount;
    
    /**
     * 归还连接次数
     */
    private long returnCount;
    
    /**
     * 创建连接次数
     */
    private long createCount;
    
    /**
     * 销毁连接次数
     */
    private long destroyCount;
    
    /**
     * 命中次数
     */
    private long hitCount;
    
    /**
     * 未命中次数
     */
    private long missCount;
    
    /**
     * 命中率（百分比）
     */
    private double hitRate;
    
    /**
     * 获取连接池使用率
     */
    public double getUsageRate() {
        if (maxTotal == 0) {
            return 0.0;
        }
        return (double) activeCount / maxTotal * 100.0;
    }
    
    /**
     * 获取空闲率
     */
    public double getIdleRate() {
        if (maxTotal == 0) {
            return 0.0;
        }
        return (double) idleCount / maxTotal * 100.0;
    }
    
    /**
     * 获取连接复用率
     */
    public double getReuseRate() {
        if (createCount == 0) {
            return 0.0;
        }
        return (double) borrowCount / createCount;
    }
    
    /**
     * 是否健康
     */
    public boolean isHealthy() {
        // 连接池健康标准：
        // 1. 活跃连接数没有达到上限
        // 2. 命中率超过50%
        // 3. 有空闲连接可用
        return activeCount < maxTotal && hitRate > 50.0 && idleCount > 0;
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (isHealthy()) {
            return "健康";
        } else if (activeCount >= maxTotal) {
            return "连接池已满";
        } else if (hitRate < 50.0) {
            return "命中率过低";
        } else if (idleCount == 0) {
            return "无空闲连接";
        } else {
            return "异常";
        }
    }
    
    @Override
    public String toString() {
        return String.format(
                "SSH连接池[%s]: 活跃=%d/%d, 空闲=%d, 命中率=%.1f%%, 状态=%s",
                hostIp, activeCount, maxTotal, idleCount, hitRate, getStatusDescription()
        );
    }
}