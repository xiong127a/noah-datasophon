package com.datasophon.api.config;

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
        executor.setThreadNamePrefix("check-task-");
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
        executor.setThreadNamePrefix("fix-task-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        logger.info("初始化主机修复任务执行器 - 核心线程:{}, 最大线程:{}, 队列容量:{}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), fixQueueCapacity);
        return executor.getThreadPoolExecutor();
    }
} 