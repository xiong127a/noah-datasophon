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

package com.datasophon.api.service.impl;

import com.datasophon.api.service.RoleGroupQueryService;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.mapper.ClusterServiceInstanceRoleGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 服务角色组查询服务实现类
 * 这个服务仅用于提供查询功能，避免循环依赖
 */
@Service
public class RoleGroupQueryServiceImpl implements RoleGroupQueryService {

    @Autowired
    private ClusterServiceInstanceRoleGroupMapper roleGroupMapper;

    @Override
    public ClusterServiceInstanceRoleGroup getById(Integer id) {
        return roleGroupMapper.selectOneById(id);
    }
}