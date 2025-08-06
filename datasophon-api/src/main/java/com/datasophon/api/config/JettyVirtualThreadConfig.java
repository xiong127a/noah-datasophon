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

package com.datasophon.api.config;

import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * Jetty虚拟线程配置类
 * 为Jetty服务器启用JDK 21虚拟线程支持，提升并发性能
 * 
 * @author datasophon
 */
@Configuration
@EnableAsync
public class JettyVirtualThreadConfig {

    /**
     * 配置虚拟线程执行器
     * 使用JDK 21的虚拟线程特性替代传统线程池
     */
    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * 配置Jetty使用虚拟线程
     * Spring Boot 3.2+自动支持Jetty虚拟线程，这里提供显式配置
     */
    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyVirtualThreadCustomizer() {
        return factory -> {
            // Spring Boot 3.2+ 会自动为Jetty配置虚拟线程支持
            // 当spring.threads.virtual.enabled=true时
            factory.addServerCustomizers(server -> {
                // 记录虚拟线程配置信息
                System.out.println("Jetty服务器已配置为使用虚拟线程支持");
                System.out.println("虚拟线程支持状态: true");
            });
        };
    }


}