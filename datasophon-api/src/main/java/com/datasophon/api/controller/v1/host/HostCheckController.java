package com.datasophon.api.controller.v1.host;

import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.api.service.checker.AsyncCheckService;
import com.datasophon.api.service.checker.queue.HostCheckQueueManager;
import com.datasophon.api.service.checker.queue.QueueManagerServiceImpl;
import com.datasophon.common.model.QueueSystemStatus;
import com.datasophon.common.model.QueueTaskDetailResult;
import com.datasophon.common.model.QueueTaskInfo;
import com.datasophon.common.model.ScheduleConfigResult;
import com.datasophon.common.model.ScheduledTasksStatus;
import com.datasophon.api.vo.Result;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 主机检查控制器
 */
@Validated
@ApiVersion(path = "host/check")
@Slf4j
public class HostCheckController {

    @Autowired
    private HostCheckService hostCheckService;

    @Autowired
    private QueueManagerServiceImpl queueManagerService;

    @Autowired
    private HostCheckQueueManager hostCheckQueueManager;

    @Autowired
    private AsyncCheckService asyncCheckService;



    /**
     * 获取主机检查项列表
     */
    @GetMapping("/getHostCheckItems")
    @UserPermission
    public Result getHostCheckItems(@RequestParam(name = "ip") String ip,
            @RequestParam(name = "clusterId") Integer clusterId) {
        // 委托给服务层处理业务逻辑
        return hostCheckService.getHostCheckItems(ip, clusterId);
    }

    /**
     * 控制主机检查队列管理器
     * 用于控制队列管理器的运行状态和定时任务
     *
     * @param action    操作类型: status(获取状态), pause(暂停), resume(恢复), shutdown(关闭)
     *                  pauseTask(暂停定时任务), resumeTask(恢复定时任务),
     *                  cleanupConnections(清理连接)
     * @param scopeCode 作用范围: all(所有), queue(仅队列), scheduler(仅定时任务)，默认为all
     * @param taskId    定时任务ID，仅在pauseTask/resumeTask操作时需要
     */
    @GetMapping("/queueManager")
    @UserPermission
    public Result manageQueueManager(
            @RequestParam(name = "action") String action,
            @RequestParam(name = "scope", required = false, defaultValue = "all") String scopeCode,
            @RequestParam(name = "taskId", required = false) String taskId) {
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
            @RequestParam(name = "clusterId") Integer clusterId,
            @RequestParam(name = "ip", required = false, defaultValue = "-1") String ip) {
        log.info("收到终止主机检查请求，clusterId: {}, ip: {}", clusterId, ip);

        if (clusterId == null) {
            return Result.error("集群ID不能为空");
        }

        return hostCheckService.stopHostCheck(clusterId, ip);
    }

