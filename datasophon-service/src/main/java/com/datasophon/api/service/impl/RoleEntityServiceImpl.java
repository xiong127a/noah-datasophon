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

import com.datasophon.api.service.RoleEntityService;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.mybatisflex.core.query.QueryChain;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 服务角色实例实体服务实现类
 * 整合了查询和更新功能，避免循环依赖
 */
@Service
public class RoleEntityServiceImpl implements RoleEntityService {

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

        if (StringUtils.isNotBlank(hostname)) {
            query.and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname);
        }

        List<ClusterServiceRoleInstanceEntity> list = query.list();
        if (Objects.nonNull(list) && !list.isEmpty()) {
            return list.getFirst();
        }
        return null;
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> listRoleIns(String hostname, String serviceName) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceName).eq(serviceName)
                .list();
    }

    @Override
    public boolean updateRoleGroupId(Integer roleInstanceId, Integer roleGroupId, boolean needRestart) {
        ClusterServiceRoleInstanceEntity roleInstance = getById(roleInstanceId);
        if (roleInstance != null) {
            roleInstance.setRoleGroupId(roleGroupId);
            if (needRestart) {
                roleInstance.setNeedRestart(NeedRestart.YES);
            }
            return roleInstanceMapper.update(roleInstance) > 0;
        }
        return false;
    }

    @Override
    public boolean updateById(ClusterServiceRoleInstanceEntity roleInstance) {
        return roleInstanceMapper.update(roleInstance) > 0;
    }
}