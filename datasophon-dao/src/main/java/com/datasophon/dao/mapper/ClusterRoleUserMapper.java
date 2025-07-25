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
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import static com.datasophon.dao.entity.table.ClusterRoleUserEntityTableDef.CLUSTER_ROLE_USER_ENTITY;
import static com.datasophon.dao.entity.table.UserInfoEntityTableDef.USER_INFO_ENTITY;

/**
 * 集群角色用户中间表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
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
}
