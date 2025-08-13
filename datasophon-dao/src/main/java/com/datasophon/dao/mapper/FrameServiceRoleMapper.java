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

import com.datasophon.dao.entity.FrameServiceRoleEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;

/**
 * 框架服务角色表
 *
 */
@Mapper
public interface FrameServiceRoleMapper extends BaseMapper<FrameServiceRoleEntity> {

        /**
         * 根据服务ID列表和角色类型查询服务角色列表
         * 
         * @param serviceIds      服务ID列表
         * @param serviceRoleType 服务角色类型，可为null
         * @return 服务角色列表
         */
        default List<FrameServiceRoleEntity> selectByServiceIdsAndRoleType(@Param("serviceIds") List<Integer> serviceIds,
                        @Param("serviceRoleType") Integer serviceRoleType) {
                QueryWrapper query = QueryWrapper.create()
                                .where(FrameServiceRoleEntity::getServiceId).in(serviceIds);

                if (serviceRoleType != null) {
                        query.and(FrameServiceRoleEntity::getServiceRoleType).eq(serviceRoleType);
                }

                return this.selectListByQuery(query);
        }

        /**
         * 根据服务ID和角色名称查询服务角色
         * 
         * @param serviceId 服务ID
         * @param roleName  角色名称
         * @return 服务角色实体
         */
        default FrameServiceRoleEntity selectByServiceIdAndRoleName(@Param("serviceId") Long serviceId,
                        @Param("roleName") String roleName) {
                QueryWrapper query = QueryWrapper.create()
                                .where(FrameServiceRoleEntity::getServiceId).eq(serviceId)
                                .and(FrameServiceRoleEntity::getServiceRoleName).eq(roleName);

                return this.selectOneByQuery(query);
        }

        /**
         * 根据框架代码和角色名称查询服务角色
         * 
         * @param frameCode 框架代码
         * @param roleName  角色名称
         * @return 服务角色实体
         */
        default FrameServiceRoleEntity selectByFrameCodeAndRoleName(@Param("frameCode") String frameCode,
                        @Param("roleName") String roleName) {
                QueryWrapper query = QueryWrapper.create()
                                .where(FrameServiceRoleEntity::getFrameCode).eq(frameCode)
                                .and(FrameServiceRoleEntity::getServiceRoleName).eq(roleName);

                return this.selectOneByQuery(query);
        }

        /**
         * 根据服务ID列表查询非MASTER角色列表
         * 
         * @param serviceIds 服务ID列表
         * @return 非MASTER角色列表
         */
        default List<FrameServiceRoleEntity> selectNonMasterRoles(@Param("serviceIds") List<Integer> serviceIds) {
                QueryWrapper query = QueryWrapper.create()
                                .where(FrameServiceRoleEntity::getServiceRoleType).ne(1)
                                .and(FrameServiceRoleEntity::getServiceId).in(serviceIds);

                return this.selectListByQuery(query);
        }

        /**
         * 根据服务ID查询所有角色列表
         * 
         * @param serviceId 服务ID
         * @return 角色列表
         */
        default List<FrameServiceRoleEntity> selectByServiceId(@Param("serviceId") Long serviceId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(FrameServiceRoleEntity::getServiceId).eq(serviceId);

                return this.selectListByQuery(query);
        }

        /**
         * 根据服务ID删除相关的服务角色配置
         * 
         * @param serviceId 服务ID
         * @return 是否删除成功
         */
        default boolean removeByServiceId(Integer serviceId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(FrameServiceRoleEntity::getServiceId).eq(serviceId);
                return this.deleteByQuery(query) > 0;
        }

}
