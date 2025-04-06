package com.datasophon.api.service.checker;

import com.datasophon.api.service.checker.config.TaskManager;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.LogEntryManager;
import com.datasophon.api.service.checker.core.ItemChecker;
import com.datasophon.api.service.checker.core.ItemCheckerFactory;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.AsyncServiceStatus;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.service.checker.common.ItemCode;
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
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;

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

    // 操作系统信息获取专用执行器
    @Autowired
    @Qualifier("osInfoExecutor")
    private ExecutorService osInfoExecutor;

    // 硬件信息获取专用执行器
    @Autowired
    @Qualifier("hardwareInfoExecutor")
    private ExecutorService hardwareInfoExecutor;

    // Hosts文件操作专用执行器
    @Autowired
    @Qualifier("hostsFileExecutor")
    private ExecutorService hostsFileExecutor;

    // 主机名设置专用执行器
    @Autowired
    @Qualifier("hostnameExecutor")
    private ExecutorService hostnameExecutor;

    // SSH连接池 - 按主机名缓存SSH连接
    private final Map<String, ClientSession> hostConnectionPool = new ConcurrentHashMap<>();

    // 连接锁，防止并发问题
    private final Map<String, Object> connectionLocks = new ConcurrentHashMap<>();

    // 定时任务启用标志
    private final AtomicBoolean scheduledTasksEnabled = new AtomicBoolean(true);

    // 定时任务执行间隔（默认值）
    private long taskCleanupIntervalMs = TimeUnit.SECONDS.toMillis(60); // 默认60秒
    private long connectionCleanupIntervalMs = TimeUnit.SECONDS.toMillis(60); // 默认60秒

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

    @Autowired
    private com.datasophon.api.service.checker.common.SshConnectionPoolManager sshConnectionPoolManager;

    @PostConstruct
    public void init() {
        logger.info("初始化异步检查服务...");
        // 注释掉自动启动定时任务的代码
        // startScheduledTasks();
        // 将定时任务标志设置为已停用
        scheduledTasksEnabled.set(false);
        logger.info("异步检查服务初始化完成，定时任务默认关闭");
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

        // 启动任务清理定时任务（每60秒执行一次）
        if (taskCleanupTask == null || taskCleanupTask.isCancelled()) {
            taskCleanupTask = taskScheduler.scheduleAtFixedRate(
                    this::cleanupTasks, taskCleanupIntervalMs);
            logger.info("任务清理定时任务已启动，执行间隔: {}毫秒", taskCleanupIntervalMs);
        }

        // 启动连接清理定时任务（每60秒执行一次）
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
     * 
     * @return 定时任务状态对象
     */
    public ScheduledTasksStatus getScheduledTasksStatus() {
        ScheduledTasksStatus status = new ScheduledTasksStatus();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        status.setScheduledTasksEnabled(scheduledTasksEnabled != null ? scheduledTasksEnabled.get() : false);
        status.setTaskCleanupActive(taskCleanupTask != null && !taskCleanupTask.isCancelled());
        status.setConnectionCleanupActive(connectionCleanupTask != null && !connectionCleanupTask.isCancelled());

        // 添加定时任务执行间隔
        status.setTaskCleanupIntervalMs(Long.valueOf(this.taskCleanupIntervalMs));
        status.setConnectionCleanupIntervalMs(Long.valueOf(this.connectionCleanupIntervalMs));

        // 格式化为人类可读的时间间隔
        status.setTaskCleanupInterval(formatTimeInterval(this.taskCleanupIntervalMs));
        status.setConnectionCleanupInterval(formatTimeInterval(this.connectionCleanupIntervalMs));

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

        status.setConnectionPoolSize(hostConnectionPool != null ? hostConnectionPool.size() : 0);
        status.setRunningTasksCount(runningTasks != null ? runningTasks.size() : 0);
        return status;
    }

    /**
     * 设置任务清理定时任务执行间隔
     * 
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
     * 
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
     * 
     * @param ms 毫秒数
     * @return 格式化后的时间间隔
     */
    private String formatTimeInterval(long ms) {
        if (ms <= 0) {
            return "0秒";
        }

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
     * 异步执行检查检查项
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @return 任务ID
     */
    public String executeCheckItemAsync(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        String taskKey = getTaskKey(clusterId, hostInfo.getIp(), checkItem.getId());

        // 检查任务是否已在运行
        if (isTaskRunning(taskKey)) {
            logger.warn("任务已在运行中: {}", taskKey);
            return taskKey;
        }

        // 创建并注册异步任务 - 使用doCheckAsync
        CompletableFuture<CheckItem> future = doCheckAsync(clusterId, hostInfo, checkItem);

        // 注册任务
        String taskId = taskManager.registerTask("CHECK",
                "检查项: " + checkItem.getItemName() + ", 主机: " + hostInfo.getIp(), future);

        // 记录任务信息
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.taskId = taskId;
        taskInfo.future = future;
        taskInfo.clusterId = clusterId;
        taskInfo.hostname = hostInfo.getIp();
        taskInfo.itemId = checkItem.getId();
        taskInfo.setExecutorName("checkExecutor");

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
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @return 任务ID
     */
    public String executeFixItemAsync(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        String taskKey = "FIX_" + getTaskKey(clusterId, hostInfo.getIp(), checkItem.getId());

        // 检查任务是否已在运行
        if (isTaskRunning(taskKey)) {
            logger.warn("修复任务已在运行中: {}", taskKey);
            return taskKey;
        }

        // 创建并注册异步任务 - 使用doFixAsync
        CompletableFuture<Boolean> future = doFixAsync(clusterId, hostInfo, checkItem);

        // 注册任务
        String taskId = taskManager.registerTask("FIX",
                "修复检查项: " + checkItem.getItemName() + ", 主机: " + hostInfo.getIp(), future);

        // 记录任务信息
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.taskId = taskId;
        taskInfo.future = future;
        taskInfo.clusterId = clusterId;
        taskInfo.hostname = hostInfo.getIp();
        taskInfo.itemId = checkItem.getId();
        taskInfo.setExecutorName("fixExecutor");

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
     * 
     * @param clusterId 集群ID
     * @param hostname  主机名
     * @param itemId    检查项ID
     * @return 是否成功取消
     */
    public boolean cancelCheckTask(Integer clusterId, String hostname, Integer itemId) {
        String taskKey = getTaskKey(clusterId, hostname, itemId);
        return cancelTask(taskKey);
    }

    /**
     * 取消修复检查项任务
     * 
     * @param clusterId 集群ID
     * @param hostname  主机名
     * @param itemId    检查项ID
     * @return 是否成功取消
     */
    public boolean cancelFixTask(Integer clusterId, String hostname, Integer itemId) {
        String taskKey = "FIX_" + getTaskKey(clusterId, hostname, itemId);
        return cancelTask(taskKey);
    }

    /**
     * 取消所有主机检查任务
     * 
     * @param clusterId 集群ID
     * @param hostname  主机名
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
     * 
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
            checkItem.setMessage("执行检查异常: " + e.getMessage());
            return checkItem;
        }
    }

    /**
     * 异步执行检查逻辑
     */
    private CompletableFuture<CheckItem> doCheckAsync(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doCheck(clusterId, hostInfo, checkItem);
            } catch (Exception e) {
                logger.error("异步执行检查时发生异常: {}", e.getMessage(), e);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("执行检查异常: " + e.getMessage());
                return checkItem;
            }
        }, checkExecutor);
    }

    /**
     * 执行实际修复逻辑
     */
    private boolean doFix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 获取相应的检查器
            ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
            if (checker == null) {
                logger.error("找不到检查项 {} 的检查器", checkItem.getItemName());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("找不到检查器");
                return false;
            }

            // 执行修复
            boolean fixResult = checker.fix(clusterId, hostInfo, checkItem);
            if (fixResult) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                if (checkItem.getMessage() == null || checkItem.getMessage().isEmpty()) {
                    checkItem.setMessage("修复成功");
                }
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);
                if (checkItem.getMessage() == null || checkItem.getMessage().isEmpty()) {
                    checkItem.setMessage("修复失败");
                }
            }
            return fixResult;
        } catch (Exception e) {
            logger.error("执行修复时发生异常: {}", e.getMessage(), e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("执行修复异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 异步执行修复逻辑
     */
    private CompletableFuture<Boolean> doFixAsync(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doFix(clusterId, hostInfo, checkItem);
            } catch (Exception e) {
                logger.error("异步执行修复时发生异常: {}", e.getMessage(), e);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("执行修复异常: " + e.getMessage());
                return false;
            }
        }, fixExecutor);
    }

    /**
     * 获取或创建SSH连接
     * 
     * @param hostInfo 主机信息
     * @return SSH会话，如果创建失败则返回null
     */
    private ClientSession getOrCreateConnection(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.error("主机信息为空，无法创建连接");
            return null;
        }

        if (hostInfo.getIp() == null || hostInfo.getSshPort() == null) {
            logger.error("主机IP或SSH端口为空，无法创建连接: {}", hostInfo.getIp());
            return null;
        }

        // 检查必要的Map对象是否初始化

        String hostKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();

        // 增加总请求计数
        long requests = hostCacheRequests.getOrDefault(hostKey, 0L) + 1;
        hostCacheRequests.put(hostKey, requests);

        // 获取连接锁，确保同一主机的连接操作串行化
        Object lock = connectionLocks.computeIfAbsent(hostKey, k -> new Object());

        synchronized (lock) {
            ClientSession session = hostConnectionPool.get(hostKey);

            // 检查连接是否存在且有效
            if (session != null) {
                try {
                    // 检查连接是否仍然可用
                    if (session.isOpen()) {
                        // 尝试发送一个无害的命令来验证连接是否真正有效
                        CommandResult testResult = execCommand(session, "echo connection_test");
                        if (testResult.isSuccess() && testResult.getOutput().trim().contains("connection_test")) {
                            logger.debug("复用主机 {} 的现有SSH连接 (健康检查通过)", hostInfo.getIp());
                            // 更新最后访问时间
                            connectionLastAccessTime.put(hostKey, System.currentTimeMillis());

                            // 增加缓存命中计数
                            long hits = hostCacheHits.getOrDefault(hostKey, 0L) + 1;
                            hostCacheHits.put(hostKey, hits);

                            return session;
                        } else {
                            logger.warn("主机 {} 的SSH连接健康检查失败，将创建新连接", hostInfo.getIp());
                        }
                    } else {
                        logger.info("主机 {} 的SSH连接已关闭，将创建新连接", hostInfo.getIp());
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
                logger.info("创建主机 {} 的新SSH连接", hostInfo.getIp());
                session = MinaUtils.openConnectionWithPassword(hostInfo);

                if (session != null) {
                    hostConnectionPool.put(hostKey, session);

                    // 设置初始访问时间
                    connectionLastAccessTime.put(hostKey, System.currentTimeMillis());

                    logger.info("成功创建主机 {} 的SSH连接", hostInfo.getIp());
                }
                return session;
            } catch (Exception e) {
                logger.error("建立SSH连接失败: {}", e.getMessage(), e);
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

        // 使用checkExecutor异步执行任务清理
        CompletableFuture.runAsync(() -> {
            try {
                int count = taskManager.cleanCompletedTasks(24 * 60 * 60 * 1000); // 24小时
                lastTaskCleanupTime = System.currentTimeMillis();
                logger.info("清理了 {} 个过期任务记录", count);
            } catch (Exception e) {
                logger.error("清理过期任务时发生异常: {}", e.getMessage(), e);
            }
        }, checkExecutor);
    }

    /**
     * 定期清理不活跃连接
     * 每10分钟执行一次
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void cleanupConnections() {
        if (scheduledTasksEnabled == null || !scheduledTasksEnabled.get()) {
            logger.debug("定时任务已禁用，跳过执行cleanupConnections()");
            return;
        }

        if (hostConnectionPool == null || hostConnectionPool.isEmpty()) {
            logger.debug("连接池为空，跳过清理");
            return;
        }

        // 使用checkExecutor异步执行连接清理
        CompletableFuture.runAsync(() -> {
            try {
                int closedCount = 0;
                int idleClosedCount = 0;
                logger.info("开始清理不活跃SSH连接...");

                long currentTime = System.currentTimeMillis();
                long idleTimeout = TimeUnit.MINUTES.toMillis(1); // 1分钟没有使用的连接将被关闭

                List<String> keysToRemove = new ArrayList<>();

                for (Map.Entry<String, ClientSession> entry : hostConnectionPool.entrySet()) {
                    String key = entry.getKey();
                    if (key == null) {
                        continue;
                    }

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
                        Long lastAccess = connectionLastAccessTime != null ? connectionLastAccessTime.get(key) : null;
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
                    if (key != null) {
                        hostConnectionPool.remove(key);
                        // 同时也要移除对应的访问时间记录
                        if (connectionLastAccessTime != null) {
                            connectionLastAccessTime.remove(key);
                        }
                    }
                }

                lastConnectionCleanupTime = System.currentTimeMillis();
                logger.info("SSH连接清理完成，关闭{}个失效连接，{}个空闲连接，当前连接池大小: {}",
                        closedCount, idleClosedCount, hostConnectionPool.size());

                // 日志记录当前缓存命中率
                try {
                    int hitRate = calculateSessionCacheHitRate();
                    logger.info("当前SSH会话缓存命中率: {}%", hitRate);
                } catch (Exception e) {
                    logger.warn("计算缓存命中率时发生异常: {}", e.getMessage());
                }
            } catch (Exception e) {
                logger.error("清理连接池时发生异常: {}", e.getMessage(), e);
            }
        }, checkExecutor);
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
        String executorName;

        void setExecutorName(String executorName) {
            this.executorName = executorName;
        }
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
                    hostInfo.getIp(), checkItem.getId(), checkItem.getStatus(), checkItem.getMessage());

            try {
                Map<String, HostInfo> hostInfoMap = (Map<String, HostInfo>) CacheUtils.get(cacheKey);
                if (hostInfoMap != null) {
                    HostInfo cachedHostInfo = hostInfoMap.get(hostInfo.getIp());
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
                            hostInfoMap.put(hostInfo.getIp(), cachedHostInfo);
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
     * 
     * @return 缓存命中百分比
     */
    private int calculateSessionCacheHitRate() {
        if (hostCacheRequests == null || hostCacheHits == null) {
            return 0;
        }

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
     * 
     * @return AsyncServiceStatus对象
     */
    public AsyncServiceStatus getAsyncServiceStatus() {
        AsyncServiceStatus status = new AsyncServiceStatus();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            // 获取状态信息
            ScheduledTasksStatus statusInfo = getScheduledTasksStatus();
            if (statusInfo != null) {
                // 填充实体类
                status.setScheduledTasksEnabled(statusInfo.isScheduledTasksEnabled());
                status.setLastTaskCleanupTime(statusInfo.getLastTaskCleanupTime());
                status.setRunningTasksCount(statusInfo.getRunningTasksCount());
                status.setConnectionPoolSize(statusInfo.getConnectionPoolSize());
                status.setTaskCleanupActive(statusInfo.isTaskCleanupActive());
                status.setConnectionCleanupActive(statusInfo.isConnectionCleanupActive());
                status.setLastConnectionCleanupTime(statusInfo.getLastConnectionCleanupTime());
            } else {
                // 如果状态信息为空，设置默认值
                status.setScheduledTasksEnabled(false);
                status.setLastTaskCleanupTime("未获取到");
                status.setRunningTasksCount(0);
                status.setConnectionPoolSize(0);
                status.setTaskCleanupActive(false);
                status.setConnectionCleanupActive(false);
                status.setLastConnectionCleanupTime("未获取到");
            }

            // 添加间隔毫秒值
            status.setTaskCleanupIntervalMs(Long.valueOf(this.taskCleanupIntervalMs));
            status.setConnectionCleanupIntervalMs(Long.valueOf(this.connectionCleanupIntervalMs));

            // 添加可读间隔
            status.setTaskCleanupInterval(formatTimeInterval(this.taskCleanupIntervalMs));
            status.setConnectionCleanupInterval(formatTimeInterval(this.connectionCleanupIntervalMs));

            // 添加SSH会话缓存命中率
            int cacheHitRate = calculateSessionCacheHitRate();
            status.setSessionCacheHitRate(cacheHitRate);
        } catch (Exception e) {
            // 异常处理，设置默认值
            logger.error("获取异步服务状态时发生异常", e);
            status.setScheduledTasksEnabled(false);
            status.setLastTaskCleanupTime("获取异常");
            status.setRunningTasksCount(0);
            status.setConnectionPoolSize(0);
            status.setTaskCleanupActive(false);
            status.setConnectionCleanupActive(false);
            status.setLastConnectionCleanupTime("获取异常");
            status.setTaskCleanupIntervalMs(Long.valueOf(this.taskCleanupIntervalMs));
            status.setConnectionCleanupIntervalMs(Long.valueOf(this.connectionCleanupIntervalMs));
            status.setTaskCleanupInterval(formatTimeInterval(this.taskCleanupIntervalMs));
            status.setConnectionCleanupInterval(formatTimeInterval(this.connectionCleanupIntervalMs));
            status.setSessionCacheHitRate(0);
        }

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
     * 
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
     * 
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
     * 异步执行命令
     * 根据命令类型选择合适的线程池执行
     * 
     * @param session 会话
     * @param command 要执行的命令
     * @return 包含命令执行结果的CompletableFuture
     */
    public CompletableFuture<CommandResult> execCommandAsync(ClientSession session, String command) {
        if (session == null) {
            CompletableFuture<CommandResult> future = new CompletableFuture<>();
            future.complete(new CommandResult("", "会话为空", -1));
            return future;
        }

        // 根据命令类型选择执行器
        ExecutorService executor = determineExecutorForCommand(command);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return execCommand(session, command);
            } catch (Exception e) {
                logger.error("异步执行命令时发生异常: {}", e.getMessage(), e);
                return new CommandResult("", "执行异常: " + e.getMessage(), -1);
            }
        }, executor);
    }

    /**
     * 根据命令类型确定使用哪个执行器
     * 
     * @param command 命令内容
     * @return 适合的执行器
     */
    private ExecutorService determineExecutorForCommand(String command) {
        // 命令字符串转小写便于比较
        String lowerCommand = command.toLowerCase();

        // 第一阶段信息收集 - 操作系统基本信息和主机名，主列表显示，优先级高
        // 操作系统信息相关命令使用osInfoExecutor
        if (lowerCommand.contains("uname") ||
                lowerCommand.contains("cat /etc/os-release") ||
                lowerCommand.contains("cat /etc/system-release") ||
                lowerCommand.contains("hostname") ||
                lowerCommand.contains("cat /etc/issue")) {
            logger.debug("【第一阶段】使用操作系统信息执行器执行命令: {}", command);
            return osInfoExecutor;
        }

        // 第二阶段信息收集 - 悬浮卡片中显示的信息，优先级低
        // 硬件信息相关命令使用hardwareInfoExecutor
        if (lowerCommand.contains("lscpu") ||
                lowerCommand.contains("lspci") ||
                lowerCommand.contains("lsblk") ||
                lowerCommand.contains("free -m") ||
                lowerCommand.contains("dmidecode") ||
                lowerCommand.contains("cat /proc/meminfo") ||
                lowerCommand.contains("nvidia-smi") ||
                // DNS相关命令 - 确保所有DNS相关命令都被列出
                lowerCommand.contains("cat /etc/resolv.conf") ||
                lowerCommand.contains("dig") ||
                lowerCommand.contains("nslookup") ||
                lowerCommand.contains("host ") ||
                lowerCommand.contains("getent hosts") ||
                lowerCommand.contains("systemd-resolve") ||
                // hosts文件相关命令 - 确保所有hosts文件相关命令都被列出
                lowerCommand.contains("/etc/hosts") ||
                lowerCommand.contains("ping -c") ||
                lowerCommand.contains("ping ") ||
                lowerCommand.contains("traceroute") ||
                lowerCommand.contains("cat /etc/nsswitch.conf")) {
            logger.debug("【第二阶段】使用硬件信息执行器执行命令: {}", command);
            return hardwareInfoExecutor;
        }

        // 主机名设置相关命令使用hostnameExecutor
        if (lowerCommand.contains("hostnamectl set-hostname") ||
                (lowerCommand.contains("echo") && lowerCommand.contains("/etc/hostname"))) {
            logger.debug("使用主机名设置执行器执行命令: {}", command);
            return hostnameExecutor;
        }

        // 其他命令使用默认执行器
        return checkExecutor;
    }

    /**
     * 异步执行硬件信息收集命令
     * 专门使用hardwareInfoExecutor线程池
     * 
     * @param session 会话
     * @param command 要执行的命令
     * @return 包含命令执行结果的CompletableFuture
     */
    public CompletableFuture<CommandResult> execHardwareInfoCommandAsync(ClientSession session, String command) {
        if (session == null) {
            CompletableFuture<CommandResult> future = new CompletableFuture<>();
            future.complete(new CommandResult("", "会话为空", -1));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("使用硬件信息执行器执行命令: {}", command);
                return execCommand(session, command);
            } catch (Exception e) {
                logger.error("异步执行硬件信息命令时发生异常: {}", e.getMessage(), e);
                return new CommandResult("", "执行异常: " + e.getMessage(), -1);
            }
        }, hardwareInfoExecutor);
    }

    /**
     * 异步执行操作系统信息收集命令
     * 专门使用osInfoExecutor线程池
     * 
     * @param session 会话
     * @param command 要执行的命令
     * @return 包含命令执行结果的CompletableFuture
     */
    public CompletableFuture<CommandResult> execOsInfoCommandAsync(ClientSession session, String command) {
        if (session == null) {
            CompletableFuture<CommandResult> future = new CompletableFuture<>();
            future.complete(new CommandResult("", "会话为空", -1));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("使用操作系统信息执行器执行命令: {}", command);
                return execCommand(session, command);
            } catch (Exception e) {
                logger.error("异步执行操作系统信息命令时发生异常: {}", e.getMessage(), e);
                return new CommandResult("", "执行异常: " + e.getMessage(), -1);
            }
        }, osInfoExecutor);
    }

    /**
     * 批量执行检查项，复用SSH连接
     * 
     * @param clusterId  集群ID
     * @param hostInfo   主机信息
     * @param checkItems 检查项列表
     * @return 检查结果列表
     */
    public List<CheckItem> batchExecuteCheck(Integer clusterId, HostInfo hostInfo, List<CheckItem> checkItems) {
        List<CheckItem> results = new ArrayList<>();
        ClientSession session = null;
        String hostKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();

        try {
            // 尝试获取或创建一个连接
            session = getOrCreateConnection(hostInfo);
            if (session == null || !session.isOpen()) {
                logger.error("无法建立到主机 {} 的SSH连接", hostInfo.getIp());
                // 标记所有检查项为失败
                for (CheckItem item : checkItems) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("无法建立SSH连接");

                    // 记录失败日志到缓存日志 - 确保每个检查项都有日志记录
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry logEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "无法建立到主机 " + hostInfo.getIp() + " 的SSH连接",
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, logEntry);

                    results.add(item);
                    continue;
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

                        // 记录失败日志到缓存日志
                        String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_"
                                + item.getId();
                        com.datasophon.common.model.LogEntry logEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.ERROR,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "找不到检查器: " + item.getItemName(),
                                com.datasophon.common.model.LogEntry.Type.CHECK);
                        LogEntryManager.addLogEntry(logKey, logEntry);

                        results.add(item);
                        continue;
                    }

                    // 手动创建并存储检查项的日志键和开始日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();

                    // 记录检查开始日志
                    com.datasophon.common.model.LogEntry startLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.INFO,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "开始检查项: " + item.getItemName() + ", 使用SSH连接复用机制",
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, startLogEntry);

                    logger.debug("开始执行检查项 {}, 使用现有SSH连接: {}", item.getItemName(), hostInfo.isUseExistingSession());

                    // 确保每个检查项都使用同一个会话 - 确保这个标志设置正确
                    hostInfo.setUseExistingSession(true);
                    hostInfo.setExternalSession(session);

                    // 再次验证会话是否就绪
                    if (!hostInfo.isSessionReady()) {
                        logger.error("执行检查前会话未就绪: {}", item.getItemName());
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("SSH会话未就绪");

                        // 记录失败日志到缓存日志
                        com.datasophon.common.model.LogEntry errorLogEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.ERROR,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "执行检查前会话未就绪: " + item.getItemName(),
                                com.datasophon.common.model.LogEntry.Type.CHECK);
                        LogEntryManager.addLogEntry(logKey, errorLogEntry);

                        results.add(item);
                        continue;
                    }

                    // 执行检查
                    CheckItem result = checker.check(clusterId, hostInfo, item);
                    results.add(result);

                    // 记录检查完成日志
                    com.datasophon.common.model.LogEntry endLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.INFO,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "检查项 " + item.getItemName() + " 完成，状态: " + result.getStatus(),
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, endLogEntry);

                    // 更新最后访问时间
                    connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
                } catch (Exception e) {
                    logger.error("执行检查项 {} 时发生异常: {}", item.getItemName(), e.getMessage(), e);
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("检查异常: " + e.getMessage());

                    // 记录异常日志到缓存日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry exceptionLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "执行检查项 " + item.getItemName() + " 时发生异常: " + e.getMessage(),
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, exceptionLogEntry);

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

                    // 记录失败日志到缓存日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry errorLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "批量执行检查时发生异常: " + e.getMessage(),
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, errorLogEntry);

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
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param fixItems  修复项列表
     * @return 修复结果列表
     */
    public List<CheckItem> batchExecuteFix(Integer clusterId, HostInfo hostInfo, List<CheckItem> fixItems) {
        List<CheckItem> results = new ArrayList<>();
        ClientSession session = null;
        String hostKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();

        try {
            // 检查是否包含免密登录检查项，如果包含则不使用连接池
            boolean containsPasswordFreeChecker = fixItems.stream()
                    .anyMatch(item -> ItemCode.PASSWORD_FREE.toString().equals(item.getItemCode()));

            // 如果是免密修复，则跳过连接池
            if (!containsPasswordFreeChecker) {
                // 尝试获取或创建一个连接
                session = getOrCreateConnection(hostInfo);
                if (session == null || !session.isOpen()) {
                    logger.error("无法建立到主机 {} 的SSH连接", hostInfo.getIp());
                    // 标记所有修复项为失败
                    for (CheckItem item : fixItems) {
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("无法建立SSH连接");

                        // 记录失败日志到缓存日志
                        String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_"
                                + item.getId();
                        com.datasophon.common.model.LogEntry logEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.ERROR,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "无法建立到主机 " + hostInfo.getIp() + " 的SSH连接",
                                com.datasophon.common.model.LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, logEntry);

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
            } else {
                logger.info("检测到免密登录修复项，将跳过连接池直接执行修复");
                hostInfo.setUseExistingSession(false);
                hostInfo.setExternalSession(null);
            }

            // 执行每个修复项
            for (CheckItem item : fixItems) {
                try {
                    // 获取相应的检查器
                    ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(item.getItemCode()));
                    if (checker == null) {
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("找不到检查器: " + item.getItemName());

                        // 记录失败日志到缓存日志
                        String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_"
                                + item.getId();
                        com.datasophon.common.model.LogEntry logEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.ERROR,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "找不到检查器: " + item.getItemName(),
                                com.datasophon.common.model.LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, logEntry);

                        results.add(item);
                        continue;
                    }

                    // 手动创建并存储修复项的日志键和开始日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();

                    // 记录修复开始日志
                    com.datasophon.common.model.LogEntry startLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.INFO,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "开始修复项: " + item.getItemName() + ", 使用SSH连接复用机制",
                            com.datasophon.common.model.LogEntry.Type.FIX);
                    LogEntryManager.addLogEntry(logKey, startLogEntry);

                    // 对于免密登录检查项，始终使用独立会话
                    boolean isPasswordFreeItem = ItemCode.PASSWORD_FREE.toString().equals(item.getItemCode());
                    if (isPasswordFreeItem) {
                        // 免密检查项总是使用独立会话，不使用共享连接池
                        hostInfo.setUseExistingSession(false);
                        hostInfo.setExternalSession(null);
                        logger.info("执行免密登录修复项，使用独立SSH连接");

                        // 记录使用独立连接的日志
                        com.datasophon.common.model.LogEntry connLogEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.INFO,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "免密登录修复项使用独立SSH连接",
                                com.datasophon.common.model.LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, connLogEntry);
                    } else if (session != null) {
                        // 非免密检查项继续使用共享会话
                        logger.debug("开始执行修复项 {}, 使用现有SSH连接: {}", item.getItemName(), hostInfo.isUseExistingSession());

                        // 确保每个修复项都使用同一个会话 - 确保这个标志设置正确
                        hostInfo.setUseExistingSession(true);
                        hostInfo.setExternalSession(session);

                        // 再次验证会话是否就绪
                        if (!hostInfo.isSessionReady()) {
                            logger.error("执行修复前会话未就绪: {}", item.getItemName());
                            item.setStatus(CheckItem.Status.FAILED);
                            item.setMessage("SSH会话未就绪");

                            // 记录失败日志到缓存日志
                            com.datasophon.common.model.LogEntry errorLogEntry = new com.datasophon.common.model.LogEntry(
                                    new Date(),
                                    com.datasophon.common.model.LogEntry.Level.ERROR,
                                    Thread.currentThread().getName(),
                                    this.getClass().getSimpleName(),
                                    "执行修复前会话未就绪: " + item.getItemName(),
                                    com.datasophon.common.model.LogEntry.Type.FIX);
                            LogEntryManager.addLogEntry(logKey, errorLogEntry);

                            results.add(item);
                            continue;
                        }
                    }

                    // 执行修复
                    boolean fixResult = checker.fix(clusterId, hostInfo, item);
                    if (fixResult) {
                        item.setStatus(CheckItem.Status.SUCCESS);
                        if (item.getMessage() == null || item.getMessage().isEmpty() ||
                                item.getMessage().equals("正在修复...")) {
                            item.setMessage("修复成功");
                        }

                        // 记录修复成功日志
                        com.datasophon.common.model.LogEntry successLogEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.INFO,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "修复项 " + item.getItemName() + " 成功完成",
                                com.datasophon.common.model.LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, successLogEntry);
                    } else {
                        item.setStatus(CheckItem.Status.FAILED);
                        if (item.getMessage() == null || item.getMessage().isEmpty() ||
                                item.getMessage().equals("正在修复...")) {
                            item.setMessage("修复失败");
                        }

                        // 记录修复失败日志
                        com.datasophon.common.model.LogEntry failLogEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.ERROR,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "修复项 " + item.getItemName() + " 失败: " + item.getMessage(),
                                com.datasophon.common.model.LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, failLogEntry);
                    }
                    results.add(item);

                    // 更新最后访问时间（如果使用共享连接）
                    if (!isPasswordFreeItem && session != null) {
                        connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    logger.error("执行修复项 {} 时发生异常: {}", item.getItemName(), e.getMessage(), e);
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("修复异常: " + e.getMessage());

                    // 记录异常日志到缓存日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry exceptionLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "执行修复项 " + item.getItemName() + " 时发生异常: " + e.getMessage(),
                            com.datasophon.common.model.LogEntry.Type.FIX);
                    LogEntryManager.addLogEntry(logKey, exceptionLogEntry);

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

                    // 记录失败日志到缓存日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry errorLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "批量执行修复时发生异常: " + e.getMessage(),
                            com.datasophon.common.model.LogEntry.Type.FIX);
                    LogEntryManager.addLogEntry(logKey, errorLogEntry);

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

    /**
     * 异步执行同步hosts文件任务
     *
     * @param taskId           任务ID
     * @param clusterId        集群ID
     * @param hostMap          主机信息映射
     * @param hostsFilePreview hosts文件预览信息
     */
    public void syncHostsFileTask(String taskId, Integer clusterId, Map<String, HostInfo> hostMap,
            Object hostsFilePreview) {
        // 参数校验
        if (taskId == null || clusterId == null || hostMap == null || hostsFilePreview == null) {
            logger.error("同步hosts文件任务参数异常: taskId={}, clusterId={}, hostMap={}, hostsFilePreview={}",
                    taskId, clusterId, hostMap != null ? "非空" : "空",
                    hostsFilePreview != null ? "非空" : "空");
            return;
        }

        logger.info("开始异步执行hosts文件同步任务，集群ID: {}, 任务ID: {}", clusterId, taskId);

        // 使用hardwareInfoExecutor执行（第二阶段）而不是hostsFileExecutor
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // 检查Spring上下文是否可用
                if (taskManager == null || taskManager.getApplicationContext() == null) {
                    logger.error("任务管理器或Spring上下文为空，无法获取HostCheckService");
                    return;
                }

                // 获取主机IP列表并排序
                List<String> ips = new ArrayList<>(hostMap.keySet());
                ips = com.datasophon.common.utils.HostUtils.sortIpAddresses(ips);

                // 获取hosts文件内容
                String hostsContent = ((com.datasophon.api.service.impl.HostCheckServiceImpl.HostsFilePreviewVO) hostsFilePreview)
                        .getHostsContent();

                // 从Spring容器获取HostCheckService
                com.datasophon.api.service.HostCheckService hostCheckService = taskManager.getApplicationContext()
                        .getBean(com.datasophon.api.service.HostCheckService.class);

                if (hostCheckService == null) {
                    logger.error("无法获取HostCheckService服务实例");
                    return;
                }

                // 批量并行处理主机（每批10个）
                final int batchSize = 10;
                for (int i = 0; i < ips.size(); i += batchSize) {
                    // 获取当前批次的主机IP
                    int endIndex = Math.min(i + batchSize, ips.size());
                    List<String> batchIps = ips.subList(i, endIndex);

                    logger.info("开始并行处理第{}批主机，数量: {}", (i / batchSize) + 1, batchIps.size());

                    // 创建当前批次的任务列表
                    List<CompletableFuture<Void>> batchTasks = new ArrayList<>();

                    // 为每个主机创建异步任务
                    for (String ip : batchIps) {
                        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                            try {
                                logger.info("正在同步hosts文件到主机: {}", ip);

                                // 调用主机检查服务更新hosts文件
                                com.datasophon.common.utils.Result updateResult = hostCheckService.updateHostsFile(
                                        clusterId,
                                        ip, hostsContent);

                                // 更新主机处理状态
                                try {
                                    com.datasophon.api.service.impl.TaskProgressHelper.updateHostProcessStatus(
                                            taskId,
                                            ip,
                                            updateResult.isSuccess(),
                                            updateResult.isSuccess() ? null : updateResult.getMsg());
                                } catch (Exception e) {
                                    logger.error("更新任务进度状态失败: {}", e.getMessage(), e);
                                }
                            } catch (Exception e) {
                                logger.error("同步hosts文件到主机{}时发生错误", ip, e);
                                // 更新主机处理状态为失败
                                try {
                                    com.datasophon.api.service.impl.TaskProgressHelper.updateHostProcessStatus(
                                            taskId,
                                            ip,
                                            false,
                                            e.getMessage());
                                } catch (Exception ex) {
                                    logger.error("更新任务进度状态失败: {}", ex.getMessage(), ex);
                                }
                            }
                        }, hardwareInfoExecutor);

                        batchTasks.add(task);
                    }

                    // 等待当前批次的所有任务完成
                    try {
                        CompletableFuture.allOf(batchTasks.toArray(new CompletableFuture[0])).get();
                        logger.info("第{}批主机处理完成", (i / batchSize) + 1);
                    } catch (Exception e) {
                        logger.error("等待批处理任务完成时发生错误", e);
                    }
                }

                // 完成任务
                try {
                    com.datasophon.api.service.impl.TaskProgressHelper.completeTask(
                            taskId,
                            "所有主机的hosts文件已成功同步",
                            "部分主机的hosts文件同步失败，请检查详情");
                } catch (Exception e) {
                    logger.error("完成任务状态更新失败: {}", e.getMessage(), e);
                }

                logger.info("hosts文件同步任务完成，集群ID: {}, 任务ID: {}", clusterId, taskId);

            } catch (Exception e) {
                logger.error("执行hosts文件同步任务时发生错误", e);
            }
        }, hardwareInfoExecutor); // 使用硬件信息执行器（第二阶段）

        // 注册任务
        try {
            taskManager.registerTask("sync_hosts_file", "同步hosts文件 - 集群ID: " + clusterId, future);
        } catch (Exception e) {
            logger.error("注册任务时发生错误: {}", e.getMessage(), e);
        }

        // 任务完成后保留一段时间进度信息，然后移除
        future.thenRun(() -> {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(30));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // 移除任务进度
                try {
                    com.datasophon.api.service.impl.TaskProgressHelper.removeTaskProgress(taskId);
                } catch (Exception e) {
                    logger.error("移除任务进度信息失败: {}", e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 异步执行批量设置主机名任务
     *
     * @param taskId          任务ID
     * @param clusterId       集群ID
     * @param hostMap         主机信息映射
     * @param hostnamePreview 主机名预览列表
     */
    public void batchSetHostnameTask(String taskId, Integer clusterId, Map<String, HostInfo> hostMap,
            List<Map<String, String>> hostnamePreview) {
        // 参数校验
        if (taskId == null || clusterId == null || hostMap == null || hostnamePreview == null) {
            logger.error("批量设置主机名任务参数异常: taskId={}, clusterId={}, hostMap={}, hostnamePreview={}",
                    taskId, clusterId, hostMap != null ? "非空" : "空",
                    hostnamePreview != null ? "非空" : "空");
            return;
        }

        logger.info("开始异步执行批量设置主机名任务，集群ID: {}, 任务ID: {}", clusterId, taskId);

        // 将任务注册到任务管理器并使用hostnameExecutor执行
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // 检查Spring上下文是否可用
                if (taskManager == null || taskManager.getApplicationContext() == null) {
                    logger.error("任务管理器或Spring上下文为空，无法获取HostCheckService");
                    return;
                }

                // 获取HostCheckService
                com.datasophon.api.service.HostCheckService hostCheckService = taskManager.getApplicationContext()
                        .getBean(com.datasophon.api.service.HostCheckService.class);

                if (hostCheckService == null) {
                    logger.error("无法获取HostCheckService服务实例");
                    return;
                }

                // 创建主机IP与预览信息的映射，便于查找
                Map<String, Map<String, String>> ipToPreviewMap = new HashMap<>();
                for (Map<String, String> hostItem : hostnamePreview) {
                    String ip = hostItem.get("ip");
                    if (ip != null && !ip.isEmpty()) {
                        ipToPreviewMap.put(ip, hostItem);
                    }
                }

                // 获取待处理主机的IP列表
                List<String> ips = new ArrayList<>(ipToPreviewMap.keySet());

                // 批量并行处理主机（每批10个）
                final int batchSize = 10;
                for (int i = 0; i < ips.size(); i += batchSize) {
                    // 获取当前批次的主机IP
                    int endIndex = Math.min(i + batchSize, ips.size());
                    List<String> batchIps = ips.subList(i, endIndex);

                    logger.info("开始并行处理第{}批主机名设置，数量: {}", (i / batchSize) + 1, batchIps.size());

                    // 创建当前批次的任务列表
                    List<CompletableFuture<Void>> batchTasks = new ArrayList<>();

                    // 为每个主机创建异步任务
                    for (String ip : batchIps) {
                        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                            try {
                                Map<String, String> hostItem = ipToPreviewMap.get(ip);
                                String newHostname = hostItem.get("newHostname");

                                if (newHostname == null || newHostname.isEmpty()) {
                                    logger.warn("主机信息不完整，跳过该主机: {}", hostItem);
                                    return;
                                }

                                // 获取主机信息
                                HostInfo hostInfo = hostMap.get(ip);
                                if (hostInfo == null) {
                                    throw new Exception("未找到主机信息");
                                }

                                logger.info("为主机 {} 设置新主机名：{}", ip, newHostname);

                                // 获取SSH连接信息
                                String username = hostInfo.getSshUser();
                                Integer port = hostInfo.getSshPort();

                                // 获取或创建SSH连接
                                ClientSession session = getOrCreateConnection(hostInfo);
                                if (session == null || !session.isOpen()) {
                                    throw new Exception("无法创建SSH连接");
                                }

                                // 先检查系统是否有sudo命令
                                com.datasophon.api.service.checker.common.CommandResult checkSudoResult = execCommand(
                                        session,
                                        "which sudo || echo 'nosudo'");
                                boolean hasSudo = !checkSudoResult.getOutput().trim().contains("nosudo");
                                String sudoPrefix = hasSudo ? "sudo " : "";

                                // 再检查系统是否有hostnamectl命令
                                com.datasophon.api.service.checker.common.CommandResult checkHostnamectlResult = execCommand(
                                        session,
                                        "which hostnamectl || echo 'nohostnamectl'");
                                boolean hasHostnamectl = !checkHostnamectlResult.getOutput().trim()
                                        .contains("nohostnamectl");

                                // 根据命令可用性决定使用哪种方式设置主机名
                                String command;
                                if (hasHostnamectl) {
                                    // 使用hostnamectl命令设置主机名
                                    logger.info("使用hostnamectl命令设置主机名: {}", newHostname);
                                    command = sudoPrefix + "hostnamectl set-hostname " + newHostname;
                                } else {
                                    // 使用hostname命令并直接修改/etc/hostname文件
                                    logger.info("使用hostname命令设置主机名: {}", newHostname);
                                    command = sudoPrefix + "hostname " + newHostname + " && " +
                                            sudoPrefix + "sh -c 'echo \"" + newHostname + "\" > /etc/hostname'";
                                }

                                // 执行命令
                                com.datasophon.api.service.checker.common.CommandResult result = execCommand(session,
                                        command);

                                if (!result.isSuccess()) {
                                    throw new Exception("设置主机名失败: " + result.getError());
                                }

                                // 验证主机名是否设置成功
                                com.datasophon.api.service.checker.common.CommandResult verifyResult = execCommand(
                                        session,
                                        "hostname");
                                String currentHostname = verifyResult.getOutput().trim();

                                boolean hostnameSetSuccess = false; // 标记主机名是否设置成功

                                if (!currentHostname.equals(newHostname)) {
                                    logger.warn("主机名未成功设置，期望: {}，实际: {}", newHostname, currentHostname);

                                    // 如果第一次设置失败，尝试使用更直接的方式
                                    logger.info("尝试使用直接方式设置主机名: {}", newHostname);
                                    String directCommand = hasSudo
                                            ? "sudo hostname " + newHostname + " && sudo sh -c 'echo \"" + newHostname
                                                    + "\" > /etc/hostname'"
                                            : "hostname " + newHostname + " && sh -c 'echo \"" + newHostname
                                                    + "\" > /etc/hostname'";

                                    com.datasophon.api.service.checker.common.CommandResult retryResult = execCommand(
                                            session, directCommand);

                                    // 再次验证
                                    verifyResult = execCommand(session, "hostname");
                                    currentHostname = verifyResult.getOutput().trim();

                                    if (!currentHostname.equals(newHostname)) {
                                        throw new Exception(
                                                "重试后设置主机名仍然失败，期望: " + newHostname + "，实际: " + currentHostname);
                                    } else {
                                        logger.info("重试设置主机名成功: {}", newHostname);
                                        hostnameSetSuccess = true; // 重试成功
                                    }
                                } else {
                                    logger.info("主机名成功设置为: {}", newHostname);
                                    hostnameSetSuccess = true; // 首次设置成功
                                }

                                // 只有当主机名设置成功时才更新缓存
                                if (hostnameSetSuccess) {
                                    // 更新缓存中的主机名
                                    hostInfo.setHostname(newHostname);
                                    hostCheckService.updateHostInfoCache(clusterId, hostInfo);
                                } else {
                                    logger.warn("主机 {} 设置主机名失败，不更新缓存", ip);
                                }

                                // 更新任务进度
                                try {
                                    com.datasophon.api.service.impl.TaskProgressHelper.updateHostProcessStatus(
                                            taskId, ip, true, null);
                                } catch (Exception e) {
                                    logger.error("更新任务进度状态失败: {}", e.getMessage(), e);
                                }
                            } catch (Exception e) {
                                logger.error("为主机 {} 设置主机名时出错", ip, e);
                                // 更新任务进度
                                try {
                                    com.datasophon.api.service.impl.TaskProgressHelper.updateHostProcessStatus(
                                            taskId, ip, false, e.getMessage());
                                } catch (Exception ex) {
                                    logger.error("更新任务进度状态失败: {}", ex.getMessage(), ex);
                                }
                            }
                        }, hostnameExecutor);

                        batchTasks.add(task);
                    }

                    // 等待当前批次的所有任务完成
                    try {
                        CompletableFuture.allOf(batchTasks.toArray(new CompletableFuture[0])).get();
                        logger.info("第{}批主机名设置完成", (i / batchSize) + 1);
                    } catch (Exception e) {
                        logger.error("等待批处理任务完成时发生错误", e);
                    }
                }

                // 完成任务
                try {
                    com.datasophon.api.service.impl.TaskProgressHelper.completeTask(
                            taskId,
                            "所有主机名设置成功",
                            "部分主机名设置失败，请检查详情");
                } catch (Exception e) {
                    logger.error("完成任务状态更新失败: {}", e.getMessage(), e);
                }

                // 更新主机信息缓存
                try {
                    hostCheckService.updateHostMapInCache(clusterId);
                } catch (Exception e) {
                    logger.error("更新主机信息缓存失败: {}", e.getMessage(), e);
                }

                logger.info("批量设置主机名任务完成，集群ID: {}, 任务ID: {}", clusterId, taskId);

            } catch (Exception e) {
                logger.error("执行批量设置主机名任务时发生错误", e);
            }
        }, hostnameExecutor); // 使用专用的hostnameExecutor

        // 注册任务
        try {
            taskManager.registerTask("set_hostname", "批量设置主机名 - 集群ID: " + clusterId, future);
        } catch (Exception e) {
            logger.error("注册任务时发生错误: {}", e.getMessage(), e);
        }

        // 任务完成后保留一段时间进度信息，然后移除
        future.thenRun(() -> {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(30));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // 移除任务进度
                try {
                    com.datasophon.api.service.impl.TaskProgressHelper.removeTaskProgress(taskId);
                } catch (Exception e) {
                    logger.error("移除任务进度信息失败: {}", e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 异步获取或创建SSH连接
     * 
     * @param hostInfo 主机信息
     * @return 包含SSH会话的CompletableFuture，如果创建失败则返回包含null的CompletableFuture
     */
    public CompletableFuture<ClientSession> getOrCreateConnectionAsync(HostInfo hostInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getOrCreateConnection(hostInfo);
            } catch (Exception e) {
                logger.error("异步创建SSH连接时发生异常: {}", e.getMessage(), e);
                return null;
            }
        }, checkExecutor);
    }

    /**
     * 执行检查项
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param checkItem 检查项信息
     * @return 检查结果
     */
    public CheckItem executeCheck(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        logger.info("开始执行检查项: {} for {}", checkItem.getItemName(), hostInfo.getIp());

        // 检查是否已经在使用会话，否则创建新会话
        ClientSession session = null;
        boolean shouldCloseSession = false;

        try {
            if (hostInfo.isUseExistingSession() && hostInfo.getExternalSession() != null) {
                // 使用外部提供的会话
                session = hostInfo.getExternalSession();
                logger.debug("使用外部提供的会话");
            } else {
                // 从连接池获取连接
                session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
                shouldCloseSession = false; // 不关闭从连接池获取的连接

                if (session == null) {
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("无法建立SSH连接");
                    return checkItem;
                }
            }

            // 如果还未拿到会话，报告连接失败
            if (session == null) {
                logger.error("获取会话失败");
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法建立SSH连接");
                return checkItem;
            }

            // 获取对应的检查器
            ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));

            if (checker == null) {
                logger.error("未找到对应的检查器: {}", checkItem.getItemCode());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("未找到对应的检查器: " + checkItem.getItemCode());
                return checkItem;
            }

            // 执行具体检查
            return checker.check(clusterId, hostInfo, checkItem);

        } catch (Exception e) {
            logger.error("执行检查项时出错: {}", e.getMessage(), e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("执行检查时发生错误: " + e.getMessage());
            return checkItem;
        } finally {
            // 如果是我们自己创建的会话，并且需要关闭，则关闭它
            // 连接池中的连接不关闭，而是放回池中
            if (shouldCloseSession && session != null && !hostInfo.isUseExistingSession()) {
                try {
                    session.close();
                } catch (Exception e) {
                    logger.warn("关闭SSH会话时出错", e);
                }
            }
        }
    }

    /**
     * 执行修复项
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param fixItem   修复项信息
     * @return 修复结果
     */
    public CheckItem executeFix(Integer clusterId, HostInfo hostInfo, CheckItem fixItem) {
        logger.info("开始执行修复项: {} for {}", fixItem.getItemName(), hostInfo.getIp());

        // 检查是否已经在使用会话，否则创建新会话
        ClientSession session = null;
        boolean shouldCloseSession = false;

        try {
            if (hostInfo.isUseExistingSession() && hostInfo.getExternalSession() != null) {
                // 使用外部提供的会话
                session = hostInfo.getExternalSession();
                logger.debug("使用外部提供的会话");
            } else {
                // 从连接池获取连接
                session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
                shouldCloseSession = false; // 不关闭从连接池获取的连接

                if (session == null) {
                    fixItem.setStatus(CheckItem.Status.FAILED);
                    fixItem.setMessage("无法建立SSH连接");
                    return fixItem;
                }
            }

            // 如果还未拿到会话，报告连接失败
            if (session == null) {
                logger.error("获取会话失败");
                fixItem.setStatus(CheckItem.Status.FAILED);
                fixItem.setMessage("无法建立SSH连接");
                return fixItem;
            }

            // 获取对应的检查器
            ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(fixItem.getItemCode()));

            if (checker == null) {
                logger.error("未找到对应的检查器: {}", fixItem.getItemCode());
                fixItem.setStatus(CheckItem.Status.FAILED);
                fixItem.setMessage("未找到对应的检查器: " + fixItem.getItemCode());
                return fixItem;
            }

            // 执行具体修复
            boolean fixResult = checker.fix(clusterId, hostInfo, fixItem);
            if (fixResult) {
                fixItem.setStatus(CheckItem.Status.SUCCESS);
                fixItem.setMessage("修复成功");
            } else {
                fixItem.setStatus(CheckItem.Status.FAILED);
                fixItem.setMessage("修复失败");
            }
            return fixItem;

        } catch (Exception e) {
            logger.error("执行修复项时出错: {}", e.getMessage(), e);
            fixItem.setStatus(CheckItem.Status.FAILED);
            fixItem.setMessage("执行修复时发生错误: " + e.getMessage());
            return fixItem;
        } finally {
            // 如果是我们自己创建的会话，并且需要关闭，则关闭它
            // 连接池中的连接不关闭，而是放回池中
            if (shouldCloseSession && session != null && !hostInfo.isUseExistingSession()) {
                try {
                    session.close();
                } catch (Exception e) {
                    logger.warn("关闭SSH会话时出错", e);
                }
            }
        }
    }
}