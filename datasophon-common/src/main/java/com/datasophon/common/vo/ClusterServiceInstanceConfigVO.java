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
 * 集群服务实例配置VO - 视图展示对象
 * 使用JDK21 Record特性，专为前端展示优化
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
public record ClusterServiceInstanceConfigVO(
    Integer id,
    Integer serviceId,
    String createTimeFormatted,
    String configJson,
    String updateTimeFormatted,
    String configJsonMd5,
    Integer configVersion,
    Integer clusterId,
    String configFileJson,
    String configFileJsonMd5,
    boolean isValidConfig,
    boolean isConfigUpdated,
    int configSize,
    String configSizeFormatted,
    ConfigStatus status
) {
    
    /**
     * 配置状态枚举 - JDK21嵌套特性
     */
    @Getter
    public enum ConfigStatus {
        DRAFT("草稿"),
        ACTIVE("生效中"),
        OUTDATED("已过期"),
        ERROR("错误");
        
        private final String description;
        
        ConfigStatus(String description) {
            this.description = description;
        }

    }
    
    /**
     * 从DTO创建VO的静态工厂方法
     */
    public static ClusterServiceInstanceConfigVO fromDTO(
            com.datasophon.common.dto.ClusterServiceInstanceConfigDTO dto) {
        return new ClusterServiceInstanceConfigVO(
            dto.id(),
            dto.serviceId(),
            dto.createTime() != null ? dto.createTime().toString() : null,
            dto.configJson(),
            dto.updateTime() != null ? dto.updateTime().toString() : null,
            dto.configJsonMd5(),
            dto.configVersion(),
            dto.clusterId(),
            dto.configFileJson(),
            dto.configFileJsonMd5(),
            dto.isValidConfig(),
            dto.isConfigUpdated(),
            dto.getConfigSize(),
            formatSize(dto.getConfigSize()),
            determineConfigStatus(dto)
        );
    }
    
    /**
     * 格式化文件大小
     */
    private static String formatSize(int bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    /**
     * 确定配置状态
     */
    private static ConfigStatus determineConfigStatus(
            com.datasophon.common.dto.ClusterServiceInstanceConfigDTO dto) {
        
        if (!dto.isValidConfig()) {
            return ConfigStatus.ERROR;
        }
        
        if (dto.configVersion() == null || dto.configVersion() == 1) {
            return ConfigStatus.DRAFT;
        }
        
        return dto.isConfigUpdated() ? ConfigStatus.ACTIVE : ConfigStatus.OUTDATED;
    }
}