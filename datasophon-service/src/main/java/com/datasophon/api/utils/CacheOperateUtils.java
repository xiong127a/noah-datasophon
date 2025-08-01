package com.datasophon.api.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.datasophon.api.master.handler.host.ServiceCacheSyncHandler;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.CacheCommand;
import com.datasophon.common.command.ConfigMapCacheCommand;
import com.datasophon.common.command.VariableCacheCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Slf4j
public class CacheOperateUtils {

    private static final ServiceCacheSyncHandler serviceCacheSyncHandler = new ServiceCacheSyncHandler();

    // 通用的获取远程缓存的方法
    private static Object getRemoteCache(String key) {
        if (key.equals(Constants.HOSTNAME)) {
            return null;
        }

        CacheCommand cacheCommand = new CacheCommand(key, false);
        ExecResult execResult = serviceCacheSyncHandler.serviceCacheSync(cacheCommand);
        return execResult.getExecResult() ? execResult.getObject() : null;
    }

    // 通用的检查远程缓存的方法
    private static boolean remoteCacheContains(String key) {
        if (key.equals(Constants.HOSTNAME)) {
            return false;
        }

        CacheCommand cacheCommand = new CacheCommand(key, false);
        ExecResult execResult = serviceCacheSyncHandler.serviceCacheSync(cacheCommand);
        return execResult.getExecResult();
    }

    // 通用的删除远程缓存的方法
    private static void remoteCacheRemove(String key) {
        if (key.equals(Constants.HOSTNAME)) {
            return;
        }

        CacheCommand cacheCommand = new CacheCommand(key, true);
        serviceCacheSyncHandler.serviceCacheSync(cacheCommand);
    }

    // 获取缓存对象，如果本地缓存没有，再去远程获取
    private static Object get(String key) {
        Object data = CacheUtils.get(key);
        if (ObjUtil.isNotEmpty(data)) {
            return data;
        }
        Object remoteCache = getRemoteCache(key);
        if (ObjectUtil.isNotEmpty(remoteCache)) {
            return getRemoteCache(key);
        }
        return data;
    }

    /**
     * 获取指定复杂泛型类型的缓存数据（如Map<String, Object>等）
     *
     * @param <T>           返回的数据类型
     * @param key           缓存键
     * @param typeReference 复杂类型的TypeReference
     * @return 指定类型的缓存数据，如果类型不匹配或缓存不存在则返回null
     */
    public static <T> T getGeneric(String key, com.alibaba.fastjson2.TypeReference<T> typeReference) {
        Object value = get(key);
        if (value == null) {
            return null;
        }

        try {
            // 通过JSON序列化和反序列化进行转换
            String jsonString = JSON.toJSONString(value);
            return JSON.parseObject(jsonString, typeReference);
        } catch (Exception e) {
            log.error("缓存数据类型转换错误，键: {}, 类型: {}, 错误: {}", key, typeReference.getType(), e.getMessage());
            return null;
        }
    }

    public static boolean containsKey(String key) {
        return CacheUtils.constainsKey(key) || remoteCacheContains(key);
    }


    public static void removeKey(String key) {
        CacheUtils.removeKey(key);
        remoteCacheRemove(key);
    }

    // 获取 Integer 类型的缓存
    public static Integer getInteger(String key) {
        Integer data = CacheUtils.getInteger(key);
        if (ObjectUtils.isEmpty(data)) {
            data = (Integer) getRemoteCache(key);
        }
        return data;
    }

    // 获取 Boolean 类型的缓存
    public static Boolean getBoolean(String key) {
        Boolean data = CacheUtils.getBoolean(key);
        if (ObjectUtils.isEmpty(data)) {
            data = (Boolean) getRemoteCache(key);
        }
        return data;
    }

    // 获取 String 类型的缓存
    public static String getString(String key) {
        String data = CacheUtils.getString(key);
        if (ObjectUtils.isEmpty(data)) {
            data = (String) getRemoteCache(key);
        }
        return data;
    }

    // 放入远程 Service 配置
    public static void putRemoteServiceConfigMap(String key, List<ServiceConfig> configs) {
        ConfigMapCacheCommand configMapCacheCommand = new ConfigMapCacheCommand(key, configs);
        serviceCacheSyncHandler.serviceCacheSync(configMapCacheCommand);
    }

    // 放入远程变量缓存
    public static void putRemoteVariableCache(String variableName, String value, Integer clusterId) {
        VariableCacheCommand variableCacheCommand = new VariableCacheCommand(variableName, value, clusterId);
        serviceCacheSyncHandler.serviceCacheSync(variableCacheCommand);
    }
}
