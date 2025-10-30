/*
 *
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
 *
 */

package com.datasophon.worker.http;

import com.datasophon.common.command.BaseCommand;
import com.datasophon.common.command.SystemInfoResult;
import com.datasophon.common.utils.HardwareInfoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Worker任务控制器
 * 提供HTTP REST API和SSE接口
 * 
 * 单向通讯设计：
 * - POST /api/tasks: Master提交任务，Worker立即返回taskId
 * - GET /api/tasks/{taskId}/events: Master建立SSE长连接订阅任务事件
 * - Worker通过已建立的SSE连接推送任务状态、日志、结果
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    private final TaskExecutor taskExecutor;
    private final SseEmitterManager sseManager;

    public TaskController(TaskExecutor taskExecutor, SseEmitterManager sseManager) {
        this.taskExecutor = taskExecutor;
        this.sseManager = sseManager;
    }

    /**
     * 提交任务
     * Master通过POST请求提交命令，Worker立即返回taskId
     */
    @PostMapping("/tasks")
    public ResponseEntity<Map<String, Object>> submitTask(@RequestBody BaseCommand command) {
        try {
            logger.info("Received task submission: {}", command.getClass().getSimpleName());
            
            // 提交任务
            String taskId = taskExecutor.submitTask(command);
            
            // 返回任务ID
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("status", "PENDING");
            response.put("message", "Task submitted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to submit task", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to submit task: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查询任务状态
     * Master可以查询任务当前状态
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        TaskInfo taskInfo = taskExecutor.getTask(taskId);
        
        if (taskInfo == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Task not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("taskId", taskInfo.getTaskId());
        response.put("status", taskInfo.getStatus());
        response.put("createTime", taskInfo.getCreateTime());
        response.put("startTime", taskInfo.getStartTime());
        response.put("completeTime", taskInfo.getCompleteTime());
        
        if ("COMPLETED".equals(taskInfo.getStatus())) {
            response.put("result", taskInfo.getResult());
        }
        
        if ("FAILED".equals(taskInfo.getStatus())) {
            response.put("errorMessage", taskInfo.getErrorMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 订阅任务事件流 (SSE)
     * Master主动建立SSE长连接，Worker通过此连接推送事件
     */
    @GetMapping(value = "/tasks/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeTaskEvents(@PathVariable String taskId) {
        logger.info("Master subscribed to task events: {}", taskId);
        
        SseEmitter emitter = sseManager.createEmitter();
        sseManager.addTaskSubscription(taskId, emitter);
        
        return emitter;
    }

    /**
     * 取消任务
     */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        boolean cancelled = taskExecutor.cancelTask(taskId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", cancelled);
        
        if (cancelled) {
            response.put("message", "Task cancelled successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Task not found or already completed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * 健康检查
     * 替代原来的Pekko Ping，Master用此接口检查Worker是否可用
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "datasophon-worker");
        
        try {
            response.put("hostname", InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            logger.warn("Failed to get hostname", e);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取Worker系统信息
     * 替代原来的Pekko SystemInfoActor，Master用此接口获取硬件信息
     * 使用 OSHI 库获取准确的跨平台硬件信息
     */
    @GetMapping("/info")
    public ResponseEntity<SystemInfoResult> getSystemInfo() {
        try {
            SystemInfoResult systemInfo = new SystemInfoResult();
            
            // 获取主机名和IP地址
            String hostname = InetAddress.getLocalHost().getHostName();
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            systemInfo.setHostname(hostname);
            systemInfo.setIpAddress(ipAddress);
            
            // 获取CPU架构（使用OSHI）
            String cpuArchitecture = HardwareInfoUtils.getCpuArchitecture();
            systemInfo.setCpuArchitecture(cpuArchitecture);
            
            // 获取CPU核心数（使用OSHI）
            int cpuCores = HardwareInfoUtils.getCpuCores();
            systemInfo.setCpuCores(cpuCores);
            
            // 获取操作系统信息（使用OSHI）
            String osInfo = HardwareInfoUtils.getOsInfo();
            systemInfo.setOsVersion(osInfo);
            
            // 获取系统物理内存信息（使用OSHI）
            String memoryInfo = HardwareInfoUtils.getMemoryInfoString();
            systemInfo.setMemoryInfo(memoryInfo);
            
            // 获取磁盘信息（使用OSHI）
            String diskInfo = HardwareInfoUtils.getDiskInfoString();
            systemInfo.setDiskInfo(diskInfo);
            
            // 获取系统负载（使用OSHI）
            String systemLoad = HardwareInfoUtils.getSystemLoad();
            systemInfo.setSystemLoad(systemLoad);
            
            systemInfo.setExecResult(true);
            logger.info("System info retrieved: Host={}, CPU={}cores, Arch={}", hostname, cpuCores, cpuArchitecture);
            return ResponseEntity.ok(systemInfo);
            
        } catch (Exception e) {
            logger.error("Failed to collect system info", e);
            SystemInfoResult errorResult = new SystemInfoResult();
            errorResult.setExecResult(false);
            errorResult.setExecOut("Failed to collect system info: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Ping接口
     * 简单快速的连接测试
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}

