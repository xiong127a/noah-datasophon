package com.datasophon.api.workflow.model;

import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.OsInfo;
import com.datasophon.common.model.HostInfo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 主机检查结果模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class HostCheckResult {
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 主机信息
     */
    private HostInfo hostInfo;
    
    /**
     * 操作系统信息
     */
    private OsInfo osInfo;
    
    /**
     * 检查结果列表
     */
    private List<CheckResult> checkResults;
    
    /**
     * 整体状态
     */
    private WorkflowStatus status;
    
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
     * 成功的检查数量
     */
    private int successCount;
    
    /**
     * 失败的检查数量
     */
    private int failedCount;
    
    /**
     * 错误的检查数量
     */
    private int errorCount;
    
    /**
     * 跳过的检查数量
     */
    private int skippedCount;
    
    /**
     * 总检查数量
     */
    private int totalCount;
    
    /**
     * 执行摘要
     */
    private String summary;
    
    /**
     * 附加元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100.0;
    }
    
    /**
     * 检查是否全部成功
     */
    public boolean isAllSuccess() {
        return failedCount == 0 && errorCount == 0;
    }
    
    /**
     * 检查是否有严重问题
     */
    public boolean hasCriticalIssues() {
        return checkResults.stream()
                .anyMatch(result -> result.isCritical());
    }
    
    /**
     * 获取执行时间（秒）
     */
    public double getExecutionTimeSeconds() {
        return totalExecutionTimeMs / 1000.0;
    }
}