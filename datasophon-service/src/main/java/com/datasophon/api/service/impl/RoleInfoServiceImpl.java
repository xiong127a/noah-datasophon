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

import com.datasophon.api.converter.RoleInfoConverter;
import com.datasophon.api.service.RoleInfoService;
import com.datasophon.common.dto.RoleInfoDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.RoleInfoEntity;
import com.datasophon.dao.mapper.RoleInfoMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色信息服务实现类
 * 继承ServiceImpl<RoleInfoMapper, RoleInfoEntity>，获得标准CRUD能力
 * 使用JDK21现代特性和MyBatis-Flex QueryChain
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@Service("roleInfoService")
public class RoleInfoServiceImpl extends ServiceImpl<RoleInfoMapper, RoleInfoEntity> implements RoleInfoService {
    
    @Autowired
    private RoleInfoConverter roleInfoConverter;
    
    @Override
    public RoleInfoDTO createRole(RoleInfoDTO roleInfoDTO) {
        log.debug("创建角色: {}", roleInfoDTO.roleName());
        
        // 检查角色编码是否已存在
        if (checkRoleCodeExists(roleInfoDTO.roleCode(), null)) {
            throw new com.datasophon.common.exception.BusinessException("角色编码已存在: " + roleInfoDTO.roleCode());
        }
        
        // DTO转Entity
        var roleEntity = roleInfoConverter.dtoToEntity(roleInfoDTO);
        
        // 保存到数据库
        save(roleEntity);
        
        // Entity转DTO返回
        return roleInfoConverter.entityToDto(roleEntity);
    }
    
    @Override
    public RoleInfoDTO updateRole(RoleInfoDTO roleInfoDTO) {
        log.debug("更新角色: {}", roleInfoDTO.id());
        
        // 检查角色是否存在
        var existingEntity = getById(roleInfoDTO.id());
        if (existingEntity == null) {
            throw new com.datasophon.common.exception.BusinessException("角色不存在: " + roleInfoDTO.id());
        }
        
        // 检查角色编码是否已存在（排除当前角色）
        if (checkRoleCodeExists(roleInfoDTO.roleCode(), roleInfoDTO.id())) {
            throw new com.datasophon.common.exception.BusinessException("角色编码已存在: " + roleInfoDTO.roleCode());
        }
        
        // DTO转Entity
        var roleEntity = roleInfoConverter.dtoToEntity(roleInfoDTO);
        
        // 更新数据库
        updateById(roleEntity);
        
        // Entity转DTO返回
        return roleInfoConverter.entityToDto(roleEntity);
    }
    
    @Override
    public RoleInfoDTO getRoleById(Long id) {
        log.debug("根据ID获取角色: {}", id);
        
        var roleEntity = getById(id);
        if (roleEntity == null) {
            throw new com.datasophon.common.exception.BusinessException("角色不存在: " + id);
        }
        
        return roleInfoConverter.entityToDto(roleEntity);
    }
    
    @Override
    public PageResult<RoleInfoDTO> getRoleListByPage(String roleName, Integer page, Integer pageSize) {
        log.debug("分页查询角色列表: roleName={}, page={}, pageSize={}", roleName, page, pageSize);
        
        // 调用DAO层方法，SQL逻辑在Mapper中处理
        var pageResult = getMapper().selectRolePageByName(roleName, page, pageSize);
        
        // Entity列表转DTO列表 - 仅做业务逻辑处理
        var dtoList = pageResult.getRecords().stream()
                .map(roleInfoConverter::entityToDto)
                .toList(); // JDK21特性
        
        return PageResult.of(dtoList, pageResult.getTotalRow(), page, pageSize);
    }
    
    @Override
    public RoleInfoDTO getRoleByCode(String roleCode) {
        log.debug("根据角色编码获取角色: {}", roleCode);
        
        // 调用DAO层方法，SQL逻辑在Mapper中处理
        var roleEntity = getMapper().selectByRoleCode(roleCode);
        
        return roleEntity != null ? roleInfoConverter.entityToDto(roleEntity) : null;
    }
    
    @Override
    public List<RoleInfoDTO> getAllRoles() {
        log.debug("获取所有角色列表");
        
        // 调用DAO层方法，SQL逻辑在Mapper中处理
        var roleEntities = getMapper().selectAllRolesOrderByCreateTime();
        
        return roleEntities.stream()
                .map(roleInfoConverter::entityToDto)
                .toList(); // JDK21特性
    }
    
    @Override
    public boolean checkRoleCodeExists(String roleCode, Long excludeId) {
        log.debug("检查角色编码是否存在: roleCode={}, excludeId={}", roleCode, excludeId);
        
        // 调用DAO层方法，SQL逻辑在Mapper中处理
        return getMapper().existsByRoleCode(roleCode, excludeId);
    }
    
    @Override
    public void deleteRole(Long id) {
        log.debug("删除角色: {}", id);
        
        // 检查角色是否存在
        var existingEntity = getById(id);
        if (existingEntity == null) {
            throw new com.datasophon.common.exception.BusinessException("角色不存在: " + id);
        }
        
        // TODO: 检查角色是否被用户使用中，如果被使用则不允许删除
        
        // 删除角色
        removeById(id);
        
        log.info("角色删除成功: {}", existingEntity.getRoleName());
    }
}
