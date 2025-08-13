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

package com.datasophon.api.service;

import com.datasophon.common.dto.ClusterGroupDTO;
import com.datasophon.common.dto.ClusterUserDTO;
import com.datasophon.common.dto.ClusterUserGroupDTO;
import com.datasophon.dao.entity.ClusterUserGroupEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群用户组服务接口
 * 提供集群用户组关联的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterUserGroupService extends IService<ClusterUserGroupEntity> {

    /**
     * 统计指定组的用户数量
     *
     * @param groupId 组ID
     * @return 用户数量
     */
    Long countGroupUserNum(Integer groupId);

    /**
     * 删除指定用户的所有组关联
     *
     * @param userId 用户ID
     */
    void deleteByUser(Integer userId);

    /**
     * 查询指定用户的主用户组
     *
     * @param userId 用户ID
     * @return 主用户组DTO
     */
    ClusterGroupDTO queryMainGroup(Integer userId);

    /**
     * 查询指定用户的其他用户组列表
     *
     * @param userId 用户ID
     * @return 其他用户组DTO列表
     */
    List<ClusterGroupDTO> listOtherGroups(Integer userId);

    /**
     * 查询指定组的所有用户列表
     *
     * @param groupId 组ID
     * @return 用户DTO列表
     */
    List<ClusterUserDTO> listClusterUsers(Integer groupId);

    /**
     * 根据ID获取用户组关联DTO
     *
     * @param id 关联ID
     * @return 用户组关联DTO
     */
    ClusterUserGroupDTO getByIdAsDto(Integer id);

    /**
     * 保存用户组关联
     *
     * @param dto 用户组关联DTO
     */
    void saveUserGroup(ClusterUserGroupDTO dto);

    /**
     * 更新用户组关联
     *
     * @param dto 用户组关联DTO
     */
    void updateUserGroup(ClusterUserGroupDTO dto);
}
