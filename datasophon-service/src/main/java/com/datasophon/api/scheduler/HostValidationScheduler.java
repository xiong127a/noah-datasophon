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

import com.datasophon.api.model.HostValidationTaskData;
import com.datasophon.api.service.HostValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 主机验证任务调度器
 * 负责管理主机验证相关的所有调度任务
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Component
@Slf4j
public class HostValidationScheduler {
    
    @Autowired
    private HostValidationService hostValidationService;
    
    @Autowired
    private Scheduler scheduler;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * SSH连接检查任务
     * 优先级最高，其他检查依赖此任务
     */
    @Bean
    public Task<HostValidationTaskData> sshConnectivityCheckTask() {
        return Tasks.oneTime("ssh-connectivity-check", HostValidationTaskData.class)
                .execute((instance, ctx) -> {
                    HostValidationTaskData taskData = instance.getData();
                    log.info("开始执行SSH连接检查: 主机={}, 集群={}", 
                            taskData.getHostIp(), taskData.getClusterId());
                    
                    try {
                        // 执行SSH连接检查
                        hostValidationService.executeSshConnectivityCheck(taskData);
                        
                        // SSH检查成功后，调度后续检查任务
                        scheduleFollowUpTasks(taskData);
                        
                        log.info("SSH连接检查完成: 主机={}", taskData.getHostIp());
                    } catch (Exception e) {
                        log.error("SSH连接检查失败: 主机={}, 错误={}", 
                                taskData.getHostIp(), e.getMessage(), e);
                        
                        // 处理重试逻辑
                        handleTaskRetry(taskData, "ssh-connectivity-check", e);
                        throw new RuntimeException("SSH连接检查失败", e); // 让db-scheduler记录失败
                    }
                });
    }
    
    /**
     * 操作系统信息收集任务
     */
    @Bean
    public Task<HostValidationTaskData> osInfoCollectionTask() {
        return Tasks.oneTime("os-info-collection", HostValidationTaskData.class)
                .execute((instance, ctx) -> {
                    HostValidationTaskData taskData = instance.getData();
                    log.info("开始执行操作系统信息收集: 主机={}, 集群={}", 
                            taskData.getHostIp(), taskData.getClusterId());
                    
                    try {
                        // 执行操作系统信息收集
                        hostValidationService.executeOsInfoCollection(taskData);
                        
                        log.info("操作系统信息收集完成: 主机={}", taskData.getHostIp());
                    } catch (Exception e) {
                        log.error("操作系统信息收集失败: 主机={}, 错误={}", 
                                taskData.getHostIp(), e.getMessage(), e);
                        
                        // 处理重试逻辑
                        handleTaskRetry(taskData, "os-info-collection", e);
                        throw new RuntimeException("操作系统信息收集失败", e);
                    }
                });
    }
    
    /**
     * 硬件信息收集任务
     */
    @Bean
    public Task<HostValidationTaskData> hardwareInfoCollectionTask() {
        return Tasks.oneTime("hardware-info-collection", HostValidationTaskData.class)
                .execute((instance, ctx) -> {
                    HostValidationTaskData taskData = instance.getData();
                    log.info("开始执行硬件信息收集: 主机={}, 集群={}", 
                            taskData.getHostIp(), taskData.getClusterId());
                    
                    try {
                        // 执行硬件信息收集
                        hostValidationService.executeHardwareInfoCollection(taskData);
                        
                        log.info("硬件信息收集完成: 主机={}", taskData.getHostIp());
                    } catch (Exception e) {
                        log.error("硬件信息收集失败: 主机={}, 错误={}", 
                                taskData.getHostIp(), e.getMessage(), e);
                        
                        // 处理重试逻辑
                        handleTaskRetry(taskData, "hardware-info-collection", e);
                        throw new RuntimeException("硬件信息收集失败", e);
                    }
                });
    }
    
    /**
     * 主机名和网络检查任务
     */
    @Bean
    public Task<HostValidationTaskData> hostnameNetworkCheckTask() {
        return Tasks.oneTime("hostname-network-check", HostValidationTaskData.class)
                .execute((instance, ctx) -> {
                    HostValidationTaskData taskData = instance.getData();
                    log.info("开始执行主机名和网络检查: 主机={}, 集群={}", 
                            taskData.getHostIp(), taskData.getClusterId());
                    
                    try {
                        // 执行主机名和网络检查
                        hostValidationService.executeHostnameNetworkCheck(taskData);
                        
                        log.info("主机名和网络检查完成: 主机={}", taskData.getHostIp());
                    } catch (Exception e) {
                        log.error("主机名和网络检查失败: 主机={}, 错误={}", 
                                taskData.getHostIp(), e.getMessage(), e);
                        
                        // 处理重试逻辑
                        handleTaskRetry(taskData, "hostname-network-check", e);
                        throw e;
                    }
                });
    }
    
    /**
     * 启动主机验证流程
     * 这是外部调用的入口方法
     * 
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @param sshInfo SSH连接信息
     */
    public void startHostValidation(String clusterId, String hostIp, 
                                   HostValidationTaskData.SshConnectionInfo sshInfo) {
        try {
            log.info("启动主机验证流程: 集群={}, 主机={}", clusterId, hostIp);
            
            // 创建SSH连接检查任务（第一个任务）
            HostValidationTaskData sshTaskData = HostValidationTaskData.createSshCheckTask(
                    clusterId, hostIp, sshInfo);
            
            // 立即调度SSH检查任务
            scheduler.schedule(
                    sshConnectivityCheckTask().instance(sshTaskData.getTaskInstanceId(), sshTaskData),
                    Instant.now()
            );
            
            log.info("主机验证SSH检查任务已调度: 主机={}, 实例ID={}", 
                    hostIp, sshTaskData.getTaskInstanceId());
            
        } catch (Exception e) {
            log.error("启动主机验证流程失败: 集群={}, 主机={}, 错误={}", 
                    clusterId, hostIp, e.getMessage(), e);
            throw new RuntimeException("启动主机验证失败", e);
        }
    }
    
