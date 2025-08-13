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

package com.datasophon.api.service.host.strategy.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 主机发现请求模型
 */
@Data
@Builder
public class HostDiscoveryRequest {
    
    /**
     * 集群ID
     */
    private Long clusterId;
    
    /**
     * 连接参数（包含不同策略特有的连接信息）
     * PVM模式：hosts, sshUser, sshPort, sshPassword等
     * K8S模式：kubeConfigContent, namespace等
     */
    private Map<String, Object> connectionParams;
    
    /**
     * 是否强制刷新（忽略缓存）
     */
    @Builder.Default
    private Boolean forceRefresh = false;
    
    /**
     * 超时时间（秒）
     */
    @Builder.Default
    private Integer timeoutSeconds = 30;
}