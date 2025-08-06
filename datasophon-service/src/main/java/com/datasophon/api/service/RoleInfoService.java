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

package com.datasophon.api.service;

import com.datasophon.common.dto.RoleInfoDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.RoleInfoEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 角色信息服务接口
 * 继承IService<RoleInfoEntity>，提供标准CRUD操作
 * 按照架构重构规范，Service层返回DTO，不返回Result
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
public interface RoleInfoService extends IService<RoleInfoEntity> {
    
    /**
     * 创建角色
     * 
     * @param roleInfoDTO 角色信息DTO
     * @return 创建的角色信息DTO
     * @throws com.datasophon.common.exception.BusinessException 角色编码已存在等业务异常
     */
    RoleInfoDTO createRole(RoleInfoDTO roleInfoDTO);
    
    /**
     * 更新角色信息
     * 
     * @param roleInfoDTO 角色信息DTO
     * @return 更新后的角色信息DTO
     * @throws com.datasophon.common.exception.BusinessException 角色不存在等业务异常
     */
    RoleInfoDTO updateRole(RoleInfoDTO roleInfoDTO);
    
    /**
     * 根据ID获取角色信息
     * 
     * @param id 角色ID
     * @return 角色信息DTO
     * @throws com.datasophon.common.exception.BusinessException 角色不存在
     */
    RoleInfoDTO getRoleById(Integer id);
    
    /**
     * 分页查询角色列表
     * 
     * @param roleName 角色名称（模糊查询）
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<RoleInfoDTO> getRoleListByPage(String roleName, Integer page, Integer pageSize);
    
    /**
     * 根据角色编码查询角色
     * 
     * @param roleCode 角色编码
     * @return 角色信息DTO
     */
    RoleInfoDTO getRoleByCode(String roleCode);
    
    /**
     * 获取所有角色列表
     * 
     * @return 角色列表
     */
    List<RoleInfoDTO> getAllRoles();
    
    /**
     * 检查角色编码是否存在
     * 
     * @param roleCode  角色编码
     * @param excludeId 排除的角色ID（编辑时排除当前角色）
     * @return true表示角色编码已存在，false表示可用
     */
    boolean checkRoleCodeExists(String roleCode, Integer excludeId);
    
    /**
     * 删除角色
     * 
     * @param id 角色ID
     * @throws com.datasophon.common.exception.BusinessException 角色不存在或被使用中
     */
    void deleteRole(Integer id);
}
