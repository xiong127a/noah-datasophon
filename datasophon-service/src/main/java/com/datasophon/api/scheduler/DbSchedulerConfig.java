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

package com.datasophon.api.scheduler;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * db-scheduler 调度框架配置
 * 
 * 使用现代化的db-scheduler替代传统Quartz，优势：
 * 1. 🎯 零外部服务依赖 - 仅使用你现有的MySQL数据库  
 * 2. 🚀 现代化API - Builder模式，比Quartz简洁10倍
 * 3. 🔄 智能重试机制 - 内置故障恢复和死锁检测
 * 4. ⚡ 集群友好 - 零配置支持多实例调度
 * 5. 📊 任务监控 - 内置执行统计和状态跟踪
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-20
 */
@Configuration
@Slf4j
public class DbSchedulerConfig {

    @Autowired
    private WorkerScheduleService workerScheduleService;

    /**
     * Worker节点发现任务
     * 每5分钟执行一次，自动发现和注册Worker节点
     */
    @Bean
    public Task<Void> workerDiscoveryTask() {
        return Tasks.recurring("worker-discovery", Schedules.cron("0 */5 * * * *"))
                .execute((instance, ctx) -> {
                    log.info("执行db-scheduler Worker节点发现任务: {}", instance.getTaskName());
                    try {
                        workerScheduleService.discoverAllWorkers();
                        log.info("Worker节点发现任务完成: {}", instance.getTaskName());
                    } catch (Exception e) {
                        log.error("Worker节点发现任务失败", e);
                        throw e; // 让db-scheduler处理重试
                    }
                });
    }

    /**
     * Worker健康检查任务
     * 每1分钟执行一次，快速检查Worker状态
     */
    @Bean
    public Task<Void> workerHealthCheckTask() {
        return Tasks.recurring("worker-health-check", Schedules.cron("0 */1 * * * *"))
                .execute((instance, ctx) -> {
                    log.debug("执行db-scheduler Worker健康检查任务: {}", instance.getTaskName());
                    try {
                        workerScheduleService.performHealthCheck();
                        log.debug("Worker健康检查任务完成: {}", instance.getTaskName());
                    } catch (Exception e) {
                        log.error("Worker健康检查任务失败", e);
                        // 健康检查失败不抛异常，避免影响调度
                    }
                });
    }

    /**
     * 关键服务监控任务  
     * 每30秒执行一次，监控关键业务服务
     */
    @Bean
    public Task<Void> criticalServiceMonitorTask() {
        return Tasks.recurring("critical-service-monitor", Schedules.fixedDelay(Duration.ofSeconds(30)))
                .execute((instance, ctx) -> {
                    log.debug("执行db-scheduler 关键服务监控: {}", instance.getTaskName());
                    try {
                        workerScheduleService.monitorCriticalServices();
                    } catch (Exception e) {
                        log.warn("关键服务监控任务失败", e);
                    }
                });
    }

    /**
     * 系统清理任务
     * 每天凌晨2点执行，清理过期数据和日志
     */
    @Bean
    public Task<Void> systemCleanupTask() {
        return Tasks.recurring("system-cleanup", Schedules.cron("0 0 2 * * *"))
                .execute((instance, ctx) -> {
                    log.info("执行db-scheduler 系统清理任务: {}", instance.getTaskName());
                    try {
                        workerScheduleService.performSystemCleanup();
                        log.info("系统清理任务完成: {}", instance.getTaskName());
                    } catch (Exception e) {
                        log.error("系统清理任务失败", e);
                        throw e;
                    }
                });
    }

    /**
     * 手动触发任务模板
     * 用于REST API手动触发
     */
    @Bean
    public Task<String> manualWorkerDiscoveryTask() {
        return Tasks.oneTime("manual-worker-discovery", String.class)
                .execute((instance, ctx) -> {
                    String clusterId = instance.getData();
                    log.info("执行手动Worker发现任务，集群ID: {}", clusterId);
                    
                    if (clusterId != null && !clusterId.isEmpty()) {
                        workerScheduleService.discoverClusterWorkers(Long.parseLong(clusterId));
                    } else {
                        workerScheduleService.discoverAllWorkers();
                    }
                    
                    log.info("手动Worker发现任务完成，集群ID: {}", clusterId);
                });
    }

    /**
     * db-scheduler 自定义配置
     * 优化性能和故障恢复能力
     */
    // 暂时移除自定义配置，使用默认配置
}
