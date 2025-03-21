package com.datasophon.api.service.impl;

import com.datasophon.api.service.checker.ItemChecker;
import com.datasophon.api.service.checker.ItemCheckerFactory;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import com.datasophon.common.model.QueueManagerStatus;
import com.datasophon.common.model.QueueTaskInfo;
import lombok.Getter;
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
    @Getter
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
    
    private long queueHealthMonitorIntervalMs;
    private long taskTimeoutMonitorIntervalMs;

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
        startScheduledTasks();
        logger.info("主机检查队列管理器初始化完成");
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
        
        // 启动队列健康监控任务（每2分钟执行一次）
        if (queueHealthMonitorTask == null || queueHealthMonitorTask.isCancelled()) {
            queueHealthMonitorTask = taskScheduler.scheduleAtFixedRate(
                this::monitorQueueHealth, 120000);
            logger.info("队列健康监控定时任务已启动，执行间隔: 2分钟");
        }
        
        // 启动任务超时监控（每30秒执行一次）
        if (taskTimeoutMonitorTask == null || taskTimeoutMonitorTask.isCancelled()) {
            taskTimeoutMonitorTask = taskScheduler.scheduleAtFixedRate(
                this::checkForTaskTimeouts, 30000);
            logger.info("任务超时监控定时任务已启动，执行间隔: 30秒");
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

    public void addCheckTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService) {
        String taskKey = getTaskKey(clusterId, hostInfo.getHostname());
        try {
            // 强制确保所有组件处于运行状态
            ensureSystemRunning();
            
            // 如果任务已在运行，则不添加
            if (runningTasks.containsKey(taskKey)) {
                logger.info("主机 {} 的检查任务正在运行中，跳过本次添加", hostInfo.getHostname());
                return;
            }
            
            // 使用Set检查队列中是否已存在该任务，避免遍历队列
            if (taskKeysInQueue.contains(taskKey)) {
                logger.info("主机 {} 的检查任务已在队列中等待执行，跳过本次添加", hostInfo.getHostname());
                return;
            }
            
            logger.info("正在将主机 {} 的检查任务添加到队列，当前队列大小: {}, 运行中任务数: {}", 
                hostInfo.getHostname(), checkQueue.size(), runningTasks.size());
            
            // 创建任务（使用默认优先级）
            CheckTask newTask = new CheckTask(clusterId, hostInfo, hostCheckService);
            boolean added = checkQueue.offer(newTask, 10, TimeUnit.SECONDS); // 添加超时机制
            
            if (added) {
                // 添加到Set中跟踪
                taskKeysInQueue.add(taskKey);
            logger.info("成功添加主机 {} 的检查任务到队列，新队列大小: {}", 
                hostInfo.getHostname(), checkQueue.size());
            } else {
                logger.error("添加主机 {} 的检查任务到队列失败，队列可能已满", hostInfo.getHostname());
            }
            
        } catch (InterruptedException e) {
            logger.error("添加检查任务被中断: {}", hostInfo.getHostname(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("添加检查任务时发生错误: {}, {}", hostInfo.getHostname(), e.getMessage(), e);
        }
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
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
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
        
        // 9. 确保队列健康监控和任务超时监控一定被启动
        startQueueHealthMonitor();
        startTaskTimeoutMonitor();
        
        logger.info("系统组件状态检查完成，所有组件已确保运行");
    }

    /**
     * 添加带优先级的检查任务
     */
    public void addCheckTaskWithPriority(Integer clusterId, HostInfo hostInfo, 
                                        HostCheckServiceImpl hostCheckService, int priority) {
        String taskKey = getTaskKey(clusterId, hostInfo.getHostname());
        try {
            // 如果任务已在运行，则不添加
            if (runningTasks.containsKey(taskKey)) {
                logger.info("主机 {} 的检查任务正在运行中，跳过本次添加", hostInfo.getHostname());
                return;
            }
            
            // 使用Set检查队列中是否已存在该任务
            if (taskKeysInQueue.contains(taskKey)) {
                logger.info("主机 {} 的检查任务已在队列中等待执行，跳过本次添加", hostInfo.getHostname());
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
                    hostInfo.getHostname(), priority, checkQueue.size());
            } else {
                logger.error("添加主机 {} 的检查任务到队列失败，队列可能已满", hostInfo.getHostname());
            }
        } catch (Exception e) {
            logger.error("添加检查任务时发生错误: {}, {}", hostInfo.getHostname(), e.getMessage(), e);
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
            boolean shouldRemove = getTaskKey(task.getClusterId(), task.getHostInfo().getHostname()).equals(taskKey);
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
                }
                
                // 添加超时机制，避免无限期阻塞
                CheckTask task = checkQueue.poll(5, TimeUnit.SECONDS);
                
                // 如果队列为空，继续等待
                if (task == null) {
                    continue;
                }
                
                String hostname = task.getHostInfo().getHostname();
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
            Thread.currentThread().setName("host-check-" + hostInfo.getHostname());
            logger.info("开始执行主机 {} 的检查任务", hostInfo.getHostname());
            
            long startTime = System.currentTimeMillis();
            tasksProcessed.incrementAndGet();
            
            try {
                hostCheckService.processHostCheck(clusterId, hostInfo);
                logger.info("主机 {} 的检查任务执行完成", hostInfo.getHostname());
                tasksSucceeded.incrementAndGet();
            } catch (Exception e) {
                logger.error("执行主机 {} 的检查任务时发生错误: {}", 
                    hostInfo.getHostname(), e.getMessage(), e);
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
                
                logger.info("移除主机 {} 的检查任务，释放资源", hostInfo.getHostname());
                runningTasks.remove(taskKey);
                taskStartTimes.remove(taskKey);
            }
        }
    }

    @Getter
    private static class CheckTask implements Comparable<CheckTask> {
        private final Integer clusterId;
        private final HostInfo hostInfo;
        private final HostCheckServiceImpl hostCheckService;
        private final int priority; // 优先级，数字越小优先级越高

        public CheckTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService) {
            this(clusterId, hostInfo, hostCheckService, 5); // 默认优先级为5
        }
        
        public CheckTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService, int priority) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.hostCheckService = hostCheckService;
            this.priority = priority;
        }

        @Override
        public int compareTo(CheckTask other) {
            return Integer.compare(this.priority, other.priority);
        }
    }

    /**
     * 获取队列管理器状态（返回实体类）
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
        
        // 线程池信息
        ThreadPoolExecutor mainExecutor = (ThreadPoolExecutor) checkExecutorService;
        ThreadPoolExecutor itemExecutor = (ThreadPoolExecutor) itemCheckExecutorService;
        
        // 修复线程池可能为null，进行判断
        int fixActiveCount = 0;
        int fixQueueSize = 0;
        int fixPoolSize = 0;
        long fixCompletedTasks = 0;
        
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
            taskInfo.setHostname(task.getHostInfo().getHostname());
            taskInfo.setTaskId("waiting_" + task.getClusterId() + ":" + task.getHostInfo().getHostname());
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
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     */
    public void processHostFix(Integer clusterId, HostInfo hostInfo) {
        logger.info("开始为主机 {} 执行修复任务", hostInfo.getHostname());
        
        // 修复状态为FAILED的所有检查项
        List<CheckItem> checkItems = hostInfo.getCheckItems();
        if (checkItems == null || checkItems.isEmpty()) {
            logger.warn("主机 {} 没有需要修复的检查项", hostInfo.getHostname());
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
            hostInfo.getHostname(), fixedCount, failedCount);
    }
    
    /**
     * 修复单个检查项
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
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
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @return 任务ID或null（如果任务已在队列中）
     */
    public String addFixTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 生成任务标识和追踪键
            String taskId = "FIX_" + clusterId + "_" + hostInfo.getHostname() + "_" + checkItem.getId();
            String taskKey = getFixTaskKey(clusterId, hostInfo.getHostname(), checkItem.getId());
            
            // 检查是否已经在队列中
            if (fixTaskKeysInQueue.contains(taskKey)) {
                logger.info("主机 {} 的修复任务已在队列中, 项目: {}", 
                    hostInfo.getHostname(), checkItem.getItemName());
                return null;
            }
            
            // 检查是否正在运行
            if (runningFixTasks.containsKey(taskKey)) {
                logger.info("主机 {} 的修复任务正在执行中, 项目: {}", 
                    hostInfo.getHostname(), checkItem.getItemName());
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
                
                String hostname = task.getHostInfo().getHostname();
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
                        break;
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
            taskInfo.setHostname(task.getHostInfo().getHostname());
            taskInfo.setTaskId("waiting_fix_" + task.getClusterId() + ":" + task.getHostInfo().getHostname());
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
        
        public HostFixTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem, String taskKey, HostCheckServiceImpl hostCheckService) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.checkItem = checkItem;
            this.taskKey = taskKey;
            this.hostCheckService = hostCheckService;
        }
        
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                logger.info("开始执行主机 {} 的修复任务: {}", 
                    hostInfo.getHostname(), checkItem.getItemName());
                
                // 标记修复任务为进行中
                checkItem.setStatus(CheckItem.Status.FIXING);
                
                // 统计信息
                fixTasksProcessed.incrementAndGet();
                
                // 创建修复项列表
                List<CheckItem> fixItems = new ArrayList<>();
                fixItems.add(checkItem);
                
                // 使用doHostFix方法批量执行修复，实现SSH连接复用
                boolean success = hostCheckService.doHostFix(clusterId, hostInfo, fixItems);
                
                // 更新结果
                if (success) {
                    fixTasksSucceeded.incrementAndGet();
                    logger.info("主机 {} 的修复任务 {} 执行成功", 
                        hostInfo.getHostname(), checkItem.getItemName());
                } else {
                    fixTasksFailed.incrementAndGet();
                    logger.warn("主机 {} 的修复任务 {} 执行失败", 
                        hostInfo.getHostname(), checkItem.getItemName());
                }
                
            } catch (Exception e) {
                // 处理异常情况
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("修复异常: " + e.getMessage());
                fixTasksFailed.incrementAndGet();
                logger.error("执行主机 {} 的修复任务时发生异常: {}", 
                    hostInfo.getHostname(), e.getMessage(), e);
            } finally {
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
            }
        }
    }
    
    /**
     * 修复任务数据结构
     */
    @Getter
    private static class FixTask implements Comparable<FixTask> {
        private final Integer clusterId;
        private final HostInfo hostInfo;
        private final CheckItem checkItem;
        private final int priority; // 优先级，数字越小优先级越高
        private final HostCheckServiceImpl hostCheckService;
        
        public FixTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
            this(clusterId, hostInfo, checkItem, 5); // 默认优先级为5
        }
        
        public FixTask(Integer clusterId, HostInfo hostInfo, CheckItem checkItem, int priority) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.checkItem = checkItem;
            this.priority = priority;
            this.hostCheckService = null; // 默认为null，由HostFixTask在运行时从Spring获取
        }
        
        @Override
        public int compareTo(FixTask other) {
            return Integer.compare(this.priority, other.priority);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 记录应用启动时间
        applicationStartTime = System.currentTimeMillis();
        logger.info("应用启动时间已记录: {}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(applicationStartTime)));
    }

    /**
     * 格式化持续时间
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
} 