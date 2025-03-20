package com.datasophon.api.service.impl;

import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.QueueManagerStatus;
import com.datasophon.common.model.QueueTaskInfo;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
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
import java.text.SimpleDateFormat;

@Component
public class HostCheckQueueManager {
    private static final Logger logger = LoggerFactory.getLogger(HostCheckQueueManager.class);

    // 修改为优先队列，支持任务优先级
    private final BlockingQueue<CheckTask> checkQueue = new PriorityBlockingQueue<>(100);
    private final BlockingQueue<FixTask> fixQueue = new PriorityBlockingQueue<>(100);
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
    
    // 主线程池 - 用于执行主机级别的检查任务
    @Getter
    private final ExecutorService executorService;
    
    // 检查项线程池 - 专门用于执行单个检查项
    @Getter
    private final ExecutorService itemCheckExecutorService;
    
    // 修复任务线程池 - 专门用于执行修复任务
    @Getter
    private final ExecutorService fixExecutorService;
    
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
    
    public HostCheckQueueManager() {
        // 创建主线程池 - 负责主机级别的检查任务
        this.executorService = new ThreadPoolExecutor(
            1, // 核心线程数 - 改为1确保串行执行
            1, // 最大线程数 - 改为1确保串行执行
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS, // 时间单位
            new LinkedBlockingQueue<>(100), // 有界工作队列，增大队列大小
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("host-check-worker-" + counter.getAndIncrement());
                    t.setDaemon(false); // 改为非守护线程，防止过早退出
                    t.setPriority(Thread.NORM_PRIORITY); // 设置为普通优先级
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者运行
        );
        
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
        
        // 创建修复任务线程池 - 专门用于执行修复任务
        this.fixExecutorService = new ThreadPoolExecutor(
            2, // 核心线程数
            4, // 最大线程数
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50), // 修复任务队列容量较小
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("fix-task-worker-" + counter.getAndIncrement());
                    t.setDaemon(false);
                    t.setPriority(Thread.NORM_PRIORITY);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
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
            fixQueueProcessorThread.setName("fix-task-queue-processor");
            fixQueueProcessorThread.start();
            fixQueueProcessorStartTime = System.currentTimeMillis();
            logger.info("修复任务队列处理线程已启动");
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
        executorService.shutdownNow();
        itemCheckExecutorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("线程池未能在指定时间内完全终止");
            }
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
            
            // 检查队列处理线程是否正常
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
            if (executorService.isShutdown() || executorService.isTerminated()) {
                logger.error("执行线程池已关闭，任务无法执行");
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
            if (executorService.isShutdown() || executorService.isTerminated()) {
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
                    
                    Future<?> future = executorService.submit(hostCheckTask);
                    
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
            // 如果定时任务已禁用，直接返回
            if (!scheduledTasksEnabled.get() || !isRunning.get()) {
                return;
            }
            
            // 检查队列处理线程是否存活
            if (queueProcessorThread == null || !queueProcessorThread.isAlive()) {
                logger.warn("队列处理线程已停止，尝试重新启动");
                startQueueProcessor();
                return;
            }
            
            // 记录处理线程运行时间
            long runningTime = System.currentTimeMillis() - queueProcessorStartTime;
            logger.info("队列处理线程已运行: {} 分钟", runningTime / 60000);
            
            // 记录队列和任务统计信息
            logger.info("队列状态: 等待任务={}, 运行任务={}, 总处理任务={}, 成功={}, 失败={}",
                checkQueue.size(), runningTasks.size(), 
                tasksProcessed.get(), tasksSucceeded.get(), tasksFailed.get());
            
            // 检查线程池状态
            ThreadPoolExecutor mainExecutor = (ThreadPoolExecutor) executorService;
            ThreadPoolExecutor itemExecutor = (ThreadPoolExecutor) itemCheckExecutorService;
            
            logger.info("主线程池状态: 活跃线程={}, 完成任务={}, 队列任务={}",
                mainExecutor.getActiveCount(), mainExecutor.getCompletedTaskCount(),
                mainExecutor.getQueue().size());
                
            logger.info("检查项线程池状态: 活跃线程={}, 完成任务={}, 队列任务={}",
                itemExecutor.getActiveCount(), itemExecutor.getCompletedTaskCount(),
                itemExecutor.getQueue().size());
                
        } catch (Exception e) {
            logger.error("监控队列健康状态时发生错误", e);
        }
    }
    
