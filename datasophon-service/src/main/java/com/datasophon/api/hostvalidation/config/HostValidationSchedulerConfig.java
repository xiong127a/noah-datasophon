/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.hostvalidation.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 主机校验调度器配置
 * 支持虚拟线程和传统线程池
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "datasophon.checker.scheduler.enabled", 
    havingValue = "true", 
    matchIfMissing = true  // 默认启用
)
public class HostValidationSchedulerConfig {
    
    private final DataSource dataSource;
    private final Environment environment;
    
    @Bean
    public Scheduler hostValidationScheduler() {
        
        return Scheduler
            .create(dataSource) // 只创建基础Scheduler，Task稍后注册
            .serializer(new JacksonSerializer()) // 使用Jackson序列化
            .executorService(createExecutorService()) // 使用虚拟线程执行器
            .pollingInterval(getPollingInterval()) // 轮询间隔
            .heartbeatInterval(getHeartbeatInterval()) // 心跳间隔
            .enableImmediateExecution() // 启用立即执行
            .build();
    }
    
    /**
     * 创建虚拟线程执行器服务
     * 统一使用虚拟线程提升性能
     */
    private ExecutorService createExecutorService() {
        log.info("db-scheduler使用虚拟线程执行器");
        
        // 使用虚拟线程工厂创建执行器
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
            .name("host-validation-execute-task-", 1)
            .factory();
            
        return Executors.newThreadPerTaskExecutor(virtualThreadFactory);
    }
    
    /**
     * 获取线程数配置
     */
    private int getThreadCount() {
        return environment.getProperty("datasophon.checker.scheduler.threads", Integer.class, 10);
    }
    
    /**
     * 获取轮询间隔
     */
    private Duration getPollingInterval() {
        int seconds = environment.getProperty("datasophon.checker.scheduler.polling-interval-seconds", Integer.class, 10);
        return Duration.ofSeconds(seconds);
    }
    
    /**
     * 获取心跳间隔
     */
    private Duration getHeartbeatInterval() {
        int minutes = environment.getProperty("datasophon.checker.scheduler.heartbeat-interval-minutes", Integer.class, 5);
        return Duration.ofMinutes(minutes);
    }
}
