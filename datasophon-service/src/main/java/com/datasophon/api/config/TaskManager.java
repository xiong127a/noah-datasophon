package com.datasophon.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 任务管理器
 * 用于管理和跟踪异步任务的执行
 */
@Component
public class TaskManager {
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
    
    // 正在运行的任务列表
    private final Map<String, TaskInfo> tasks = new ConcurrentHashMap<>();
    
    /**
     * 注册任务
     * @param type 任务类型
     * @param description 任务描述
     * @param future 任务Future
     * @return 任务ID
     */
    public String registerTask(String type, String description, CompletableFuture<?> future) {
        String taskId = generateTaskId(type);
        
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.type = type;
        taskInfo.description = description;
        taskInfo.future = future;
        taskInfo.startTime = System.currentTimeMillis();
        
        tasks.put(taskId, taskInfo);
        logger.info("注册任务: {}, 描述: {}", taskId, description);
        
        return taskId;
    }
    
    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelTask(String taskId) {
        TaskInfo taskInfo = tasks.get(taskId);
        if (taskInfo != null && !taskInfo.future.isDone()) {
            logger.info("取消任务: {}, 描述: {}", taskId, taskInfo.description);
            boolean result = taskInfo.future.cancel(true);
            if (result) {
                taskInfo.endTime = System.currentTimeMillis();
                taskInfo.success = false;
                taskInfo.completed = true;
            }
            return result;
        }
        return false;
    }
    
    /**
     * 标记任务完成
     * @param taskId 任务ID
     * @param success 是否成功
     */
    public void markTaskCompleted(String taskId, boolean success) {
        TaskInfo taskInfo = tasks.get(taskId);
        if (taskInfo != null) {
            taskInfo.endTime = System.currentTimeMillis();
            taskInfo.success = success;
            taskInfo.completed = true;
            logger.info("任务完成: {}, 结果: {}", taskId, success ? "成功" : "失败");
        }
    }
    
    /**
     * 清理已完成的任务
     * @param olderThanMs 清理多久之前完成的任务（毫秒）
     * @return 清理的任务数量
     */
    public int cleanCompletedTasks(long olderThanMs) {
        long current = System.currentTimeMillis();
        long threshold = current - olderThanMs;
        
        // 找出需要清理的任务
        Map<String, TaskInfo> tasksToRemove = tasks.entrySet().stream()
                .filter(entry -> entry.getValue().completed && entry.getValue().endTime < threshold)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // 清理任务
        for (String taskId : tasksToRemove.keySet()) {
            tasks.remove(taskId);
        }
        
        int count = tasksToRemove.size();
        if (count > 0) {
            logger.info("清理了 {} 个已完成的任务", count);
        }
        
        return count;
    }
    
    /**
     * 获取任务信息
     * @param taskId 任务ID
     * @return 任务信息
     */
    public TaskInfo getTaskInfo(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * 检查任务是否完成
     * @param taskId 任务ID
     * @return 是否完成
     */
    public boolean isTaskCompleted(String taskId) {
        TaskInfo taskInfo = tasks.get(taskId);
        return taskInfo != null && taskInfo.completed;
    }
    
    /**
     * 检查任务是否成功
     * @param taskId 任务ID
     * @return 是否成功
     */
    public boolean isTaskSuccessful(String taskId) {
        TaskInfo taskInfo = tasks.get(taskId);
        return taskInfo != null && taskInfo.completed && taskInfo.success;
    }
    
    /**
     * 获取任务执行时间
     * @param taskId 任务ID
     * @return 执行时间（毫秒）
     */
    public long getTaskExecutionTime(String taskId) {
        TaskInfo taskInfo = tasks.get(taskId);
        if (taskInfo == null) {
            return -1;
        }
        
        if (taskInfo.completed) {
            return taskInfo.endTime - taskInfo.startTime;
        } else {
            return System.currentTimeMillis() - taskInfo.startTime;
        }
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId(String type) {
        return type + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    /**
     * 任务信息内部类
     */
    public static class TaskInfo {
        String type;
        String description;
        CompletableFuture<?> future;
        long startTime;
        long endTime;
        boolean completed;
        boolean success;
    }
} 