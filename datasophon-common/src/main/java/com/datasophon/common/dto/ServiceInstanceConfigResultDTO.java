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

import com.datasophon.common.model.ServiceConfig;

import java.util.List;
import java.util.Map;

/**
 * 服务实例配置查询结果DTO - 服务间传输对象
 * 使用JDK21 Record特性，不可变数据载体
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
public record ServiceInstanceConfigResultDTO(
    Map<String, List<ServiceConfig>> data,
    Integer totalCount,
    boolean hasData
) {
    
    /**
     * 创建查询结果DTO的静态工厂方法
     */
    public static ServiceInstanceConfigResultDTO create(Map<String, List<ServiceConfig>> data) {
        var totalCount = data != null ? 
            data.values().stream().mapToInt(List::size).sum() : 0;
        var hasData = data != null && !data.isEmpty();
        
        return new ServiceInstanceConfigResultDTO(data, totalCount, hasData);
    }
    
    /**
     * 创建空结果
     */
    public static ServiceInstanceConfigResultDTO empty() {
        return new ServiceInstanceConfigResultDTO(Map.of(), 0, false);
    }
}