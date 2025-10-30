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

import com.datasophon.common.command.*;
import com.datasophon.worker.http.event.TaskResultEvent;
import com.datasophon.worker.http.event.TaskStatusEvent;
import com.datasophon.worker.service.*;
import com.datasophon.worker.service.ConfigureServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 任务执行器
 * 负责接收命令、分发到对应的Service、管理任务生命周期、推送SSE事件
 */
@Component
public class TaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);

    private final Executor taskExecutor;
    private final SseEmitterManager sseManager;
    private final PingService pingService;
    private final SystemInfoService systemInfoService;
    private final InstallServiceService installServiceService;
    private final ServiceOperateService serviceOperateService;
    private final ConfigureServiceService configureServiceService;
    private final ExecuteCmdService executeCmdService;
    private final FileOperateService fileOperateService;
    private final LogService logService;
    private final UnixUserService unixUserService;
    private final UnixGroupService unixGroupService;

    /**
     * 存储所有任务信息
     * Key: taskId, Value: TaskInfo
     */
    private final Map<String, TaskInfo> tasks = new ConcurrentHashMap<>();

    public TaskExecutor(@Qualifier("taskExecutor") Executor taskExecutor, 
                       SseEmitterManager sseManager,
                       PingService pingService,
                       SystemInfoService systemInfoService,
                       InstallServiceService installServiceService,
                       ServiceOperateService serviceOperateService,
                       ConfigureServiceService configureServiceService,
                       ExecuteCmdService executeCmdService,
                       FileOperateService fileOperateService,
                       LogService logService,
                       UnixUserService unixUserService,
                       UnixGroupService unixGroupService) {
        this.taskExecutor = taskExecutor;
        this.sseManager = sseManager;
        this.pingService = pingService;
        this.systemInfoService = systemInfoService;
        this.installServiceService = installServiceService;
        this.serviceOperateService = serviceOperateService;
        this.configureServiceService = configureServiceService;
        this.executeCmdService = executeCmdService;
        this.fileOperateService = fileOperateService;
        this.logService = logService;
        this.unixUserService = unixUserService;
        this.unixGroupService = unixGroupService;
    }

    /**
     * 提交任务执行
     * @param command 命令对象
     * @return 任务ID
     */
    public String submitTask(BaseCommand command) {
        // 生成任务ID
        String taskId = UUID.randomUUID().toString();
        
        // 创建任务信息
        TaskInfo taskInfo = new TaskInfo(taskId, command);
        tasks.put(taskId, taskInfo);
        
        logger.info("Task submitted: {}, command type: {}", taskId, command.getClass().getSimpleName());
        
        // 推送任务创建事件
        sseManager.sendEventToTask(taskId, 
            new TaskStatusEvent(taskId, "PENDING", "Task created"));
        
            // 异步执行任务
            taskExecutor.execute(() -> executeTask(taskInfo));
        
        return taskId;
    }

    /**
     * 执行任务
     */
    private void executeTask(TaskInfo taskInfo) {
        String taskId = taskInfo.getTaskId();
        BaseCommand command = taskInfo.getCommand();
        
        try {
            // 更新状态为运行中
            taskInfo.setStatus("RUNNING");
            taskInfo.setStartTime(LocalDateTime.now());
            sseManager.sendEventToTask(taskId, 
                new TaskStatusEvent(taskId, "RUNNING", "Task started"));
            
            logger.info("Executing task: {}", taskId);
            
            // 根据命令类型分发到对应的Service
            Object result = dispatchCommand(command, taskId);
            
            // 任务完成
            taskInfo.setStatus("COMPLETED");
            taskInfo.setResult(result);
            taskInfo.setCompleteTime(LocalDateTime.now());
            
            logger.info("Task completed: {}", taskId);
            
            // 推送完成事件
            sseManager.sendEventToTask(taskId, 
                new TaskResultEvent(taskId, result));
            sseManager.sendEventToTask(taskId, 
                new TaskStatusEvent(taskId, "COMPLETED", "Task completed successfully"));
            
        } catch (Exception e) {
            // 任务失败
            taskInfo.setStatus("FAILED");
            taskInfo.setErrorMessage(e.getMessage());
            taskInfo.setCompleteTime(LocalDateTime.now());
            
            logger.error("Task failed: {}", taskId, e);
            
            // 推送失败事件
            sseManager.sendEventToTask(taskId, 
                new TaskResultEvent(taskId, e.getMessage()));
            sseManager.sendEventToTask(taskId, 
                new TaskStatusEvent(taskId, "FAILED", "Task failed: " + e.getMessage()));
            
        } finally {
            // 完成SSE连接（如果有订阅者的话，5秒后自动关闭）
            // 注意：不立即关闭，给Master一点时间接收最后的事件
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    sseManager.completeTask(taskId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * 根据命令类型分发到对应的Service处理
     * TODO: 在创建Service层后实现具体的分发逻辑
     * 使用类名判断，因为各Command类不一定继承自BaseCommand
     */
    private Object dispatchCommand(BaseCommand command, String taskId) throws Exception {
        String commandType = command.getClass().getSimpleName();
        
        logger.info("Dispatching command type: {}", commandType);
        
        // 根据命令类型分发
        return switch (commandType) {
            case "InstallServiceRoleCommand" -> handleInstallService((InstallServiceRoleCommand) command, taskId);
            case "ServiceRoleOperateCommand" -> handleServiceOperate((ServiceRoleOperateCommand) command, taskId);
            case "GenerateServiceConfigCommand" -> handleGenerateConfig((GenerateServiceConfigCommand) command, taskId);
            case "ExecuteCmdCommand" -> handleExecuteCmd((ExecuteCmdCommand) command, taskId);
            case "FileOperateCommand" -> handleFileOperate((FileOperateCommand) command, taskId);
            case "CollectSystemInfoCommand" -> handleCollectSystemInfo((CollectSystemInfoCommand) command, taskId);
            case "PingCommand" -> handlePing((PingCommand) command, taskId);
            case "GetLogCommand" -> handleGetLog((GetLogCommand) command, taskId);
            case "CreateUnixUserCommand" -> handleCreateUnixUser(
                (com.datasophon.common.command.remote.CreateUnixUserCommand) command, taskId);
            case "CreateUnixGroupCommand" -> handleCreateUnixGroup(
                (com.datasophon.common.command.remote.CreateUnixGroupCommand) command, taskId);
            case "DelUnixUserCommand" -> handleDelUnixUser(
                (com.datasophon.common.command.remote.DelUnixUserCommand) command, taskId);
            case "DelUnixGroupCommand" -> handleDelUnixGroup(
                (com.datasophon.common.command.remote.DelUnixGroupCommand) command, taskId);
            case "GenerateKeytabFileCommand" -> handleGenerateKeytab(
                (com.datasophon.common.command.remote.GenerateKeytabFileCommand) command, taskId);
            default -> throw new UnsupportedOperationException(
                "Unsupported command type: " + commandType);
        };
    }

    // ==================== 临时处理方法（待Service层实现后替换） ====================
    
    private Object handleInstallService(InstallServiceRoleCommand command, String taskId) {
        logger.info("Handling install service command for task: {}", taskId);
        return installServiceService.install(command);
    }

    private Object handleServiceOperate(ServiceRoleOperateCommand command, String taskId) {
        logger.info("Handling service operate command for task: {}", taskId);
        String operateType = command.getCommandType();
        
        return switch (operateType) {
            case "START" -> serviceOperateService.start(command);
            case "STOP" -> serviceOperateService.stop(command);
            case "RESTART" -> serviceOperateService.restart(command);
            default -> {
                logger.warn("Unknown operate type: {}", operateType);
                ExecResult result = new ExecResult();
                result.setExecResult(false);
                result.setExecOut("Unknown operate type: " + operateType);
                yield result;
            }
        };
    }

    private Object handleGenerateConfig(GenerateServiceConfigCommand command, String taskId) {
        logger.info("Handling generate config command for task: {}", taskId);
        return configureServiceService.configure(command);
    }

    private Object handleExecuteCmd(ExecuteCmdCommand command, String taskId) {
        logger.info("Handling execute cmd command for task: {}", taskId);
        return executeCmdService.executeCmd(command);
    }

    private Object handleFileOperate(FileOperateCommand command, String taskId) {
        logger.info("Handling file operate command for task: {}", taskId);
        return fileOperateService.operateFile(command);
    }

    private Object handleCollectSystemInfo(CollectSystemInfoCommand command, String taskId) {
        logger.info("Handling collect system info command for task: {}", taskId);
        return systemInfoService.collectSystemInfo(command);
    }

    private Object handlePing(PingCommand command, String taskId) {
        logger.info("Handling ping command for task: {}", taskId);
        return pingService.ping(command);
    }

    private Object handleGetLog(GetLogCommand command, String taskId) {
        logger.info("Handling get log command for task: {}", taskId);
        return logService.getLog(command);
    }

    private Object handleCreateUnixUser(com.datasophon.common.command.remote.CreateUnixUserCommand command, String taskId) {
        logger.info("Handling create unix user command for task: {}", taskId);
        return unixUserService.createUser(command);
    }

    private Object handleCreateUnixGroup(com.datasophon.common.command.remote.CreateUnixGroupCommand command, String taskId) {
        logger.info("Handling create unix group command for task: {}", taskId);
        return unixGroupService.createGroup(command);
    }

    private Object handleDelUnixUser(com.datasophon.common.command.remote.DelUnixUserCommand command, String taskId) {
        logger.info("Handling delete unix user command for task: {}", taskId);
        return unixUserService.deleteUser(command);
    }

    private Object handleDelUnixGroup(com.datasophon.common.command.remote.DelUnixGroupCommand command, String taskId) {
        logger.info("Handling delete unix group command for task: {}", taskId);
        return unixGroupService.deleteGroup(command);
    }

    private Object handleGenerateKeytab(com.datasophon.common.command.remote.GenerateKeytabFileCommand command, String taskId) {
        logger.info("Handling generate keytab command for task: {}", taskId);
        // TODO: 创建KerberosService后实现
        ExecResult result = new ExecResult();
        result.setExecResult(true);
        result.setExecOut("Keytab generation placeholder");
        return result;
    }

    // ==================== 任务查询和管理方法 ====================

    /**
     * 获取任务信息
     */
    public TaskInfo getTask(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        TaskInfo taskInfo = tasks.get(taskId);
        if (taskInfo == null) {
            return false;
        }

        if ("PENDING".equals(taskInfo.getStatus()) || "RUNNING".equals(taskInfo.getStatus())) {
            taskInfo.setStatus("CANCELLED");
            taskInfo.setCompleteTime(LocalDateTime.now());
            
            sseManager.sendEventToTask(taskId, 
                new TaskStatusEvent(taskId, "CANCELLED", "Task cancelled"));
            sseManager.completeTask(taskId);
            
            logger.info("Task cancelled: {}", taskId);
            return true;
        }

        return false;
    }

    /**
     * 清理已完成的任务
     */
    public void cleanupCompletedTasks(int maxAgeMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        tasks.entrySet().removeIf(entry -> {
            TaskInfo task = entry.getValue();
            if (task.getCompleteTime() != null && task.getCompleteTime().isBefore(threshold)) {
                logger.debug("Cleaning up task: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}

