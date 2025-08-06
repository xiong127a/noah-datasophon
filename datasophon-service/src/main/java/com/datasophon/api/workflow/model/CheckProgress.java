package com.datasophon.api.workflow.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 检查进度信息模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class CheckProgress {
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 当前状态
     */
    private WorkflowStatus currentStatus;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 当前时间
     */
    @Builder.Default
    private LocalDateTime currentTime = LocalDateTime.now();
    
    /**
     * 预计结束时间
     */
    private LocalDateTime estimatedEndTime;
    
    /**
     * 总任务数量
     */
    private int totalTasks;
    
    /**
     * 已完成任务数量
     */
    private int completedTasks;
    
    /**
     * 正在执行的任务数量
     */
    private int runningTasks;
    
    /**
     * 等待执行的任务数量
     */
    private int pendingTasks;
    
    /**
     * 失败的任务数量
     */
    private int failedTasks;
    
    /**
     * 当前正在执行的插件
     */
    private String currentPlugin;
    
    /**
     * 当前正在检查的主机
     */
    private String currentHost;
    
    /**
     * 进度百分比
     */
    private double progressPercentage;
    
    /**
     * 阶段信息
     */
    private List<ProgressStage> stages;
    
    /**
     * 详细进度信息
     */
    private Map<String, Object> details;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 计算进度百分比
     */
    public double calculateProgressPercentage() {
        if (totalTasks == 0) {
            return 0.0;
        }
        return (double) completedTasks / totalTasks * 100.0;
    }
    
    /**
     * 获取已用时间（秒）
     */
    public long getElapsedTimeSeconds() {
        if (startTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, currentTime).getSeconds();
    }
    
    /**
     * 获取预计剩余时间（秒）
     */
    public long getEstimatedRemainingTimeSeconds() {
        if (estimatedEndTime == null || estimatedEndTime.isBefore(currentTime)) {
            return 0;
        }
        return java.time.Duration.between(currentTime, estimatedEndTime).getSeconds();
    }
    
    /**
     * 检查是否完成
     */
    public boolean isCompleted() {
        return currentStatus != null && currentStatus.isTerminal();
    }
}