package com.datasophon.api.service.checker.impl;

import com.datasophon.api.config.TaskManager;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.api.service.checker.ItemChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 异步检查服务
 * 提供基于Spring异步任务的检查项执行
 */
@Service
public class AsyncCheckService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncCheckService.class);
    
    // 正在执行的任务信息
    private final Map<String, TaskInfo> runningTasks = new ConcurrentHashMap<>();
    
    // 检查器工厂
    @Autowired
    private ItemCheckerFactory itemCheckerFactory;
    
    // 任务管理器
    @Autowired
    private TaskManager taskManager;
    
    // 检查任务执行器
    @Autowired
    @Qualifier("checkExecutor")
    private Executor checkExecutor;
    
    /**
     * 异步执行单个检查项
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @return 任务ID
     */
    public String executeCheckItemAsync(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        String taskKey = getTaskKey(clusterId, hostInfo.getHostname(), checkItem.getId());
        
        // 检查任务是否已在运行
        if (isTaskRunning(taskKey)) {
            logger.warn("任务已在运行中: {}", taskKey);
            return taskKey;
        }
        
        // 创建并注册异步任务
        CompletableFuture<CheckItem> future = CompletableFuture.supplyAsync(() -> {
            try {
                return doCheck(clusterId, hostInfo, checkItem);
            } catch (Exception e) {
                logger.error("执行检查项时发生异常: {}", e.getMessage(), e);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("检查异常: " + e.getMessage());
                return checkItem;
            }
        }, checkExecutor);
        
        // 注册任务
        String taskId = taskManager.registerTask("CHECK", 
                "检查项: " + checkItem.getItemName() + ", 主机: " + hostInfo.getHostname(), future);
        
        // 记录任务信息
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.taskId = taskId;
        taskInfo.future = future;
        taskInfo.clusterId = clusterId;
        taskInfo.hostname = hostInfo.getHostname();
        taskInfo.itemId = checkItem.getId();
        
        runningTasks.put(taskKey, taskInfo);
        
        // 当任务完成时，更新状态和从运行列表移除
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                logger.error("检查任务异常结束: {}", exception.getMessage(), exception);
                taskManager.markTaskCompleted(taskId, false);
            } else {
                logger.info("检查任务正常完成, 状态: {}", result.getStatus());
                taskManager.markTaskCompleted(taskId, true);
            }
            runningTasks.remove(taskKey);
        });
        
        return taskId;
    }
    
    /**
     * 异步执行修复检查项
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @return 任务ID
     */
    public String executeFixItemAsync(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        String taskKey = "FIX_" + getTaskKey(clusterId, hostInfo.getHostname(), checkItem.getId());
        
        // 检查任务是否已在运行
        if (isTaskRunning(taskKey)) {
            logger.warn("修复任务已在运行中: {}", taskKey);
            return taskKey;
        }
        
        // 创建并注册异步任务
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                return doFix(clusterId, hostInfo, checkItem);
            } catch (Exception e) {
                logger.error("执行修复检查项时发生异常: {}", e.getMessage(), e);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("修复异常: " + e.getMessage());
                return false;
            }
        }, checkExecutor);
        
        // 注册任务
        String taskId = taskManager.registerTask("FIX", 
                "修复检查项: " + checkItem.getItemName() + ", 主机: " + hostInfo.getHostname(), future);
        
        // 记录任务信息
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.taskId = taskId;
        taskInfo.future = future;
        taskInfo.clusterId = clusterId;
        taskInfo.hostname = hostInfo.getHostname();
        taskInfo.itemId = checkItem.getId();
        
        runningTasks.put(taskKey, taskInfo);
        
        // 当任务完成时，更新状态和从运行列表移除
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                logger.error("修复任务异常结束: {}", exception.getMessage(), exception);
                taskManager.markTaskCompleted(taskId, false);
            } else {
                logger.info("修复任务正常完成, 结果: {}", result ? "成功" : "失败");
                taskManager.markTaskCompleted(taskId, result);
            }
            runningTasks.remove(taskKey);
        });
        
        return taskId;
    }
    
    /**
     * 取消检查项任务
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @return 是否成功取消
     */
    public boolean cancelCheckTask(Integer clusterId, String hostname, Integer itemId) {
        String taskKey = getTaskKey(clusterId, hostname, itemId);
        return cancelTask(taskKey);
    }
    
    /**
     * 取消修复检查项任务
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @param itemId 检查项ID
     * @return 是否成功取消
     */
    public boolean cancelFixTask(Integer clusterId, String hostname, Integer itemId) {
        String taskKey = "FIX_" + getTaskKey(clusterId, hostname, itemId);
        return cancelTask(taskKey);
    }
    
    /**
     * 取消所有主机检查任务
     * @param clusterId 集群ID
     * @param hostname 主机名
     * @return 取消的任务数量
     */
    public int cancelHostTasks(Integer clusterId, String hostname) {
        int count = 0;
        String prefix = getHostTaskPrefix(clusterId, hostname);
        
        for (String taskKey : runningTasks.keySet()) {
            if (taskKey.startsWith(prefix)) {
                if (cancelTask(taskKey)) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * 取消集群内所有任务
     * @param clusterId 集群ID
     * @return 取消的任务数量
     */
    public int cancelClusterTasks(Integer clusterId) {
        int count = 0;
        String prefix = getClusterTaskPrefix(clusterId);
        
        for (String taskKey : runningTasks.keySet()) {
            if (taskKey.startsWith(prefix)) {
                if (cancelTask(taskKey)) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * 执行实际检查逻辑
     */
    private CheckItem doCheck(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 将String类型的itemCode转换为ItemCode枚举
            ItemCode itemCode = ItemCode.valueOf(checkItem.getItemCode());
            ItemChecker checker = itemCheckerFactory.getChecker(itemCode);
            
            if (checker == null) {
                logger.error("找不到检查项对应的检查器: {}", itemCode);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("找不到对应的检查器");
                return checkItem;
            }
            
            return checker.check(clusterId, hostInfo, checkItem);
        } catch (IllegalArgumentException e) {
            logger.error("无效的检查项代码: {}", checkItem.getItemCode());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("无效的检查项代码: " + checkItem.getItemCode());
            return checkItem;
        } catch (InterruptedException e) {
            logger.info("检查任务被中断");
            Thread.currentThread().interrupt();
            checkItem.setStatus(CheckItem.Status.SKIPPED);
            checkItem.setMessage("检查被中断");
            return checkItem;
        } catch (Exception e) {
            logger.error("执行检查时发生异常", e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("检查异常: " + e.getMessage());
            return checkItem;
        }
    }
    
    /**
     * 执行实际修复逻辑
     */
    private boolean doFix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 将String类型的itemCode转换为ItemCode枚举
            ItemCode itemCode = ItemCode.valueOf(checkItem.getItemCode());
            ItemChecker checker = itemCheckerFactory.getChecker(itemCode);
            
            if (checker == null) {
                logger.error("找不到检查项对应的检查器: {}", itemCode);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("找不到对应的检查器");
                return false;
            }
            
            return checker.fix(clusterId, hostInfo, checkItem);
        } catch (IllegalArgumentException e) {
            logger.error("无效的检查项代码: {}", checkItem.getItemCode());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("无效的检查项代码: " + checkItem.getItemCode());
            return false;
        } catch (InterruptedException e) {
            logger.info("修复任务被中断");
            Thread.currentThread().interrupt();
            checkItem.setStatus(CheckItem.Status.SKIPPED);
            checkItem.setMessage("修复被中断");
            return false;
        } catch (Exception e) {
            logger.error("执行修复时发生异常", e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("修复异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查任务是否在运行
     */
    private boolean isTaskRunning(String taskKey) {
        TaskInfo taskInfo = runningTasks.get(taskKey);
        return taskInfo != null && !taskInfo.future.isDone();
    }
    
    /**
     * 取消任务
     */
    private boolean cancelTask(String taskKey) {
        TaskInfo taskInfo = runningTasks.get(taskKey);
        if (taskInfo != null) {
            boolean result = taskManager.cancelTask(taskInfo.taskId);
            if (result) {
                runningTasks.remove(taskKey);
            }
            return result;
        }
        return false;
    }
    
    /**
     * 定期清理过期任务信息
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void cleanupTasks() {
        int count = taskManager.cleanCompletedTasks(24 * 60 * 60 * 1000); // 24小时
        logger.info("清理了 {} 个过期任务记录", count);
    }
    
    /**
     * 生成任务唯一键
     */
    private String getTaskKey(Integer clusterId, String hostname, Integer itemId) {
        return clusterId + ":" + hostname + ":" + itemId;
    }
    
    /**
     * 生成主机任务前缀
     */
    private String getHostTaskPrefix(Integer clusterId, String hostname) {
        return clusterId + ":" + hostname + ":";
    }
    
    /**
     * 生成集群任务前缀
     */
    private String getClusterTaskPrefix(Integer clusterId) {
        return clusterId + ":";
    }
    
    /**
     * 任务信息内部类
     */
    private static class TaskInfo {
        String taskId;
        Integer clusterId;
        String hostname;
        Integer itemId;
        CompletableFuture<?> future;
    }
} 