package com.datasophon.api.configuration;

import com.datasophon.plugins.manager.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件配置类
 * 负责根据配置属性设置插件管理器的行为
 * 
 * @author DataSophon Team
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(PluginProperties.class)
@ConditionalOnProperty(
    name = "datasophon.plugins.loading.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class PluginConfiguration {
    
    @Autowired
    private PluginProperties pluginProperties;
    
    @Autowired
    private PluginManager pluginManager;
    
    @PostConstruct
    public void configurePluginManager() {
        log.info("配置插件管理器...");
        
        // 构建插件扫描路径
        List<String> pluginPaths = buildPluginScanPaths();
        pluginManager.setPluginScanPaths(pluginPaths);
        
        // 配置PF4J插件管理器路径
        pluginManager.configurePluginPaths();
        
        // 设置延迟加载配置
        boolean lazyLoading = pluginProperties.getLoading().isLazy();
        pluginManager.setLazyLoading(lazyLoading);
        
        log.info("插件管理器配置完成 - 延迟加载: {}, 插件功能启用: {}, 扫描路径: {}", 
                lazyLoading, pluginProperties.getLoading().isEnabled(), pluginPaths);
        
        if (!pluginProperties.getLoading().isEnabled()) {
            log.warn("插件功能已禁用，所有插件相关操作将不可用");
        } else if (lazyLoading) {
            log.info("延迟加载模式已启用，插件将在首次使用时自动加载");
            // 延迟加载模式：不在启动时初始化插件，等待手动调用
        } else {
            log.info("立即加载模式，插件将在应用启动时自动加载");
            // 立即加载模式：仍然在启动时初始化（保持原有行为）
        }
    }
    
    /**
     * 根据配置构建插件扫描路径
     */
    private List<String> buildPluginScanPaths() {
        List<String> paths = new ArrayList<>();
        
        // 1. 优先使用配置文件中明确指定的扫描路径
        if (!pluginProperties.getScanPaths().isEmpty()) {
            paths.addAll(pluginProperties.getScanPaths());
        }
        
        // 2. 根据开发模式添加路径
        if (pluginProperties.getDevelopment().isEnabled()) {
            log.info("开发模式已启用，添加开发模式插件路径");
            paths.addAll(pluginProperties.getDevelopment().getPluginPaths());
        }
        
        // 3. 添加生产模式的基础路径
        paths.add(pluginProperties.getDirectory());
        
        // 4. 环境变量配置的路径
        String customPluginPath = System.getProperty("datasophon.plugins.path");
        if (customPluginPath != null && !customPluginPath.trim().isEmpty()) {
            paths.add(customPluginPath.trim());
        }
        
        // 去重并返回
        return paths.stream().distinct().collect(java.util.stream.Collectors.toList());
    }
}