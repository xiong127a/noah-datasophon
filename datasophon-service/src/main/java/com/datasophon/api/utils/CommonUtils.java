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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        // 先按原有逻辑分组
        Map<String, List<ServiceConfig>> unsortedMap = list.stream()
                .collect(Collectors.groupingBy(config -> {
                    // 处理模板内容
                    String templateName = config.getTemplateName();
                    if (StrUtil.isNotBlank(templateName)) {
                        String templateContent = TemplatePathUtils.getTemplateContent(templateName);
                        config.setTemplateContent(templateContent);
                    }

                    // 首先检查是否有新的分组字段
                    if (config.getConfigCategory() != null && config.getConfigGroup() != null) {
                        return config.getConfigGroup();
                    }

                    // 如果没有新字段，回退到原有的分组逻辑
                    if (config.getConfigTargetRoles() != null) {
                        return config.getConfigTargetRoles();
                    } else {
                        return GENERAL;
                    }
                }));

        // 获取所有分组名称
        List<String> groupNames = new ArrayList<>(unsortedMap.keySet());

        // 使用ConfigGroupSorter对分组进行排序
        List<String> sortedGroups = ConfigGroupSorter.sortGroups(serviceName, groupNames);

        // 创建有序的LinkedHashMap来保持排序
        Map<String, List<ServiceConfig>> sortedMap = new LinkedHashMap<>();

        // 按照排序后的顺序重建map
        for (String groupName : sortedGroups) {
            if (unsortedMap.containsKey(groupName)) {
                sortedMap.put(groupName, unsortedMap.get(groupName));
            }
        }

        return sortedMap;
    }
}
