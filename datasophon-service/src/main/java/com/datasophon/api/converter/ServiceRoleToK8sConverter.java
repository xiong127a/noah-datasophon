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

package com.datasophon.api.converter;

import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.kubernetes.model.K8sServiceRoleInfo;
import org.springframework.stereotype.Component;

/**
 * 服务角色实体到K8S服务角色信息的转换器
 * 负责将DAO实体转换为Kubernetes模块专用的数据模型
 */
@Component
public class ServiceRoleToK8sConverter {

    /**
     * 将ClusterServiceRoleInstanceEntity转换为K8sServiceRoleInfo
     * 
     * @param roleInstanceEntity 服务角色实例实体
     * @param clusterInfo        集群信息（用于获取namespace）
     * @return K8S服务角色信息
     */
    public K8sServiceRoleInfo convertToK8sServiceRoleInfo(
            ClusterServiceRoleInstanceEntity roleInstanceEntity,
            ClusterInfoEntity clusterInfo) {
        if (roleInstanceEntity == null) {
            return null;
        }

        return K8sServiceRoleInfo.builder()
                .clusterId(roleInstanceEntity.getClusterId())
                .serviceName(roleInstanceEntity.getServiceName())
                .serviceRoleName(roleInstanceEntity.getServiceRoleName())
                .hostname(roleInstanceEntity.getHostname())
                .namespace(clusterInfo != null ? clusterInfo.getNamespace() : "default")
                .build();
    }

    /**
     * 根据基本信息创建K8sServiceRoleInfo
     * 
     * @param clusterId       集群ID
     * @param serviceName     服务名称
     * @param serviceRoleName 服务角色名称
     * @param hostname        主机名
     * @param namespace       命名空间
     * @return K8S服务角色信息
     */
    public K8sServiceRoleInfo createK8sServiceRoleInfo(
            Long clusterId,
            String serviceName,
            String serviceRoleName,
            String hostname,
            String namespace) {
        return K8sServiceRoleInfo.builder()
                .clusterId(clusterId)
                .serviceName(serviceName)
                .serviceRoleName(serviceRoleName)
                .hostname(hostname)
                .namespace(namespace)
                .build();
    }
}