package com.datasophon.common.model;

import lombok.Data;
import java.util.List;

/**
 * 队列任务详情结果实体类
 * 包含队列状态和任务详情
 */
@Data
public class QueueTaskDetailResult {
    // 队列管理器状态
    private QueueManagerStatus queueManager;
    
    // 异步服务状态
    private AsyncServiceStatus asyncService;
    
    // 检查任务队列中的任务
    private List<QueueTaskInfo> queueTasks;
    
    // 修复任务队列中的任务
    private List<QueueTaskInfo> fixQueueTasks;
} 