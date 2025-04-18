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

package com.datasophon.common.model.k8s;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * Deployment信息实体类
 */
@Data
public class DeploymentInfo {
    /**
     * Deployment名称
     */
    private String name;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 标签
     */
    private Map<String, String> labels;

    /**
     * 容器镜像
     */
    private String image;

    /**
     * 副本数
     */
    private int replicas;

    /**
     * 可用副本数
     */
    private int availableReplicas;

    /**
     * 就绪副本数
     */
    private int readyReplicas;

    /**
     * 更新策略
     */
    private String strategy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 选择器
     */
    private Map<String, String> selector;

    /**
     * 资源配额
     */
    private ResourceQuota resources;

    /**
     * 资源配额
     */
    @Data
    public static class ResourceQuota {
        /**
         * CPU请求
         */
        private String cpuRequest;

        /**
         * CPU限制
         */
        private String cpuLimit;

        /**
         * 内存请求
         */
        private String memoryRequest;

        /**
         * 内存限制
         */
        private String memoryLimit;
    }
}