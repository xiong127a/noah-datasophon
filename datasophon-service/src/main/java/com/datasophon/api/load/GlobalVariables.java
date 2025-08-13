package com.datasophon.api.load;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局变量缓存
 */
public class GlobalVariables {

    // （ clusterId -> (变量名称 -> 变量值) ）
    private static final Map<Long, Map<String, String>> map = new HashMap<>();

    public static void put(Long key, Map<String, String> value) {
        map.put(key, value);
    }

    public static Map<String, String> get(Long key) {
        return map.get(key);
    }

    public static boolean exists(Long key) {
        return map.containsKey(key);
    }
}
