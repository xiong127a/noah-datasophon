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

package com.datasophon.api.service.impl;

import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ServiceRoleHostnameService;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务角色主机名查询服务实现
 * 从ProcessUtils迁移而来的主机名查询功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Slf4j
@Service
public class ServiceRoleHostnameServiceImpl implements ServiceRoleHostnameService {

    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

    @Override
    public String getServiceRoleHostname(Long clusterId, String serviceName, String roleName) {
        if (clusterId == null || serviceName == null || roleName == null) {
            log.warn("获取服务角色主机名参数为空: clusterId={}, serviceName={}, roleName={}", 
                clusterId, serviceName, roleName);
            return null;
        }
        
        try {
            List<ClusterServiceRoleInstanceDTO> roleInstances = 
                clusterServiceRoleInstanceService.getServiceRoleInstanceListByClusterIdAndRoleName(
                    clusterId, roleName);
            
            if (roleInstances != null && !roleInstances.isEmpty()) {
                // 过滤匹配的服务名，然后返回第一个实例的主机名
                for (ClusterServiceRoleInstanceDTO instance : roleInstances) {
                    if (serviceName.equals(instance.serviceName())) {
                        return instance.hostname();
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取服务角色主机名失败: clusterId={}, serviceName={}, roleName={}", 
                clusterId, serviceName, roleName, e);
        }
        
        return null;
    }
}