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

import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterGroup;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群组服务
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public interface ClusterGroupService extends IService<ClusterGroup> {

    /**
     * 保存集群组
     * 
     * @param clusterId 集群ID
     * @param groupName 组名
     * @return 保存的集群组
     */
    ClusterGroup saveClusterGroup(Integer clusterId, String groupName);

    /**
     * 刷新用户组到主机
     * 
     * @param clusterId 集群ID
     */
    void refreshUserGroupToHost(Integer clusterId);

    /**
     * 删除用户组
     * 
     * @param id 组ID
     * @return 是否删除成功
     */
    boolean deleteUserGroup(Integer id);

    /**
     * 在Kubernetes上删除用户组
     * 
     * @param id 组ID
     * @return 是否删除成功
     */
    boolean deleteUserGroupOnKubernetes(Integer id);

    /**
     * 分页查询集群组
     * 
     * @param groupName 组名
     * @param clusterId 集群ID
     * @param page      页码
     * @param pageSize  页大小
     * @return 分页结果
     */
    PageResult<ClusterGroup> listPage(String groupName, Integer clusterId, Integer page, Integer pageSize);

    /**
     * 查询集群下所有用户组
     * 
     * @param clusterId 集群ID
     * @return 用户组列表
     */
    List<ClusterGroup> listAllUserGroup(Integer clusterId);

    /**
     * 在主机上创建Unix组
     * 
     * @param hostname  主机名
     * @param groupName 组名
     */
    void createUnixGroupOnHost(String hostname, String groupName);

    /**
     * 在Kubernetes上保存集群组
     * 
     * @param clusterId 集群ID
     * @param groupName 组名
     * @return 保存的集群组
     */
    ClusterGroup saveClusterGroupOnKubernetes(Integer clusterId, String groupName);
}
