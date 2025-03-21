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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Cache<String, Object> cache = CacheUtil.newTimedCache(DEFAULT_TIMEOUT);

    public static Object get(String key) {
        Object value = cache.get(key);
        if (key != null && key.startsWith(CHECK_ITEM_LOG_PREFIX)) {
            logger.debug("获取日志缓存: {}, 是否存在: {}", key, value != null);
        }
        return value;
    }

    public static void put(String key, Object value) {
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
    }

    public static void clear() {
        cache.clear();
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
     * 获取指定类型的缓存对象
     * @param <T> 返回对象类型
     * @param key 缓存键
     * @param clazz 期望返回的类型
     * @return 缓存的对象，如果不存在或类型不匹配则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> clazz) {
        Object value = cache.get(key);
        if (value == null) {
            return null;
        }
        
        if (clazz.isInstance(value)) {
            return (T) value;
        } else {
            logger.warn("缓存对象类型不匹配，期望: {}, 实际: {}", clazz.getName(), value.getClass().getName());
            return null;
        }
    }
    
    /**
     * 放入缓存并指定过期时间
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间（毫秒）
     */
    public static void put(String key, Object value, long timeout) {
        cache.put(key, value, timeout);
    }
    
    /**
     * 删除缓存项
     * @param key 要删除的缓存键
     */
    public static void remove(String key) {
        cache.remove(key);
    }
}
