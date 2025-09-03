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

package com.datasophon.api.hostvalidation.scheduler;

import com.datasophon.api.hostvalidation.executor.HostValidationExecutor;
import com.datasophon.api.hostvalidation.manager.HostValidationStateManager;
import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.enums.CheckType;

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.Scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 主机校验调度服务 - 基于官方pf4j-spring标准
 * 基于db-scheduler实现任务调度和管理
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Service
public class HostValidationSchedulerService {
    
    private final HostValidationExecutor hostValidationExecutor;
    private final HostValidationStateManager stateManager;
    private final Scheduler scheduler;
    
    // 任务名称常量（直接使用字符串常量，符合官方pf4j-spring标准）
    public static final String TASK_HOST_VALIDATION = "host-validation";
    public static final String TASK_HOST_REPAIR = "host-repair";
    public static final String TASK_HOST_CLEANUP = "host-cleanup";
    
    public HostValidationSchedulerService(HostValidationExecutor hostValidationExecutor,
                                         HostValidationStateManager stateManager,
                                         Scheduler scheduler) {
        this.hostValidationExecutor = hostValidationExecutor;
        this.stateManager = stateManager;
        this.scheduler = scheduler;
        
        log.info("主机校验调度服务初始化完成 - 官方pf4j-spring标准");
    }

    /**
     * 立即调度主机校验任务
     */
    public String scheduleHostValidationNow(HostValidationRequestDTO request) {
        return scheduleHostValidation(request, 0);
    }
    
    /**
     * 调度主机校验任务
     * 
     * @param request 校验请求
     * @param delaySeconds 延迟执行秒数
     * @return 任务ID
     */
    public String scheduleHostValidation(HostValidationRequestDTO request, int delaySeconds) {
        String taskId = generateTaskId(TASK_HOST_VALIDATION, request.clusterId());
        Instant executionTime = Instant.now().plusSeconds(delaySeconds);
        
        // 使用内联任务定义，避免循环依赖
        OneTimeTask<HostValidationRequestDTO> task = Tasks.oneTime(TASK_HOST_VALIDATION, HostValidationRequestDTO.class)
            .execute((instance, context) -> {
                HostValidationRequestDTO data = instance.getData();
                log.info("执行主机校验任务: taskId={}, clusterId={}", 
                        instance.getId(), data.clusterId());
                
                try {
                    // 调用执行器执行校验
                    hostValidationExecutor.executeValidation(data);
                    log.info("主机校验任务执行成功: taskId={}, clusterId={}", 
                            instance.getId(), data.clusterId());
                } catch (Exception e) {
                    log.error("主机校验任务执行失败: taskId={}, clusterId={}, error={}", 
                            instance.getId(), data.clusterId(), e.getMessage(), e);
                    throw e; // 重新抛出异常，让调度器处理重试
                }
            });
        
        scheduler.schedule(task.instance(taskId, request), executionTime);
        
        log.info("已调度主机校验任务: taskId={}, clusterId={}, executeAt={}", 
                taskId, request.clusterId(), executionTime);
        
        return taskId;
    }

    /**
     * 立即调度主机修复任务
     */
    public String scheduleHostRepairNow(Long clusterId, String hostIp, CheckType checkType) {
        return scheduleHostRepair(clusterId, hostIp, checkType, 0);
    }
    
    /**
     * 调度主机修复任务
     */
    public String scheduleHostRepair(Long clusterId, String hostIp, CheckType checkType, int delaySeconds) {
        String taskId = generateTaskId(TASK_HOST_REPAIR, clusterId, hostIp, checkType.getCode());
        Instant executionTime = Instant.now().plusSeconds(delaySeconds);
        
        // 创建修复任务数据，使用简单字符串格式避免泛型问题
        String repairData = clusterId + "," + hostIp + "," + checkType.getCode();
        
        // 使用String类型，避免复杂的泛型问题
        OneTimeTask<String> task = Tasks.oneTime(TASK_HOST_REPAIR, String.class)
            .execute((instance, context) -> {
                String data = instance.getData();
                String[] parts = data.split(",");
                
                if (parts.length == 3) {
                    try {
                        Long cId = Long.parseLong(parts[0]);
                        String hIp = parts[1];
                        String checkTypeCode = parts[2];
                        
                        CheckType cType = CheckType.fromCode(checkTypeCode);
                        if (cType != null) {
                            // 调用执行器执行修复
                            hostValidationExecutor.executeRepair(cId, hIp, cType);
                            log.info("主机修复任务执行成功: taskId={}, clusterId={}, hostIp={}, checkType={}", 
                                    instance.getId(), cId, hIp, cType);
                        } else {
                            log.error("无效的检查类型: taskId={}, checkTypeCode={}", instance.getId(), checkTypeCode);
                        }
                    } catch (NumberFormatException e) {
                        log.error("解析clusterId失败: taskId={}, data={}, error={}", 
                                instance.getId(), data, e.getMessage());
                    }
                } else {
                    log.error("主机修复任务参数格式错误: taskId={}, data={}", instance.getId(), data);
                }
            });
        
        scheduler.schedule(task.instance(taskId, repairData), executionTime);
        
        log.info("已调度主机修复任务: taskId={}, clusterId={}, hostIp={}, checkType={}, executeAt={}", 
                taskId, clusterId, hostIp, checkType, executionTime);
        
        return taskId;
    }

    /**
     * 取消任务
     */
    public void cancelTask(String taskId) {
        try {
            // 从taskId中解析出任务名称，格式为: taskName-params-timestamp
            String taskName = extractTaskNameFromId(taskId);
            TaskInstanceId instanceId = TaskInstanceId.of(taskName, taskId);
            scheduler.cancel(instanceId);
            log.info("已取消任务: taskId={}", taskId);
        } catch (Exception e) {
            log.warn("取消任务失败: taskId={}, error={}", taskId, e.getMessage());
        }
    }

    /**
     * 取消指定类型的任务
     */
    public void cancelTask(String taskId, String taskType) {
        try {
            TaskInstanceId instanceId = TaskInstanceId.of(taskType, taskId);
            scheduler.cancel(instanceId);
            log.info("已取消{}任务: taskId={}", taskType, taskId);
        } catch (Exception e) {
            log.warn("取消{}任务失败: taskId={}, error={}", taskType, taskId, e.getMessage());
        }
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(String taskType, Object... params) {
        StringBuilder builder = new StringBuilder(taskType);
        for (Object param : params) {
            builder.append("-").append(param);
        }
        builder.append("-").append(System.currentTimeMillis());
        return builder.toString();
    }
    
    /**
     * 从任务ID中提取任务名称
     * 任务ID格式: taskName-params-timestamp
     */
    private String extractTaskNameFromId(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return "";
        }
        
        // 根据任务ID前缀判断任务类型
        if (taskId.startsWith(TASK_HOST_VALIDATION)) {
            return TASK_HOST_VALIDATION;
        } else if (taskId.startsWith(TASK_HOST_REPAIR)) {
            return TASK_HOST_REPAIR;
        } else if (taskId.startsWith(TASK_HOST_CLEANUP)) {
            return TASK_HOST_CLEANUP;
        }
        
        // 如果无法识别，返回第一个"-"之前的部分
        int firstDash = taskId.indexOf('-');
        return firstDash > 0 ? taskId.substring(0, firstDash) : taskId;
    }
}