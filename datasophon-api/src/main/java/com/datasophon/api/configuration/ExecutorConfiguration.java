package com.datasophon.api.configuration;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Configuration;

/**
 * 执行器配置
 * 定义各种线程池执行器
 * 
 * 注意：
 * 1. 所有原 @Async("taskExecutor") 异步方法已改为同步执行
 * 2. 真正的异步由 db-scheduler 框架和 DAG 调度器保证
 * 3. 如需异步任务，使用 AsyncTaskScheduler 提交到 db-scheduler
 * 
 * @author DataSophon Team
 */
@Configuration
public class ExecutorConfiguration {

    // 所有原使用 @Async 的方法已改为同步执行
    // 真正的异步由 db-scheduler 和 DAG 调度器保证

    /**
     * 创建检查任务执行器
     * 
     * @return 检查任务执行器
     */
    // @Bean(name = "checkExecutor")
    // public ExecutorService checkExecutor() {
    //     return createThreadPool("check-executor", 20);
    // }

    /**
     * 创建修复任务执行器
     * 
     * @return 修复任务执行器
     */
    // @Bean(name = "fixExecutor")
    // public ExecutorService fixExecutor() {
    //     return createThreadPool("fix-executor", 10);
    // }

    /**
     * 创建操作系统信息执行器
     * 
     * @return 操作系统信息执行器
     */
    // @Bean(name = "osInfoExecutor")
    // public ExecutorService osInfoExecutor() {
    //     return createThreadPool("os-info-executor", 20);
    // }

    /**
     * 创建硬件信息执行器
     * 
     * @return 硬件信息执行器
     */
    // @Bean(name = "hardwareInfoExecutor")
    // public ExecutorService hardwareInfoExecutor() {
    //     return createThreadPool("hardware-info-executor", 10);
    // }

    /**
     * 创建hosts文件执行器
     * 
     * @return hosts文件执行器
     */
    // @Bean(name = "hostsFileExecutor")
    // public ExecutorService hostsFileExecutor() {
    //     return createThreadPool("hosts-file-executor", 5);
    // }

    /**
     * 创建主机名执行器
     * 
     * @return 主机名执行器
     */
    // @Bean(name = "hostnameExecutor")
    // public ExecutorService hostnameExecutor() {
    //     return createThreadPool("hostname-executor", 5);
    // }

    /**
     * 示例：如需自定义线程池，可参考以下模板
     * 
     * @Bean(name = "customExecutor")
     * public ExecutorService customExecutor() {
     *     return new ThreadPoolExecutor(
     *         10,  // 核心线程数
     *         20,  // 最大线程数
     *         60L, TimeUnit.SECONDS,
     *         new LinkedBlockingQueue<>(1000),
     *         new NamedThreadFactory("custom"));
     * }
     */

    /**
     * 命名线程工厂（保留供未来使用）
     * 为线程池中的线程提供有意义的名称
     */
    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix + "-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    }
}