package com.datasophon.common.model;

import lombok.Data;

/**
 * 队列任务信息
 * 包含任务的详细信息，包括状态、时间等
 */
@Data
public class QueueTaskInfo {
    // 任务基本信息
    private String taskId;
    private Long clusterId;
    private String hostname;
    private Integer itemId;
    private String status;
    private int priority;
    
    // 任务时间信息
    private String startTime;
    private long duration;
    private String endTime;
    
    // 任务执行信息
    private String executorName;
    private String threadName;
    private String errorMessage;
    
    // 任务类型
    private boolean fixTask;
    
    // 任务进度
    private int progress;
    private String progressMessage;
} 