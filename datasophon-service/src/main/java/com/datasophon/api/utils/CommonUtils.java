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
    public List<ServiceConfig> listServiceConfigByFrameServiceAndServiceRoleName(
            String configFileJson, String serviceRoleName) {

        // 1. 预处理构建角色配置映射
        Map<String, List<ServiceConfig>> roleConfigMap = buildRoleConfigMap(configFileJson);

        // 2. 直接获取目标角色的配置列表
        return roleConfigMap.getOrDefault(serviceRoleName, Collections.emptyList());
    }




    public static Map<String, List<ServiceConfig>> buildRoleConfigMap(String configFileJson) {
        // 1. 解析配置文件，获取 Generators 到配置列表的映射
        Map<Generators, List<ServiceConfig>> generatorsListMap = parseConfigJson(configFileJson);

        return generatorsListMap.entrySet().stream()
                .flatMap(entry -> {
                    Generators generator = entry.getKey();
                    List<ServiceConfig> configs = entry.getValue();

                    // 2. 直接获取单个角色（假设 configTargetRoles 存储单个角色名）
                    String role = generator.getConfigTargetRoles();

                    // 3. 生成键值对（角色 -> 配置列表）
                    return Stream.of(new AbstractMap.SimpleEntry<>(role, configs));
                })
                // 4. 合并相同角色的配置
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existingConfigs, newConfigs) -> {
                            // 合并策略：将新旧配置合并（可根据需求去重或覆盖）
                            List<ServiceConfig> merged = new ArrayList<>(existingConfigs);
                            merged.addAll(newConfigs);
                            return merged;
                        }
                ));
    }


    public static Map<Generators, List<ServiceConfig>> parseConfigJson(String configJson) {
        return JSON.parseObject(configJson,
                new TypeReference<Map<Generators, List<ServiceConfig>>>() {
                });
    }

    public static List<String> extractRoles(JSONObject jsonObject) {
        String roleJson = (String) jsonObject.getOrDefault(CONFIG_TARGET_ROLES, "CommonConfig");
        return JSONObject.parseObject(roleJson, new TypeReference<List<String>>() {});
    }

    public static List<ServiceConfig> parseServiceConfigs(JSONArray jsonArray) {
        return JSONObject.parseObject(jsonArray.toJSONString(),
                new TypeReference<List<ServiceConfig>>() {}
        );
    }
}
