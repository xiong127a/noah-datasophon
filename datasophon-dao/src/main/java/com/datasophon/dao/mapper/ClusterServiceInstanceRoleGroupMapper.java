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
 * 只保留业务特定的查询方法，标准CRUD使用IService提供
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
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
     * 根据角色组类型和服务实例ID统计数量
     * 用于ServiceInstallService中生成新角色组名称
     * 
     * @param roleGroupType 角色组类型
     * @param serviceInstanceId 服务实例ID
     * @return 数量
     */
    default long countByRoleGroupTypeAndServiceInstanceId(
            @Param("roleGroupType") String roleGroupType,
            @Param("serviceInstanceId") Integer serviceInstanceId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceInstanceRoleGroup::getRoleGroupType).eq(roleGroupType)
                .and(ClusterServiceInstanceRoleGroup::getServiceInstanceId).eq(serviceInstanceId);

        return this.selectCountByQuery(query);
    }

    // 基础CRUD方法已由BaseMapper提供，此处只保留业务特定查询方法

}
