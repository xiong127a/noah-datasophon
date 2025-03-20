package com.datasophon.common.model;

import lombok.Data;

/**
 * 定时任务状态实体类
 */
@Data
public class ScheduledTasksStatus {
    // 定时任务是否启用
    private boolean scheduledTasksEnabled;
    
    // 任务清理定时任务是否活跃
    private boolean taskCleanupActive;
    
    // 连接清理定时任务是否活跃
    private boolean connectionCleanupActive;
    
    // 任务清理定时任务执行间隔（毫秒）
    private long taskCleanupIntervalMs;
    
    // 连接清理定时任务执行间隔（毫秒）
    private long connectionCleanupIntervalMs;
    
    // 任务清理定时任务执行间隔（格式化）
    private String taskCleanupInterval;
    
    // 连接清理定时任务执行间隔（格式化）
    private String connectionCleanupInterval;
    
    // 最后一次任务清理时间
    private String lastTaskCleanupTime;
    
    // 最后一次连接清理时间
    private String lastConnectionCleanupTime;
    
    // 连接池大小
    private int connectionPoolSize;
    
    // 运行中的任务数量
    private int runningTasksCount;
} 