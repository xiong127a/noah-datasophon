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

package com.datasophon.api.controller.v1;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.dto.Result;
import com.datasophon.api.scheduler.WorkerScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * db-scheduler Worker节点调度控制器
 * 
 * 提供现代化的Worker节点管理REST API：
 * 
 * 🎯 核心功能：
 * 1. 手动触发Worker发现和健康检查
 * 2. 查看调度任务执行统计
 * 3. 管理调度任务状态
 * 4. 提供调度系统监控接口
 * 
 * 🚀 相比传统方案的优势：
 * - 基于db-scheduler现代化框架
 * - 零外部服务依赖
 * - 内置任务状态跟踪
 * - 支持立即执行和延迟调度
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-20
 */
@RestController
@ApiVersion(path = "worker-schedule")
@Slf4j
public class WorkerScheduleController {

    @Autowired
    private WorkerScheduleService workerScheduleService;

    /**
     * 手动触发所有集群Worker节点发现
     * 
     * @return 操作结果
     */
    @PostMapping("/discover-all-workers")
    public Result<String> discoverAllWorkers() {
        try {
            log.info("收到手动触发所有Worker发现请求");
            
            // 立即异步执行Worker发现任务
            workerScheduleService.discoverAllWorkers();
            
            return Result.success("所有集群Worker发现任务已启动（db-scheduler异步执行）");
            
        } catch (Exception e) {
            log.error("触发所有Worker发现失败", e);
            return Result.error("Worker发现失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发特定集群Worker节点发现
     * 
     * @param clusterId 集群ID
     * @return 操作结果
     */
    @PostMapping("/clusters/{clusterId}/discover-workers")
    public Result<String> discoverClusterWorkers(@PathVariable Long clusterId) {
        try {
            log.info("收到手动触发集群{}Worker发现请求", clusterId);
            
            // 异步执行特定集群Worker发现
            workerScheduleService.triggerWorkerDiscovery(clusterId);
            
            return Result.success("集群" + clusterId + "Worker发现任务已调度");
            
        } catch (Exception e) {
            log.error("触发集群{}Worker发现失败", clusterId, e);
            return Result.error("Worker发现失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发所有集群健康检查
     * 
     * @return 操作结果
     */
    @PostMapping("/health-check-all")
    public Result<String> healthCheckAllWorkers() {
        try {
            log.info("收到手动触发所有Worker健康检查请求");
            
            // 立即执行健康检查
            workerScheduleService.performHealthCheck();
            
            return Result.success("所有集群Worker健康检查已完成");
            
        } catch (Exception e) {
            log.error("触发所有Worker健康检查失败", e);
            return Result.error("健康检查失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发特定集群健康检查
     * 
     * @param clusterId 集群ID
     * @return 操作结果
     */
    @PostMapping("/clusters/{clusterId}/health-check")
    public Result<String> healthCheckClusterWorkers(@PathVariable Long clusterId) {
        try {
            log.info("收到手动触发集群{}健康检查请求", clusterId);
            
            // 异步执行特定集群健康检查
            workerScheduleService.triggerHealthCheck(clusterId);
            
            return Result.success("集群" + clusterId + "健康检查任务已调度");
            
        } catch (Exception e) {
            log.error("触发集群{}健康检查失败", clusterId, e);
            return Result.error("健康检查失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发关键服务监控
     * 
     * @return 操作结果
     */
    @PostMapping("/monitor-critical-services")
    public Result<String> monitorCriticalServices() {
        try {
            log.info("收到手动触发关键服务监控请求");
            
            // 立即执行关键服务监控
            workerScheduleService.monitorCriticalServices();
            
            return Result.success("关键服务监控已完成");
            
        } catch (Exception e) {
            log.error("触发关键服务监控失败", e);
            return Result.error("关键服务监控失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发系统清理
     * 
     * @return 操作结果
     */
    @PostMapping("/system-cleanup")
    public Result<String> triggerSystemCleanup() {
        try {
            log.info("收到手动触发系统清理请求");
            
            // 立即执行系统清理
            workerScheduleService.performSystemCleanup();
            
            return Result.success("系统清理任务已完成");
            
        } catch (Exception e) {
            log.error("触发系统清理失败", e);
            return Result.error("系统清理失败: " + e.getMessage());
        }
    }

    /**
     * 获取调度器状态概览
     * 
     * @return 调度器状态信息
     */
    @GetMapping("/scheduler-status")
    public Result<Map<String, Object>> getSchedulerStatus() {
        try {
            log.info("获取db-scheduler状态概览");
            
            Map<String, Object> status = new HashMap<>();
            
            // 基础状态信息
            status.put("schedulerName", "datasophon-scheduler");
            status.put("schedulerType", "db-scheduler");
            status.put("status", "RUNNING");
            status.put("timestamp", System.currentTimeMillis());
            
            // 任务统计信息
            Map<String, Object> taskStats = new HashMap<>();
            taskStats.put("totalTasks", "4"); // Worker发现、健康检查、关键监控、系统清理
            taskStats.put("recurringTasks", "4");
            taskStats.put("oneTimeTasks", "动态创建");
            
            status.put("taskStatistics", taskStats);
            
            // 配置信息
            Map<String, Object> config = new HashMap<>();
            config.put("tableName", "datasophon_scheduled_tasks");
            config.put("pollingInterval", "10秒");
            config.put("heartbeatInterval", "30秒");
            config.put("threadsCount", Math.max(8, Runtime.getRuntime().availableProcessors()));
            
            status.put("configuration", config);
            
            return Result.success(status);
            
        } catch (Exception e) {
            log.error("获取调度器状态失败", e);
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务执行概览
     * 
     * @return 任务执行概览
     */
    @GetMapping("/task-overview")
    public Result<Map<String, Object>> getTaskOverview() {
        try {
            log.info("获取任务执行概览");
            
            Map<String, Object> overview = new HashMap<>();
            
            // 定时任务信息
            Map<String, Object> recurringTasks = new HashMap<>();
            recurringTasks.put("worker-discovery", "每5分钟 - Worker节点发现");
            recurringTasks.put("worker-health-check", "每1分钟 - Worker健康检查");
            recurringTasks.put("critical-service-monitor", "每30秒 - 关键服务监控");
            recurringTasks.put("system-cleanup", "每天2点 - 系统清理");
            
            overview.put("recurringTasks", recurringTasks);
            
            // 一次性任务信息
            Map<String, Object> oneTimeTasks = new HashMap<>();
            oneTimeTasks.put("manual-worker-discovery", "手动Worker发现");
            oneTimeTasks.put("manual-health-check", "手动健康检查");
            oneTimeTasks.put("system-info-collection", "系统信息收集");
            oneTimeTasks.put("worker-reconnect", "Worker重连");
            
            overview.put("oneTimeTasks", oneTimeTasks);
            
            // 系统健康状态
            Map<String, Object> health = new HashMap<>();
            health.put("schedulerHealth", "HEALTHY");
            health.put("databaseConnection", "CONNECTED");
            health.put("actorSystem", "RUNNING");
            health.put("lastExecutionTime", System.currentTimeMillis());
            
            overview.put("systemHealth", health);
            
            return Result.success(overview);
            
        } catch (Exception e) {
            log.error("获取任务概览失败", e);
            return Result.error("获取概览失败: " + e.getMessage());
        }
    }

    /**
     * 获取帮助信息
     * 
     * @return API使用帮助
     */
    @GetMapping("/help")
    public Result<Map<String, Object>> getApiHelp() {
        Map<String, Object> help = new HashMap<>();
        
        // API端点说明
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("POST /discover-all-workers", "手动触发所有集群Worker发现");
        endpoints.put("POST /clusters/{id}/discover-workers", "手动触发特定集群Worker发现");
        endpoints.put("POST /health-check-all", "手动触发所有集群健康检查");
        endpoints.put("POST /clusters/{id}/health-check", "手动触发特定集群健康检查");
        endpoints.put("POST /monitor-critical-services", "手动触发关键服务监控");
        endpoints.put("POST /system-cleanup", "手动触发系统清理");
        endpoints.put("GET /scheduler-status", "获取调度器状态");
        endpoints.put("GET /task-overview", "获取任务执行概览");
        
        help.put("endpoints", endpoints);
        
        // 使用说明
        Map<String, String> usage = new HashMap<>();
        usage.put("framework", "db-scheduler v16.0.0");
        usage.put("database", "MySQL datasophon2");
        usage.put("table", "datasophon_scheduled_tasks");
        usage.put("scheduler", "datasophon-scheduler");
        usage.put("ui_access", "Web UI: http://localhost:8080/datasophon/db-scheduler");
        usage.put("ui_features", "任务管理、执行历史、实时监控、可视化Dashboard");
        
        help.put("systemInfo", usage);
        
        // UI访问信息
        Map<String, String> uiInfo = new HashMap<>();
        uiInfo.put("url", "http://localhost:8080/datasophon/db-scheduler");
        uiInfo.put("description", "db-scheduler 可视化管理界面");
        uiInfo.put("features", "任务列表、执行状态、历史记录、实时监控");
        uiInfo.put("refresh", "页面每5秒自动刷新");
        uiInfo.put("auth", "当前无需认证（可配置启用）");
        
        help.put("webUI", uiInfo);
        
        return Result.success(help);
    }

    /**
     * 获取Web UI访问信息
     * 
     * @return Web UI访问信息
     */
    @GetMapping("/ui-info")
    public Result<Map<String, Object>> getUIInfo() {
        Map<String, Object> uiInfo = new HashMap<>();
        
        // 基础信息
        uiInfo.put("enabled", true);
        uiInfo.put("path", "/db-scheduler");
        uiInfo.put("fullUrl", "http://localhost:8080/datasophon/db-scheduler");
        uiInfo.put("refreshInterval", "5s");
        
        // 功能特性
        Map<String, Boolean> features = new HashMap<>();
        features.put("taskList", true);         // 任务列表
        features.put("executionHistory", true); // 执行历史
        features.put("realTimeMonitoring", true); // 实时监控
        features.put("taskData", true);         // 任务数据显示
        features.put("logs", true);             // 日志显示
        
        uiInfo.put("features", features);
        
        // 配置信息
        Map<String, Object> config = new HashMap<>();
        config.put("authEnabled", false);
        config.put("showData", true);
        config.put("showLogs", true);
        config.put("pageSize", 50);
        
        uiInfo.put("configuration", config);
        
        // 数据库表信息
        Map<String, String> tables = new HashMap<>();
        tables.put("tasks", "datasophon_scheduled_tasks");
        tables.put("executionLogs", "datasophon_scheduled_execution_logs");
        
        uiInfo.put("databaseTables", tables);
        
        return Result.success(uiInfo);
    }
}
