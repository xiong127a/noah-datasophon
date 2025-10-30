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

import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.alert.gateway.AlertHistoryGateway;
import com.datasophon.api.alert.model.AlertHistory;
import com.datasophon.api.alert.model.AlertLabels;
import com.datasophon.api.alert.model.AlertMessage;
import com.datasophon.api.alert.model.Alerts;
import com.datasophon.api.converter.ClusterServiceInstanceConverter;
import com.datasophon.api.service.*;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.AlertLevel;
import com.datasophon.common.enums.HostState;
import com.datasophon.common.enums.ServiceRoleState;
import com.datasophon.common.enums.ServiceState;
import com.datasophon.dao.entity.ClusterAlertHistoryEntity;
import com.datasophon.dao.entity.ClusterHostEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 告警处理服务实现
 * 替代AlertActor，使用Spring Service实现
 */
@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertServiceImpl.class);

    private static final String FIRING = "firing";
    private static final String NODE = "node";
    private static final String WARNING = "warning";
    private static final String EXCEPTION = "exception";
    private static final String RESOLVED = "resolved";

    @Autowired
    private AlertHistoryGateway alertHistoryGateway;
    
    @Autowired
    private ClusterHostService hostService;
    
    @Autowired
    private ClusterAlertHistoryService alertHistoryService;
    
    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;
    
    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;
    
    @Autowired
    private ClusterServiceInstanceConverter serviceInstanceConverter;

    @Override
    @Async("taskExecutor")
    public void handleAlertMessage(String alertMessage) {
        try {
            AlertMessage alertMes = JSONObject.parseObject(alertMessage, AlertMessage.class);
            List<Alerts> alerts = alertMes.getAlerts();
            
            for (Alerts alertInfo : alerts) {
                processAlert(alertInfo);
            }
        } catch (Exception e) {
            logger.error("处理告警消息时出错", e);
            throw new RuntimeException("处理告警消息时出错", e);
        }
    }

    private void processAlert(Alerts alertInfo) {
        AlertLabels labels = alertInfo.getLabels();
        String alertname = labels.getAlertname();
        Long clusterId = labels.getClusterId();
        String instance = labels.getInstance();
        String status = alertInfo.getStatus();
        String hostname = instance.split(":")[0];
        String serviceRoleName = labels.getServiceRoleName();
        
        if (FIRING.equals(status)) {
            handleFiringAlert(alertInfo, labels, alertname, clusterId, hostname, serviceRoleName);
        } else if (RESOLVED.equals(status)) {
            handleResolvedAlert(labels, alertname, clusterId, hostname, serviceRoleName);
        }
    }

    private void handleFiringAlert(Alerts alertInfo, AlertLabels labels, 
                                  String alertname, Long clusterId, 
                                  String hostname, String serviceRoleName) {
        boolean hasEnabledAlertHistory = alertHistoryGateway.hasEnabledAlertHistory(
                alertname, clusterId, hostname);
        
        if (NODE.equals(serviceRoleName)) {
            handleNodeAlert(alertInfo, labels, clusterId, hostname, hasEnabledAlertHistory, alertname);
        } else {
            handleServiceRoleAlert(alertInfo, labels, clusterId, hostname, 
                    serviceRoleName, hasEnabledAlertHistory, alertname);
        }
    }

    private void handleNodeAlert(Alerts alertInfo, AlertLabels labels, 
                                Long clusterId, String hostname, 
                                boolean hasEnabledAlertHistory, String alertname) {
        ClusterHostEntity clusterHost = hostService.getClusterHostByHostname(hostname);
        clusterHost.setHostState(
                EXCEPTION.equals(labels.getSeverity()) ? HostState.OFFLINE : HostState.EXISTS_ALARM);
        
        if (!hasEnabledAlertHistory) {
            ClusterAlertHistoryEntity clusterAlertHistoryEntity = ClusterAlertHistoryEntity.builder()
                    .clusterId(clusterId)
                    .alertGroupName(labels.getJob())
                    .alertTargetName(alertname)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .alertLevel(WARNING.equals(labels.getSeverity()) ? 
                            AlertLevel.WARN : AlertLevel.EXCEPTION)
                    .alertInfo(alertInfo.getAnnotations().getDescription())
                    .alertAdvice(alertInfo.getAnnotations().getSummary())
                    .hostname(hostname)
                    .isEnabled(1)
                    .build();
            alertHistoryService.save(clusterAlertHistoryEntity);
        }
        hostService.updateById(clusterHost);
    }

    private void handleServiceRoleAlert(Alerts alertInfo, AlertLabels labels, 
                                       Long clusterId, String hostname, 
                                       String serviceRoleName, boolean hasEnabledAlertHistory,
                                       String alertname) {
        ClusterServiceRoleInstanceDTO roleInstance = roleInstanceService
                .getOneServiceRole(serviceRoleName, hostname, clusterId);
        
        if (Objects.isNull(roleInstance)) {
            return;
        }
        
        ClusterServiceInstanceDTO serviceInstance = serviceInstanceConverter
                .entityToDto(serviceInstanceService.getById(roleInstance.serviceId()));
        
        // 更新服务实例状态为告警
        serviceInstanceService.updateServiceInstanceState(
                serviceInstance.id(), ServiceState.EXISTS_ALARM);
        // 更新服务角色实例状态为告警
        roleInstanceService.updateServiceRoleInstanceState(
                roleInstance.id(), ServiceRoleState.EXISTS_ALARM);
        
        if (!hasEnabledAlertHistory) {
            ClusterAlertHistoryEntity clusterAlertHistoryEntity = ClusterAlertHistoryEntity.builder()
                    .clusterId(clusterId)
                    .alertGroupName(labels.getJob())
                    .alertTargetName(alertname)
                    .serviceInstanceId(serviceInstance.id())
                    .serviceRoleInstanceId(roleInstance.id())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .alertLevel(WARNING.equals(labels.getSeverity()) ? 
                            AlertLevel.WARN : AlertLevel.EXCEPTION)
                    .alertInfo(alertInfo.getAnnotations().getDescription())
                    .alertAdvice(alertInfo.getAnnotations().getSummary())
                    .hostname(hostname)
                    .isEnabled(1)
                    .build();
            
            alertHistoryService.save(clusterAlertHistoryEntity);
        }
        
        if (EXCEPTION.equals(labels.getSeverity())) {
            // 异常告警，更新为异常状态
            serviceInstanceService.updateServiceInstanceState(
                    serviceInstance.id(), ServiceState.EXISTS_EXCEPTION);
            roleInstanceService.updateServiceRoleInstanceState(
                    roleInstance.id(), ServiceRoleState.STOP);
        }
    }

    private void handleResolvedAlert(AlertLabels labels, String alertname, 
                                    Long clusterId, String hostname, 
                                    String serviceRoleName) {
        AlertHistory alertHistory = alertHistoryGateway.getEnabledAlertHistory(
                alertname, clusterId, hostname);
        
        if (Objects.isNull(alertHistory)) {
            return;
        }
        
        boolean nodeHasWarnAlertList = alertHistoryGateway.nodeHasWarnAlertList(
                hostname, serviceRoleName, alertHistory.getId());
        
        if (EXCEPTION.equals(labels.getSeverity())) {
            handleResolvedExceptionAlert(hostname, serviceRoleName, clusterId, 
                    labels, nodeHasWarnAlertList);
        } else {
            handleResolvedWarningAlert(hostname, serviceRoleName, clusterId, 
                    labels, nodeHasWarnAlertList);
        }
        
        alertHistoryGateway.updateAlertHistoryToDisabled(alertHistory.getId());
    }

    private void handleResolvedExceptionAlert(String hostname, String serviceRoleName, 
                                            Long clusterId, AlertLabels labels,
                                            boolean nodeHasWarnAlertList) {
        if (NODE.equals(serviceRoleName)) {
            ClusterHostEntity clusterHost = hostService.getClusterHostByHostname(hostname);
            clusterHost.setHostState(
                    nodeHasWarnAlertList ? HostState.EXISTS_ALARM : HostState.RUNNING);
            hostService.updateById(clusterHost);
        } else {
            ClusterServiceRoleInstanceDTO roleInstance = roleInstanceService
                    .getOneServiceRole(labels.getServiceRoleName(), hostname, clusterId);
            
            if (!Objects.equals(ServiceRoleState.RUNNING.getValue(), 
                    roleInstance.serviceRoleState())) {
                ServiceRoleState newState = nodeHasWarnAlertList ? 
                        ServiceRoleState.EXISTS_ALARM : ServiceRoleState.RUNNING;
                roleInstanceService.updateServiceRoleInstanceState(roleInstance.id(), newState);
            }
        }
    }

    private void handleResolvedWarningAlert(String hostname, String serviceRoleName, 
                                          Long clusterId, AlertLabels labels,
                                          boolean nodeHasWarnAlertList) {
        if (NODE.equals(serviceRoleName)) {
            ClusterHostEntity clusterHost = hostService.getClusterHostByHostname(hostname);
            clusterHost.setHostState(
                    nodeHasWarnAlertList ? HostState.EXISTS_ALARM : HostState.RUNNING);
            hostService.updateById(clusterHost);
        } else {
            ClusterServiceRoleInstanceDTO roleInstance = roleInstanceService
                    .getOneServiceRole(labels.getServiceRoleName(), hostname, clusterId);
            
            if (!Objects.equals(ServiceRoleState.RUNNING.getValue(), 
                    roleInstance.serviceRoleState())) {
                ServiceRoleState newState = nodeHasWarnAlertList ? 
                        ServiceRoleState.EXISTS_ALARM : ServiceRoleState.RUNNING;
                roleInstanceService.updateServiceRoleInstanceState(roleInstance.id(), newState);
            }
        }
    }
}

