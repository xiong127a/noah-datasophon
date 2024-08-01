package com.datasophon.k8s.util;

import com.datasophon.common.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DockerImageUtils {

    private static final Logger logger = LoggerFactory.getLogger(DockerImageUtils.class);

    private static final Properties properties = new Properties();

    private DockerImageUtils() {
        throw new UnsupportedOperationException("Construct PropertyUtils");
    }

    private static final String IMAGE_PROPERTIES_PATH = "/dockerImage.properties";

    static {
        String[] propertyFiles = new String[]{IMAGE_PROPERTIES_PATH};
        for (String fileName : propertyFiles) {
            InputStream fis = null;
            try {
                fis = DockerImageUtils.class.getResourceAsStream(fileName);
                properties.load(fis);
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
