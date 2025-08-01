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

import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 框架服务角色表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-18 14:38:53
 */
public interface FrameServiceRoleService extends IService<FrameServiceRoleEntity> {

    /**
     * 获取服务角色列表
     *
     * @param clusterId 集群ID
     * @param serviceIds 服务ID列表，逗号分隔
     * @param serviceRoleType 服务角色类型
     * @return 服务角色列表
     */
    List<FrameServiceRoleEntity> getServiceRoleList(Integer clusterId, String serviceIds, Integer serviceRoleType);

    /**
     * 根据服务ID和角色名获取服务角色
     *
     * @param serviceId 服务ID
     * @param roleName 角色名
     * @return 服务角色实体
     */
    FrameServiceRoleEntity getServiceRoleByServiceIdAndServiceRoleName(Integer serviceId, String roleName);

    /**
     * 根据框架代码和角色名获取服务角色
     *
     * @param clusterFrame 集群框架代码
     * @param serviceRoleName 服务角色名
     * @return 服务角色实体
     */
    FrameServiceRoleEntity getServiceRoleByFrameCodeAndServiceRoleName(String clusterFrame, String serviceRoleName);

    /**
     * 获取非Master角色列表
     *
     * @param clusterId 集群ID
     * @param serviceIds 服务ID列表，逗号分隔
     * @return 非Master角色列表
     */
    List<FrameServiceRoleEntity> getNonMasterRoleList(Integer clusterId, String serviceIds);

    /**
     * 根据服务名获取服务角色列表
     *
     * @param clusterId 集群ID
     * @param serviceName 服务名
     * @return 服务角色列表
     */
    List<FrameServiceRoleEntity> getServiceRoleByServiceName(Integer clusterId, String serviceName);

    /**
     * 获取所有服务角色列表
     *
     * @param frameServiceId 框架服务ID
     * @return 服务角色列表
     */
    List<FrameServiceRoleEntity> getAllServiceRoleList(Integer frameServiceId);
}
