package com.datasophon.api.service.checker.queue;

import com.datasophon.api.service.checker.common.LogEntryManager;
import com.datasophon.api.service.checker.core.ItemChecker;
import com.datasophon.api.service.checker.core.ItemCheckerFactory;
import com.datasophon.api.service.impl.HostCheckServiceImpl;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.common.model.LogEntry;
import com.datasophon.common.model.QueueManagerStatus;
import com.datasophon.common.model.QueueTaskInfo;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.utils.HostUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class HostCheckQueueManager {
    private static final Logger logger = LoggerFactory.getLogger(HostCheckQueueManager.class);

    // 修改为优先队列，支持任务优先级
    private final BlockingQueue<CheckTask> checkQueue = new PriorityBlockingQueue<>(100);
    // 修复队列，独立于检查队列
    private final BlockingQueue<FixTask> fixQueue = new PriorityBlockingQueue<>(50);

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> runningFixTasks = new ConcurrentHashMap<>();

    // 使用额外的Set保存队列中任务的key，避免遍历整个队列
    private final Set<String> taskKeysInQueue = ConcurrentHashMap.newKeySet();
    private final Set<String> fixTaskKeysInQueue = ConcurrentHashMap.newKeySet();

    // 跟踪任务执行开始时间，用于超时监控
    private final Map<String, Long> taskStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> fixTaskStartTimes = new ConcurrentHashMap<>();

    // 任务超时时间（毫秒）
    private static final long TASK_TIMEOUT_MS = 30 * 60 * 1000; // 30分钟

    // 记录处理统计信息
    private final AtomicLong tasksProcessed = new AtomicLong(0);
    private final AtomicLong tasksSucceeded = new AtomicLong(0);
    private final AtomicLong tasksFailed = new AtomicLong(0);

    // 修复任务统计信息
    private final AtomicLong fixTasksProcessed = new AtomicLong(0);
    private final AtomicLong fixTasksSucceeded = new AtomicLong(0);
    private final AtomicLong fixTasksFailed = new AtomicLong(0);

    // 添加任务执行时间统计
    private final AtomicLong fixTasksTotalExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong fixTasksMaxExecutionTimeMs = new AtomicLong(0);
    // 检查任务执行时间统计
    private final AtomicLong tasksTotalExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong tasksMaxExecutionTimeMs = new AtomicLong(0);

    // 检查项线程池 - 专门用于执行单个检查项
    private final ExecutorService itemCheckExecutorService;

    private Thread queueProcessorThread;
    private Thread fixQueueProcessorThread;
    private long queueProcessorStartTime;
    private long fixQueueProcessorStartTime;

    // 定时任务标志
    private final AtomicBoolean scheduledTasksEnabled = new AtomicBoolean(true);

    // 定时任务调度器
    @Autowired(required = false)
    private TaskScheduler taskScheduler;

    // 定时任务的Future
    private ScheduledFuture<?> queueHealthMonitorTask;
    private ScheduledFuture<?> taskTimeoutMonitorTask;

    private long queueHealthMonitorIntervalMs = TimeUnit.SECONDS.toMillis(60); // 默认60秒
    private long taskTimeoutMonitorIntervalMs = TimeUnit.SECONDS.toMillis(60); // 默认60秒

    @Autowired
    @Qualifier("checkExecutor")
    private ExecutorService checkExecutorService;

    @Autowired
    @Qualifier("fixExecutor")
    private ExecutorService fixExecutorService;

    @Autowired
    private ItemCheckerFactory itemCheckerFactory;

    // 添加上次执行时间记录
    private volatile String lastQueueHealthMonitorTime = null;
    private volatile String lastTaskTimeoutMonitorTime = null;

    // 添加日期格式化器
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 系统启动时间
    private static long applicationStartTime;

    @Autowired
    private HostCheckServiceImpl hostCheckService;

    public HostCheckQueueManager() {
        // 创建检查项线程池 - 负责检查项级别的任务
        this.itemCheckExecutorService = new ThreadPoolExecutor(
                4, // 核心线程数 - 减少并行度
                8, // 最大线程数 - 减少并行度
                30L, // 空闲线程存活时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(100), // 有界工作队列
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("item-checker-" + counter.getAndIncrement());
                        t.setDaemon(false); // 改为非守护线程，避免任务执行中断
                        t.setPriority(Thread.NORM_PRIORITY);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 改为调用者运行策略，防止任务丢失
        );
    }

    @PostConstruct
    public void init() {
        logger.info("正在初始化主机检查队列管理器...");
        startQueueProcessor();
        startFixQueueProcessor();
        // 注释掉自动启动定时任务的代码
        // startScheduledTasks();
        // 将定时任务标志设置为已停用
        scheduledTasksEnabled.set(false);
        logger.info("主机检查队列管理器初始化完成，定时任务默认关闭");
    }

    /**
     * 启动队列处理线程
     */
    private void startQueueProcessor() {
        if (queueProcessorThread == null || !queueProcessorThread.isAlive()) {
            queueProcessorThread = new Thread(this::processQueueTasks);
            queueProcessorThread.setName("host-check-queue-processor");
            queueProcessorThread.start();
            queueProcessorStartTime = System.currentTimeMillis();
            logger.info("主机检查队列处理线程已启动");
        }
    }

    /**
     * 启动修复队列处理线程
     */
    private void startFixQueueProcessor() {
        if (fixQueueProcessorThread == null || !fixQueueProcessorThread.isAlive()) {
            fixQueueProcessorThread = new Thread(this::processFixQueueTasks);
            fixQueueProcessorThread.setName("host-fix-queue-processor");
            fixQueueProcessorThread.start();
            fixQueueProcessorStartTime = System.currentTimeMillis();
            logger.info("主机修复队列处理线程已启动");
        }
    }

    /**
     * 启动定时任务
     */
    public void startScheduledTasks() {
        if (taskScheduler == null) {
            logger.info("TaskScheduler未注入，创建自定义TaskScheduler");
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(2);
            scheduler.setThreadNamePrefix("host-check-scheduler-");
            scheduler.initialize();
            taskScheduler = scheduler;
        }

        // 启动队列健康监控任务（每60秒执行一次）
        if (queueHealthMonitorTask == null || queueHealthMonitorTask.isCancelled()) {
            queueHealthMonitorTask = taskScheduler.scheduleAtFixedRate(
                    this::monitorQueueHealth, 60000);
            logger.info("队列健康监控定时任务已启动，执行间隔: 60秒");
        }

        // 启动任务超时监控（每60秒执行一次）
        if (taskTimeoutMonitorTask == null || taskTimeoutMonitorTask.isCancelled()) {
            taskTimeoutMonitorTask = taskScheduler.scheduleAtFixedRate(
                    this::checkForTaskTimeouts, 60000);
            logger.info("任务超时监控定时任务已启动，执行间隔: 60秒");
        }

        // 设置定时任务标志为已启用
        scheduledTasksEnabled.set(true);
    }

    /**
     * 暂停/停止定时任务
     */
    public void stopScheduledTasks() {
        // 取消队列健康监控任务
        if (queueHealthMonitorTask != null && !queueHealthMonitorTask.isCancelled()) {
            queueHealthMonitorTask.cancel(false);
            logger.info("队列健康监控定时任务已停止");
        }

        // 取消任务超时监控任务
        if (taskTimeoutMonitorTask != null && !taskTimeoutMonitorTask.isCancelled()) {
            taskTimeoutMonitorTask.cancel(false);
            logger.info("任务超时监控定时任务已停止");
        }

        // 设置定时任务标志为已停用
        scheduledTasksEnabled.set(false);
    }

    /**
     * 完全关闭队列管理器
     * 注意: 这通常只在应用关闭时或确认不再需要时调用
     */
    @PreDestroy
    public void shutdown() {
        logger.info("正在关闭主机检查队列管理器...");
        // 停止定时任务
        stopScheduledTasks();

        // 停止队列处理
        isRunning.set(false);

        // 取消所有运行中的任务
        runningTasks.forEach((key, future) -> {
            logger.info("正在取消任务: {}", key);
            future.cancel(true);
        });

        // 清空队列
        checkQueue.clear();
        taskKeysInQueue.clear();

        // 关闭线程池
        itemCheckExecutorService.shutdownNow();
        try {
            if (!itemCheckExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("检查项线程池未能在指定时间内完全终止");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 中断队列处理线程
        if (queueProcessorThread != null && queueProcessorThread.isAlive()) {
            queueProcessorThread.interrupt();
            logger.info("队列处理线程已中断");
        }

        // 清空任务跟踪数据
        taskStartTimes.clear();
        logger.info("主机检查队列管理器已完全关闭");
    }

    /**
     * 暂停队列处理
     * 注意: 这不会取消已在运行的任务，但会停止处理新任务
     */
    public void pauseQueueProcessing() {
        isRunning.set(false);
        logger.info("队列处理器已暂停，将不再处理新任务");
    }

    /**
     * 恢复队列处理
     */
    public void resumeQueueProcessing() {
        if (!isRunning.get()) {
            isRunning.set(true);
            startQueueProcessor(); // 确保处理线程在运行
            logger.info("队列处理器已恢复运行");
        }
    }

    /**
     * 获取队列管理器状态
     */
    public Map<String, Object> getManagerStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("queueProcessingEnabled", isRunning.get());
        status.put("scheduledTasksEnabled", scheduledTasksEnabled.get());
        status.put("queueProcessorAlive", queueProcessorThread != null && queueProcessorThread.isAlive());
        status.put("queueHealthMonitorActive", queueHealthMonitorTask != null && !queueHealthMonitorTask.isCancelled());
        status.put("taskTimeoutMonitorActive", taskTimeoutMonitorTask != null && !taskTimeoutMonitorTask.isCancelled());

        // 添加其他状态信息
        status.putAll(getQueueStatus());

        return status;
    }

    /**
     * 添加主机检查任务
     */
    public void addCheckTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService) {
        ensureSystemRunning();
        String taskKey = getTaskKey(clusterId, hostInfo.getIp());

        // 检查任务是否已经在队列或正在运行
        if (taskKeysInQueue.contains(taskKey) || runningTasks.containsKey(taskKey)) {
            logger.info("任务已经在队列或正在运行中，不重复添加: {}", taskKey);
            return;
        }

        CheckTask task = new CheckTask(clusterId, hostInfo, hostCheckService);
        checkQueue.offer(task);
        taskKeysInQueue.add(taskKey);
        logger.info("已添加检查任务到队列: 主机={}, 任务键={}", hostInfo.getIp(), taskKey);
    }

    /**
     * 添加部分检查项的主机检查任务
     * 仅对指定的检查项进行检查，而不是主机上的所有检查项
     */
    public void addPartialCheckTask(Integer clusterId, HostInfo hostInfo, List<CheckItem> itemsToCheck,
            HostCheckServiceImpl hostCheckService) {
        ensureSystemRunning();
        String taskKey = getTaskKey(clusterId, hostInfo.getIp());

        // 检查任务是否已经在队列或正在运行
        if (taskKeysInQueue.contains(taskKey) || runningTasks.containsKey(taskKey)) {
            logger.info("任务已经在队列或正在运行中，将取消旧任务并添加新任务: {}", taskKey);
            // 取消任何正在运行的任务
            cancelTask(clusterId, hostInfo.getIp());
        }

        // 创建一个修改后的HostInfo对象，只包含需要检查的项目
        HostInfo partialHostInfo = new HostInfo();
        partialHostInfo.setIp(hostInfo.getIp());
        partialHostInfo.setSshPort(hostInfo.getSshPort());
        partialHostInfo.setSshUser(hostInfo.getSshUser());
        partialHostInfo.setSshPassword(hostInfo.getSshPassword());
        // 复制其他必要的属性，但不包括不存在的sshKeyPath
        partialHostInfo.setClusterId(hostInfo.getClusterId());
        partialHostInfo.setCheckItems(new ArrayList<>(itemsToCheck));

        // 使用自定义优先级添加任务，给重试任务更高的优先级
        CheckTask task = new CheckTask(clusterId, partialHostInfo, hostCheckService, 5); // 优先级5比默认的10高
        checkQueue.offer(task);
        taskKeysInQueue.add(taskKey);

        logger.info("已添加部分检查任务到队列: 主机={}, 检查项数量={}, 任务键={}",
                hostInfo.getIp(), itemsToCheck.size(), taskKey);
    }

    /**
     * 确保系统所有组件都处于运行状态
     * 如果有组件未运行，则强制启动
     */
    private void ensureSystemRunning() {
        logger.info("正在确保系统组件处于运行状态...");

        // 1. 确保系统运行标志为true
        if (!isRunning.get()) {
            logger.info("系统运行标志为false，正在启用...");
            isRunning.set(true);
        }

        // 2. 确保队列处理线程在运行
        if (queueProcessorThread == null || !queueProcessorThread.isAlive()) {
            logger.info("队列处理线程未运行，正在启动...");
            startQueueProcessor();
        }

        // 3. 确保线程池处于活跃状态
        if (itemCheckExecutorService.isShutdown() || itemCheckExecutorService.isTerminated()) {
            logger.warn("检查项线程池已关闭，正在重新创建...");
            // 重新创建检查项线程池
            ThreadPoolExecutor newItemExecutor = new ThreadPoolExecutor(
                    4, 8, 30L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new ThreadFactory() {
                        private final AtomicInteger counter = new AtomicInteger(1);

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r);
                            t.setName("item-checker-" + counter.getAndIncrement());
                            t.setDaemon(false);
                            t.setPriority(Thread.NORM_PRIORITY);
                            return t;
                        }
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy());
            // 使用反射设置新的线程池
            try {
                java.lang.reflect.Field field = this.getClass().getDeclaredField("itemCheckExecutorService");
                field.setAccessible(true);
                field.set(this, newItemExecutor);
            } catch (Exception e) {
                logger.error("重新创建检查项线程池失败", e);
            }
        }

        // 6. 确保定时任务处于启用状态
        if (!scheduledTasksEnabled.get()) {
            logger.info("定时任务未启用，正在启用...");
            scheduledTasksEnabled.set(true);
        }

        // 7. 确保定时任务在运行
        if (taskScheduler == null) {
            logger.info("TaskScheduler未初始化，正在创建...");
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(2);
            scheduler.setThreadNamePrefix("host-check-scheduler-");
            scheduler.initialize();
            taskScheduler = scheduler;
        }

        // 8. 启动所有定时任务
        startScheduledTasks();

        logger.info("系统组件状态检查完成，所有组件已确保运行");
    }

    /**
     * 添加带优先级的检查任务
     */
    public void addCheckTaskWithPriority(Integer clusterId, HostInfo hostInfo,
            HostCheckServiceImpl hostCheckService, int priority) {
        String taskKey = getTaskKey(clusterId, hostInfo.getIp());
        try {
            // 如果任务已在运行，则不添加
            if (runningTasks.containsKey(taskKey)) {
                logger.info("主机 {} 的检查任务正在运行中，跳过本次添加", hostInfo.getIp());
                return;
            }

            // 使用Set检查队列中是否已存在该任务
            if (taskKeysInQueue.contains(taskKey)) {
                logger.info("主机 {} 的检查任务已在队列中等待执行，跳过本次添加", hostInfo.getIp());
                return;
            }

            // 检查队列处理线程状态和其他初始化检查
            if (queueProcessorThread == null || !queueProcessorThread.isAlive()) {
                logger.warn("队列处理线程不存在或已停止，尝试重新初始化");
                startQueueProcessor();
            }

            // 确保isRunning标志为true
            if (!isRunning.get()) {
                logger.warn("队列管理器未运行，尝试重启");
                resumeQueueProcessing();
            }

            // 检查线程池状态
            if (itemCheckExecutorService.isShutdown() || itemCheckExecutorService.isTerminated()) {
                logger.error("执行线程池已关闭，任务无法执行");
                return;
            }

            // 创建带优先级的任务
            CheckTask newTask = new CheckTask(clusterId, hostInfo, hostCheckService, priority);
            boolean added = checkQueue.offer(newTask, 10, TimeUnit.SECONDS);

            if (added) {
                taskKeysInQueue.add(taskKey);
                logger.info("成功添加主机 {} 的检查任务到队列，优先级: {}, 新队列大小: {}",
                        hostInfo.getIp(), priority, checkQueue.size());
            } else {
                logger.error("添加主机 {} 的检查任务到队列失败，队列可能已满", hostInfo.getIp());
            }
        } catch (Exception e) {
            logger.error("添加检查任务时发生错误: {}, {}", hostInfo.getIp(), e.getMessage(), e);
        }
    }

    public void cancelItemTask(Integer clusterId, String hostname, Integer itemId) {
        logger.info("正在取消指定检查项任务: 集群ID={}, 主机名={}, 检查项ID={}",
                clusterId, hostname, itemId);
        // 方法不需要取消整个主机任务
        // 具体取消逻辑在HostCheckServiceImpl中通过Future处理
    }

    /**
     * 取消指定主机的所有检查任务
     */
    public void cancelTask(Integer clusterId, String hostname) {
        String taskKey = getTaskKey(clusterId, hostname);
        Future<?> future = runningTasks.get(taskKey);
        if (future != null) {
            logger.info("正在强制取消主机 {} 的检查任务", hostname);
            future.cancel(true);
            runningTasks.remove(taskKey);
            taskStartTimes.remove(taskKey);
        }

        // 同时从队列中移除待执行的任务
        checkQueue.removeIf(task -> {
            boolean shouldRemove = getTaskKey(task.getClusterId(), task.getHostInfo().getIp()).equals(taskKey);
            if (shouldRemove) {
                taskKeysInQueue.remove(taskKey);
            }
            return shouldRemove;
        });
    }

    /**
     * 取消所有检查任务
     * 当需要开始新一轮批量检查时调用此方法
     */
    public void cancelAllTasks() {
        logger.info("正在取消所有检查任务，当前运行任务数: {}, 队列中任务数: {}",
                runningTasks.size(), checkQueue.size());

        // 取消所有运行中的任务
        runningTasks.forEach((key, future) -> {
            logger.info("正在取消运行中的任务: {}", key);
            future.cancel(true);
        });

        // 清空运行任务映射
        runningTasks.clear();
        taskStartTimes.clear();

        // 清空队列
        int queueSize = checkQueue.size();
        checkQueue.clear();
        taskKeysInQueue.clear();

        logger.info("已成功取消所有检查任务，清理了 {} 个运行中任务和 {} 个队列中任务",
                runningTasks.size(), queueSize);
    }

    private String getTaskKey(Integer clusterId, String hostname) {
        return clusterId + ":" + hostname;
    }

    /**
     * 队列处理任务，循环从队列中取出任务并提交给线程池执行
     * 在单独的线程中运行
     */
    private void processQueueTasks() {
        logger.info("开始处理主机检查队列任务");
        int consecutiveErrorCount = 0;

        while (isRunning.get()) {
            try {
                // 定期打印当前状态日志以监控队列健康
                if (!checkQueue.isEmpty()) {
                    logger.info("当前队列状态: 队列中等待的任务数量={}, 正在执行的任务数量={}",
                            checkQueue.size(), runningTasks.size());

                    // 从队列中获取所有待处理的任务
                    List<CheckTask> taskList = new ArrayList<>();
                    checkQueue.drainTo(taskList);

                    if (!taskList.isEmpty()) {
                        // 使用HostUtils.sortIpAddresses方法对任务按IP地址排序
                        taskList.sort((task1, task2) -> {
                            // 创建一个只包含两个IP的列表
                            List<String> ips = new ArrayList<>(2);
                            ips.add(task1.getHostInfo().getIp());
                            ips.add(task2.getHostInfo().getIp());

                            // 使用HostUtils排序这两个IP
                            List<String> sortedIps = HostUtils.sortIpAddresses(ips);

                            // 如果第一个IP是task1的IP，则task1排在前面，否则task2排在前面
                            if (sortedIps.get(0).equals(task1.getHostInfo().getIp())) {
                                return -1;
                            } else {
                                return 1;
                            }
                        });

                        // 排序后的任务重新放回队列
                        for (CheckTask task : taskList) {
                            checkQueue.put(task);
                        }

                        logger.info("已完成队列任务排序，按IP排序后第一个任务: {}",
                                taskList.isEmpty() ? "无" : taskList.get(0).getHostInfo().getIp());
                    }
                }

                // 添加超时机制，避免无限期阻塞
                CheckTask task = checkQueue.poll(5, TimeUnit.SECONDS);

                // 如果队列为空，继续等待
                if (task == null) {
                    continue;
                }

                String hostname = task.getHostInfo().getIp();
                String taskKey = getTaskKey(task.getClusterId(), hostname);

                // 从任务跟踪集合中移除
                taskKeysInQueue.remove(taskKey);

                // 检查是否已经有相同的任务在执行
                if (runningTasks.containsKey(taskKey)) {
                    logger.warn("主机 {} 的检查任务已在运行中，将延迟处理", hostname);
                    // 将任务重新放回队列末尾，等待稍后处理
                    checkQueue.put(task);
                    taskKeysInQueue.add(taskKey);
                    Thread.sleep(1000); // 短暂等待避免立即重试
                    continue;
                }

                logger.info("正在提交主机 {} 的检查任务，当前运行中任务数: {}",
                        hostname, runningTasks.size());

                try {
                    // 直接创建任务对象，不使用匿名内部类，便于清晰地管理执行流程
                    HostCheckTask hostCheckTask = new HostCheckTask(
                            task.getClusterId(), task.getHostInfo(), task.getHostCheckService(), taskKey);

                    Future<?> future = itemCheckExecutorService.submit(hostCheckTask);

                    // 记录运行中的任务和开始时间
                    runningTasks.put(taskKey, future);
                    taskStartTimes.put(taskKey, System.currentTimeMillis());

                    // 重置错误计数
                    consecutiveErrorCount = 0;

                } catch (Exception e) {
                    logger.error("提交主机 {} 的检查任务时发生错误: {}", hostname, e.getMessage(), e);
                    // 出现异常时，尝试将任务重新加入队列
                    checkQueue.put(task);
                    taskKeysInQueue.add(taskKey);
                    Thread.sleep(2000); // 出错后等待一段时间再重试
                }

            } catch (InterruptedException e) {
                if (isRunning.get()) {
                    logger.error("队列处理被中断", e);
                }
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("队列处理过程中发生意外错误", e);
                consecutiveErrorCount++;

                // 如果连续出错次数过多，尝试重置处理过程
                if (consecutiveErrorCount > 5) {
                    logger.warn("连续出错次数过多，尝试短暂休息后继续处理");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    consecutiveErrorCount = 0;
                }
            }
        }
        logger.info("队列处理已停止");
    }

    /**
     * 健康监控 - 定期检查队列处理线程和运行任务的状态
     * 由任务调度器调用，不再使用@Scheduled注解
     */
    public void monitorQueueHealth() {
        try {
            // 记录本次执行时间
            lastQueueHealthMonitorTime = dateFormat.format(new Date());

            // 添加每次执行的日志记录
            logger.info("队列健康监控执行中，执行时间: {}", lastQueueHealthMonitorTime);

            // 如果定时任务已禁用，直接返回
            if (!scheduledTasksEnabled.get() || !isRunning.get()) {
                return;
            }

            // 检查队列处理线程是否存活
            if (queueProcessorThread == null || !queueProcessorThread.isAlive()) {
                logger.warn("队列处理线程已停止，尝试重新启动");
                startQueueProcessor();
            }

            // 检查修复队列处理线程是否存活
            if (fixQueueProcessorThread == null || !fixQueueProcessorThread.isAlive()) {
                logger.warn("修复队列处理线程已停止，尝试重新启动");
                startFixQueueProcessor();
            }

            // 记录处理线程运行时间
            long checkRunningTime = System.currentTimeMillis() - queueProcessorStartTime;
            long fixRunningTime = System.currentTimeMillis() - fixQueueProcessorStartTime;
            logger.info("检查队列处理线程已运行: {} 分钟, 修复队列处理线程已运行: {} 分钟",
                    checkRunningTime / 60000, fixRunningTime / 60000);

            // 记录队列和任务统计信息
            logger.info("检查队列状态: 等待任务={}, 运行任务={}, 总处理任务={}, 成功={}, 失败={}",
                    checkQueue.size(), runningTasks.size(),
                    tasksProcessed.get(), tasksSucceeded.get(), tasksFailed.get());

            logger.info("修复队列状态: 等待任务={}, 运行任务={}, 总处理任务={}, 成功={}, 失败={}",
                    fixQueue.size(), runningFixTasks.size(),
                    fixTasksProcessed.get(), fixTasksSucceeded.get(), fixTasksFailed.get());

            // 检查线程池状态
            ThreadPoolExecutor checkExecutor = (ThreadPoolExecutor) itemCheckExecutorService;

            logger.info("检查项线程池状态: 活跃线程={}, 完成任务={}, 队列任务={}",
                    checkExecutor.getActiveCount(), checkExecutor.getCompletedTaskCount(),
                    checkExecutor.getQueue().size());

        } catch (Exception e) {
            logger.error("监控队列健康状态时发生错误", e);
        }
    }

    /**
     * 检查任务是否超时
     * 由任务调度器调用，独立于monitorQueueHealth方法
     */
    public void checkForTaskTimeouts() {
        // 记录本次执行时间
        lastTaskTimeoutMonitorTime = dateFormat.format(new Date());

        // 添加每次执行的日志记录
        logger.info("任务超时监控执行中，执行时间: {}", lastTaskTimeoutMonitorTime);

        // 如果定时任务已禁用，直接返回
        if (!scheduledTasksEnabled.get() || !isRunning.get()) {
            return;
        }

        long now = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : taskStartTimes.entrySet()) {
            String taskKey = entry.getKey();
            long startTime = entry.getValue();
            long runningTime = now - startTime;

            // 任务运行时间超过阈值
            if (runningTime > TASK_TIMEOUT_MS) {
                logger.warn("任务 {} 执行时间过长: {} 分钟，考虑取消",
                        taskKey, runningTime / 60000);

                // 可以选择自动取消长时间运行的任务
                Future<?> future = runningTasks.get(taskKey);
                if (future != null && !future.isDone()) {
                    logger.warn("自动取消超时任务: {}", taskKey);
                    future.cancel(true);
                    runningTasks.remove(taskKey);
                    taskStartTimes.remove(taskKey);
                    tasksFailed.incrementAndGet();
                }
            }
        }

        // 同样检查修复任务超时
        for (Map.Entry<String, Long> entry : fixTaskStartTimes.entrySet()) {
            String taskKey = entry.getKey();
            long startTime = entry.getValue();
            long runningTime = now - startTime;

            // 修复任务运行时间超过阈值
            if (runningTime > TASK_TIMEOUT_MS) {
                logger.warn("修复任务 {} 执行时间过长: {} 分钟，考虑取消",
                        taskKey, runningTime / 60000);

                // 可以选择自动取消长时间运行的修复任务
                Future<?> future = runningFixTasks.get(taskKey);
                if (future != null && !future.isDone()) {
                    logger.warn("自动取消超时修复任务: {}", taskKey);
                    future.cancel(true);
                    runningFixTasks.remove(taskKey);
                    fixTaskStartTimes.remove(taskKey);
                    fixTasksFailed.incrementAndGet();
                }
            }
        }

        // 清理已完成但未正确移除的任务
        runningTasks.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone() || entry.getValue().isCancelled()) {
                String taskKey = entry.getKey();
                logger.info("清理已完成/已取消但未移除的任务: {}", taskKey);
                taskStartTimes.remove(taskKey);
                return true;
            }
            return false;
        });

        // 清理修复队列中已完成但未正确移除的任务
        runningFixTasks.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone() || entry.getValue().isCancelled()) {
                String taskKey = entry.getKey();
                logger.info("清理已完成/已取消但未移除的修复任务: {}", taskKey);
                fixTaskStartTimes.remove(taskKey);
                return true;
            }
            return false;
        });
    }

    /**
     * 获取队列健康状态信息
     */
    public Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("queueSize", checkQueue.size());
        status.put("runningTasks", runningTasks.size());
        status.put("processorThreadAlive", queueProcessorThread != null && queueProcessorThread.isAlive());
        status.put("tasksProcessed", tasksProcessed.get());
        status.put("tasksSucceeded", tasksSucceeded.get());
        status.put("tasksFailed", tasksFailed.get());

        // 计算总线程统计
        ThreadPoolExecutor checkExecutor = (ThreadPoolExecutor) itemCheckExecutorService;

        status.put("totalActiveThreads", checkExecutor.getActiveCount());
        status.put("totalPoolSize", checkExecutor.getPoolSize());
        status.put("totalCompletedTasks", checkExecutor.getCompletedTaskCount());
        status.put("totalQueuedTasks", checkExecutor.getQueue().size());

        // 添加队列名称列表
        List<String> queueNames = new ArrayList<>();
        queueNames.add("检查队列(checkQueue)");
        queueNames.add("修复队列(fixQueue)");
        queueNames.add("主机信息收集队列(osInfoExecutor)");
        queueNames.add("硬件信息收集队列(hardwareInfoExecutor)");
        queueNames.add("hosts文件设置队列(hostsFileExecutor)");
        queueNames.add("主机名设置队列(hostnameExecutor)");
        status.put("queueNames", queueNames);

        return status;
    }

    /**
     * 任务执行类，用于在线程池中执行主机检查任务
     */
    private class HostCheckTask implements Runnable {
        private final Integer clusterId;
        private final HostInfo hostInfo;
        private final HostCheckServiceImpl hostCheckService;
        private final String taskKey;

        public HostCheckTask(Integer clusterId, HostInfo hostInfo,
                HostCheckServiceImpl hostCheckService, String taskKey) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.hostCheckService = hostCheckService;
            this.taskKey = taskKey;
        }

        @Override
        public void run() {
            // 保存原始线程名
            Thread currentThread = Thread.currentThread();
            String originalThreadName = currentThread.getName();

            // 设置新的线程名
            currentThread.setName("host-check-" + hostInfo.getIp());
            logger.info("开始执行主机 {} 的检查任务", hostInfo.getIp());

            long startTime = System.currentTimeMillis();
            tasksProcessed.incrementAndGet();

            try {
                hostCheckService.processHostCheck(clusterId, hostInfo);
                logger.info("主机 {} 的检查任务执行完成", hostInfo.getIp());
                tasksSucceeded.incrementAndGet();
            } catch (Exception e) {
                logger.error("执行主机 {} 的检查任务时发生错误: {}",
                        hostInfo.getIp(), e.getMessage(), e);
                tasksFailed.incrementAndGet();
            } finally {
                // 统计执行时间
                long executionTime = System.currentTimeMillis() - startTime;
                // 更新总执行时间
                tasksTotalExecutionTimeMs.addAndGet(executionTime);

                // 更新最大执行时间
                long currentMax = tasksMaxExecutionTimeMs.get();
                while (executionTime > currentMax) {
                    if (tasksMaxExecutionTimeMs.compareAndSet(currentMax, executionTime)) {
                        break;
                    }
                    currentMax = tasksMaxExecutionTimeMs.get();
                }

                // 移除任务
                runningTasks.remove(taskKey);
                // 确保从taskStartTimes中删除任务记录，防止任务完成后仍被当作超时任务
                taskStartTimes.remove(taskKey);

                // 恢复原始线程名
                currentThread.setName(originalThreadName);
            }
        }
    }

    private static class FixTask implements Comparable<FixTask> {
        private final Integer clusterId;
        private final HostInfo hostInfo;
        private final CheckItem checkItem;
        private final HostCheckServiceImpl hostCheckService;
        private final int priority;

        public FixTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
            this(clusterId, hostInfo, checkItem, null, 5);
        }

        public FixTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem,
                HostCheckServiceImpl hostCheckService) {
            this(clusterId, hostInfo, checkItem, hostCheckService, 5);
        }

        public FixTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem, HostCheckServiceImpl hostCheckService,
                int priority) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.checkItem = checkItem;
            this.hostCheckService = hostCheckService;
            this.priority = priority;
        }

        public Integer getClusterId() {
            return clusterId;
        }

        public HostInfo getHostInfo() {
            return hostInfo;
        }

        public CheckItem getCheckItem() {
            return checkItem;
        }

        public HostCheckServiceImpl getHostCheckService() {
            return hostCheckService;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(FixTask other) {
            return Integer.compare(this.priority, other.priority);
        }
    }

    private static class CheckTask implements Comparable<CheckTask> {
        private final Integer clusterId;
        private final HostInfo hostInfo;
        private final HostCheckServiceImpl hostCheckService;
        private final int priority;

        public CheckTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService) {
            this(clusterId, hostInfo, hostCheckService, 5);
        }

        public CheckTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService, int priority) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.hostCheckService = hostCheckService;
            this.priority = priority;
        }

        public Integer getClusterId() {
            return clusterId;
        }

        public HostInfo getHostInfo() {
            return hostInfo;
        }

        public HostCheckServiceImpl getHostCheckService() {
            return hostCheckService;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(CheckTask other) {
            return Integer.compare(this.priority, other.priority);
        }
    }

    /**
     * 获取队列管理器状态（返回实体类）
     * 
     * @return QueueManagerStatus对象
     */
    public QueueManagerStatus getQueueManagerStatus() {
        QueueManagerStatus status = new QueueManagerStatus();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 基本状态
        status.setRunning(isRunning.get());
        status.setScheduledTasksEnabled(scheduledTasksEnabled.get());
        status.setQueueEmpty(checkQueue.isEmpty());
        status.setFixQueueEmpty(fixQueue.isEmpty());

        // 设置系统运行时间
        if (applicationStartTime > 0) {
            long uptimeMs = System.currentTimeMillis() - applicationStartTime;
            status.setSystemStartTime(dateFormat.format(new Date(applicationStartTime)));
            status.setSystemUptimeMs(uptimeMs);
            status.setSystemUptime(formatDuration(uptimeMs));
        }

        // 队列详情
        status.setQueueSize(checkQueue.size());
        status.setFixQueueSize(fixQueue.size());
        status.setRunningTasksCount(runningTasks.size());
        status.setRunningFixTasksCount(runningFixTasks.size());

        // 添加队列名称列表
        List<String> queueNames = new ArrayList<>();
        queueNames.add("检查队列(checkQueue)");
        queueNames.add("修复队列(fixQueue)");
        queueNames.add("主机信息收集队列(osInfoExecutor)");
        queueNames.add("硬件信息收集队列(hardwareInfoExecutor)");
        queueNames.add("hosts文件设置队列(hostsFileExecutor)");
        queueNames.add("主机名设置队列(hostnameExecutor)");
        status.setQueueNames(queueNames);

        // 线程池信息
        ThreadPoolExecutor mainExecutor = (ThreadPoolExecutor) checkExecutorService;
        ThreadPoolExecutor itemExecutor = (ThreadPoolExecutor) itemCheckExecutorService;

        // 修复线程池可能为null，进行判断
        int fixActiveCount = 0;
        int fixQueueSize = 0;
        int fixPoolSize = 0;
        long fixCompletedTasks = 0;

        // 获取修复线程池状态
        if (fixExecutorService != null && fixExecutorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor fixExecutor = (ThreadPoolExecutor) fixExecutorService;
            fixActiveCount = fixExecutor.getActiveCount();
            fixQueueSize = fixExecutor.getQueue().size();
            fixPoolSize = fixExecutor.getPoolSize();
            fixCompletedTasks = fixExecutor.getCompletedTaskCount();
        }

        status.setMainExecutorActiveCount(mainExecutor.getActiveCount());
        status.setMainExecutorQueueSize(mainExecutor.getQueue().size());
        status.setItemExecutorActiveCount(itemExecutor.getActiveCount());
        status.setItemExecutorQueueSize(itemExecutor.getQueue().size());
        status.setFixExecutorActiveCount(fixActiveCount);
        status.setFixExecutorQueueSize(fixQueueSize);

        // 计算线程池总统计数据
        int totalActiveThreads = mainExecutor.getActiveCount() +
                itemExecutor.getActiveCount() +
                fixActiveCount;
        int totalPoolSize = mainExecutor.getPoolSize() +
                itemExecutor.getPoolSize() +
                fixPoolSize;
        long totalCompletedTasks = mainExecutor.getCompletedTaskCount() +
                itemExecutor.getCompletedTaskCount() +
                fixCompletedTasks;
        int totalQueuedTasks = mainExecutor.getQueue().size() +
                itemExecutor.getQueue().size() +
                fixQueueSize;

        // 设置线程池总统计
        status.setTotalActiveThreads(totalActiveThreads);
        status.setTotalPoolSize(totalPoolSize);
        status.setTotalCompletedTasks(totalCompletedTasks);
        status.setTotalQueuedTasks(totalQueuedTasks);

        // 统计信息
        status.setTasksProcessed(tasksProcessed.get());
        status.setTasksSucceeded(tasksSucceeded.get());
        status.setTasksFailed(tasksFailed.get());
        status.setFixTasksProcessed(fixTasksProcessed.get());
        status.setFixTasksSucceeded(fixTasksSucceeded.get());
        status.setFixTasksFailed(fixTasksFailed.get());

        // 计算平均执行时间
        long fixAvgTimeMs = 0;
        if (fixTasksProcessed.get() > 0) {
            fixAvgTimeMs = fixTasksTotalExecutionTimeMs.get() / fixTasksProcessed.get();
        }
        status.setFixTasksAvgExecutionTimeMs(fixAvgTimeMs);
        status.setFixTasksMaxExecutionTimeMs(fixTasksMaxExecutionTimeMs.get());

        // 格式化执行时间
        status.setFixTasksAvgExecutionTime(formatDuration(fixAvgTimeMs));
        status.setFixTasksMaxExecutionTime(formatDuration(fixTasksMaxExecutionTimeMs.get()));

        // 计算检查任务平均执行时间
        long tasksAvgTimeMs = 0;
        if (tasksProcessed.get() > 0) {
            tasksAvgTimeMs = tasksTotalExecutionTimeMs.get() / tasksProcessed.get();
        }
        status.setTasksAvgExecutionTimeMs(tasksAvgTimeMs);
        status.setTasksMaxExecutionTimeMs(tasksMaxExecutionTimeMs.get());

        // 格式化执行时间
        status.setTasksAvgExecutionTime(formatDuration(tasksAvgTimeMs));
        status.setTasksMaxExecutionTime(formatDuration(tasksMaxExecutionTimeMs.get()));

        // 监控任务状态
        status.setQueueHealthMonitorActive(queueHealthMonitorTask != null && !queueHealthMonitorTask.isCancelled());
        status.setTaskTimeoutMonitorActive(taskTimeoutMonitorTask != null && !taskTimeoutMonitorTask.isCancelled());

        // 监控任务间隔
        status.setQueueHealthMonitorIntervalMs(queueHealthMonitorIntervalMs);
        status.setTaskTimeoutMonitorIntervalMs(taskTimeoutMonitorIntervalMs);

        // 可读的监控任务间隔
        status.setQueueHealthMonitorInterval(formatTimeInterval(queueHealthMonitorIntervalMs));
        status.setTaskTimeoutMonitorInterval(formatTimeInterval(taskTimeoutMonitorIntervalMs));

        // 添加上次监控执行时间
        status.setLastQueueHealthMonitorTime(lastQueueHealthMonitorTime != null ? lastQueueHealthMonitorTime : null);
        status.setLastTaskTimeoutMonitorTime(lastTaskTimeoutMonitorTime != null ? lastTaskTimeoutMonitorTime : null);

        // 设置处理线程状态
        status.setQueueProcessorThreadAlive(queueProcessorThread != null && queueProcessorThread.isAlive());
        status.setFixQueueProcessorThreadAlive(fixQueueProcessorThread != null && fixQueueProcessorThread.isAlive());

        // 设置处理器启动时间
        if (queueProcessorStartTime > 0) {
            status.setQueueProcessorStartTime(dateFormat.format(new Date(queueProcessorStartTime)));
        }
        if (fixQueueProcessorStartTime > 0) {
            status.setFixQueueProcessorStartTime(dateFormat.format(new Date(fixQueueProcessorStartTime)));
        }

        return status;
    }

    /**
     * 仅停止队列健康监控任务
     */
    public void stopQueueHealthMonitor() {
        if (queueHealthMonitorTask != null && !queueHealthMonitorTask.isCancelled()) {
            queueHealthMonitorTask.cancel(false);
            logger.info("队列健康监控定时任务已停止");
        }
    }

    /**
     * 仅停止任务超时监控任务
     */
    public void stopTaskTimeoutMonitor() {
        if (taskTimeoutMonitorTask != null && !taskTimeoutMonitorTask.isCancelled()) {
            taskTimeoutMonitorTask.cancel(false);
            logger.info("任务超时监控定时任务已停止");
        }
    }

    /**
     * 仅启动队列健康监控任务
     */
    public void startQueueHealthMonitor() {
        if (taskScheduler == null) {
            logger.info("TaskScheduler未注入，无法启动队列健康监控");
            return;
        }

        // 启动队列健康监控任务（每2分钟执行一次）
        if (queueHealthMonitorTask == null || queueHealthMonitorTask.isCancelled()) {
            queueHealthMonitorTask = taskScheduler.scheduleAtFixedRate(
                    this::monitorQueueHealth, 120000);
            logger.info("队列健康监控定时任务已启动，执行间隔: 2分钟");
        }
    }

    /**
     * 仅启动任务超时监控任务
     */
    public void startTaskTimeoutMonitor() {
        if (taskScheduler == null) {
            logger.info("TaskScheduler未注入，无法启动任务超时监控");
            return;
        }

        // 启动任务超时监控（每30秒执行一次）
        if (taskTimeoutMonitorTask == null || taskTimeoutMonitorTask.isCancelled()) {
            taskTimeoutMonitorTask = taskScheduler.scheduleAtFixedRate(
                    this::checkForTaskTimeouts, 30000);
            logger.info("任务超时监控定时任务已启动，执行间隔: 30秒");
        }
    }

    /**
     * 获取检查任务队列详情
     * 
     * @return 任务队列中的详细任务信息列表
     */
    public List<QueueTaskInfo> getQueueTasksDetails() {
        List<QueueTaskInfo> taskDetails = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 添加运行中的任务
        for (Map.Entry<String, Future<?>> entry : runningTasks.entrySet()) {
            String taskKey = entry.getKey();
            Future<?> future = entry.getValue();
            Long startTime = taskStartTimes.get(taskKey);

            if (startTime != null) {
                QueueTaskInfo taskInfo = new QueueTaskInfo();
                String[] parts = taskKey.split(":");
                taskInfo.setClusterId(Integer.parseInt(parts[0]));
                taskInfo.setHostname(parts[1]);
                if (parts.length >= 3) {
                    taskInfo.setItemId(Integer.parseInt(parts[2]));
                }

                taskInfo.setTaskId(taskKey);
                taskInfo.setStatus("运行中");
                taskInfo.setStartTime(dateFormat.format(new Date(startTime)));
                taskInfo.setDuration(System.currentTimeMillis() - startTime);
                taskInfo.setFixTask(false);

                if (future instanceof CompletableFuture) {
                    CompletableFuture<?> cf = (CompletableFuture<?>) future;
                    taskInfo.setExecutorName("checkExecutor");
                    taskInfo.setThreadName(Thread.currentThread().getName());
                }

                taskDetails.add(taskInfo);
            }
        }

        // 添加等待中的任务
        for (CheckTask task : checkQueue) {
            QueueTaskInfo taskInfo = new QueueTaskInfo();
            taskInfo.setClusterId(task.getClusterId());
            taskInfo.setHostname(task.getHostInfo().getIp());
            taskInfo.setTaskId("waiting_" + task.getClusterId() + ":" + task.getHostInfo().getIp());
            taskInfo.setStatus("等待中");
            taskInfo.setPriority(task.getPriority());
            taskInfo.setFixTask(false);
            taskDetails.add(taskInfo);
        }

        return taskDetails;
    }

    private String formatTimeInterval(long intervalMs) {
        long minutes = intervalMs / 60000;
        long seconds = (intervalMs % 60000) / 1000;
        return String.format("%d分钟 %d秒", minutes, seconds);
    }

    /**
     * 为修复任务添加处理主机修复的方法
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     */
    public void processHostFix(Integer clusterId, HostInfo hostInfo) {
        logger.info("开始为主机 {} 执行修复任务", hostInfo.getIp());

        // 修复状态为FAILED的所有检查项
        List<CheckItem> checkItems = hostInfo.getCheckItems();
        if (checkItems == null || checkItems.isEmpty()) {
            logger.warn("主机 {} 没有需要修复的检查项", hostInfo.getIp());
            return;
        }

        int fixedCount = 0;
        int failedCount = 0;

        for (CheckItem checkItem : checkItems) {
            if (checkItem.getStatus() == CheckItem.Status.FAILED) {
                logger.info("正在修复检查项 {}: {}", checkItem.getId(), checkItem.getItemName());
                try {
                    // 通过直接的方式执行修复
                    boolean success = fixCheckItem(clusterId, hostInfo, checkItem);
                    if (success) {
                        checkItem.setStatus(CheckItem.Status.SUCCESS);
                        checkItem.setMessage("修复成功");
                        fixedCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    logger.error("修复检查项 {} 时发生错误: {}", checkItem.getId(), e.getMessage(), e);
                    failedCount++;
                }
            }
        }

        logger.info("主机 {} 的修复任务完成，成功修复 {} 项，失败 {} 项",
                hostInfo.getIp(), fixedCount, failedCount);
    }

    /**
     * 修复单个检查项
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @return 是否修复成功
     */
    private boolean fixCheckItem(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 执行检查项的修复逻辑
            logger.info("执行检查项 {} 的修复逻辑", checkItem.getId());

            // 通过ItemCheckerFactory获取checker并调用fix方法
            ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(checkItem.getItemCode()));
            if (checker == null) {
                logger.error("找不到检查项 {} 对应的检查器", checkItem.getId());
                return false;
            }

            return checker.fix(clusterId, hostInfo, checkItem);
        } catch (Exception e) {
            logger.error("修复检查项 {} 时发生异常: {}", checkItem.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新队列健康监控定时任务执行间隔
     * 
     * @param intervalMs 执行间隔（毫秒）
     */
    public void updateQueueHealthMonitorInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("队列健康监控间隔不能小于1秒，忽略此次更新");
            return;
        }

        this.queueHealthMonitorIntervalMs = intervalMs;

        if (queueHealthMonitorTask != null && !queueHealthMonitorTask.isCancelled()) {
            queueHealthMonitorTask.cancel(false);
            queueHealthMonitorTask = taskScheduler.scheduleAtFixedRate(
                    this::monitorQueueHealth, intervalMs);
            logger.info("队列健康监控已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
    }

    /**
     * 更新任务超时监控定时任务执行间隔
     * 
     * @param intervalMs 执行间隔（毫秒）
     */
    public void updateTaskTimeoutMonitorInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("任务超时监控间隔不能小于1秒，忽略此次更新");
            return;
        }

        this.taskTimeoutMonitorIntervalMs = intervalMs;

        if (taskTimeoutMonitorTask != null && !taskTimeoutMonitorTask.isCancelled()) {
            taskTimeoutMonitorTask.cancel(false);
            taskTimeoutMonitorTask = taskScheduler.scheduleAtFixedRate(
                    this::checkForTaskTimeouts, intervalMs);
            logger.info("任务超时监控已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
    }

    /**
     * 添加修复任务到队列
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @return 任务ID或null（如果任务已在队列中）
     */
    public String addFixTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 生成任务标识和追踪键
            String taskId = "FIX_" + clusterId + "_" + hostInfo.getIp() + "_" + checkItem.getId();
            String taskKey = getFixTaskKey(clusterId, hostInfo.getIp(), checkItem.getId());

            // 检查是否已经在队列中
            if (fixTaskKeysInQueue.contains(taskKey)) {
                logger.info("主机 {} 的修复任务已在队列中, 项目: {}",
                        hostInfo.getIp(), checkItem.getItemName());
                return null;
            }

            // 检查是否正在运行
            if (runningFixTasks.containsKey(taskKey)) {
                logger.info("主机 {} 的修复任务正在执行中, 项目: {}",
                        hostInfo.getIp(), checkItem.getItemName());
                return null;
            }

            // 创建并添加任务到队列
            FixTask fixTask = new FixTask(clusterId, hostInfo, checkItem);
            fixQueue.put(fixTask);
            fixTaskKeysInQueue.add(taskKey);

            logger.info("成功添加修复任务到队列: {}, 队列大小: {}",
                    taskId, fixQueue.size());

            return taskId;
        } catch (Exception e) {
            logger.error("添加修复任务到队列时发生错误: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 处理修复队列任务
     */
    private void processFixQueueTasks() {
        logger.info("开始处理主机修复队列任务");
        int consecutiveErrorCount = 0;

        while (isRunning.get()) {
            try {
                // 定期打印当前状态日志以监控队列健康
                if (!fixQueue.isEmpty()) {
                    logger.info("当前修复队列状态: 队列中等待的任务数量={}, 正在执行的任务数量={}",
                            fixQueue.size(), runningFixTasks.size());
                }

                // 添加超时机制，避免无限期阻塞
                FixTask task = fixQueue.poll(5, TimeUnit.SECONDS);

                // 如果队列为空，继续等待
                if (task == null) {
                    continue;
                }

                String hostname = task.getHostInfo().getIp();
                String taskKey = getFixTaskKey(task.getClusterId(), hostname, task.getCheckItem().getId());

                // 从任务跟踪集合中移除
                fixTaskKeysInQueue.remove(taskKey);

                // 检查是否已经有相同的任务在执行
                if (runningFixTasks.containsKey(taskKey)) {
                    logger.warn("主机 {} 的修复任务已在运行中，将延迟处理", hostname);
                    // 将任务重新放回队列末尾，等待稍后处理
                    fixQueue.put(task);
                    fixTaskKeysInQueue.add(taskKey);
                    Thread.sleep(1000); // 短暂等待避免立即重试
                    continue;
                }

                logger.info("正在提交主机 {} 的修复任务，当前运行中任务数: {}",
                        hostname, runningFixTasks.size());

                try {
                    // 创建修复任务
                    HostFixTask hostFixTask = new HostFixTask(
                            task.getClusterId(), task.getHostInfo(), task.getCheckItem(), taskKey, hostCheckService);

                    // 使用修复专用线程池执行
                    Future<?> future = fixExecutorService.submit(hostFixTask);

                    // 记录运行中的任务和开始时间
                    runningFixTasks.put(taskKey, future);
                    fixTaskStartTimes.put(taskKey, System.currentTimeMillis());

                    // 重置错误计数
                    consecutiveErrorCount = 0;

                } catch (Exception e) {
                    logger.error("提交主机 {} 的修复任务时发生错误: {}", hostname, e.getMessage(), e);
                    // 出现异常时，尝试将任务重新加入队列
                    fixQueue.put(task);
                    fixTaskKeysInQueue.add(taskKey);
                    Thread.sleep(2000); // 出错后等待一段时间再重试
                }

            } catch (InterruptedException e) {
                if (isRunning.get()) {
                    logger.error("修复队列处理被中断", e);
                }
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("修复队列处理过程中发生意外错误", e);
                consecutiveErrorCount++;

                // 如果连续出错次数过多，尝试重置处理过程
                if (consecutiveErrorCount > 5) {
                    logger.warn("连续出错次数过多，尝试短暂休息后继续处理");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break; // 如果线程被中断，退出重试
                    }
                    consecutiveErrorCount = 0;
                }
            }
        }
        logger.info("修复队列处理已停止");
    }

    /**
     * 获取修复任务队列中的任务详情
     */
    public List<QueueTaskInfo> getFixQueueTasksDetails() {
        List<QueueTaskInfo> taskDetails = new ArrayList<>();

        // 添加运行中的任务
        for (Map.Entry<String, Future<?>> entry : runningFixTasks.entrySet()) {
            String taskKey = entry.getKey();
            Future<?> future = entry.getValue();

            if (!future.isDone() && !future.isCancelled()) {
                String[] parts = taskKey.split(":");
                if (parts.length >= 2) {
                    QueueTaskInfo taskInfo = new QueueTaskInfo();
                    taskInfo.setClusterId(Integer.parseInt(parts[0]));
                    taskInfo.setHostname(parts[1]);
                    taskInfo.setTaskId("running_" + taskKey);
                    taskInfo.setStatus("执行中");
                    taskInfo.setFixTask(true);

                    // 计算运行时间
                    Long startTime = fixTaskStartTimes.get(taskKey);
                    if (startTime != null) {
                        long runningTime = System.currentTimeMillis() - startTime;
                        taskInfo.setStatus("执行中 - " + formatTimeInterval(runningTime));
                    }

                    taskDetails.add(taskInfo);
                }
            }
        }

        // 添加等待中的任务
        for (FixTask task : fixQueue) {
            QueueTaskInfo taskInfo = new QueueTaskInfo();
            taskInfo.setClusterId(task.getClusterId());
            taskInfo.setHostname(task.getHostInfo().getIp());
            taskInfo.setTaskId("waiting_fix_" + task.getClusterId() + ":" + task.getHostInfo().getIp());
            taskInfo.setStatus("等待中");
            taskInfo.setFixTask(true);
            taskDetails.add(taskInfo);
        }

        return taskDetails;
    }

    /**
     * 获取修复任务的唯一标识
     */
    private String getFixTaskKey(Integer clusterId, String hostname, Integer itemId) {
        return clusterId + ":" + hostname + ":" + itemId;
    }

    /**
     * 修复任务执行类
     */
    private class HostFixTask implements Runnable {
        private final Integer clusterId;
        private final HostInfo hostInfo;
        private final CheckItem checkItem;
        private final String taskKey;
        private final HostCheckServiceImpl hostCheckService;
        private final ItemCheckerFactory itemCheckerFactory;

        public HostFixTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem, String taskKey,
                HostCheckServiceImpl hostCheckService) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.checkItem = checkItem;
            this.taskKey = taskKey;
            this.hostCheckService = hostCheckService;
            this.itemCheckerFactory = HostCheckQueueManager.this.itemCheckerFactory;
        }

        @Override
        public void run() {
            String originalThreadName = Thread.currentThread().getName();
            Thread.currentThread().setName("host-fix-" + hostInfo.getIp() + "-" + checkItem.getId());

            long startTime = System.currentTimeMillis();
            String logKey = "FIX_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + checkItem.getId();

            try {
                // 记录任务开始日志
                String startMessage = String.format("开始执行主机 %s 的修复任务: %s (ID: %d)",
                        hostInfo.getIp(), checkItem.getItemName(), checkItem.getId());
                LogEntry startLogEntry = createLogEntry(LogEntry.Level.INFO, startMessage, LogEntry.Type.FIX);
                LogEntryManager.addLogEntry(logKey, startLogEntry);

                logger.info("开始执行主机 {} 的修复任务: {}", hostInfo.getIp(), checkItem.getItemName());

                // 标记修复任务为进行中
                checkItem.setStatus(CheckItem.Status.FIXING);
                checkItem.setMessage("正在执行修复...");

                // 更新主机状态并立即更新缓存
                hostInfo.calculateStatus();
                HostCheckQueueManager.this.updateHostInfoCache(clusterId, hostInfo);

                // 统计信息
                fixTasksProcessed.incrementAndGet();

                // 使修复状态至少持续2秒，确保用户能看到"修复中"的状态
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    logger.warn("修复等待时间被中断", e);
                    Thread.currentThread().interrupt();
                }

                // 检查是否是特殊的免密检查项
                boolean isPasswordFreeItem = "PASSWORD_FREE".equals(checkItem.getItemCode());

                if (isPasswordFreeItem) {
                    // 免密检查项特殊处理
                    LogEntry logEntry = createLogEntry(LogEntry.Level.INFO,
                            "检测到免密登录修复项，将直接调用checker而非使用连接池", LogEntry.Type.FIX);
                    LogEntryManager.addLogEntry(logKey, logEntry);

                    logger.info("检测到免密登录修复项，将直接调用checker而非使用连接池");

                    try {
                        // 获取免密登录检查器
                        ItemChecker passwordFreeChecker = itemCheckerFactory.getChecker(ItemCode.PASSWORD_FREE);
                        if (passwordFreeChecker == null) {
                            String errorMsg = "未找到免密登录检查器";
                            LogEntry errorLogEntry = createLogEntry(LogEntry.Level.ERROR, errorMsg,
                                    LogEntry.Type.FIX);
                            LogEntryManager.addLogEntry(logKey, errorLogEntry);

                            logger.error(errorMsg);
                            checkItem.setStatus(CheckItem.Status.FAILED);
                            checkItem.setMessage(errorMsg);
                            fixTasksFailed.incrementAndGet();
                            // 更新缓存
                            HostCheckQueueManager.this.updateHostInfoCache(clusterId, hostInfo);
                            return;
                        }

                        // 确保不使用连接池
                        hostInfo.setUseExistingSession(false);
                        hostInfo.setExternalSession(null);

                        LogEntry connLogEntry = createLogEntry(LogEntry.Level.INFO,
                                "正在建立独立SSH连接进行免密登录配置", LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, connLogEntry);

                        // 直接调用免密检查器的fix方法
                        boolean success = passwordFreeChecker.fix(clusterId, hostInfo, checkItem);

                        // 更新结果
                        if (success) {
                            fixTasksSucceeded.incrementAndGet();

                            String successMsg = String.format("主机 %s 的免密登录修复任务执行成功", hostInfo.getIp());
                            LogEntry successLogEntry = createLogEntry(LogEntry.Level.INFO, successMsg,
                                    LogEntry.Type.FIX);
                            LogEntryManager.addLogEntry(logKey, successLogEntry);

                            logger.info(successMsg);
                            // 更新缓存
                            HostCheckQueueManager.this.updateHostInfoCache(clusterId, hostInfo);
                        } else {
                            fixTasksFailed.incrementAndGet();

                            String failMsg = String.format("主机 %s 的免密登录修复任务执行失败", hostInfo.getIp());
                            LogEntry failLogEntry = createLogEntry(LogEntry.Level.WARN, failMsg, LogEntry.Type.FIX);
                            LogEntryManager.addLogEntry(logKey, failLogEntry);

                            logger.warn(failMsg);
                            // 更新缓存
                            HostCheckQueueManager.this.updateHostInfoCache(clusterId, hostInfo);
                        }
                    } catch (Exception e) {
                        String errorMsg = String.format("执行主机 %s 的免密登录修复任务时发生异常: %s",
                                hostInfo.getIp(), e.getMessage());
                        LogEntry exceptionLogEntry = createLogEntry(LogEntry.Level.ERROR, errorMsg,
                                LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, exceptionLogEntry);

                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage("免密登录修复异常: " + e.getMessage());
                        fixTasksFailed.incrementAndGet();
                        logger.error(errorMsg, e);
                        // 更新缓存
                        HostCheckQueueManager.this.updateHostInfoCache(clusterId, hostInfo);
                    }
                } else {
                    // 常规检查项使用正常的doHostFix方法
                    // 创建修复项列表
                    List<CheckItem> fixItems = new ArrayList<>();
                    fixItems.add(checkItem);

                    // 记录将要使用连接池执行修复
                    LogEntry poolLogEntry = createLogEntry(LogEntry.Level.INFO,
                            "将使用SSH连接池执行修复任务: " + checkItem.getItemName(), LogEntry.Type.FIX);
                    LogEntryManager.addLogEntry(logKey, poolLogEntry);

                    // 使用doHostFix方法批量执行修复，实现SSH连接复用
                    boolean success = hostCheckService.doHostFix(clusterId, hostInfo, fixItems);

                    // 更新结果
                    if (success) {
                        fixTasksSucceeded.incrementAndGet();

                        String successMsg = String.format("主机 %s 的修复任务 %s 执行成功",
                                hostInfo.getIp(), checkItem.getItemName());
                        LogEntry successLogEntry = createLogEntry(LogEntry.Level.INFO, successMsg,
                                LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, successLogEntry);

                        logger.info(successMsg);
                        // 更新缓存
                        HostCheckQueueManager.this.updateHostInfoCache(clusterId, hostInfo);
                    } else {
                        fixTasksFailed.incrementAndGet();

                        String failMsg = String.format("主机 %s 的修复任务 %s 执行失败",
                                hostInfo.getIp(), checkItem.getItemName());
                        LogEntry failLogEntry = createLogEntry(LogEntry.Level.WARN, failMsg, LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, failLogEntry);

                        logger.warn(failMsg);
                        // 更新缓存
                        HostCheckQueueManager.this.updateHostInfoCache(clusterId, hostInfo);
                    }
                }

            } catch (Exception e) {
                // 处理异常情况
                String errorMsg = String.format("执行主机 %s 的修复任务时发生异常: %s",
                        hostInfo.getIp(), e.getMessage());
                LogEntry exceptionLogEntry = createLogEntry(LogEntry.Level.ERROR, errorMsg, LogEntry.Type.FIX);
                LogEntryManager.addLogEntry(logKey, exceptionLogEntry);

                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("修复异常: " + e.getMessage());
                fixTasksFailed.incrementAndGet();
                logger.error(errorMsg, e);
                // 更新缓存
                HostCheckQueueManager.this.updateHostInfoCache(clusterId, hostInfo);
            } finally {
                // 记录任务完成
                String endMessage = String.format("主机 %s 的修复任务 %s 已完成，耗时: %d 毫秒",
                        hostInfo.getIp(), checkItem.getItemName(), System.currentTimeMillis() - startTime);
                LogEntry endLogEntry = createLogEntry(LogEntry.Level.INFO, endMessage, LogEntry.Type.FIX);
                LogEntryManager.addLogEntry(logKey, endLogEntry);

                // 统计执行时间
                long executionTime = System.currentTimeMillis() - startTime;
                fixTasksTotalExecutionTimeMs.addAndGet(executionTime);

                // 更新最大执行时间
                long currentMax = fixTasksMaxExecutionTimeMs.get();
                while (executionTime > currentMax) {
                    if (fixTasksMaxExecutionTimeMs.compareAndSet(currentMax, executionTime)) {
                        break;
                    }
                    currentMax = fixTasksMaxExecutionTimeMs.get();
                }

                // 移除任务
                runningFixTasks.remove(taskKey);
                // 确保从fixTaskStartTimes中删除任务记录，防止任务完成后仍被当作超时任务
                fixTaskStartTimes.remove(taskKey);

                // 恢复原始线程名
                Thread.currentThread().setName(originalThreadName);
            }
        }
    }

    /**
     * 创建日志条目对象
     * 
     * @param level   日志级别
     * @param message 日志消息
     * @param type    日志类型
     * @return 日志条目对象
     */
    private LogEntry createLogEntry(LogEntry.Level level, String message, LogEntry.Type type) {
        Date timestamp = new Date();
        String threadName = Thread.currentThread().getName();
        String className = HostCheckQueueManager.class.getName();

        LogEntry logEntry = new LogEntry(timestamp, level, threadName, className, message, type);
        // 获取调用者的行号作为元数据
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length >= 3) {
            logEntry.setLineNumber(stackTrace[2].getLineNumber());
        }

        return logEntry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 记录应用启动时间
        applicationStartTime = System.currentTimeMillis();
        logger.info("应用启动时间已记录: {}",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(applicationStartTime)));
    }

    /**
     * 格式化持续时间
     * 
     * @param durationMs 持续时间（毫秒）
     * @return 格式化的持续时间字符串
     */
    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("分");
        }
        sb.append(seconds).append("秒");

        return sb.toString();
    }

    /**
     * 修复队列处理器方法
     */
    private void processFixQueue() {
        if (!isRunning.get()) {
            logger.info("修复队列处理器已停止，不再处理任务");
            return;
        }

        ThreadPoolExecutor fixThreadPool = (ThreadPoolExecutor) fixExecutorService;

        // 检查是否有可用线程
        int activeCount = fixThreadPool.getActiveCount();
        int corePoolSize = fixThreadPool.getCorePoolSize();

        if (activeCount >= corePoolSize) {
            logger.debug("当前修复线程池已满: 活动线程数={}, 核心线程数={}", activeCount, corePoolSize);
            return;
        }

        logger.debug("当前修复线程池状态: 活动线程数={}, 核心线程数={}", activeCount, corePoolSize);

        try {
            // 从队列中获取任务
            FixTask fixTask = fixQueue.peek();
            if (fixTask == null) {
                return;
            }

            // 获取任务信息
            Integer clusterId = fixTask.getClusterId();
            HostInfo hostInfo = fixTask.getHostInfo();
            CheckItem checkItem = fixTask.getCheckItem();
            HostCheckServiceImpl hostCheckServiceImpl = fixTask.getHostCheckService();

            // 检查是否是免密检查项
            boolean isPasswordFreeItem = ItemCode.PASSWORD_FREE.equals(checkItem.getItemCode());
            String taskKey = getFixTaskKey(clusterId, hostInfo.getIp(), checkItem.getId());

            // 任务已在运行中，跳过
            if (runningFixTasks.containsKey(taskKey)) {
                logger.debug("修复任务已经在运行中，跳过: {}", taskKey);
                return;
            }

            // 移除任务
            fixQueue.poll();
            fixTaskKeysInQueue.remove(taskKey);

            logger.info("正在处理修复任务: clusterId={}, 主机={}, 检查项={}",
                    clusterId, hostInfo.getIp(), checkItem.getItemName());

            // 将任务提交到线程池执行
            Future<?> future = fixExecutorService.submit(new HostFixTask(
                    clusterId, hostInfo, checkItem, taskKey, hostCheckServiceImpl));

            // 记录任务开始时间
            fixTaskStartTimes.put(taskKey, System.currentTimeMillis());
            runningFixTasks.put(taskKey, future);
        } catch (Exception e) {
            logger.error("处理修复队列任务时发生异常", e);
        }
    }

    public ExecutorService getItemCheckExecutorService() {
        return itemCheckExecutorService;
    }

    /**
     * 更新主机信息缓存
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     */
    private void updateHostInfoCache(Integer clusterId, HostInfo hostInfo) {
        try {
            Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
            if (map != null) {
                map.put(hostInfo.getIp(), hostInfo);
                CacheUtils.put(clusterId + Constants.HOST_MAP, map);
                logger.debug("已更新主机信息缓存: clusterId={}, hostname={}", clusterId, hostInfo.getIp());
            } else {
                logger.warn("无法更新主机信息缓存，未找到集群对应的缓存: clusterId={}", clusterId);
            }
        } catch (Exception e) {
            logger.error("更新主机信息缓存时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理修复任务完成后的操作
     * 该方法会在修复任务完成后被调用，确保状态正确更新并刷新到UI
     */
    private void handleFixTaskCompletion(FixTask fixTask, boolean success) {
        // 获取任务信息
        Integer clusterId = fixTask.getClusterId();
        HostInfo hostInfo = fixTask.getHostInfo();
        CheckItem checkItem = fixTask.getCheckItem();

        try {
            if (success) {
                // 记录修复前的状态以便日志记录
                CheckItem.Status oldStatus = checkItem.getStatus();
                logger.info("修复任务执行成功，准备更新状态: clusterId={}, hostname={}, itemId={}, 原状态={}",
                        clusterId, hostInfo.getIp(), checkItem.getId(), oldStatus);

                // 修复成功后，强制更新状态为SUCCESS
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage("修复成功");

                // 同时更新HostInfo中该检查项的状态，确保两边同步
                hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.SUCCESS, "修复成功");

                // 对于特定类型的检查项（如Java环境），增加额外的日志和检查
                // 获取检查项代码，如果是JAVA_ENV，增加特殊处理
                if ("JAVA_ENV".equals(checkItem.getItemCode())) {
                    logger.info("Java环境检查项修复成功，特别确保状态更新: clusterId={}, hostname={}, itemId={}, 原状态={}, 新状态=SUCCESS",
                            clusterId, hostInfo.getIp(), checkItem.getId(), oldStatus);

                    // 直接遍历并更新检查项状态，确保修改生效
                    boolean foundInHost = false;
                    if (hostInfo.getCheckItems() != null) {
                        for (CheckItem item : hostInfo.getCheckItems()) {
                            if (item.getId().equals(checkItem.getId())) {
                                foundInHost = true;
                                // 记录原始状态
                                CheckItem.Status beforeUpdate = item.getStatus();
                                // 强制更新状态
                                item.setStatus(CheckItem.Status.SUCCESS);
                                item.setMessage("Java环境修复成功");
                                logger.info("Java环境检查项在HostInfo中状态更新: 原状态={}, 新状态=SUCCESS",
                                        beforeUpdate);
                                break;
                            }
                        }
                    }

                    if (!foundInHost) {
                        logger.warn("Java环境检查项在HostInfo中未找到: itemId={}", checkItem.getId());
                    }
                }

                // 重新计算主机状态
                hostInfo.calculateStatus();

                // 更新缓存中的主机信息
                updateHostInfoCache(clusterId, hostInfo);

                // 立即再次获取并检查状态，确保状态已经正确更新
                Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
                if (map != null) {
                    HostInfo cachedInfo = map.get(hostInfo.getIp());
                    if (cachedInfo != null) {
                        CheckItem cachedItem = null;
                        for (CheckItem item : cachedInfo.getCheckItems()) {
                            if (item.getId().equals(checkItem.getId())) {
                                cachedItem = item;
                                break;
                            }
                        }

                        if (cachedItem != null) {
                            logger.info("修复任务完成后缓存状态确认: clusterId={}, hostname={}, itemId={}, 缓存状态={}",
                                    clusterId, hostInfo.getIp(), checkItem.getId(), cachedItem.getStatus());

                            // 如果缓存中的状态不是SUCCESS，强制更新
                            if (cachedItem.getStatus() != CheckItem.Status.SUCCESS) {
                                logger.warn("修复后状态未正确更新，强制更新状态: clusterId={}, hostname={}, itemId={}",
                                        clusterId, hostInfo.getIp(), checkItem.getId());
                                cachedItem.setStatus(CheckItem.Status.SUCCESS);
                                cachedItem.setMessage("修复成功（状态已强制更新）");
                                cachedInfo.calculateStatus();
                                map.put(hostInfo.getIp(), cachedInfo);
                                CacheUtils.put(clusterId + Constants.HOST_MAP, map);

                                // 对Java环境特殊处理，确保状态正确
                                if ("JAVA_ENV".equals(checkItem.getItemCode())) {
                                    // 再次延迟确认状态（前面的状态更新可能被其他线程覆盖）
                                    try {
                                        Thread.sleep(500); // 短暂延迟，等待其他可能的状态更新

                                        // 最终状态确认和强制更新
                                        HostInfo finalInfo = map.get(hostInfo.getIp());
                                        if (finalInfo != null) {
                                            for (CheckItem item : finalInfo.getCheckItems()) {
                                                if (item.getId().equals(checkItem.getId()) &&
                                                        item.getStatus() != CheckItem.Status.SUCCESS) {
                                                    logger.warn("Java环境最终状态仍不正确，再次强制更新: 当前状态={}",
                                                            item.getStatus());
                                                    item.setStatus(CheckItem.Status.SUCCESS);
                                                    item.setMessage("Java环境修复成功（最终强制更新）");
                                                    finalInfo.calculateStatus();
                                                    map.put(hostInfo.getIp(), finalInfo);
                                                    CacheUtils.put(clusterId + Constants.HOST_MAP, map);
                                                }
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        logger.warn("Java环境状态确认延迟被中断", e);
                                    }
                                }
                            }
                        }
                    }
                }

                logger.info("修复任务完成后状态已更新: clusterId={}, hostname={}, itemId={}, 状态={}",
                        clusterId, hostInfo.getIp(), checkItem.getId(), CheckItem.Status.SUCCESS);
            } else {
                // 修复失败
                logger.info("修复任务执行失败: clusterId={}, hostname={}, itemId={}",
                        clusterId, hostInfo.getIp(), checkItem.getId());

                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("修复失败");

                hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, "修复失败");
                hostInfo.calculateStatus();
                updateHostInfoCache(clusterId, hostInfo);

                logger.info("修复任务失败后状态已更新: clusterId={}, hostname={}, itemId={}, 状态={}",
                        clusterId, hostInfo.getIp(), checkItem.getId(), CheckItem.Status.FAILED);
            }
        } catch (Exception e) {
            logger.error("处理修复任务完成后更新状态时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新最大执行时间
     * 
     * @param maxTimeAtomic 最大时间原子变量
     * @param currentTimeMs 当前执行时间
     */
    private void updateMaxExecutionTime(AtomicLong maxTimeAtomic, long currentTimeMs) {
        long currentMax;
        do {
            currentMax = maxTimeAtomic.get();
            if (currentTimeMs <= currentMax) {
                break;
            }
        } while (!maxTimeAtomic.compareAndSet(currentMax, currentTimeMs));
    }

    /**
     * 修复任务执行器
     * 负责执行具体的修复操作并更新任务状态
     */
    private class ExecuteFixTaskJob implements Runnable {
        private final FixTask fixTask;
        private final List<CheckItem> fixItems;
        private final long startTimeMs;
        private final String taskKey;
        private static final int MAX_RETRY_ATTEMPTS = 3; // 最大重试次数
        private static final long RETRY_DELAY_MS = 5000; // 重试间隔时间，5秒

        public ExecuteFixTaskJob(FixTask fixTask, List<CheckItem> fixItems, String taskKey) {
            this.fixTask = fixTask;
            this.fixItems = fixItems;
            this.startTimeMs = System.currentTimeMillis();
            this.taskKey = taskKey;
        }

        @Override
        public void run() {
            logger.info("开始执行主机 {} 上的修复任务", fixTask.getHostInfo().getIp());
            Integer clusterId = fixTask.getClusterId();
            HostInfo hostInfo = fixTask.getHostInfo();
            HostCheckServiceImpl hostCheckService = fixTask.getHostCheckService();
            List<String> fixItemNames = fixItems.stream()
                    .map(CheckItem::getItemName)
                    .collect(Collectors.toList());

            boolean success = false;
            int attempts = 0;
            Exception lastException = null;

            // 自动重试逻辑，最多尝试MAX_RETRY_ATTEMPTS次
            while (!success && attempts < MAX_RETRY_ATTEMPTS) {
                attempts++;
                try {
                    logger.info("执行主机 {} 的修复任务，第 {} 次尝试，修复项: {}", hostInfo.getIp(),
                            attempts, String.join(", ", fixItemNames));

                    // 执行修复操作
                    success = hostCheckService.doHostFix(clusterId, hostInfo, fixItems);

                    if (success) {
                        logger.info("主机 {} 的修复任务在第 {} 次尝试成功完成", hostInfo.getIp(), attempts);
                        break; // 修复成功，跳出循环
                    } else {
                        logger.warn("主机 {} 的修复任务第 {} 次尝试失败", hostInfo.getIp(), attempts);
                        if (attempts < MAX_RETRY_ATTEMPTS) {
                            logger.info("将在 {} 毫秒后进行第 {} 次重试", RETRY_DELAY_MS, attempts + 1);
                            Thread.sleep(RETRY_DELAY_MS); // 延迟一段时间后重试
                        }
                    }
                } catch (Exception e) {
                    lastException = e;
                    logger.error("主机 {} 的修复任务第 {} 次尝试出现异常: {}", hostInfo.getIp(),
                            attempts, e.getMessage(), e);
                    if (attempts < MAX_RETRY_ATTEMPTS) {
                        logger.info("将在 {} 毫秒后进行第 {} 次重试", RETRY_DELAY_MS, attempts + 1);
                        try {
                            Thread.sleep(RETRY_DELAY_MS); // 延迟一段时间后重试
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            logger.warn("重试等待被中断", ie);
                            break; // 如果线程被中断，退出重试
                        }
                    }
                }
            }

            // 计算任务执行耗时
            long executionTimeMs = System.currentTimeMillis() - startTimeMs;
            fixTasksTotalExecutionTimeMs.addAndGet(executionTimeMs);
            updateMaxExecutionTime(fixTasksMaxExecutionTimeMs, executionTimeMs);

            // 更新运行统计信息
            fixTasksProcessed.incrementAndGet();
            if (success) {
                fixTasksSucceeded.incrementAndGet();
                logger.info("主机 {} 的修复任务成功完成，耗时 {} ms，尝试次数: {}/{}",
                        hostInfo.getIp(), executionTimeMs, attempts, MAX_RETRY_ATTEMPTS);
            } else {
                fixTasksFailed.incrementAndGet();
                if (lastException != null) {
                    logger.error("主机 {} 的修复任务最终失败，耗时 {} ms，尝试次数: {}/{}, 最后错误: {}",
                            hostInfo.getIp(), executionTimeMs, attempts, MAX_RETRY_ATTEMPTS,
                            lastException.getMessage());
                } else {
                    logger.error("主机 {} 的修复任务最终失败，耗时 {} ms，尝试次数: {}/{}",
                            hostInfo.getIp(), executionTimeMs, attempts, MAX_RETRY_ATTEMPTS);
                }
            }

            // 处理任务完成逻辑
            handleFixTaskCompletion(fixTask, success);

            // 从运行任务列表中移除当前任务
            try {
                runningFixTasks.remove(taskKey);
                fixTaskKeysInQueue.remove(taskKey);
            } catch (Exception e) {
                logger.error("移除修复任务 {} 时出错: {}", taskKey, e.getMessage(), e);
            }
        }
    }
}