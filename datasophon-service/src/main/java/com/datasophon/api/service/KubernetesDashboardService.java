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

import com.datasophon.api.vo.Result;

/**
 * Kubernetes仪表盘服务接口
 * 提供Kubernetes资源查询方法
 */
public interface KubernetesDashboardService {

    /**
     * 获取Kubernetes命名空间列表
     * 
     * @param clusterId 集群ID
     * @return 命名空间列表
     */
    Result getNamespaces(Integer clusterId);

    /**
     * 获取Deployments列表（带服务ID，分页）
     * 
     * @param clusterId 集群ID
     * @param serviceId 服务ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return Deployments列表（包含分页信息）
     */
    Result getDeployments(Integer clusterId, Integer serviceId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取Pods列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param serviceId 服务ID
     * @param namespace 命名空间（null或"all"表示所有命名空间）
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return 包含Pod列表、状态统计等的详细信息
     */
    Result getPods(Integer clusterId, Integer serviceId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取Services列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return Services列表（包含分页信息）
     */
    Result getServices(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取ConfigMaps列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return ConfigMaps列表（包含分页信息）
     */
    Result getConfigMaps(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取Secrets列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return Secrets列表（包含分页信息）
     */
    Result getSecrets(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取PersistentVolumes列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return PersistentVolumes列表（包含分页信息）
     */
    Result getPersistentVolumes(Integer clusterId, Integer pageNum, Integer pageSize);

    /**
     * 获取PersistentVolumeClaims列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return PersistentVolumeClaims列表（包含分页信息）
     */
    Result getPersistentVolumeClaims(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取StorageClasses列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return StorageClasses列表（包含分页信息）
     */
    Result getStorageClasses(Integer clusterId, Integer pageNum, Integer pageSize);

    /**
     * 获取Ingresses列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return Ingresses列表（包含分页信息）
     */
    Result getIngresses(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取IngressClasses列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return IngressClasses列表
     */
    Result getIngressClasses(Integer clusterId, Integer pageNum, Integer pageSize);

    /**
     * 获取DaemonSets列表（带服务ID）
     * 
     * @param clusterId 集群ID
     * @param serviceId 服务ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return DaemonSets列表（包含分页信息）
     */
    Result getDaemonSets(Integer clusterId, Integer serviceId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取StatefulSets列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return StatefulSets列表（包含分页信息）
     */
    Result getStatefulSets(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取ReplicaSets列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return ReplicaSets列表（包含分页信息）
     */
    Result getReplicaSets(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取ReplicationControllers列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return ReplicationControllers列表（包含分页信息）
     */
    Result getReplicationControllers(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取Jobs列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return Jobs列表（包含分页信息）
     */
    Result getJobs(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取CronJobs列表（带分页）
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return CronJobs列表（包含分页信息）
     */
    Result getCronJobs(Integer clusterId, String namespace, Integer pageNum, Integer pageSize);

    /**
     * 获取Deployment详情
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param name      Deployment名称
     * @return Deployment详情
     */
    Result getDeploymentDetail(Integer clusterId, String namespace, String name);

    /**
     * 获取资源相关事件
     * 
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param kind      资源类型
     * @param name      资源名称
     * @return 事件列表
     */
    Result getResourceEvents(Integer clusterId, String namespace, String kind, String name);

    /**
     * 一次性获取所有Kubernetes资源统计数据
     * 该方法旨在提高性能，通过一次客户端连接获取所有资源数量
     *
     * @param clusterId 集群ID
     * @param serviceId 服务ID（可选）
     * @param namespace 命名空间（可选）
     * @return 包含所有资源数量的统计结果
     */
    Result getResourceStats(Integer clusterId, Integer serviceId, String namespace);
}