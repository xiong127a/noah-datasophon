package com.datasophon.api.master;

import java.util.HashMap;

public class CancelCommandMap {
    private static final HashMap<Long, String> map = new HashMap<>();

    public static void put(Long key, String value) {
        map.put(key, value);
    }

    public static String get(Long key) {
        return map.get(key);
    }

    public static boolean exists(Long key) {
        return map.containsKey(key);
    }
}
