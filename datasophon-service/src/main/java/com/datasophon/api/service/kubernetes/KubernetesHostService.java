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

package com.datasophon.api.service.kubernetes;

import com.datasophon.dao.entity.ClusterHostDO;

import java.util.List;
import java.util.Map;

/**
 * Kubernetes 主机管理服务接口
 * 专门处理K8S模式下的主机发现、管理和导入功能
 */
public interface KubernetesHostService {

    /**
     * 发现K8S集群中的主机
     * 直接从K8S API获取节点信息，包含完整的硬件详情
     *
     * @param kubeConfigContent K8S配置内容
     * @param namespace 命名空间
     * @param clusterId 集群ID
     * @return 发现的主机列表
     */
    List<ClusterHostDO> discoverHosts(String kubeConfigContent, String namespace, Integer clusterId);

    /**
     * 获取已发现的K8S主机列表
     * 用于Step2界面显示和主机校验
     *
     * @param clusterId 集群ID
     * @return 主机列表
     */
    List<ClusterHostDO> getDiscoveredHosts(Integer clusterId);

    /**
     * 导入选定的K8S主机到集群
     * 用户在Step2选择主机后调用此接口
     *
     * @param selectedHosts 选定的主机列表
     * @param clusterId 集群ID
     */
    void importHosts(List<ClusterHostDO> selectedHosts, Integer clusterId);

    /**
     * 刷新K8S主机硬件信息
     * 重新获取最新的硬件状态
     *
     * @param kubeConfigContent K8S配置内容
     * @param namespace 命名空间
     * @param clusterId 集群ID
     * @return 刷新后的主机列表
     */
    List<ClusterHostDO> refreshHosts(String kubeConfigContent, String namespace, Integer clusterId);

    /**
     * 检查K8S连接状态
     *
     * @param kubeConfigContent K8S配置内容
     * @return 连接信息
     */
    Map<String, Object> checkConnection(String kubeConfigContent);

    /**
     * 从K8S API获取节点的硬件信息
     *
     * @param kubeConfigContent K8S配置内容
     * @param namespace 命名空间
     * @return 节点硬件信息列表
     */
    List<ClusterHostDO> fetchNodesFromKubernetes(String kubeConfigContent, String namespace);

    /**
     * 清理K8S主机相关的临时数据
     *
     * @param clusterId 集群ID
     */
    void cleanupKubernetesHostData(Integer clusterId);
}