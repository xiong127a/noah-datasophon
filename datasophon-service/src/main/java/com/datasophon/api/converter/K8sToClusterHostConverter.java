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
import cn.hutool.json.JSONUtil;
import com.datasophon.common.dto.HostInfoDTO;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.kubernetes.model.K8sNodeInfo;
import com.datasophon.common.enums.HostState;
import com.datasophon.common.enums.ManagementStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * K8S节点信息到集群主机实体的转换器
 * 负责将Kubernetes领域模型转换为DAO实体
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-31
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
    public ClusterHostEntity convertToClusterHost(K8sNodeInfo k8sNodeInfo, Long clusterId) {
        if (k8sNodeInfo == null) {
            return null;
        }

        return ClusterHostEntity.builder()
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
                .managementStatus(ManagementStatus.CONFIGURING) // K8S新发现的节点初始状态为未受管
                .rack("/default-rack") // 默认机架
                .nodeLabel(null) // 主机标签字段，由用户自定义
                // K8s节点扩展信息JSON
                .k8sNodeInfo(buildK8sNodeInfoJson(k8sNodeInfo))
                .build();
    }

    /**
     * 批量转换K8sNodeInfo列表为ClusterHostDO列表
     *
     * @param k8sNodeInfoList K8S节点信息列表
     * @param clusterId       集群ID
     * @return 集群主机实体列表
     */
    public List<ClusterHostEntity> convertToClusterHostList(List<K8sNodeInfo> k8sNodeInfoList, Long clusterId) {
        if (k8sNodeInfoList == null) {
            return null;
        }

        return k8sNodeInfoList.stream()
                .map(k8sNodeInfo -> convertToClusterHost(k8sNodeInfo, clusterId))
                .collect(Collectors.toList());
    }

    /**
     * 将K8sNodeInfo直接转换为HostInfoDTO
     * 包含K8S扩展信息：roles、version、age等
     *
     * @param k8sNodeInfo K8S节点信息
     * @param clusterId   集群ID
     * @return 主机信息DTO
     */
    public HostInfoDTO convertToHostInfoDTO(K8sNodeInfo k8sNodeInfo, Long clusterId) {
        if (k8sNodeInfo == null) {
            return null;
        }

        HostInfoDTO hostInfoDTO = new HostInfoDTO();
        
        // 设置基础字段
        hostInfoDTO.setClusterId(clusterId);
        hostInfoDTO.setIp(k8sNodeInfo.getIp());
        hostInfoDTO.setHostname(k8sNodeInfo.getHostname());
        hostInfoDTO.setCoreNum(k8sNodeInfo.getCoreNum());
        hostInfoDTO.setTotalMem(k8sNodeInfo.getTotalMem());
        hostInfoDTO.setUsedMem(k8sNodeInfo.getUsedMem());
        hostInfoDTO.setTotalDisk(k8sNodeInfo.getTotalDisk());
        hostInfoDTO.setUsedDisk(k8sNodeInfo.getUsedDisk());
        hostInfoDTO.setCpuArchitecture(k8sNodeInfo.getCpuArchitecture());
        hostInfoDTO.setCreateTime(k8sNodeInfo.getCreateTime());
        hostInfoDTO.setHostState(convertToHostState(k8sNodeInfo.getStatus()));
        // 初始状态为未受管，使用新的管理状态字段
        hostInfoDTO.setRack("/default-rack");
        hostInfoDTO.setNodeLabel(null); // 主机标签由用户自定义
        
        // 设置K8S扩展字段
        hostInfoDTO.setRoles(k8sNodeInfo.getRoles() != null ? k8sNodeInfo.getRoles() : "<none>");
        hostInfoDTO.setVersion(k8sNodeInfo.getKubeVersion() != null ? k8sNodeInfo.getKubeVersion() : "unknown");
        hostInfoDTO.setAge(k8sNodeInfo.getAge() != null ? k8sNodeInfo.getAge() : "unknown");
        hostInfoDTO.setStatus(k8sNodeInfo.getStatus() != null ? k8sNodeInfo.getStatus() : "Ready");
        
        return hostInfoDTO;
    }

    /**
     * 批量转换K8sNodeInfo列表为HostInfoDTO列表
     *
     * @param k8sNodeInfoList K8S节点信息列表
     * @param clusterId       集群ID
     * @return 主机信息DTO列表
     */
    public List<HostInfoDTO> convertToHostInfoDTOList(List<K8sNodeInfo> k8sNodeInfoList, Long clusterId) {
        if (k8sNodeInfoList == null) {
            return null;
        }

        return k8sNodeInfoList.stream()
                .map(k8sNodeInfo -> convertToHostInfoDTO(k8sNodeInfo, clusterId))
                .collect(Collectors.toList());
    }

    /**
     * 将ClusterHostDO转换为HostInfoDTO
     * 主要用于传统主机（PVM）或从数据库读取的K8S主机
     *
     * @param clusterHost 集群主机实体
     * @return 主机信息DTO
     */
    public HostInfoDTO convertClusterHostToDTO(ClusterHostEntity clusterHost) {
        if (clusterHost == null) {
            return null;
        }

        HostInfoDTO hostInfoDTO = new HostInfoDTO();
        
        // 复制基础字段
        hostInfoDTO.setId(clusterHost.getId() != null ? clusterHost.getId() : null);
        hostInfoDTO.setCreateTime(clusterHost.getCreateTime());
        hostInfoDTO.setHostname(clusterHost.getHostname());
        hostInfoDTO.setIp(clusterHost.getIp());
        hostInfoDTO.setRack(clusterHost.getRack());
        hostInfoDTO.setCoreNum(clusterHost.getCoreNum());
        hostInfoDTO.setTotalMem(clusterHost.getTotalMem());
        hostInfoDTO.setTotalDisk(clusterHost.getTotalDisk());
        hostInfoDTO.setUsedMem(clusterHost.getUsedMem());
        hostInfoDTO.setUsedDisk(clusterHost.getUsedDisk());
        hostInfoDTO.setAverageLoad(clusterHost.getAverageLoad());
        hostInfoDTO.setCheckTime(clusterHost.getCheckTime());
        hostInfoDTO.setClusterId(clusterHost.getClusterId());
        hostInfoDTO.setHostState(clusterHost.getHostState());
        // 使用新的管理状态字段
        hostInfoDTO.setCpuArchitecture(clusterHost.getCpuArchitecture());
        hostInfoDTO.setNodeLabel(clusterHost.getNodeLabel());
        hostInfoDTO.setServiceRoleNum(clusterHost.getServiceRoleNum());
        
        // 从K8s JSON信息中提取字段
        extractK8sInfoFromJson(clusterHost.getK8sNodeInfo(), hostInfoDTO);
        
        // 设置状态字符串
        hostInfoDTO.setStatus(clusterHost.getHostState() != null && 
                              clusterHost.getHostState() == HostState.RUNNING ? "Ready" : "NotReady");
        
        return hostInfoDTO;
    }

    /**
     * 批量转换ClusterHostDO列表为HostInfoDTO列表
     */
    public List<HostInfoDTO> convertClusterHostListToDTO(List<ClusterHostEntity> clusterHostList) {
        if (clusterHostList == null) {
            return null;
        }

        return clusterHostList.stream()
                .map(this::convertClusterHostToDTO)
                .collect(Collectors.toList());
    }



    /**
     * 构建K8s节点信息JSON字符串
     * 
     * @param k8sNodeInfo K8s节点信息
     * @return JSON字符串
     */
    private String buildK8sNodeInfoJson(K8sNodeInfo k8sNodeInfo) {
        if (k8sNodeInfo == null) {
            return null;
        }
        
        Map<String, Object> k8sInfo = new HashMap<>();
        k8sInfo.put("status", k8sNodeInfo.getStatus() != null ? k8sNodeInfo.getStatus() : "Ready");
        k8sInfo.put("roles", k8sNodeInfo.getRoles() != null ? k8sNodeInfo.getRoles() : "<none>");
        k8sInfo.put("age", k8sNodeInfo.getAge() != null ? k8sNodeInfo.getAge() : "unknown");
        k8sInfo.put("version", k8sNodeInfo.getKubeVersion() != null ? k8sNodeInfo.getKubeVersion() : "unknown");
        
        return JSONUtil.toJsonStr(k8sInfo);
    }

    /**
     * 从K8s JSON信息中提取字段到HostInfoDTO
     * 
     * @param k8sNodeInfoJson K8s节点信息JSON字符串
     * @param hostInfoDTO 主机信息DTO
     */
    private void extractK8sInfoFromJson(String k8sNodeInfoJson, HostInfoDTO hostInfoDTO) {
        if (StrUtil.isBlank(k8sNodeInfoJson)) {
            // 非K8S主机的默认值
            hostInfoDTO.setRoles("<none>");
            hostInfoDTO.setVersion("unknown");
            hostInfoDTO.setAge("unknown");
            hostInfoDTO.setStatus("Ready");
            return;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> k8sInfo = JSONUtil.toBean(k8sNodeInfoJson, Map.class);
            hostInfoDTO.setStatus((String) k8sInfo.getOrDefault("status", "Ready"));
            hostInfoDTO.setRoles((String) k8sInfo.getOrDefault("roles", "<none>"));
            hostInfoDTO.setAge((String) k8sInfo.getOrDefault("age", "unknown"));
            hostInfoDTO.setVersion((String) k8sInfo.getOrDefault("version", "unknown"));
        } catch (Exception e) {
            // JSON解析失败时使用默认值
            hostInfoDTO.setRoles("<none>");
            hostInfoDTO.setVersion("unknown");
            hostInfoDTO.setAge("unknown");
            hostInfoDTO.setStatus("Ready");
        }
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