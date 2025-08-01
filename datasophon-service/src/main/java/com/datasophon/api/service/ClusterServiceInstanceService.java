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

import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.common.model.ConnectionInfo;

import java.util.List;
import java.util.Map;

/**
 * 集群服务表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public interface ClusterServiceInstanceService {

    ClusterServiceInstanceEntity getServiceInstanceByClusterIdAndServiceName(Integer clusterId, String parentName);

    String getServiceConfigByClusterIdAndServiceName(Integer id, String node);

    List<ClusterServiceInstanceEntity> listAll(Integer clusterId);

    String downloadClientConfig(Integer clusterId, String serviceName);

    List<FrameServiceRoleEntity> getServiceRoleType(Integer serviceInstanceId);

    Map<String, List<Map<String, Object>>> configVersionCompare(Integer serviceInstanceId, Integer roleGroupId,
            Boolean showOnlyDifferences);

    boolean delServiceInstance(Integer serviceInstanceId);

    List<ClusterServiceInstanceEntity> listRunningServiceInstance(Integer clusterId);

    boolean hasRunningRoleInstance(Integer serviceInstanceId);

    Boolean hasRoleInstance(Integer clusterId, String serviceName);

    /**
     * 获取服务连接信息
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 连接信息
     */
    ConnectionInfo getConnectionInfo(Integer serviceInstanceId);

    // 标准CRUD方法
    ClusterServiceInstanceEntity getById(Integer id);

    ClusterServiceInstanceEntity save(ClusterServiceInstanceEntity entity);

    ClusterServiceInstanceEntity updateById(ClusterServiceInstanceEntity entity);

    boolean removeByIds(List<Integer> ids);

    List<ClusterServiceInstanceEntity> getAllServiceInstances();
}
