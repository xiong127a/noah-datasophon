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

import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.common.model.kubernetes.KubernetesNamespaceDto;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 集群信息服务接口
 * 继承IService提供基础CRUD操作，返回DTO进行数据传输
 * 按照架构重构规范，Service层不返回Result，抛出业务异常
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ClusterInfoService extends IService<ClusterInfoEntity> {

    /**
     * 根据集群代码获取集群信息
     */
    ClusterInfoDTO getClusterByClusterCode(String clusterCode);

    /**
     * 保存集群信息
     */
    ClusterInfoDTO saveCluster(ClusterInfoDTO clusterInfo);

    /**
     * 获取集群列表
     */
    List<ClusterInfoDTO> getClusterList();

    /**
     * 获取运行中的集群列表
     */
    List<ClusterInfoDTO> runningClusterList();

    /**
     * 更新集群状态
     */
    boolean updateClusterState(Long clusterId, Integer clusterState);

    /**
     * 根据框架代码获取集群列表
     */
    List<ClusterInfoDTO> getClusterByFrameCode(String frameCode);

    /**
     * 更新集群信息
     */
    ClusterInfoDTO updateCluster(ClusterInfoDTO clusterInfo);

    /**
     * 删除集群
     */
    void deleteCluster(List<Long> ids);

    /**
     * 根据集群ID获取Kubernetes配置
     */
    String getKubeConfigByClusterId(Long clusterId);

    /**
     * 获取服务角色指标信息
     * 返回Prometheus格式的服务角色实例统计数据
     */
    String getServiceRoleMetrics();

    /**
     * 根据ID获取集群详细信息
     */
    ClusterInfoDTO getClusterById(Long clusterId);

    /**
     * 获取Kubernetes命名空间列表
     */
    KubernetesNamespaceDto getKubernetesNamespaces(String kubeConfig);

    /**
     * 更新集群Kubernetes配置
     */
    String updateClusterKubeConfig(Long clusterId, String kubeConfig, String namespace, String customNamespace);

    /**
     * 获取集群的Kubernetes命名空间
     */
    String getKubernetesNamespace(Long clusterId);

    /**
     * 获取所有集群ID和类型的映射
     *
     * @return 集群ID和类型映射
     */
    Map<Long, ClusterType> getAllClusterIdAndType();
}
