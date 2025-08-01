/*
 *
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
 *
 */

package com.datasophon.api.service;

import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;

import java.util.List;

/**
 * 集群服务实例角色组服务接口
 */
public interface ClusterServiceInstanceRoleGroupService {

    /**
     * 根据服务实例ID获取角色组
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 角色组实体
     */
    ClusterServiceInstanceRoleGroup getRoleGroupByServiceInstanceId(Integer serviceInstanceId);

    /**
     * 保存角色组
     * 
     * @param serviceInstanceId 服务实例ID
     * @param roleGroupId       角色组ID
     * @param roleGroupName     角色组名称
     */
    void saveRoleGroup(Integer serviceInstanceId, Integer roleGroupId, String roleGroupName);

    /**
     * 绑定角色实例到角色组
     * 
     * @param roleInstanceIds 角色实例ID列表
     * @param roleGroupId     角色组ID
     * @return 是否绑定成功
     */
    boolean bind(String roleInstanceIds, Integer roleGroupId);

    /**
     * 根据服务ID获取角色组配置
     * 
     * @param serviceId 服务ID
     * @return 角色组配置
     */
    ClusterServiceRoleGroupConfig getRoleGroupConfigByServiceId(Integer serviceId);

    /**
     * 重命名角色组
     * 
     * @param roleGroupId   角色组ID
     * @param roleGroupName 新的角色组名称
     * @return 是否重命名成功
     */
    boolean rename(Integer roleGroupId, String roleGroupName);

    /**
     * 删除角色组
     * 
     * @param roleGroupId 角色组ID
     * @return 是否删除成功
     */
    boolean deleteRoleGroup(Integer roleGroupId);

    /**
     * 根据服务实例ID获取角色组列表
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 角色组列表
     */
    List<ClusterServiceInstanceRoleGroup> listRoleGroupByServiceInstanceId(Integer serviceInstanceId);

    /**
     * 更新角色组为需要重启状态
     * 
     * @param roleGroupId 角色组ID
     */
    void updateToNeedRestart(Integer roleGroupId);

    // 基础CRUD方法

    /**
     * 根据ID获取角色组
     * 
     * @param id 主键ID
     * @return 角色组实体
     */
    ClusterServiceInstanceRoleGroup getById(Integer id);

    /**
     * 保存角色组
     * 
     * @param entity 角色组实体
     * @return 是否保存成功
     */
    boolean save(ClusterServiceInstanceRoleGroup entity);

    /**
     * 根据ID更新角色组
     * 
     * @param entity 角色组实体
     * @return 是否更新成功
     */
    boolean updateById(ClusterServiceInstanceRoleGroup entity);

    /**
     * 根据ID列表批量删除角色组
     * 
     * @param ids ID列表
     * @return 是否删除成功
     */
    boolean removeByIds(List<Integer> ids);
}
