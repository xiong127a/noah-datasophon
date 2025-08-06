package com.datasophon.api.service;

import com.datasophon.plugins.manager.PluginManager;
import com.datasophon.plugins.manager.PluginManagerExtensions;
import com.datasophon.plugins.manager.PluginStatus;
import com.datasophon.plugins.manager.BatchOperationResult;
import com.datasophon.plugins.manager.PluginHealthInfo;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.PluginMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 插件管理服务
 * 提供插件管理的业务逻辑实现
 * 
 * @author DataSophon Team
 */
@Service
@Slf4j
public class PluginManagementService {
    
    @Autowired
    private PluginManager pluginManager;
    
    @Autowired
    private PluginManagerExtensions pluginExtensions;
    
    /**
     * 获取所有插件列表
     */
    public Map<String, Object> listAllPlugins() {
        List<HostCheckerPlugin> plugins = pluginManager.getAllPlugins();
        Map<String, PluginStatus> statusMap = pluginManager.getAllPluginStatus();
        
        List<Map<String, Object>> pluginList = plugins.stream()
                .map(plugin -> {
                    Map<String, Object> info = new HashMap<>();
                    PluginMetadata metadata = plugin.getMetadata();
                    
                    info.put("pluginId", plugin.getPluginId());
                    info.put("name", metadata.getName());
                    info.put("version", metadata.getVersion());
                    info.put("description", metadata.getDescription());
                    info.put("author", metadata.getAuthor());
                    info.put("category", metadata.getCategory());
                    info.put("tags", metadata.getTags());
                    info.put("supportedOs", metadata.getSupportedOs());
                    info.put("priority", plugin.getPriority());
                    info.put("status", statusMap.get(plugin.getPluginId()));
                    info.put("healthy", plugin.isHealthy());
                    info.put("corePlugin", metadata.isCorePlugin());
                    
                    return info;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", pluginList);
        result.put("total", pluginList.size());
        result.put("message", "获取插件列表成功");
        
        return result;
    }
    
    /**
     * 获取插件详细信息
     */
    public Map<String, Object> getPluginInfo(String pluginId) {
        HostCheckerPlugin plugin = pluginManager.getPlugin(pluginId);
        
        if (plugin == null) {
            return Map.of("success", false, "message", "插件未找到");
        }
        
        PluginMetadata metadata = plugin.getMetadata();
        PluginStatus status = pluginManager.getPluginStatus(pluginId);
        PluginHealthInfo health = pluginExtensions.getPluginHealth(pluginId);
        
        Map<String, Object> info = new HashMap<>();
        info.put("pluginId", plugin.getPluginId());
        info.put("name", metadata.getName());
        info.put("version", metadata.getVersion());
        info.put("description", metadata.getDescription());
        info.put("author", metadata.getAuthor());
        info.put("homepage", metadata.getHomepage());
        info.put("license", metadata.getLicense());
        info.put("category", metadata.getCategory());
        info.put("tags", metadata.getTags());
        info.put("supportedOs", metadata.getSupportedOs());
        info.put("dependencies", metadata.getDependencies());
        info.put("priority", plugin.getPriority());
        info.put("status", status);
        info.put("health", health);
        info.put("corePlugin", metadata.isCorePlugin());
        info.put("configFile", metadata.getConfigFile());
        info.put("documentationUrl", metadata.getDocumentationUrl());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", info);
        result.put("message", "获取插件信息成功");
        
        return result;
    }
    
    /**
     * 动态加载插件
     */
    public Map<String, Object> loadPlugin(String pluginPath) {
        log.info("开始加载插件: {}", pluginPath);
        
        // 检查插件文件是否存在
        Path path = Paths.get(pluginPath);
        if (!Files.exists(path)) {
            return Map.of("success", false, "message", "插件文件不存在: " + pluginPath);
        }
        
        // 加载插件
        boolean success = pluginManager.loadPlugin(pluginPath);
        
        if (success) {
            log.info("插件加载成功: {}", pluginPath);
            return Map.of(
                    "success", true,
                    "message", "插件加载成功",
                    "pluginPath", pluginPath
            );
        } else {
            return Map.of("success", false, "message", "插件加载失败");
        }
    }
    
    /**
     * 上传并加载插件
     */
    public Map<String, Object> uploadAndLoadPlugin(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Map.of("success", false, "message", "请选择要上传的插件文件");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".jar")) {
            return Map.of("success", false, "message", "只支持.jar格式的插件文件");
        }
        
        // 创建插件目录
        Path pluginDir = Paths.get("plugins/uploaded");
        Files.createDirectories(pluginDir);
        
        // 保存上传的插件文件
        Path pluginPath = pluginDir.resolve(filename);
        Files.copy(file.getInputStream(), pluginPath);
        
        log.info("插件文件上传成功: {}", pluginPath);
        
        // 加载插件
        boolean success = pluginManager.loadPlugin(pluginPath.toString());
        
        if (success) {
            log.info("插件加载成功: {}", filename);
            return Map.of(
                    "success", true,
                    "message", "插件上传并加载成功",
                    "filename", filename,
                    "pluginPath", pluginPath.toString()
            );
        } else {
            // 加载失败，删除文件
            Files.deleteIfExists(pluginPath);
            return Map.of("success", false, "message", "插件加载失败，文件已删除");
        }
    }
    