    /**
     * 调度后续检查任务
     * SSH检查成功后调用
     */
    private void scheduleFollowUpTasks(HostValidationTaskData baseTaskData) {
        try {
            // 延迟1秒调度，确保SSH检查状态已更新
            Instant scheduleTime = Instant.now().plusSeconds(1);
            
            // 创建操作系统信息收集任务
            HostValidationTaskData osTaskData = HostValidationTaskData.createOsInfoTask(
                    baseTaskData.getClusterId(), 
                    baseTaskData.getHostIp(), 
                    baseTaskData.getSshInfo());
            
            scheduler.schedule(
                    osInfoCollectionTask().instance(osTaskData.getTaskInstanceId(), osTaskData),
                    scheduleTime
            );
            
            // 创建硬件信息收集任务
            HostValidationTaskData hwTaskData = HostValidationTaskData.createHardwareInfoTask(
                    baseTaskData.getClusterId(), 
                    baseTaskData.getHostIp(), 
                    baseTaskData.getSshInfo());
            
            scheduler.schedule(
                    hardwareInfoCollectionTask().instance(hwTaskData.getTaskInstanceId(), hwTaskData),
                    scheduleTime
            );
            
            // 创建主机名和网络检查任务
            HostValidationTaskData networkTaskData = HostValidationTaskData.builder()
                    .clusterId(baseTaskData.getClusterId())
                    .taskId(baseTaskData.getTaskId())
                    .hostIp(baseTaskData.getHostIp())
                    .sshInfo(baseTaskData.getSshInfo())
                    .checkType("hostname-network-check")
                    .priority(3)
                    .build();
            
            scheduler.schedule(
                    hostnameNetworkCheckTask().instance(networkTaskData.getTaskInstanceId(), networkTaskData),
                    scheduleTime
            );
            
            log.info("后续检查任务已调度: 主机={}, 任务数=3", baseTaskData.getHostIp());
            
        } catch (Exception e) {
            log.error("调度后续任务失败: 主机={}, 错误={}", 
                    baseTaskData.getHostIp(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理任务重试逻辑
     */
    private void handleTaskRetry(HostValidationTaskData taskData, String taskName, Exception error) {
        try {
            if (taskData.canRetry()) {
                taskData.incrementRetry();
                
                log.warn("任务将重试: 任务={}, 主机={}, 重试次数={}/{}", 
                        taskName, taskData.getHostIp(), 
                        taskData.getCurrentRetry(), taskData.getMaxRetries());
                
                // 延迟重试（指数退避）
                Instant retryTime = Instant.now().plusSeconds(30L * taskData.getCurrentRetry());
                
                // 根据任务类型重新调度
                switch (taskName) {
                    case "ssh-connectivity-check":
                        scheduler.schedule(
                                sshConnectivityCheckTask().instance(taskData.getTaskInstanceId(), taskData),
                                retryTime
                        );
                        break;
                    case "os-info-collection":
                        scheduler.schedule(
                                osInfoCollectionTask().instance(taskData.getTaskInstanceId(), taskData),
                                retryTime
                        );
                        break;
                    case "hardware-info-collection":
                        scheduler.schedule(
                                hardwareInfoCollectionTask().instance(taskData.getTaskInstanceId(), taskData),
                                retryTime
                        );
                        break;
                    case "hostname-network-check":
                        scheduler.schedule(
                                hostnameNetworkCheckTask().instance(taskData.getTaskInstanceId(), taskData),
                                retryTime
                        );
                        break;
                }
                
            } else {
                log.error("任务重试次数已达上限: 任务={}, 主机={}, 最终错误={}", 
                        taskName, taskData.getHostIp(), error.getMessage());
                
                // 标记主机验证失败
                hostValidationService.markHostValidationFailed(
                        taskData.getClusterId(), 
                        taskData.getHostIp(), 
                        taskName, 
                        error.getMessage()
                );
            }
        } catch (Exception e) {
            log.error("处理任务重试失败: 任务={}, 主机={}, 错误={}", 
                    taskName, taskData.getHostIp(), e.getMessage(), e);
        }
    }
    
    /**
     * 取消主机的所有验证任务
     */
    public void cancelHostValidation(String clusterId, String hostIp) {
        try {
            log.info("取消主机验证任务: 集群={}, 主机={}", clusterId, hostIp);
            
            // 取消所有相关任务
            String[] taskTypes = {"ssh-connectivity-check", "os-info-collection", 
                                 "hardware-info-collection", "hostname-network-check"};
            
            for (String taskType : taskTypes) {
                String instanceId = String.format("%s-%s-%s", clusterId, hostIp, taskType);
                
                // 注意：db-scheduler不直接支持取消任务，这里记录日志
                log.info("标记取消任务: 实例ID={}", instanceId);
            }
            
        } catch (Exception e) {
            log.error("取消主机验证任务失败: 集群={}, 主机={}, 错误={}", 
                    clusterId, hostIp, e.getMessage(), e);
        }
    }
}
