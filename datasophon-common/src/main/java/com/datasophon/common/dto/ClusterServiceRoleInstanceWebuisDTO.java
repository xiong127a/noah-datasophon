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

/**
 * 集群服务角色实例WebUI DTO - 服务间传输对象
 * 使用JDK21 Record特性，不可变数据载体
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
public record ClusterServiceRoleInstanceWebuisDTO(
    Integer id,
    Integer serviceRoleInstanceId,
    String webUrl,
    Integer serviceInstanceId,
    String name
) {
    
    /**
     * 创建新WebUI DTO的静态工厂方法
     */
    public static ClusterServiceRoleInstanceWebuisDTO create(
            Integer serviceRoleInstanceId,
            String webUrl,
            Integer serviceInstanceId,
            String name) {
        return new ClusterServiceRoleInstanceWebuisDTO(
            null, serviceRoleInstanceId, webUrl, serviceInstanceId, name);
    }
    
    /**
     * 检查WebUI URL是否有效
     */
    public boolean isValidUrl() {
        return webUrl != null && 
               (webUrl.startsWith("http://") || webUrl.startsWith("https://"));
    }
    
    /**
     * 获取WebUI显示名称
     */
    public String getDisplayName() {
        return name != null && !name.trim().isEmpty() ? name : "Web UI";
    }
}