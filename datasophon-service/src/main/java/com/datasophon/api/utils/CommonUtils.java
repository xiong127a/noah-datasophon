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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.enums.RoleType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.datasophon.common.Constants.COMMON_CONFIG;
import static com.datasophon.common.Constants.CONFIG_TARGET_ROLES;

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
                                .map(config -> new AbstractMap.SimpleEntry<>(config.getName(), COMMON_CONFIG));
                    }
                    return configs.stream()
                            .map(config -> new AbstractMap.SimpleEntry<>(config.getName(), configTargetRoles));
                })
                // 4. 收集成 Map<String, String>，即 Name -> Role
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existingValue, newValue) -> COMMON_CONFIG
                ));
    }

    public static List<ServiceConfig> filterByServiceRoleName(List<ServiceConfig> list, String serviceRoleName) {
        return list.stream()
                .filter(serviceConfig -> serviceRoleName.equals(serviceConfig.getConfigTargetRoles()))
                .collect(Collectors.toList());
    }
    public static Map<String, List<ServiceConfig>> groupByConfigTargetRoleOrCommon(List<ServiceConfig> list) {
        return list.stream()
                .collect(Collectors.groupingBy(
                        // 如果 configTargetRoles 为空，则使用 COMMON_CONFIG 作为键
                        config -> config.getConfigTargetRoles() != null
                                ? config.getConfigTargetRoles()
                                : COMMON_CONFIG,
                        // 保持插入顺序（可选）
                        LinkedHashMap::new,
                        // 收集为 List<ServiceConfig>
                        Collectors.toList()
                ));
    }


}
