package com.datasophon.api.controller;

import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.api.service.checker.QueueManagerService;
import com.datasophon.api.service.checker.impl.AsyncCheckService;
import com.datasophon.api.service.impl.HostCheckQueueManager;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.HostInfo;
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
import java.util.Map;

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
        // 从缓存中获取指定主机的检查项
        Map<String, HostInfo> hostInfoMap = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
        if (hostInfoMap == null || !hostInfoMap.containsKey(hostname)) {
            return Result.error("找不到主机信息: " + hostname);
        }
        
        HostInfo hostInfo = hostInfoMap.get(hostname);
        return Result.success(hostInfo.getCheckItems());
    }
    
    /**
     * 控制主机检查队列管理器
     * 用于控制队列管理器的运行状态和定时任务
     * @param action 操作类型: status(获取状态), pause(暂停), resume(恢复), shutdown(关闭)
     *               pauseTask(暂停定时任务), resumeTask(恢复定时任务), cleanupConnections(清理连接)
     * @param scope 作用范围: all(所有), queue(仅队列), scheduler(仅定时任务)，默认为all
     * @param taskId 定时任务ID，仅在pauseTask/resumeTask操作时需要
     */
    @GetMapping("/queueManager")
    @UserPermission
    public Result manageQueueManager(
            @RequestParam(value = "action") String action,
            @RequestParam(value = "scope", required = false, defaultValue = "all") String scopeCode,
            @RequestParam(value = "taskId", required = false) String taskId) {
        
        log.info("收到队列管理器控制请求: action={}, scope={}, taskId={}", action, scopeCode, taskId);
        
        // 根据action类型执行相应操作
        if ("status".equalsIgnoreCase(action)) {
            Result statusResult = queueManagerService.getQueueSystemStatus();
            
            // 如果status请求，同时获取任务队列详情
            if (statusResult.getCode() == 200) {
                try {
                    Result checkQueueResult = queueManagerService.getCheckQueueTasks();
                    Result fixQueueResult = queueManagerService.getFixQueueTasks();
                    
                    Map<String, Object> data = (Map<String, Object>) statusResult.getData();
                    
                    if (checkQueueResult.getCode() == 200) {
                        data.put("queueTasks", checkQueueResult.getData());
                    }
                    
                    if (fixQueueResult.getCode() == 200) {
                        data.put("fixQueueTasks", fixQueueResult.getData());
                    }
                } catch (Exception e) {
                    log.error("获取队列任务详情失败", e);
                }
            }
            
            return statusResult;
        } else if ("pauseTask".equalsIgnoreCase(action)) {
            if (taskId == null || taskId.isEmpty()) {
                return Result.error("暂停定时任务时需要提供taskId");
            }
            return queueManagerService.pauseScheduledTask(taskId);
        } else if ("resumeTask".equalsIgnoreCase(action)) {
            if (taskId == null || taskId.isEmpty()) {
                return Result.error("恢复定时任务时需要提供taskId");
            }
            return queueManagerService.resumeScheduledTask(taskId);
        } else if ("cleanupConnections".equalsIgnoreCase(action)) {
            return queueManagerService.cleanupConnections();
        } else {
            return queueManagerService.manageQueueSystem(action, scopeCode);
        }
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
                itemIds.add(itemNamesStr);
            }
        }
        
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
            Map<String, Object> status = asyncCheckService.getScheduledTasksStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取异步服务状态失败", e);
            return Result.error(500, "获取异步服务状态失败: " + e.getMessage());
        }
    }
} 