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

import com.datasophon.api.hostvalidation.manager.HostValidationStateManager;
import com.datasophon.api.hostvalidation.service.HostValidationService;
import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.utils.Result;
import com.datasophon.common.vo.HostValidationStatusVO;
import com.datasophon.common.annotation.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 主机校验操作控制器
 * 负责校验任务的启动、停止、暂停等操作（普通HTTP接口）
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@ApiVersion(path = "host-validation")
@RequiredArgsConstructor
@Tag(name = "主机校验操作", description = "主机校验与修复操作接口")
public class HostValidationController {
    
    private final HostValidationService hostValidationService;
    private final HostValidationStateManager stateManager;
    
    /**
     * 启动主机校验
     */
    @PostMapping("/start")
    @Operation(summary = "启动主机校验", description = "启动主机校验任务，包括SSH连接、信息收集和环境检查")
    public Result<String> startValidation(@RequestBody HostValidationRequestDTO request) {
        
        log.info("启动主机校验: clusterId={}, 主机数量={}", 
                request.clusterId(), request.hostIps().size());
        
        try {
            // 参数校验
            if (request.clusterId() == null || request.hostIps() == null || request.hostIps().isEmpty()) {
                return Result.error("集群ID和主机IP列表不能为空");
            }
            
            // 启动校验服务
            hostValidationService.startValidation(request);
            
            return Result.success("主机校验任务已启动，请通过SSE接收实时状态更新");
            
        } catch (Exception e) {
            log.error("启动主机校验失败: clusterId={}, error={}", 
                    request.clusterId(), e.getMessage(), e);
            return Result.error("启动主机校验失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前校验状态（快照）
     */
    @GetMapping("/status/{clusterId}")
    @Operation(summary = "获取校验状态", description = "获取指定集群的当前主机校验状态快照")
    public Result<List<HostValidationStatusVO>> getValidationStatus(
            @Parameter(description = "集群ID") @PathVariable Long clusterId) {
        
        try {
            List<HostValidationStatusVO> statuses = hostValidationService.getValidationStatus(clusterId);
            return Result.success(statuses);
            
        } catch (Exception e) {
            log.error("获取校验状态失败: clusterId={}, error={}", clusterId, e.getMessage(), e);
            return Result.error("获取校验状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 暂停主机校验
     */
    @PostMapping("/pause/{clusterId}")
    @Operation(summary = "暂停主机校验", description = "暂停指定主机或所有主机的校验任务")
    public Result<String> pauseValidation(
            @Parameter(description = "集群ID") @PathVariable Long clusterId,
            @Parameter(description = "主机IP，为空则暂停所有主机") @RequestParam(required = false) String hostIp) {
        
        log.info("暂停主机校验: clusterId={}, hostIp={}", clusterId, hostIp);
        
        try {
            hostValidationService.pauseValidation(clusterId, hostIp);
            String message = hostIp != null ? 
                String.format("已暂停主机 %s 的校验", hostIp) : 
                "已暂停所有主机的校验";
            return Result.success(message);
            
        } catch (Exception e) {
            log.error("暂停主机校验失败: clusterId={}, hostIp={}, error={}", 
                    clusterId, hostIp, e.getMessage(), e);
            return Result.error("暂停主机校验失败: " + e.getMessage());
        }
    }

    /**
     * 继续主机校验
     */
    @PostMapping("/resume/{clusterId}")
    @Operation(summary = "继续主机校验", description = "继续指定主机或所有主机的校验任务")
    public Result<String> resumeValidation(
            @Parameter(description = "集群ID") @PathVariable Long clusterId,
            @Parameter(description = "主机IP，为空则继续所有主机") @RequestParam(required = false) String hostIp) {
        
        log.info("继续主机校验: clusterId={}, hostIp={}", clusterId, hostIp);
        
        try {
            hostValidationService.resumeValidation(clusterId, hostIp);
            String message = hostIp != null ? 
                String.format("已继续主机 %s 的校验", hostIp) : 
                "已继续所有主机的校验";
            return Result.success(message);
            
        } catch (Exception e) {
            log.error("继续主机校验失败: clusterId={}, hostIp={}, error={}", 
                    clusterId, hostIp, e.getMessage(), e);
            return Result.error("继续主机校验失败: " + e.getMessage());
        }
    }

    /**
     * 停止校验任务
     */
    @PostMapping("/stop/{clusterId}")
    @Operation(summary = "停止校验任务", description = "停止正在进行的主机校验任务")
    public Result<String> stopValidation(
            @Parameter(description = "集群ID") @PathVariable Long clusterId,
            @Parameter(description = "主机IP，为空则停止所有主机") @RequestParam(required = false) String hostIp) {
        
        log.info("停止主机校验: clusterId={}, hostIp={}", clusterId, hostIp);
        
        try {
            if (hostIp != null) {
                // 停止单个主机
                hostValidationService.pauseValidation(clusterId, hostIp);
                return Result.success(String.format("已停止主机 %s 的校验", hostIp));
            } else {
                // 停止整个集群的校验
                hostValidationService.stopValidation(clusterId);
                return Result.success("主机校验任务已停止");
            }
            
        } catch (Exception e) {
            log.error("停止主机校验失败: clusterId={}, hostIp={}, error={}", 
                    clusterId, hostIp, e.getMessage(), e);
            return Result.error("停止主机校验失败: " + e.getMessage());
        }
    }

    /**
     * 重新检查指定项目
     */
    @PostMapping("/recheck/{clusterId}")
    @Operation(summary = "重新检查", description = "重新检查指定主机的特定检查项")
    public Result<String> recheckItem(
            @Parameter(description = "集群ID") @PathVariable Long clusterId,
            @Parameter(description = "主机IP") @RequestParam String hostIp,
            @Parameter(description = "检查类型") @RequestParam String checkType) {
        
        log.info("重新检查: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
        
        try {
            hostValidationService.recheckItem(clusterId, hostIp, checkType);
            return Result.success(String.format("已重新检查主机 %s 的 %s 项目", hostIp, checkType));
            
        } catch (Exception e) {
            log.error("重新检查失败: clusterId={}, hostIp={}, checkType={}, error={}", 
                    clusterId, hostIp, checkType, e.getMessage(), e);
            return Result.error("重新检查失败: " + e.getMessage());
        }
    }

    /**
     * 修复指定项目
     */
    @PostMapping("/repair/{clusterId}")
    @Operation(summary = "修复指定项目", description = "修复指定主机的特定检查项")
    public Result<String> repairItem(
            @Parameter(description = "集群ID") @PathVariable Long clusterId,
            @Parameter(description = "主机IP") @RequestParam String hostIp,
            @Parameter(description = "检查类型") @RequestParam String checkType) {
        
        log.info("修复指定项目: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
        
        try {
            hostValidationService.startRepair(clusterId, hostIp, checkType);
            return Result.success(String.format("已开始修复主机 %s 的 %s 项目", hostIp, checkType));
            
        } catch (Exception e) {
            log.error("修复失败: clusterId={}, hostIp={}, checkType={}, error={}", 
                    clusterId, hostIp, checkType, e.getMessage(), e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }

    /**
     * 批量修复失败的检查项
     */
    @PostMapping("/repair-batch/{clusterId}")
    @Operation(summary = "批量修复失败检查项", description = "对指定主机的失败检查项执行批量修复")
    public Result<String> repairFailedChecks(
            @Parameter(description = "集群ID") @PathVariable Long clusterId,
            @Parameter(description = "主机IP列表") @RequestBody List<String> hostIps) {
        
        log.info("启动批量主机修复: clusterId={}, 主机数量={}", clusterId, hostIps.size());
        
        try {
            // 启动修复服务
            hostValidationService.repairFailedChecks(clusterId, hostIps);
            
            return Result.success("批量主机修复任务已启动，请通过SSE接收实时修复进度");
            
        } catch (Exception e) {
            log.error("启动批量主机修复失败: clusterId={}, error={}", clusterId, e.getMessage(), e);
            return Result.error("启动批量主机修复失败: " + e.getMessage());
        }
    }

    /**
     * 清理校验会话
     */
    @DeleteMapping("/cleanup/{clusterId}")
    @Operation(summary = "清理校验会话", description = "清理指定集群的校验会话数据，释放内存")
    public Result<String> cleanupValidation(
            @Parameter(description = "集群ID") @PathVariable Long clusterId) {
        
        log.info("清理主机校验会话: clusterId={}", clusterId);
        
        try {
            stateManager.cleanupValidationSession(clusterId);
            return Result.success("校验会话已清理");
            
        } catch (Exception e) {
            log.error("清理校验会话失败: clusterId={}, error={}", clusterId, e.getMessage(), e);
            return Result.error("清理校验会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取活跃的校验会话
     */
    @GetMapping("/sessions")
    @Operation(summary = "获取活跃会话", description = "获取所有活跃的主机校验会话")
    public Result<List<Long>> getActiveSessions() {
        try {
            List<Long> activeSessions = stateManager.getActiveValidationSessions();
            return Result.success(activeSessions);
            
        } catch (Exception e) {
            log.error("获取活跃会话失败: error={}", e.getMessage(), e);
            return Result.error("获取活跃会话失败: " + e.getMessage());
        }
    }
}
