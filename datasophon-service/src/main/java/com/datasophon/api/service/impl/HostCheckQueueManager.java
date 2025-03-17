package com.datasophon.api.service.impl;

import com.datasophon.common.model.HostInfo;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class HostCheckQueueManager {
    private static final Logger logger = LoggerFactory.getLogger(HostCheckQueueManager.class);

    private final BlockingQueue<CheckTask> checkQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    
    // 主线程池 - 用于执行主机级别的检查任务
    @Getter
    private final ExecutorService executorService;
    
    // 检查项线程池 - 专门用于执行单个检查项
    @Getter
    private final ExecutorService itemCheckExecutorService;
    
    private Thread queueProcessorThread;

    public HostCheckQueueManager() {
        // 创建主线程池 - 负责主机级别的检查任务
        this.executorService = new ThreadPoolExecutor(
            4, // 核心线程数 - 支持多主机并行检查
            8, // 最大线程数
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS, // 时间单位
            new LinkedBlockingQueue<>(50), // 有界工作队列，避免无限堆积
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("host-check-worker-" + counter.getAndIncrement());
                    t.setDaemon(true); // 设置为守护线程，避免阻止JVM退出
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者运行
        );
        
        // 创建检查项线程池 - 负责检查项级别的任务
        this.itemCheckExecutorService = new ThreadPoolExecutor(
            8, // 核心线程数
            16, // 最大线程数
            30L, // 空闲线程存活时间
            TimeUnit.SECONDS, // 时间单位
            new LinkedBlockingQueue<>(100), // 有界工作队列
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("item-checker-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.AbortPolicy() // 拒绝策略：抛出异常
        );
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing host check queue manager...");
        queueProcessorThread = new Thread(this::processQueueTasks);
        queueProcessorThread.setName("host-check-queue-processor");
        queueProcessorThread.start();
        logger.info("Host check queue processor thread started");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down host check queue manager...");
        isRunning.set(false);
        
        // 取消所有运行中的任务
        runningTasks.forEach((key, future) -> {
            logger.info("Cancelling task: {}", key);
            future.cancel(true);
        });
        
        // 清空队列
        checkQueue.clear();
        
        // 关闭线程池
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Executor service did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 中断队列处理线程
        queueProcessorThread.interrupt();
    }

    public void addCheckTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService) {
        String taskKey = getTaskKey(clusterId, hostInfo.getHostname());
        try {
            // 如果任务已在运行，则不添加
            if (runningTasks.containsKey(taskKey)) {
                logger.info("Task for host {} is already running, skipping", hostInfo.getHostname());
                return;
            }
            
            logger.info("Adding check task to queue for host: {}, current queue size: {}", 
                hostInfo.getHostname(), checkQueue.size());
            checkQueue.put(new CheckTask(clusterId, hostInfo, hostCheckService));
            logger.info("Successfully added check task for host {} to queue, new queue size: {}", 
                hostInfo.getHostname(), checkQueue.size());
        } catch (InterruptedException e) {
            logger.error("Failed to add check task to queue for host {}", hostInfo.getHostname(), e);
            Thread.currentThread().interrupt();
        }
    }

    public void cancelItemTask(Integer clusterId, String hostname, Integer itemId) {
        logger.info("Cancelling specific check item task: clusterId={}, hostname={}, itemId={}", 
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
            logger.info("Forcibly cancelling task for host: {}", hostname);
            future.cancel(true);
            runningTasks.remove(taskKey);
        }
        
        // 同时从队列中移除待执行的任务
        checkQueue.removeIf(task -> 
            getTaskKey(task.getClusterId(), task.getHostInfo().getHostname()).equals(taskKey));
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
            logger.info("取消运行中的任务: {}", key);
            future.cancel(true);
        });
        
        // 清空运行任务映射
        runningTasks.clear();
        
        // 清空队列
        int queueSize = checkQueue.size();
        checkQueue.clear();
        
        logger.info("成功取消所有检查任务，清空了 {} 个运行中任务和 {} 个队列中任务", 
                    runningTasks.size(), queueSize);
    }

    private String getTaskKey(Integer clusterId, String hostname) {
        return clusterId + ":" + hostname;
    }

    private void processQueueTasks() {
        logger.info("Starting to process queue tasks...");
        while (isRunning.get()) {
            try {
                logger.debug("Waiting for next task, current queue size: {}", checkQueue.size());
                CheckTask task = checkQueue.take();
                String taskKey = getTaskKey(task.getClusterId(), task.getHostInfo().getHostname());
                
                logger.info("Submitting check task for host: {}", task.getHostInfo().getHostname());
                Future<?> future = executorService.submit(() -> {
                    try {
                        task.getHostCheckService().processHostCheck(task.getClusterId(), task.getHostInfo());
                    } catch (Exception e) {
                        logger.error("Error executing check task for host {}: {}", 
                            task.getHostInfo().getHostname(), e.getMessage(), e);
                    } finally {
                        runningTasks.remove(taskKey);
                    }
                });
                
                // 记录运行中的任务
                runningTasks.put(taskKey, future);
                
            } catch (InterruptedException e) {
                if (isRunning.get()) {
                    logger.error("Queue processing interrupted", e);
                }
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Unexpected error in queue processing", e);
            }
        }
        logger.info("Queue processing stopped");
    }

    private static class CheckTask {
        private final Integer clusterId;
        private final HostInfo hostInfo;
        private final HostCheckServiceImpl hostCheckService;

        public CheckTask(Integer clusterId, HostInfo hostInfo, HostCheckServiceImpl hostCheckService) {
            this.clusterId = clusterId;
            this.hostInfo = hostInfo;
            this.hostCheckService = hostCheckService;
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
    }
} 