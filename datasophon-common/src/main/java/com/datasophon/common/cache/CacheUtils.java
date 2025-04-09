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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.datasophon.common.Constants;
import com.datasophon.common.model.HostInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 获取指定复杂类型的缓存数据
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
        // 使用TypeReference获取泛型类型
        Map<String, HostInfo> result = getGeneric(key, new TypeReference<Map<String, HostInfo>>() {
        });
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
}
