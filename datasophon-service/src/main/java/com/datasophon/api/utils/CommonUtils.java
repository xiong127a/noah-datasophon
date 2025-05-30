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

import cn.hutool.core.util.StrUtil;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.enums.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.datasophon.common.Constants.GENERAL;

public class CommonUtils {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);

    private CommonUtils() {
    }

    public static void updateInstallState(InstallState installState, HostInfo hostInfo) {
        hostInfo.setInstallState(installState);
        hostInfo.setInstallStateCode(installState.getValue());
    }

    public static RoleType convertRoleType(String roleType) {
        if (roleType == null || "".equals(roleType.trim())) {
            logger.error("Convert role type failed, roleType is null.");
            return null;
        }
        try {
            return RoleType.valueOf(roleType.toUpperCase());
        } catch (Exception e) {
            logger.error("Unsupported role type:{}", roleType);
            return null;
        }
    }

    public static Map<String, String> buildNameToRoleMap(Map<Generators, List<ServiceConfig>> configFileMap) {
        return configFileMap.entrySet().stream()
                .flatMap(entry -> {
                    Generators generator = entry.getKey();
                    List<ServiceConfig> configs = entry.getValue();

                    String configTargetRoles = generator.getConfigTargetRoles();
                    if (configTargetRoles == null) {

                        return configs.stream()
                                .map(config -> new AbstractMap.SimpleEntry<>(config.getName(), GENERAL));
                    }
                    return configs.stream()
                            .map(config -> new AbstractMap.SimpleEntry<>(config.getName(), configTargetRoles));
                })
                // 4. 收集成 Map<String, String>，即 Name -> Role
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existingValue, newValue) -> GENERAL));
    }

    public static List<ServiceConfig> filterByServiceRoleName(List<ServiceConfig> list, String serviceRoleName) {
        return list.stream()
                .filter(serviceConfig -> serviceRoleName.equals(serviceConfig.getConfigTargetRoles()))
                .collect(Collectors.toList());
    }

    /**
     * 将配置项按配置组分组
     * 
     * @param list 配置项列表
     * @return 按配置组分组后的映射
     */
    public static Map<String, List<ServiceConfig>> groupByConfigTargetRoleOrCommon(String serviceName,
            List<ServiceConfig> list) {
        // 最终返回结果
        Map<String, List<ServiceConfig>> resultMap = new LinkedHashMap<>();

        // 先处理所有配置项的模板内容
        for (ServiceConfig config : list) {
            String templateName = config.getTemplateName();
            if (StrUtil.isNotBlank(templateName)) {
                String templateContent = TemplatePathUtils.getTemplateContent(templateName);
                config.setTemplateContent(templateContent);
            }
        }

        // 分组存储配置
        Map<String, List<ServiceConfig>> groupedConfigs = new HashMap<>();

        // 处理所有配置
        for (ServiceConfig config : list) {
            String groupKey;

            // 判断是否为Kubernetes配置
            if (config.getConfigGroup() != null && config.getConfigGroup().startsWith("kubernetes.config.")) {
                // 对于K8s配置，其 configGroup 属性应该已经是期望的、可能带有角色后缀的最终形态。
                // (e.g., "kubernetes.config.pvc.ZkServer" or "kubernetes.config.general")
                // 因此，直接使用它作为 groupKey。
                groupKey = config.getConfigGroup();
            }
            // 非Kubernetes配置
            else {
                String configCategory = config.getConfigCategory();
                String configLevel = config.getConfigLevel();
                String actualConfigGroup = config.getConfigGroup();

                if ("file".equals(configCategory) &&
                        ("custom".equalsIgnoreCase(configLevel) || "advanced".equalsIgnoreCase(configLevel)) &&
                        StrUtil.isNotBlank(actualConfigGroup)) {

                    String levelPrefix = configLevel.toLowerCase() + "_";
                    if (actualConfigGroup.startsWith(levelPrefix)) {
                        groupKey = actualConfigGroup;
                    } else {
                        groupKey = levelPrefix + actualConfigGroup;
                    }
                }
                // Fallback to existing logic if the above condition is not met
                else if (configCategory != null && actualConfigGroup != null) {
                    groupKey = actualConfigGroup;
                } else if (config.getConfigTargetRoles() != null) {
                    groupKey = config.getConfigTargetRoles();
                } else {
                    groupKey = GENERAL;
                }
            }

            // 将配置添加到对应分组
            groupedConfigs.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(config);
        }

        // 使用ConfigGroupSorter对分组进行排序
        List<String> sortedGroups = ConfigGroupSorter.sortGroups(serviceName, groupedConfigs.keySet());

        // 按照排序后的顺序重建map
        for (String groupName : sortedGroups) {
            List<ServiceConfig> configs = groupedConfigs.get(groupName);
            if (configs != null && !configs.isEmpty()) {
                resultMap.put(groupName, configs);
            }
        }

        return resultMap;
    }
}
