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

package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.QueueManagerService;
import com.datasophon.api.service.impl.HostCheckQueueManager;
import com.datasophon.common.enums.ScopeCode;
import com.datasophon.common.model.AsyncServiceStatus;
import com.datasophon.common.model.OperationResult;
import com.datasophon.common.model.QueueManagerStatus;
import com.datasophon.common.model.QueueSystemStatus;
import com.datasophon.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Queue Manager Service Implementation
 */
@Service
@Slf4j
public class QueueManagerServiceImpl implements QueueManagerService {

    @Autowired
    @Lazy
    private HostCheckQueueManager hostCheckQueueManager;

    @Autowired
    @Lazy
    private AsyncCheckService asyncCheckService;

    /**
     * 获取队列系统状态
     * @return 队列系统状态对象
     */
    @Override
    public Result getQueueSystemStatus() {
        try {
            // 直接获取实体类对象
            QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
            AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();
            
            // 创建QueueSystemStatus对象
            QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
            queueSystemStatus.setQueueManager(queueManagerStatus);
            queueSystemStatus.setAsyncService(asyncServiceStatus);
            
            return Result.success(queueSystemStatus);
        } catch (Exception e) {
            log.error("获取队列系统状态失败", e);
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 暂停队列系统
     * @param scope 范围代码
     * @return 操作结果
     */
    @Override
    public Result pauseQueueSystem(String scope) {
        try {
            ScopeCode scopeCode = ScopeCode.valueOf(scope.toUpperCase());
            StringBuilder messageBuilder = new StringBuilder("已暂停");
            
            // 根据作用范围执行暂停操作
            if (scopeCode == ScopeCode.ALL || scopeCode == ScopeCode.SCHEDULER) {
                // 暂停AsyncCheckService的定时任务
                asyncCheckService.stopScheduledTasks();
                if (scopeCode == ScopeCode.SCHEDULER) {
                    messageBuilder.append("定时任务");
                }
            }
            
            if (scopeCode == ScopeCode.ALL || scopeCode == ScopeCode.QUEUE) {
                // 暂停队列处理
                hostCheckQueueManager.pauseQueueProcessing();
                if (scopeCode == ScopeCode.QUEUE) {
                    messageBuilder.append("队列处理");
                } else if (scopeCode == ScopeCode.ALL) {
                    messageBuilder.append("队列处理和定时任务");
                }
            }
            
            // 获取最新状态
            QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
            AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();
            
            // 创建QueueSystemStatus对象
            QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
            queueSystemStatus.setQueueManager(queueManagerStatus);
            queueSystemStatus.setAsyncService(asyncServiceStatus);
            
            // 创建操作结果
            OperationResult result = new OperationResult();
            result.setSuccess(true);
            result.setMessage(messageBuilder.toString());
            result.setStatus(queueSystemStatus);
            result.setTimestamp(System.currentTimeMillis());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("暂停队列系统失败", e);
            return Result.error("暂停操作失败: " + e.getMessage());
        }
    }

    /**
     * 恢复队列系统
     * @param scope 范围代码
     * @return 操作结果
     */
    @Override
    public Result resumeQueueSystem(String scope) {
        try {
            ScopeCode scopeCode = ScopeCode.valueOf(scope.toUpperCase());
            StringBuilder messageBuilder = new StringBuilder("已恢复");
            
            // 根据作用范围执行恢复操作
            if (scopeCode == ScopeCode.ALL || scopeCode == ScopeCode.SCHEDULER) {
                // 恢复AsyncCheckService的定时任务
                asyncCheckService.startScheduledTasks();
                if (scopeCode == ScopeCode.SCHEDULER) {
                    messageBuilder.append("定时任务");
                }
            }
            
            if (scopeCode == ScopeCode.ALL || scopeCode == ScopeCode.QUEUE) {
                // 恢复队列处理
                hostCheckQueueManager.resumeQueueProcessing();
                if (scopeCode == ScopeCode.QUEUE) {
                    messageBuilder.append("队列处理");
                } else if (scopeCode == ScopeCode.ALL) {
                    messageBuilder.append("队列处理和定时任务");
                }
            }
            
            // 获取最新状态
            QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
            AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();
            
            // 创建QueueSystemStatus对象
            QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
            queueSystemStatus.setQueueManager(queueManagerStatus);
            queueSystemStatus.setAsyncService(asyncServiceStatus);
            
            // 创建操作结果
            OperationResult result = new OperationResult();
            result.setSuccess(true);
            result.setMessage(messageBuilder.toString());
            result.setStatus(queueSystemStatus);
            result.setTimestamp(System.currentTimeMillis());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("恢复队列系统失败", e);
            return Result.error("恢复操作失败: " + e.getMessage());
        }
    }

    /**
     * 关闭队列系统
     * @return 操作结果
     */
    @Override
    public Result shutdownQueueSystem() {
        try {
            // 为安全起见，先取消所有任务
            hostCheckQueueManager.cancelAllTasks();
            
            // 完全关闭队列管理器
            hostCheckQueueManager.shutdown();
            
            // 禁用AsyncCheckService的定时任务
            asyncCheckService.disableScheduledTasks();
            
            // 创建状态对象
            QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
            
            QueueManagerStatus queueManagerStatus = new QueueManagerStatus();
            queueManagerStatus.setRunning(false);
            
            AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();
            
            queueSystemStatus.setQueueManager(queueManagerStatus);
            queueSystemStatus.setAsyncService(asyncServiceStatus);
            
            // 创建操作结果
            OperationResult result = new OperationResult();
            result.setSuccess(true);
            result.setMessage("已完全关闭队列管理器，定时任务已禁用");
            result.setStatus(queueSystemStatus);
            result.setTimestamp(System.currentTimeMillis());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("关闭队列系统失败", e);
            return Result.error("关闭操作失败: " + e.getMessage());
        }
    }

    /**
     * 管理队列系统
     * @param action 动作
     * @param scope 范围代码
     * @return 操作结果
     */
    @Override
    public Result manageQueueSystem(String action, String scope) {
        log.info("处理队列管理请求: action={}, scope={}", action, scope);
        
        try {
            // 根据action类型执行不同操作
            if ("status".equalsIgnoreCase(action)) {
                return getQueueSystemStatus();
            } else if ("pause".equalsIgnoreCase(action)) {
                return pauseQueueSystem(scope);
            } else if ("resume".equalsIgnoreCase(action)) {
                return resumeQueueSystem(scope);
            } else if ("shutdown".equalsIgnoreCase(action)) {
                return shutdownQueueSystem();
            } else {
                return Result.error(400, "不支持的操作: " + action);
            }
        } catch (Exception e) {
            log.error("控制队列管理器时发生错误", e);
            return Result.error(500, "操作异常: " + e.getMessage());
        }
    }

    /**
     * 清理不活跃的SSH连接
     * @return 操作结果，包含清理的连接数
     */
    @Override
    public Result cleanupConnections() {
        try {
            // 手动触发连接清理
            asyncCheckService.cleanupConnections();
            
            // 获取清理后的连接信息
            AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();
            
            // 创建操作结果
            OperationResult result = new OperationResult();
            result.setSuccess(true);
            result.setMessage("已清理不活跃SSH连接");
            
            // 创建状态对象
            QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
            QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
            queueSystemStatus.setQueueManager(queueManagerStatus);
            queueSystemStatus.setAsyncService(asyncServiceStatus);
            result.setStatus(queueSystemStatus);
            
            result.setTimestamp(System.currentTimeMillis());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("清理SSH连接失败", e);
            return Result.error("清理SSH连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 暂停指定的定时任务
     * @param taskId 任务ID
     * @return 操作结果
     */
    @Override
    public Result pauseScheduledTask(String taskId) {
        try {
            boolean success = false;
            String message = "";
            
            // 根据任务ID执行相应操作
            switch (taskId) {
                case "taskCleanup":
                    asyncCheckService.stopTaskCleanup();
                    message = "已暂停任务清理定时任务";
                    success = true;
                    break;
                case "connectionCleanup":
                    asyncCheckService.stopConnectionCleanup();
                    message = "已暂停连接清理定时任务";
                    success = true;
                    break;
                case "queueHealthMonitor":
                    hostCheckQueueManager.stopQueueHealthMonitor();
                    message = "已暂停队列健康监控定时任务";
                    success = true;
                    break;
                case "taskTimeoutMonitor":
                    hostCheckQueueManager.stopTaskTimeoutMonitor();
                    message = "已暂停任务超时监控定时任务";
                    success = true;
                    break;
                default:
                    return Result.error("无效的任务ID: " + taskId);
            }
            
            // 获取最新状态
            QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
            AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();
            
            // 创建状态对象
            QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
            queueSystemStatus.setQueueManager(queueManagerStatus);
            queueSystemStatus.setAsyncService(asyncServiceStatus);
            
            // 创建操作结果
            OperationResult result = new OperationResult();
            result.setSuccess(success);
            result.setMessage(message);
            result.setStatus(queueSystemStatus);
            result.setTimestamp(System.currentTimeMillis());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("暂停定时任务失败", e);
            return Result.error("暂停定时任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 恢复指定的定时任务
     * @param taskId 任务ID
     * @return 操作结果
     */
    @Override
    public Result resumeScheduledTask(String taskId) {
        try {
            boolean success = false;
            String message = "";
            
            // 根据任务ID执行相应操作
            switch (taskId) {
                case "taskCleanup":
                    asyncCheckService.startTaskCleanup();
                    message = "已恢复任务清理定时任务";
                    success = true;
                    break;
                case "connectionCleanup":
                    asyncCheckService.startConnectionCleanup();
                    message = "已恢复连接清理定时任务";
                    success = true;
                    break;
                case "queueHealthMonitor":
                    hostCheckQueueManager.startQueueHealthMonitor();
                    message = "已恢复队列健康监控定时任务";
                    success = true;
                    break;
                case "taskTimeoutMonitor":
                    hostCheckQueueManager.startTaskTimeoutMonitor();
                    message = "已恢复任务超时监控定时任务";
                    success = true;
                    break;
                default:
                    return Result.error("无效的任务ID: " + taskId);
            }
            
            // 获取最新状态
            QueueManagerStatus queueManagerStatus = hostCheckQueueManager.getQueueManagerStatus();
            AsyncServiceStatus asyncServiceStatus = asyncCheckService.getAsyncServiceStatus();
            
            // 创建状态对象
            QueueSystemStatus queueSystemStatus = new QueueSystemStatus();
            queueSystemStatus.setQueueManager(queueManagerStatus);
            queueSystemStatus.setAsyncService(asyncServiceStatus);
            
            // 创建操作结果
            OperationResult result = new OperationResult();
            result.setSuccess(success);
            result.setMessage(message);
            result.setStatus(queueSystemStatus);
            result.setTimestamp(System.currentTimeMillis());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("恢复定时任务失败", e);
            return Result.error("恢复定时任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取检查任务队列详情
     * @return 任务队列中的详细任务信息
     */
    @Override
    public Result getCheckQueueTasks() {
        try {
            // 获取检查队列中的任务
            List<Map<String, Object>> checkTasks = hostCheckQueueManager.getQueueTasksDetails();
            return Result.success(checkTasks);
        } catch (Exception e) {
            log.error("获取检查任务队列详情失败", e);
            return Result.error("获取队列详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取修复任务队列详情
     * @return 修复任务队列中的详细任务信息
     */
    @Override
    public Result getFixQueueTasks() {
        try {
            // 获取修复队列中的任务
            List<Map<String, Object>> fixTasks = hostCheckQueueManager.getFixQueueTasksDetails();
            return Result.success(fixTasks);
        } catch (Exception e) {
            log.error("获取修复任务队列详情失败", e);
            return Result.error("获取队列详情失败: " + e.getMessage());
        }
    }
} 