    /**
     * 动态卸载插件
     */
    public Map<String, Object> unloadPlugin(String pluginId) {
        log.info("开始卸载插件: {}", pluginId);
        
        // 检查插件是否存在
        HostCheckerPlugin plugin = pluginManager.getPlugin(pluginId);
        if (plugin == null) {
            return Map.of("success", false, "message", "插件未找到");
        }
        
        // 检查是否是核心插件
        if (plugin.getMetadata().isCorePlugin()) {
            return Map.of("success", false, "message", "核心插件不允许卸载");
        }
        
        // 卸载插件
        boolean success = pluginManager.unloadPlugin(pluginId);
        
        if (success) {
            log.info("插件卸载成功: {}", pluginId);
            return Map.of(
                    "success", true,
                    "message", "插件卸载成功",
                    "pluginId", pluginId
            );
        } else {
            return Map.of("success", false, "message", "插件卸载失败");
        }
    }
    
    /**
     * 重载插件
     */
    public Map<String, Object> reloadPlugin(String pluginId) {
        log.info("开始重载插件: {}", pluginId);
        
        // 检查插件是否存在
        HostCheckerPlugin plugin = pluginManager.getPlugin(pluginId);
        if (plugin == null) {
            return Map.of("success", false, "message", "插件未找到");
        }
        
        // 重载插件
        boolean success = pluginExtensions.reloadPlugin(pluginId);
        
        if (success) {
            log.info("插件重载成功: {}", pluginId);
            return Map.of(
                    "success", true,
                    "message", "插件重载成功",
                    "pluginId", pluginId
            );
        } else {
            return Map.of("success", false, "message", "插件重载失败");
        }
    }
    
    /**
     * 启用插件
     */
    public Map<String, Object> enablePlugin(String pluginId) {
        log.info("启用插件: {}", pluginId);
        
        boolean success = pluginExtensions.enablePlugin(pluginId);
        
        if (success) {
            return Map.of(
                    "success", true,
                    "message", "插件启用成功",
                    "pluginId", pluginId
            );
        } else {
            return Map.of("success", false, "message", "插件启用失败");
        }
    }
    
    /**
     * 禁用插件
     */
    public Map<String, Object> disablePlugin(String pluginId) {
        log.info("禁用插件: {}", pluginId);
        
        // 检查是否是核心插件
        HostCheckerPlugin plugin = pluginManager.getPlugin(pluginId);
        if (plugin != null && plugin.getMetadata().isCorePlugin()) {
            return Map.of("success", false, "message", "核心插件不允许禁用");
        }
        
        boolean success = pluginExtensions.disablePlugin(pluginId);
        
        if (success) {
            return Map.of(
                    "success", true,
                    "message", "插件禁用成功",
                    "pluginId", pluginId
            );
        } else {
            return Map.of("success", false, "message", "插件禁用失败");
        }
    }
    
    /**
     * 批量操作插件
     */
    public Map<String, Object> batchOperatePlugins(List<String> pluginIds, String operation) {
        PluginManagerExtensions.PluginOperation op = PluginManagerExtensions.PluginOperation.valueOf(operation.toUpperCase());
        BatchOperationResult result = pluginExtensions.batchOperatePlugins(pluginIds, op);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isAllSuccess());
        response.put("data", result);
        response.put("message", String.format("批量操作完成：成功 %d 个，失败 %d 个", 
                result.getSuccessCount(), result.getFailureCount()));
        
        return response;
    }
    
    /**
     * 获取插件统计信息
     */
    public Map<String, Object> getPluginStatistics() {
        Map<String, PluginStatus> statusMap = pluginManager.getAllPluginStatus();
        
        long totalCount = statusMap.size();
        long activeCount = statusMap.values().stream().mapToLong(status -> status.isAvailable() ? 1 : 0).sum();
        long errorCount = statusMap.values().stream().mapToLong(status -> status == PluginStatus.ERROR ? 1 : 0).sum();
        long disabledCount = statusMap.values().stream().mapToLong(status -> status == PluginStatus.DISABLED ? 1 : 0).sum();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("activeCount", activeCount);
        statistics.put("errorCount", errorCount);
        statistics.put("disabledCount", disabledCount);
        statistics.put("healthRate", totalCount > 0 ? (double) activeCount / totalCount * 100.0 : 0.0);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", statistics);
        result.put("message", "获取插件统计信息成功");
        
        return result;
    }
    
    /**
     * 获取插件健康检查
     */
    public Map<String, Object> getPluginHealth(String pluginId) {
        PluginHealthInfo health = pluginExtensions.getPluginHealth(pluginId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", health);
        result.put("message", "获取插件健康状态成功");
        
        return result;
    }
}