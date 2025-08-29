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

import com.datasophon.api.hostvalidation.manager.HostValidationStateManager;
import com.datasophon.api.hostvalidation.service.HostValidationService;
import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.enums.CheckType;
import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.Scheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * 主机校验调度服务
 * 基于db-scheduler实现任务调度和管理
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HostValidationSchedulerService {
    
    private final HostValidationService hostValidationService;
    private final HostValidationStateManager stateManager;
    private final Scheduler scheduler;
    
    // 任务类型定义
    public static final String TASK_HOST_VALIDATION = "host-validation";
    public static final String TASK_HOST_REPAIR = "host-repair";
    public static final String TASK_HOST_CLEANUP = "host-cleanup";
    
    /**
     * 主机校验任务
     */
    public final OneTimeTask<HostValidationRequestDTO> hostValidationTask = 
        Tasks.oneTime(TASK_HOST_VALIDATION, HostValidationRequestDTO.class)
            .execute((instance, context) -> {
                log.info("执行主机校验任务: taskId={}, clusterId={}", 
                        instance.getId(), context.clusterId());
                
                try {
                    hostValidationService.startValidation(context);
                    log.info("主机校验任务执行成功: taskId={}, clusterId={}", 
                            instance.getId(), context.clusterId());
                } catch (Exception e) {
                    log.error("主机校验任务执行失败: taskId={}, clusterId={}, error={}", 
                            instance.getId(), context.clusterId(), e.getMessage(), e);
                    throw e; // 重新抛出异常，让调度器处理重试
                }
            });
    
    /**
     * 主机修复任务
     */
    public final OneTimeTask<Map<String, Object>> hostRepairTask = 
        Tasks.oneTime(TASK_HOST_REPAIR, Map.class)
            .execute((instance, context) -> {
                log.info("执行主机修复任务: taskId={}", instance.getId());
                
                try {
                    Long clusterId = (Long) context.get("clusterId");
                    String hostIp = (String) context.get("hostIp");
                    String checkTypeCode = (String) context.get("checkType");
                    
                    if (clusterId != null && hostIp != null && checkTypeCode != null) {
                        CheckType checkType = CheckType.fromCode(checkTypeCode);
                        if (checkType != null) {
                            hostValidationService.startRepair(clusterId, hostIp, checkType);
                            log.info("主机修复任务执行成功: taskId={}, clusterId={}, hostIp={}, checkType={}", 
                                    instance.getId(), clusterId, hostIp, checkType);
                        } else {
                            log.error("无效的检查类型: taskId={}, checkTypeCode={}", instance.getId(), checkTypeCode);
                        }
                    } else {
                        log.error("主机修复任务参数不完整: taskId={}, context={}", instance.getId(), context);
                    }
                } catch (Exception e) {
                    log.error("主机修复任务执行失败: taskId={}, error={}", instance.getId(), e.getMessage(), e);
                    throw e;
                }
            });
    
    /**
     * 主机清理任务
     */
    public final OneTimeTask<Long> hostCleanupTask = 
        Tasks.oneTime(TASK_HOST_CLEANUP, Long.class)
            .execute((instance, context) -> {
                log.info("执行主机清理任务: taskId={}, clusterId={}", instance.getId(), context);
                
                try {
                    // 清理过期的校验会话
                    boolean cleaned = stateManager.cleanupValidationSession(context);
                    if (cleaned) {
                        log.info("主机清理任务执行成功: taskId={}, clusterId={}, 会话已清理", instance.getId(), context);
                    } else {
                        log.info("主机清理任务执行成功: taskId={}, clusterId={}, 无需清理", instance.getId(), context);
                    }
                } catch (Exception e) {
                    log.error("主机清理任务执行失败: taskId={}, clusterId={}, error={}", 
                            instance.getId(), context, e.getMessage(), e);
                    throw e;
                }
            });
    
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
        
        scheduler.schedule(hostValidationTask.instance(taskId, request), executionTime);
        
        log.info("已调度主机校验任务: taskId={}, clusterId={}, executeAt={}", 
                taskId, request.clusterId(), executionTime);
        
        return taskId;
    }
    
    /**
     * 立即调度主机校验任务
     */
    public String scheduleHostValidationNow(HostValidationRequestDTO request) {
        return scheduleHostValidation(request, 0);
    }
    
    /**
     * 调度主机修复任务
     * 
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @param checkType 检查类型
     * @param delaySeconds 延迟执行秒数
     * @return 任务ID
     */
    public String scheduleHostRepair(Long clusterId, String hostIp, CheckType checkType, int delaySeconds) {
        String taskId = generateTaskId(TASK_HOST_REPAIR, clusterId, hostIp, checkType);
        Instant executionTime = Instant.now().plusSeconds(delaySeconds);
        
        Map<String, Object> context = Map.of(
            "clusterId", clusterId,
            "hostIp", hostIp,
            "checkType", checkType.getCode()
        );
        
        scheduler.schedule(hostRepairTask.instance(taskId, context), executionTime);
        
        log.info("已调度主机修复任务: taskId={}, clusterId={}, hostIp={}, checkType={}, executeAt={}", 
                taskId, clusterId, hostIp, checkType, executionTime);
        
        return taskId;
    }
    
    /**
     * 立即调度主机修复任务
     */
    public String scheduleHostRepairNow(Long clusterId, String hostIp, CheckType checkType) {
        return scheduleHostRepair(clusterId, hostIp, checkType, 0);
    }
    
    /**
     * 调度主机清理任务
     */
    public String scheduleHostCleanup(Long clusterId, int delayMinutes) {
        String taskId = generateTaskId(TASK_HOST_CLEANUP, clusterId);
        Instant executionTime = Instant.now().plusSeconds(delayMinutes * 60L);
        
        scheduler.schedule(hostCleanupTask.instance(taskId, clusterId), executionTime);
        
        log.info("已调度主机清理任务: taskId={}, clusterId={}, executeAt={}", 
                taskId, clusterId, executionTime);
        
        return taskId;
    }
    
    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId, String taskType) {
        try {
            Task<?> task = getTaskByType(taskType);
            if (task != null) {
                scheduler.cancel(task.instance(taskId));
                log.info("任务取消成功: taskId={}, taskType={}", taskId, taskType);
                return true;
            } else {
                log.warn("未找到对应的任务类型: taskType={}", taskType);
                return false;
            }
        } catch (Exception e) {
            log.error("取消任务失败: taskId={}, taskType={}, error={}", taskId, taskType, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 重新调度任务
     */
    public boolean rescheduleTask(String taskId, String taskType, Instant newExecutionTime) {
        try {
            Task<?> task = getTaskByType(taskType);
            if (task != null) {
                scheduler.reschedule(task.instance(taskId), newExecutionTime);
                log.info("任务重新调度成功: taskId={}, taskType={}, newExecutionTime={}", 
                        taskId, taskType, newExecutionTime);
                return true;
            } else {
                log.warn("未找到对应的任务类型: taskType={}", taskType);
                return false;
            }
        } catch (Exception e) {
            log.error("重新调度任务失败: taskId={}, taskType={}, error={}", taskId, taskType, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId(String taskType, Object... params) {
        StringBuilder sb = new StringBuilder(taskType);
        for (Object param : params) {
            sb.append("-").append(param);
        }
        sb.append("-").append(System.currentTimeMillis());
        return sb.toString();
    }
    
    /**
     * 根据任务类型获取任务
     */
    private Task<?> getTaskByType(String taskType) {
        return switch (taskType) {
            case TASK_HOST_VALIDATION -> hostValidationTask;
            case TASK_HOST_REPAIR -> hostRepairTask;
            case TASK_HOST_CLEANUP -> hostCleanupTask;
            default -> null;
        };
    }
}
