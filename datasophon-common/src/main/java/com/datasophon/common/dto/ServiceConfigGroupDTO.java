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
 * 服务配置分组DTO
 * 使用JDK21 record语法定义不可变数据结构
 * 
 * @author 任相鹏
 * @email 635887935@qq.com  
 * @date 2025-01-20
 * @since JDK21
 */
public record ServiceConfigGroupDTO(
        /**
         * 分组键值映射
         * key: 分组标识
         * value: 分组详细信息
         */
        Map<String, GroupInfo> groups
) {
    
    /**
     * 分组信息
     * 使用JDK21 record定义嵌套数据结构
     */
    public record GroupInfo(
            /**
             * 分组显示名称
             */
            String displayName,
            
            /**
             * 分组内的配置项列表
             */
            List<ServiceConfig> configs
    ) {
        
        /**
         * 构造器验证
         */
        public GroupInfo {
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("分组显示名称不能为空");
            }
            if (configs == null) {
                configs = List.of(); // JDK21: 不可变空列表
            }
        }
        
        /**
         * 获取配置项数量
         * 
         * @return 配置项数量
         */
        public int getConfigCount() {
            return configs.size();
        }
        
        /**
         * 检查是否为空分组
         * 
         * @return 是否为空分组
         */
        public boolean isEmpty() {
            return configs.isEmpty();
        }
    }
    
    /**
     * 构造器验证
     */
    public ServiceConfigGroupDTO {
        if (groups == null) {
            groups = Map.of(); // JDK21: 不可变空映射
        }
    }
    
    /**
     * 获取分组数量
     * 
     * @return 分组数量
     */
    public int getGroupCount() {
        return groups.size();
    }
    
    /**
     * 获取总配置项数量
     * 
     * @return 总配置项数量
     */
    public int getTotalConfigCount() {
        return groups.values().stream()
                .mapToInt(GroupInfo::getConfigCount)
                .sum();
    }
    
    /**
     * 检查是否为空
     * 
     * @return 是否为空
     */
    public boolean isEmpty() {
        return groups.isEmpty() || groups.values().stream().allMatch(GroupInfo::isEmpty);
    }
    
    /**
     * 获取指定分组的配置
     * 
     * @param groupKey 分组键
     * @return 分组信息，不存在时返回null
     */
    public GroupInfo getGroup(String groupKey) {
        return groups.get(groupKey);
    }
    
    /**
     * 检查是否包含指定分组
     * 
     * @param groupKey 分组键
     * @return 是否包含该分组
     */
    public boolean hasGroup(String groupKey) {
        return groups.containsKey(groupKey);
    }
}
