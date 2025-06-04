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
        if (groups == null || groups.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取服务特定的排序规则
        Map<String, Integer> serviceSpecificOrder = SERVICE_GROUP_ORDER_MAP.getOrDefault(serviceName.toUpperCase(),
                new HashMap<>());

        // 首先按照基础规则排序
        List<String> initialSortedGroups = groups.stream()
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

        // 特殊处理：Kubernetes相关配置组始终排在最前面
        // 提取kubernetes相关的组和非kubernetes组
        List<String> kubernetesGroups = initialSortedGroups.stream()
                .filter(name -> name.endsWith("_Kubernetes") || name.startsWith("kubernetes.config."))
                .collect(Collectors.toList());

        List<String> nonKubernetesGroups = initialSortedGroups.stream()
                .filter(name -> !name.endsWith("_Kubernetes") && !name.startsWith("kubernetes.config."))
                .collect(Collectors.toList());

        // 合并结果，保持kubernetes组在前面
        List<String> finalSortedGroups = new ArrayList<>(kubernetesGroups);
        finalSortedGroups.addAll(nonKubernetesGroups);

        logger.info("最终排序后的配置组顺序: {}", finalSortedGroups);
        return finalSortedGroups;
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