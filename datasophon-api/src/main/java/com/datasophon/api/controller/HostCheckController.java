package com.datasophon.api.controller;

import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.LogEntry;
import com.datasophon.common.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
            @RequestParam @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam String hostname,
            @RequestParam Integer itemId) {
        return hostCheckService.fixCheckItem(clusterId, hostname, itemId);
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
    @PostMapping("/getLog")
    @UserPermission
    public Result getLog(
            @RequestParam Integer clusterId,
            @RequestParam String hostname,
            @RequestParam Integer itemId,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false, defaultValue = "all") String filterMode) {
        return hostCheckService.getLog(clusterId, hostname, itemId, logType, logLevel, filterMode);
    }

    /**
     * 获取可用的日志级别
     * @return 日志级别列表
     */
    @GetMapping("/log-levels")
    @UserPermission
    public Result getLogLevels() {
        try {
            return Result.success(LogEntry.Level.values());
        } catch (Exception e) {
            log.error("获取日志级别列表失败: {}", e.getMessage(), e);
            return Result.error("获取日志级别列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取可用的日志类型
     * @return 日志类型列表
     */
    @GetMapping("/log-types")
    @UserPermission
    public Result getLogTypes() {
        try {
            Map<String, String> types = new LinkedHashMap<>();
            types.put("all", "全部日志");
            types.put("check", "检查日志");
            types.put("fix", "修复日志");
            return Result.success(types);
        } catch (Exception e) {
            log.error("获取日志类型列表失败: {}", e.getMessage(), e);
            return Result.error("获取日志类型列表失败: " + e.getMessage());
        }
    }
} 