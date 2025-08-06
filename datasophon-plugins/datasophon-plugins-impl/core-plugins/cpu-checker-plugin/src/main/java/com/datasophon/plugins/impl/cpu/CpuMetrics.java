package com.datasophon.plugins.impl.cpu;

import lombok.Builder;
import lombok.Data;

/**
 * CPU指标数据模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class CpuMetrics {
    
    /**
     * CPU使用率（百分比）
     */
    private double cpuUsage;
    
    /**
     * 负载平均值（1分钟，5分钟，15分钟）
     */
    private double[] loadAverage;
    
    /**
     * CPU核心数
     */
    private int cpuCores;
    
    /**
     * CPU型号
     */
    private String cpuModel;
    
    /**
     * CPU架构
     */
    private String architecture;
    
    /**
     * CPU主频
     */
    private String frequency;
    
    /**
     * 获取1分钟负载平均值
     */
    public double getLoad1Min() {
        return loadAverage != null && loadAverage.length > 0 ? loadAverage[0] : 0.0;
    }
    
    /**
     * 获取5分钟负载平均值
     */
    public double getLoad5Min() {
        return loadAverage != null && loadAverage.length > 1 ? loadAverage[1] : 0.0;
    }
    
    /**
     * 获取15分钟负载平均值
     */
    public double getLoad15Min() {
        return loadAverage != null && loadAverage.length > 2 ? loadAverage[2] : 0.0;
    }
    
    /**
     * 计算每核心负载
     */
    public double getLoadPerCore() {
        return cpuCores > 0 ? getLoad1Min() / cpuCores : 0.0;
    }
    
    /**
     * 检查负载是否正常
     */
    public boolean isLoadNormal() {
        return getLoadPerCore() < 1.0; // 每核心负载小于1.0被认为是正常的
    }
}