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

/**
 * 主机列表请求模型
 */
@Data
@Builder
public class HostListRequest {
    
    /**
     * 集群ID
     */
    private Long clusterId;
    
    /**
     * 分页页码
     */
    @Builder.Default
    private Integer page = 1;
    
    /**
     * 分页大小
     */
    @Builder.Default
    private Integer pageSize = 20;
    
    /**
     * 主机名筛选
     */
    private String hostname;
    
    /**
     * IP地址筛选
     */
    private String ip;
    
    /**
     * CPU架构筛选
     */
    private String cpuArchitecture;
    
    /**
     * 主机状态筛选
     */
    private Integer hostState;
    
    /**
     * 排序字段
     */
    private String orderField;
    
    /**
     * 排序类型（ASC/DESC）
     */
    private String orderType;
    
    /**
     * 是否包含检查项信息
     */
    @Builder.Default
    private Boolean includeCheckItems = true;
}