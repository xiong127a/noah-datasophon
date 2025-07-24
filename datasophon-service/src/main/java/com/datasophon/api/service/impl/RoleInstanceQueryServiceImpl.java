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

import com.datasophon.api.service.RoleInstanceQueryService;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.mybatisflex.core.query.QueryChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务角色实例查询服务实现类
 * 这个服务仅用于提供查询功能，避免循环依赖
 */
@Service
public class RoleInstanceQueryServiceImpl implements RoleInstanceQueryService {

    @Autowired
    private ClusterServiceRoleInstanceMapper roleInstanceMapper;

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleListByHostnameAndClusterId(String hostname,
            Integer clusterId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .list();
    }

    @Override
    public ClusterServiceRoleInstanceEntity getById(Integer id) {
        return roleInstanceMapper.selectOneById(id);
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByServiceId(int serviceId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceId)
                .list();
    }

    @Override
    public ClusterServiceRoleInstanceEntity getOneServiceRole(String serviceRoleName, String hostname,
            Integer clusterId) {
        QueryChain<ClusterServiceRoleInstanceEntity> query = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName)
                .and(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId);

        if (hostname != null && !hostname.isEmpty()) {
            query.and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname);
        }

        return query.one();
    }
}