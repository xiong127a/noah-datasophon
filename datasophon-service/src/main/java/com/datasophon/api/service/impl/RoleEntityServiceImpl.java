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
import com.datasophon.common.enums.NeedRestart;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 服务角色实例实体服务实现类
 * 按照架构重构规范，迁移QueryChain到DAO层，整合查询和更新功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service
public class RoleEntityServiceImpl implements RoleEntityService {

    private static final Logger logger = LoggerFactory.getLogger(RoleEntityServiceImpl.class);

    @Autowired
    private ClusterServiceRoleInstanceMapper roleInstanceMapper;

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleListByHostnameAndClusterId(String hostname,
            Long clusterId) {
        return roleInstanceMapper.selectByHostnameAndClusterId(hostname, clusterId);
    }

    @Override
    public ClusterServiceRoleInstanceEntity getById(Integer id) {
        return roleInstanceMapper.selectOneById(id);
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByServiceId(int serviceId) {
        return roleInstanceMapper.selectByServiceId(serviceId);
    }

    @Override
    public ClusterServiceRoleInstanceEntity getOneServiceRole(String serviceRoleName, String hostname,
            Long clusterId) {
        List<ClusterServiceRoleInstanceEntity> list = roleInstanceMapper
                .selectByServiceRoleNameAndClusterId(serviceRoleName, clusterId, hostname);

        if (Objects.nonNull(list) && !list.isEmpty()) {
            return list.getFirst(); // JDK21现代特性：使用getFirst()替代get(0)
        }
        return null;
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> listRoleIns(String hostname, String serviceName) {
        return roleInstanceMapper.selectByHostnameAndServiceName(hostname, serviceName);
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