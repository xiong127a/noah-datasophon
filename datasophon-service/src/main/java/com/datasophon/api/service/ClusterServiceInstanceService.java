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

import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.enums.ServiceState;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.common.model.ConnectionInfo;
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 集群服务实例服务
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ClusterServiceInstanceService extends IService<ClusterServiceInstanceEntity> {

    /**
     * 根据集群ID和服务名称获取服务实例
     *
     * @param clusterId   集群ID
     * @param serviceName 服务名称
     * @return 服务实例
     */
    ClusterServiceInstanceDTO getServiceInstanceByClusterIdAndServiceName(Long clusterId, String serviceName);

    /**
     * 根据集群ID和服务名称获取服务配置
     *
     * @param id   集群ID
     * @param node 节点
     * @return 配置字符串
     */
    String getServiceConfigByClusterIdAndServiceName(Long id, String node);

    /**
     * 获取集群下所有服务实例
     *
     * @param clusterId 集群ID
     * @return 服务实例列表
     */
    List<ClusterServiceInstanceDTO> listAll(Long clusterId);

    /**
     * 下载客户端配置
     *
     * @param clusterId   集群ID
     * @param serviceName 服务名称
     * @return 配置文件路径
     */
    String downloadClientConfig(Long clusterId, String serviceName);

    /**
     * 获取服务角色类型
     *
     * @param serviceInstanceId 服务实例ID
     * @return 角色类型列表
     */
    List<FrameServiceRoleEntity> getServiceRoleType(Integer serviceInstanceId);

    /**
     * 配置版本比较
     *
     * @param serviceInstanceId   服务实例ID
     * @param roleGroupId         角色组ID
     * @param showOnlyDifferences 是否仅显示差异
     * @return 比较结果
     */
    Map<String, List<Map<String, Object>>> configVersionCompare(Integer serviceInstanceId, Integer roleGroupId,
            Boolean showOnlyDifferences);

    /**
     * 删除服务实例
     *
     * @param serviceInstanceId 服务实例ID
     * @return 是否删除成功
     */
    boolean delServiceInstance(Integer serviceInstanceId);

    /**
     * 获取集群下正在运行的服务实例
     *
     * @param clusterId 集群ID
     * @return 运行中的服务实例列表
     */
    List<ClusterServiceInstanceDTO> listRunningServiceInstance(Long clusterId);

    /**
     * 判断服务实例是否有正在运行的角色实例
     *
     * @param serviceInstanceId 服务实例ID
     * @return 是否有运行中的角色实例
     */
    boolean hasRunningRoleInstance(Integer serviceInstanceId);

    /**
     * 判断集群中是否存在指定服务的角色实例
     *
     * @param clusterId   集群ID
     * @param serviceName 服务名称
     * @return 是否存在角色实例
     */
    Boolean hasRoleInstance(Long clusterId, String serviceName);

    /**
     * 获取服务连接信息
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 连接信息
     */
    ConnectionInfo getConnectionInfo(Integer serviceInstanceId);

    /**
     * 获取所有服务实例
     *
     * @return 所有服务实例列表
     */
    List<ClusterServiceInstanceDTO> getAllServiceInstances();

    /**
     * 更新服务实例状态
     *
     * @param serviceInstanceId 服务实例ID
     * @param serviceState 新的服务状态
     */
    void updateServiceInstanceState(Integer serviceInstanceId, ServiceState serviceState);

    /**
     * 检查是否存在使用指定框架服务的集群服务实例
     *
     * @param frameServiceId 框架服务ID
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByFrameServiceId(Integer frameServiceId);
}
