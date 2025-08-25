package com.datasophon.api.workflow.config;


import com.datasophon.api.workflow.activity.HostCheckActivities;
import com.datasophon.api.workflow.impl.SingleHostCheckWorkflowImpl;
import com.datasophon.api.workflow.impl.BatchHostCheckWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import jakarta.annotation.PreDestroy;

/**
 * Temporal工作流配置类
 * 
 * @author DataSophon Team
 */
@Configuration
@ConditionalOnProperty(
    name = "datasophon.temporal.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
@Lazy
@Slf4j
public class TemporalConfiguration {
    
    @Value("${datasophon.temporal.server.host:localhost}")
    private String temporalHost;
    
    @Value("${datasophon.temporal.server.port:7233}")
    private int temporalPort;
    
    @Value("${datasophon.temporal.namespace:default}")
    private String namespace;
    
    @Value("${datasophon.temporal.task-queue:host-check-task-queue}")
    private String taskQueue;
    
    @Value("${datasophon.temporal.worker.max-concurrent-workflow-task-pollers:5}")
    private int maxConcurrentWorkflowTaskPollers;
    
    @Value("${datasophon.temporal.worker.max-concurrent-activity-task-pollers:10}")
    private int maxConcurrentActivityTaskPollers;
    
    private WorkerFactory workerFactory;
    
    /**
     * 配置Temporal服务存根
     */
    @Bean
    @Lazy
    public WorkflowServiceStubs workflowServiceStubs() {
        log.info("配置Temporal服务连接: {}:{}", temporalHost, temporalPort);
        
        WorkflowServiceStubsOptions.Builder optionsBuilder = WorkflowServiceStubsOptions.newBuilder();
        
        // 如果不是本地开发环境，配置服务端地址
        if (!"localhost".equals(temporalHost)) {
            optionsBuilder.setTarget(temporalHost + ":" + temporalPort);
        }
        
        return WorkflowServiceStubs.newServiceStubs(optionsBuilder.build());
    }
    
    /**
     * 配置Temporal工作流客户端
     */
    @Bean
    @Lazy
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        log.info("配置Temporal工作流客户端，命名空间: {}", namespace);
        
        WorkflowClientOptions clientOptions = WorkflowClientOptions.newBuilder()
                .setNamespace(namespace)
                .build();
                
        return WorkflowClient.newInstance(workflowServiceStubs, clientOptions);
    }
    
    /**
     * 配置Worker工厂
     */
    @Bean
    @Lazy
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        log.info("配置Temporal Worker工厂");
        return WorkerFactory.newInstance(workflowClient);
    }
    
    /**
     * 配置主机检查Worker
     */
    @Bean
    @Lazy
    public Worker hostCheckWorker(WorkerFactory workerFactory, HostCheckActivities hostCheckActivities) {
        log.info("配置主机检查Worker，任务队列: {}", taskQueue);
        
        // 配置Worker选项
        WorkerOptions workerOptions = WorkerOptions.newBuilder()
                .setMaxConcurrentWorkflowTaskPollers(maxConcurrentWorkflowTaskPollers)
                .setMaxConcurrentActivityTaskPollers(maxConcurrentActivityTaskPollers)
                .build();
        
        // 创建Worker
        Worker worker = workerFactory.newWorker(taskQueue, workerOptions);
        
        // 注册工作流实现 - 拆分为两个独立的工作流
        worker.registerWorkflowImplementationTypes(
                SingleHostCheckWorkflowImpl.class,
                BatchHostCheckWorkflowImpl.class
        );
        
        // 注册活动实现
        worker.registerActivitiesImplementations(hostCheckActivities);
        
        log.info("主机检查Worker配置完成");
        return worker;
    }
    
    /**
     * 延迟启动Worker - 由需要使用的服务手动调用
     * 不再使用@PostConstruct自动启动，实现真正的延迟加载
     */
    public void startWorkersIfNeeded() {
        if (workerFactory != null) {
            log.info("检测到Temporal使用需求，启动Temporal Workers...");
            workerFactory.start();
            log.info("Temporal Workers启动完成");
        }
    }
    
    /**
     * 关闭Worker
     */
    @PreDestroy
    public void shutdownWorkers() {
        if (workerFactory != null) {
            log.info("正在关闭Temporal Workers...");
            workerFactory.shutdown();
            log.info("Temporal Workers已关闭");
        }
    }
}