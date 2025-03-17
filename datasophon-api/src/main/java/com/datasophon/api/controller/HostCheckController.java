package com.datasophon.api.controller;

import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.utils.Result;
import com.datasophon.common.model.CheckItemLog;
import com.datasophon.api.service.CheckItemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;

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
    private CheckItemLogService checkItemLogService;

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
            @RequestParam("itemId") Integer itemId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return hostCheckService.getCheckItemLog(clusterId, hostname, itemId, page, pageSize);
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
     * 获取检查项日志列表（支持筛选）
     */
    @PostMapping("/getCheckItemLogs")
    @UserPermission
    public Result getCheckItemLogs(
            @RequestParam("clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam(value = "hostname", required = false) String hostname,
            @RequestParam(value = "itemId", required = false) Integer itemId,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        
        // 解析日志级别
        CheckItemLog.LogLevel logLevel = null;
        if (level != null) {
            try {
                logLevel = CheckItemLog.LogLevel.valueOf(level);
            } catch (IllegalArgumentException e) {
                return Result.error("无效的日志级别");
            }
        }
        
        // 解析时间范围
        Date startDate = null;
        Date endDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        if (startTime != null) {
            try {
                startDate = sdf.parse(startTime);
            } catch (ParseException e) {
                return Result.error("开始时间格式无效，请使用 yyyy-MM-dd HH:mm:ss 格式");
            }
        }
        
        if (endTime != null) {
            try {
                endDate = sdf.parse(endTime);
            } catch (ParseException e) {
                return Result.error("结束时间格式无效，请使用 yyyy-MM-dd HH:mm:ss 格式");
            }
        }
        
        // 调用服务获取日志
        return checkItemLogService.getCheckItemLogs(
                clusterId, 
                hostname, 
                itemId, 
                logLevel, 
                startDate, 
                endDate, 
                keyword, 
                page, 
                pageSize);
    }

    /**
     * 执行主机检查
     */
    @PostMapping("/executeHostCheck")
    @UserPermission
    public Result executeHostCheck(
            @RequestParam("clusterId") @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam("hostname") String hostname,
            @RequestParam(value = "itemId", required = false) Integer itemId) {
        return hostCheckService.executeHostCheck(clusterId, hostname, itemId);
    }
} 