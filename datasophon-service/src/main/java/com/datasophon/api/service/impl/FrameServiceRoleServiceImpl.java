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

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.converter.FrameServiceRoleConverter;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.dto.FrameServiceDTO;
import com.datasophon.common.dto.FrameServiceRoleDTO;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.datasophon.dao.mapper.FrameServiceRoleMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 框架服务角色表服务实现
 * 继承ServiceImpl提供基础CRUD操作，使用Converter进行对象转换
 * 保持复杂业务逻辑完整性，按照架构重构规范返回DTO对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Slf4j
@Service("frameServiceRoleService")
@RequiredArgsConstructor
public class FrameServiceRoleServiceImpl extends ServiceImpl<FrameServiceRoleMapper, FrameServiceRoleEntity>
        implements FrameServiceRoleService {

    // 定义常量
    private static final String SERVICE_NODE = "NODE";
    private static final String ROLE_NODE = "node";
    private static final String SERVICE_ROLE_CACHE_KEY_FORMAT = "%d_%s";

    // 依赖注入 - 使用构造器注入
    private final FrameServiceRoleConverter frameServiceRoleConverter;
    private final ClusterServiceRoleInstanceMapper clusterServiceRoleInstanceMapper;
    private final FrameServiceService frameServiceService;

    @Override
    public List<FrameServiceRoleDTO> getServiceRoleList(Long clusterId, List<Long> serviceIds,
            Integer serviceRoleType) {
        validateCommonParams(clusterId, serviceIds);

        // 调用Dao层方法查询服务角色
        List<FrameServiceRoleEntity> roles = getMapper().selectByServiceIdsAndRoleType(serviceIds, serviceRoleType);

        return processServiceRoles(clusterId, serviceIds, roles);
    }

    /**
     * 通用参数校验方法
     */
    private void validateCommonParams(Long clusterId, List<Long> serviceIds) {
        if (clusterId == null) {
            throw new RuntimeException("集群ID不能为空");
        }
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new RuntimeException("服务ID列表不能为空");
        }
    }

    /**
     * 通用服务角色处理方法 - 批量查询serviceName并转换为DTO
     * 
     * @param clusterId 集群ID
     * @param serviceIds 服务ID列表
     * @param roles 查询到的服务角色实体列表
     * @return 转换后的DTO列表
     */
    private List<FrameServiceRoleDTO> processServiceRoles(Long clusterId, List<Long> serviceIds, 
                                                          List<FrameServiceRoleEntity> roles) {
        if (roles.isEmpty()) {
            return List.of();
        }
        
        // 🚀 批量查询服务名称 - 只查1次，提高效率
        Map<Long, String> serviceIdToNameMap = frameServiceService.getServiceListByServiceIds(serviceIds)
                .stream()
                .collect(Collectors.toMap(
                        FrameServiceDTO::id,
                        FrameServiceDTO::serviceName,
                        (existing, replacement) -> existing // 处理重复key
                ));

        // 转换为DTO并填充serviceName和主机信息 - 使用优化的MapStruct映射
        return roles.stream()
                .map(role -> {
                    // 从批量查询结果中获取serviceName
                    String serviceName = serviceIdToNameMap.get(role.getServiceId());
                    FrameServiceRoleDTO dto = serviceName != null 
                        ? frameServiceRoleConverter.entityToDtoWithServiceName(role, serviceName)
                        : frameServiceRoleConverter.entityToDto(role);
                    
                    // 查询并设置主机信息
                    List<String> hosts = getHostsForRole(clusterId, dto.serviceRoleName());
                    return dto.withHosts(hosts);
                })
                .toList();
    }

    /**
     * 辅助方法：获取角色的主机列表（保持复杂业务逻辑完整性）
     */
    private List<String> getHostsForRole(Long clusterId, String serviceRoleName) {
        try {
            // 查询已安装的角色实例
            List<ClusterServiceRoleInstanceEntity> roleInstances = clusterServiceRoleInstanceMapper
                    .selectByClusterIdAndServiceNameAndServiceRoleName(clusterId, "", serviceRoleName);

            // 如果有角色实例，从实例中获取主机列表
            if (CollUtil.isNotEmpty(roleInstances)) {
                return roleInstances.stream()
                        .map(ClusterServiceRoleInstanceEntity::getHostname)
                        .toList();
            }

            // 否则，尝试从缓存中获取
            String cacheKey = String.format(SERVICE_ROLE_CACHE_KEY_FORMAT, clusterId,
                    Constants.SERVICE_ROLE_HOST_MAPPING);
            if (CacheOperateUtils.containsKey(cacheKey)) {
                Map<String, List<String>> roleToHostsMap = CacheOperateUtils.getGeneric(
                        cacheKey, TypeRefs.MAP_STRING_LIST_STRING);

                if (roleToHostsMap.containsKey(serviceRoleName)) {
                    return roleToHostsMap.get(serviceRoleName);
                }
            }

            return List.of();
        } catch (Exception e) {
            log.warn("获取角色 {} 的主机信息失败: {}", serviceRoleName, e.getMessage());
            return List.of();
        }
    }

    @Override
    public FrameServiceRoleDTO getServiceRoleByServiceIdAndServiceRoleName(Long serviceId, String roleName) {
        if (serviceId == null) {
            throw new RuntimeException("服务ID不能为空");
        }
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new RuntimeException("角色名称不能为空");
        }

        FrameServiceRoleEntity entity = getMapper().selectByServiceIdAndRoleName(serviceId, roleName);
        if (entity == null) {
            throw new RuntimeException("未找到服务ID为 " + serviceId + " 且角色名称为 " + roleName + " 的服务角色");
        }

        return frameServiceRoleConverter.entityToDto(entity);
    }

    @Override
    public java.util.Optional<FrameServiceRoleDTO> findServiceRoleByServiceIdAndServiceRoleName(Long serviceId, String roleName) {
        // JDK 21现代化参数验证
        if (serviceId == null) {
            log.warn("服务角色查找参数验证失败: 服务ID不能为空");
            return java.util.Optional.empty();
        }
        
        if (roleName == null || roleName.trim().isEmpty()) {
            log.warn("服务角色查找参数验证失败: 角色名称不能为空");
            return java.util.Optional.empty();
        }

        var entity = getMapper().selectByServiceIdAndRoleName(serviceId, roleName);
        return java.util.Optional.ofNullable(entity)
                .map(frameServiceRoleConverter::entityToDto);
    }

    @Override
    public FrameServiceRoleDTO getServiceRoleByFrameCodeAndServiceRoleName(String clusterFrame,
            String serviceRoleName) {
        if (clusterFrame == null || clusterFrame.trim().isEmpty()) {
            throw new RuntimeException("框架代码不能为空");
        }
        if (serviceRoleName == null || serviceRoleName.trim().isEmpty()) {
            throw new RuntimeException("服务角色名称不能为空");
        }

        FrameServiceRoleEntity entity = getMapper().selectByFrameCodeAndRoleName(clusterFrame, serviceRoleName);
        if (entity == null) {
            throw new RuntimeException("未找到框架代码为 " + clusterFrame + " 且角色名称为 " + serviceRoleName + " 的服务角色");
        }

        return frameServiceRoleConverter.entityToDto(entity);
    }





    @Override
    public List<FrameServiceRoleDTO> getNonMasterRoleList(Long clusterId, List<Long> serviceIds) {
        validateCommonParams(clusterId, serviceIds);

        // 调用Dao层方法查询非MASTER角色
        List<FrameServiceRoleEntity> roles = getMapper().selectNonMasterRoles(serviceIds);

        return processServiceRoles(clusterId, serviceIds, roles);
    }

    @Override
    public List<FrameServiceRoleDTO> getServiceRoleByServiceName(Long clusterId, String serviceName) {
        if (clusterId == null) {
            throw new RuntimeException("集群ID不能为空");
        }
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new RuntimeException("服务名称不能为空");
        }

        // 特殊处理NODE服务
        if (SERVICE_NODE.equals(serviceName)) {
            FrameServiceRoleDTO nodeRole = FrameServiceRoleDTO.of(null, null, SERVICE_NODE, ROLE_NODE, 2, "1+", null);
            return Collections.singletonList(nodeRole);
        }

        // 由于集群信息获取逻辑需要重新设计，暂时返回空列表
        // 后续需要根据实际业务需求调整
        log.debug("getServiceRoleByServiceName: 集群ID={}, 服务名称={}", clusterId, serviceName);
        return List.of();
    }

    @Override
    public List<FrameServiceRoleDTO> getAllServiceRoleList(Long frameServiceId) {
        if (frameServiceId == null) {
            throw new RuntimeException("框架服务ID不能为空");
        }

        List<FrameServiceRoleEntity> entities = getMapper().selectByServiceId(frameServiceId);
        return frameServiceRoleConverter.entityListToDtoList(entities);
    }

    // 基础CRUD方法实现

    @Override
    public FrameServiceRoleDTO getFrameServiceRoleById(Long id) {
        if (id == null) {
            throw new RuntimeException("服务角色ID不能为空");
        }

        FrameServiceRoleEntity entity = getById(id);
        if (entity == null) {
            throw new RuntimeException("未找到ID为 " + id + " 的服务角色");
        }

        return frameServiceRoleConverter.entityToDto(entity);
    }

    @Override
    public FrameServiceRoleDTO saveFrameServiceRole(FrameServiceRoleDTO frameServiceRoleDTO) {
        if (frameServiceRoleDTO == null) {
            throw new RuntimeException("服务角色信息不能为空");
        }

        FrameServiceRoleEntity entity = frameServiceRoleConverter.dtoToEntity(frameServiceRoleDTO);
        boolean result = save(entity);

        if (!result) {
            throw new RuntimeException("保存服务角色失败");
        }

        return frameServiceRoleConverter.entityToDto(entity);
    }

    @Override
    public FrameServiceRoleDTO updateFrameServiceRole(FrameServiceRoleDTO frameServiceRoleDTO) {
        if (frameServiceRoleDTO == null || frameServiceRoleDTO.id() == null) {
            throw new RuntimeException("服务角色信息或ID不能为空");
        }

        // 检查记录是否存在
        FrameServiceRoleEntity existingEntity = getById(frameServiceRoleDTO.id());
        if (existingEntity == null) {
            throw new RuntimeException("未找到ID为 " + frameServiceRoleDTO.id() + " 的服务角色");
        }

        FrameServiceRoleEntity entity = frameServiceRoleConverter.dtoToEntity(frameServiceRoleDTO);
        boolean result = updateById(entity);

        if (!result) {
            throw new RuntimeException("更新服务角色失败");
        }

        return frameServiceRoleConverter.entityToDto(entity);
    }

    @Override
    public boolean removeFrameServiceRoleByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("删除的ID列表不能为空");
        }

        return removeByIds(ids);
    }

    @Override
    public boolean removeByServiceId(Long serviceId) {
        if (serviceId == null) {
            return false;
        }

        return getMapper().removeByServiceId(serviceId);
    }
}
