///*
// *  Licensed to the Apache Software Foundation (ASF) under one or more
// *  contributor license agreements.  See the NOTICE file distributed with
// *  this work for additional information regarding copyright ownership.
// *  The ASF licenses this file to You under the Apache License, Version 2.0
// *  (the "License"); you may not use this file except in compliance with
// *  the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// */
//
//package com.datasophon.api.controller.v1.host;
//
//import com.datasophon.api.security.UserPermission;
//import com.datasophon.api.service.HostCheckService;
//import com.datasophon.api.service.checker.queue.QueueManagerServiceImpl;
//import com.datasophon.common.model.QueueSystemStatus;
//import com.datasophon.common.model.QueueTaskDetailResult;
//import com.datasophon.common.model.QueueTaskInfo;
//import com.datasophon.api.vo.Result;
//import jakarta.validation.constraints.NotNull;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import com.datasophon.api.annotation.ApiVersion;
//import org.springframework.http.MediaType;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
///**
// * 主机检查控制器
// *
// * @author 任相鹏
// * @email 635887935@qq.com
// * @date 2024-12-19
// */
//@Validated
//@ApiVersion(path = "host/check")
//@Slf4j
//public class HostCheckController {
//
//    @Autowired
//    private HostCheckService hostCheckService;
//
////    @Autowired
////    private QueueManagerServiceImpl queueManagerService;
//
//    // @Autowired
//    // private HostCheckQueueManager hostCheckQueueManager;
//
//    // @Autowired
//    // private AsyncCheckService asyncCheckService;
//
//    /**
//     * 获取主机检查项列表
//     */
//    @GetMapping("/getHostCheckItems")
//    @UserPermission
//    public Result<Object> getHostCheckItems(@RequestParam(name = "ip") String ip,
//            @RequestParam(name = "clusterId") Long clusterId) {
//        // 委托给服务层处理业务逻辑
//        try {
//            var checkItems = hostCheckService.getHostCheckItems(ip, clusterId);
//            return Result.success(checkItems);
//        } catch (Exception e) {
//            log.error("获取主机检查项失败", e);
//            return Result.error("获取主机检查项失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 控制主机检查队列管理器
//     * 用于控制队列管理器的运行状态和定时任务
//     *
//     * @param action    操作类型: status(获取状态), pause(暂停), resume(恢复), shutdown(关闭)
//     *                  pauseTask(暂停定时任务), resumeTask(恢复定时任务),
//     *                  cleanupConnections(清理连接)
//     * @param scopeCode 作用范围: all(所有), queue(仅队列), scheduler(仅定时任务)，默认为all
//     * @param taskId    定时任务ID，仅在pauseTask/resumeTask操作时需要
//     */
//    @GetMapping("/queueManager")
//    @UserPermission
//    public Result<Object> manageQueueManager(
//            @RequestParam(name = "action") String action,
//            @RequestParam(name = "scope", required = false, defaultValue = "all") String scopeCode,
//            @RequestParam(name = "taskId", required = false) String taskId) {
//        log.info("收到队列管理器控制请求: action={}, scope={}, taskId={}", action, scopeCode, taskId);
//
//        // 委托给服务层处理所有业务逻辑
//        return queueManagerService.manageQueueManagerWithDetails(action, scopeCode, taskId);
//    }
//
//    /**
//     * 终止主机检查
//     */
//    @PostMapping("/stopHostCheck")
//    @UserPermission
//    public Result<String> stopHostCheck(
//            @RequestParam(name = "clusterId") Long clusterId,
//            @RequestParam(name = "ip", required = false, defaultValue = "-1") String ip) {
//        log.info("收到终止主机检查请求，clusterId: {}, ip: {}", clusterId, ip);
//
//        if (clusterId == null) {
//            return Result.error("集群ID不能为空");
//        }
//
//        try {
//            boolean success = hostCheckService.stopHostCheck(clusterId, ip);
//            return success ? Result.success("终止主机检查成功") : Result.error("终止主机检查失败");
//        } catch (Exception e) {
//            log.error("终止主机检查失败", e);
//            return Result.error("终止主机检查失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 终止单个检查项
//     */
//    @PostMapping("/stopCheckItem")
//    @UserPermission
//    public Result<String> stopCheckItem(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestParam(name = "ip") String ip,
//            @RequestParam(name = "itemId") Integer itemId) {
//        try {
//            boolean success = hostCheckService.stopItemCheck(clusterId, ip, itemId);
//            return success ? Result.success("终止检查项成功") : Result.error("终止检查项失败");
//        } catch (Exception e) {
//            log.error("终止检查项失败", e);
//            return Result.error("终止检查项失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 修复指定检查项
//     */
//    @PostMapping("/fixCheckItem")
//    @UserPermission
//    public Result<String> fixCheckItem(
//            @ClusterId @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestParam("ip") String ip,
//            @RequestParam("itemId") Integer itemId,
//            @RequestParam(value = "skipConfirm", required = false, defaultValue = "false") Boolean skipConfirm) {
//        try {
//            boolean success = hostCheckService.fixCheckItem(clusterId, ip, itemId, skipConfirm);
//            return success ? Result.success("修复检查项成功") : Result.error("修复检查项失败");
//        } catch (Exception e) {
//            log.error("修复检查项失败", e);
//            return Result.error("修复检查项失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 修复选中的检查项
//     */
//    @PostMapping("/fixSelectedCheckItems")
//    @UserPermission
//    public Result<String> fixSelectedCheckItems(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestParam(name = "ip") String ip,
//            @RequestParam(name = "itemIds") String itemIds) {
//        try {
//            boolean success = hostCheckService.fixSelectedCheckItems(clusterId, ip, itemIds);
//            return success ? Result.success("修复选中检查项成功") : Result.error("修复选中检查项失败");
//        } catch (Exception e) {
//            log.error("修复选中检查项失败", e);
//            return Result.error("修复选中检查项失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 修复所有检查项
//     */
//    @PostMapping("/fixAllCheckItems")
//    @UserPermission
//    public Result<String> fixAllCheckItems(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestParam(name = "ip") String ip) {
//        try {
//            boolean success = hostCheckService.fixAllCheckItems(clusterId, ip);
//            return success ? Result.success("修复所有检查项成功") : Result.error("修复所有检查项失败");
//        } catch (Exception e) {
//            log.error("修复所有检查项失败", e);
//            return Result.error("修复所有检查项失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 批量检查主机
//     * 前端可以在获取主机列表后调用此接口统一启动检查
//     */
//    @PostMapping("/batchCheckHosts")
//    @UserPermission
//    public Result<String> batchCheckHosts(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestBody List<String> ips) {
//        try {
//            boolean success = hostCheckService.batchCheckHosts(clusterId, ips);
//            return success ? Result.success("批量检查主机启动成功") : Result.error("批量检查主机启动失败");
//        } catch (Exception e) {
//            log.error("批量检查主机失败", e);
//            return Result.error("批量检查主机失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 重新进行主机环境校验
//     * 注：从HostInstallController移动过来
//     */
//    @PostMapping("/rehostCheck")
//    @UserPermission
//    public Result<String> rehostCheck(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestParam(name = "ips") String ips,
//            @RequestParam(name = "sshUser", required = false) String sshUser,
//            @RequestParam(name = "sshPort", required = false) Integer sshPort) {
//        // 将IP字符串转换为列表
//        List<String> ipList = Arrays.asList(ips.split(","));
//        try {
//            boolean success = hostCheckService.batchCheckHosts(clusterId, ipList);
//            return success ? Result.success("重新检查主机启动成功") : Result.error("重新检查主机启动失败");
//        } catch (Exception e) {
//            log.error("重新检查主机失败", e);
//            return Result.error("重新检查主机失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 开始主机检查
//     *
//     * @param clusterId 集群ID
//     * @return 开始检查结果
//     */
//    @PostMapping("/startHostCheck")
//    @UserPermission
//    public Result<String> startHostCheck(@RequestParam(name = "clusterId") Long clusterId) {
//        try {
//            boolean success = hostCheckService.startHostCheck(clusterId);
//            return success ? Result.success("开始主机检查成功") : Result.error("开始主机检查失败");
//        } catch (Exception e) {
//            log.error("开始主机检查失败", e);
//            return Result.error("开始主机检查失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取检查项的实时日志
//     */
//    @PostMapping("/getCheckItemLog")
//    @UserPermission
//    public Result<Object> getCheckItemLog(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestParam(name = "ip") String ip,
//            @RequestParam(name = "itemId") Integer itemId) {
//        try {
//            var logEntries = hostCheckService.getCheckItemLog(clusterId, ip, itemId);
//            return Result.success(logEntries);
//        } catch (Exception e) {
//            log.error("获取检查项日志失败", e);
//            return Result.error("获取检查项日志失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 重试指定检查项
//     *
//     * @param clusterId    集群ID
//     * @param ip           主机IP
//     * @param itemNamesStr 检查项名称列表，以逗号分隔
//     * @return 操作结果
//     */
//    @PostMapping("/retryCheckItems")
//    @UserPermission
//    public Result<String> retryCheckItems(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestParam(name = "ip") String ip,
//            @RequestParam(name = "itemNames") String itemNamesStr) {
//        if (clusterId == null) {
//            return Result.error("集群ID不能为空");
//        }
//
//        // 将itemNames字符串转换为列表
//        List<String> itemIds = new ArrayList<>();
//        if (itemNamesStr != null && !itemNamesStr.isEmpty()) {
//            // 处理可能的多值情况（如果前端发送了数组）
//            if (itemNamesStr.contains(",")) {
//                // 如果是逗号分隔的字符串
//                String[] items = itemNamesStr.split(",");
//                for (String item : items) {
//                    itemIds.add(item.trim());
//                }
//            } else {
//                // 单个值
//                itemIds = Collections.singletonList(itemNamesStr.trim());
//            }
//        }
//
//        // 记录详细日志，帮助诊断
//        log.info("retryCheckItems请求参数: clusterId={}, ip={}, itemNames={}, 解析后的itemIds={}",
//                clusterId, ip, itemNamesStr, itemIds);
//
//        try {
//            boolean success = hostCheckService.retryCheckItems(clusterId, ip, itemIds);
//            return success ? Result.success("重试检查项成功") : Result.error("重试检查项失败");
//        } catch (Exception e) {
//            log.error("重试检查项失败", e);
//            return Result.error("重试检查项失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 统一的日志获取API
//     */
//    @PostMapping(value = "/getLog", produces = MediaType.APPLICATION_JSON_VALUE)
//    @UserPermission
//    public Result<String> getLog(
//            @RequestParam(name = "clusterId") Long clusterId,
//            @RequestParam(name = "ip") String ip,
//            @RequestParam(name = "itemId") Integer itemId,
//            @RequestParam(name = "logType", required = false) String logType,
//            @RequestParam(name = "logLevel", required = false) String logLevel,
//            @RequestParam(name = "filterMode", required = false, defaultValue = "all") String filterMode) {
//        try {
//            String formattedLog = hostCheckService.getFormattedLog(clusterId, ip, itemId, logType, logLevel,
//                    filterMode);
//            return Result.success(formattedLog);
//        } catch (Exception e) {
//            log.error("获取格式化日志失败", e);
//            return Result.error("获取格式化日志失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取可用的日志级别
//     *
//     * @return 日志级别列表
//     */
//    @GetMapping("/log-levels")
//    @UserPermission
//    public Result<Object> getLogLevels() {
//        return Result.success(hostCheckService.getLogLevels());
//    }
//
//    /**
//     * 跳过指定检查项
//     */
//    @PostMapping("/skipCheckItem")
//    @UserPermission
//    public Result<String> skipCheckItem(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestParam(name = "ip") String ip,
//            @RequestParam(name = "itemId") Integer itemId) {
//        try {
//            boolean success = hostCheckService.skipCheckItem(clusterId, ip, itemId);
//            return success ? Result.success("跳过检查项成功") : Result.error("跳过检查项失败");
//        } catch (Exception e) {
//            log.error("跳过检查项失败", e);
//            return Result.error("跳过检查项失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取可用的日志类型
//     *
//     * @return 日志类型列表
//     */
//    @GetMapping("/log-types")
//    @UserPermission
//    public Result<Object> getLogTypes() {
//        return Result.success(hostCheckService.getLogTypes());
//    }
//
//    /**
//     * 获取检查项的确认信息
//     */
//    @GetMapping("/getCheckItemConfirmInfo")
//    @UserPermission
//    public Result<Object> getCheckItemConfirmInfo(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId,
//            @RequestParam(name = "ip") String ip,
//            @RequestParam(name = "itemId") Integer itemId) {
//        try {
//            var confirmInfo = hostCheckService.getCheckItemConfirmInfo(clusterId, ip, itemId);
//            return Result.success(confirmInfo);
//        } catch (Exception e) {
//            log.error("获取检查项确认信息失败", e);
//            return Result.error("获取检查项确认信息失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取异步服务状态
//     *
//     * @return 异步服务状态信息
//     */
//    // @UserPermission
//    // @GetMapping("/asyncService/status")
//    // public Result getAsyncServiceStatus() {
//    // try {
//    // ScheduledTasksStatus status = asyncCheckService.getScheduledTasksStatus();
//    // return Result.success(status);
//    // } catch (Exception e) {
//    // log.error("获取异步服务状态失败", e);
//    // return Result.error(500, "获取异步服务状态失败: " + e.getMessage());
//    // }
//    // }
//
//    /**
//     * 配置定时任务执行间隔
//     * 注：AsyncCheckService已被注释，暂时停用此功能
//     *
//     * @param type       任务类型：taskCleanup或connectionCleanup
//     * @param intervalMs 执行间隔（毫秒）
//     * @return 操作结果
//     */
//    // @UserPermission
//    // @PostMapping("/asyncService/schedule")
//    // public Result configureScheduledTask(
//    // @RequestParam(name = "type") String type,
//    // @RequestParam(name = "intervalMs") long intervalMs) {
//    // try {
//    // boolean success;
//    // String message;
//    //
//    // if ("taskCleanup".equals(type)) {
//    // success = asyncCheckService.setTaskCleanupInterval(intervalMs);
//    // message = success ? "任务清理定时任务间隔设置成功，当前间隔: " + (intervalMs / 1000) + "秒" :
//    // "间隔时间不能小于1秒";
//    // } else if ("connectionCleanup".equals(type)) {
//    // success = asyncCheckService.setConnectionCleanupInterval(intervalMs);
//    // message = success ? "连接清理定时任务间隔设置成功，当前间隔: " + (intervalMs / 1000) + "秒" :
//    // "间隔时间不能小于1秒";
//    // } else {
//    // return Result.error("不支持的任务类型: " + type);
//    // }
//    //
//    // if (success) {
//    // // 返回最新状态
//    // ScheduleConfigResult resultData = new ScheduleConfigResult();
//    // resultData.setMessage(message);
//    // resultData.setStatus(asyncCheckService.getScheduledTasksStatus());
//    //
//    // // 添加当前设置的值到结果中
//    // resultData.setCurrentIntervalMs(intervalMs);
//    // resultData.setCurrentIntervalSeconds(intervalMs / 1000);
//    //
//    // return Result.success(resultData);
//    // } else {
//    // return Result.error(message);
//    // }
//    // } catch (Exception e) {
//    // log.error("配置定时任务失败", e);
//    // return Result.error("配置定时任务失败: " + e.getMessage());
//    // }
//    // }
//
//    /**
//     * 修改定时任务执行间隔
//     *
//     * @param taskId          任务ID
//     * @param intervalSeconds 执行间隔（秒）
//     * @return 操作结果
//     */
//    @PostMapping("/updateTaskInterval")
//    @UserPermission
//    public Result<Object> updateTaskInterval(
//            @RequestParam(name = "taskId") String taskId,
//            @RequestParam(name = "intervalSeconds") int intervalSeconds) {
//        try {
//            // 将秒转换为毫秒
//            long intervalMs = intervalSeconds * 1000L;
//
//            if (intervalSeconds <= 0) {
//                return Result.error("执行间隔必须大于0秒");
//            }
//
//            return (Result<Object>) queueManagerService.updateTaskInterval(taskId, intervalMs);
//        } catch (Exception e) {
//            log.error("修改定时任务执行间隔失败", e);
//            return Result.error("修改执行间隔失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 更新队列健康监控任务间隔
//     *
//     * @param intervalMs 执行间隔（毫秒）
//     * @return 操作结果
//     */
//    @PostMapping("/updateQueueHealthMonitorInterval")
//    @UserPermission
//    public Result<Object> updateQueueHealthMonitorInterval(
//            @RequestParam(name = "intervalMs") long intervalMs) {
//        try {
//            if (intervalMs <= 0) {
//                return Result.error("执行间隔必须大于0毫秒");
//            }
//
//            // 调用队列管理服务更新间隔
//            return (Result<Object>) queueManagerService.updateTaskInterval("queueHealthMonitor", intervalMs);
//        } catch (Exception e) {
//            log.error("更新队列健康监控间隔失败", e);
//            return Result.error("更新间隔失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 更新任务超时监控任务间隔
//     *
//     * @param intervalMs 执行间隔（毫秒）
//     * @return 操作结果
//     */
//    @PostMapping("/updateTaskTimeoutMonitorInterval")
//    @UserPermission
//    public Result<Object> updateTaskTimeoutMonitorInterval(
//            @RequestParam(name = "intervalMs") long intervalMs) {
//        try {
//            if (intervalMs <= 0) {
//                return Result.error("执行间隔必须大于0毫秒");
//            }
//
//            // 调用队列管理服务更新间隔
//            return (Result<Object>) queueManagerService.updateTaskInterval("taskTimeoutMonitor", intervalMs);
//        } catch (Exception e) {
//            log.error("更新任务超时监控间隔失败", e);
//            return Result.error("更新间隔失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取队列系统详情（包含所有状态和任务信息）
//     * 用于详情页面，一次性获取所有数据
//     */
//    @GetMapping("/queueSystemDetails")
//    @UserPermission
//    public Result<Object> getQueueSystemDetails() {
//        try {
//            // 获取队列系统状态
//            QueueSystemStatus queueSystemStatus = queueManagerService.getQueueSystemStatusDirect();
//
//            // 获取队列任务详情
//            List<QueueTaskInfo> checkQueueTasks = queueManagerService.getCheckQueueTasksDirect();
//            List<QueueTaskInfo> fixQueueTasks = queueManagerService.getFixQueueTasksDirect();
//
//            // 创建详细结果对象
//            QueueTaskDetailResult result = new QueueTaskDetailResult();
//            result.setQueueManager(queueSystemStatus.getQueueManager());
//            result.setAsyncService(queueSystemStatus.getAsyncService());
//            result.setQueueTasks(checkQueueTasks);
//            result.setFixQueueTasks(fixQueueTasks);
//
//            return Result.success(result);
//        } catch (Exception e) {
//            log.error("获取队列系统详情失败", e);
//            return Result.error("获取队列系统详情失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 更新主机名
//     *
//     * @param clusterId 集群ID
//     * @param ip        主机IP
//     * @param hostname  新主机名
//     * @param syncHosts 是否同步更新hosts文件
//     * @return 操作结果
//     */
//    @PostMapping("/updateHostname")
//    @UserPermission
//    public Result<String> updateHostname(
//            @RequestParam(name = "clusterId") Long clusterId,
//            @RequestParam(name = "ip") String ip,
//            @RequestParam(name = "hostname") String hostname,
//            @RequestParam(name = "syncHosts", required = false, defaultValue = "false") Boolean syncHosts) {
//        try {
//            boolean success = hostCheckService.updateHostname(clusterId, ip, hostname, syncHosts);
//            return success ? Result.success("更新主机名成功") : Result.error("更新主机名失败");
//        } catch (Exception e) {
//            log.error("更新主机名失败", e);
//            return Result.error("更新主机名失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 更新hosts文件内容
//     *
//     * @param clusterId        集群ID
//     * @param ip               主机IP
//     * @param hostsFileContent hosts文件新内容
//     * @return 操作结果
//     */
//    @PostMapping("/updateHostsFile")
//    @UserPermission
//    public Result<String> updateHostsFile(
//            @RequestParam(name = "clusterId") Long clusterId,
//            @RequestParam(name = "ip") String ip,
//            @RequestParam(name = "hostsFileContent") String hostsFileContent) {
//        try {
//            boolean success = hostCheckService.updateHostsFile(clusterId, ip, hostsFileContent);
//            return success ? Result.success("更新hosts文件成功") : Result.error("更新hosts文件失败");
//        } catch (Exception e) {
//            log.error("更新hosts文件失败", e);
//            return Result.error("更新hosts文件失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 生成hosts文件预览
//     *
//     * @param clusterId 集群ID
//     * @param page      页码
//     * @param pageSize  每页大小
//     * @return 包含所有主机名和IP的预览内容
//     */
//    @GetMapping("/generateHostsFilePreview")
//    @UserPermission
//    public Result<String> generateHostsFilePreview(@RequestParam(name = "clusterId") Long clusterId,
//            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
//            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
//        try {
//            String preview = hostCheckService.generateHostsFilePreview(clusterId, page, pageSize);
//            return Result.success(preview);
//        } catch (Exception e) {
//            log.error("生成hosts文件预览失败", e);
//            return Result.error("生成hosts文件预览失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 同步hosts文件到所有主机
//     *
//     * @param clusterId 集群ID
//     * @return 操作结果
//     */
//    @PostMapping("/syncHostsFile")
//    @UserPermission
//    public Result<String> syncHostsFile(@RequestParam(name = "clusterId") Long clusterId) {
//        try {
//            boolean success = hostCheckService.syncHostsFile(clusterId);
//            return success ? Result.success("同步hosts文件成功") : Result.error("同步hosts文件失败");
//        } catch (Exception e) {
//            log.error("同步hosts文件失败", e);
//            return Result.error("同步hosts文件失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 批量设置主机名
//     *
//     * @param clusterId 集群ID
//     * @param prefix    主机名前缀
//     * @param zeroCount 中间0的位数
//     * @param separator 分隔符
//     * @param suffix    后缀
//     * @return 操作结果
//     */
//    @PostMapping("/batchSetHostname")
//    @UserPermission
//    public Result<String> batchSetHostname(
//            @RequestParam(name = "clusterId") Long clusterId,
//            @RequestParam(name = "prefix") String prefix,
//            @RequestParam(name = "zeroCount") Integer zeroCount,
//            @RequestParam(name = "separator", required = false) String separator,
//            @RequestParam(name = "suffix", required = false) String suffix) {
//        try {
//            boolean success = hostCheckService.batchSetHostname(clusterId, prefix, zeroCount, separator, suffix);
//            return success ? Result.success("批量设置主机名成功") : Result.error("批量设置主机名失败");
//        } catch (Exception e) {
//            log.error("批量设置主机名失败", e);
//            return Result.error("批量设置主机名失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取任务进度
//     *
//     * @param taskId 任务ID
//     * @return 任务进度信息
//     */
//    @GetMapping("/getTaskProgress")
//    @UserPermission
//    public Result<Object> getTaskProgress(@RequestParam(name = "taskId") String taskId) {
//        try {
//            var taskProgress = hostCheckService.getTaskProgress(taskId);
//            return Result.success(taskProgress);
//        } catch (Exception e) {
//            log.error("获取任务进度失败", e);
//            return Result.error("获取任务进度失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 修复集群中所有主机的所有失败项
//     */
//    @PostMapping("/fixAllFailedItems")
//    @UserPermission
//    public Result<String> fixAllFailedItems(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId) {
//        log.info("收到修复所有失败项请求: clusterId={}", clusterId);
//        try {
//            boolean success = hostCheckService.fixAllFailedItems(clusterId);
//            return success ? Result.success("修复所有失败项成功") : Result.error("修复所有失败项失败");
//        } catch (Exception e) {
//            log.error("修复所有失败项失败", e);
//            return Result.error("修复所有失败项失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 跳过集群中所有主机的所有失败项
//     */
//    @PostMapping("/skipAllFailedItems")
//    @UserPermission
//    public Result<String> skipAllFailedItems(
//            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Long clusterId) {
//        log.info("收到跳过所有失败项请求: clusterId={}", clusterId);
//        try {
//            boolean success = hostCheckService.skipAllFailedItems(clusterId);
//            return success ? Result.success("跳过所有失败项成功") : Result.error("跳过所有失败项失败");
//        } catch (Exception e) {
//            log.error("跳过所有失败项失败", e);
//            return Result.error("跳过所有失败项失败: " + e.getMessage());
//        }
//    }
//
//}
