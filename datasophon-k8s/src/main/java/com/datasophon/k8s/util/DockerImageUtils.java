package com.datasophon.k8s.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.datasophon.common.utils.IOUtils;
import com.datasophon.common.utils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DockerImageUtils {

    private static final Logger logger = LoggerFactory.getLogger(DockerImageUtils.class);

    private static final Properties properties = new Properties();

    private DockerImageUtils() {
        throw new UnsupportedOperationException("Construct PropertyUtils");
    }

    private static final String IMAGE_PROPERTIES_PATH = "dockerImage.properties";

    static {
        String[] propertyFiles = new String[]{IMAGE_PROPERTIES_PATH};
        for (String fileName : propertyFiles) {
            InputStream fis = null;
            try {
                File file = FileUtil.file(fileName);
                fis = IoUtil.toStream(file);
                properties.load(fis);
                String imageRegistry = PropertyUtils.getString("IMAGE_REGISTRY");
                // 遍历所有配置项并替换 $IMAGE_REGISTRY 占位符
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    if (value != null && value.contains("$IMAGE_REGISTRY")) {
                        // 替换镜像地址中的 $IMAGE_REGISTRY
                        String updatedValue = value.replace("$IMAGE_REGISTRY", imageRegistry);
                        properties.setProperty(key, updatedValue);  // 更新值
                        logger.info("Updated docker image for {}: {}", key, updatedValue);
                    }
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                if (fis != null) {
                    IOUtils.closeQuietly(fis);
                }
                System.exit(1);
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
