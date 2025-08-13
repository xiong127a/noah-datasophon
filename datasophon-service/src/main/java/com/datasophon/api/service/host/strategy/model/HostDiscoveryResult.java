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

import com.datasophon.dao.entity.ClusterHostEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 主机发现结果模型
 */
@Data
@Builder
public class HostDiscoveryResult {
    
    /**
     * 发现的主机列表
     */
    private List<ClusterHostEntity> hosts;
    
    /**
     * 发现总数
     */
    private Integer totalCount;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 额外的元数据信息
     */
    private Map<String, Object> metadata;
    
    /**
     * 发现耗时（毫秒）
     */
    private Long discoveryTime;
}