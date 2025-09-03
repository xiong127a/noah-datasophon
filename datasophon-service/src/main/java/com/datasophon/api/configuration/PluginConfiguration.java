package com.datasophon.api.configuration;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.spring.ExtensionsInjector;
import org.pf4j.spring.SpringPluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 插件配置类 - 基于官方pf4j-spring标准实现
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

    @Autowired
    private ApplicationContext applicationContext;
    
    /**
     * 创建官方SpringPluginManager Bean
     * 使用官方pf4j-spring实现
     */
    @Bean
    public SpringPluginManager springPluginManager() {
        log.info("初始化官方SpringPluginManager...");
        
        // 构建插件扫描路径
        List<Path> pluginPaths = buildPluginPaths();
        
        // 使用官方SpringPluginManager
        SpringPluginManager pluginManager = new SpringPluginManager(pluginPaths);
        
        log.info("SpringPluginManager创建完成 - 插件路径数量: {}", pluginPaths.size());
                
        return pluginManager;
    }

    /**
     * ExtensionsInjector配置 - 核心功能！
     * 自动将插件扩展注入为Spring Bean
     */
    @Bean
    @DependsOn("springPluginManager")
    public ExtensionsInjector extensionsInjector() {
        log.info("初始化ExtensionsInjector - 启用插件扩展自动注入");
        // 使用官方正确的构造函数参数
        AbstractAutowireCapableBeanFactory beanFactory = 
            (AbstractAutowireCapableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        return new ExtensionsInjector(springPluginManager(), beanFactory);
    }
    
    /**
     * 插件系统启动配置
     */
    @PostConstruct
    public void initializePluginSystem() {
        if (!pluginProperties.getLoading().isEnabled()) {
            log.warn("插件功能已禁用");
            return;
        }
        
        log.info("插件系统启动完成 - ExtensionsInjector已启用自动注入功能");
        log.info("插件扩展将自动注入到Spring容器中，业务代码可直接使用@Autowired");
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
     * 动态扫描datasophon-plugins目录下所有插件的target目录
     * 基于官方pf4j-spring简化结构
     */
    private List<String> getDefaultDevelopmentPluginPaths() {
        List<String> defaultPaths = new ArrayList<>();
        
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");
        String pluginsDir = projectRoot + "/datasophon-plugins";
        
        try {
            // 扫描datasophon-plugins目录下的所有子目录
            Path pluginsPath = Paths.get(pluginsDir);
            if (Files.exists(pluginsPath) && Files.isDirectory(pluginsPath)) {
                try (Stream<Path> pluginDirs = Files.list(pluginsPath)) {
                    pluginDirs.filter(Files::isDirectory)
                            .filter(dir -> !dir.getFileName().toString().equals("datasophon-plugin-api")) // 排除API模块
                            .filter(dir -> !dir.getFileName().toString().equals("assembly")) // 排除assembly目录
                            .map(pluginDir -> pluginDir.resolve("target").toString())
                            .filter(targetPath -> Files.exists(Paths.get(targetPath)))
                            .forEach(defaultPaths::add);
                }
            }
            
            log.info("官方pf4j-spring结构：扫描到 {} 个插件开发路径: {}", defaultPaths.size(), defaultPaths);
        } catch (Exception e) {
            log.warn("扫描插件开发路径失败，使用空列表", e);
        }
        
        return defaultPaths;
    }
}