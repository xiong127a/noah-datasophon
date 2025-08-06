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

import lombok.Getter;

/**
 * 集群服务角色实例WebUI VO - 视图展示对象
 * 使用JDK21 Record特性，专为前端展示优化
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
public record ClusterServiceRoleInstanceWebuisVO(
    Integer id,
    Integer serviceRoleInstanceId,
    String webUrl,
    Integer serviceInstanceId,
    String name,
    String displayName,
    boolean isValidUrl,
    WebUIType type,
    WebUIStatus status
) {
    
    /**
     * WebUI类型枚举 - JDK21嵌套特性
     */
    @Getter
    public enum WebUIType {
        ADMIN("管理界面"),
        MONITOR("监控界面"),
        API("API接口"),
        DASHBOARD("仪表盘"),
        OTHER("其他");
        
        private final String description;
        
        WebUIType(String description) {
            this.description = description;
        }

    }
    
    /**
     * WebUI状态枚举
     */
    @Getter
    public enum WebUIStatus {
        ACTIVE("可访问"),
        INACTIVE("不可访问"),
        UNKNOWN("未知");
        
        private final String description;
        
        WebUIStatus(String description) {
            this.description = description;
        }

    }
    
    /**
     * 从DTO创建VO的静态工厂方法
     */
    public static ClusterServiceRoleInstanceWebuisVO fromDTO(
            com.datasophon.common.dto.ClusterServiceRoleInstanceWebuisDTO dto) {
        return new ClusterServiceRoleInstanceWebuisVO(
            dto.id(),
            dto.serviceRoleInstanceId(),
            dto.webUrl(),
            dto.serviceInstanceId(),
            dto.name(),
            dto.getDisplayName(),
            dto.isValidUrl(),
            determineWebUIType(dto.name()),
            WebUIStatus.UNKNOWN // 默认状态，实际应该通过健康检查确定
        );
    }
    
    /**
     * 根据名称确定WebUI类型
     */
    private static WebUIType determineWebUIType(String name) {
        if (name == null) return WebUIType.OTHER;
        
        var lowerName = name.toLowerCase();
        return switch (lowerName) {
            case String s when s.contains("admin") -> WebUIType.ADMIN;
            case String s when s.contains("monitor") -> WebUIType.MONITOR;
            case String s when s.contains("api") -> WebUIType.API;
            case String s when s.contains("dashboard") -> WebUIType.DASHBOARD;
            default -> WebUIType.OTHER;
        };
    }
}