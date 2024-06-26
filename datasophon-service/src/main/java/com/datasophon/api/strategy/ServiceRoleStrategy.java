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

package com.datasophon.api.strategy;

import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.List;
import java.util.Map;

public interface ServiceRoleStrategy {

    /**
     * 保存角色host映射关系时根据roleName调用
     */
    void handler(Integer clusterId, List<String> hosts);

    /**
     * 保存服务配置时根据ServiceName调用
     */
    void handlerConfig(Integer clusterId, List<ServiceConfig> list);

    /**
     * 获取服务配置时修改配置，根据ServiceName调用
     * handler之后handlerConfig之前调用
     * 提取角色本身配置和handler中自定义的变量
     */
    void getConfig(Integer clusterId, List<ServiceConfig> list);

    /**
     * 构建DAG时处理角色关系，例如设置主从角色，设置搭建顺序等。
     *
     * 可以将自定义角色配置传递给worker
     */
    void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname);

    /**
     * 定期检查角色处理
     */
    void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,
                                 Map<String, ClusterServiceRoleInstanceEntity> map);
}
