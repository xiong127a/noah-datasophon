package com.datasophon.api.service.checker.impl;

import com.datasophon.api.config.TaskManager;
import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.api.service.checker.ItemChecker;
import com.datasophon.api.service.checker.ItemCheckerFactory;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.AsyncServiceStatus;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import com.datasophon.common.model.ScheduledTasksStatus;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private ExecutorService checkExecutor;
    
    // 修复任务执行器
    @Autowired
    @Qualifier("fixExecutor")
    private ExecutorService fixExecutor;
    
    // SSH连接池 - 按主机名缓存SSH连接
    private final Map<String, ClientSession> hostConnectionPool = new ConcurrentHashMap<>();
    
    // 连接锁，防止并发问题
    private final Map<String, Object> connectionLocks = new ConcurrentHashMap<>();
    
    // 定时任务启用标志
    private final AtomicBoolean scheduledTasksEnabled = new AtomicBoolean(true);
    
    // 定时任务执行间隔（默认值）
    private long taskCleanupIntervalMs = TimeUnit.HOURS.toMillis(1); // 默认1小时
    private long connectionCleanupIntervalMs = TimeUnit.MINUTES.toMillis(10); // 默认10分钟
    
    // 上次执行时间
    private volatile long lastTaskCleanupTime = 0;
    private volatile long lastConnectionCleanupTime = 0;
    
    // 定时任务调度器
    @Autowired(required = false)
    private TaskScheduler taskScheduler;
    
    // 定时任务的Future
    private ScheduledFuture<?> taskCleanupTask;
    private ScheduledFuture<?> connectionCleanupTask;
    
    // 添加连接池清理改进
    private final Map<String, Long> connectionLastAccessTime = new ConcurrentHashMap<>();
    
    // 添加缓存命中和总请求计数，用于计算缓存命中率
    private final Map<String, Long> hostCacheHits = new ConcurrentHashMap<>();
    private final Map<String, Long> hostCacheRequests = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        logger.info("初始化异步检查服务...");
        startScheduledTasks();
        logger.info("异步检查服务初始化完成");
    }
    
    /**
     * 启动定时任务
     */
    public void startScheduledTasks() {
        if (!scheduledTasksEnabled.get()) {
            scheduledTasksEnabled.set(true);
        }
        
        if (taskScheduler == null) {
            logger.info("TaskScheduler未注入，创建自定义TaskScheduler");
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(2);
            scheduler.setThreadNamePrefix("async-check-scheduler-");
            scheduler.initialize();
            taskScheduler = scheduler;
        }
        
        // 启动任务清理定时任务（每小时执行一次）
        if (taskCleanupTask == null || taskCleanupTask.isCancelled()) {
            taskCleanupTask = taskScheduler.scheduleAtFixedRate(
                this::cleanupTasks, taskCleanupIntervalMs);
            logger.info("任务清理定时任务已启动，执行间隔: {}毫秒", taskCleanupIntervalMs);
        }
        
        // 启动连接清理定时任务（每10分钟执行一次）
        if (connectionCleanupTask == null || connectionCleanupTask.isCancelled()) {
            connectionCleanupTask = taskScheduler.scheduleAtFixedRate(
                this::cleanupConnections, connectionCleanupIntervalMs);
            logger.info("连接清理定时任务已启动，执行间隔: {}毫秒", connectionCleanupIntervalMs);
        }
    }
    
    /**
     * 停止定时任务
     */
    public void stopScheduledTasks() {
        // 取消任务清理定时任务
        if (taskCleanupTask != null && !taskCleanupTask.isCancelled()) {
            taskCleanupTask.cancel(false);
            logger.info("任务清理定时任务已停止");
        }
        
        // 取消连接清理定时任务
        if (connectionCleanupTask != null && !connectionCleanupTask.isCancelled()) {
            connectionCleanupTask.cancel(false);
            logger.info("连接清理定时任务已停止");
        }
        
        // 设置定时任务标志为已停用
        scheduledTasksEnabled.set(false);
    }
    
    /**
     * 启用定时任务
     */
    public void enableScheduledTasks() {
        if (!scheduledTasksEnabled.get()) {
            startScheduledTasks();
            logger.info("AsyncCheckService定时任务已启用");
        }
    }
    
    /**
     * 禁用定时任务
     */
    public void disableScheduledTasks() {
        if (scheduledTasksEnabled.get()) {
            stopScheduledTasks();
            logger.info("AsyncCheckService定时任务已禁用");
        }
    }
    
    /**
     * 获取定时任务状态
     * @return 定时任务状态对象
     */
    public ScheduledTasksStatus getScheduledTasksStatus() {
        ScheduledTasksStatus status = new ScheduledTasksStatus();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        status.setScheduledTasksEnabled(scheduledTasksEnabled.get());
        status.setTaskCleanupActive(taskCleanupTask != null && !taskCleanupTask.isCancelled());
        status.setConnectionCleanupActive(connectionCleanupTask != null && !connectionCleanupTask.isCancelled());
        
        // 添加定时任务执行间隔
        status.setTaskCleanupIntervalMs(taskCleanupIntervalMs);
        status.setConnectionCleanupIntervalMs(connectionCleanupIntervalMs);
        
        // 格式化为人类可读的时间间隔
        status.setTaskCleanupInterval(formatTimeInterval(taskCleanupIntervalMs));
        status.setConnectionCleanupInterval(formatTimeInterval(connectionCleanupIntervalMs));
        
        // 格式化时间日期
        if (lastTaskCleanupTime > 0) {
            status.setLastTaskCleanupTime(dateFormat.format(new java.util.Date(lastTaskCleanupTime)));
        } else {
            status.setLastTaskCleanupTime("未执行");
        }
        
        if (lastConnectionCleanupTime > 0) {
            status.setLastConnectionCleanupTime(dateFormat.format(new java.util.Date(lastConnectionCleanupTime)));
        } else {
            status.setLastConnectionCleanupTime("未执行");
        }
        
        status.setConnectionPoolSize(hostConnectionPool.size());
        status.setRunningTasksCount(runningTasks.size());
        return status;
    }
    
    /**
     * 设置任务清理定时任务执行间隔
     * @param intervalMs 间隔时间（毫秒）
     * @return 是否设置成功
     */
    public boolean setTaskCleanupInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("任务清理定时任务间隔不能小于1秒，忽略此次更新");
            return false;
        }
        
        this.taskCleanupIntervalMs = intervalMs;
        
        // 如果任务已经在运行，则重新调度
        if (taskCleanupTask != null && !taskCleanupTask.isCancelled()) {
            taskCleanupTask.cancel(false);
            taskCleanupTask = taskScheduler.scheduleAtFixedRate(
                this::cleanupTasks, intervalMs);
            logger.info("任务清理定时任务已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
        
        return true;
    }
    
    /**
     * 设置连接清理定时任务的执行间隔
     * @param intervalMs 间隔时间（毫秒）
     * @return 是否设置成功
     */
    public boolean setConnectionCleanupInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("连接清理定时任务间隔不能小于1秒，忽略此次更新");
            return false;
        }
        
        this.connectionCleanupIntervalMs = intervalMs;
        
        // 如果任务已经在运行，则重新调度
        if (connectionCleanupTask != null && !connectionCleanupTask.isCancelled()) {
            connectionCleanupTask.cancel(false);
            connectionCleanupTask = taskScheduler.scheduleAtFixedRate(
                this::cleanupConnections, intervalMs);
            logger.info("连接清理定时任务已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
        
        return true;
    }
    
    /**
     * 将毫秒时间格式化为人类可读的时间间隔
     * @param ms 毫秒数
     * @return 格式化后的时间间隔
     */
    private String formatTimeInterval(long ms) {
        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时";
        } else {
            return (seconds / 86400) + "天";
        }
    }
    
    /**
     * 关闭服务
     */
    @PreDestroy
    public void shutdown() {
        logger.info("正在关闭异步检查服务...");
        
        // 停止定时任务
        stopScheduledTasks();
        
        // 关闭所有SSH连接
        for (Map.Entry<String, ClientSession> entry : hostConnectionPool.entrySet()) {
            try {
                ClientSession session = entry.getValue();
                if (session != null && session.isOpen()) {
                    session.close();
                    logger.info("关闭SSH连接: {}", entry.getKey());
                }
            } catch (Exception e) {
                logger.warn("关闭SSH连接时发生异常: {}", e.getMessage());
            }
        }
        
        // 清空连接池
        hostConnectionPool.clear();
        connectionLocks.clear();
        
        logger.info("异步检查服务已关闭");
    }
    
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
        }, fixExecutor);
        
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
            // 将单个检查项放入列表中使用批量检查方法
            List<CheckItem> items = new ArrayList<>();
            items.add(checkItem);
            
            // 使用批量检查方法进行处理
            List<CheckItem> results = batchExecuteCheck(clusterId, hostInfo, items);
            
            // 返回检查结果
            if (results != null && !results.isEmpty()) {
                return results.get(0);
            } else {
                // 如果没有结果，则返回原始检查项但标记为失败
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("执行检查时发生内部错误");
                return checkItem;
            }
        } catch (Exception e) {
            logger.error("执行检查时发生异常: {}", e.getMessage(), e);
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
            // 将单个修复项放入列表中使用批量修复方法
            List<CheckItem> items = new ArrayList<>();
            items.add(checkItem);
            
            // 使用批量修复方法进行处理
            List<CheckItem> results = batchExecuteFix(clusterId, hostInfo, items);
            
            // 返回修复结果
            if (results != null && !results.isEmpty()) {
                CheckItem result = results.get(0);
                return result != null && result.getStatus() == CheckItem.Status.SUCCESS;
            } else {
                // 如果没有结果，则标记为失败
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("执行修复时发生内部错误");
                return false;
            }
        } catch (Exception e) {
            logger.error("执行修复时发生异常: {}", e.getMessage(), e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("修复异常: " + e.getMessage());
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
        
        // 增加总请求计数
        long requests = hostCacheRequests.getOrDefault(hostKey, 0L) + 1;
        hostCacheRequests.put(hostKey, requests);
        
        // 获取连接锁，确保同一主机的连接操作串行化
        Object lock = connectionLocks.computeIfAbsent(hostKey, k -> new Object());
        
        synchronized(lock) {
            ClientSession session = hostConnectionPool.get(hostKey);
            
            // 检查连接是否存在且有效
            if (session != null) {
                try {
                    // 检查连接是否仍然可用
                    if (session.isOpen()) {
                        // 尝试发送一个无害的命令来验证连接是否真正有效
                        CommandResult testResult = execCommand(session, "echo connection_test");
                        if (testResult.isSuccess() && testResult.getOutput().trim().contains("connection_test")) {
                            logger.debug("复用主机 {} 的现有SSH连接 (健康检查通过)", hostInfo.getHostname());
                            // 更新最后访问时间
                            connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
                            
                            // 增加缓存命中计数
                            long hits = hostCacheHits.getOrDefault(hostKey, 0L) + 1;
                            hostCacheHits.put(hostKey, hits);
                            
                            return session;
                        } else {
                            logger.warn("主机 {} 的SSH连接健康检查失败，将创建新连接", hostInfo.getHostname());
                        }
                    } else {
                        logger.info("主机 {} 的SSH连接已关闭，将创建新连接", hostInfo.getHostname());
                    }
                } catch (Exception e) {
                    logger.warn("测试SSH连接时发生异常: {}", e.getMessage());
                }
                
                // 关闭无效连接
                try {
                    session.close();
                } catch (Exception e) {
                    logger.debug("关闭失效连接时发生异常: {}", e.getMessage());
                } finally {
                    hostConnectionPool.remove(hostKey);
                }
            }
            
            // 创建新连接
            try {
                logger.info("创建主机 {} 的新SSH连接", hostInfo.getHostname());
                session = MinaUtils.openConnection(hostInfo.getHostname(), 
                        hostInfo.getSshPort(), hostInfo.getSshUser());
                
                if (session != null) {
                    hostConnectionPool.put(hostKey, session);
                    // 设置初始访问时间
                    connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
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
    public void cleanupTasks() {
        if (!scheduledTasksEnabled.get()) {
            logger.debug("定时任务已禁用，跳过执行cleanupTasks()");
            return;
        }
        
        int count = taskManager.cleanCompletedTasks(24 * 60 * 60 * 1000); // 24小时
        lastTaskCleanupTime = System.currentTimeMillis();
        logger.info("清理了 {} 个过期任务记录", count);
    }
    
    /**
     * 定期清理不活跃连接
     * 每10分钟执行一次
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void cleanupConnections() {
        if (!scheduledTasksEnabled.get()) {
            logger.debug("定时任务已禁用，跳过执行cleanupConnections()");
            return;
        }
        
        int closedCount = 0;
        int idleClosedCount = 0;
        logger.info("开始清理不活跃SSH连接...");
        
        long currentTime = System.currentTimeMillis();
        long idleTimeout = TimeUnit.MINUTES.toMillis(1); // 1分钟没有使用的连接将被关闭
        
        List<String> keysToRemove = new ArrayList<>();
        
        for (Map.Entry<String, ClientSession> entry : hostConnectionPool.entrySet()) {
            String key = entry.getKey();
            try {
                ClientSession session = entry.getValue();
                // 检查连接是否有效
                if (session == null || !session.isOpen()) {
                    keysToRemove.add(key);
                    closedCount++;
                    logger.debug("已移除无效连接: {}", key);
                    continue;
                }
                
                // 检查连接是否空闲超时
                Long lastAccess = connectionLastAccessTime.get(key);
                if (lastAccess != null && (currentTime - lastAccess) > idleTimeout) {
                    try {
                        logger.info("关闭空闲超时的连接: {}, 空闲时长: {}分钟", 
                            key, (currentTime - lastAccess) / 60000);
                        session.close();
                        keysToRemove.add(key);
                        idleClosedCount++;
                    } catch (Exception e) {
                        logger.warn("关闭空闲连接时发生异常: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.warn("检查连接时发生异常: {}", e.getMessage());
            }
        }
        
        // 移除已关闭的连接
        for (String key : keysToRemove) {
            hostConnectionPool.remove(key);
            // 同时也要移除对应的访问时间记录
            connectionLastAccessTime.remove(key);
            
            // 注意：不要清除缓存命中统计数据，保留以便计算长期命中率
        }
        
        lastConnectionCleanupTime = System.currentTimeMillis();
        logger.info("SSH连接清理完成，关闭{}个失效连接，{}个空闲连接，当前连接池大小: {}",
            closedCount, idleClosedCount, hostConnectionPool.size());
            
        // 日志记录当前缓存命中率
        int hitRate = calculateSessionCacheHitRate();
        logger.info("当前SSH会话缓存命中率: {}%", hitRate);
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
    
    /**
     * 计算SSH会话缓存命中率
     * @return 缓存命中百分比
     */
    private int calculateSessionCacheHitRate() {
        long totalHits = 0;
        long totalRequests = 0;
        
        for (String hostKey : hostCacheRequests.keySet()) {
            totalHits += hostCacheHits.getOrDefault(hostKey, 0L);
            totalRequests += hostCacheRequests.getOrDefault(hostKey, 0L);
        }
        
        if (totalRequests == 0) {
            return 0;
        }
        
        return (int) ((totalHits * 100) / totalRequests);
    }
    
    /**
     * 获取异步服务状态（返回实体类）
     * @return AsyncServiceStatus对象
     */
    public AsyncServiceStatus getAsyncServiceStatus() {
        AsyncServiceStatus status = new AsyncServiceStatus();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 获取状态信息
        ScheduledTasksStatus statusInfo = getScheduledTasksStatus();
        
        // 填充实体类
        status.setScheduledTasksEnabled(statusInfo.isScheduledTasksEnabled());
        status.setLastTaskCleanupTime(statusInfo.getLastTaskCleanupTime());
        status.setRunningTasksCount(statusInfo.getRunningTasksCount());
        status.setConnectionPoolSize(statusInfo.getConnectionPoolSize());
        status.setTaskCleanupActive(statusInfo.isTaskCleanupActive());
        status.setConnectionCleanupActive(statusInfo.isConnectionCleanupActive());
        status.setLastConnectionCleanupTime(statusInfo.getLastConnectionCleanupTime());
        
        // 添加间隔毫秒值
        status.setTaskCleanupIntervalMs(this.taskCleanupIntervalMs);
        status.setConnectionCleanupIntervalMs(this.connectionCleanupIntervalMs);
        
        // 添加可读间隔
        status.setTaskCleanupInterval(formatTimeInterval(this.taskCleanupIntervalMs));
        status.setConnectionCleanupInterval(formatTimeInterval(this.connectionCleanupIntervalMs));
        
        // 添加SSH会话缓存命中率
        status.setSessionCacheHitRate(calculateSessionCacheHitRate());
        
        return status;
    }
    
    /**
     * 仅停止任务清理定时任务
     */
    public void stopTaskCleanup() {
        if (taskCleanupTask != null && !taskCleanupTask.isCancelled()) {
            taskCleanupTask.cancel(false);
            logger.info("任务清理定时任务已停止");
        }
    }
    
    /**
     * 仅停止连接清理定时任务
     */
    public void stopConnectionCleanup() {
        if (connectionCleanupTask != null && !connectionCleanupTask.isCancelled()) {
            connectionCleanupTask.cancel(false);
            logger.info("连接清理定时任务已停止");
        }
    }
    
    /**
     * 仅启动任务清理定时任务
     */
    public void startTaskCleanup() {
        if (taskCleanupTask == null || taskCleanupTask.isCancelled()) {
            taskCleanupTask = taskScheduler.scheduleAtFixedRate(
                this::cleanupTasks, taskCleanupIntervalMs);
            logger.info("任务清理定时任务已启动，执行间隔: {}毫秒", taskCleanupIntervalMs);
        }
    }
    
    /**
     * 仅启动连接清理定时任务
     */
    public void startConnectionCleanup() {
        if (connectionCleanupTask == null || connectionCleanupTask.isCancelled()) {
            connectionCleanupTask = taskScheduler.scheduleAtFixedRate(
                this::cleanupConnections, connectionCleanupIntervalMs);
            logger.info("连接清理定时任务已启动，执行间隔: {}毫秒", connectionCleanupIntervalMs);
        }
    }
    
    /**
     * 更新任务清理定时任务执行间隔
     * @param intervalMs 执行间隔（毫秒）
     */
    public void updateTaskCleanupInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("任务清理定时任务间隔不能小于1秒，忽略此次更新");
            return;
        }
        
        this.taskCleanupIntervalMs = intervalMs;
        
        if (taskCleanupTask != null && !taskCleanupTask.isCancelled()) {
            taskCleanupTask.cancel(false);
            taskCleanupTask = taskScheduler.scheduleAtFixedRate(
                this::cleanupTasks, intervalMs);
            logger.info("任务清理定时任务已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
    }
    
    /**
     * 更新连接清理定时任务执行间隔
     * @param intervalMs 执行间隔（毫秒）
     */
    public void updateConnectionCleanupInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("连接清理定时任务间隔不能小于1秒，忽略此次更新");
            return;
        }
        
        this.connectionCleanupIntervalMs = intervalMs;
        
        if (connectionCleanupTask != null && !connectionCleanupTask.isCancelled()) {
            connectionCleanupTask.cancel(false);
            connectionCleanupTask = taskScheduler.scheduleAtFixedRate(
                this::cleanupConnections, intervalMs);
            logger.info("连接清理定时任务已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
    }
    
    /**
     * 在一个会话上执行命令
     */
    private CommandResult execCommand(ClientSession session, String command) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            
            ClientChannel channel = session.createExecChannel(command);
            channel.setOut(outputStream);
            channel.setErr(errorStream);
            
            // 打开通道
            channel.open().verify(30, TimeUnit.SECONDS);
            
            // 等待命令完成
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 30000);
            
            // 获取退出状态
            Integer exitStatus = channel.getExitStatus();
            String output = outputStream.toString();
            String error = errorStream.toString();
            
            // 关闭通道
            channel.close();
            
            return new CommandResult(output, error, exitStatus != null ? exitStatus : -1);
        } catch (Exception e) {
            return new CommandResult("", e.getMessage(), -1);
        }
    }
    
    /**
     * 批量执行检查项，复用SSH连接
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItems 检查项列表
     * @return 检查结果列表
     */
    public List<CheckItem> batchExecuteCheck(Integer clusterId, HostInfo hostInfo, List<CheckItem> checkItems) {
        List<CheckItem> results = new ArrayList<>();
        ClientSession session = null;
        String hostKey = hostInfo.getHostname() + ":" + hostInfo.getSshPort();
        
        try {
            // 尝试获取或创建一个连接
            session = getOrCreateConnection(hostInfo);
            if (session == null || !session.isOpen()) {
                logger.error("无法建立到主机 {} 的SSH连接", hostInfo.getHostname());
                // 标记所有检查项为失败
                for (CheckItem item : checkItems) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("无法建立SSH连接");
                    results.add(item);
                }
                return results;
            }
            
            // 标记使用现有会话并设置外部会话 - 这里是关键
            hostInfo.setUseExistingSession(true);
            hostInfo.setExternalSession(session);
            
            logger.debug("批量执行检查 - 已设置SSH会话: session.isOpen={}, hostInfo.useExistingSession={}", 
                session.isOpen(), hostInfo.isUseExistingSession());
            
            // 验证会话设置是否正确
            if (!hostInfo.isSessionReady()) {
                logger.error("会话设置后未就绪: externalSession={}, useExistingSession={}", 
                    hostInfo.getExternalSession() != null, hostInfo.isUseExistingSession());
            }
            
            // 更新最后访问时间
            connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
            
            // 执行每个检查项
            for (CheckItem item : checkItems) {
                try {
                    // 获取相应的检查器
                    ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(item.getItemCode()));
                    if (checker == null) {
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("找不到检查器: " + item.getItemName());
                        results.add(item);
                        continue;
                    }
                    
                    logger.debug("开始执行检查项 {}, 使用现有SSH连接: {}", item.getItemName(), hostInfo.isUseExistingSession());
                    
                    // 确保每个检查项都使用同一个会话 - 确保这个标志设置正确
                    hostInfo.setUseExistingSession(true);
                    hostInfo.setExternalSession(session);
                    
                    // 再次验证会话是否就绪
                    if (!hostInfo.isSessionReady()) {
                        logger.error("执行检查前会话未就绪: {}", item.getItemName());
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("SSH会话未就绪");
                        results.add(item);
                        continue;
                    }
                    
                    // 执行检查
                    CheckItem result = checker.check(clusterId, hostInfo, item);
                    results.add(result);
                    
                    // 更新最后访问时间
                    connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
                } catch (Exception e) {
                    logger.error("执行检查项 {} 时发生异常: {}", item.getItemName(), e.getMessage(), e);
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("检查异常: " + e.getMessage());
                    results.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("批量执行检查时发生异常: {}", e.getMessage(), e);
            // 标记所有剩余检查项为失败
            for (CheckItem item : checkItems) {
                if (!results.contains(item)) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("批量检查异常: " + e.getMessage());
                    results.add(item);
                }
            }
        } finally {
            // 执行完毕后清理，但不关闭会话
            logger.debug("批量检查执行完毕，清理hostInfo引用，但不关闭会话");
            hostInfo.setExternalSession(null);
            hostInfo.setUseExistingSession(false);
        }
        
        return results;
    }
    
    /**
     * 批量执行修复项，复用SSH连接
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param fixItems 修复项列表
     * @return 修复结果列表
     */
    public List<CheckItem> batchExecuteFix(Integer clusterId, HostInfo hostInfo, List<CheckItem> fixItems) {
        List<CheckItem> results = new ArrayList<>();
        ClientSession session = null;
        String hostKey = hostInfo.getHostname() + ":" + hostInfo.getSshPort();
        
        try {
            // 尝试获取或创建一个连接
            session = getOrCreateConnection(hostInfo);
            if (session == null || !session.isOpen()) {
                logger.error("无法建立到主机 {} 的SSH连接", hostInfo.getHostname());
                // 标记所有修复项为失败
                for (CheckItem item : fixItems) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("无法建立SSH连接");
                    results.add(item);
                }
                return results;
            }
            
            // 标记使用现有会话并设置外部会话 - 这里是关键
            hostInfo.setUseExistingSession(true);
            hostInfo.setExternalSession(session);
            
            logger.debug("批量执行修复 - 已设置SSH会话: session.isOpen={}, hostInfo.useExistingSession={}", 
                session.isOpen(), hostInfo.isUseExistingSession());
            
            // 验证会话设置是否正确
            if (!hostInfo.isSessionReady()) {
                logger.error("会话设置后未就绪: externalSession={}, useExistingSession={}", 
                    hostInfo.getExternalSession() != null, hostInfo.isUseExistingSession());
            }
            
            // 更新最后访问时间
            connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
            
            // 执行每个修复项
            for (CheckItem item : fixItems) {
                try {
                    // 获取相应的检查器
                    ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(item.getItemCode()));
                    if (checker == null) {
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("找不到检查器: " + item.getItemName());
                        results.add(item);
                        continue;
                    }
                    
                    logger.debug("开始执行修复项 {}, 使用现有SSH连接: {}", item.getItemName(), hostInfo.isUseExistingSession());
                    
                    // 确保每个修复项都使用同一个会话 - 确保这个标志设置正确
                    hostInfo.setUseExistingSession(true);
                    hostInfo.setExternalSession(session);
                    
                    // 再次验证会话是否就绪
                    if (!hostInfo.isSessionReady()) {
                        logger.error("执行修复前会话未就绪: {}", item.getItemName());
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("SSH会话未就绪");
                        results.add(item);
                        continue;
                    }
                    
                    // 执行修复
                    boolean fixResult = checker.fix(clusterId, hostInfo, item);
                    if (fixResult) {
                        item.setStatus(CheckItem.Status.SUCCESS);
                        item.setMessage("修复成功");
                    } else {
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("修复失败");
                    }
                    results.add(item);
                    
                    // 更新最后访问时间
                    connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
                } catch (Exception e) {
                    logger.error("执行修复项 {} 时发生异常: {}", item.getItemName(), e.getMessage(), e);
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("修复异常: " + e.getMessage());
                    results.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("批量执行修复时发生异常: {}", e.getMessage(), e);
            // 标记所有剩余修复项为失败
            for (CheckItem item : fixItems) {
                if (!results.contains(item)) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("批量修复异常: " + e.getMessage());
                    results.add(item);
                }
            }
        } finally {
            // 执行完毕后清理，但不关闭会话
            logger.debug("批量修复执行完毕，清理hostInfo引用，但不关闭会话");
            hostInfo.setExternalSession(null);
            hostInfo.setUseExistingSession(false);
        }
        
        return results;
    }
} 