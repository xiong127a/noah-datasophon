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

package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.RoleInfoEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.datasophon.dao.entity.table.RoleInfoEntityTableDef.ROLE_INFO_ENTITY;

/**
 * 角色信息Mapper
 * 按照架构重构规范，复杂SQL逻辑在DAO层处理
 * 使用MyBatis-Flex QueryChain Lambda写法
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Mapper
public interface RoleInfoMapper extends BaseMapper<RoleInfoEntity> {
    
    /**
     * 分页查询角色列表
     * 支持角色名称模糊查询
     */
    default Page<RoleInfoEntity> selectRolePageByName(String roleName, Integer page, Integer pageSize) {
        var queryChain = QueryChain.of(RoleInfoEntity.class)
                .from(ROLE_INFO_ENTITY);
        
        // 条件查询
        if (StringUtils.hasText(roleName)) {
            queryChain.where(ROLE_INFO_ENTITY.ROLE_NAME.like("%" + roleName + "%"));
        }
        
        // 分页查询
        return queryChain.orderBy(ROLE_INFO_ENTITY.CREATE_TIME.desc())
                .page(Page.of(page, pageSize));
    }
    
    /**
     * 根据角色编码查询角色
     */
    default RoleInfoEntity selectByRoleCode(String roleCode) {
        return QueryChain.of(RoleInfoEntity.class)
                .where(ROLE_INFO_ENTITY.ROLE_CODE.eq(roleCode))
                .one();
    }
    
    /**
     * 获取所有角色列表（按创建时间降序）
     */
    default List<RoleInfoEntity> selectAllRolesOrderByCreateTime() {
        return QueryChain.of(RoleInfoEntity.class)
                .orderBy(ROLE_INFO_ENTITY.CREATE_TIME.desc())
                .list();
    }
    
    /**
     * 检查角色编码是否存在
     * 
     * @param roleCode  角色编码
     * @param excludeId 排除的角色ID（编辑时排除当前角色）
     * @return true表示存在，false表示不存在
     */
    default boolean existsByRoleCode(String roleCode, Integer excludeId) {
        var queryChain = QueryChain.of(RoleInfoEntity.class)
                .where(ROLE_INFO_ENTITY.ROLE_CODE.eq(roleCode));
        
        // 排除指定ID
        if (excludeId != null) {
            queryChain.and(ROLE_INFO_ENTITY.ID.ne(excludeId));
        }
        
        return queryChain.exists();
    }
}
