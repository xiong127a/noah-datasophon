package com.datasophon.api.service.impl;

import com.datasophon.common.model.HostInfo;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
            4, // 核心线程数 - 支持多主机并行检查，从1改为4
            10, // 最大线程数，从8改为10
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
        logger.info("正在初始化主机检查队列管理器...");
        queueProcessorThread = new Thread(this::processQueueTasks);
        queueProcessorThread.setName("host-check-queue-processor");
        queueProcessorThread.start();
        logger.info("主机检查队列处理线程已启动");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("正在关闭主机检查队列管理器...");
        isRunning.set(false);
        
        // 取消所有运行中的任务
        runningTasks.forEach((key, future) -> {
            logger.info("正在取消任务: {}", key);
            future.cancel(true);
        });
        
        // 清空队列
        checkQueue.clear();
        
        // 关闭线程池
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("线程池未能在指定时间内完全终止");
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
                logger.debug("主机 {} 的检查任务正在运行中，跳过本次添加", hostInfo.getHostname());
                return;
            }
            
            // 状态更新已经在HostCheckServiceImpl.checkSingleHost中处理，此处不再重复更新
            
            logger.debug("正在将主机 {} 的检查任务添加到队列，当前队列大小: {}", 
                hostInfo.getHostname(), checkQueue.size());
            checkQueue.put(new CheckTask(clusterId, hostInfo, hostCheckService));
            logger.info("成功添加主机 {} 的检查任务到队列，新队列大小: {}", 
                hostInfo.getHostname(), checkQueue.size());
        } catch (InterruptedException e) {
            logger.error("添加检查任务被中断: {}", hostInfo.getHostname(), e);
            Thread.currentThread().interrupt();
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
            logger.info("正在取消运行中的任务: {}", key);
            future.cancel(true);
        });
        
        // 清空运行任务映射
        runningTasks.clear();
        
        // 清空队列
        int queueSize = checkQueue.size();
        checkQueue.clear();
        
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
        while (isRunning.get()) {
            try {
                logger.debug("等待下一个任务，当前队列大小: {}", checkQueue.size());
                CheckTask task = checkQueue.take();
                String taskKey = getTaskKey(task.getClusterId(), task.getHostInfo().getHostname());
                
                logger.debug("正在提交主机 {} 的检查任务", task.getHostInfo().getHostname());
                Future<?> future = executorService.submit(() -> {
                    try {
                        task.getHostCheckService().processHostCheck(task.getClusterId(), task.getHostInfo());
                    } catch (Exception e) {
                        logger.error("执行主机 {} 的检查任务时发生错误: {}", 
                            task.getHostInfo().getHostname(), e.getMessage(), e);
                    } finally {
                        runningTasks.remove(taskKey);
                    }
                });
                
                // 记录运行中的任务
                runningTasks.put(taskKey, future);
                
            } catch (InterruptedException e) {
                if (isRunning.get()) {
                    logger.error("队列处理被中断", e);
                }
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("队列处理过程中发生意外错误", e);
            }
        }
        logger.info("队列处理已停止");
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