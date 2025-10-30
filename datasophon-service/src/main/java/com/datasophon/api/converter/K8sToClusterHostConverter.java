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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * K8S节点信息到集群主机实体的转换器
 * 使用MapStruct自动生成映射代码，避免手写set方法
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-31
 */
@Mapper(componentModel = "spring", imports = {ArrayList.class})
public interface K8sToClusterHostConverter {

    /**
     * 将K8sNodeInfo转换为ClusterHostEntity
     *
     * @param k8sNodeInfo K8S节点信息
     * @param clusterId   集群ID
     * @return 集群主机实体
     */
    @Mapping(target = "clusterId", source = "clusterId")
    @Mapping(target = "hostState", source = "k8sNodeInfo.status", qualifiedByName = "convertToHostState")
    @Mapping(target = "managementStatus", constant = "UNMANAGED")
    @Mapping(target = "rack", constant = "/default-rack")
    @Mapping(target = "nodeLabel", ignore = true)
    @Mapping(target = "k8sNodeInfo", source = "k8sNodeInfo", qualifiedByName = "buildK8sNodeInfoJson")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "averageLoad", ignore = true)
    @Mapping(target = "checkTime", ignore = true)
    @Mapping(target = "serviceRoleNum", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ClusterHostEntity convertToClusterHost(K8sNodeInfo k8sNodeInfo, Long clusterId);

    /**
     * 批量转换K8sNodeInfo列表为ClusterHostEntity列表
     *
     * @param k8sNodeInfoList K8S节点信息列表
     * @param clusterId       集群ID
     * @return 集群主机实体列表
     */
    default List<ClusterHostEntity> convertToClusterHostList(List<K8sNodeInfo> k8sNodeInfoList, Long clusterId) {
        if (k8sNodeInfoList == null) {
            return null;
        }
        
        List<ClusterHostEntity> result = new ArrayList<>();
        for (K8sNodeInfo k8sNodeInfo : k8sNodeInfoList) {
            result.add(convertToClusterHost(k8sNodeInfo, clusterId));
        }
        return result;
    }



    /**
     * 将ClusterHostEntity转换为HostInfoDTO
     * 主要用于传统主机（PVM）或从数据库读取的K8S主机
     *
     * @param clusterHost 集群主机实体
     * @return 主机信息DTO
     */
    @Mapping(target = "status", source = "hostState", qualifiedByName = "hostStateToStatus")
    @Mapping(target = "roles", source = "k8sNodeInfo", qualifiedByName = "extractRoles")
    @Mapping(target = "version", source = "k8sNodeInfo", qualifiedByName = "extractVersion")
    @Mapping(target = "age", source = "k8sNodeInfo", qualifiedByName = "extractAge")
    HostInfoDTO convertClusterHostToDTO(ClusterHostEntity clusterHost);





    // ========== MapStruct自定义转换方法 ==========
    
    /**
     * 构建K8s节点信息JSON字符串
     * 使用JDK21 Map.of()简化构建
     * 
     * @param k8sNodeInfo K8s节点信息
     * @return JSON字符串
     */
    @Named("buildK8sNodeInfoJson")
    static String buildK8sNodeInfoJson(K8sNodeInfo k8sNodeInfo) {
        if (k8sNodeInfo == null) {
            return null;
        }
        
        var k8sInfo = Map.of(
            "status", k8sNodeInfo.getStatus() != null ? k8sNodeInfo.getStatus() : "Ready",
            "roles", k8sNodeInfo.getRoles() != null ? k8sNodeInfo.getRoles() : "<none>",
            "age", k8sNodeInfo.getAge() != null ? k8sNodeInfo.getAge() : "unknown",
            "version", k8sNodeInfo.getKubeVersion() != null ? k8sNodeInfo.getKubeVersion() : "unknown"
        );
        
        return JSONUtil.toJsonStr(k8sInfo);
    }

    /**
     * 从K8s JSON信息中提取roles字段
     */
    @Named("extractRoles")
    static String extractRoles(String k8sNodeInfoJson) {
        if (StrUtil.isBlank(k8sNodeInfoJson)) {
            return "<none>";
        }
        
        try {
            var jsonObj = JSONUtil.parseObj(k8sNodeInfoJson);
            return jsonObj.getStr("roles", "<none>");
        } catch (Exception e) {
            return "<none>";
        }
    }
    
    /**
     * 从K8s JSON信息中提取version字段
     */
    @Named("extractVersion")
    static String extractVersion(String k8sNodeInfoJson) {
        if (StrUtil.isBlank(k8sNodeInfoJson)) {
            return "unknown";
        }
        
        try {
            var jsonObj = JSONUtil.parseObj(k8sNodeInfoJson);
            return jsonObj.getStr("version", "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 从K8s JSON信息中提取age字段
     */
    @Named("extractAge")
    static String extractAge(String k8sNodeInfoJson) {
        if (StrUtil.isBlank(k8sNodeInfoJson)) {
            return "unknown";
        }
        
        try {
            var jsonObj = JSONUtil.parseObj(k8sNodeInfoJson);
            return jsonObj.getStr("age", "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 将K8S节点状态转换为主机状态枚举
     */
    @Named("convertToHostState")
    static HostState convertToHostState(String k8sStatus) {
        return "ready".equalsIgnoreCase(k8sStatus) ? HostState.RUNNING : HostState.OFFLINE;
    }
    
    /**
     * 主机状态转换为状态字符串
     */
    @Named("hostStateToStatus")
    static String hostStateToStatus(HostState hostState) {
        return hostState == HostState.RUNNING ? "Ready" : "NotReady";
    }
    

}