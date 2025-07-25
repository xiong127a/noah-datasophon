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

package com.datasophon.common.web;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局请求头处理工具类
 * 使用ThreadLocal存储当前请求的请求头信息
 */
public class HeaderContextHolder {
    // 使用ThreadLocal存储请求头信息，确保线程安全
    private static final ThreadLocal<Map<String, String>> HEADER_CONTEXT = new ThreadLocal<>();

    /**
     * 设置请求头值
     * 
     * @param name  请求头名称
     * @param value 请求头值
     */
    public static void setHeader(String name, String value) {
        Map<String, String> headers = HEADER_CONTEXT.get();
        if (headers == null) {
            headers = new HashMap<>();
            HEADER_CONTEXT.set(headers);
        }
        headers.put(name, value);
    }

    /**
     * 获取请求头值
     * 
     * @param name 请求头名称
     * @return 请求头值，如果不存在则返回null
     */
    public static String getHeader(String name) {
        Map<String, String> headers = HEADER_CONTEXT.get();
        return headers != null ? headers.get(name) : null;
    }

    /**
     * 获取所有请求头
     * 
     * @return 所有请求头的Map副本
     */
    public static Map<String, String> getAllHeaders() {
        Map<String, String> headers = HEADER_CONTEXT.get();
        return headers != null ? new HashMap<>(headers) : Map.of();
    }

    /**
     * 清除当前线程的请求头信息
     * 必须在请求处理完成后调用，否则会导致内存泄漏
     */
    public static void clear() {
        HEADER_CONTEXT.remove();
    }
}