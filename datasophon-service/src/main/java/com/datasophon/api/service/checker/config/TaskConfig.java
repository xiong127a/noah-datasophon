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
}