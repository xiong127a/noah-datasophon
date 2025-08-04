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
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.mapper.ClusterServiceInstanceRoleGroupMapper;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * 服务角色组实体服务实现类
 * 工具服务实现，按照架构重构规范迁移QueryChain到DAO层
 * 不返回Result类型，改为抛出异常
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service
public class RoleGroupEntityServiceImpl implements RoleGroupEntityService {

    private static final Logger logger = LoggerFactory.getLogger(RoleGroupEntityServiceImpl.class);

    @Autowired
    private ClusterServiceInstanceRoleGroupMapper roleGroupMapper;

    @Autowired
    private ClusterServiceRoleInstanceMapper roleInstanceMapper;

    @Autowired
    private RoleEntityService roleEntityService;

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    @Override
    public ClusterServiceInstanceRoleGroup getById(Integer id) {
        return roleGroupMapper.selectOneById(id);
    }

    @Override
    public void bindRoleInstances(String roleInstanceIds, Integer roleGroupId) {
        String[] ids = roleInstanceIds.split(",");
        for (String id : ids) {
            ClusterServiceRoleInstanceEntity roleInstanceEntity = roleEntityService
                    .getById(Integer.parseInt(id));

            if (!isSameRoleGroup(roleInstanceEntity, Arrays.asList(ids))) {
                throw new RuntimeException(Status.NEED_SAME_ROLE_GROUP.getMsg());
            }

            // 判断新角色组与原角色组配置是否相同，不相同则需标识该角色实例需要重启
            boolean needRestart = !isSameConfig(roleInstanceEntity.getRoleGroupId(), roleGroupId);

            // 通过RoleEntityService更新角色实例的角色组ID
            roleEntityService.updateRoleGroupId(roleInstanceEntity.getId(), roleGroupId, needRestart);
        }
        logger.info("成功绑定角色实例到角色组，角色组ID: {}, 角色实例数量: {}", roleGroupId, ids.length);
    }

    @Override
    public boolean updateToNeedRestart(Integer roleGroupId) {
        List<ClusterServiceRoleInstanceEntity> list = roleInstanceMapper.selectByRoleGroupId(roleGroupId);

        if (list != null && !list.isEmpty()) {
            for (ClusterServiceRoleInstanceEntity roleInstance : list) {
                roleInstance.setNeedRestart(NeedRestart.YES);
                roleEntityService.updateById(roleInstance);
            }
            logger.info("更新角色组需要重启标志成功，角色组ID: {}, 影响实例数量: {}", roleGroupId, list.size());
            return true;
        }
        logger.warn("角色组ID: {} 下没有找到角色实例", roleGroupId);
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
        try {
            // 通过Service获取配置DTO并比较
            ClusterServiceRoleGroupConfigDTO oldConfig = roleGroupConfigService.getConfigByRoleGroupId(oldRoleGroupId);
            ClusterServiceRoleGroupConfigDTO newConfig = roleGroupConfigService.getConfigByRoleGroupId(newRoleGroupId);

            if (oldConfig == null || newConfig == null) {
                return false;
            }

            // 比较配置的MD5值（record类型的accessor方法）
            return oldConfig.configJsonMd5().equals(newConfig.configJsonMd5());
        } catch (Exception e) {
            logger.warn("比较角色组配置失败: {}", e.getMessage());
            return false;
        }
    }
}