package com.datasophon.api.service.checker.impl;

import com.datasophon.api.config.TaskManager;
import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.api.service.checker.ItemChecker;
import com.datasophon.api.service.checker.ItemCheckerFactory;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    
    // SSH连接池 - 按主机名缓存SSH连接
    private final Map<String, ClientSession> hostConnectionPool = new ConcurrentHashMap<>();
    
    // 连接锁，防止并发问题
    private final Map<String, Object> connectionLocks = new ConcurrentHashMap<>();
    
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
                setCheckItemMessage(clusterId, hostInfo, checkItem, "检查异常: " + e.getMessage());
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
                setCheckItemMessage(clusterId, hostInfo, checkItem, "修复异常: " + e.getMessage());
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
        ClientSession session = null;
        
        try {
            // 将String类型的itemCode转换为ItemCode枚举
            ItemCode itemCode = ItemCode.valueOf(checkItem.getItemCode());
            ItemChecker checker = itemCheckerFactory.getChecker(itemCode);
            
            if (checker == null) {
                logger.error("找不到检查项对应的检查器: {}", itemCode);
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(clusterId, hostInfo, checkItem, "找不到对应的检查器");
                return checkItem;
            }
            
            // 获取或创建连接
            session = getOrCreateConnection(hostInfo);
            if (session == null) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(clusterId, hostInfo, checkItem, "无法建立SSH连接");
                return checkItem;
            }
            
            // 使用复用的会话执行检查
            if (checker instanceof AbstractItemChecker) {
                // 使用新添加的支持连接复用的方法
                AbstractItemChecker abstractChecker = (AbstractItemChecker) checker;
                return abstractChecker.checkWithSession(clusterId, hostInfo, checkItem, session);
            } else {
                // 不支持会话复用的检查器使用原始方法
                return checker.check(clusterId, hostInfo, checkItem);
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("无效的检查项代码: {}", checkItem.getItemCode());
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(clusterId, hostInfo, checkItem, "无效的检查项代码: " + checkItem.getItemCode());
            return checkItem;
        } catch (InterruptedException e) {
            logger.info("检查任务被中断");
            Thread.currentThread().interrupt();
            checkItem.setStatus(CheckItem.Status.SKIPPED);
            setCheckItemMessage(clusterId, hostInfo, checkItem, "检查被中断");
            return checkItem;
        } catch (Exception e) {
            logger.error("执行检查时发生异常", e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(clusterId, hostInfo, checkItem, "检查异常: " + e.getMessage());
            return checkItem;
        }
    }
    
    /**
     * 执行实际修复逻辑
     */
    private boolean doFix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        ClientSession session = null;
        
        try {
            // 将String类型的itemCode转换为ItemCode枚举
            ItemCode itemCode = ItemCode.valueOf(checkItem.getItemCode());
            ItemChecker checker = itemCheckerFactory.getChecker(itemCode);
            
            if (checker == null) {
                logger.error("找不到检查项对应的检查器: {}", itemCode);
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(clusterId, hostInfo, checkItem, "找不到对应的检查器");
                return false;
            }
            
            // 获取或创建连接
            session = getOrCreateConnection(hostInfo);
            if (session == null) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(clusterId, hostInfo, checkItem, "无法建立SSH连接");
                return false;
            }
            
            // 使用复用的会话执行修复
            if (checker instanceof AbstractItemChecker) {
                // 使用新添加的支持连接复用的方法
                AbstractItemChecker abstractChecker = (AbstractItemChecker) checker;
                return abstractChecker.fixWithSession(clusterId, hostInfo, checkItem, session);
            } else {
                // 不支持会话复用的检查器使用原始方法
                return checker.fix(clusterId, hostInfo, checkItem);
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("无效的检查项代码: {}", checkItem.getItemCode());
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(clusterId, hostInfo, checkItem, "无效的检查项代码: " + checkItem.getItemCode());
            return false;
        } catch (InterruptedException e) {
            logger.info("修复任务被中断");
            Thread.currentThread().interrupt();
            checkItem.setStatus(CheckItem.Status.SKIPPED);
            setCheckItemMessage(clusterId, hostInfo, checkItem, "修复被中断");
            return false;
        } catch (Exception e) {
            logger.error("执行修复时发生异常", e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(clusterId, hostInfo, checkItem, "修复异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取或创建SSH连接
     * @param hostInfo 主机信息
     * @return SSH会话，如果创建失败则返回null
     */
    private ClientSession getOrCreateConnection(HostInfo hostInfo) {
        String hostKey = hostInfo.getHostname() + ":" + hostInfo.getSshPort();
        
        // 获取连接锁，确保同一主机的连接操作串行化
        Object lock = connectionLocks.computeIfAbsent(hostKey, k -> new Object());
        
        synchronized(lock) {
            ClientSession session = hostConnectionPool.get(hostKey);
            
            // 检查连接是否存在且有效
            if (session != null && session.isOpen()) {
                logger.debug("复用主机 {} 的现有SSH连接", hostInfo.getHostname());
                return session;
            }
            
            // 创建新连接
            try {
                logger.info("创建主机 {} 的新SSH连接", hostInfo.getHostname());
                session = MinaUtils.openConnection(hostInfo.getHostname(), 
                        hostInfo.getSshPort(), hostInfo.getSshUser());
                
                if (session != null) {
                    hostConnectionPool.put(hostKey, session);
                    logger.info("成功创建主机 {} 的SSH连接", hostInfo.getHostname());
                }
                return session;
            } catch (Exception e) {
                logger.error("建立SSH连接失败: {}", e.getMessage());
                return null;
            }
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
     * 定期清理不活跃连接
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    public void cleanupConnections() {
        int closedCount = 0;
        logger.info("开始清理不活跃SSH连接...");
        
        for (Map.Entry<String, ClientSession> entry : hostConnectionPool.entrySet()) {
            try {
                ClientSession session = entry.getValue();
                if (session == null || !session.isOpen()) {
                    hostConnectionPool.remove(entry.getKey());
                    closedCount++;
                    logger.debug("已移除无效连接: {}", entry.getKey());
                }
            } catch (Exception e) {
                logger.warn("清理连接时发生异常: {}", e.getMessage());
            }
        }
        
        logger.info("连接池清理完成，移除了 {} 个无效连接，当前连接数: {}", 
                closedCount, hostConnectionPool.size());
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
    
    /**
     * 设置检查项消息并立即更新状态
     */
    private void setCheckItemMessage(Integer clusterId, HostInfo hostInfo, CheckItem checkItem, String message) {
        if (checkItem != null) {
            checkItem.setMessage(message);
            logger.debug("正在实时更新检查状态消息: {}", message);
            // 立即更新状态
            updateCheckStatus(clusterId, hostInfo, checkItem);
        }
    }
    
    /**
     * 更新检查状态
     */
    private void updateCheckStatus(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        if (clusterId != null && hostInfo != null && checkItem != null) {
            String cacheKey = clusterId + Constants.HOST_MAP;
            logger.debug("更新检查状态: 主机={}, 检查项ID={}, 状态={}, 消息={}", 
                    hostInfo.getHostname(), checkItem.getId(), checkItem.getStatus(), checkItem.getMessage());
            
            try {
                Map<String, HostInfo> hostInfoMap = (Map<String, HostInfo>) CacheUtils.get(cacheKey);
                if (hostInfoMap != null) {
                    HostInfo cachedHostInfo = hostInfoMap.get(hostInfo.getHostname());
                    if (cachedHostInfo != null) {
                        boolean updated = false;
                        for (CheckItem item : cachedHostInfo.getCheckItems()) {
                            if (item.getId().equals(checkItem.getId())) {
                                item.setStatus(checkItem.getStatus());
                                item.setMessage(checkItem.getMessage());
                                updated = true;
                                break;
                            }
                        }
                        
                        if (updated) {
                            cachedHostInfo.calculateStatus();
                            hostInfoMap.put(hostInfo.getHostname(), cachedHostInfo);
                            CacheUtils.put(cacheKey, hostInfoMap);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("更新检查状态时发生异常: {}", e.getMessage(), e);
            }
        }
    }
} 