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

package com.datasophon.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置组排序工具类
 * 用于自定义每个服务的配置组显示顺序和名称
 */
public class ConfigGroupSorter {

    private static final Logger logger = LoggerFactory.getLogger(ConfigGroupSorter.class);

    // 存储每个服务的配置组排序规则
    private static final Map<String, Map<String, Integer>> SERVICE_GROUP_ORDER_MAP = new ConcurrentHashMap<>();

    // 存储配置组名称替换规则
    private static final Map<String, String> GROUP_NAME_REPLACEMENT_MAP = new ConcurrentHashMap<>();

    static {
        // 为HDFS服务定义默认排序
        Map<String, Integer> hdfsOrderMap = new HashMap<>();
        hdfsOrderMap.put("NameNode", 1);
        hdfsOrderMap.put("DataNode", 2);
        hdfsOrderMap.put("General", 3);
        // 添加高级配置组排序
        hdfsOrderMap.put("advanced_core-site", 4);
        hdfsOrderMap.put("advanced_hdfs-site", 5);
        hdfsOrderMap.put("custom_core-site", 6);
        hdfsOrderMap.put("custom_hdfs-site", 7);
        SERVICE_GROUP_ORDER_MAP.put("HDFS", hdfsOrderMap);
    }

    /**
     * 获取指定服务的配置组排序规则
     * 
     * @param serviceName 服务名称
     * @return 排序规则映射表，如果没有定义则返回空映射
     */
    public static Map<String, Integer> getServiceGroupOrder(String serviceName) {
        return SERVICE_GROUP_ORDER_MAP.getOrDefault(serviceName, Collections.emptyMap());
    }

    /**
     * 为指定服务设置配置组排序规则
     * 
     * @param serviceName   服务名称
     * @param groupOrderMap 配置组排序规则
     */
    public static void setServiceGroupOrder(String serviceName, Map<String, Integer> groupOrderMap) {
        SERVICE_GROUP_ORDER_MAP.put(serviceName, groupOrderMap);
    }

    /**
     * 添加或更新单个配置组的排序
     * 
     * @param serviceName 服务名称
     * @param groupName   配置组名称
     * @param order       排序顺序（从1开始）
     */
    public static void addGroupOrder(String serviceName, String groupName, int order) {
        Map<String, Integer> orderMap = SERVICE_GROUP_ORDER_MAP.computeIfAbsent(
                serviceName, k -> new HashMap<>());
        orderMap.put(groupName, order);
    }

    /**
     * 替换配置组名称
     * 
     * @param originalName 原始名称
     * @param newName      新名称
     */
    public static void replaceGroupName(String originalName, String newName) {
        GROUP_NAME_REPLACEMENT_MAP.put(originalName, newName);
    }

    /**
     * 获取替换后的配置组名称
     * 
     * @param originalName 原始名称
     * @return 替换后的名称，如果没有替换规则则返回原始名称
     */
    public static String getReplacedGroupName(String originalName) {
        return GROUP_NAME_REPLACEMENT_MAP.getOrDefault(originalName, originalName);
    }

    /**
     * 应用排序规则和名称替换，返回有序的配置组映射
     * 
     * @param unsortedMap 未排序的配置组映射
     * @param serviceName 服务名称
     * @return 排序后的配置组映射
     */
    public static <T> Map<String, T> applySorting(Map<String, T> unsortedMap, String serviceName) {
        // 获取服务的排序规则
        final Map<String, Integer> orderMap = getServiceGroupOrder(serviceName.toUpperCase());

        // 创建一个排序的映射表
        Map<String, T> sortedMap = new TreeMap<>((g1, g2) -> {
            // 替换配置组名称
            String name1 = getReplacedGroupName(g1);
            String name2 = getReplacedGroupName(g2);

            // 获取排序值
            Integer order1 = orderMap.getOrDefault(name1, Integer.MAX_VALUE);
            Integer order2 = orderMap.getOrDefault(name2, Integer.MAX_VALUE);

            // 优先比较排序值
            int result = order1.compareTo(order2);
            if (result != 0) {
                return result;
            }

            // 如果排序值相同，按名称字母顺序排序
            return name1.compareTo(name2);
        });

        // 将未排序的映射表复制到排序的映射表中，并替换组名
        unsortedMap.forEach((groupName, value) -> {
            String newGroupName = getReplacedGroupName(groupName);
            sortedMap.put(newGroupName, value);
        });

        return sortedMap;
    }
}