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

import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群服务角色组配置数据访问对象
 * 提供集群服务角色组配置的数据库操作
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterServiceRoleGroupConfigMapper extends BaseMapper<ClusterServiceRoleGroupConfigEntity> {

        /**
         * 根据角色组ID获取配置（最新版本）
         *
         * @param roleGroupId 角色组ID
         * @return 配置实体
         */
        default ClusterServiceRoleGroupConfigEntity selectByRoleGroupId(@Param("roleGroupId") Long roleGroupId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterServiceRoleGroupConfigEntity::getRoleGroupId).eq(roleGroupId)
                                .orderBy(ClusterServiceRoleGroupConfigEntity::getConfigVersion).desc()
                                .limit(1);
                List<ClusterServiceRoleGroupConfigEntity> results = this.selectListByQuery(query);
                return results.isEmpty() ? null : results.getFirst();
        }

        /**
         * 根据角色组ID和版本号获取配置
         *
         * @param roleGroupId 角色组ID
         * @param version     版本号
         * @return 配置实体
         */
        default ClusterServiceRoleGroupConfigEntity selectByRoleGroupIdAndVersion(
                        @Param("roleGroupId") Long roleGroupId,
                        @Param("version") Integer version) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterServiceRoleGroupConfigEntity::getRoleGroupId).eq(roleGroupId)
                                .and(ClusterServiceRoleGroupConfigEntity::getConfigVersion).eq(version);
                return this.selectOneByQuery(query);
        }

        /**
         * 删除指定角色组的所有配置
         *
         * @param roleGroupId 角色组ID
         * @return 删除的记录数
         */
        default int deleteByRoleGroupId(@Param("roleGroupId") Long roleGroupId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterServiceRoleGroupConfigEntity::getRoleGroupId).eq(roleGroupId);
                return this.deleteByQuery(query);
        }

        /**
         * 根据角色组ID列表获取配置列表
         *
         * @param roleGroupIds 角色组ID列表
         * @return 配置实体列表
         */
        default List<ClusterServiceRoleGroupConfigEntity> selectByRoleGroupIds(
                        @Param("roleGroupIds") List<Integer> roleGroupIds) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterServiceRoleGroupConfigEntity::getRoleGroupId).in(roleGroupIds);
                return this.selectListByQuery(query);
        }

        /**
         * 根据角色组ID获取配置版本列表（按版本号降序）
         *
         * @param roleGroupId 角色组ID
         * @return 配置版本列表
         */
        default List<ClusterServiceRoleGroupConfigEntity> selectConfigVersionsByRoleGroupId(
                        @Param("roleGroupId") Long roleGroupId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterServiceRoleGroupConfigEntity::getRoleGroupId).eq(roleGroupId)
                                .orderBy(ClusterServiceRoleGroupConfigEntity::getConfigVersion).desc();
                return this.selectListByQuery(query);
        }

        /**
         * 根据角色组ID获取最新的两个配置版本（用于版本比较）
         *
         * @param roleGroupId 角色组ID
         * @return 最新的两个配置版本列表
         */
        default List<ClusterServiceRoleGroupConfigEntity> selectLatestTwoConfigsByRoleGroupId(
                        @Param("roleGroupId") Long roleGroupId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterServiceRoleGroupConfigEntity::getRoleGroupId).eq(roleGroupId)
                                .orderBy(ClusterServiceRoleGroupConfigEntity::getConfigVersion).desc()
                                .limit(2);
                return this.selectListByQuery(query);
        }
}
