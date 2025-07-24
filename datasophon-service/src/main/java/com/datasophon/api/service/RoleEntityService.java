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

import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.NeedRestart;

import java.util.List;

/**
 * 服务角色实例实体服务
 * 这个接口整合了查询和更新功能，避免循环依赖
 */
public interface RoleEntityService {
    /**
     * 根据主机名和集群ID获取服务角色列表
     * 
     * @param hostname  主机名
     * @param clusterId 集群ID
     * @return 服务角色实例列表
     */
    List<ClusterServiceRoleInstanceEntity> getServiceRoleListByHostnameAndClusterId(String hostname, Integer clusterId);

    /**
     * 根据ID获取服务角色实例
     * 
     * @param id 服务角色实例ID
     * @return 服务角色实例
     */
    ClusterServiceRoleInstanceEntity getById(Integer id);

    /**
     * 根据服务ID获取服务角色实例列表
     * 
     * @param serviceId 服务ID
     * @return 服务角色实例列表
     */
    List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByServiceId(int serviceId);

    /**
     * 根据服务角色名称、主机名和集群ID获取服务角色实例
     * 
     * @param serviceRoleName 服务角色名称
     * @param hostname        主机名
     * @param clusterId       集群ID
     * @return 服务角色实例
     */
    ClusterServiceRoleInstanceEntity getOneServiceRole(String serviceRoleName, String hostname, Integer clusterId);

    /**
     * 根据主机名和服务名称获取角色实例列表
     * 
     * @param hostname    主机名
     * @param serviceName 服务名称
     * @return 服务角色实例列表
     */
    List<ClusterServiceRoleInstanceEntity> listRoleIns(String hostname, String serviceName);

    /**
     * 更新角色实例的角色组ID
     * 
     * @param roleInstanceId 角色实例ID
     * @param roleGroupId    角色组ID
     * @param needRestart    是否需要重启
     * @return 是否更新成功
     */
    boolean updateRoleGroupId(Integer roleInstanceId, Integer roleGroupId, boolean needRestart);

    /**
     * 更新角色实例信息
     * 
     * @param roleInstance 角色实例
     * @return 是否更新成功
     */
    boolean updateById(ClusterServiceRoleInstanceEntity roleInstance);
}