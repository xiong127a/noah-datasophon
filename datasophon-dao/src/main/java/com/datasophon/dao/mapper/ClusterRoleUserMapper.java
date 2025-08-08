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

import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import static com.datasophon.dao.entity.table.ClusterRoleUserEntityTableDef.CLUSTER_ROLE_USER_ENTITY;
import static com.datasophon.dao.entity.table.UserInfoEntityTableDef.USER_INFO_ENTITY;

/**
 * 集群角色用户映射器
 * 只保留业务特定的查询方法，标准CRUD使用IService提供
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface ClusterRoleUserMapper extends BaseMapper<ClusterRoleUserEntity> {

    /**
     * 获取指定集群的所有管理员
     * 使用MyBatis-Flex的QueryChain实现优雅的SQL构建
     *
     * @param clusterId 集群ID
     * @return 管理员用户列表
     */
    default List<UserInfoEntity> getAllClusterManagerByClusterId(@Param("clusterId") Integer clusterId) {
        // 使用表定义常量和类型安全的QueryChain构建优雅的SQL
        return QueryChain.of(ClusterRoleUserEntity.class)
                .select(USER_INFO_ENTITY.ALL_COLUMNS)
                .from(CLUSTER_ROLE_USER_ENTITY)
                .leftJoin(USER_INFO_ENTITY).on(USER_INFO_ENTITY.ID.eq(CLUSTER_ROLE_USER_ENTITY.USER_ID))
                .where(CLUSTER_ROLE_USER_ENTITY.CLUSTER_ID.eq(clusterId))
                .listAs(UserInfoEntity.class);
    }

    /**
     * 根据用户ID和集群ID查询角色用户关系
     */
    default List<ClusterRoleUserEntity> selectByUserIdAndClusterId(Integer userId, Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterRoleUserEntity::getUserId).eq(userId)
                .and(ClusterRoleUserEntity::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID删除角色用户关系
     */
    default int removeByClusterId(Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterRoleUserEntity::getClusterId).eq(clusterId);
        return this.deleteByQuery(query);
    }

    /**
     * 批量保存角色用户关系
     */
    default int saveBatch(List<ClusterRoleUserEntity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ClusterRoleUserEntity entity : entityList) {
            count += this.insertSelective(entity);
        }
        return count;
    }

}
