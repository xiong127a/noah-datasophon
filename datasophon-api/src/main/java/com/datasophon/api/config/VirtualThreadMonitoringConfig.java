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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 虚拟线程监控配置类
 * 提供虚拟线程的监控、健康检查和指标收集功能
 * 
 * @author datasophon
 */
@Slf4j
@Configuration
public class VirtualThreadMonitoringConfig {

    private final AtomicLong virtualThreadCount = new AtomicLong(0);
    private final AtomicLong platformThreadCount = new AtomicLong(0);
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 虚拟线程健康检查指示器
     */
    @Bean
    public HealthIndicator virtualThreadHealthIndicator() {
        return () -> {
            long activeThreads = threadMXBean.getThreadCount();
            long peakThreads = threadMXBean.getPeakThreadCount();
            
            var details = Map.of(
                "virtualThreadsSupported", true,
                "activeThreads", activeThreads,
                "peakThreads", peakThreads,
                "virtualThreadCount", virtualThreadCount.get(),
                "platformThreadCount", platformThreadCount.get()
            );
            
            return activeThreads < 10000 
                ? Health.up().withDetails(details).build()
                : Health.down().withDetails(details).build();
        };
    }

    /**
     * 虚拟线程信息贡献者
     */
    @Bean
    public InfoContributor virtualThreadInfoContributor() {
        return builder -> {
            var jvmInfo = Map.of(
                "version", System.getProperty("java.version"),
                "vendor", System.getProperty("java.vendor"),
                "runtime", System.getProperty("java.runtime.name")
            );
            
            var threadInfo = Map.of(
                "virtualThreadsSupported", true,
                "availableProcessors", Runtime.getRuntime().availableProcessors(),
                "maxMemory", Runtime.getRuntime().maxMemory(),
                "totalMemory", Runtime.getRuntime().totalMemory(),
                "freeMemory", Runtime.getRuntime().freeMemory()
            );
            
            builder.withDetail("jvm", jvmInfo);
            builder.withDetail("threading", threadInfo);
        };
    }

    /**
     * 注册虚拟线程指标
     */
    @Bean
    public Gauge virtualThreadGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("virtual.threads.active", this, VirtualThreadMonitoringConfig::getVirtualThreadCount)
            .description("当前活跃的虚拟线程数量")
            .register(meterRegistry);
    }

    @Bean
    public Gauge platformThreadGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("platform.threads.active", this, VirtualThreadMonitoringConfig::getPlatformThreadCount)
            .description("当前活跃的平台线程数量")
            .register(meterRegistry);
    }

    /**
     * 定期更新线程统计信息
     */
    @Scheduled(fixedRate = 5000) // 每5秒更新一次
    public void updateThreadStatistics() {
        try {
            // 获取所有线程信息
            var allThreads = Thread.getAllStackTraces().keySet();
            
            long virtualCount = allThreads.stream()
                .mapToLong(thread -> thread.isVirtual() ? 1L : 0L)
                .sum();
                
            long platformCount = allThreads.size() - virtualCount;
            
            virtualThreadCount.set(virtualCount);
            platformThreadCount.set(platformCount);
            
            if (log.isDebugEnabled()) {
                log.debug("线程统计更新 - 虚拟线程: {}, 平台线程: {}", virtualCount, platformCount);
            }
            
        } catch (Exception e) {
            log.warn("更新线程统计信息时发生错误", e);
        }
    }



    // Getter方法供指标使用
    public double getVirtualThreadCount() {
        return virtualThreadCount.get();
    }

    public double getPlatformThreadCount() {
        return platformThreadCount.get();
    }
}