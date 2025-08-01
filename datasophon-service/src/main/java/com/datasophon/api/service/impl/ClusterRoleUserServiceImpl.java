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

import com.datasophon.api.service.ClusterRoleUserService;
import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.enums.UserType;
import com.datasophon.dao.mapper.ClusterRoleUserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 集群角色用户服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Service("clusterRoleUserService")
public class ClusterRoleUserServiceImpl implements ClusterRoleUserService {

    @Autowired
    private ClusterRoleUserMapper clusterRoleUserMapper;

    @Override
    public boolean isClusterManager(Integer userId, String clusterId) {
        List<ClusterRoleUserEntity> list = clusterRoleUserMapper.selectByUserIdAndClusterId(userId, clusterId);
        return Objects.nonNull(list) && list.size() == 1;
    }

    @Override
    public boolean saveClusterManager(Integer clusterId, String userIds) {
        // 首先删除原有管理员
        clusterRoleUserMapper.removeByClusterId(clusterId);

        if (StringUtils.isEmpty(userIds)) {
            // userIds 为空,表示取消授权
            return true;
        }

        // 使用流式处理构建实体列表
        List<ClusterRoleUserEntity> entityList = Arrays.stream(userIds.split(","))
                .map(id -> {
                    ClusterRoleUserEntity entity = new ClusterRoleUserEntity();
                    entity.setClusterId(clusterId);
                    entity.setUserId(Integer.parseInt(id));
                    entity.setUserType(UserType.ADMIN);
                    return entity;
                })
                .collect(Collectors.toList());

        clusterRoleUserMapper.saveBatch(entityList);
        return true;
    }

    @Override
    public List<UserInfoEntity> getAllClusterManagerByClusterId(Integer clusterId) {
        return clusterRoleUserMapper.getAllClusterManagerByClusterId(clusterId);
    }

    // 标准CRUD方法实现
    @Override
    public ClusterRoleUserEntity getById(Integer id) {
        return clusterRoleUserMapper.selectById(id);
    }

    @Override
    public ClusterRoleUserEntity save(ClusterRoleUserEntity entity) {
        clusterRoleUserMapper.insert(entity);
        return entity;
    }

    @Override
    public ClusterRoleUserEntity updateById(ClusterRoleUserEntity entity) {
        clusterRoleUserMapper.updateById(entity);
        return entity;
    }

    @Override
    public boolean removeByIds(List<Integer> ids) {
        return clusterRoleUserMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<ClusterRoleUserEntity> getAllClusterRoleUsers() {
        return clusterRoleUserMapper.selectAll();
    }
}