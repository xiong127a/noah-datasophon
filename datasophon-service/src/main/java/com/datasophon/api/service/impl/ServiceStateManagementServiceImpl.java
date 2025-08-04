/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.service.impl;

import com.datasophon.api.converter.ClusterServiceRoleInstanceConverter;
import com.datasophon.api.service.*;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.CommandType;
import com.datasophon.dao.entity.ClusterAlertHistory;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceRoleState;
import com.datasophon.dao.enums.ServiceState;
import com.mybatisflex.core.query.QueryChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

/**
 * 服务状态管理服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service
public class ServiceStateManagementServiceImpl implements ServiceStateManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceStateManagementServiceImpl.class);

    @Autowired
    private ClusterServiceRoleInstanceService serviceRoleInstanceService;

    @Autowired
    private ClusterAlertHistoryService alertHistoryService;

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private ClusterServiceRoleInstanceConverter serviceRoleInstanceConverter;

    @Override
    public void updateServiceRoleState(CommandType commandType, String serviceRoleName, String hostname,
            Integer clusterId, ServiceRoleState serviceRoleState) {
        ClusterServiceRoleInstanceDTO serviceRoleDTO = serviceRoleInstanceService.getOneServiceRole(serviceRoleName,
                hostname, clusterId);
        ClusterServiceRoleInstanceEntity serviceRole = serviceRoleInstanceConverter.dtoToEntity(serviceRoleDTO);
        serviceRole.setServiceRoleState(serviceRoleState);
        serviceRole.setServiceRoleStateCode(serviceRoleState.getValue());
        if (commandType != CommandType.STOP_SERVICE) {
            serviceRole.setNeedRestart(NeedRestart.NO);
        }
        serviceRoleInstanceService.updateById(serviceRole);
    }

    @Override
    public void saveAlert(ClusterServiceRoleInstanceEntity roleInstanceEntity, String alertTargetName,
            AlertLevel alertLevel, String alertAdvice) {
        ClusterAlertHistory clusterAlertHistory = QueryChain.of(ClusterAlertHistory.class)
                .where(ClusterAlertHistory::getAlertTargetName).eq(alertTargetName)
                .and(ClusterAlertHistory::getClusterId).eq(roleInstanceEntity.getClusterId())
                .and(ClusterAlertHistory::getHostname).eq(roleInstanceEntity.getHostname())
                .and(ClusterAlertHistory::getIsEnabled).eq(1)
                .one();

        ClusterServiceInstanceEntity serviceInstanceEntity = serviceInstanceService
                .getById(roleInstanceEntity.getServiceId());
        if (Objects.isNull(clusterAlertHistory)) {
            clusterAlertHistory = ClusterAlertHistory.builder()
                    .clusterId(roleInstanceEntity.getClusterId())
                    .alertGroupName(roleInstanceEntity.getServiceName().toLowerCase())
                    .alertTargetName(alertTargetName)
                    .createTime(new Date())
                    .updateTime(new Date())
                    .alertLevel(alertLevel)
                    .alertInfo("")
                    .alertAdvice(alertAdvice)
                    .hostname(roleInstanceEntity.getHostname())
                    .serviceRoleInstanceId(roleInstanceEntity.getId())
                    .serviceInstanceId(roleInstanceEntity.getServiceId())
                    .isEnabled(1)
                    .serviceInstanceId(roleInstanceEntity.getServiceId())
                    .build();

            alertHistoryService.save(clusterAlertHistory);
        }
        // update service role instance state
        serviceInstanceEntity.setServiceState(ServiceState.EXISTS_EXCEPTION);
        roleInstanceEntity.setServiceRoleState(ServiceRoleState.STOP);
        if (alertLevel == AlertLevel.WARN) {
            serviceInstanceEntity.setServiceState(ServiceState.EXISTS_ALARM);
            roleInstanceEntity.setServiceRoleState(ServiceRoleState.EXISTS_ALARM);
        }
        serviceInstanceService.updateById(serviceInstanceEntity);
        serviceRoleInstanceService.updateById(roleInstanceEntity);
    }

    @Override
    public void recoverAlert(ClusterServiceRoleInstanceEntity roleInstanceEntity) {
        ClusterAlertHistory clusterAlertHistory = QueryChain.of(ClusterAlertHistory.class)
                .where(ClusterAlertHistory::getAlertTargetName).eq(roleInstanceEntity.getServiceRoleName() + " Survive")
                .and(ClusterAlertHistory::getClusterId).eq(roleInstanceEntity.getClusterId())
                .and(ClusterAlertHistory::getHostname).eq(roleInstanceEntity.getHostname())
                .and(ClusterAlertHistory::getIsEnabled).eq(1)
                .one();

        if (Objects.nonNull(clusterAlertHistory)) {
            clusterAlertHistory.setIsEnabled(2);
            alertHistoryService.updateById(clusterAlertHistory);
        }
        // update service role instance state
        if (roleInstanceEntity.getServiceRoleState() != ServiceRoleState.RUNNING) {
            roleInstanceEntity.setServiceRoleState(ServiceRoleState.RUNNING);
            serviceRoleInstanceService.updateById(roleInstanceEntity);
        }
    }
}