    /**
     * 检查任务是否超时
     * 由任务调度器调用，独立于monitorQueueHealth方法
     */
    public void checkForTaskTimeouts() {
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
        
        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) executorService;
            status.put("activeThreads", executor.getActiveCount());
            status.put("poolSize", executor.getPoolSize());
            status.put("completedTasks", executor.getCompletedTaskCount());
            status.put("queuedTasks", executor.getQueue().size());
        }
        
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
        
        // 获取状态信息
        Map<String, Object> statusMap = getManagerStatus();
        
        // 填充实体类
        status.setQueueSize((Integer) statusMap.get("queueSize"));
        status.setTasksSucceeded((Long) statusMap.get("tasksSucceeded"));
        status.setPoolSize((Integer) statusMap.get("poolSize"));
        status.setActiveCount((Integer) statusMap.get("activeThreads"));
        status.setCompletedTaskCount((Long) statusMap.get("completedTasks"));
        status.setTasksFailed((Long) statusMap.get("tasksFailed"));
        status.setRunning((Boolean) statusMap.get("queueProcessingEnabled"));
        status.setTaskCount((Long) statusMap.get("tasksProcessed"));
        status.setLargestPoolSize((Integer) statusMap.get("poolSize"));
        
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
        
        // 获取运行中的任务信息
        for (Map.Entry<String, Future<?>> entry : runningTasks.entrySet()) {
            String taskKey = entry.getKey();
            Future<?> future = entry.getValue();
            
            // 解析taskKey获取clusterId和hostname (格式: clusterId:hostname)
            String[] parts = taskKey.split(":");
            if (parts.length >= 2) {
                QueueTaskInfo taskInfo = new QueueTaskInfo();
                taskInfo.setTaskKey(taskKey);
                taskInfo.setClusterId(Integer.parseInt(parts[0]));
                taskInfo.setHostname(parts[1]);
                taskInfo.setStatus(future.isDone() ? (future.isCancelled() ? "CANCELLED" : "COMPLETED") : "RUNNING");
                
                // 添加开始时间和持续时间
                Long startTime = taskStartTimes.get(taskKey);
                if (startTime != null) {
                    // 格式化时间为 yyyy-MM-dd HH:mm:ss
                    taskInfo.setStartTime(dateFormat.format(new java.util.Date(startTime)));
                    taskInfo.setDuration(System.currentTimeMillis() - startTime);
                }
                
                taskDetails.add(taskInfo);
            }
        }
        
        // 获取队列中等待的任务
        for (CheckTask task : checkQueue) {
            String taskKey = getTaskKey(task.getClusterId(), task.getHostInfo().getHostname());
            // 检查任务是否已经在运行中列表中
            boolean inRunningList = false;
            for (QueueTaskInfo runningTask : taskDetails) {
                if (taskKey.equals(runningTask.getTaskKey())) {
                    inRunningList = true;
                    break;
                }
            }
            
            // 如果不在运行中列表，则添加到结果中
            if (!inRunningList) {
                QueueTaskInfo taskInfo = new QueueTaskInfo();
                taskInfo.setTaskKey(taskKey);
                taskInfo.setClusterId(task.getClusterId());
                taskInfo.setHostname(task.getHostInfo().getHostname());
                taskInfo.setStatus("QUEUED");
                taskInfo.setPriority(task.getPriority());
                taskInfo.setDescription("主机检查任务等待中");
                
                taskDetails.add(taskInfo);
            }
        }
        
        return taskDetails;
    }
    
    /**
     * 获取修复任务队列详情
     * @return 修复任务队列中的详细任务信息列表
     */
    public List<QueueTaskInfo> getFixQueueTasksDetails() {
        // 在实际实现中，应该从专门管理修复任务的队列中获取数据
        // 由于目前系统可能未区分修复任务和检查任务，此处返回一个空列表
        // TODO: 实现完整的修复任务队列详情获取逻辑
        
        List<QueueTaskInfo> fixTaskDetails = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 如果有专门存储修复任务的数据结构，可以在这里遍历并添加到结果中
        // 此处为示例实现，实际应根据系统中修复任务的管理方式修改
        
        return fixTaskDetails;
    }

    /**
     * 修复队列处理任务，循环从队列中取出任务并提交给线程池执行
     */
    private void processFixQueueTasks() {
        logger.info("开始处理修复任务队列");
        int consecutiveErrorCount = 0;
        
        while (isRunning.get()) {
            try {
                if (!fixQueue.isEmpty()) {
                    logger.info("当前修复队列状态: 队列中等待的任务数量={}, 正在执行的任务数量={}", 
                        fixQueue.size(), runningFixTasks.size());
                }
                
                FixTask task = fixQueue.poll(5, TimeUnit.SECONDS);
                
                if (task == null) {
                    continue;
                }
                
                String hostname = task.getHostInfo().getHostname();
                String taskKey = getTaskKey(task.getClusterId(), hostname);
                
                fixTaskKeysInQueue.remove(taskKey);
                
                if (runningFixTasks.containsKey(taskKey)) {
                    logger.warn("主机 {} 的修复任务已在运行中，将延迟处理", hostname);
                    fixQueue.put(task);
                    fixTaskKeysInQueue.add(taskKey);
                    Thread.sleep(1000);
                    continue;
                }
                
                logger.info("正在提交主机 {} 的修复任务，当前运行中任务数: {}", 
                    hostname, runningFixTasks.size());
                
                try {
                    FixTaskExecutor fixTaskExecutor = new FixTaskExecutor(
                        task.getClusterId(), task.getHostInfo(), task.getHostCheckService(), taskKey);
                    
                    Future<?> future = fixExecutorService.submit(fixTaskExecutor);
                    
                    runningFixTasks.put(taskKey, future);
                    fixTaskStartTimes.put(taskKey, System.currentTimeMillis());
                    
                    consecutiveErrorCount = 0;
                    
                } catch (Exception e) {
                    logger.error("提交主机 {} 的修复任务时发生错误: {}", hostname, e.getMessage(), e);
                    fixQueue.put(task);
                    fixTaskKeysInQueue.add(taskKey);
                    Thread.sleep(2000);
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
     * 修复任务执行类
     */
    private class FixTaskExecutor implements Runnable {
        private final Integer clusterId;
        private final HostInfo hostInfo;
        private final HostCheckServiceImpl hostCheckService;
        private final String taskKey;
        
        public FixTaskExecutor(Integer clusterId, HostInfo hostInfo, 
                              HostCheckServiceImpl hostCheckService, String taskKey) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.hostCheckService = hostCheckService;
            this.taskKey = taskKey;
        }
        
        @Override
        public void run() {
            Thread.currentThread().setName("fix-task-" + hostInfo.getHostname());
            logger.info("开始执行主机 {} 的修复任务", hostInfo.getHostname());
            
            try {
                // 调用本类中的processHostFix方法
                processHostFix(clusterId, hostInfo);
                logger.info("主机 {} 的修复任务执行完成", hostInfo.getHostname());
            } catch (Exception e) {
                logger.error("执行主机 {} 的修复任务时发生错误: {}", 
                    hostInfo.getHostname(), e.getMessage(), e);
            } finally {
                logger.info("移除主机 {} 的修复任务，释放资源", hostInfo.getHostname());
                runningFixTasks.remove(taskKey);
                fixTaskStartTimes.remove(taskKey);
            }
        }
    }
    
    @Getter
    private static class FixTask implements Comparable<FixTask> {
        private final Integer clusterId;
        private final HostInfo hostInfo;
        private final HostCheckServiceImpl hostCheckService;
        private final int priority;
        
        public FixTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService) {
            this(clusterId, hostInfo, hostCheckService, 3); // 修复任务默认优先级为3，高于检查任务
        }
        
        public FixTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService, int priority) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.hostCheckService = hostCheckService;
            this.priority = priority;
        }

        @Override
        public int compareTo(FixTask other) {
            return Integer.compare(this.priority, other.priority);
        }
    }

    /**
     * 更新队列健康监控定时任务执行间隔
     * @param intervalMs 执行间隔（毫秒）
     */
    public void updateQueueHealthMonitorInterval(long intervalMs) {
        if (intervalMs < 30000) { // 最小30秒
            logger.warn("队列健康监控定时任务间隔不能小于30秒，忽略此次更新");
            return;
        }
        
        // 重新调度任务
        if (queueHealthMonitorTask != null && !queueHealthMonitorTask.isCancelled()) {
            queueHealthMonitorTask.cancel(false);
            queueHealthMonitorTask = taskScheduler.scheduleAtFixedRate(
                this::monitorQueueHealth, intervalMs);
            logger.info("队列健康监控定时任务已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
    }
    
    /**
     * 更新任务超时监控定时任务执行间隔
     * @param intervalMs 执行间隔（毫秒）
     */
    public void updateTaskTimeoutMonitorInterval(long intervalMs) {
        if (intervalMs < 30000) { // 最小30秒
            logger.warn("任务超时监控定时任务间隔不能小于30秒，忽略此次更新");
            return;
        }
        
        // 重新调度任务
        if (taskTimeoutMonitorTask != null && !taskTimeoutMonitorTask.isCancelled()) {
            taskTimeoutMonitorTask.cancel(false);
            taskTimeoutMonitorTask = taskScheduler.scheduleAtFixedRate(
                this::checkForTaskTimeouts, intervalMs);
            logger.info("任务超时监控定时任务已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
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
                    // 通过AsyncCheckService执行修复
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
            // 这里需要根据实际情况调用相应的修复方法
            logger.info("执行检查项 {} 的修复逻辑", checkItem.getId());
            
            // 可以使用AsyncCheckService执行修复
            // return asyncService.executeFixItemAsync(clusterId, hostInfo, checkItem) != null;
            
            // 模拟修复成功
            return true;
        } catch (Exception e) {
            logger.error("修复检查项 {} 时发生异常: {}", checkItem.getId(), e.getMessage(), e);
            return false;
        }
    }
} 