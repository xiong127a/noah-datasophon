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

package com.datasophon.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 框架服务角色视图对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrameServiceRoleVO(
        Integer id,
        Integer serviceId,
        String serviceRoleName,
        Integer serviceRoleType,
        String serviceRoleTypeText, // 角色类型显示文本
        String cardinality,
        String cardinalityDescription, // 基数描述
        String serviceRoleJson,
        String serviceRoleJsonMd5,
        String frameCode,
        String jmxPort,
        String logFile,
        List<String> hosts, // 主机列表
        Integer hostsCount, // 主机数量
        String hostsSummary, // 主机摘要
        Boolean hasHosts, // 是否有主机
        Boolean isMasterRole, // 是否为Master角色
        Boolean isWorkerRole, // 是否为Worker角色
        Boolean isClientRole // 是否为Client角色
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 获取角色类型显示文本
     */
    public String getServiceRoleTypeText() {
        if (serviceRoleTypeText != null) {
            return serviceRoleTypeText;
        }
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
     * 获取基数描述
     */
    public String getCardinalityDescription() {
        if (cardinalityDescription != null) {
            return cardinalityDescription;
        }
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

    /**
     * 获取主机摘要
     */
    public String getHostsSummary() {
        if (hostsSummary != null) {
            return hostsSummary;
        }
        if (hosts == null || hosts.isEmpty()) {
            return "暂无主机";
        }
        if (hosts.size() == 1) {
            return hosts.get(0);
        }
        return hosts.get(0) + " 等 " + hosts.size() + " 台主机";
    }

    /**
     * 获取主机数量
     */
    public Integer getHostsCount() {
        if (hostsCount != null) {
            return hostsCount;
        }
        return hosts != null ? hosts.size() : 0;
    }

    /**
     * 检查是否有主机
     */
    public Boolean hasHosts() {
        if (hasHosts != null) {
            return hasHosts;
        }
        return hosts != null && !hosts.isEmpty();
    }

    /**
     * 检查是否为Master角色
     */
    public Boolean isMasterRole() {
        if (isMasterRole != null) {
            return isMasterRole;
        }
        return serviceRoleType != null && serviceRoleType.equals(1);
    }

    /**
     * 检查是否为Worker角色
     */
    public Boolean isWorkerRole() {
        if (isWorkerRole != null) {
            return isWorkerRole;
        }
        return serviceRoleType != null && serviceRoleType.equals(2);
    }

    /**
     * 检查是否为Client角色
     */
    public Boolean isClientRole() {
        if (isClientRole != null) {
            return isClientRole;
        }
        return serviceRoleType != null && serviceRoleType.equals(3);
    }

    /**
     * 创建基础FrameServiceRoleVO
     */
    public static FrameServiceRoleVO of(Integer id, Integer serviceId, String serviceRoleName,
            Integer serviceRoleType, String cardinality, String frameCode) {
        String roleTypeText = getRoleTypeText(serviceRoleType);
        String cardinalityDesc = getCardinalityDesc(cardinality);

        return new FrameServiceRoleVO(id, serviceId, serviceRoleName, serviceRoleType, roleTypeText,
                cardinality, cardinalityDesc, null, null, frameCode, null, null,
                null, 0, "暂无主机", false,
                serviceRoleType != null && serviceRoleType.equals(1),
                serviceRoleType != null && serviceRoleType.equals(2),
                serviceRoleType != null && serviceRoleType.equals(3));
    }

    /**
     * 创建包含主机信息的FrameServiceRoleVO
     */
    public static FrameServiceRoleVO withHosts(Integer id, Integer serviceId, String serviceRoleName,
            Integer serviceRoleType, String cardinality, String serviceRoleJson, String serviceRoleJsonMd5,
            String frameCode, String jmxPort, String logFile, List<String> hosts) {
        String roleTypeText = getRoleTypeText(serviceRoleType);
        String cardinalityDesc = getCardinalityDesc(cardinality);
        Integer hostsCount = hosts != null ? hosts.size() : 0;
        String hostsSummary = getHostsSummaryText(hosts);
        Boolean hasHosts = hosts != null && !hosts.isEmpty();

        return new FrameServiceRoleVO(id, serviceId, serviceRoleName, serviceRoleType, roleTypeText,
                cardinality, cardinalityDesc, serviceRoleJson, serviceRoleJsonMd5, frameCode,
                jmxPort, logFile, hosts, hostsCount, hostsSummary, hasHosts,
                serviceRoleType != null && serviceRoleType.equals(1),
                serviceRoleType != null && serviceRoleType.equals(2),
                serviceRoleType != null && serviceRoleType.equals(3));
    }

    /**
     * 获取角色类型文本
     */
    private static String getRoleTypeText(Integer roleType) {
        if (roleType == null) {
            return "未知";
        }
        if (roleType.equals(1)) {
            return "主节点";
        } else if (roleType.equals(2)) {
            return "工作节点";
        } else if (roleType.equals(3)) {
            return "客户端";
        }
        return "未知";
    }

    /**
     * 获取基数描述
     */
    private static String getCardinalityDesc(String cardinality) {
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

    /**
     * 获取主机摘要文本
     */
    private static String getHostsSummaryText(List<String> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            return "暂无主机";
        }
        if (hosts.size() == 1) {
            return hosts.get(0);
        }
        return hosts.get(0) + " 等 " + hosts.size() + " 台主机";
    }
}