package com.datasophon.api.controller.v1;

import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.HostManagementService;
import com.datasophon.common.dto.host.BatchHostnameChangeRequest;
import com.datasophon.common.dto.host.HostsSyncRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 主机管理API控制器
 * 提供主机名批量修改和hosts文件同步功能
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/host-management")
public class HostManagementController {
    
    @Autowired
    private HostManagementService hostManagementService;
    
    /**
     * 获取主机名配置（前缀推荐、格式选项）
     */
    @GetMapping("/hostname-config")
    public Result<Map<String, Object>> getHostnameConfig(@ClusterId Long clusterId) {
        log.info("获取主机名配置: clusterId={}", clusterId);
        
        try {
            Map<String, Object> config = hostManagementService.getHostnameConfig(clusterId);
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取主机名配置失败: clusterId={}, error={}", clusterId, e.getMessage(), e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 预览批量主机名修改
     */
    @PostMapping("/preview-hostname-changes")
    public Result<Map<String, String>> previewHostnameChanges(
            @RequestBody BatchHostnameChangeRequest request,
            @ClusterId Long clusterId) {
        
        log.info("预览主机名变更: clusterId={}, prefix={}, hosts={}", 
                clusterId, request.getPrefix(), request.getHostIps().size());
        
        try {
            request.setClusterId(clusterId);
            Map<String, String> preview = hostManagementService.previewHostnameChanges(request);
            return Result.success(preview);
        } catch (Exception e) {
            log.error("预览主机名变更失败: clusterId={}, error={}", clusterId, e.getMessage(), e);
            return Result.error("预览失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量修改主机名（异步，返回taskId，通过SSE获取进度）
     */
    @PostMapping("/batch-hostname")
    public Result<String> batchChangeHostnames(
            @RequestBody BatchHostnameChangeRequest request,
            @ClusterId Long clusterId) {
        
        log.info("批量修改主机名: clusterId={}, prefix={}, hosts={}", 
                clusterId, request.getPrefix(), request.getHostIps().size());
        
        try {
            request.setClusterId(clusterId);
            
            if (request.getHostIps() == null || request.getHostIps().isEmpty()) {
                return Result.error("主机列表不能为空");
            }
            if (request.getPrefix() == null || request.getPrefix().trim().isEmpty()) {
                return Result.error("主机名前缀不能为空");
            }
            if (request.getConnectionParams() == null) {
                return Result.error("连接参数不能为空");
            }
            
            String taskId = hostManagementService.batchChangeHostnames(request);
            return Result.success(taskId);
            
        } catch (Exception e) {
            log.error("批量修改主机名失败: clusterId={}, error={}", clusterId, e.getMessage(), e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步hosts文件（异步，返回taskId，通过SSE获取进度）
     */
    @PostMapping("/sync-hosts-file")
    public Result<String> syncHostsFile(
            @RequestBody HostsSyncRequest request,
            @ClusterId Long clusterId) {
        
        log.info("同步hosts文件: clusterId={}, hosts={}", clusterId, request.getHostIps().size());
        
        try {
            request.setClusterId(clusterId);
            
            if (request.getHostIps() == null || request.getHostIps().isEmpty()) {
                return Result.error("主机列表不能为空");
            }
            if (request.getConnectionParams() == null) {
                return Result.error("连接参数不能为空");
            }
            
            String taskId = hostManagementService.syncHostsFile(request);
            return Result.success(taskId);
            
        } catch (Exception e) {
            log.error("同步hosts文件失败: clusterId={}, error={}", clusterId, e.getMessage(), e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }
}

