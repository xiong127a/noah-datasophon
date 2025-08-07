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

package com.datasophon.api.load.model;

import com.datasophon.common.dto.ClusterInfoDTO;

import java.util.List;

/**
 * JDK 21 Record: 服务元数据加载上下文
 * 封装加载过程中需要的共享数据
 * 
 * @param clusters 集群信息列表
 * @param hostName 主机名
 * @param apiPort API端口
 * @param priorityNetworks 优先网络配置
 */
public record LoadContext(
    List<ClusterInfoDTO> clusters,
    String hostName,
    String apiPort,
    String priorityNetworks
) {
    
    /**
     * 创建LoadContext的便捷工厂方法
     */
    public static LoadContext of(List<ClusterInfoDTO> clusters, 
                                String hostName, 
                                String apiPort, 
                                String priorityNetworks) {
        return new LoadContext(clusters, hostName, apiPort, priorityNetworks);
    }
    
    /**
     * 检查是否有集群信息
     */
    public boolean hasClusters() {
        return clusters != null && !clusters.isEmpty();
    }
    
    /**
     * 获取集群数量
     */
    public int clusterCount() {
        return clusters != null ? clusters.size() : 0;
    }
}