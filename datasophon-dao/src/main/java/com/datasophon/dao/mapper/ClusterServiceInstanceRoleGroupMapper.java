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

import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;

/**
 * 集群服务实例角色组映射器
 *
 */
@Mapper
public interface ClusterServiceInstanceRoleGroupMapper extends BaseMapper<ClusterServiceInstanceRoleGroup> {

    /**
     * 根据服务实例ID和角色组类型查询角色组
     * 
     * @param serviceInstanceId 服务实例ID
     * @param roleGroupType     角色组类型
     * @return 角色组实体
     */
    default ClusterServiceInstanceRoleGroup selectByServiceInstanceIdAndRoleGroupType(
            @Param("serviceInstanceId") Integer serviceInstanceId,
            @Param("roleGroupType") String roleGroupType) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceInstanceRoleGroup::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterServiceInstanceRoleGroup::getRoleGroupType).eq(roleGroupType);

        return this.selectOneByQuery(query);
    }

    /**
     * 根据服务实例ID和角色组名称统计数量
     * 
     * @param serviceInstanceId 服务实例ID
     * @param roleGroupName     角色组名称
     * @return 数量
     */
    default long countByServiceInstanceIdAndRoleGroupName(@Param("serviceInstanceId") Integer serviceInstanceId,
            @Param("roleGroupName") String roleGroupName) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceInstanceRoleGroup::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterServiceInstanceRoleGroup::getRoleGroupName).eq(roleGroupName);

        return this.selectCountByQuery(query);
    }

    /**
     * 根据服务实例ID查询所有角色组
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 角色组列表
     */
    default List<ClusterServiceInstanceRoleGroup> selectByServiceInstanceId(
            @Param("serviceInstanceId") Integer serviceInstanceId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceInstanceRoleGroup::getServiceInstanceId).eq(serviceInstanceId);

        return this.selectListByQuery(query);
    }

    /**
     * 根据ID查询角色组
     * 
     * @param id 主键ID
     * @return 角色组实体
     */
    default ClusterServiceInstanceRoleGroup selectById(@Param("id") Integer id) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceInstanceRoleGroup::getId).eq(id);

        return this.selectOneByQuery(query);
    }

    /**
     * 插入角色组
     * 
     * @param entity 角色组实体
     * @return 影响行数
     */
    default int insertEntity(ClusterServiceInstanceRoleGroup entity) {
        return this.insertSelective(entity);
    }

    /**
     * 根据ID更新角色组
     * 
     * @param entity 角色组实体
     * @return 影响行数
     */
    default int updateByIdEntity(ClusterServiceInstanceRoleGroup entity) {
        return this.updateByQuery(entity, QueryWrapper.create()
                .where(ClusterServiceInstanceRoleGroup::getId).eq(entity.getId()));
    }

    /**
     * 根据ID列表删除角色组
     * 
     * @param ids ID列表
     * @return 影响行数
     */
    default int deleteByIds(@Param("ids") List<Integer> ids) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceInstanceRoleGroup::getId).in(ids);

        return this.deleteByQuery(query);
    }

}
