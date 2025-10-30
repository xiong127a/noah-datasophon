package com.datasophon.api.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
     * @return 指定类型的缓存数据，如果类型不匹配或缓存不存在则返回对应类型的空对象
     */
    public static <T> T getGeneric(String key, TypeReference<T> typeReference) {
        Object value = get(key);
        if (value == null) {
            log.debug("缓存键 [{}] 不存在，返回空对象", key);
            return createEmptyObject(typeReference);
        }

        try {
            // 通过JSON序列化和反序列化进行转换
            String jsonString = JSON.toJSONString(value);
            T result = JSON.parseObject(jsonString, typeReference);
            return result != null ? result : createEmptyObject(typeReference);
        } catch (Exception e) {
            log.error("缓存数据类型转换错误，键: {}, 类型: {}, 错误: {}, 返回空对象",
                    key, typeReference.getType(), e.getMessage());
            return createEmptyObject(typeReference);
        }
    }

    /**
     * 根据TypeReference创建对应类型的空对象
     * 
     * @param <T>           返回的数据类型
     * @param typeReference 类型引用
     * @return 对应类型的空对象
     */
    private static <T> T createEmptyObject(TypeReference<T> typeReference) {
        Type type = typeReference.getType();
        Object result;

        // 处理原始类型（Class）
        if (type instanceof Class<?> clazz) {
            result = createEmptyObjectByRawClass(clazz);
        }
        // 处理泛型类型（ParameterizedType）
        else if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class<?> rawClass) {
                result = createEmptyCollectionByRawClass(rawClass);
            } else {
                result = null;
            }
        } else {
            // 其他情况返回null
            log.debug("无法为类型 [{}] 创建空对象，返回null", type);
            result = null;
        }

        // 类型安全地转换：先检查类型兼容性
        if (result != null && type instanceof Class<?> targetClass) {
            if (targetClass.isInstance(result)) {
                return (T) targetClass.cast(result);
            }
        }
        
        // 对于泛型类型，使用JSON序列化/反序列化来确保类型安全
        // 这样可以避免unchecked转换，利用JSON库的类型转换能力
        if (result != null) {
            try {
                String jsonString = JSON.toJSONString(result);
                return JSON.parseObject(jsonString, typeReference);
            } catch (Exception e) {
                log.warn("通过JSON转换空对象失败: 期望类型 {}, 实际对象 {}, 错误: {}", 
                        type, result, e.getMessage());
                // JSON转换失败，返回null
                return null;
            }
        }
        
        return null;
    }

    /**
     * 根据原始类创建空集合对象
     * 返回Object类型，由调用方负责类型安全
     */
    private static Object createEmptyCollectionByRawClass(Class<?> rawClass) {
        // Map类型
        if (Map.class.isAssignableFrom(rawClass)) {
            if (ConcurrentHashMap.class.isAssignableFrom(rawClass)) {
                return new ConcurrentHashMap<>();
            } else {
                return new HashMap<>();
            }
        }

        // List类型
        if (List.class.isAssignableFrom(rawClass)) {
            return new ArrayList<>();
        }

        // Set类型
        if (Set.class.isAssignableFrom(rawClass)) {
            return new HashSet<>();
        }

        return null;
    }

    /**
     * 根据Class创建空对象
     * 返回Object类型，由调用方负责类型安全
     */
    private static Object createEmptyObjectByRawClass(Class<?> clazz) {
        // String类型
        if (String.class.equals(clazz)) {
            return "";
        }

        // 基本类型和包装类型
        if (Integer.class.equals(clazz) || int.class.equals(clazz)) {
            return Integer.valueOf(0);
        }
        if (Long.class.equals(clazz) || long.class.equals(clazz)) {
            return Long.valueOf(0L);
        }
        if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
            return Boolean.FALSE;
        }
        if (Double.class.equals(clazz) || double.class.equals(clazz)) {
            return Double.valueOf(0.0);
        }
        if (Float.class.equals(clazz) || float.class.equals(clazz)) {
            return Float.valueOf(0.0f);
        }

        // 集合类型委托给专门的方法处理
        return createEmptyCollectionByRawClass(clazz);
    }

    public static boolean containsKey(String key) {
        return CacheUtils.containsKey(key) || remoteCacheContains(key);
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
    public static void putRemoteVariableCache(String variableName, String value, Long clusterId) {
        VariableCacheCommand variableCacheCommand = new VariableCacheCommand(variableName, value, clusterId);
        serviceCacheSyncHandler.serviceCacheSync(variableCacheCommand);
    }
}
