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

import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.load.model.ServiceMetaConfig;
import com.datasophon.api.service.*;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.api.utils.ProcessUtils;

import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.api.converter.FrameServiceConverter;
import com.datasophon.api.converter.FrameServiceRoleConverter;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import static com.datasophon.common.Constants.GENERAL;

/**
 * 服务元数据事务处理服务实现
 * 专注于数据库操作，避免循环调用
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com  
 * 日期：2025-01-20
 */
@Service
public class ServiceMetadataTransactionServiceImpl implements ServiceMetadataTransactionService {
    
    @Autowired
    private FrameServiceService frameServiceService;
    
    @Autowired
    private FrameServiceRoleService roleService;
    
    @Autowired
    private ClusterInfoService clusterInfoService;
    
    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;
    
    @Autowired
    private ClusterServiceInstanceRoleGroupService roleGroupService;
    
    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FrameServiceEntity saveFrameServiceInTransaction(ServiceMetaConfig config) {
        // 使用新的非异常方法查找服务
        var serviceDto = frameServiceService.findServiceByFrameIdAndServiceName(
                config.frameInfo().getId(), config.serviceName()).orElse(null);
        
        var serviceConverter = SpringUtil.getBean(FrameServiceConverter.class);
        var serviceEntity = serviceDto != null ? serviceConverter.dtoToEntity(serviceDto) : null;
        var parameters = config.parameters();
        // 使用JDK21框架隔离版本，传入框架代码确保配置隔离
        var nameToRoleMap = ConfigGroupUtils.buildNameToRoleMap(config.configFileMap(), config.frameCode());

        // 使用现代化流式API处理配置目标角色
        parameters.stream()
                .filter(serviceConfig -> ObjectUtils.isEmpty(serviceConfig.getConfigTargetRoles()))
                .forEach(serviceConfig -> {
                    var configTargetRoles = nameToRoleMap.getOrDefault(serviceConfig.getName(), GENERAL);
                    serviceConfig.setConfigTargetRoles(configTargetRoles);
                });

        // JDK 21现代化处理服务实体状态
        if (serviceEntity == null) {
            serviceEntity = new FrameServiceEntity();
            buildServiceEntity(config, serviceEntity);
            frameServiceService.save(serviceEntity);
        } else if (!serviceEntity.getServiceJsonMd5().equals(config.serviceInfoMd5())) {
            var configMapStr = JSONObject.toJSONString(config.configFileMap());
            var configFileMapStrMd5 = SecureUtil.md5(configMapStr);
            
            if (!configFileMapStrMd5.equals(serviceEntity.getConfigFileJsonMd5())) {
                updateServiceInstanceConfigInTransaction(config.frameCode(), 
                        config.serviceInfo().getName(), 
                        config.serviceInfo().getParameters());
            }
            buildServiceEntity(config, serviceEntity);
            frameServiceService.updateById(serviceEntity);
        }

        return serviceEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFrameServiceRoleInTransaction(ServiceMetaConfig config, FrameServiceEntity serviceEntity) {
        var serviceRoles = config.serviceRoles();
        
        // 使用虚拟线程处理服务角色，确保角色级别的隔离
        try (var roleExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var roleTasks = serviceRoles.stream()
                .map(serviceRole -> 
                    roleExecutor.submit(() -> processServiceRole(config, serviceEntity, serviceRole))
                )
                .toList();
            
            // 等待所有角色处理完成
            roleTasks.forEach(task -> {
                try {
                    task.get();
                } catch (Exception e) {
                    throw new RuntimeException("服务角色处理失败", e);
                }
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateServiceInstanceConfigInTransaction(String frameCode, String serviceName, List<ServiceConfig> parameters) {
        // 查询frameCode相同的集群
        var clusters = clusterInfoService.getClusterByFrameCode(frameCode);
        // 查询集群的服务实例
        for (var cluster : clusters) {
            var serviceInstanceDto = serviceInstanceService
                    .getServiceInstanceByClusterIdAndServiceName(
                            cluster.id(), serviceName);
            if (Objects.nonNull(serviceInstanceDto)) {
                var configDto = roleGroupService
                        .getRoleGroupConfigByServiceId(serviceInstanceDto.id());
                var configConverter = SpringUtil.getBean(ClusterServiceRoleGroupConfigConverter.class);
                var config = configConverter.dtoToEntity(configDto);
                var configJson = config.getConfigJson();
                var serviceConfigs = JSONArray.parseArray(configJson, ServiceConfig.class);
                ProcessUtils.addAll(serviceConfigs, parameters);
                // 更新服务实例的配置
                config.setConfigJson(JSONObject.toJSONString(serviceConfigs));
                roleGroupConfigService.updateById(config);
            }
        }
    }

    /**
     * 处理单个服务角色
     */
    private void processServiceRole(ServiceMetaConfig config, FrameServiceEntity serviceEntity, 
                                  ServiceRoleInfo serviceRole) {
        var serviceRoleJson = JSONObject.toJSONString(serviceRole);
        var serviceRoleJsonMd5 = SecureUtil.md5(serviceRoleJson);
        
        saveOrUpdateServiceRole(config, serviceEntity, serviceRole, serviceRoleJson, serviceRoleJsonMd5);
    }

    /**
     * 保存或更新服务角色实体
     */
    private void saveOrUpdateServiceRole(ServiceMetaConfig config, FrameServiceEntity serviceEntity,
                                       ServiceRoleInfo serviceRole, String serviceRoleJson, 
                                       String serviceRoleJsonMd5) {
        // 使用新的非异常方法查找服务角色
        var roleDto = roleService.findServiceRoleByServiceIdAndServiceRoleName(
                serviceEntity.getId(), serviceRole.getName()).orElse(null);
        var roleConverter = SpringUtil.getBean(FrameServiceRoleConverter.class);
        var role = roleDto != null ? roleConverter.dtoToEntity(roleDto) : null;
        
        // JDK 21现代化处理 - 使用简洁的条件处理
        if (role == null) {
            var newRole = new FrameServiceRoleEntity();
            buildFrameServiceRole(config, serviceEntity, serviceRole, 
                    serviceRoleJson, serviceRoleJsonMd5, newRole);
            roleService.save(newRole);
        } else if (!role.getServiceRoleJsonMd5().equals(serviceRoleJsonMd5)) {
            buildFrameServiceRole(config, serviceEntity, serviceRole, 
                    serviceRoleJson, serviceRoleJsonMd5, role);
            roleService.updateById(role);
        }
    }

    /**
     * 构建框架服务角色实体 - 重构使用JDK 21新特性
     */
    private void buildFrameServiceRole(
            ServiceMetaConfig config,
            FrameServiceEntity serviceEntity,
            ServiceRoleInfo serviceRole,
            String serviceRoleJson,
            String serviceRoleJsonMd5,
            FrameServiceRoleEntity role) {
        role.setServiceId(serviceEntity.getId());
        role.setServiceRoleName(serviceRole.getName());
        role.setCardinality(serviceRole.getCardinality());
        role.setFrameCode(config.frameCode());
        role.setServiceRoleJson(serviceRoleJson);
        role.setServiceRoleType(CommonUtils.convertRoleType(serviceRole.getRoleType().getName()));
        role.setJmxPort(serviceRole.getJmxPort());
        role.setServiceRoleJsonMd5(serviceRoleJsonMd5);
        role.setLogFile(serviceRole.getLogFile());
    }

    /**
     * 构建服务实体 - 重构使用JDK 21新特性
     */
    private void buildServiceEntity(ServiceMetaConfig config, FrameServiceEntity serviceEntity) {
        var serviceInfo = config.serviceInfo();
        serviceEntity.setServiceName(config.serviceName());
        serviceEntity.setLabel(serviceInfo.getLabel());
        serviceEntity.setFrameId(config.frameInfo().getId());
        serviceEntity.setServiceDesc(serviceInfo.getDescription());
        serviceEntity.setServiceVersion(serviceInfo.getVersion());
        serviceEntity.setPackageName(serviceInfo.getPackageName());
        serviceEntity.setDependencies(StringUtils.join(serviceInfo.getDependencies(), ","));
        serviceEntity.setFrameCode(config.frameCode());
        serviceEntity.setServiceConfig(JSON.toJSONString(serviceInfo.getParameters()));
        serviceEntity.setServiceJson(config.serviceDdl());
        serviceEntity.setServiceJsonMd5(config.serviceInfoMd5());
        serviceEntity.setDecompressPackageName(config.decompressPackageName());
        serviceEntity.setConfigFileJson(JSONObject.toJSONString(config.configFileMap()));
        serviceEntity.setConfigFileJsonMd5(SecureUtil.md5(serviceEntity.getConfigFileJson()));
        serviceEntity.setSortNum(serviceInfo.getSortNum());
    }
}
