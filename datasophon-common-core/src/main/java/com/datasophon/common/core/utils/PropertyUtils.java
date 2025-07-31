/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.datasophon.common.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 属性工具类
 * 单例模式
 */
public final class PropertyUtils {

    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(PropertyUtils.class);

    private static final Properties properties = new Properties();

    /**
     * 配置提供者接口，用于从外部获取配置值
     */
    public interface ConfigProvider {
        /**
         * 获取指定键的配置值
         *
         * @param key 配置键名
         * @return 配置值，如果不存在则返回null
         */
        String getProperty(String key);

        /**
         * 获取指定前缀的所有配置
         *
         * @param prefix 配置键前缀
         * @return 匹配前缀的配置键值对
         */
        Map<String, String> getPrefixedProperties(String prefix);
    }

    /**
     * 注册的配置提供者
     */
    private static ConfigProvider configProvider = null;

    private PropertyUtils() {
        throw new UnsupportedOperationException("Construct PropertyUtils");
    }

    private static final String COMMON_PROPERTIES_PATH = "/common.properties";

    static {
        String[] propertyFiles = new String[] { COMMON_PROPERTIES_PATH };
        for (String fileName : propertyFiles) {
            InputStream fis = null;
            try {
                fis = PropertyUtils.class.getResourceAsStream(fileName);
                if (fis != null) {
                    properties.load(fis);
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                IOUtils.closeQuietly(fis);
                System.exit(1);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }
    }

    /**
     * 注册配置提供者
     *
     * @param provider 配置提供者
     */
    public static void registerConfigProvider(ConfigProvider provider) {
        configProvider = provider;
        logger.info("Registered custom config provider: {}", provider != null ? provider.getClass().getName() : "null");
    }

    /**
     * 尝试从注册的提供者或properties文件中获取配置值
     *
     * @param key 配置键名
     * @return 配置值
     */
    private static String getFromProviderOrProperties(String key) {
        if (key == null) {
            return null;
        }

        // 首先尝试从注册的提供者获取
        if (configProvider != null) {
            try {
                String value = configProvider.getProperty(key);
                if (value != null) {
                    return value;
                }
            } catch (Exception e) {
                logger.debug("Failed to get property from config provider: {}", key, e);
            }
        }

        // 回退到properties文件
        return properties.getProperty(key.trim());
    }

    /**
     * get property value
     *
     * @param key property name
     * @return property value
     */
    public static String getString(String key) {
        return getFromProviderOrProperties(key);
    }

    /**
     * get property value with upper case
     *
     * @param key property name
     * @return property value with upper case
     */
    public static String getUpperCaseString(String key) {
        String value = getFromProviderOrProperties(key);
        return value != null ? value.toUpperCase() : null;
    }

    /**
     * get property value
     *
     * @param key        property name
     * @param defaultVal default value
     * @return property value
     */
    public static String getString(String key, String defaultVal) {
        String val = getFromProviderOrProperties(key);
        return val == null ? defaultVal : val;
    }

    /**
     * get property value
     *
     * @param key property name
     * @return get property int value , if key == null, then return -1
     */
    public static int getInt(String key) {
        return getInt(key, -1);
    }

    /**
     * @param key          key
     * @param defaultValue default value
     * @return property value
     */
    public static int getInt(String key, int defaultValue) {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.info(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * get property value
     *
     * @param key property name
     * @return property value
     */
    public static boolean getBoolean(String key) {
        String value = getFromProviderOrProperties(key);
        if (null != value) {
            return Boolean.parseBoolean(value);
        }

        return false;
    }

    /**
     * get property value
     *
     * @param key          property name
     * @param defaultValue default value
     * @return property value
     */
    public static Boolean getBoolean(String key, boolean defaultValue) {
        String value = getFromProviderOrProperties(key);
        if (null != value) {
            return Boolean.parseBoolean(value);
        }

        return defaultValue;
    }

    /**
     * get property long value
     *
     * @param key        key
     * @param defaultVal default value
     * @return property value
     */
    public static long getLong(String key, long defaultVal) {
        String val = getString(key);
        return val == null ? defaultVal : Long.parseLong(val);
    }

    /**
     * @param key key
     * @return property value
     */
    public static long getLong(String key) {
        return getLong(key, -1);
    }

    /**
     * @param key        key
     * @param defaultVal default value
     * @return property value
     */
    public double getDouble(String key, double defaultVal) {
        String val = getString(key);
        return val == null ? defaultVal : Double.parseDouble(val);
    }

    /**
     * get array
     *
     * @param key      property name
     * @param splitStr separator
     * @return property value through array
     */
    public static String[] getArray(String key, String splitStr) {
        String value = getString(key);
        if (value == null) {
            return new String[0];
        }
        try {
            return value.split(splitStr);
        } catch (NumberFormatException e) {
            logger.info(e.getMessage(), e);
        }
        return new String[0];
    }

    /**
     * @param key          key
     * @param type         type
     * @param defaultValue default value
     * @param <T>          T
     * @return get enum value
     */
    public <T extends Enum<T>> T getEnum(String key, Class<T> type,
            T defaultValue) {
        String val = getString(key);
        return val == null ? defaultValue : Enum.valueOf(type, val);
    }

    /**
     * get all properties with specified prefix, like: fs.
     *
     * @param prefix prefix to search
     * @return all properties with specified prefix
     */
    public static Map<String, String> getPrefixedProperties(String prefix) {
        Map<String, String> matchedProperties = new HashMap<>();

        // 首先尝试从注册的提供者获取
        if (configProvider != null) {
            try {
                Map<String, String> providerProperties = configProvider.getPrefixedProperties(prefix);
                if (providerProperties != null && !providerProperties.isEmpty()) {
                    return providerProperties;
                }
            } catch (Exception e) {
                logger.debug("Failed to get prefixed properties from config provider: {}", prefix, e);
            }
        }

        // 回退到properties文件
        for (String propName : properties.stringPropertyNames()) {
            if (propName.startsWith(prefix)) {
                matchedProperties.put(propName, properties.getProperty(propName));
            }
        }
        return matchedProperties;
    }

    /**
     * Set a property value
     */
    public static void setValue(String key, String value) {
        properties.setProperty(key, value);
    }
}