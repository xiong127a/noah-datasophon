package com.datasophon.api.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.datasophon.api.master.handler.host.ServiceCacheSyncHandler;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.CacheCommand;
import com.datasophon.common.command.ConfigMapCacheCommand;
import com.datasophon.common.command.VariableCacheCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import org.springframework.util.ObjectUtils;

import java.util.List;

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
    public static Object get(String key) {
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
