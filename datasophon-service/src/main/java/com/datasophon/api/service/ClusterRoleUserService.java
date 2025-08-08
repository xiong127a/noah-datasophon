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

import com.datasophon.common.dto.ClusterRoleUserDTO;
import com.datasophon.common.dto.UserInfoDTO;
import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群角色用户服务
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ClusterRoleUserService extends IService<ClusterRoleUserEntity> {

    /**
     * 判断用户是否为集群管理员
     *
     * @param userId    用户ID
     * @param clusterId 集群ID
     * @return 是否为管理员
     */
    boolean isClusterManager(Integer userId, Integer clusterId);

    /**
     * 保存集群管理员
     *
     * @param clusterId 集群ID
     * @param userIds   用户ID列表（逗号分隔）
     * @return 是否保存成功
     */
    boolean saveClusterManager(Integer clusterId, String userIds);

    /**
     * 获取集群下所有管理员
     *
     * @param clusterId 集群ID
     * @return 管理员用户列表
     */
    List<UserInfoDTO> getAllClusterManagerByClusterId(Integer clusterId);

    /**
     * 获取所有集群角色用户
     *
     * @return 集群角色用户列表
     */
    List<ClusterRoleUserDTO> getAllClusterRoleUsers();
}