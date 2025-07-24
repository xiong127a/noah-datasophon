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

import java.util.List;

/**
 * 服务角色实例查询服务
 * 这个接口专门用于查询服务角色实例，避免循环依赖
 */
public interface RoleInstanceQueryService {

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
}