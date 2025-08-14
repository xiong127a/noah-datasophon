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

import com.datasophon.api.converter.ClusterServiceRoleInstanceConverter;
import com.datasophon.api.service.RoleInstanceQueryService;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务角色实例查询服务实现类
 * 这个服务专门用于查询功能，避免循环依赖
 * 符合三层架构：DAO ← Service ← Controller
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("roleInstanceQueryService")
public class RoleInstanceQueryServiceImpl
        extends ServiceImpl<ClusterServiceRoleInstanceMapper, ClusterServiceRoleInstanceEntity>
        implements RoleInstanceQueryService {

    @Autowired
    private ClusterServiceRoleInstanceConverter converter;

    @Override
    public List<ClusterServiceRoleInstanceDTO> getServiceRoleListByHostnameAndClusterId(String hostname,
            Long clusterId) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByClusterIdAndHostname(clusterId, hostname);
        return converter.entityListToDtoList(entities);
    }

    @Override
    public ClusterServiceRoleInstanceDTO getByIdAsDto(Long id) {
        ClusterServiceRoleInstanceEntity entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        return converter.entityToDto(entity);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByServiceId(Long serviceId) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByServiceId(serviceId);
        return converter.entityListToDtoList(entities);
    }

    @Override
    public ClusterServiceRoleInstanceDTO getOneServiceRole(String serviceRoleName, String hostname,
            Long clusterId) {
        ClusterServiceRoleInstanceEntity entity;
        if (hostname != null && !hostname.isEmpty()) {
            entity = getMapper().selectByClusterIdAndServiceRoleNameAndHostname(clusterId, serviceRoleName, hostname);
        } else {
            entity = getMapper().selectByClusterIdAndServiceRoleName(clusterId, serviceRoleName);
        }

        if (entity == null) {
            return null;
        }
        return converter.entityToDto(entity);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> listRoleIns(String hostname, String serviceName) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByHostnameAndServiceName(hostname,
                serviceName);
        return converter.entityListToDtoList(entities);
    }
}