package com.datasophon.api.workflow.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批量主机检查结果模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class BatchCheckResult {
    
    /**
     * 批量请求ID
     */
    private String batchRequestId;
    
    /**
     * 单个主机检查结果列表
     */
    private List<HostCheckResult> hostResults;
    
    /**
     * 整体状态
     */
    private WorkflowStatus overallStatus;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 总执行时间（毫秒）
     */
    private long totalExecutionTimeMs;
    
    /**
     * 总主机数量
     */
    private int totalHostCount;
    
    /**
     * 成功的主机数量
     */
    private int successHostCount;
    
    /**
     * 失败的主机数量
     */
    private int failedHostCount;
    
    /**
     * 错误的主机数量
     */
    private int errorHostCount;
    
    /**
     * 跳过的主机数量
     */
    private int skippedHostCount;
    
    /**
     * 执行摘要
     */
    private String summary;
    
    /**
     * 统计信息
     */
    private Map<String, Object> statistics;
    
    /**
     * 计算主机成功率
     */
    public double getHostSuccessRate() {
        if (totalHostCount == 0) {
            return 0.0;
        }
        return (double) successHostCount / totalHostCount * 100.0;
    }
    
    /**
     * 检查是否全部成功
     */
    public boolean isAllHostsSuccess() {
        return failedHostCount == 0 && errorHostCount == 0;
    }
    
    /**
     * 获取执行时间（秒）
     */
    public double getExecutionTimeSeconds() {
        return totalExecutionTimeMs / 1000.0;
    }
    
    /**
     * 获取平均每台主机执行时间
     */
    public double getAverageHostExecutionTime() {
        if (totalHostCount == 0) {
            return 0.0;
        }
        return (double) totalExecutionTimeMs / totalHostCount;
    }
}