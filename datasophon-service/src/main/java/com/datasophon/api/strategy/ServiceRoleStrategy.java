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
     * 传入参数为所有master节点host
     * 保存服务角色与主机对应关系时
     * 添加自定义变量保存到变量表和全局变量缓存
     * 可以在服务配置时取到对应的变量
     */
    void handler(Integer clusterId, List<String> hosts);

    /**
     * 保存服务配置时
     * 添加自定义配置 或者修改其它服务配置
     * 之后会将对应的配置添加到表和全部变量
     */
    void handlerConfig(Integer clusterId, List<ServiceConfig> list);

    /**
     * 获取服务配置时修改配置
     * handler之后handlerConfig之前调用
     * 提取角色本身配置和handler中自定义的变量
     */
    void getConfig(Integer clusterId, List<ServiceConfig> list);

    /**
     * 构建DAG时处理角色关系
     */
    void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname);

    /**
     * 定期检查角色处理
     */
    void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,
                                 Map<String, ClusterServiceRoleInstanceEntity> map);
}
