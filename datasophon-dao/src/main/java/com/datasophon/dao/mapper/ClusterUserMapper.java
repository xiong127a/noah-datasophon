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

import com.datasophon.dao.entity.ClusterUser;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群用户映射器
 * 迁移SQL逻辑到DAO层
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterUserMapper extends BaseMapper<ClusterUser> {

    /**
     * 根据集群ID和用户名查询用户列表（检查重复用户名）
     *
     * @param clusterId 集群ID
     * @param username  用户名
     * @return 用户列表
     */
    default List<ClusterUser> selectByClusterIdAndUsername(@Param("clusterId") Integer clusterId,
            @Param("username") String username) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterUser::getClusterId).eq(clusterId)
                .and(ClusterUser::getUsername).eq(username);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID分页查询用户列表，支持用户名模糊搜索
     *
     * @param clusterId 集群ID
     * @param username  用户名（可为null，支持模糊搜索）
     * @param offset    偏移量
     * @param pageSize  每页大小
     * @return 用户列表
     */
    default List<ClusterUser> selectByClusterIdWithPagination(@Param("clusterId") Integer clusterId,
            @Param("username") String username,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterUser::getClusterId).eq(clusterId);

        if (username != null && !username.trim().isEmpty()) {
            query.and(ClusterUser::getUsername).like("%" + username + "%");
        }

        return this.selectListByQuery(query.limit(offset, pageSize));
    }

    /**
     * 根据集群ID查询所有用户列表
     *
     * @param clusterId 集群ID
     * @return 用户列表
     */
    default List<ClusterUser> selectByClusterId(@Param("clusterId") Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterUser::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID和用户名统计用户总数（分页查询总数）
     *
     * @param clusterId 集群ID
     * @param username  用户名（可为null）
     * @return 用户总数
     */
    default long countByClusterIdAndUsername(@Param("clusterId") Integer clusterId,
            @Param("username") String username) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterUser::getClusterId).eq(clusterId);

        if (username != null && !username.trim().isEmpty()) {
            query.and(ClusterUser::getUsername).like("%" + username + "%");
        }

        return this.selectCountByQuery(query);
    }

    /**
     * 根据用户ID列表获取用户名列表
     *
     * @param userIds 用户ID列表
     * @return 用户名列表
     */
    default List<String> selectUsernamesByIds(@Param("userIds") List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterUser::getId).in(userIds);
        return this.selectListByQuery(query)
                .stream()
                .map(ClusterUser::getUsername)
                .toList(); // JDK21现代特性
    }
}
