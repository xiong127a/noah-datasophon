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
import com.datasophon.api.service.ClusterServiceInstanceConfigService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ConfigVersionInfoService;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ConfigVersionInfoEntity;
import com.datasophon.dao.mapper.ClusterServiceInstanceConfigMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 集群服务实例配置服务实现
 * 按照架构重构规范，迁移QueryChain到DAO层，移除Result返回类型
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterServiceInstanceConfigService")
public class ClusterServiceInstanceConfigServiceImpl
                extends ServiceImpl<ClusterServiceInstanceConfigMapper, ClusterServiceInstanceConfigEntity>
                implements ClusterServiceInstanceConfigService {

        private static final Logger logger = LoggerFactory.getLogger(ClusterServiceInstanceConfigServiceImpl.class);

        @Autowired
        private ClusterServiceRoleGroupConfigService roleGroupConfigService;

        @Autowired
        private ConfigVersionInfoService configVersionInfoService;

        @Override
        public Map<String, Object> getServiceInstanceConfig(Integer serviceInstanceId, Integer version,
                        Integer roleGroupId,
                        Integer page, Integer pageSize) {
                ClusterServiceRoleGroupConfigDTO roleGroupConfigDTO = roleGroupConfigService
                                .getConfigByRoleGroupIdAndVersion(roleGroupId, version);
                if (Objects.nonNull(roleGroupConfigDTO)) {
                        List<ServiceConfig> serviceConfigs = JSON.parseObject(roleGroupConfigDTO.configJson(),
                                        new TypeReference<List<ServiceConfig>>() {
                                        });

                        // 设置服务名称，用于排序
                        String serviceName = roleGroupConfigDTO.serviceName();
                        serviceConfigs.forEach(config -> config.setServiceName(serviceName));

                        // 使用服务名称进行分组排序
                        Map<String, List<ServiceConfig>> roleToConfigMap = ConfigGroupUtils
                                        .groupByConfigTargetRoleOrCommon(serviceConfigs);

                        Map<String, Object> result = new HashMap<>();
                        result.put("data", roleToConfigMap);
                        return result;
                }
                return new HashMap<>();
        }

        @Override
        public ClusterServiceInstanceConfigEntity getServiceConfigByServiceId(Integer id) {
                return getMapper().selectLatestConfigByServiceId(id);
        }

        @Override
        public List<Map<String, Object>> getConfigVersion(Integer serviceInstanceId, Integer roleGroupId) {
                // 获取角色组的所有配置版本
                List<ClusterServiceRoleGroupConfig> list = roleGroupConfigService
                                .getConfigVersionsByRoleGroupId(roleGroupId);

                // 如果没有配置版本，直接返回空列表
                if (list == null || list.isEmpty()) {
                        return new ArrayList<>();
                }

                // JDK21现代特性：使用stream().toList()直接转换

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

                return versionDetailsList;
        }
}
