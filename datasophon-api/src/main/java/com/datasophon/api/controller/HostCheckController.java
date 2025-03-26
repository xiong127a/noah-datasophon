package com.datasophon.api.controller;

import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.api.service.checker.queue.QueueManagerService;
import com.datasophon.api.service.checker.AsyncCheckService;
import com.datasophon.api.service.checker.queue.HostCheckQueueManager;
import com.datasophon.common.model.ScheduledTasksStatus;
import com.datasophon.common.model.ScheduleConfigResult;
import com.datasophon.common.model.QueueSystemStatus;
import com.datasophon.common.model.QueueTaskInfo;
import com.datasophon.common.model.QueueTaskDetailResult;
import com.datasophon.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 主机检查控制器
 */
@Validated
@RestController
@RequestMapping("/host/check")
@Slf4j
public class HostCheckController {

    @Autowired
    private HostCheckService hostCheckService;

    @Autowired
    private QueueManagerService queueManagerService;

    @Autowired
    private HostCheckQueueManager hostCheckQueueManager;

    @Autowired
    private AsyncCheckService asyncCheckService;

    /**
     * 获取主机检查项列表
     */
    @GetMapping("/getHostCheckItems")
    @UserPermission
    public Result getHostCheckItems(@RequestParam String hostname, @RequestParam Integer clusterId) {
        // 委托给服务层处理业务逻辑
        return hostCheckService.getHostCheckItems(hostname, clusterId);
    }

    /**
     * 控制主机检查队列管理器
     * 用于控制队列管理器的运行状态和定时任务
     * 
     * @param action 操作类型: status(获取状态), pause(暂停), resume(恢复), shutdown(关闭)
     *               pauseTask(暂停定时任务), resumeTask(恢复定时任务), cleanupConnections(清理连接)
     * @param scopeCode  作用范围: all(所有), queue(仅队列), scheduler(仅定时任务)，默认为all
     * @param taskId 定时任务ID，仅在pauseTask/resumeTask操作时需要
     */
    @GetMapping("/queueManager")
    @UserPermission
    public Result manageQueueManager(
            @RequestParam(value = "action") String action,
            @RequestParam(value = "scope", required = false, defaultValue = "all") String scopeCode,
            @RequestParam(value = "taskId", required = false) String taskId) {

        log.info("收到队列管理器控制请求: action={}, scope={}, taskId={}", action, scopeCode, taskId);

        // 委托给服务层处理所有业务逻辑
        return queueManagerService.manageQueueManagerWithDetails(action, scopeCode, taskId);
    }

    /**
     * 终止主机检查
     */
    @PostMapping("/stopHostCheck")
    @UserPermission
    public Result stopHostCheck(
            @RequestParam @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam String hostname) {
        return hostCheckService.stopHostCheck(clusterId, hostname);
    }

    /**
     * 终止单个检查项
     */
    @PostMapping("/stopCheckItem")
    @UserPermission
    public Result stopCheckItem(
            @RequestParam @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam String hostname,
            @RequestParam Integer itemId) {
        return hostCheckService.stopItemCheck(clusterId, hostname, itemId);
    }

