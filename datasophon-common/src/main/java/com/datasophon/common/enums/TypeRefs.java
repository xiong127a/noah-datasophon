package com.datasophon.common.enums;

import com.alibaba.fastjson2.TypeReference;
import com.datasophon.common.model.HostInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeRefs {
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