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
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.converter.FrameServiceConverter;
import com.datasophon.api.converter.FrameServiceRoleConverter;
import com.datasophon.api.load.model.BatchServiceData;
import com.datasophon.api.load.model.ServiceMetaConfig;
import com.datasophon.dao.model.ServiceRoleQueryCondition;
import com.datasophon.api.service.BatchServiceMetadataTransactionService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.api.utils.CommonUtils;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datasophon.common.Constants.GENERAL;

/**
 * 批量服务元数据事务处理服务实现
 * 核心优化：将136个服务的1088次SQL操作优化为6-8次批量操作
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com  
 * 日期：2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchServiceMetadataTransactionServiceImpl implements BatchServiceMetadataTransactionService {
    
    private final FrameServiceService frameServiceService;
    private final FrameServiceRoleService frameServiceRoleService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchProcessResult batchProcessFrameServices(String frameCode, FrameInfoEntity frameInfo, 
                                                       List<ServiceMetaConfig> configs) {
        
        long startTime = System.currentTimeMillis();
        log.info("开始批量处理框架 {} 的 {} 个服务", frameCode, configs.size());
        
        try {
            // 1. 批量查询现有数据 (2次SQL)
            var batchData = batchQueryExistingData(frameInfo.getId(), configs);
            log.debug("批量查询完成: 现有服务 {}, 现有角色 {}", 
                    batchData.existingServices().size(), 
                    batchData.existingRoles().size());
            
            // 2. 批量分析差异并准备服务数据
            prepareBatchServiceData(configs, frameInfo, batchData);
            log.debug("服务数据准备完成: 插入 {}, 更新 {}", 
                    batchData.servicesToInsert().size(),
                    batchData.servicesToUpdate().size());
            
            // 3. 先执行服务的批量操作 (最多2次SQL)
            executeBatchServiceOperations(batchData);
            
            // 4. 准备角色数据 (包括新插入服务的角色)
            prepareBatchRoleData(configs, batchData);
            log.debug("角色数据准备完成: 插入 {}, 更新 {}", 
                    batchData.rolesToInsert().size(),
                    batchData.rolesToUpdate().size());
            
            // 5. 执行角色的批量操作 (最多2次SQL)
            executeBatchRoleOperations(batchData);
            
            // 6. 更新服务ID映射供后续使用
            updateServiceIdMappings(batchData);
            
            long processingTime = System.currentTimeMillis() - startTime;
            var result = BatchProcessResult.of(batchData, processingTime);
            
            log.info("框架 {} 批量处理完成: {}", frameCode, result.getSummary());
            return result;
            
        } catch (Exception e) {
            log.error("框架 {} 批量处理失败", frameCode, e);
            throw new RuntimeException("批量处理框架服务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量查询现有服务和角色数据
     * 优化：2次SQL查询替代数百次单独查询
     */
    private BatchServiceData batchQueryExistingData(Long frameId, List<ServiceMetaConfig> configs) {
        // 收集所有服务名称
        var serviceNames = configs.stream()
                .map(ServiceMetaConfig::serviceName)
                .collect(Collectors.toList());
        
        // 1. 批量查询现有服务 (1次SQL)
        var existingServices = frameServiceService.findServicesByFrameIdAndNames(frameId, serviceNames)
                .stream()
                .collect(Collectors.toMap(
                        service -> service.serviceName(),
                        service -> {
                            var converter = SpringUtil.getBean(FrameServiceConverter.class);
                            return converter.dtoToEntity(service);
                        },
                        // 处理重复键：保留第一个，忽略后续重复的
                        (existing, duplicate) -> {
                            log.debug("发现重复服务记录: {}，保留现有记录", existing.getServiceName());
                            return existing;
                        }
                ));
        
        // 收集所有角色查询条件（自动去重）
        var roleQueryConditions = new ArrayList<ServiceRoleQueryCondition>();
        var addedConditions = new java.util.HashSet<String>();
        
        for (var config : configs) {
            var serviceName = config.serviceName();
            var existingService = existingServices.get(serviceName);
            if (existingService != null) {
                for (var roleInfo : config.serviceRoles()) {
                    var conditionKey = existingService.getId() + "_" + roleInfo.getName();
                    // 避免重复查询条件
                    if (addedConditions.add(conditionKey)) {
                        roleQueryConditions.add(ServiceRoleQueryCondition.of(
                                existingService.getId(), roleInfo.getName()));
                    }
                }
            }
        }
        
        log.debug("构建了 {} 个唯一的角色查询条件", roleQueryConditions.size());
        
        // 2. 批量查询现有角色 (1次SQL)
        var existingRoles = roleQueryConditions.isEmpty() ? 
                Map.<String, FrameServiceRoleEntity>of() :
                frameServiceRoleService.findServiceRolesByConditions(roleQueryConditions)
                        .stream()
                        .collect(Collectors.toMap(
                                role -> role.serviceId() + "_" + role.serviceRoleName(),
                                role -> {
                                    var converter = SpringUtil.getBean(FrameServiceRoleConverter.class);
                                    return converter.dtoToEntity(role);
                                },
                                // 处理重复键：保留第一个，忽略后续重复的
                                (existing, duplicate) -> {
                                    log.debug("发现重复角色记录: {}_{}，保留现有记录", 
                                            existing.getServiceId(), existing.getServiceRoleName());
                                    return existing;
                                }
                        ));
        
        return new BatchServiceData(
                new ArrayList<>(), new ArrayList<>(), 
                new ArrayList<>(), new ArrayList<>(),
                existingServices, existingRoles,
                new java.util.HashMap<>()
        );
    }
    
    /**
     * 准备服务的批量操作数据
     */
    private void prepareBatchServiceData(List<ServiceMetaConfig> configs, FrameInfoEntity frameInfo, 
                                       BatchServiceData batchData) {
        
        for (var config : configs) {
            // 处理服务数据
            var existingService = batchData.existingServices().get(config.serviceName());
            var serviceEntity = prepareServiceEntity(config, frameInfo, existingService);
            
            if (existingService == null) {
                batchData.servicesToInsert().add(serviceEntity);
            } else if (serviceNeedsUpdate(existingService, config)) {
                serviceEntity.setId(existingService.getId());
                batchData.servicesToUpdate().add(serviceEntity);
            }
        }
    }
    
    /**
     * 执行服务的批量数据库操作
     */
    private void executeBatchServiceOperations(BatchServiceData batchData) {
        int operationCount = 0;
        
        // 1. 批量插入服务 (1次SQL)
        if (!batchData.servicesToInsert().isEmpty()) {
            frameServiceService.saveBatch(batchData.servicesToInsert());
            log.info("批量插入 {} 个服务", batchData.servicesToInsert().size());
            operationCount++;
        }
        
        // 2. 批量更新服务 (1次SQL) 
        if (!batchData.servicesToUpdate().isEmpty()) {
            frameServiceService.saveOrUpdateBatch(batchData.servicesToUpdate());
            log.info("批量更新 {} 个服务", batchData.servicesToUpdate().size());
            operationCount++;
        }
        
        log.info("服务批量操作完成，共执行 {} 次SQL", operationCount);
    }
    
    /**
     * 准备角色的批量操作数据 (包括新插入服务的角色)
     */
    private void prepareBatchRoleData(List<ServiceMetaConfig> configs, BatchServiceData batchData) {
        
        for (var config : configs) {
            var serviceName = config.serviceName();
            
            // 首先尝试从已存在服务中找
            var existingService = batchData.existingServices().get(serviceName);
            if (existingService != null) {
                prepareRoleEntities(config, existingService, batchData);
                continue;
            }
            
            // 然后从新插入的服务中找
            var newService = batchData.servicesToInsert().stream()
                    .filter(service -> serviceName.equals(service.getServiceName()))
                    .findFirst()
                    .orElse(null);
                    
            if (newService != null && newService.getId() != null) {
                prepareRoleEntities(config, newService, batchData);
            } else {
                log.warn("无法为服务 {} 准备角色数据：服务ID为空", serviceName);
            }
        }
    }
    
    /**
     * 执行角色的批量数据库操作
     */
    private void executeBatchRoleOperations(BatchServiceData batchData) {
        int operationCount = 0;
        
        // 1. 批量插入角色 (1次SQL)
        if (!batchData.rolesToInsert().isEmpty()) {
            frameServiceRoleService.saveBatch(batchData.rolesToInsert());
            log.info("批量插入 {} 个角色", batchData.rolesToInsert().size());
            operationCount++;
        }
        
        // 2. 批量更新角色 (1次SQL)
        if (!batchData.rolesToUpdate().isEmpty()) {
            frameServiceRoleService.saveOrUpdateBatch(batchData.rolesToUpdate());
            log.info("批量更新 {} 个角色", batchData.rolesToUpdate().size());
            operationCount++;
        }
        
        log.info("角色批量操作完成，共执行 {} 次SQL", operationCount);
    }
    
    /**
     * 准备服务实体
     */
    private FrameServiceEntity prepareServiceEntity(ServiceMetaConfig config, FrameInfoEntity frameInfo, 
                                                  FrameServiceEntity existingService) {
        var serviceEntity = existingService != null ? new FrameServiceEntity() : new FrameServiceEntity();
        
        // 处理参数配置
        var parameters = config.parameters();
        var nameToRoleMap = ConfigGroupUtils.buildNameToRoleMap(config.configFileMap(), config.frameCode());
        
        // 设置配置目标角色
        parameters.stream()
                .filter(serviceConfig -> ObjectUtils.isEmpty(serviceConfig.getConfigTargetRoles()))
                .forEach(serviceConfig -> {
                    var configTargetRoles = nameToRoleMap.getOrDefault(serviceConfig.getName(), GENERAL);
                    serviceConfig.setConfigTargetRoles(configTargetRoles);
                });
        
        // 构建服务实体
        buildServiceEntity(config, serviceEntity);
        
        return serviceEntity;
    }
    
    /**
     * 检查服务是否需要更新
     */
    private boolean serviceNeedsUpdate(FrameServiceEntity existingService, ServiceMetaConfig config) {
        return !existingService.getServiceJsonMd5().equals(config.serviceInfoMd5());
    }
    
    /**
     * 准备角色实体数据
     */
    private void prepareRoleEntities(ServiceMetaConfig config, FrameServiceEntity serviceEntity, 
                                   BatchServiceData batchData) {
        
        for (var roleInfo : config.serviceRoles()) {
            var roleKey = serviceEntity.getId() + "_" + roleInfo.getName();
            var existingRole = batchData.existingRoles().get(roleKey);
            
            var serviceRoleJson = JSONObject.toJSONString(roleInfo);
            var serviceRoleJsonMd5 = SecureUtil.md5(serviceRoleJson);
            
            if (existingRole == null) {
                var roleEntity = new FrameServiceRoleEntity();
                buildFrameServiceRole(config, serviceEntity, roleInfo, 
                        serviceRoleJson, serviceRoleJsonMd5, roleEntity);
                batchData.rolesToInsert().add(roleEntity);
            } else if (roleNeedsUpdate(existingRole, serviceRoleJsonMd5)) {
                var roleEntity = new FrameServiceRoleEntity();
                roleEntity.setId(existingRole.getId());
                buildFrameServiceRole(config, serviceEntity, roleInfo, 
                        serviceRoleJson, serviceRoleJsonMd5, roleEntity);
                batchData.rolesToUpdate().add(roleEntity);
            }
        }
    }
    
    /**
     * 检查角色是否需要更新
     */
    private boolean roleNeedsUpdate(FrameServiceRoleEntity existingRole, String newMd5) {
        return !existingRole.getServiceRoleJsonMd5().equals(newMd5);
    }
    
    /**
     * 更新服务ID映射
     */
    private void updateServiceIdMappings(BatchServiceData batchData) {
        // 为新插入的服务建立ID映射
        for (var service : batchData.servicesToInsert()) {
            batchData.serviceNameToIdMap().put(service.getServiceName(), service.getId());
        }
        
        // 为已存在的服务建立ID映射
        for (var entry : batchData.existingServices().entrySet()) {
            batchData.serviceNameToIdMap().put(entry.getKey(), entry.getValue().getId());
        }
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
}
