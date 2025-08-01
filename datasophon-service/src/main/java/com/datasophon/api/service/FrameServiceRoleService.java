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

import java.util.List;

/**
 * 框架服务角色表
 *
 */
public interface FrameServiceRoleService {

    /**
     * 根据集群ID、服务ID列表和角色类型获取服务角色列表
     * 
     * @param clusterId       集群ID
     * @param serviceIds      服务ID列表（逗号分隔）
     * @param serviceRoleType 服务角色类型
     * @return 服务角色列表
     */
    List<FrameServiceRoleEntity> getServiceRoleList(Integer clusterId, String serviceIds, Integer serviceRoleType);

    /**
     * 根据服务ID和服务角色名称获取服务角色
     * 
     * @param serviceId 服务ID
     * @param roleName  角色名称
     * @return 服务角色实体
     */
    FrameServiceRoleEntity getServiceRoleByServiceIdAndServiceRoleName(Integer serviceId, String roleName);

    /**
     * 根据集群框架和服务角色名称获取服务角色
     * 
     * @param clusterFrame    集群框架
     * @param serviceRoleName 服务角色名称
     * @return 服务角色实体
     */
    FrameServiceRoleEntity getServiceRoleByFrameCodeAndServiceRoleName(String clusterFrame, String serviceRoleName);

    /**
     * 获取非Master角色列表
     * 
     * @param clusterId  集群ID
     * @param serviceIds 服务ID列表（逗号分隔）
     * @return 非Master角色列表
     */
    List<FrameServiceRoleEntity> getNonMasterRoleList(Integer clusterId, String serviceIds);

    /**
     * 根据服务名称获取服务角色列表
     * 
     * @param clusterId   集群ID
     * @param serviceName 服务名称
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

    // 基础CRUD方法

    /**
     * 根据ID获取服务角色
     * 
     * @param id 主键ID
     * @return 服务角色实体
     */
    FrameServiceRoleEntity getById(Integer id);

    /**
     * 保存服务角色
     * 
     * @param entity 服务角色实体
     * @return 是否保存成功
     */
    boolean save(FrameServiceRoleEntity entity);

    /**
     * 根据ID更新服务角色
     * 
     * @param entity 服务角色实体
     * @return 是否更新成功
     */
    boolean updateById(FrameServiceRoleEntity entity);

    /**
     * 根据ID列表批量删除服务角色
     * 
     * @param ids ID列表
     * @return 是否删除成功
     */
    boolean removeByIds(List<Integer> ids);
}
