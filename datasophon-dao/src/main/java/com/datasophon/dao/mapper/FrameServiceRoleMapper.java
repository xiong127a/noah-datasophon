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

import java.util.List;

/**
 * 框架服务角色表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-18 14:38:53
 */
@Mapper
public interface FrameServiceRoleMapper extends BaseMapper<FrameServiceRoleEntity> {

    /**
     * 根据服务ID列表和角色类型查询服务角色
     *
     * @param serviceIds      服务ID列表
     * @param serviceRoleType 服务角色类型，可为空
     * @return 服务角色列表
     */
    List<FrameServiceRoleEntity> selectServiceRolesByServiceIds(@Param("serviceIds") List<String> serviceIds,
            @Param("serviceRoleType") Integer serviceRoleType);

    /**
     * 根据服务ID列表查询非Master角色
     *
     * @param serviceIds 服务ID列表
     * @return 非Master角色列表
     */
    List<FrameServiceRoleEntity> selectNonMasterRolesByServiceIds(@Param("serviceIds") List<String> serviceIds);

    /**
     * 根据服务ID和角色名查询服务角色
     *
     * @param serviceId       服务ID
     * @param serviceRoleName 角色名
     * @return 服务角色实体
     */
    FrameServiceRoleEntity selectServiceRoleByServiceIdAndRoleName(@Param("serviceId") Integer serviceId,
            @Param("serviceRoleName") String serviceRoleName);

    /**
     * 根据框架代码和角色名查询服务角色
     *
     * @param frameCode       框架代码
     * @param serviceRoleName 角色名
     * @return 服务角色实体
     */
    FrameServiceRoleEntity selectServiceRoleByFrameCodeAndRoleName(@Param("frameCode") String frameCode,
            @Param("serviceRoleName") String serviceRoleName);

    /**
     * 根据服务ID查询所有服务角色
     *
     * @param serviceId 服务ID
     * @return 服务角色列表
     */
    List<FrameServiceRoleEntity> selectAllServiceRolesByServiceId(@Param("serviceId") Integer serviceId);

    /**
     * 根据集群ID和服务名查询服务角色
     *
     * @param clusterId 集群ID
     * @param serviceName 服务名
     * @return 服务角色列表
     */
    List<FrameServiceRoleEntity> selectServiceRolesByServiceName(@Param("clusterId") Integer clusterId, 
            @Param("serviceName") String serviceName);

}
