package com.datasophon.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 元数据路径配置
 * 支持开发环境和生产环境不同的路径配置
 */
@Configuration
public class MetaPathConfig {

    private static final Logger logger = LoggerFactory.getLogger(MetaPathConfig.class);

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${datasophon.checker.meta.base-dir}")
    private String configuredPath;

    @Bean
    public String metaBasePath() {
        // 首先尝试从配置的路径获取
        if (configuredPath.startsWith("classpath:")) {
            try {
                Resource resource = resourceLoader.getResource(configuredPath);
                if (resource.exists()) {
                    return resource.getFile().getAbsolutePath();
                }
            } catch (IOException e) {
                logger.warn("无法从 classpath 加载元数据目录: {}", e.getMessage());
            }
        }

        // 如果配置的是绝对路径或相对路径
        File file = new File(configuredPath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }

        // 如果都不存在，使用默认路径
        String defaultPath = System.getProperty("user.dir") + "/meta";
        logger.info("使用默认元数据目录: {}", defaultPath);
        return defaultPath;
    }
}