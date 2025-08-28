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
import com.datasophon.api.service.impl.HostValidationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 主机验证监控控制器
 * 提供SSH连接池和插件系统的监控接口
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@RestController
@ApiVersion(path = "host-validation/monitor")
@Slf4j
public class HostValidationMonitorController {
    
    @Autowired
    private HostValidationServiceImpl hostValidationService;
    
    /**
     * 获取SSH连接池状态
     */
    @GetMapping("/ssh-pool-stats")
    public Result<Map<String, Object>> getSshPoolStats() {
        try {
            Map<String, Object> stats = hostValidationService.getSshPoolStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取SSH连接池状态失败", e);
            return Result.error("获取SSH连接池状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取插件使用统计
     */
    @GetMapping("/plugin-stats")
    public Result<Map<String, Object>> getPluginStats() {
        try {
            Map<String, Object> stats = hostValidationService.getPluginStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取插件统计失败", e);
            return Result.error("获取插件统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 强制清理闲置插件
     */
    @PostMapping("/cleanup-idle-plugins")
    public Result<Integer> cleanupIdlePlugins() {
        try {
            int cleanupCount = hostValidationService.cleanupIdlePlugins();
            log.info("手动清理闲置插件完成，清理数量: {}", cleanupCount);
            return Result.success(cleanupCount);
        } catch (Exception e) {
            log.error("清理闲置插件失败", e);
            return Result.error("清理闲置插件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取完整的监控概览
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getMonitorOverview() {
        try {
            Map<String, Object> overview = new java.util.HashMap<>();
            
            // SSH连接池状态
            overview.put("sshPool", hostValidationService.getSshPoolStats());
            
            // 插件状态
            overview.put("plugins", hostValidationService.getPluginStats());
            
            // 系统信息
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> systemInfo = new java.util.HashMap<>();
            systemInfo.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
            systemInfo.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
            systemInfo.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
            systemInfo.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
            systemInfo.put("availableProcessors", runtime.availableProcessors());
            overview.put("system", systemInfo);
            
            return Result.success(overview);
            
        } catch (Exception e) {
            log.error("获取监控概览失败", e);
            return Result.error("获取监控概览失败: " + e.getMessage());
        }
    }
}
