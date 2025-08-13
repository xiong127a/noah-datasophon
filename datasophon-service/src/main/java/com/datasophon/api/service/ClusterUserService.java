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

import com.datasophon.common.dto.ClusterUserDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterUserEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群用户服务
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ClusterUserService extends IService<ClusterUserEntity> {

    /**
     * 创建集群用户
     *
     * @param clusterId     集群ID
     * @param username      用户名
     * @param mainGroupId   主组ID
     * @param otherGroupIds 其他组ID
     * @return 创建的用户
     */
    ClusterUserDTO createClusterUser(Long clusterId, String username, Integer mainGroupId, String otherGroupIds);

    /**
     * 在Kubernetes上创建集群用户
     *
     * @param clusterId     集群ID
     * @param username      用户名
     * @param mainGroupId   主组ID
     * @param otherGroupIds 其他组ID
     * @return 创建的用户
     */
    ClusterUserDTO createClusterUserOnKubernetes(Long clusterId, String username, Integer mainGroupId,
            String otherGroupIds);

    /**
     * 分页查询集群用户
     *
     * @param clusterId 集群ID
     * @param username  用户名
     * @param page      页码
     * @param pageSize  页大小
     * @return 分页结果
     */
    PageResult<ClusterUserDTO> listPagedUsers(Long clusterId, String username, Integer page, Integer pageSize);

    /**
     * 删除集群用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    boolean deleteClusterUser(Integer id);

    /**
     * 在Kubernetes上删除集群用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    boolean deleteClusterUserOnKubernetes(Integer id);

    /**
     * 查询集群下所有用户
     *
     * @param clusterId 集群ID
     * @return 用户列表
     */
    List<ClusterUserDTO> listAllUser(Long clusterId);

    /**
     * 在主机上创建Unix用户
     *
     * @param clusterUserDTO 集群用户
     * @param hostname       主机名
     */
    void createUnixUserOnHost(ClusterUserDTO clusterUserDTO, String hostname);

    /**
     * 根据用户ID列表获取用户名列表
     *
     * @param userIds 用户ID列表
     * @return 用户名列表
     */
    List<String> getUsernamesByIds(List<Integer> userIds);
}
