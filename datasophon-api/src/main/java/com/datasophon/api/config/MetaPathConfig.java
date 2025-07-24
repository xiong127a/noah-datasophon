package com.datasophon.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;

/**
 * 元数据路径配置
 * 支持开发环境和生产环境不同的路径配置
 */
@Configuration
public class MetaPathConfig {

    private static final Logger logger = LoggerFactory.getLogger(MetaPathConfig.class);

    private final ResourceLoader resourceLoader;

    @Value("${datasophon.checker.meta.base-dir}")
    private String configuredPath;
    @Autowired
    public MetaPathConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public String metaBasePath() {
        // 首先尝试从配置的路径获取
        if (configuredPath.startsWith("classpath:")) {
            try {
                Resource resource = resourceLoader.getResource(configuredPath);
                if (resource.exists()) {
                    logger.info("从classpath加载元数据目录: {}", configuredPath);
                    try {
                        // 尝试获取文件系统路径
                        return resource.getFile().getAbsolutePath();
                    } catch (IOException e) {
                        // 无法获取文件系统路径，可能是在JAR中
                        logger.warn("无法获取classpath资源的文件系统路径，将使用URI: {}", e.getMessage());
                        return resource.getURI().toString();
                    }
                } else {
                    logger.warn("配置的classpath资源不存在: {}", configuredPath);
                }
            } catch (IOException e) {
                logger.warn("无法从classpath加载元数据目录: {}", e.getMessage());
            }
        }

        // 如果配置的是绝对路径或相对路径
        File file = new File(configuredPath);
        if (file.exists()) {
            logger.info("从文件系统加载元数据目录: {}", file.getAbsolutePath());
            return file.getAbsolutePath();
        }

        // 检查IDEA开发环境路径
        String ideaPath = detectIdeaPath();
        if (ideaPath != null) {
            logger.info("从IDEA开发环境加载元数据目录: {}", ideaPath);
            return ideaPath;
        }

        // 如果都不存在，使用默认路径
        String defaultPath = System.getProperty("user.dir") + "/meta";
        logger.info("使用默认元数据目录: {}", defaultPath);

        // 创建目录确保存在
        File defaultDir = new File(defaultPath);
        if (!defaultDir.exists()) {
            if (defaultDir.mkdirs()) {
                logger.info("已创建默认元数据目录: {}", defaultPath);
            } else {
                logger.warn("无法创建默认元数据目录: {}", defaultPath);
            }
        }

        return defaultPath;
    }

    /**
     * 检测IDEA开发环境中的路径
     * 
     * @return IDEA项目中的元数据路径，如果不在IDEA环境中则返回null
     */
    private String detectIdeaPath() {
        // 尝试常见的IDEA项目结构路径
        String projectRoot = System.getProperty("user.dir");

        // 检查是否在datasophon-api模块中
        if (projectRoot.endsWith("datasophon-api")) {
            projectRoot = new File(projectRoot).getParent();
        }
        // 检查是否在其他模块中
        else if (projectRoot.contains("datasophon-") || projectRoot.contains("noah-bigdata-platform")) {
            File parent = new File(projectRoot).getParentFile();
            if (parent != null && parent.exists()) {
                projectRoot = parent.getAbsolutePath();
            }
        }

        // 尝试几个可能的路径
        String[] possiblePaths = {
                projectRoot + "/datasophon-api/src/main/resources/meta",
                projectRoot + "/src/main/resources/meta",
                projectRoot + "/conf/meta"
        };

        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                logger.info("在IDEA环境中找到meta目录: {}", path);
                return dir.getAbsolutePath();
            }
        }

        // 如果找不到，尝试创建一个路径
        String ideaPath = projectRoot + "/datasophon-api/src/main/resources/meta";
        File ideaDir = new File(ideaPath);
        if (!ideaDir.exists()) {
            if (ideaDir.mkdirs()) {
                logger.info("已在IDEA环境中创建meta目录: {}", ideaPath);
                return ideaDir.getAbsolutePath();
            }
        }

        return null;
    }
}