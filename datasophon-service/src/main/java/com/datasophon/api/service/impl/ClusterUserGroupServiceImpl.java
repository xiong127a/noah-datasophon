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

import com.datasophon.api.converter.ClusterGroupConverter;
import com.datasophon.api.converter.ClusterUserConverter;
import com.datasophon.api.converter.ClusterUserGroupConverter;
import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.api.service.ClusterUserGroupService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.common.dto.ClusterGroupDTO;
import com.datasophon.common.dto.ClusterUserDTO;
import com.datasophon.common.dto.ClusterUserGroupDTO;
import com.datasophon.dao.entity.ClusterGroup;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.entity.ClusterUserGroup;
import com.datasophon.dao.mapper.ClusterUserGroupMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 集群用户组关联服务实现类
 * 提供集群用户组关联的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterUserGroupService")
public class ClusterUserGroupServiceImpl extends ServiceImpl<ClusterUserGroupMapper, ClusterUserGroup>
        implements ClusterUserGroupService {

    // 用户组类型常量
    private static final int USER_GROUP_TYPE_MAIN = 1;
    private static final int USER_GROUP_TYPE_OTHER = 2;

    @Autowired
    private ClusterGroupService clusterGroupService;

    @Autowired
    private ClusterUserService userService;

    @Autowired
    private ClusterUserGroupConverter clusterUserGroupConverter;

    @Autowired
    private ClusterGroupConverter clusterGroupConverter;

    @Autowired
    private ClusterUserConverter clusterUserConverter;

    @Override
    public Long countGroupUserNum(Integer groupId) {
        return getMapper().countByGroupId(groupId);
    }

    @Override
    public void deleteByUser(Integer userId) {
        getMapper().deleteByUserId(userId);
    }

    @Override
    public ClusterGroupDTO queryMainGroup(Integer userId) {
        // 查询指定用户的主用户组
        List<ClusterUserGroup> userGroups = getMapper().selectByUserIdAndType(userId, USER_GROUP_TYPE_MAIN);

        if (userGroups.isEmpty()) {
            return null;
        }

        // 获取第一个组ID并查询组信息
        Integer groupId = userGroups.getFirst().getGroupId();
        ClusterGroup clusterGroup = clusterGroupService.getById(groupId);
        return clusterGroupConverter.entityToDto(clusterGroup);
    }

    @Override
    public List<ClusterGroupDTO> listOtherGroups(Integer userId) {
        // 查询指定用户的其他用户组
        List<ClusterUserGroup> userGroups = getMapper().selectByUserIdAndType(userId, USER_GROUP_TYPE_OTHER);

        if (userGroups.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取组ID列表并查询组信息
        List<Integer> groupIds = userGroups.stream()
                .map(ClusterUserGroup::getGroupId)
                .toList();

        List<ClusterGroup> clusterGroups = clusterGroupService.listByIds(groupIds);
        return clusterGroups.stream()
                .map(clusterGroupConverter::entityToDto)
                .toList();
    }

    @Override
    public List<ClusterUserDTO> listClusterUsers(Integer groupId) {
        // 查询指定组的所有用户关联
        List<ClusterUserGroup> userGroups = getMapper().selectByGroupId(groupId);

        if (userGroups.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取用户ID列表并查询用户信息
        List<Integer> userIds = userGroups.stream()
                .map(ClusterUserGroup::getUserId)
                .toList();

        List<ClusterUser> clusterUsers = userService.listByIds(userIds);
        return clusterUsers.stream()
                .map(clusterUserConverter::entityToDto)
                .toList();
    }

    @Override
    public ClusterUserGroupDTO getByIdAsDto(Integer id) {
        // Service层：Entity → DTO转换
        ClusterUserGroup entity = this.getById(id);
        return clusterUserGroupConverter.entityToDto(entity);
    }

    @Override
    public void saveUserGroup(ClusterUserGroupDTO dto) {
        // Service层：DTO → Entity转换
        ClusterUserGroup entity = clusterUserGroupConverter.dtoToEntity(dto);
        this.save(entity);
    }

    @Override
    public void updateUserGroup(ClusterUserGroupDTO dto) {
        // Service层：DTO → Entity转换
        ClusterUserGroup entity = clusterUserGroupConverter.dtoToEntity(dto);
        this.updateById(entity);
    }
}
