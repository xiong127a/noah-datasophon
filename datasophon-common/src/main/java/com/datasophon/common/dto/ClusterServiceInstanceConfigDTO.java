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

import java.time.LocalDateTime;

/**
 * 集群服务实例配置DTO - 服务间传输对象
 * 使用JDK21 Record特性，不可变数据载体
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
public record ClusterServiceInstanceConfigDTO(
    Integer id,
    Integer serviceId,
    LocalDateTime createTime,
    String configJson,
    LocalDateTime updateTime,
    String configJsonMd5,
    Integer configVersion,
    Long clusterId,
    String configFileJson,
    String configFileJsonMd5
) {
    
    /**
     * 创建新配置DTO的静态工厂方法
     */
    public static ClusterServiceInstanceConfigDTO create(
            Integer serviceId,
            Long clusterId,
            String configJson,
            String configFileJson) {
        var now = LocalDateTime.now();
        return new ClusterServiceInstanceConfigDTO(
            null, serviceId, now, configJson, now,
            generateMd5(configJson), 1, clusterId,
            configFileJson, generateMd5(configFileJson)
        );
    }
    
    /**
     * 检查配置是否有效
     */
    public boolean isValidConfig() {
        return configJson != null && !configJson.trim().isEmpty() &&
               configJsonMd5 != null && !configJsonMd5.trim().isEmpty();
    }
    
    /**
     * 检查配置是否已更新
     */
    public boolean isConfigUpdated() {
        return updateTime != null && createTime != null && 
               updateTime.isAfter(createTime);
    }
    
    /**
     * 获取配置大小（字节）
     */
    public int getConfigSize() {
        return configJson != null ? configJson.getBytes().length : 0;
    }
    
    /**
     * 简单MD5生成（实际项目中应使用真实MD5算法）
     */
    private static String generateMd5(String content) {
        return content != null ? String.valueOf(content.hashCode()) : "";
    }
}