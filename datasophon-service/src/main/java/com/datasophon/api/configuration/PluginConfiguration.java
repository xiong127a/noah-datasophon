package com.datasophon.api.configuration;

import com.datasophon.plugins.manager.SpringPluginManager;
import lombok.extern.slf4j.Slf4j;
// import org.pf4j.spring.ExtensionsInjector; // 暂时禁用
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
// import org.springframework.context.ApplicationContext; // 将在ExtensionsInjector实现时使用
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.DependsOn; // 暂时不需要

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

    // ApplicationContext将在需要时使用
    // @Autowired
    // private ApplicationContext applicationContext;
    
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
     * ExtensionsInjector配置
     * 暂时禁用自动扩展注入，等插件系统完全稳定后再启用
     */
    @PostConstruct
    public void configureExtensionsInjector() {
        // ExtensionsInjector的配置将在插件系统稳定后进行
        // 目前通过SpringPluginManager的getPluginsByType等方法使用插件
        log.info("插件系统已配置，扩展将通过SpringPluginManager API访问");
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
            } else {
                // 添加默认的开发模式插件路径（target/classes目录）
                pathStrings.addAll(getDefaultDevelopmentPluginPaths());
                log.info("使用默认开发模式插件路径");
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
    
    /**
     * 获取默认的开发模式插件路径
     * 动态扫描core-plugins目录下所有插件的target目录
     */
    private List<String> getDefaultDevelopmentPluginPaths() {
        List<String> defaultPaths = new ArrayList<>();
        
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");
        String corePluginsDir = projectRoot + "/datasophon-plugins/datasophon-plugins-impl/core-plugins";
        
        try {
            // 扫描core-plugins目录下的所有子目录
            Path corePluginsPath = Paths.get(corePluginsDir);
            if (Files.exists(corePluginsPath) && Files.isDirectory(corePluginsPath)) {
                try (Stream<Path> pluginDirs = Files.list(corePluginsPath)) {
                    pluginDirs.filter(Files::isDirectory)
                            .map(pluginDir -> pluginDir.resolve("target").toString())
                            .filter(targetPath -> Files.exists(Paths.get(targetPath)))
                            .forEach(defaultPaths::add);
                }
            }
            
            log.info("动态扫描到 {} 个插件开发路径: {}", defaultPaths.size(), defaultPaths);
        } catch (Exception e) {
            log.warn("扫描插件开发路径失败，使用空列表", e);
        }
        
        return defaultPaths;
    }
}