    /**
     * 终止单个检查项
     */
    @PostMapping("/stopCheckItem")
    @UserPermission
    public Result stopCheckItem(
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "itemId") Integer itemId) {
        return hostCheckService.stopItemCheck(clusterId, ip, itemId);
    }

    /**
     * 修复指定检查项
     */
    @PostMapping("/fixCheckItem")
    @UserPermission
    public Result fixCheckItem(
            @RequestParam("clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam("ip") String ip,
            @RequestParam("itemId") Integer itemId,
            @RequestParam(value = "skipConfirm", required = false, defaultValue = "false") Boolean skipConfirm) {
        return hostCheckService.fixCheckItem(clusterId, ip, itemId, skipConfirm);
    }

    /**
     * 修复选中的检查项
     */
    @PostMapping("/fixSelectedCheckItems")
    @UserPermission
    public Result fixSelectedCheckItems(
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "itemIds") String itemIds) {
        return hostCheckService.fixSelectedCheckItems(clusterId, ip, itemIds);
    }

    /**
     * 修复所有检查项
     */
    @PostMapping("/fixAllCheckItems")
    @UserPermission
    public Result fixAllCheckItems(
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam(name = "ip") String ip) {
        return hostCheckService.fixAllCheckItems(clusterId, ip);
    }

    /**
     * 批量检查主机
     * 前端可以在获取主机列表后调用此接口统一启动检查
     */
    @PostMapping("/batchCheckHosts")
    @UserPermission
    public Result batchCheckHosts(
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestBody List<String> ips) {
        return hostCheckService.batchCheckHosts(clusterId, ips);
    }

    /**
     * 重新进行主机环境校验
     * 注：从HostInstallController移动过来
     */
    @PostMapping("/rehostCheck")
    @UserPermission
    public Result rehostCheck(
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam(name = "ips") String ips,
            @RequestParam(name = "sshUser", required = false) String sshUser,
            @RequestParam(name = "sshPort", required = false) Integer sshPort) {
        // 将IP字符串转换为列表
        List<String> ipList = Arrays.asList(ips.split(","));
        return hostCheckService.batchCheckHosts(clusterId, ipList);
    }

    /**
     * 开始主机检查
     *
     * @param clusterId 集群ID
     * @return 开始检查结果
     */
    @PostMapping("/startHostCheck")
    @UserPermission
    public Result startHostCheck(@RequestParam(name = "clusterId") Integer clusterId) {
        return hostCheckService.startHostCheck(clusterId);
    }

    /**
     * 获取检查项的实时日志
     */
    @PostMapping("/getCheckItemLog")
    @UserPermission
    public Result getCheckItemLog(
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "itemId") Integer itemId) {
        return hostCheckService.getCheckItemLog(clusterId, ip, itemId);
    }

    /**
     * 重试指定检查项
     *
     * @param clusterId    集群ID
     * @param ip           主机IP
     * @param itemNamesStr 检查项名称列表，以逗号分隔
     * @return 操作结果
     */
    @PostMapping("/retryCheckItems")
    @UserPermission
    public Result retryCheckItems(
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "itemNames") String itemNamesStr) {
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
        log.info("retryCheckItems请求参数: clusterId={}, ip={}, itemNames={}, 解析后的itemIds={}",
                clusterId, ip, itemNamesStr, itemIds);

        return hostCheckService.retryCheckItems(clusterId, ip, itemIds);
    }

    /**
     * 统一的日志获取API
     */
    @PostMapping(value = "/getLog", produces = MediaType.APPLICATION_JSON_VALUE)
    @UserPermission
    public Result getLog(
            @RequestParam(name = "clusterId") Integer clusterId,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "itemId") Integer itemId,
            @RequestParam(name = "logType", required = false) String logType,
            @RequestParam(name = "logLevel", required = false) String logLevel,
            @RequestParam(name = "filterMode", required = false, defaultValue = "all") String filterMode) {
        return hostCheckService.getFormattedLog(clusterId, ip, itemId, logType, logLevel, filterMode);
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
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "itemId") Integer itemId) {
        return hostCheckService.skipCheckItem(clusterId, ip, itemId);
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
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "itemId") Integer itemId) {
        return hostCheckService.getCheckItemConfirmInfo(clusterId, ip, itemId);
    }

    /**
     * 获取异步服务状态
     *
     * @return 异步服务状态信息
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
            @RequestParam(name = "type") String type,
            @RequestParam(name = "intervalMs") long intervalMs) {
        try {
            boolean success;
            String message;

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
     * 修改定时任务执行间隔
     *
     * @param taskId          任务ID
     * @param intervalSeconds 执行间隔（秒）
     * @return 操作结果
     */
    @PostMapping("/updateTaskInterval")
    @UserPermission
    public Result updateTaskInterval(
            @RequestParam(name = "taskId") String taskId,
            @RequestParam(name = "intervalSeconds") int intervalSeconds) {
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
            @RequestParam(name = "intervalMs") long intervalMs) {
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
            @RequestParam(name = "intervalMs") long intervalMs) {
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
     * 更新主机名
     *
     * @param clusterId 集群ID
     * @param ip        主机IP
     * @param hostname  新主机名
     * @param syncHosts 是否同步更新hosts文件
     * @return 操作结果
     */
    @PostMapping("/updateHostname")
    @UserPermission
    public Result updateHostname(
            @RequestParam(name = "clusterId") Integer clusterId,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "hostname") String hostname,
            @RequestParam(name = "syncHosts", required = false, defaultValue = "false") Boolean syncHosts) {
        return hostCheckService.updateHostname(clusterId, ip, hostname, syncHosts);
    }

    /**
     * 更新hosts文件内容
     *
     * @param clusterId        集群ID
     * @param ip               主机IP
     * @param hostsFileContent hosts文件新内容
     * @return 操作结果
     */
    @PostMapping("/updateHostsFile")
    @UserPermission
    public Result updateHostsFile(
            @RequestParam(name = "clusterId") Integer clusterId,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "hostsFileContent") String hostsFileContent) {
        return hostCheckService.updateHostsFile(clusterId, ip, hostsFileContent);
    }

    /**
     * 生成hosts文件预览
     *
     * @param clusterId 集群ID
     * @param page      页码
     * @param pageSize  每页大小
     * @return 包含所有主机名和IP的预览内容
     */
    @GetMapping("/generateHostsFilePreview")
    @UserPermission
    public Result generateHostsFilePreview(@RequestParam(name = "clusterId") Integer clusterId,
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return hostCheckService.generateHostsFilePreview(clusterId, page, pageSize);
    }

    /**
     * 同步hosts文件到所有主机
     *
     * @param clusterId 集群ID
     * @return 操作结果
     */
    @PostMapping("/syncHostsFile")
    @UserPermission
    public Result syncHostsFile(@RequestParam(name = "clusterId") Integer clusterId) {
        return hostCheckService.syncHostsFile(clusterId);
    }

    /**
     * 批量设置主机名
     *
     * @param clusterId 集群ID
     * @param prefix    主机名前缀
     * @param zeroCount 中间0的位数
     * @param separator 分隔符
     * @param suffix    后缀
     * @return 操作结果
     */
    @PostMapping("/batchSetHostname")
    @UserPermission
    public Result batchSetHostname(
            @RequestParam(name = "clusterId") Integer clusterId,
            @RequestParam(name = "prefix") String prefix,
            @RequestParam(name = "zeroCount") Integer zeroCount,
            @RequestParam(name = "separator", required = false) String separator,
            @RequestParam(name = "suffix", required = false) String suffix) {
        return hostCheckService.batchSetHostname(clusterId, prefix, zeroCount, separator, suffix);
    }

    /**
     * 获取任务进度
     *
     * @param taskId 任务ID
     * @return 任务进度信息
     */
    @GetMapping("/getTaskProgress")
    @UserPermission
    public Result getTaskProgress(@RequestParam(name = "taskId") String taskId) {
        return hostCheckService.getTaskProgress(taskId);
    }

    /**
     * 修复集群中所有主机的所有失败项
     */
    @PostMapping("/fixAllFailedItems")
    @UserPermission
    public Result fixAllFailedItems(
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId) {
        log.info("收到修复所有失败项请求: clusterId={}", clusterId);
        return hostCheckService.fixAllFailedItems(clusterId);
    }

    /**
     * 跳过集群中所有主机的所有失败项
     */
    @PostMapping("/skipAllFailedItems")
    @UserPermission
    public Result skipAllFailedItems(
            @RequestParam(name = "clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId) {
        log.info("收到跳过所有失败项请求: clusterId={}", clusterId);
        return hostCheckService.skipAllFailedItems(clusterId);
    }

}
