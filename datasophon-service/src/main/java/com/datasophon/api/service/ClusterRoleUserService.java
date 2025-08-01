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

import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.datasophon.dao.entity.UserInfoEntity;

import java.util.List;

/**
 * 集群角色用户中间表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public interface ClusterRoleUserService {

    boolean isClusterManager(Integer userId, String clusterId);

    boolean saveClusterManager(Integer clusterId, String userIds);

    List<UserInfoEntity> getAllClusterManagerByClusterId(Integer clusterId);

    // 标准CRUD方法
    ClusterRoleUserEntity getById(Integer id);

    ClusterRoleUserEntity save(ClusterRoleUserEntity entity);

    ClusterRoleUserEntity updateById(ClusterRoleUserEntity entity);

    boolean removeByIds(List<Integer> ids);

    List<ClusterRoleUserEntity> getAllClusterRoleUsers();
}