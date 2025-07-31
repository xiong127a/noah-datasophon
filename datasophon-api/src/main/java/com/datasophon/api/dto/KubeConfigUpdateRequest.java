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

package com.datasophon.api.dto;

import lombok.Data;

/**
 * 更新集群Kubernetes配置请求DTO
 */
@Data
public class KubeConfigUpdateRequest {

    /**
     * 集群ID
     */
    private Integer clusterId;

    /**
     * Kubernetes配置内容
     */
    private String kubeConfig;

    /**
     * 选择的命名空间
     */
    private String namespace;

    /**
     * 自定义命名空间名称（当选择创建新命名空间时）
     */
    private String customNamespace;
}