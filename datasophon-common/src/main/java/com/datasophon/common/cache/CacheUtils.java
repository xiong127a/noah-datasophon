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
import org.apache.commons.lang.StringUtils;
import com.datasophon.common.Constants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache工具类
 */
public class CacheUtils {
    private static final Logger logger = LoggerFactory.getLogger(CacheUtils.class);
    private static final String CHECK_ITEM_LOG_PREFIX = "CHECK_ITEM_LOG_";

    // 添加更多已知的缓存键前缀，用于缓存查询
    private static final String[] KNOWN_PREFIXES = {
            CHECK_ITEM_LOG_PREFIX, // 检查项日志前缀
            "HOST_MAP", // 主机映射
            "CLUSTER_", // 集群相关
            "SERVICE_", // 服务相关
            "CONFIG_", // 配置相关
            "TASK_", // 任务相关
            "USER_", // 用户相关
            "SESSION_", // 会话相关
            "AUTH_", // 认证相关
            "ROLE_", // 角色相关
            "PERM_", // 权限相关
            "MENU_", // 菜单相关
            "DASHBOARD_", // 仪表盘相关
            "ALERT_", // 告警相关
            "JOB_", // 作业相关
            "LOG_", // 日志相关
            "STAT_" // 统计相关
    };

    // 默认缓存过期时间为1小时
    private static final long DEFAULT_TIMEOUT = 3600000;

    // 日志缓存过期时间为24小时
    private static final long LOG_TIMEOUT = 24 * 3600000;

    // 使用定时缓存替代LRU缓存
    private static Cache<String, Object> cache = CacheUtil.newTimedCache(DEFAULT_TIMEOUT);

    // 用于记录所有添加到缓存中的键
    private static final Set<String> cacheKeys = ConcurrentHashMap.newKeySet();

    public static Object get(String key) {
        Object value = cache.get(key);
        if (key != null && key.startsWith(CHECK_ITEM_LOG_PREFIX)) {
            logger.debug("获取日志缓存: {}, 是否存在: {}", key, value != null);
        }
        return value;
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
     * 获取所有缓存键列表
     * 
     * @return 缓存键列表
     */
    public static String[] getCacheKeys() {
        // 直接返回记录的键集合
        return cacheKeys.toArray(new String[0]);
    }

    /**
     * 获取所有缓存内容
     * 
     * @return 包含所有缓存键值对的Map
     */
    public static Map<String, Object> getAllCache() {
        Map<String, Object> result = new HashMap<>();

        // 遍历记录的所有键，获取对应的值
        for (String key : cacheKeys) {
            if (cache.containsKey(key)) {
                result.put(key, cache.get(key));
            }
        }

        logger.debug("获取所有缓存，共{}个项目", result.size());
        return result;
    }

    /**
     * 获取指定前缀的所有缓存键
     * 
     * @param prefix 前缀
     * @return 匹配前缀的缓存键数组
     */
    public static String[] getKeysByPrefix(String prefix) {
        if (prefix == null) {
            return new String[0];
        }

        Set<String> matchedKeys = new HashSet<>();
        for (String key : cacheKeys) {
            if (key.startsWith(prefix)) {
                matchedKeys.add(key);
            }
        }

        return matchedKeys.toArray(new String[0]);
    }

    /**
     * 更新主机信息缓存
     * 
     * @param clusterId 集群ID
     * @param ip        主机IP
     * @param hostInfo  主机信息对象
     */
    public static void putHostInfo(Integer clusterId, String ip, Object hostInfo) {
        if (clusterId == null || StringUtils.isBlank(ip) || hostInfo == null) {
            logger.warn("更新主机缓存参数无效: clusterId={}, ip={}", clusterId, ip);
            return;
        }

        String cacheKey = clusterId + Constants.HOST_MAP;

        try {
            // 获取当前缓存
            Map<String, Object> hostMap = (Map<String, Object>) get(cacheKey);
            if (hostMap == null) {
                // 如果缓存不存在，创建新的Map
                hostMap = new ConcurrentHashMap<>();
            }

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
                Map<String, Object> hostMap = (Map<String, Object>) get(cacheKey);
                if (hostMap == null) {
                    hostMap = new ConcurrentHashMap<>();
                }
                hostMap.put(ip, hostInfo);
                put(cacheKey, hostMap);
                logger.info("重试更新主机缓存成功: clusterId={}, ip={}", clusterId, ip);
            } catch (Exception e2) {
                logger.error("重试更新主机缓存失败: clusterId={}, ip={}, 原因: {}", clusterId, ip, e2.getMessage(), e2);
            }
        }
    }
}
