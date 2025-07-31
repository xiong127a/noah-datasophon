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

import cn.hutool.core.util.StrUtil;
import com.datasophon.kubernetes.model.K8sNodeInfo;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.enums.HostState;
import com.datasophon.dao.enums.MANAGED;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * K8S节点信息到集群主机实体的转换器
 * 负责将Kubernetes领域模型转换为DAO实体
 */
@Component
public class K8sToClusterHostConverter {

    /**
     * 将K8sNodeInfo转换为ClusterHostDO
     *
     * @param k8sNodeInfo K8S节点信息
     * @param clusterId   集群ID
     * @return 集群主机实体
     */
    public ClusterHostDO convertToClusterHost(K8sNodeInfo k8sNodeInfo, Integer clusterId) {
        if (k8sNodeInfo == null) {
            return null;
        }

        return ClusterHostDO.builder()
                .clusterId(clusterId)
                .ip(k8sNodeInfo.getIp())
                .hostname(k8sNodeInfo.getHostname())
                .coreNum(k8sNodeInfo.getCoreNum())
                .totalMem(k8sNodeInfo.getTotalMem())
                .usedMem(k8sNodeInfo.getUsedMem())
                .totalDisk(k8sNodeInfo.getTotalDisk())
                .usedDisk(k8sNodeInfo.getUsedDisk())
                .cpuArchitecture(k8sNodeInfo.getCpuArchitecture())
                .createTime(k8sNodeInfo.getCreateTime())
                .hostState(convertToHostState(k8sNodeInfo.getStatus()))
                .managed(MANAGED.YES)
                .rack("/default-rack") // 默认机架
                .nodeLabel("default") // 默认节点标签
                .build();
    }

    /**
     * 批量转换K8sNodeInfo列表为ClusterHostDO列表
     *
     * @param k8sNodeInfoList K8S节点信息列表
     * @param clusterId       集群ID
     * @return 集群主机实体列表
     */
    public List<ClusterHostDO> convertToClusterHostList(List<K8sNodeInfo> k8sNodeInfoList, Integer clusterId) {
        if (k8sNodeInfoList == null) {
            return null;
        }

        return k8sNodeInfoList.stream()
                .map(k8sNodeInfo -> convertToClusterHost(k8sNodeInfo, clusterId))
                .collect(Collectors.toList());
    }

    /**
     * 将K8S节点状态转换为主机状态枚举
     *
     * @param k8sStatus K8S节点状态
     * @return 主机状态枚举
     */
    private HostState convertToHostState(String k8sStatus) {
        if (k8sStatus == null) {
            return HostState.OFFLINE;
        }
        if (StrUtil.equalsAnyIgnoreCase(k8sStatus, "ready")) {
            return HostState.RUNNING;
        }
        return HostState.OFFLINE;
    }
}