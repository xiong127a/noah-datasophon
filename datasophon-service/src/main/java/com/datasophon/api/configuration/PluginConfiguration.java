package com.datasophon.api.configuration;

import com.datasophon.plugins.manager.SpringPluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件配置类 - 使用SpringPluginManager的优雅方案
 * 完美整合Spring Boot与PF4J插件框架
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
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
    
    /**
     * 创建SpringPluginManager Bean
     * 自动装配，支持依赖注入
     */
    @Bean
    public SpringPluginManager springPluginManager() {
        log.info("初始化SpringPluginManager...");
        
        // 构建插件扫描路径
        List<Path> pluginPaths = buildPluginPaths();
        
        // 创建SpringPluginManager实例
        SpringPluginManager pluginManager = new SpringPluginManager(pluginPaths);
        
        log.info("SpringPluginManager创建完成 - 插件路径数量: {}, 延迟加载: {}", 
                pluginPaths.size(), pluginProperties.getLoading().isLazy());
                
        return pluginManager;
    }
    
    /**
     * SpringPluginManager启动后配置
     */
    @PostConstruct
    public void initializePluginManager() {
        if (!pluginProperties.getLoading().isEnabled()) {
            log.warn("插件功能已禁用");
            return;
        }
        
        boolean lazyLoading = pluginProperties.getLoading().isLazy();
        if (lazyLoading) {
            log.info("延迟加载模式：插件将在首次使用时自动加载");
        } else {
            log.info("立即加载模式：SpringPluginManager将自动启动并加载所有插件");
        }
    }
    
    /**
     * 根据配置构建插件Path列表
     */
    private List<Path> buildPluginPaths() {
        List<String> pathStrings = new ArrayList<>();
        
        // 1. 优先使用配置文件中明确指定的扫描路径
        if (!pluginProperties.getScanPaths().isEmpty()) {
            pathStrings.addAll(pluginProperties.getScanPaths());
        }
        
        // 2. 根据开发模式添加路径
        if (pluginProperties.getDevelopment().isEnabled()) {
            log.info("开发模式已启用，添加开发模式插件路径");
            List<String> devPaths = pluginProperties.getDevelopment().getPluginPaths();
            if (!devPaths.isEmpty()) {
                pathStrings.addAll(devPaths);
                log.info("添加了 {} 个开发模式插件路径", devPaths.size());
            }
        } else {
            // 3. 生产模式：添加基础插件目录（JAR文件）
            pathStrings.add(pluginProperties.getDirectory());
            log.info("生产模式：添加插件目录 {}", pluginProperties.getDirectory());
        }
        
        // 4. 环境变量配置的路径
        String customPluginPath = System.getProperty("datasophon.plugins.path");
        if (customPluginPath != null && !customPluginPath.trim().isEmpty()) {
            pathStrings.add(customPluginPath.trim());
            log.info("添加自定义插件路径: {}", customPluginPath.trim());
        }
        
        // 转换为Path对象并去重
        return pathStrings.stream()
                .distinct()
                .map(Paths::get)
                .filter(path -> {
                    boolean exists = path.toFile().exists();
                    if (!exists) {
                        log.debug("插件路径不存在，跳过: {}", path);
                    } else {
                        log.info("添加有效插件路径: {}", path);
                    }
                    return exists;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}