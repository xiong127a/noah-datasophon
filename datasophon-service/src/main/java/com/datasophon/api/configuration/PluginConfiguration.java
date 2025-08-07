package com.datasophon.api.configuration;

import com.datasophon.plugins.manager.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 插件配置类
 * 负责根据配置属性设置插件管理器的行为
 * 
 * @author DataSophon Team
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(PluginProperties.class)
public class PluginConfiguration {
    
    @Autowired
    private PluginProperties pluginProperties;
    
    @Autowired
    private PluginManager pluginManager;
    
    @PostConstruct
    public void configurePluginManager() {
        log.info("配置插件管理器...");
        
        // 设置延迟加载配置
        boolean lazyLoading = pluginProperties.isLazyLoadingEnabled();
        pluginManager.setLazyLoading(lazyLoading);
        
        log.info("插件管理器配置完成 - 延迟加载: {}, 插件功能启用: {}", 
                lazyLoading, pluginProperties.isPluginEnabled());
        
        if (!pluginProperties.isPluginEnabled()) {
            log.warn("插件功能已禁用，所有插件相关操作将不可用");
        } else if (lazyLoading) {
            log.info("延迟加载模式已启用，插件将在首次使用时自动加载");
        } else {
            log.info("立即加载模式，插件将在应用启动时自动加载");
        }
    }
}