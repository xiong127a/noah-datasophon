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

import com.datasophon.common.dto.K8sNamespaceDTO;
import com.datasophon.common.dto.K8sResourceStatsDTO;
import com.datasophon.common.dto.KubernetesResourceDTO;
import com.datasophon.common.model.PageResult;

import java.util.List;
import java.util.Map;

/**
 * Kubernetes仪表盘服务接口
 * 提供Kubernetes资源查询方法
 * 按照架构重构规范，Service层不返回Result，抛出业务异常
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface KubernetesDashboardService {

        /**
         * 获取Kubernetes命名空间列表
         * 
         * @param clusterId 集群ID
         * @return 命名空间DTO列表
         */
        List<K8sNamespaceDTO> getNamespaces(Long clusterId);

        /**
         * 一次性获取所有Kubernetes资源统计数据
         * 该方法旨在提高性能，通过一次客户端连接获取所有资源数量
         *
         * @param clusterId 集群ID
         * @param serviceId 服务ID（可选）
         * @param namespace 命名空间（可选）
         * @return K8S资源统计DTO
         */
        K8sResourceStatsDTO getResourceStats(Long clusterId, Long serviceId, String namespace);

        /**
         * 获取Deployments列表
         * 
         * @param clusterId 集群ID
         * @param serviceId 服务ID
         * @param namespace 命名空间
         * @param pageNum   当前页码
         * @param pageSize  每页大小
         * @return Kubernetes资源分页结果
         */
        PageResult<KubernetesResourceDTO> getDeployments(Long clusterId, Long serviceId, String namespace,
                        Integer pageNum,
                        Integer pageSize);

        /**
         * 获取Pods列表
         * 
         * @param clusterId 集群ID
         * @param serviceId 服务ID
         * @param namespace 命名空间
         * @param searchTerm 搜索关键词（支持Pod名称、节点名称、标签搜索）
         * @param statusFilter 状态筛选（Running、Pending、Failed、Succeeded等）
         * @param pageNum   当前页码
         * @param pageSize  每页大小
         * @return Kubernetes资源分页结果
         */
        PageResult<KubernetesResourceDTO> getPods(Long clusterId, Long serviceId, String namespace,
                        String searchTerm, String statusFilter,
                        Integer pageNum, Integer pageSize);

        /**
         * 获取Services列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getServices(Long clusterId, String namespace, Integer pageNum,
                        Integer pageSize);

        /**
         * 获取ConfigMaps列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getConfigMaps(Long clusterId, String namespace, Integer pageNum,
                        Integer pageSize);

        /**
         * 获取Secrets列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getSecrets(Long clusterId, String namespace, Integer pageNum,
                        Integer pageSize);

        /**
         * 获取PersistentVolumes列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getPersistentVolumes(Long clusterId, Integer pageNum, Integer pageSize);

        /**
         * 获取PersistentVolumeClaims列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getPersistentVolumeClaims(Long clusterId, String namespace,
                        Integer pageNum,
                        Integer pageSize);

        /**
         * 获取StorageClasses列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getStorageClasses(Long clusterId, Integer pageNum, Integer pageSize);

        /**
         * 获取Ingresses列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getIngresses(Long clusterId, String namespace, Integer pageNum,
                        Integer pageSize);

        /**
         * 获取IngressClasses列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getIngressClasses(Long clusterId, Integer pageNum, Integer pageSize);

        /**
         * 获取DaemonSets列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getDaemonSets(Long clusterId, Long serviceId, String namespace,
                        Integer pageNum,
                        Integer pageSize);

        /**
         * 获取StatefulSets列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getStatefulSets(Long clusterId, Long serviceId, String namespace, Integer pageNum,
                        Integer pageSize);

        /**
         * 获取ReplicaSets列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getReplicaSets(Long clusterId, Long serviceId, String namespace, Integer pageNum,
                        Integer pageSize);

        /**
         * 获取ReplicationControllers列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getReplicationControllers(Long clusterId, String namespace,
                        Integer pageNum,
                        Integer pageSize);

        /**
         * 获取Jobs列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getJobs(Long clusterId, Long serviceId, String namespace, Integer pageNum,
                        Integer pageSize);

        /**
         * 获取CronJobs列表（返回通用对象，暂时保持兼容）
         */
        PageResult<KubernetesResourceDTO> getCronJobs(Long clusterId, Long serviceId, String namespace, Integer pageNum,
                        Integer pageSize);

        /**
         * 获取Deployment详情（返回通用对象，暂时保持兼容）
         */
        KubernetesResourceDTO getDeploymentDetail(Long clusterId, String namespace, String name);

        /**
         * 获取资源相关事件（返回通用对象，暂时保持兼容）
         */
        List<Map<String, Object>> getResourceEvents(Long clusterId, String namespace, String kind, String name);
}