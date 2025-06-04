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
import java.util.stream.Collectors;

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

    // 通用分组类型的优先级
    private static final int ROLE_GROUP_PRIORITY = 100; // 角色分组最高优先级
    private static final int GENERAL_GROUP_PRIORITY = 200; // 通用配置中等优先级
    private static final int ADVANCED_GROUP_PRIORITY = 300; // 高级配置较低优先级
    private static final int CUSTOM_GROUP_PRIORITY = 400; // 自定义配置最低优先级

    static {
        // 为HDFS服务定义默认排序
        Map<String, Integer> hdfsOrderMap = new HashMap<>();
        // 角色分组（优先级最高）
        hdfsOrderMap.put("NameNode", ROLE_GROUP_PRIORITY + 1);
        hdfsOrderMap.put("DataNode", ROLE_GROUP_PRIORITY + 2);
        hdfsOrderMap.put("JournalNode", ROLE_GROUP_PRIORITY + 3);
        hdfsOrderMap.put("ZKFC", ROLE_GROUP_PRIORITY + 4);
        hdfsOrderMap.put("HttpFs", ROLE_GROUP_PRIORITY + 5);
        // 通用配置（中等优先级）
        hdfsOrderMap.put("General", GENERAL_GROUP_PRIORITY);
        // 高级配置（较低优先级）
        hdfsOrderMap.put("advanced_core-site", ADVANCED_GROUP_PRIORITY + 1);
        hdfsOrderMap.put("advanced_hdfs-site", ADVANCED_GROUP_PRIORITY + 2);
        hdfsOrderMap.put("advanced_hadoop-env", ADVANCED_GROUP_PRIORITY + 3);
        hdfsOrderMap.put("advanced_httpfs-site", ADVANCED_GROUP_PRIORITY + 4);
        // 自定义配置（最低优先级）
        hdfsOrderMap.put("custom_core-site", CUSTOM_GROUP_PRIORITY + 1);
        hdfsOrderMap.put("custom_hdfs-site", CUSTOM_GROUP_PRIORITY + 2);
        hdfsOrderMap.put("custom_httpfs-site", CUSTOM_GROUP_PRIORITY + 3);
        SERVICE_GROUP_ORDER_MAP.put("HDFS", hdfsOrderMap);
    }

    /**
     * 获取分组类型的基础优先级
     * 
     * @param groupName 分组名称
     * @return 基础优先级
     */
    private static int getBaseGroupPriority(String groupName) {
        if (groupName == null) {
            return Integer.MAX_VALUE;
        }

        if (groupName.startsWith("custom_")) {
            return CUSTOM_GROUP_PRIORITY;
        } else if (groupName.startsWith("advanced_")) {
            return ADVANCED_GROUP_PRIORITY;
        } else if (groupName.equals("General") || groupName.equals("CommonConfig")) {
            return GENERAL_GROUP_PRIORITY;
        } else {
            // 假设其他都是角色分组
            return ROLE_GROUP_PRIORITY;
        }
    }

    /**
     * 对配置组进行排序
     * 
     * @param serviceName 服务名称
     * @param groups      配置组集合
     * @return 排序后的配置组列表
     */
    public static List<String> sortGroups(String serviceName, Collection<String> groups) {
        // 获取服务特定的排序规则
        Map<String, Integer> serviceSpecificOrder = SERVICE_GROUP_ORDER_MAP.getOrDefault(serviceName.toUpperCase(),
                new HashMap<>());

        return groups.stream()
                .sorted((g1, g2) -> {
                    // 1. 首先按照基础分组类型优先级排序
                    int basePriority1 = getBaseGroupPriority(g1);
                    int basePriority2 = getBaseGroupPriority(g2);

                    if (basePriority1 != basePriority2) {
                        return basePriority1 - basePriority2;
                    }

                    // 2. 如果基础优先级相同，使用服务特定的排序规则
                    Integer order1 = serviceSpecificOrder.get(g1);
                    Integer order2 = serviceSpecificOrder.get(g2);

                    if (order1 != null && order2 != null) {
                        return order1.compareTo(order2);
                    } else if (order1 != null) {
                        return -1;
                    } else if (order2 != null) {
                        return 1;
                    }

                    // 3. 如果都没有特定顺序，按名称字母顺序排序
                    return g1.compareTo(g2);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取指定服务的配置组排序规则
     * 
     * @param serviceName 服务名称
     * @return 排序规则映射表，如果没有定义则返回空映射
     */
    public static Map<String, Integer> getServiceGroupOrder(String serviceName) {
        return SERVICE_GROUP_ORDER_MAP.getOrDefault(serviceName.toUpperCase(), Collections.emptyMap());
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
        SERVICE_GROUP_ORDER_MAP.computeIfAbsent(serviceName.toUpperCase(), k -> new HashMap<>())
                .put(groupName, order);
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
     * 获取配置组显示名称
     */
    public static String getDisplayName(String groupName) {
        if (groupName == null) {
            return "";
        }

        if (groupName.startsWith("advanced_")) {
            return "高级 " + groupName.substring("advanced_".length());
        } else if (groupName.startsWith("custom_")) {
            return "自定义 " + groupName.substring("custom_".length());
        }

        return GROUP_NAME_REPLACEMENT_MAP.getOrDefault(groupName, groupName);
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

    /**
     * 对配置组进行排序，将kubernetes配置组放在前面
     *
     * @param serviceName 服务名称
     * @param groupNames  配置组名称列表
     * @return 排序后的配置组名称列表
     */
    public static List<String> sortGroups(String serviceName, List<String> groupNames) {
        if (groupNames == null || groupNames.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取以"kubernetes.config."开头的组和其他组
        List<String> kubernetesGroups = groupNames.stream()
                .filter(name -> name.endsWith("_Kubernetes"))
                .collect(Collectors.toList());

        List<String> nonKubernetesGroups = groupNames.stream()
                .filter(name -> !name.endsWith("_Kubernetes"))
                .collect(Collectors.toList());

        // 对非Kubernetes组进行排序
        nonKubernetesGroups.sort((g1, g2) -> {
            // 自定义排序规则
            if ("General".equals(g1))
                return -1;
            if ("General".equals(g2))
                return 1;
            return g1.compareTo(g2);
        });

        // 将Kubernetes组和其他组合并，Kubernetes组在前面
        List<String> sortedGroups = new ArrayList<>(kubernetesGroups);
        sortedGroups.addAll(nonKubernetesGroups);

        return sortedGroups;
    }

    /**
     * 为Kubernetes配置项的名称添加角色前缀
     *
     * @param roleName    角色名称
     * @param configName  配置项名称
     * @param configGroup 配置分组
     * @return 添加了角色前缀的配置项名称
     */
    public static String addRolePrefixForKubernetesConfig(String roleName, String configName, String configGroup) {
        if (roleName == null || configName == null) {
            return configName;
        }

        // 如果配置组是Kubernetes相关的，添加角色前缀
        if (configGroup != null && configGroup.startsWith("kubernetes.config.")) {
            // 将角色名转换为小写下划线格式，保持与ProcessUtils.generateConfigFileMap一致
            String normRoleName = roleName.toLowerCase().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            return normRoleName + "_" + configName;
        }

        return configName;
    }
}