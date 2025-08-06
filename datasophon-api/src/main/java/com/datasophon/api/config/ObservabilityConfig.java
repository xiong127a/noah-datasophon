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

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.info.InfoContributor;


import java.util.Map;

/**
 * 观测性配置类
 * 配置Spring Boot 3.5的观测性功能，包括指标、追踪和健康检查
 * 
 * @author datasophon
 */
@Configuration
public class ObservabilityConfig {

    /**
     * 配置定时方面支持
     * 启用@Timed注解功能
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * 自定义指标注册器
     * 添加应用程序特定的标签
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "datasophon")
            .commonTags("version", "3.0.0")
            .commonTags("java.version", "21")
            .commonTags("spring.boot.version", "3.5.4");
    }

    /**
     * 自定义应用信息贡献者
     * 使用JDK 21的现代化特性提供应用信息
     */
    @Bean
    public InfoContributor customInfoContributor() {
        return builder -> {
            var systemInfo = Map.of(
                "java.version", System.getProperty("java.version"),
                "java.vendor", System.getProperty("java.vendor"),
                "os.name", System.getProperty("os.name"),
                "os.version", System.getProperty("os.version"),
                "virtual.threads.supported", true
            );
            
            var applicationInfo = Map.of(
                "name", "DataSophon Platform",
                "version", "3.0.0",
                "description", "Big Data Cloud Native Platform",
                "features", Map.of(
                    "virtual-threads", true,
                    "modern-java", true,
                    "spring-boot-3.5", true,
                    "kubernetes-native", true
                )
            );
            
            builder.withDetail("system", systemInfo);
            builder.withDetail("application", applicationInfo);
        };
    }


}