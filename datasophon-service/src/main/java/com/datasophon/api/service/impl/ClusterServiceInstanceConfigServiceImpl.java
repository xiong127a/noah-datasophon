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
import com.datasophon.api.converter.ClusterServiceInstanceConfigConverter;
import com.datasophon.api.service.ClusterServiceInstanceConfigService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ConfigVersionInfoService;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.common.dto.ClusterServiceInstanceConfigDTO;

import com.datasophon.common.dto.ConfigVersionDTO;
import com.datasophon.common.dto.ServiceInstanceConfigResultDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;

import com.datasophon.dao.entity.ConfigVersionInfoEntity;
import com.datasophon.dao.mapper.ClusterServiceInstanceConfigMapper;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;



/**
 * 集群服务实例配置服务实现
 * 继承ServiceImpl<ClusterServiceInstanceConfigMapper, ClusterServiceInstanceConfigEntity>，获得标准CRUD能力
 * 按照架构重构规范，ServiceImpl返回DTO，不返回Result，使用JDK21现代特性
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@Service("clusterServiceInstanceConfigService")
public class ClusterServiceInstanceConfigServiceImpl
                extends ServiceImpl<ClusterServiceInstanceConfigMapper, ClusterServiceInstanceConfigEntity>
                implements ClusterServiceInstanceConfigService {

        @Autowired
        private ClusterServiceInstanceConfigConverter configConverter;

        @Autowired
        private ClusterServiceRoleGroupConfigService roleGroupConfigService;

        @Autowired
        private ConfigVersionInfoService configVersionInfoService;

        @Override
        public ServiceInstanceConfigResultDTO getServiceInstanceConfig(Long serviceInstanceId, Integer version,
                        Long roleGroupId) {
                log.debug("获取服务实例配置: serviceInstanceId={}, version={}, roleGroupId={}", 
                         serviceInstanceId, version, roleGroupId);
                
                var roleGroupConfigDTO = roleGroupConfigService
                                .getConfigByRoleGroupIdAndVersion(roleGroupId, version);
                
                if (Objects.nonNull(roleGroupConfigDTO)) {
                        var serviceConfigs = JSON.parseObject(roleGroupConfigDTO.configJson(),
                                        new TypeReference<List<ServiceConfig>>() {});

                        // 设置服务名称，用于排序
                        var serviceName = roleGroupConfigDTO.serviceName();
                        serviceConfigs.forEach(config -> config.setServiceName(serviceName));

                        // 使用服务名称进行分组排序 - JDK21特性
                        var roleToConfigMap = ConfigGroupUtils
                                        .groupByConfigTargetRoleOrCommon(serviceConfigs);

                        return ServiceInstanceConfigResultDTO.create(roleToConfigMap);
                }
                
                return ServiceInstanceConfigResultDTO.empty();
        }

        @Override
        public ClusterServiceInstanceConfigDTO getServiceConfigByServiceId(Long serviceId) {
                log.debug("根据服务ID获取服务配置: {}", serviceId);
                
                var configEntity = getMapper().selectLatestConfigByServiceId(serviceId);
                return configEntity != null ? configConverter.entityToDto(configEntity) : null;
        }

        @Override
        public List<ConfigVersionDTO> getConfigVersion(Long serviceInstanceId, Long roleGroupId) {
                log.debug("获取配置版本列表: serviceInstanceId={}, roleGroupId={}", serviceInstanceId, roleGroupId);
                
                // 获取角色组的所有配置版本
                var configList = roleGroupConfigService.getConfigVersionsByRoleGroupId(roleGroupId);

                // 如果没有配置版本，直接返回空列表 - JDK21特性
                if (configList == null || configList.isEmpty()) {
                        return List.of();
                }

                // 获取配置版本详情信息
                var versionInfoList = configVersionInfoService.getVersionInfoList("ROLE_GROUP", roleGroupId);

                // 创建版本详情Map (版本号 -> 版本详情) - JDK21特性
                Map<Integer, ConfigVersionInfoEntity> versionInfoMap = versionInfoList != null && !versionInfoList.isEmpty() ?
                        versionInfoList.stream()
                                .collect(java.util.stream.Collectors.toMap(
                                        ConfigVersionInfoEntity::getVersion,
                                        versionInfo -> versionInfo,
                                        (v1, v2) -> v1)) : Map.<Integer, ConfigVersionInfoEntity>of();

                // 构建返回DTO列表 - 使用JDK21特性
                return configList.stream()
                        .map(config -> {
                                var version = config.getConfigVersion();
                                var versionInfo = versionInfoMap.get(version);
                                
                                return new ConfigVersionDTO(
                                        version,
                                        versionInfo != null ? versionInfo.getDescription() : null,
                                        versionInfo != null ? versionInfo.getEditor() : null,
                                        versionInfo != null ? versionInfo.getEditTime() : null,
                                        versionInfo != null && versionInfo.getIsCurrent() != null ? 
                                                versionInfo.getIsCurrent() : false
                                );
                        })
                        .collect(java.util.stream.Collectors.toList()); // 避免泛型推断问题
        }
        
        @Override
        public ClusterServiceInstanceConfigDTO createServiceInstanceConfig(ClusterServiceInstanceConfigDTO configDTO) {
                log.debug("创建服务实例配置: serviceId={}", configDTO.serviceId());
                
                // DTO转Entity
                var configEntity = configConverter.dtoToEntity(configDTO);
                
                // 保存到数据库
                save(configEntity);
                
                // Entity转DTO返回
                return configConverter.entityToDto(configEntity);
        }
        
        @Override
        public ClusterServiceInstanceConfigDTO updateServiceInstanceConfig(ClusterServiceInstanceConfigDTO configDTO) {
                log.debug("更新服务实例配置: {}", configDTO.id());
                
                // 检查配置是否存在
                var existingEntity = getById(configDTO.id());
                if (existingEntity == null) {
                        throw new com.datasophon.common.exception.BusinessException("服务实例配置不存在: " + configDTO.id());
                }
                
                // DTO转Entity
                var configEntity = configConverter.dtoToEntity(configDTO);
                
                // 更新数据库
                updateById(configEntity);
                
                // Entity转DTO返回
                return configConverter.entityToDto(configEntity);
        }
        
        @Override
        public ClusterServiceInstanceConfigDTO getServiceInstanceConfigById(Long id) {
                log.debug("根据ID获取服务实例配置: {}", id);
                
                var configEntity = getById(id);
                if (configEntity == null) {
                        throw new com.datasophon.common.exception.BusinessException("服务实例配置不存在: " + id);
                }
                
                return configConverter.entityToDto(configEntity);
        }
        
        @Override
        public PageResult<ClusterServiceInstanceConfigDTO> getServiceInstanceConfigListByPage(
                        Long clusterId, Long serviceId, Integer page, Integer pageSize) {
                log.debug("分页查询服务实例配置列表: clusterId={}, serviceId={}, page={}, pageSize={}", 
                         clusterId, serviceId, page, pageSize);
                
                // 调用DAO层方法，SQL逻辑在Mapper中处理
                var pageResult = getMapper().selectConfigPageByConditions(clusterId, serviceId, page, pageSize);
                
                // Entity列表转DTO列表 - 避免泛型推断问题
                var dtoList = pageResult.getRecords().stream()
                        .map(configConverter::entityToDto)
                        .collect(java.util.stream.Collectors.toList());
                
                return PageResult.of(dtoList, pageResult.getTotalRow(), page, pageSize);
        }
}
