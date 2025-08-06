package com.datasophon.api.workflow.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 进度阶段信息模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class ProgressStage {
    
    /**
     * 阶段名称
     */
    private String stageName;
    
    /**
     * 阶段描述
     */
    private String description;
    
    /**
     * 阶段状态
     */
    private StageStatus status;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 阶段进度百分比
     */
    private double progressPercentage;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 阶段顺序
     */
    private int order;
    
    /**
     * 获取阶段执行时间（秒）
     */
    public long getExecutionTimeSeconds() {
        if (startTime == null) {
            return 0;
        }
        
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }
    
    /**
     * 检查阶段是否完成
     */
    public boolean isCompleted() {
        return status == StageStatus.COMPLETED || status == StageStatus.FAILED;
    }
}

/**
 * 阶段状态枚举
 */
enum StageStatus {
    /**
     * 等待执行
     */
    PENDING("pending", "等待执行"),
    
    /**
     * 执行中
     */
    RUNNING("running", "执行中"),
    
    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),
    
    /**
     * 失败
     */
    FAILED("failed", "失败"),
    
    /**
     * 跳过
     */
    SKIPPED("skipped", "跳过");
    
    private final String code;
    private final String description;
    
    StageStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}