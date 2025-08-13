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

import com.datasophon.api.converter.ClusterRoleUserConverter;
import com.datasophon.api.converter.UserInfoConverter;
import com.datasophon.api.service.ClusterRoleUserService;
import com.datasophon.common.dto.ClusterRoleUserDTO;
import com.datasophon.common.dto.UserInfoDTO;
import com.datasophon.dao.entity.ClusterRoleUserEntity;

import com.datasophon.common.enums.UserType;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.ClusterRoleUserMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 集群角色用户服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterRoleUserService")
public class ClusterRoleUserServiceImpl extends ServiceImpl<ClusterRoleUserMapper, ClusterRoleUserEntity>
        implements ClusterRoleUserService {

    @Autowired
    private ClusterRoleUserConverter clusterRoleUserConverter;

    @Autowired
    private UserInfoConverter userInfoConverter;

    @Override
    public boolean isClusterManager(Long userId, Long clusterId) {
        List<ClusterRoleUserEntity> list = getMapper().selectByUserIdAndClusterId(userId, clusterId);
        return Objects.nonNull(list) && !list.isEmpty();
    }

    @Override
    public boolean saveClusterManager(Long clusterId, String userIds) {
        // 首先删除原有管理员
        getMapper().removeByClusterId(clusterId);

        if (StringUtils.isEmpty(userIds)) {
            // userIds 为空,表示取消授权
            return true;
        }

        // 使用流式处理构建实体列表，使用JDK21的toList()
        List<ClusterRoleUserEntity> entityList = Arrays.stream(userIds.split(","))
                .map(id -> {
                    ClusterRoleUserEntity entity = new ClusterRoleUserEntity();
                    entity.setClusterId(clusterId);
                    entity.setUserId(Integer.parseInt(id));
                    entity.setUserType(UserType.ADMIN);
                    return entity;
                })
                .toList();

        getMapper().saveBatch(entityList);
        return true;
    }

    @Override
    public List<UserInfoDTO> getAllClusterManagerByClusterId(Long clusterId) {
        List<UserInfoEntity> entities = getMapper().getAllClusterManagerByClusterId(clusterId);
        return userInfoConverter.entityListToDtoList(entities);
    }

    @Override
    public List<ClusterRoleUserDTO> getAllClusterRoleUsers() {
        List<ClusterRoleUserEntity> entities = list();
        return clusterRoleUserConverter.entityListToDtoList(entities);
    }
}