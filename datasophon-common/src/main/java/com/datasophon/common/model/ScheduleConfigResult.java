package com.datasophon.common.model;

import lombok.Data;

/**
 * 定时任务配置结果实体类
 */
@Data
public class ScheduleConfigResult {
    // 操作消息
    private String message;
    
    // 定时任务状态
    private ScheduledTasksStatus status;
} 