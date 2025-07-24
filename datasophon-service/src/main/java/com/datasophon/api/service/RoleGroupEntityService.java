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

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;

/**
 * 服务角色组实体服务
 * 这个接口提供角色组相关的功能，避免循环依赖
 */
public interface RoleGroupEntityService {
    /**
     * 根据ID获取角色组
     * 
     * @param id 角色组ID
     * @return 角色组
     */
    ClusterServiceInstanceRoleGroup getById(Integer id);

    /**
     * 将角色实例绑定到角色组
     * 
     * @param roleInstanceIds 角色实例ID列表，以逗号分隔
     * @param roleGroupId     角色组ID
     * @return 绑定结果
     */
    Result bindRoleInstances(String roleInstanceIds, Integer roleGroupId);

    /**
     * 更新角色组的需要重启标志
     * 
     * @param roleGroupId 角色组ID
     * @return 是否更新成功
     */
    boolean updateToNeedRestart(Integer roleGroupId);
}