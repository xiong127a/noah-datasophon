package com.datasophon.api.controller;

import com.datasophon.api.service.PluginManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 插件管理控制器
 * 提供插件动态加载、卸载、管理的REST API
 * 
 * @author DataSophon Team
 */
@RestController
@RequestMapping("/api/plugins")
@Slf4j
public class PluginManagementController {
    
    @Autowired
    private PluginManagementService pluginManagementService;
    
    /**
     * 获取所有插件列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listPlugins() {
        try {
            Map<String, Object> result = pluginManagementService.listAllPlugins();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取插件列表失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "获取插件列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取插件详细信息
     */
    @GetMapping("/{pluginId}")
    public ResponseEntity<Map<String, Object>> getPluginInfo(@PathVariable String pluginId) {
        try {
            Map<String, Object> result = pluginManagementService.getPluginInfo(pluginId);
            
            if (!(Boolean) result.get("success")) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取插件信息失败: {}", pluginId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "获取插件信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 动态加载插件
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> loadPlugin(@RequestParam String pluginPath) {
        try {
            Map<String, Object> result = pluginManagementService.loadPlugin(pluginPath);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("加载插件异常: {}", pluginPath, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "加载插件异常: " + e.getMessage()));
        }
    }
    
    /**
     * 上传并加载插件
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndLoadPlugin(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = pluginManagementService.uploadAndLoadPlugin(file);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("上传并加载插件异常", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "上传并加载插件异常: " + e.getMessage()));
        }
    }
    
    /**
     * 动态卸载插件
     */
    @DeleteMapping("/{pluginId}")
    public ResponseEntity<Map<String, Object>> unloadPlugin(@PathVariable String pluginId) {
        try {
            Map<String, Object> result = pluginManagementService.unloadPlugin(pluginId);
            
            if (!(Boolean) result.get("success")) {
                String message = (String) result.get("message");
                if ("插件未找到".equals(message)) {
                    return ResponseEntity.notFound().build();
                } else {
                    return ResponseEntity.badRequest().body(result);
                }
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("卸载插件异常: {}", pluginId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "卸载插件异常: " + e.getMessage()));
        }
    }
    
    /**
     * 重载插件
     */
    @PostMapping("/{pluginId}/reload")
    public ResponseEntity<Map<String, Object>> reloadPlugin(@PathVariable String pluginId) {
        try {
            Map<String, Object> result = pluginManagementService.reloadPlugin(pluginId);
            
            if (!(Boolean) result.get("success")) {
                String message = (String) result.get("message");
                if ("插件未找到".equals(message)) {
                    return ResponseEntity.notFound().build();
                } else {
                    return ResponseEntity.badRequest().body(result);
                }
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("重载插件异常: {}", pluginId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "重载插件异常: " + e.getMessage()));
        }
    }
    
    /**
     * 启用插件
     */
    @PostMapping("/{pluginId}/enable")
    public ResponseEntity<Map<String, Object>> enablePlugin(@PathVariable String pluginId) {
        try {
            Map<String, Object> result = pluginManagementService.enablePlugin(pluginId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.internalServerError().body(result);
            }
            
        } catch (Exception e) {
            log.error("启用插件异常: {}", pluginId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "启用插件异常: " + e.getMessage()));
        }
    }
    
    /**
     * 禁用插件
     */
    @PostMapping("/{pluginId}/disable")
    public ResponseEntity<Map<String, Object>> disablePlugin(@PathVariable String pluginId) {
        try {
            Map<String, Object> result = pluginManagementService.disablePlugin(pluginId);
            
            if (!(Boolean) result.get("success")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("禁用插件异常: {}", pluginId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "禁用插件异常: " + e.getMessage()));
        }
    }
    
    /**
     * 批量操作插件
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchOperatePlugins(
            @RequestParam List<String> pluginIds,
            @RequestParam String operation) {
        try {
            Map<String, Object> result = pluginManagementService.batchOperatePlugins(pluginIds, operation);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("批量操作插件异常", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "批量操作异常: " + e.getMessage()));
        }
    }
    
    /**
     * 获取插件统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getPluginStatistics() {
        try {
            Map<String, Object> result = pluginManagementService.getPluginStatistics();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取插件统计信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "获取插件统计信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取插件健康检查
     */
    @GetMapping("/{pluginId}/health")
    public ResponseEntity<Map<String, Object>> getPluginHealth(@PathVariable String pluginId) {
        try {
            Map<String, Object> result = pluginManagementService.getPluginHealth(pluginId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取插件健康状态失败: {}", pluginId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "获取插件健康状态失败: " + e.getMessage()));
        }
    }
}