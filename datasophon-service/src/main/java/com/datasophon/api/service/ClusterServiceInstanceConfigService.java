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

import com.mybatisflex.core.service.IService;
import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;

import java.util.List;
import java.util.Map;

/**
 * 集群服务角色实例配置表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ClusterServiceInstanceConfigService extends IService<ClusterServiceInstanceConfigEntity> {

    Map<String, Object> getServiceInstanceConfig(Integer serviceInstanceId, Integer version, Integer roleGroupId,
            Integer page,
            Integer pageSize);

    ClusterServiceInstanceConfigEntity getServiceConfigByServiceId(Integer id);

    List<Map<String, Object>> getConfigVersion(Integer serviceInstanceId, Integer roleGroupId);
}
