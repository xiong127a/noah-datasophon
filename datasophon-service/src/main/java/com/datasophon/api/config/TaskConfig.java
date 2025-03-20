package com.datasophon.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
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
    @Bean("checkExecutor")
    public Executor checkExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(10);
        // 队列容量
        executor.setQueueCapacity(50);
        // 线程名前缀
        executor.setThreadNamePrefix("check-exec-");
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(120);
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        logger.info("初始化检查任务执行器: 核心线程={}, 最大线程={}, 队列容量={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), 50);
        
        return executor;
    }
    
    /**
     * 修复任务专用执行器
     */
    @Bean("fixExecutor")
    public Executor fixExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(3);
        // 最大线程数
        executor.setMaxPoolSize(6);
        // 队列容量
        executor.setQueueCapacity(30);
        // 线程名前缀
        executor.setThreadNamePrefix("fix-exec-");
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(180);
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        logger.info("初始化修复任务执行器: 核心线程={}, 最大线程={}, 队列容量={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), 30);
        
        return executor;
    }
} 