    /**
     * 修复指定检查项
     */
    @PostMapping("/fixCheckItem")
    @UserPermission
    public Result fixCheckItem(
            @RequestParam("clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam("hostname") String hostname,
            @RequestParam("itemId") Integer itemId,
            @RequestParam(value = "skipConfirm", required = false, defaultValue = "false") Boolean skipConfirm) {
        return hostCheckService.fixCheckItem(clusterId, hostname, itemId, skipConfirm);
    }

    /**
     * 修复选中的检查项
     */
    @PostMapping("/fixSelectedCheckItems")
    @UserPermission
    public Result fixSelectedCheckItems(
            @RequestParam @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam String hostname,
            @RequestParam String itemIds) {
        return hostCheckService.fixSelectedCheckItems(clusterId, hostname, itemIds);
    }

    /**
     * 修复所有检查项
     */
    @PostMapping("/fixAllCheckItems")
    @UserPermission
    public Result fixAllCheckItems(
            @RequestParam @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam String hostname) {
        return hostCheckService.fixAllCheckItems(clusterId, hostname);
    }

    /**
     * 批量检查主机
     * 前端可以在获取主机列表后调用此接口统一启动检查
     */
    @PostMapping("/batchCheckHosts")
    @UserPermission
    public Result batchCheckHosts(
            @RequestParam @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestBody List<String> hostnames) {
        return hostCheckService.batchCheckHosts(clusterId, hostnames);
    }

    /**
     * 重新进行主机环境校验
     * 注：从HostInstallController移动过来
     */
    @PostMapping("/rehostCheck")
    @UserPermission
    public Result rehostCheck(
            @RequestParam @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam String hostnames,
            @RequestParam(required = false) String sshUser,
            @RequestParam(required = false) Integer sshPort) {
        // 将主机名字符串转换为列表
        List<String> hostnameList = Arrays.asList(hostnames.split(","));
        return hostCheckService.batchCheckHosts(clusterId, hostnameList);
    }

    /**
     * 获取检查项的实时日志
     */
    @PostMapping("/getCheckItemLog")
    @UserPermission
    public Result getCheckItemLog(
            @RequestParam("clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam("hostname") String hostname,
            @RequestParam("itemId") Integer itemId) {
        return hostCheckService.getCheckItemLog(clusterId, hostname, itemId);
    }

    /**
     * 重试指定的检查项
     */
    @PostMapping("/retryCheckItems")
    @UserPermission
    public Result retryCheckItems(
            @RequestParam("clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam("hostname") String hostname,
            @RequestParam("itemNames") String itemNamesStr) {

        if (clusterId == null) {
            return Result.error("集群ID不能为空");
        }

        // 将itemNames字符串转换为列表
        List<String> itemIds = new ArrayList<>();
        if (itemNamesStr != null && !itemNamesStr.isEmpty()) {
            // 处理可能的多值情况（如果前端发送了数组）
            if (itemNamesStr.contains(",")) {
                // 如果是逗号分隔的字符串
                String[] items = itemNamesStr.split(",");
                for (String item : items) {
                    itemIds.add(item.trim());
                }
            } else {
                // 单个值
                itemIds.add(itemNamesStr.trim());
            }
        }

        // 记录详细日志，帮助诊断
        log.info("retryCheckItems请求参数: clusterId={}, hostname={}, itemNames={}, 解析后的itemIds={}",
                clusterId, hostname, itemNamesStr, itemIds);

        return hostCheckService.retryCheckItems(clusterId, hostname, itemIds);
    }

    /**
     * 统一的日志获取API
     */
    @PostMapping(value = "/getLog", produces = MediaType.APPLICATION_JSON_VALUE)
    @UserPermission
    public Result getLog(
            @RequestParam Integer clusterId,
            @RequestParam String hostname,
            @RequestParam Integer itemId,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false, defaultValue = "all") String filterMode) {
        return hostCheckService.getFormattedLog(clusterId, hostname, itemId, logType, logLevel, filterMode);
    }

    /**
     * 获取可用的日志级别
     * 
     * @return 日志级别列表
     */
    @GetMapping("/log-levels")
    @UserPermission
    public Result getLogLevels() {
        return Result.success(hostCheckService.getLogLevels());
    }

    /**
     * 跳过指定检查项
     */
    @PostMapping("/skipCheckItem")
    @UserPermission
    public Result skipCheckItem(
            @RequestParam("clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam("hostname") String hostname,
            @RequestParam("itemId") Integer itemId) {
        return hostCheckService.skipCheckItem(clusterId, hostname, itemId);
    }

    /**
     * 获取可用的日志类型
     * 
     * @return 日志类型列表
     */
    @GetMapping("/log-types")
    @UserPermission
    public Result getLogTypes() {
        return Result.success(hostCheckService.getLogTypes());
    }

    /**
     * 获取检查项的确认信息
     */
    @GetMapping("/getCheckItemConfirmInfo")
    @UserPermission
    public Result getCheckItemConfirmInfo(
            @RequestParam("clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam("hostname") String hostname,
            @RequestParam("itemId") Integer itemId) {
        return hostCheckService.getCheckItemConfirmInfo(clusterId, hostname, itemId);
    }

    /**
     * 获取异步服务状态
     */
    @UserPermission
    @GetMapping("/asyncService/status")
    public Result getAsyncServiceStatus() {
        try {
            ScheduledTasksStatus status = asyncCheckService.getScheduledTasksStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取异步服务状态失败", e);
            return Result.error(500, "获取异步服务状态失败: " + e.getMessage());
        }
    }

    /**
     * 配置定时任务执行间隔
     * 
     * @param type       任务类型：taskCleanup或connectionCleanup
     * @param intervalMs 执行间隔（毫秒）
     * @return 操作结果
     */
    @UserPermission
    @PostMapping("/asyncService/schedule")
    public Result configureScheduledTask(
            @RequestParam("type") String type,
            @RequestParam("intervalMs") long intervalMs) {
        try {
            boolean success = false;
            String message = "";

            if ("taskCleanup".equals(type)) {
                success = asyncCheckService.setTaskCleanupInterval(intervalMs);
                message = success ? "任务清理定时任务间隔设置成功，当前间隔: " + (intervalMs / 1000) + "秒" : "间隔时间不能小于1秒";
            } else if ("connectionCleanup".equals(type)) {
                success = asyncCheckService.setConnectionCleanupInterval(intervalMs);
                message = success ? "连接清理定时任务间隔设置成功，当前间隔: " + (intervalMs / 1000) + "秒" : "间隔时间不能小于1秒";
            } else {
                return Result.error("不支持的任务类型: " + type);
            }

            if (success) {
                // 返回最新状态
                ScheduleConfigResult resultData = new ScheduleConfigResult();
                resultData.setMessage(message);
                resultData.setStatus(asyncCheckService.getScheduledTasksStatus());

                // 添加当前设置的值到结果中
                resultData.setCurrentIntervalMs(intervalMs);
                resultData.setCurrentIntervalSeconds(intervalMs / 1000);

                return Result.success(resultData);
            } else {
                return Result.error(message);
            }
        } catch (Exception e) {
            log.error("配置定时任务失败", e);
            return Result.error("配置定时任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取队列系统详情（包含所有状态和任务信息）
     * 用于详情页面，一次性获取所有数据
     */
    @GetMapping("/queueSystemDetails")
    @UserPermission
    public Result getQueueSystemDetails() {
        try {
            // 获取队列系统状态
            QueueSystemStatus queueSystemStatus = queueManagerService.getQueueSystemStatusDirect();

            // 获取队列任务详情
            List<QueueTaskInfo> checkQueueTasks = queueManagerService.getCheckQueueTasksDirect();
            List<QueueTaskInfo> fixQueueTasks = queueManagerService.getFixQueueTasksDirect();

            // 创建详细结果对象
            QueueTaskDetailResult result = new QueueTaskDetailResult();
            result.setQueueManager(queueSystemStatus.getQueueManager());
            result.setAsyncService(queueSystemStatus.getAsyncService());
            result.setQueueTasks(checkQueueTasks);
            result.setFixQueueTasks(fixQueueTasks);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取队列系统详情失败", e);
            return Result.error("获取队列系统详情失败: " + e.getMessage());
        }
    }

    /**
     * 修改定时任务执行间隔
     * 
     * @param taskId          任务ID
     * @param intervalSeconds 执行间隔（秒）
     * @return 操作结果
     */
    @PostMapping("/updateTaskInterval")
    @UserPermission
    public Result updateTaskInterval(
            @RequestParam("taskId") String taskId,
            @RequestParam("intervalSeconds") int intervalSeconds) {
        try {
            // 将秒转换为毫秒
            long intervalMs = intervalSeconds * 1000L;

            if (intervalSeconds <= 0) {
                return Result.error("执行间隔必须大于0秒");
            }

            return queueManagerService.updateTaskInterval(taskId, intervalMs);
        } catch (Exception e) {
            log.error("修改定时任务执行间隔失败", e);
            return Result.error("修改执行间隔失败: " + e.getMessage());
        }
    }

    /**
     * 更新队列健康监控任务间隔
     * 
     * @param intervalMs 执行间隔（毫秒）
     * @return 操作结果
     */
    @PostMapping("/updateQueueHealthMonitorInterval")
    @UserPermission
    public Result updateQueueHealthMonitorInterval(
            @RequestParam("intervalMs") long intervalMs) {
        try {
            if (intervalMs <= 0) {
                return Result.error("执行间隔必须大于0毫秒");
            }

            // 调用队列管理服务更新间隔
            return queueManagerService.updateTaskInterval("queueHealthMonitor", intervalMs);
        } catch (Exception e) {
            log.error("更新队列健康监控间隔失败", e);
            return Result.error("更新间隔失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务超时监控任务间隔
     * 
     * @param intervalMs 执行间隔（毫秒）
     * @return 操作结果
     */
    @PostMapping("/updateTaskTimeoutMonitorInterval")
    @UserPermission
    public Result updateTaskTimeoutMonitorInterval(
            @RequestParam("intervalMs") long intervalMs) {
        try {
            if (intervalMs <= 0) {
                return Result.error("执行间隔必须大于0毫秒");
            }

            // 调用队列管理服务更新间隔
            return queueManagerService.updateTaskInterval("taskTimeoutMonitor", intervalMs);
        } catch (Exception e) {
            log.error("更新任务超时监控间隔失败", e);
            return Result.error("更新间隔失败: " + e.getMessage());
        }
    }
}