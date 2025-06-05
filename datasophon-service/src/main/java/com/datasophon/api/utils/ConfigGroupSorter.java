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
    private static final int KUBERNETES_GROUP_PRIORITY = 50; // Kubernetes配置最高优先级
    private static final int ROLE_GROUP_PRIORITY = 100; // 角色分组次高优先级
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

        int priority;
        if (groupName.startsWith("kubernetes.config.")) {
            // Kubernetes配置组有最高优先级
            priority = KUBERNETES_GROUP_PRIORITY;
        } else if (groupName.startsWith("custom_")) {
            priority = CUSTOM_GROUP_PRIORITY;
        } else if (groupName.startsWith("advanced_")) {
            priority = ADVANCED_GROUP_PRIORITY;
        } else if (groupName.equals("General") || groupName.equals("CommonConfig")) {
            priority = GENERAL_GROUP_PRIORITY;
        } else {
            // 假设其他都是角色分组
            priority = ROLE_GROUP_PRIORITY;
        }

        logger.debug("分组 [{}] 的基础优先级为: {}", groupName, priority);
        return priority;
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

        logger.debug("开始排序配置组，服务名: {}, 原始分组: {}", serviceName, groups);

        // 获取服务特定的排序规则
        Map<String, Integer> serviceSpecificOrder = SERVICE_GROUP_ORDER_MAP.getOrDefault(serviceName.toUpperCase(),
                new HashMap<>());

        logger.debug("服务特定排序规则: {}", serviceSpecificOrder);

        // 首先按照基础规则排序
        List<String> sortedGroups = groups.stream()
                .sorted((g1, g2) -> {
                    // 1. 首先按照基础分组类型优先级排序
                    int basePriority1 = getBaseGroupPriority(g1);
                    int basePriority2 = getBaseGroupPriority(g2);

                    logger.debug("分组比较: {} (优先级: {}) vs {} (优先级: {})",
                            g1, basePriority1, g2, basePriority2);

                    if (basePriority1 != basePriority2) {
                        return basePriority1 - basePriority2;
                    }

                    // 2. 如果基础优先级相同，使用服务特定的排序规则
                    Integer order1 = serviceSpecificOrder.get(g1);
                    Integer order2 = serviceSpecificOrder.get(g2);

                    logger.debug("基础优先级相同，使用服务特定规则: {} (顺序: {}) vs {} (顺序: {})",
                            g1, order1, g2, order2);

                    if (order1 != null && order2 != null) {
                        return order1.compareTo(order2);
                    } else if (order1 != null) {
                        return -1;
                    } else if (order2 != null) {
                        return 1;
                    }

                    // 3. 对于kubernetes.config.类型的配置组，进一步按配置类型排序
                    if (g1.startsWith("kubernetes.config.") && g2.startsWith("kubernetes.config.")) {
                        String[] parts1 = g1.split("\\.");
                        String[] parts2 = g2.split("\\.");

                        // 比较配置类型部分 (第3部分)
                        if (parts1.length >= 3 && parts2.length >= 3) {
                            String type1 = parts1[2];
                            String type2 = parts2[2];
                            logger.debug("Kubernetes配置类型比较: {} vs {}", type1, type2);
                            int typeCompare = type1.compareTo(type2);
                            if (typeCompare != 0) {
                                return typeCompare;
                            }
                        }
                    }

                    // 4. 如果都没有特定顺序，按名称字母顺序排序
                    logger.debug("无特定排序规则，按字母排序: {} vs {}", g1, g2);
                    return g1.compareTo(g2);
                })
                .collect(Collectors.toList());

        logger.debug("排序后的配置组顺序: {}", sortedGroups);

        // 确保General组总是第一个
        if (sortedGroups.contains("General") && sortedGroups.indexOf("General") > 0) {
            sortedGroups.remove("General");
            sortedGroups.add(0, "General");
            logger.debug("将General组移到首位: {}", sortedGroups);
        }

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