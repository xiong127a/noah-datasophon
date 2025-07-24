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

import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.api.service.ClusterUserGroupService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.dao.entity.ClusterGroup;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.entity.ClusterUserGroup;
import com.datasophon.dao.mapper.ClusterUserGroupMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service("clusterUserGroupService")
public class ClusterUserGroupServiceImpl extends ServiceImpl<ClusterUserGroupMapper, ClusterUserGroup>
        implements
        ClusterUserGroupService {

    // 用户组类型常量
    private static final int USER_GROUP_TYPE_MAIN = 1;
    private static final int USER_GROUP_TYPE_OTHER = 2;

    @Autowired
    private ClusterGroupService clusterGroupService;

    @Autowired
    private ClusterUserService userService;

    @Override
    public Long countGroupUserNum(Integer groupId) {
        return QueryChain.of(ClusterUserGroup.class)
                .where(ClusterUserGroup::getGroupId).eq(groupId)
                .count();
    }

    @Override
    public void deleteByUser(Integer userId) {
        this.remove(QueryChain.of(ClusterUserGroup.class)
                .where(ClusterUserGroup::getUserId).eq(userId));
    }

    @Override
    public ClusterGroup queryMainGroup(Integer userId) {
        // 查询指定用户的主用户组
        List<ClusterUserGroup> userGroups = QueryChain.of(ClusterUserGroup.class)
                .where(ClusterUserGroup::getUserId).eq(userId)
                .and(ClusterUserGroup::getUserGroupType).eq(USER_GROUP_TYPE_MAIN)
                .list();

        if (userGroups.isEmpty()) {
            return null;
        }

        // 获取第一个组ID并查询组信息
        Integer groupId = userGroups.getFirst().getGroupId();
        return clusterGroupService.getById(groupId);
    }

    @Override
    public List<ClusterGroup> listOtherGroups(Integer userId) {
        // 查询指定用户的其他用户组
        List<ClusterUserGroup> userGroups = QueryChain.of(ClusterUserGroup.class)
                .where(ClusterUserGroup::getUserId).eq(userId)
                .and(ClusterUserGroup::getUserGroupType).eq(USER_GROUP_TYPE_OTHER)
                .list();

        if (userGroups.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取组ID列表并查询组信息
        List<Integer> groupIds = userGroups.stream()
                .map(ClusterUserGroup::getGroupId)
                .collect(Collectors.toList());

        return clusterGroupService.listByIds(groupIds);
    }

    @Override
    public List<ClusterUser> listClusterUsers(Integer groupId) {
        // 查询指定组的所有用户关联
        List<ClusterUserGroup> userGroups = QueryChain.of(ClusterUserGroup.class)
                .where(ClusterUserGroup::getGroupId).eq(groupId)
                .list();

        if (userGroups.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取用户ID列表并查询用户信息
        List<Integer> userIds = userGroups.stream()
                .map(ClusterUserGroup::getUserId)
                .collect(Collectors.toList());

        return userService.listByIds(userIds);
    }
}
