package com.datasophon.api.service.checker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 任务配置
 * 配置异步任务执行器
 */
@Configuration
@EnableAsync
@EnableScheduling
public class TaskConfig {
    private static final Logger logger = LoggerFactory.getLogger(TaskConfig.class);

    /**
     * 检查任务专用执行器
     */
    @Bean(name = "checkExecutor")
    public ExecutorService checkExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int checkQueueCapacity = 200;
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(checkQueueCapacity);

        // 使用自定义ThreadFactory可以在创建线程时动态设置线程名称
        executor.setThreadFactory(new java.util.concurrent.ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(
                    1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                // 这里设置一个基本名称，具体主机名将在提交任务时设置
                thread.setName("host-check-task-" + counter.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        });

        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        logger.info("初始化主机自检任务执行器 - 核心线程:{}, 最大线程:{}, 队列容量:{}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), checkQueueCapacity);
        return executor.getThreadPoolExecutor();
    }

    /**
     * 修复任务执行器
     */
    @Bean(name = "fixExecutor")
    public ExecutorService fixExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int fixQueueCapacity = 100;
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(fixQueueCapacity);

        // 使用自定义ThreadFactory可以在创建线程时动态设置线程名称
        executor.setThreadFactory(new java.util.concurrent.ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(
                    1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                // 这里设置一个基本名称，具体主机名将在提交任务时设置
                thread.setName("host-fix-task-" + counter.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        });

        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        logger.info("初始化主机修复任务执行器 - 核心线程:{}, 最大线程:{}, 队列容量:{}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), fixQueueCapacity);
        return executor.getThreadPoolExecutor();
    }

    /**
     * 主机名设置专用执行器
     */
    @Bean(name = "hostnameExecutor")
    public ExecutorService hostnameExecutor() {
        ThreadPoolTaskExecutor executor = createExecutor(
                8, 16, 100, "hostname-task-",
                "主机名设置任务执行器");
        return executor.getThreadPoolExecutor();
    }

    /**
     * 操作系统信息获取专用执行器
     */
    @Bean(name = "osInfoExecutor")
    public ExecutorService osInfoExecutor() {
        ThreadPoolTaskExecutor executor = createExecutor(
                6, 12, 80, "os-info-task-",
                "操作系统信息任务执行器");
        return executor.getThreadPoolExecutor();
    }

    /**
     * 硬件信息获取专用执行器
     */
    @Bean(name = "hardwareInfoExecutor")
    public ExecutorService hardwareInfoExecutor() {
        ThreadPoolTaskExecutor executor = createExecutor(
                8, 16, 100, "hardware-info-task-",
                "硬件信息任务执行器");
        return executor.getThreadPoolExecutor();
    }

    /**
     * Hosts文件操作专用执行器
     */
    @Bean(name = "hostsFileExecutor")
    public ExecutorService hostsFileExecutor() {
        ThreadPoolTaskExecutor executor = createExecutor(
                5, 10, 50, "hosts-file-task-",
                "Hosts文件任务执行器");
        return executor.getThreadPoolExecutor();
    }

    /**
     * 创建执行器的通用方法
     */
    private ThreadPoolTaskExecutor createExecutor(int corePoolSize, int maxPoolSize,
            int queueCapacity, String threadNamePrefix, String executorName) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);

        // 使用自定义ThreadFactory
        final String prefix = threadNamePrefix;
        executor.setThreadFactory(new java.util.concurrent.ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(
                    1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(prefix + counter.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        });

        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();

        logger.info("初始化{}核心线程:{}, 最大线程:{}, 队列容量:{}",
                executorName, corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }
}