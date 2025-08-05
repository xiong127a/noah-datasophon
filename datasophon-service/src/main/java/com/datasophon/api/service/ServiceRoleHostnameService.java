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

package com.datasophon.api.service;

/**
 * 服务角色主机名查询服务
 * 从ProcessUtils迁移而来的主机名查询功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ServiceRoleHostnameService {

    /**
     * 根据集群ID、服务名称和角色名称获取服务角色主机名
     * 
     * @param clusterId 集群ID
     * @param serviceName 服务名称
     * @param roleName 角色名称
     * @return 主机名，如果未找到返回null
     */
    String getServiceRoleHostname(Integer clusterId, String serviceName, String roleName);
}