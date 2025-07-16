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

package com.datasophon.common.model.kubernetes;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 命名空间信息实体类
 */
@Data
public class NamespaceInfo {
    /**
     * 命名空间名称
     */
    private String name;
    
    /**
     * 命名空间状态
     */
    private String status;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 标签
     */
    private Map<String, String> labels;
    
    /**
     * 注解
     */
    private Map<String, String> annotations;
    
    /**
     * 资源配额
     */
    private ResourceQuota resourceQuota;
    
    /**
     * 资源配额信息
     */
    @Data
    public static class ResourceQuota {
        /**
         * 最大Pod数量
         */
        private Integer podsLimit;
        
        /**
         * 最大CPU请求
         */
        private String cpuRequestLimit;
        
        /**
         * 最大CPU限制
         */
        private String cpuLimitLimit;
        
        /**
         * 最大内存请求
         */
        private String memoryRequestLimit;
        
        /**
         * 最大内存限制
         */
        private String memoryLimitLimit;
        
        /**
         * 已用Pod数量
         */
        private Integer podsUsed;
        
        /**
         * 已用CPU请求
         */
        private String cpuRequestUsed;
        
        /**
         * 已用CPU限制
         */
        private String cpuLimitUsed;
        
        /**
         * 已用内存请求
         */
        private String memoryRequestUsed;
        
        /**
         * 已用内存限制
         */
        private String memoryLimitUsed;
    }
} 