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

package com.datasophon.api.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.api.service.ClusterServiceInstanceConfigService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ConfigVersionInfoService;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ConfigVersionInfoEntity;
import com.datasophon.dao.mapper.ClusterServiceInstanceConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.mybatisflex.core.query.QueryChain;

@Service("clusterServiceInstanceConfigService")
public class ClusterServiceInstanceConfigServiceImpl
                extends
                ServiceImpl<ClusterServiceInstanceConfigMapper, ClusterServiceInstanceConfigEntity>
                implements
                ClusterServiceInstanceConfigService {

        private ClusterServiceRoleGroupConfigService roleGroupConfigService;

        private final ConfigVersionInfoService configVersionInfoService;

        public ClusterServiceInstanceConfigServiceImpl(ConfigVersionInfoService configVersionInfoService) {
                this.configVersionInfoService = configVersionInfoService;
        }

        @Autowired
        public ClusterServiceInstanceConfigServiceImpl(ClusterServiceRoleGroupConfigService roleGroupConfigService) {
                this.roleGroupConfigService = roleGroupConfigService;
        }

        @Override
        public Result getServiceInstanceConfig(Integer serviceInstanceId, Integer version, Integer roleGroupId,
                        Integer page, Integer pageSize) {
                ClusterServiceRoleGroupConfig roleGroupConfig = roleGroupConfigService
                                .getConfigByRoleGroupIdAndVersion(roleGroupId, version);
                if (Objects.nonNull(roleGroupConfig)) {
                        List<ServiceConfig> serviceConfigs = JSON.parseObject(roleGroupConfig.getConfigJson(),
                                new TypeReference<>() {
                                });

                        // 设置服务名称，用于排序
                        String serviceName = roleGroupConfig.getServiceName();
                        serviceConfigs.forEach(config -> config.setServiceName(serviceName));

                        // 使用服务名称进行分组排序
                        Map<String, List<ServiceConfig>> roleToConfigMap = ConfigGroupUtils
                                        .groupByConfigTargetRoleOrCommon(serviceConfigs);
                        return Result.success(roleToConfigMap);
                }
                return Result.success();
        }

        @Override
        public ClusterServiceInstanceConfigEntity getServiceConfigByServiceId(Integer id) {
                return QueryChain.of(ClusterServiceInstanceConfigEntity.class)
                                .where(ClusterServiceInstanceConfigEntity::getServiceId).eq(id)
                                .orderBy(ClusterServiceInstanceConfigEntity::getConfigVersion).desc()
                                .limit(1)
                                .one();
        }

        @Override
        public Result getConfigVersion(Integer serviceInstanceId, Integer roleGroupId) {
                // 获取角色组的所有配置版本
                List<ClusterServiceRoleGroupConfig> list = QueryChain.of(ClusterServiceRoleGroupConfig.class)
                                .where(ClusterServiceRoleGroupConfig::getRoleGroupId).eq(roleGroupId)
                                .orderBy(ClusterServiceRoleGroupConfig::getConfigVersion).desc()
                                .list();

                // 如果没有配置版本，直接返回空列表
                if (list == null || list.isEmpty()) {
                        return Result.success(new ArrayList<>());
                }

                // 从角色组配置中提取版本号
                List<Integer> versionNumbers = list.stream()
                                .map(ClusterServiceRoleGroupConfig::getConfigVersion)
                                .toList();

                // 获取配置版本详情信息
                List<ConfigVersionInfoEntity> versionInfoList = configVersionInfoService
                                .getVersionInfoList("ROLE_GROUP", roleGroupId);

                // 创建版本详情Map (版本号 -> 版本详情)
                Map<Integer, ConfigVersionInfoEntity> versionInfoMap = new HashMap<>();
                if (versionInfoList != null && !versionInfoList.isEmpty()) {
                        versionInfoMap = versionInfoList.stream()
                                        .collect(Collectors.toMap(
                                                        ConfigVersionInfoEntity::getVersion,
                                                        versionInfo -> versionInfo,
                                                        (v1, v2) -> v1));
                }

                // 构建返回对象，将版本号和版本详情组合在一起
                List<Map<String, Object>> versionDetailsList = new ArrayList<>();
                for (ClusterServiceRoleGroupConfig config : list) {
                        Map<String, Object> versionDetail = new HashMap<>();
                        Integer version = config.getConfigVersion();
                        versionDetail.put("version", version);

                        // 添加版本详情信息（如果存在）
                        ConfigVersionInfoEntity versionInfo = versionInfoMap.get(version);
                        if (versionInfo != null) {
                                versionDetail.put("description", versionInfo.getDescription());
                                versionDetail.put("editor", versionInfo.getEditor());
                                versionDetail.put("editTime", versionInfo.getEditTime());
                                versionDetail.put("isCurrent", versionInfo.getIsCurrent());
                        } else {
                                versionDetail.put("description", null);
                                versionDetail.put("editor", null);
                                versionDetail.put("editTime", null);
                                versionDetail.put("isCurrent", false);
                        }

                        versionDetailsList.add(versionDetail);
                }

                // 为了保持向后兼容，如果前端仍在使用旧格式，可以通过参数控制返回格式
                // 这里默认返回新格式，包含详细信息
                return Result.success(versionDetailsList);
        }
}
