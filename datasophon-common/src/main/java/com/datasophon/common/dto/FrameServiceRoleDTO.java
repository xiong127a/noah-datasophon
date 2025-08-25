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

package com.datasophon.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 框架服务角色数据传输对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrameServiceRoleDTO(
        Long id,
        Long serviceId,
        String serviceName,     // 新增：服务名称字段
        String serviceRoleName,
        Integer serviceRoleType,
        String cardinality,
        String serviceRoleJson,
        String serviceRoleJsonMd5,
        String frameCode,
        String jmxPort,
        String logFile,
        List<String> hosts // 运行时计算的主机列表
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建基础FrameServiceRoleDTO，不包含主机列表
     */
    public static FrameServiceRoleDTO of(Long id, Long serviceId, String serviceName, String serviceRoleName,
            Integer serviceRoleType, String cardinality, String frameCode) {
        return new FrameServiceRoleDTO(id, serviceId, serviceName, serviceRoleName, serviceRoleType, cardinality,
                null, null, frameCode, null, null, null);
    }

    /**
     * 创建包含主机列表的FrameServiceRoleDTO
     */
    public static FrameServiceRoleDTO withHosts(Long id, Long serviceId, String serviceName, String serviceRoleName,
            Integer serviceRoleType, String cardinality, String serviceRoleJson, String serviceRoleJsonMd5,
            String frameCode, String jmxPort, String logFile, List<String> hosts) {
        return new FrameServiceRoleDTO(id, serviceId, serviceName, serviceRoleName, serviceRoleType, cardinality,
                serviceRoleJson, serviceRoleJsonMd5, frameCode, jmxPort, logFile, hosts);
    }

    /**
     * 创建新的DTO，设置主机列表
     */
    public FrameServiceRoleDTO withHosts(List<String> newHosts) {
        return new FrameServiceRoleDTO(id, serviceId, serviceName, serviceRoleName, serviceRoleType, cardinality,
                serviceRoleJson, serviceRoleJsonMd5, frameCode, jmxPort, logFile, newHosts);
    }

    /**
     * 获取主机数量
     */
    public int getHostsCount() {
        return hosts != null ? hosts.size() : 0;
    }

    /**
     * 检查是否有主机
     */
    public boolean hasHosts() {
        return hosts != null && !hosts.isEmpty();
    }

    /**
     * 获取角色类型显示名称
     */
    public String getRoleTypeDisplayName() {
        if (serviceRoleType == null) {
            return "未知";
        }
        if (serviceRoleType.equals(1)) {
            return "主节点";
        } else if (serviceRoleType.equals(2)) {
            return "工作节点";
        } else if (serviceRoleType.equals(3)) {
            return "客户端";
        }
        return "未知";
    }

    /**
     * 检查是否为Master角色
     */
    public boolean isMasterRole() {
        return serviceRoleType != null && serviceRoleType.equals(1);
    }

    /**
     * 检查是否为Worker角色
     */
    public boolean isWorkerRole() {
        return serviceRoleType != null && serviceRoleType.equals(2);
    }

    /**
     * 检查是否为Client角色
     */
    public boolean isClientRole() {
        return serviceRoleType != null && serviceRoleType.equals(3);
    }

    /**
     * 获取基数信息描述
     */
    public String getCardinalityDescription() {
        if (cardinality == null || cardinality.isEmpty()) {
            return "未指定";
        }
        return switch (cardinality) {
            case "1" -> "单实例";
            case "1+" -> "一个或多个实例";
            case "0+" -> "零个或多个实例";
            default -> cardinality;
        };
    }
}