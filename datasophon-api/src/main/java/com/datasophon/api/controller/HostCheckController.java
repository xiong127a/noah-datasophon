package com.datasophon.api.controller;

import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.HostCheckService;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 主机检查控制器
 */
@Validated
@RestController
@RequestMapping("host/check")
public class HostCheckController {

    @Autowired
    private HostCheckService hostCheckService;

    /**
     * 获取主机检查项
     */
    @GetMapping("/getHostCheckItems")
    public Result getHostCheckItems(@RequestParam String hostname, @RequestParam Integer clusterId) {
        return Result.success(hostCheckService.getHostCheckItems());
    }

    /**
     * 终止检查任务（针对集群中所有主机）
     */
    @PostMapping("/stopAllChecks")
    @UserPermission
    public Result stopAllChecks(@RequestParam @NotNull(message = "集群ID不能为空") Integer clusterId) {
        try {
            // 获取该集群所有主机
            Map<String, HostInfo> hostMap = (Map<String, HostInfo>) CacheUtils.get(clusterId + Constants.HOST_MAP);
            if (hostMap == null || hostMap.isEmpty()) {
                return Result.error("主机列表为空");
            }
            
            // 循环对每个主机停止检查
            for (String hostname : hostMap.keySet()) {
                hostCheckService.stopHostCheck(clusterId, hostname);
            }
            
            return Result.success("已停止所有主机检查");
        } catch (Exception e) {
            return Result.error("停止检查失败: " + e.getMessage());
        }
    }

    /**
     * 终止主机检查
     */
    @PostMapping("/stopCheckItem")
    @UserPermission
    public Result stopCheckItem(
            @RequestParam @NotNull(message = "集群ID不能为空") Integer clusterId,
            @RequestParam String hostname,
            @RequestParam(required = false) Integer itemId) {
        
        if (itemId != null) {
            // 终止指定检查项
            return hostCheckService.stopItemCheck(clusterId, hostname, itemId);
        } else {
            // 终止指定主机的所有检查
            return hostCheckService.stopHostCheck(clusterId, hostname);
        }
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
} 