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

import com.datasophon.common.dto.ClusterServiceInstanceRoleGroupDTO;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroupEntity;
import com.mybatisflex.core.service.IService;

/**
 * 服务角色组查询服务
 * 这个接口专门用于查询服务角色组，避免循环依赖
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface RoleGroupQueryService extends IService<ClusterServiceInstanceRoleGroupEntity> {

    /**
     * 根据ID获取角色组DTO
     * 
     * @param id 角色组ID
     * @return 角色组DTO
     */
    ClusterServiceInstanceRoleGroupDTO getByIdAsDto(Long id);
}