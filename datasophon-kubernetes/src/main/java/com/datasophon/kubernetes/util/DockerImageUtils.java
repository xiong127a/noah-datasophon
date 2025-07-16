package com.datasophon.kubernetes.util;

import com.datasophon.common.utils.IOUtils;
import com.datasophon.common.utils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DockerImageUtils {

    private static final Logger logger = LoggerFactory.getLogger(DockerImageUtils.class);

    private static final Properties properties = new Properties();

    private DockerImageUtils() {
        throw new UnsupportedOperationException("Construct PropertyUtils");
    }

    private static final String IMAGE_PROPERTIES_PATH = "classpath:dockerImage.properties";

    // 创建一个默认的ResourceLoader实例
    private static final ResourceLoader resourceLoader = new DefaultResourceLoader();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        String[] propertyFiles = new String[] { IMAGE_PROPERTIES_PATH };
        for (String resourcePath : propertyFiles) {
            InputStream fis = null;
            try {
                // 使用Spring的ResourceLoader加载资源
                Resource resource = resourceLoader.getResource(resourcePath);
                logger.info("Using Spring ResourceLoader to load: {}", resourcePath);

                if (resource.exists()) {
                    fis = resource.getInputStream();
                }

                // 如果通过Spring无法加载，回退到类加载器
                if (fis == null) {
                    logger.warn("Could not load resource with Spring ResourceLoader: {}", resourcePath);
                    // 尝试直接从类路径加载
                    String fileName = resourcePath.replace("classpath:", "");
                    fis = DockerImageUtils.class.getResourceAsStream("/" + fileName);

                    if (fis == null) {
                        fis = DockerImageUtils.class.getResourceAsStream(fileName);
                    }

                    if (fis == null) {
                        fis = DockerImageUtils.class.getClassLoader().getResourceAsStream(fileName);
                    }
                }

                if (fis == null) {
                    logger.error("Resource not found: {}", resourcePath);
                    throw new IOException("Cannot find resource: " + resourcePath);
                }

                properties.load(fis);
                String imageRegistry = PropertyUtils.getString("IMAGE_REGISTRY");
                // 遍历所有配置项并替换 $IMAGE_REGISTRY 占位符
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    if (value != null && value.contains("$IMAGE_REGISTRY")) {
                        // 替换镜像地址中的 $IMAGE_REGISTRY
                        String updatedValue = value.replace("$IMAGE_REGISTRY", imageRegistry);
                        properties.setProperty(key, updatedValue); // 更新值
                        logger.info("Updated docker image for {}: {}", key, updatedValue);
                    }
                }
            } catch (IOException e) {
                logger.error("Error loading properties file {}: {}", resourcePath, e.getMessage(), e);
                if (fis != null) {
                    IOUtils.closeQuietly(fis);
                }
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }
    }

    public static String getString(String key) {
        return properties.getProperty(key.trim());
    }

    public static String getString(String key, String defaultVal) {
        String val = properties.getProperty(key.trim());
        return val == null ? defaultVal : val;
    }
}
