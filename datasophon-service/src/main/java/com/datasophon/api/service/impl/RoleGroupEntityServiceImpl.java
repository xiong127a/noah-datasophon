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

import com.datasophon.common.enums.Status;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.RoleEntityService;
import com.datasophon.api.service.RoleGroupEntityService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.mapper.ClusterServiceInstanceRoleGroupMapper;
import com.mybatisflex.core.query.QueryChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * 服务角色组实体服务实现类
 * 这个实现类使用RoleEntityService处理角色实例操作
 */
@Service
public class RoleGroupEntityServiceImpl implements RoleGroupEntityService {

    @Autowired
    private ClusterServiceInstanceRoleGroupMapper roleGroupMapper;

    @Autowired
    private RoleEntityService roleEntityService;

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    @Override
    public ClusterServiceInstanceRoleGroup getById(Integer id) {
        return roleGroupMapper.selectOneById(id);
    }

    @Override
    public Result bindRoleInstances(String roleInstanceIds, Integer roleGroupId) {
        String[] ids = roleInstanceIds.split(",");
        for (String id : ids) {
            ClusterServiceRoleInstanceEntity roleInstanceEntity = roleEntityService
                    .getById(Integer.parseInt(id));

            if (!isSameRoleGroup(roleInstanceEntity, Arrays.asList(ids))) {
                return Result.error(Status.NEED_SAME_ROLE_GROUP.getMsg());
            }

            // 判断新角色组与原角色组配置是否相同，不相同则需标识该角色实例需要重启
            boolean needRestart = !isSameConfig(roleInstanceEntity.getRoleGroupId(), roleGroupId);

            // 通过RoleEntityService更新角色实例的角色组ID
            roleEntityService.updateRoleGroupId(roleInstanceEntity.getId(), roleGroupId, needRestart);
        }
        return Result.success();
    }

    @Override
    public boolean updateToNeedRestart(Integer roleGroupId) {
        List<ClusterServiceRoleInstanceEntity> list = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getRoleGroupId).eq(roleGroupId)
                .list();

        if (list != null && !list.isEmpty()) {
            for (ClusterServiceRoleInstanceEntity roleInstance : list) {
                roleInstance.setNeedRestart(NeedRestart.YES);
                roleEntityService.updateById(roleInstance);
            }
            return true;
        }
        return false;
    }

    private boolean isSameRoleGroup(ClusterServiceRoleInstanceEntity roleInstanceEntity, List<String> ids) {
        // 查询角色实例
        List<ClusterServiceRoleInstanceEntity> roleList = roleEntityService
                .listRoleIns(roleInstanceEntity.getHostname(), roleInstanceEntity.getServiceName());

        List<String> listIds = roleList.stream().map(e -> e.getId().toString()).toList();
        return new HashSet<>(ids).containsAll(listIds);
    }

    private boolean isSameConfig(Integer oldRoleGroupId, Integer newRoleGroupId) {
        ClusterServiceRoleGroupConfig oldConfig = roleGroupConfigService.getConfigByRoleGroupId(oldRoleGroupId);
        ClusterServiceRoleGroupConfig newConfig = roleGroupConfigService.getConfigByRoleGroupId(newRoleGroupId);
        return oldConfig.getConfigJsonMd5().equals(newConfig.getConfigJsonMd5());
    }
}