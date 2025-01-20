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

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;

import java.util.List;

/**
 * The `ClusterInfoService` interface provides methods to manage and retrieve information about clusters.
 * It extends the `IService` interface, which is typically used for common CRUD operations on entities.
 * This service is designed to handle operations related to cluster information, such as retrieving,
 * saving, updating, and deleting cluster details, as well as managing cluster states and configurations.
 *
 * <p>Key functionalities include:
 * <ul>
 *   <li>Retrieving cluster information by cluster code or framework code.</li>
 *   <li>Saving and updating cluster information.</li>
 *   <li>Fetching lists of clusters, including running clusters.</li>
 *   <li>Updating the state of a cluster.</li>
 *   <li>Deleting clusters by their IDs.</li>
 *   <li>Retrieving Kubernetes configuration and Kerberos information for a cluster.</li>
 * </ul>
 *
 * <p>This interface is intended to be implemented by a service class that interacts with a data source
 * (e.g., a database) to perform the necessary operations on cluster information.
 *
 * @see IService
 * @see ClusterInfoEntity
 */
public interface ClusterInfoService extends IService<ClusterInfoEntity> {

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
     * Retrieves Kerberos-related information for a specified service role.
     *
     * This method is used to fetch Kerberos configuration or authentication details
     * associated with a specific service role within the cluster. The service role name
     * is used to identify the relevant Kerberos information.
     *
     * @param serviceRoleName The name of the service role for which Kerberos information is requested.
     *                        This should be a non-null string representing the role in the cluster.
     * @return A string containing the Kerberos information for the specified service role.
     *         This could include details such as the Kerberos principal, keytab location, or other
     *         relevant configuration data. Returns null if no information is found for the given role.
     */
    String getKerberosInfo(String serviceRoleName);
}
