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

import com.datasophon.api.hostvalidation.scheduler.HostValidationSchedulerService;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.serializer.jackson.JacksonSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * 主机校验调度器配置
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "datasophon.host-validation.scheduler.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class HostValidationSchedulerConfig {
    
    private final DataSource dataSource;
    private final Environment environment;
    
    @Bean
    public Scheduler hostValidationScheduler(HostValidationSchedulerService schedulerService) {
        
        // 获取应用实例名称
        String instanceId = environment.getProperty("spring.application.name", "datasophon-api");
        String nodeId = environment.getProperty("server.port", "8081");
        SchedulerName schedulerName = SchedulerName.of(instanceId + "-" + nodeId);
        
        return Scheduler
            .create(dataSource, schedulerService.hostValidationTask, schedulerService.hostRepairTask, schedulerService.hostCleanupTask)
            .schedulerName(schedulerName)
            .serializer(new JacksonSerializer()) // 使用Jackson序列化
            .threads(getThreadCount()) // 线程数配置
            .pollingInterval(getPollingInterval()) // 轮询间隔
            .heartbeatInterval(getHeartbeatInterval()) // 心跳间隔
            .enableImmediateExecution() // 启用立即执行
            .failureLogging(true) // 启用失败日志
            .build();
    }
    
    /**
     * 获取线程数配置
     */
    private int getThreadCount() {
        return environment.getProperty("datasophon.host-validation.scheduler.threads", Integer.class, 10);
    }
    
    /**
     * 获取轮询间隔
     */
    private Duration getPollingInterval() {
        int seconds = environment.getProperty("datasophon.host-validation.scheduler.polling-interval-seconds", Integer.class, 10);
        return Duration.ofSeconds(seconds);
    }
    
    /**
     * 获取心跳间隔
     */
    private Duration getHeartbeatInterval() {
        int minutes = environment.getProperty("datasophon.host-validation.scheduler.heartbeat-interval-minutes", Integer.class, 5);
        return Duration.ofMinutes(minutes);
    }
}
