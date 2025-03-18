package com.datasophon.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务管理器
 * 用于管理和监控异步任务
 */
@Component
public class TaskManager {
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
    
    // 任务ID生成器
    private final AtomicInteger taskIdGenerator = new AtomicInteger(0);
    
    // 任务映射表
    private final Map<String, TaskInfo> taskMap = new ConcurrentHashMap<>();
    
    /**
     * 注册一个新任务
     * @param taskType 任务类型
     * @param description 任务描述
     * @param future 任务Future对象
     * @return 任务ID
     */
    public String registerTask(String taskType, String description, Future<?> future) {
        String taskId = generateTaskId(taskType);
        TaskInfo taskInfo = new TaskInfo(taskId, taskType, description, future, System.currentTimeMillis());
        taskMap.put(taskId, taskInfo);
        logger.info("注册任务: {}, 描述: {}", taskId, description);
        return taskId;
    }
    
    /**
     * 取消指定任务
     * @param taskId 任务ID
     * @return 取消是否成功
     */
    public boolean cancelTask(String taskId) {
        TaskInfo taskInfo = taskMap.get(taskId);
        if (taskInfo != null && !taskInfo.future.isDone() && !taskInfo.future.isCancelled()) {
            boolean result = taskInfo.future.cancel(true);
            if (result) {
                logger.info("成功取消任务: {}", taskId);
                taskInfo.endTime = System.currentTimeMillis();
                taskInfo.status = TaskStatus.CANCELLED;
            } else {
                logger.warn("无法取消任务: {}", taskId);
            }
            return result;
        }
        return false;
    }
    
    /**
     * 取消指定类型的所有任务
     * @param taskType 任务类型
     * @return 取消的任务数量
     */
    public int cancelTasksByType(String taskType) {
        int count = 0;
        for (Map.Entry<String, TaskInfo> entry : taskMap.entrySet()) {
            if (entry.getValue().taskType.equals(taskType)) {
                if (cancelTask(entry.getKey())) {
                    count++;
                }
            }
        }
        logger.info("取消了 {} 个 {} 类型的任务", count, taskType);
        return count;
    }
    
    /**
     * 标记任务完成
     * @param taskId 任务ID
     * @param success 是否成功
     */
    public void markTaskCompleted(String taskId, boolean success) {
        TaskInfo taskInfo = taskMap.get(taskId);
        if (taskInfo != null) {
            taskInfo.endTime = System.currentTimeMillis();
            taskInfo.status = success ? TaskStatus.COMPLETED : TaskStatus.FAILED;
            logger.info("任务 {} {}", taskId, success ? "完成" : "失败");
        }
    }
    
    /**
     * 清理已完成的任务
     * @param maxAgeMs 任务最大保留时间（毫秒）
     * @return 清理的任务数量
     */
    public int cleanCompletedTasks(long maxAgeMs) {
        int count = 0;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, TaskInfo> entry : taskMap.entrySet()) {
            TaskInfo info = entry.getValue();
            if (info.isDone() && (now - info.endTime > maxAgeMs)) {
                taskMap.remove(entry.getKey());
                count++;
            }
        }
        if (count > 0) {
            logger.info("清理了 {} 个已完成/取消的任务", count);
        }
        return count;
    }
    
    /**
     * 检查任务是否正在运行
     * @param taskId 任务ID
     * @return 是否在运行
     */
    public boolean isTaskRunning(String taskId) {
        TaskInfo taskInfo = taskMap.get(taskId);
        return taskInfo != null && !taskInfo.isDone();
    }
    
    /**
     * 生成任务ID
     * @param taskType 任务类型
     * @return 任务ID
     */
    private String generateTaskId(String taskType) {
        return taskType + "-" + taskIdGenerator.incrementAndGet();
    }
    
    /**
     * 应用关闭时尝试取消所有运行中的任务
     */
    @PreDestroy
    public void shutdown() {
        logger.info("应用关闭，取消所有运行中的任务...");
        int count = 0;
        for (TaskInfo taskInfo : taskMap.values()) {
            if (!taskInfo.isDone()) {
                taskInfo.future.cancel(true);
                count++;
            }
        }
        logger.info("已取消 {} 个运行中的任务", count);
    }
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        RUNNING,    // 运行中
        COMPLETED,  // 已完成
        FAILED,     // 失败
        CANCELLED   // 已取消
    }
    
    /**
     * 任务信息类
     */
    public static class TaskInfo {
        private final String taskId;
        private final String taskType;
        private final String description;
        private final Future<?> future;
        private final long startTime;
        private long endTime;
        private TaskStatus status;
        
        public TaskInfo(String taskId, String taskType, String description, Future<?> future, long startTime) {
            this.taskId = taskId;
            this.taskType = taskType;
            this.description = description;
            this.future = future;
            this.startTime = startTime;
            this.status = TaskStatus.RUNNING;
        }
        
        public boolean isDone() {
            return future.isDone() || future.isCancelled() || 
                   status == TaskStatus.COMPLETED || 
                   status == TaskStatus.FAILED || 
                   status == TaskStatus.CANCELLED;
        }
        
        public long getRunningTimeMs() {
            if (endTime > 0) {
                return endTime - startTime;
            }
            return System.currentTimeMillis() - startTime;
        }
    }
} 