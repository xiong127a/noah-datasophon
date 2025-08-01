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

package com.datasophon.common.cache;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.datasophon.common.Constants;
import com.datasophon.common.model.HostInfo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache工具类
 */
public class CacheUtils {
    private static final Logger logger = LoggerFactory.getLogger(CacheUtils.class);
    private static final String CHECK_ITEM_LOG_PREFIX = "CHECK_ITEM_LOG_";

    // 默认缓存过期时间为1小时
    private static final long DEFAULT_TIMEOUT = 3600000;

    // 日志缓存过期时间为24小时
    private static final long LOG_TIMEOUT = 24 * 3600000;

    // 使用定时缓存替代LRU缓存
    private static final Cache<String, Object> cache = CacheUtil.newTimedCache(DEFAULT_TIMEOUT);

    // 用于记录所有添加到缓存中的键
    private static final Set<String> cacheKeys = ConcurrentHashMap.newKeySet();

    public static Object get(String key) {
        Object value = cache.get(key);
        if (key != null && key.startsWith(CHECK_ITEM_LOG_PREFIX)) {
            logger.debug("获取日志缓存: {}, 是否存在: {}", key, value != null);
        }
        return value;
    }

    /**
     * 获取指定简单类型的缓存数据
     * 
     * @param <T>   返回的数据类型
     * @param key   缓存键
     * @param clazz 目标类型的Class对象
     * @return 指定类型的缓存数据，如果类型不匹配或缓存不存在则返回null
     */
    public static <T> T getGeneric(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) {
            return null;
        }

        try {
            // 如果直接就是目标类型，直接转换
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }

