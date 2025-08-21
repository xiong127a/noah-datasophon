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

import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.ServiceRoleState;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群服务角色实例服务
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ClusterServiceRoleInstanceService extends IService<ClusterServiceRoleInstanceEntity> {

    /**
     * 根据主机名和集群ID获取停止的服务角色列表
     */
    List<ClusterServiceRoleInstanceDTO> listStoppedServiceRoleListByHostnameAndClusterId(String hostname,
            Long clusterId);

    /**
     * 根据主机名和集群ID获取服务角色列表
     */
    List<ClusterServiceRoleInstanceDTO> getServiceRoleListByHostnameAndClusterId(String hostname, Long clusterId);

    /**
     * 根据服务ID和角色状态获取服务角色实例列表
     */
    List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByServiceIdAndRoleState(Long id,
            ServiceRoleState stop);

    /**
     * 获取单个服务角色
     */
    ClusterServiceRoleInstanceDTO getOneServiceRole(String serviceRoleName, String hostname, Long clusterId);

    /**
     * 分页列表查询
     */
    PageResult<ClusterServiceRoleInstanceDTO> listAll(Long serviceInstanceId, String hostname,
            Integer serviceRoleState, String serviceRoleName,
            Long roleGroupId, Integer page, Integer pageSize);

    /**
     * 获取日志
     */
    String getLog(Integer serviceRoleInstanceId) throws Exception;

    /**
     * 根据服务ID获取服务角色实例列表
     */
    List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByServiceId(Long id);

    /**
     * 根据集群ID获取服务角色实例列表
     */
    List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByClusterId(Long clusterId);

    /**
     * 删除服务角色
     */
    void deleteServiceRole(List<String> idList);

    /**
     * 根据集群ID和角色名称获取服务角色实例列表
     */
    List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByClusterIdAndRoleName(Long clusterId,
            String roleName);

    /**
     * 获取正在运行的服务角色实例列表
     */
    List<ClusterServiceRoleInstanceDTO> getRunningServiceRoleInstanceListByServiceId(Long serviceInstanceId);

    /**
     * 重启过时服务
     */
    void restartObsoleteService(Long roleGroupId);

    /**
     * 退役节点
     */
    String decommissionNode(String serviceRoleInstanceIds, String serviceName) throws Exception;

    /**
     * 更新为需要重启状态
     */
    void updateToNeedRestart(Long roleGroupId);

    /**
     * 更新为需要重启状态（指定服务角色名称）
     */
    void updateToNeedRestart(Long roleGroupId, String serviceRoleName);

    /**
     * 根据主机名更新为需要重启状态
     */
    void updateToNeedRestartByHost(String hostName);

    /**
     * 获取过时服务
     */
    List<ClusterServiceRoleInstanceDTO> getObsoleteService(Long id);

    /**
     * 获取主机上停止的角色实例
     */
    List<ClusterServiceRoleInstanceDTO> getStoppedRoleInstanceOnHost(Long clusterId, String hostname,
            ServiceRoleState state);

    /**
     * 移除角色实例
     */
    void reomveRoleInstance(Long serviceInstanceId);

    /**
     * 获取KAdmin角色实例
     */
    ClusterServiceRoleInstanceDTO getKAdminRoleIns(Long clusterId);

    /**
     * 根据服务角色名称列表查询
     */
    List<ClusterServiceRoleInstanceDTO> listServiceRoleByName(String serviceRoleName);

    /**
     * 根据集群ID和服务角色名称查询
     */
    ClusterServiceRoleInstanceDTO listServiceRoleByNameAndClusterId(Long clusterId, String serviceRoleName);

    /**
     * 根据主机名和服务角色名称获取服务角色实例
     */
    ClusterServiceRoleInstanceDTO getServiceRoleInsByHostAndName(String hostName, String serviceRoleName);

    /**
     * 根据主机名和服务名称列出角色实例
     */
    List<ClusterServiceRoleInstanceDTO> listRoleIns(String hostname, String serviceName);

    /**
     * 根据集群ID、服务实例ID和角色名称获取服务角色实例列表
     */
    List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByServiceInstanceIdAndRoleName(Long clusterId,
            Long serviceInstanceId, String roleName);

    /**
     * 更新服务角色实例状态
     *
     * @param serviceRoleInstanceId 服务角色实例ID
     * @param serviceRoleState 新的服务角色状态
     */
    void updateServiceRoleInstanceState(Long serviceRoleInstanceId, ServiceRoleState serviceRoleState);

    /**
     * 根据服务名列表获取服务角色实例
     *
     * @param serviceNames 服务名列表
     * @return 服务角色实例DTO列表
     */
    List<ClusterServiceRoleInstanceDTO> getServiceRolesByNames(List<String> serviceNames);
}
