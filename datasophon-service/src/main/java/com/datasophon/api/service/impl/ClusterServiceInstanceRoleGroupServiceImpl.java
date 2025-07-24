/*
 *
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
 *
 */

package com.datasophon.api.service.impl;

import com.datasophon.api.enums.Status;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.mapper.ClusterServiceInstanceRoleGroupMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Service("clusterServiceInstanceRoleGroupService")
public class ClusterServiceInstanceRoleGroupServiceImpl
        extends
        ServiceImpl<ClusterServiceInstanceRoleGroupMapper, ClusterServiceInstanceRoleGroup>
        implements
        ClusterServiceInstanceRoleGroupService {

    private final ClusterServiceInstanceService serviceInstanceService;

    private final ClusterServiceRoleInstanceService roleInstanceService;

    private final ClusterServiceRoleGroupConfigService roleGroupConfigService;

    private static final String DEFAULT = "default";
    @Autowired
    public ClusterServiceInstanceRoleGroupServiceImpl(ClusterServiceInstanceService serviceInstanceService, ClusterServiceRoleInstanceService roleInstanceService, ClusterServiceRoleGroupConfigService roleGroupConfigService) {
        this.serviceInstanceService = serviceInstanceService;
        this.roleInstanceService = roleInstanceService;
        this.roleGroupConfigService = roleGroupConfigService;
    }

    @Override
    public ClusterServiceInstanceRoleGroup getRoleGroupByServiceInstanceId(Integer serviceInstanceId) {
        return QueryChain.of(ClusterServiceInstanceRoleGroup.class)
                .where(ClusterServiceInstanceRoleGroup::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterServiceInstanceRoleGroup::getRoleGroupType).eq(DEFAULT)
                .one();
    }

    @Override
    public Result saveRoleGroup(Integer serviceInstanceId, Integer roleGroupId, String roleGroupName) {
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceInstanceId);
        // is repeat name
        if (isRepeatRoleGroupName(serviceInstanceId, roleGroupName)) {
            return Result.error(Status.REPEAT_ROLE_GROUP_NAME.getMsg());
        }
        ClusterServiceInstanceRoleGroup roleGroup = new ClusterServiceInstanceRoleGroup();
        roleGroup.setRoleGroupType("custom");
        roleGroup.setRoleGroupName(roleGroupName);
        roleGroup.setServiceName(serviceInstance.getServiceName());
        roleGroup.setServiceInstanceId(serviceInstanceId);
        roleGroup.setClusterId(serviceInstance.getClusterId());
        roleGroup.setNeedRestart(NeedRestart.NO);
        this.save(roleGroup);
        ClusterServiceRoleGroupConfig config = roleGroupConfigService.getConfigByRoleGroupId(roleGroupId);
        ClusterServiceRoleGroupConfig roleGroupConfig = new ClusterServiceRoleGroupConfig();
        BeanUtils.copyProperties(config, roleGroupConfig);
        roleGroupConfig.setConfigVersion(1);
        roleGroupConfig.setId(null);
        roleGroupConfig.setRoleGroupId(roleGroup.getId());
        roleGroupConfigService.save(roleGroupConfig);
        return Result.success();
    }

    private boolean isRepeatRoleGroupName(Integer serviceInstanceId, String roleGroupName) {
        long count = QueryChain.of(ClusterServiceInstanceRoleGroup.class)
                .where(ClusterServiceInstanceRoleGroup::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterServiceInstanceRoleGroup::getRoleGroupName).eq(roleGroupName)
                .count();
        return count > 0;
    }

    @Override
    public Result bind(String roleInstanceIds, Integer roleGroupId) {
        String[] ids = roleInstanceIds.split(",");
        for (String id : ids) {
            ClusterServiceRoleInstanceEntity roleInstanceEntity = roleInstanceService.getById(id);
            if (!isSameRoleGroup(roleInstanceEntity, Arrays.asList(ids))) {
                return Result.error(Status.NEED_SAME_ROLE_GROUP.getMsg());
            }
            // 判断新角色组与原角色组配置是否相同，不相同则需标识该角色实例需要重启
            if (!isSameConfig(roleInstanceEntity.getRoleGroupId(), roleGroupId)) {
                roleInstanceEntity.setNeedRestart(NeedRestart.YES);
            }
            roleInstanceEntity.setRoleGroupId(roleGroupId);
            roleInstanceService.updateById(roleInstanceEntity);
        }
        return Result.success();
    }

    private boolean isSameRoleGroup(ClusterServiceRoleInstanceEntity roleInstanceEntity, List<String> ids) {

        // query role instance by hostname and servicename
        List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService
                .listRoleIns(roleInstanceEntity.getHostname(), roleInstanceEntity.getServiceName());
        List<String> listIds = roleList.stream().map(e -> e.getId().toString()).toList();
        return new HashSet<>(ids).containsAll(listIds);
    }

    private boolean isSameConfig(Integer oldRoleGroupId, Integer newRoleGroupId) {
        ClusterServiceRoleGroupConfig oldConfig = roleGroupConfigService.getConfigByRoleGroupId(oldRoleGroupId);
        ClusterServiceRoleGroupConfig newConfig = roleGroupConfigService.getConfigByRoleGroupId(newRoleGroupId);
        return oldConfig.getConfigJsonMd5().equals(newConfig.getConfigJsonMd5());
    }

    @Override
    public ClusterServiceRoleGroupConfig getRoleGroupConfigByServiceId(Integer serviceInstanceId) {
        ClusterServiceInstanceRoleGroup instanceRoleGroup = QueryChain.of(ClusterServiceInstanceRoleGroup.class)
                .where(ClusterServiceInstanceRoleGroup::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterServiceInstanceRoleGroup::getRoleGroupType).eq("default")
                .one();

        if (instanceRoleGroup != null) {
            return roleGroupConfigService.getConfigByRoleGroupId(instanceRoleGroup.getId());
        }
        return null;
    }

    @Override
    public Result rename(Integer roleGroupId, String roleGroupName) {
        ClusterServiceInstanceRoleGroup roleGroup = this.getById(roleGroupId);
        if (!roleGroup.getRoleGroupName().equals(roleGroupName)
                && isRepeatRoleGroupName(roleGroup.getServiceInstanceId(), roleGroupName)) {
            return Result.error(Status.REPEAT_ROLE_GROUP_NAME.getMsg());
        }
        roleGroup.setRoleGroupName(roleGroupName);
        this.updateById(roleGroup);
        return Result.success();
    }

    @Override
    public Result deleteRoleGroup(Integer roleGroupId) {
        if (hasRoleInstanceUse(roleGroupId)) {
            return Result.error(Status.THE_CURRENT_ROLE_GROUP_BE_USING.getMsg());
        }
        if (isDefaultRoleGroup(roleGroupId)) {
            return Result.error(Status.THE_CURRENT_ROLE_GROUP_IS_DEFAULT.getMsg());
        }
        this.removeById(roleGroupId);
        roleGroupConfigService.removeAllByRoleGroupId(roleGroupId);
        return Result.success();
    }

    private boolean isDefaultRoleGroup(Integer roleGroupId) {
        ClusterServiceInstanceRoleGroup roleGroup = this.getById(roleGroupId);
        String roleGroupType = roleGroup.getRoleGroupType();
        return DEFAULT.equals(roleGroupType);
    }

    @Override
    public List<ClusterServiceInstanceRoleGroup> listRoleGroupByServiceInstanceId(Integer serviceInstanceId) {
        return QueryChain.of(ClusterServiceInstanceRoleGroup.class)
                .where(ClusterServiceInstanceRoleGroup::getServiceInstanceId).eq(serviceInstanceId)
                .list();
    }

    @Override
    public void updateToNeedRestart(Integer roleGroupId) {
        List<ClusterServiceRoleInstanceEntity> roleInstanceList = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getRoleGroupId).eq(roleGroupId)
                .list();

        if (roleInstanceList != null && !roleInstanceList.isEmpty()) {
            for (ClusterServiceRoleInstanceEntity roleInstance : roleInstanceList) {
                roleInstance.setNeedRestart(NeedRestart.YES);
                roleInstanceService.updateById(roleInstance);
            }
        }
    }

    private boolean hasRoleInstanceUse(Integer roleGroupId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getRoleGroupId).eq(roleGroupId)
                .exists();
    }
}