            // 通过JSON序列化和反序列化进行转换
            String jsonString = JSON.toJSONString(value);
            return JSON.parseObject(jsonString, clazz);
        } catch (Exception e) {
            logger.error("缓存数据类型转换错误，键: {}, 类型: {}, 错误: {}", key, clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定复杂泛型类型的缓存数据（如Map<String, Object>等）
     * 
     * @param <T>           返回的数据类型
     * @param key           缓存键
     * @param typeReference 复杂类型的TypeReference
     * @return 指定类型的缓存数据，如果类型不匹配或缓存不存在则返回null
     */
    public static <T> T getGeneric(String key, TypeReference<T> typeReference) {
        Object value = get(key);
        if (value == null) {
            return null;
        }

        try {
            // 通过JSON序列化和反序列化进行转换
            String jsonString = JSON.toJSONString(value);
            return JSON.parseObject(jsonString, typeReference);
        } catch (Exception e) {
            logger.error("缓存数据类型转换错误，键: {}, 类型: {}, 错误: {}", key, typeReference.getType(), e.getMessage());
            return null;
        }
    }

    /**
     * 获取主机信息映射
     * 
     * @param key 缓存键
     * @return 主机信息映射
     */
    public static Map<String, HostInfo> getHostMap(String key) {
        // 使用预定义的TypeReference获取泛型类型
        Map<String, HostInfo> result = getGeneric(key, TypeRefs.MAP_STRING_HOSTINFO);
        return result != null ? result : new ConcurrentHashMap<>();
    }

    public static void put(String key, Object value) {
        // 将键添加到键集合中
        if (key != null) {
            cacheKeys.add(key);
        }

        // 对于日志缓存项，使用更长的过期时间
        if (key != null && key.startsWith(CHECK_ITEM_LOG_PREFIX)) {
            logger.debug("保存日志缓存: {}, 内容长度: {}", key,
                    value instanceof String ? ((String) value).length() : "非字符串");
            cache.put(key, value, LOG_TIMEOUT);
        } else {
            cache.put(key, value);
        }
    }

    public static boolean constainsKey(String key) {
        return cache.containsKey(key);
    }

    public static void removeKey(String key) {
        cache.remove(key);
        // 从键集合中移除
        if (key != null) {
            cacheKeys.remove(key);
        }
    }

    public static void clear() {
        cache.clear();
        // 同时清空键集合
        cacheKeys.clear();
    }

    public static Integer getInteger(String key) {
        Object data = cache.get(key);
        return (Integer) data;
    }

    public static Boolean getBoolean(String key) {
        Object data = cache.get(key);
        return (Boolean) data;
    }

    public static String getString(String key) {
        Object data = cache.get(key);
        return (String) data;
    }

    /**
     * 更新主机信息缓存
     * 
     * @param clusterId 集群ID
     * @param ip        主机IP
     * @param hostInfo  主机信息对象
     */
    public static void putHostInfo(Integer clusterId, String ip, HostInfo hostInfo) {
        if (clusterId == null || StringUtils.isBlank(ip) || hostInfo == null) {
            logger.warn("更新主机缓存参数无效: clusterId={}, ip={}", clusterId, ip);
            return;
        }

        String cacheKey = clusterId + Constants.HOST_MAP;

        try {
            // 获取当前缓存
            Map<String, HostInfo> hostMap = getHostMap(cacheKey);

            // 更新特定主机信息
            hostMap.put(ip, hostInfo);

            // 保存回缓存
            put(cacheKey, hostMap);

            logger.debug("已更新主机缓存: clusterId={}, ip={}", clusterId, ip);
        } catch (Exception e) {
            logger.error("更新主机缓存失败: clusterId={}, ip={}, 原因: {}", clusterId, ip, e.getMessage(), e);

            // 重试一次
            try {
                Thread.sleep(50);
                Map<String, HostInfo> hostMap = getHostMap(cacheKey);
                hostMap.put(ip, hostInfo);
                put(cacheKey, hostMap);
                logger.info("重试更新主机缓存成功: clusterId={}, ip={}", clusterId, ip);
            } catch (Exception e2) {
                logger.error("重试更新主机缓存失败: clusterId={}, ip={}, 原因: {}", clusterId, ip, e2.getMessage(), e2);
            }
        }
    }

    /**
     * TypeReference 快速生成工具类
     * 提供常用复杂类型的 TypeReference 实例
     */
    @Getter
    public static class TypeRefs {

        // 常用的 Map 类型
        public static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT = new TypeReference<>() {
        };
        public static final TypeReference<Map<String, String>> MAP_STRING_STRING = new TypeReference<>() {
        };
        public static final TypeReference<Map<String, Integer>> MAP_STRING_INTEGER = new TypeReference<>() {
        };
        public static final TypeReference<Map<String, HostInfo>> MAP_STRING_HOSTINFO = new TypeReference<>() {
        };
        public static final TypeReference<HashMap<String, Object>> HASHMAP_STRING_OBJECT = new TypeReference<>() {
        };
        public static final TypeReference<HashMap<String, String>> HASHMAP_STRING_STRING = new TypeReference<>() {
        };

        // 常用的 List 类型
        public static final TypeReference<List<String>> LIST_STRING = new TypeReference<>() {
        };
        public static final TypeReference<List<Integer>> LIST_INTEGER = new TypeReference<>() {
        };
        public static final TypeReference<List<Object>> LIST_OBJECT = new TypeReference<>() {
        };
        public static final TypeReference<List<HostInfo>> LIST_HOSTINFO = new TypeReference<>() {
        };

        // 嵌套复杂类型
        public static final TypeReference<Map<String, List<String>>> MAP_STRING_LIST_STRING = new TypeReference<>() {
        };
        public static final TypeReference<Map<String, List<Object>>> MAP_STRING_LIST_OBJECT = new TypeReference<>() {
        };
        public static final TypeReference<List<Map<String, Object>>> LIST_MAP_STRING_OBJECT = new TypeReference<>() {
        };

        // 常用业务场景的便利方法

        /**
         * 创建自定义 Map&lt;String, T&gt; 类型的 TypeReference
         * <p>
         * 使用示例：
         * 
         * <pre>
         * TypeReference&lt;Map&lt;String, UserInfo&gt;&gt; userMapType = TypeRefs.mapStringOf(UserInfo.class);
         * Map&lt;String, UserInfo&gt; userMap = CacheUtils.getGeneric("users", userMapType);
         * </pre>
         * 
         * @param valueType 值类型的Class
         * @return 新的 TypeReference 实例
         */
        public static <T> TypeReference<Map<String, T>> mapStringOf(Class<T> valueType) {
            return new TypeReference<>() {
            };
        }

        /**
         * 创建自定义 List&lt;T&gt; 类型的 TypeReference
         * <p>
         * 使用示例：
         * 
         * <pre>
         * TypeReference&lt;List&lt;UserInfo&gt;&gt; userListType = TypeRefs.listOf(UserInfo.class);
         * List&lt;UserInfo&gt; userList = CacheUtils.getGeneric("user_list", userListType);
         * </pre>
         * 
         * @param elementType 元素类型的Class
         * @return 新的 TypeReference 实例
         */
        public static <T> TypeReference<List<T>> listOf(Class<T> elementType) {
            return new TypeReference<>() {
            };
        }

        /**
         * 创建自定义 Map&lt;String, List&lt;T&gt;&gt; 类型的 TypeReference
         * <p>
         * 使用示例：
         * 
         * <pre>
         * TypeReference&lt;Map&lt;String, List&lt;ServiceConfig&gt;&gt;&gt; configMapType = TypeRefs
         *         .mapStringListOf(ServiceConfig.class);
         * </pre>
         * 
         * @param elementType List中元素的类型
         * @return 新的 TypeReference 实例
         */
        public static <T> TypeReference<Map<String, List<T>>> mapStringListOf(Class<T> elementType) {
            return new TypeReference<>() {
            };
        }
    }
}
