package com.datasophon.api.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 执行器配置
 * 定义各种线程池执行器
 * 
 * 注意：临时注释掉所有executor bean，避免Spring启动时的多bean冲突问题
 * 这些线程池后续需要重新设计和实现
 */
@Configuration
public class ExecutorConfiguration {

    // 临时注释掉所有executor bean以解决启动冲突问题
    // 后续需要重新设计这些线程池的依赖注入方式

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
     * 创建指定名称和大小的线程池
     * 
     * @param namePrefix 线程名前缀
     * @param poolSize   线程池大小
     * @return 线程池
     */
    private ExecutorService createThreadPool(String namePrefix, int poolSize) {
        return new ThreadPoolExecutor(
                poolSize, // 核心线程数
                poolSize * 2, // 最大线程数
                60L, // 空闲线程保留时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(1000), // 工作队列
                new NamedThreadFactory(namePrefix)); // 线程工厂
    }

    /**
     * 命名线程工厂
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
            // 创建线程时使用前缀+序号的命名方式，业务代码可以在运行时修改线程名称添加主机信息
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (thread.isDaemon()) {
                thread.setDaemon(false); // 确保不是守护线程
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY); // 设置正常优先级
            }
            return thread;
        }
    }
}