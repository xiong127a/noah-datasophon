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

import com.datasophon.api.converter.ClusterServiceInstanceRoleGroupConverter;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.RoleEntityService;
import com.datasophon.api.service.RoleGroupEntityService;
import com.datasophon.common.dto.ClusterServiceInstanceRoleGroupDTO;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroupEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.common.enums.NeedRestart;
import com.datasophon.dao.mapper.ClusterServiceInstanceMapper;
import com.datasophon.dao.mapper.ClusterServiceInstanceRoleGroupMapper;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * 集群服务实例角色组服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterServiceInstanceRoleGroupService")
public class ClusterServiceInstanceRoleGroupServiceImpl extends ServiceImpl<ClusterServiceInstanceRoleGroupMapper, ClusterServiceInstanceRoleGroupEntity> implements ClusterServiceInstanceRoleGroupService {

    @Autowired
    private ClusterServiceInstanceRoleGroupConverter clusterServiceInstanceRoleGroupConverter;

    @Autowired
    private ClusterServiceRoleGroupConfigConverter clusterServiceRoleGroupConfigConverter;

    @Autowired
    private ClusterServiceInstanceMapper clusterServiceInstanceMapper;

    @Autowired
    private ClusterServiceRoleInstanceMapper clusterServiceRoleInstanceMapper;

    @Autowired
    private RoleEntityService roleEntityService;

    @Autowired
    private RoleGroupEntityService roleGroupEntityService;

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    private static final String DEFAULT = "default";

    @Autowired
    public ClusterServiceInstanceRoleGroupServiceImpl(
            RoleEntityService roleEntityService,
            ClusterServiceRoleGroupConfigService roleGroupConfigService) {
        this.roleEntityService = roleEntityService;
        this.roleGroupConfigService = roleGroupConfigService;
    }

    @Override
    public ClusterServiceInstanceRoleGroupDTO getRoleGroupByServiceInstanceId(Long serviceInstanceId) {
        ClusterServiceInstanceRoleGroupEntity entity = getMapper().selectByServiceInstanceIdAndRoleGroupType(serviceInstanceId,
                DEFAULT);
        return clusterServiceInstanceRoleGroupConverter.entityToDto(entity);
    }

    @Override
    public void saveRoleGroup(Long serviceInstanceId, Long roleGroupId, String roleGroupName) {
        // 通过Mapper直接查询服务实例信息
        ClusterServiceInstanceEntity serviceInstance = clusterServiceInstanceMapper.selectOneById(serviceInstanceId);
        if (serviceInstance == null) {
            return;
        }

        // is repeat name
        if (isRepeatRoleGroupName(serviceInstanceId, roleGroupName)) {
            return;
        }
        ClusterServiceInstanceRoleGroupEntity roleGroup = new ClusterServiceInstanceRoleGroupEntity();
        roleGroup.setRoleGroupType("custom");
        roleGroup.setRoleGroupName(roleGroupName);
        roleGroup.setServiceName(serviceInstance.getServiceName());
        roleGroup.setServiceInstanceId(serviceInstanceId);
        roleGroup.setClusterId(serviceInstance.getClusterId());
        roleGroup.setNeedRestart(NeedRestart.NO);
        this.save(roleGroup);

        ClusterServiceRoleGroupConfigDTO configDTO = roleGroupConfigService.getConfigByRoleGroupId(roleGroupId);
        if (configDTO != null) {
            ClusterServiceRoleGroupConfigEntity config = clusterServiceRoleGroupConfigConverter.dtoToEntity(configDTO);
            ClusterServiceRoleGroupConfigEntity roleGroupConfig = new ClusterServiceRoleGroupConfigEntity();
            BeanUtils.copyProperties(config, roleGroupConfig);
            roleGroupConfig.setConfigVersion(1);
            roleGroupConfig.setId(null);
            roleGroupConfig.setRoleGroupId(roleGroup.getId());
            roleGroupConfigService.save(roleGroupConfig);
        }
    }

    private boolean isRepeatRoleGroupName(Long serviceInstanceId, String roleGroupName) {
        return getMapper().countByServiceInstanceIdAndRoleGroupName(serviceInstanceId,
                roleGroupName) > 0;
    }

    @Override
    public boolean bind(String roleInstanceIds, Long roleGroupId) {
        // 委托给roleGroupEntityService处理
        try {
            roleGroupEntityService.bindRoleInstances(roleInstanceIds, roleGroupId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSameRoleGroup(ClusterServiceRoleInstanceEntity roleInstanceEntity, List<String> ids) {

        // query role instance by hostname and servicename
        List<ClusterServiceRoleInstanceEntity> roleList = roleEntityService
                .listRoleIns(roleInstanceEntity.getHostname(), roleInstanceEntity.getServiceName());
        List<String> listIds = roleList.stream().map(e -> e.getId().toString()).toList();
        return new HashSet<>(ids).containsAll(listIds);
    }

    @Override
    public ClusterServiceRoleGroupConfigDTO getRoleGroupConfigByServiceId(Long serviceInstanceId) {
        ClusterServiceInstanceRoleGroupEntity instanceRoleGroup = getMapper()
                .selectByServiceInstanceIdAndRoleGroupType(serviceInstanceId, "default");

        if (instanceRoleGroup != null) {
            return roleGroupConfigService.getConfigByRoleGroupId(instanceRoleGroup.getId());
        }
        return null;
    }

    @Override
    public boolean rename(Long roleGroupId, String roleGroupName) {
        ClusterServiceInstanceRoleGroupEntity roleGroup = this.getById(roleGroupId);
        if (roleGroup == null) {
            return false;
        }
        if (!roleGroup.getRoleGroupName().equals(roleGroupName)
                && isRepeatRoleGroupName(roleGroup.getServiceInstanceId(), roleGroupName)) {
            return false; // 重复的角色组名称
        }
        roleGroup.setRoleGroupName(roleGroupName);
        return this.updateById(roleGroup);
    }

    @Override
    public boolean deleteRoleGroup(Long roleGroupId) {
        if (hasRoleInstanceUse(roleGroupId)) {
            return false; // 当前角色组正在使用中
        }
        if (isDefaultRoleGroup(roleGroupId)) {
            return false; // 不能删除默认角色组
        }
        boolean removed = this.removeByIds(Collections.singletonList(roleGroupId));
        if (removed) {
            roleGroupConfigService.removeAllByRoleGroupId(roleGroupId);
        }
        return removed;
    }

    private boolean isDefaultRoleGroup(Long roleGroupId) {
        ClusterServiceInstanceRoleGroupEntity roleGroup = this.getById(roleGroupId);
        String roleGroupType = roleGroup.getRoleGroupType();
        return DEFAULT.equals(roleGroupType);
    }

    @Override
    public List<ClusterServiceInstanceRoleGroupDTO> listRoleGroupByServiceInstanceId(Long serviceInstanceId) {
        List<ClusterServiceInstanceRoleGroupEntity> entities = getMapper().selectByServiceInstanceId(serviceInstanceId);
        return clusterServiceInstanceRoleGroupConverter.entityListToDtoList(entities);
    }

    @Override
    public void updateToNeedRestart(Long roleGroupId) {
        // 委托给roleGroupEntityService处理
        roleGroupEntityService.updateToNeedRestart(roleGroupId);
    }

    private boolean hasRoleInstanceUse(Long roleGroupId) {
        // 查询是否有角色实例正在使用这个角色组
        return clusterServiceRoleInstanceMapper.countByRoleGroupId(roleGroupId) > 0;
    }

    // 基础CRUD方法实现

    // 基础CRUD方法已由ServiceImpl提供，无需重复实现
}
