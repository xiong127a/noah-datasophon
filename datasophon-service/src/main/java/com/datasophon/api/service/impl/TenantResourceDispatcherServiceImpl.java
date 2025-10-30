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

import com.datasophon.api.client.WorkerHttpClient;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.TenantResourceDispatcherService;
import com.datasophon.common.model.tenant.resource.TenantFrameResource;
import com.datasophon.common.model.tenant.resource.TenantHiveResource;
import com.datasophon.common.model.tenant.resource.TenantKafkaResource;
import com.datasophon.common.model.tenant.resource.TenantYarnResource;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.mybatisflex.core.query.QueryChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 租户资源分发服务实现
 * 替代TenantResourceDispatcherActor，使用Spring Service + HTTP通信
 */
@Service
public class TenantResourceDispatcherServiceImpl implements TenantResourceDispatcherService {

    private static final Logger logger = LoggerFactory.getLogger(TenantResourceDispatcherServiceImpl.class);

    @Autowired(required = false)
    private WorkerHttpClient workerHttpClient;
    
    @Autowired(required = false)
    private YarnQueueService yarnQueueService;

    @Override
    @Async("taskExecutor")
    public void handleTenantFrameResource(TenantFrameResource tenantFrameResource) {
        try {
            Map<String, String> roleHostMap = getRoleHostMap(tenantFrameResource.getClusterId());

            if ("YARN".equals(tenantFrameResource.getServiceName())) {
                TenantYarnResource tenantYarnResource = (TenantYarnResource) tenantFrameResource;
                tenantYarnResource.setClusterId(tenantFrameResource.getClusterId());
                if (yarnQueueService != null) {
                    yarnQueueService.handleTenantYarnResource(tenantYarnResource);
                }
            } else {
                String serviceMasterRoleName = getServiceMasterRoleName(tenantFrameResource.getServiceName());
                
                if ("KAFKA".equals(tenantFrameResource.getServiceName())) {
                    String zkAddr = GlobalVariables.get(tenantFrameResource.getClusterId()).get("${kafkaZkAddr}");
                    TenantKafkaResource kafkaResource = (TenantKafkaResource) tenantFrameResource;
                    kafkaResource.setKafkaZkAddr(zkAddr);
                }
                
                if ("HIVE".equals(tenantFrameResource.getServiceName())) {
                    String hiveMetastoreDir = GlobalVariables.get(tenantFrameResource.getClusterId())
                            .get("${hive.metastore.warehouse.dir}");
                    TenantHiveResource hiveResource = (TenantHiveResource) tenantFrameResource;
                    hiveResource.setHiveMetastoreDir(hiveMetastoreDir);
                }
                
                // 使用HTTP/SSE与Worker通信，而不是Actor
                String masterHost = roleHostMap.get(serviceMasterRoleName);
                if (workerHttpClient != null && masterHost != null) {
                    // TODO: 通过WorkerHttpClient发送租户资源配置到Worker
                    logger.info("发送租户资源配置到Worker: {}, 服务: {}", 
                            masterHost, tenantFrameResource.getServiceName());
                } else {
                    logger.warn("WorkerHttpClient未初始化或找不到主角色主机: {}", serviceMasterRoleName);
                }
            }
        } catch (Exception e) {
            logger.error("处理TenantFrameResource时出错", e);
        }
    }

    private Map<String, String> getRoleHostMap(Long clusterId) {
        List<ClusterServiceRoleInstanceEntity> roleInstances = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .list();

        return roleInstances.stream().collect(Collectors.toMap(
                ClusterServiceRoleInstanceEntity::getServiceRoleName,
                ClusterServiceRoleInstanceEntity::getHostname,
                (a, b) -> a,
                HashMap::new));
    }

    private String getServiceMasterRoleName(String serviceName) {
        return switch (serviceName) {
            case "HDFS" -> "NameNode";
            case "HIVE" -> "HiveServer2";
            case "KAFKA" -> "KafkaBroker";
            case "HBASE" -> "HbaseMaster";
            default -> "";
        };
    }
}

