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
import com.datasophon.dao.entity.ClusterInfoEntity;

import java.util.List;

/**
 * The `ClusterInfoService` interface provides methods to manage and retrieve
 * information about clusters.
 * It extends the `IService` interface, which is typically used for common CRUD
 * operations on entities.
 * This service is designed to handle operations related to cluster information,
 * such as retrieving,
 * saving, updating, and deleting cluster details, as well as managing cluster
 * states and configurations.
 *
 * <p>
 * Key functionalities include:
 * <ul>
 * <li>Retrieving cluster information by cluster code or framework code.</li>
 * <li>Saving and updating cluster information.</li>
 * <li>Fetching lists of clusters, including running clusters.</li>
 * <li>Updating the state of a cluster.</li>
 * <li>Deleting clusters by their IDs.</li>
 * <li>Retrieving Kubernetes configuration and Kerberos information for a
 * cluster.</li>
 * </ul>
 *
 * <p>
 * This interface is intended to be implemented by a service class that
 * interacts with a data source
 * (e.g., a database) to perform the necessary operations on cluster
 * information.
 *
 * @see IService
 * @see ClusterInfoEntity
 */
public interface ClusterInfoService {

    ClusterInfoEntity getClusterByClusterCode(String clusterCode);

    Result saveCluster(ClusterInfoEntity clusterInf);

    Result getClusterList();

    Result runningClusterList();

    Result updateClusterState(Integer clusterId, Integer clusterState);

    List<ClusterInfoEntity> getClusterByFrameCode(String frameCode);

    Result updateCluster(ClusterInfoEntity clusterInfo);

    void deleteCluster(List<Integer> asList);

    String getKubeConfigByClusterId(Integer clusterId);

    /**
     * Retrieves metrics information for all service roles in the cluster.
     * <p>
     * This method fetches the count of running instances for each service role
     * in the cluster. It returns a JSON string containing the service role names
     * as keys and their respective instance counts as values.
     *
     * @return A JSON string containing metrics about service roles and their
     *         instance counts.
     *         For example: {"HDFS_NAMENODE": 2, "HDFS_DATANODE": 10, ...}
     */
    String getServiceRoleMetrics();

    /**
     * Retrieves detailed information about a cluster by its ID.
     *
     * @param clusterId The ID of the cluster to retrieve information for.
     * @return A Result object containing the cluster information.
     */
    Result getClusterById(Integer clusterId);

    /**
     * Retrieves the list of Kubernetes namespaces for a cluster.
     *
     * @param kubeConfig The Kubernetes configuration content.
     * @return A Result object containing the list of namespaces.
     */
    Result getKubernetesNamespaces(String kubeConfig);

    /**
     * 更新集群Kubernetes配置
     *
     * @param clusterId       集群ID
     * @param kubeConfig      Kubernetes配置内容
     * @param namespace       选择的命名空间
     * @param customNamespace 自定义命名空间（如果选择创建新的命名空间）
     * @return 更新结果
     */
    Result updateClusterKubeConfig(Integer clusterId, String kubeConfig, String namespace, String customNamespace);

    String getKubernetesNamespace(Integer clusterId);
}
