package com.datasophon.api.master.alert;

import com.datasophon.common.enums.ServiceState;
import org.apache.pekko.actor.AbstractActor;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.service.ClusterAlertHistoryService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.converter.ClusterServiceInstanceConverter;
import com.datasophon.dao.entity.ClusterAlertHistory;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.AlertLevel;
import com.datasophon.common.enums.HostState;
import com.datasophon.common.enums.ServiceRoleState;

import com.datasophon.api.alert.gateway.AlertHistoryGateway;
import com.datasophon.api.alert.model.AlertHistory;
import com.datasophon.api.alert.model.AlertLabels;
import com.datasophon.api.alert.model.AlertMessage;
import com.datasophon.api.alert.model.Alerts;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 告警处理Actor
 * 负责处理集群告警消息，更新主机和服务状态，记录告警历史
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-05
 */
public class AlertActor extends AbstractActor {

    private static final String FIRING = "firing";

    private static final String NODE = "node";

    private static final String WARNING = "warning";

    private static final String EXCEPTION = "exception";

    private static final String RESOLVED = "resolved";

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, this::handleAlertMessage)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleAlertMessage(String alertMessage) {
        try {
            AlertMessage alertMes = JSONObject.parseObject(alertMessage, AlertMessage.class);
            AlertHistoryGateway alertHistoryGateway = SpringUtil.getBean(AlertHistoryGateway.class);
            ClusterHostService hostService = SpringUtil.getBean(ClusterHostService.class);
            ClusterAlertHistoryService alertHistoryService = SpringUtil.getBean(ClusterAlertHistoryService.class);
            ClusterServiceInstanceService serviceInstanceService = SpringUtil
                    .getBean(ClusterServiceInstanceService.class);
            ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                    .getBean(ClusterServiceRoleInstanceService.class);

            List<Alerts> alerts = alertMes.getAlerts();
            for (Alerts alertInfo : alerts) {
                AlertLabels labels = alertInfo.getLabels();
                String alertname = labels.getAlertname();
                int clusterId = labels.getClusterId();
                String instance = labels.getInstance();
                String status = alertInfo.getStatus();
                String hostname = instance.split(":")[0];
                String serviceRoleName = labels.getServiceRoleName();
                if (FIRING.equals(status)) {
                    boolean hasEnabledAlertHistory = alertHistoryGateway.hasEnabledAlertHistory(alertname, clusterId,
                            hostname);
                    // 查询服务实例，服务角色实例
                    if (NODE.equals(serviceRoleName)) {
                        ClusterHostDO clusterHost = hostService.getClusterHostByHostname(hostname);
                        clusterHost.setHostState(
                                EXCEPTION.equals(labels.getSeverity()) ? HostState.OFFLINE : HostState.EXISTS_ALARM);
                        if (!hasEnabledAlertHistory) {
                            ClusterAlertHistory clusterAlertHistory = ClusterAlertHistory.builder()
                                    .clusterId(clusterId)
                                    .alertGroupName(labels.getJob())
                                    .alertTargetName(alertname)
                                    .createTime(new Date())
                                    .updateTime(new Date())
                                    .alertLevel(WARNING.equals(labels.getSeverity()) ? AlertLevel.WARN
                                            : AlertLevel.EXCEPTION)
                                    .alertInfo(alertInfo.getAnnotations().getDescription())
                                    .alertAdvice(alertInfo.getAnnotations().getSummary())
                                    .hostname(hostname)
                                    .isEnabled(1)
                                    .build();
                            alertHistoryService.save(clusterAlertHistory);
                        }
                        hostService.updateById(clusterHost);
                    } else {
                        ClusterServiceRoleInstanceDTO roleInstance = roleInstanceService
                                .getOneServiceRole(serviceRoleName, hostname, clusterId);
                        if (Objects.nonNull(roleInstance)) {
                            ClusterServiceInstanceConverter serviceInstanceConverter = SpringUtil
                                    .getBean(ClusterServiceInstanceConverter.class);
                            ClusterServiceInstanceDTO serviceInstance = serviceInstanceConverter
                                    .entityToDto(serviceInstanceService.getById(roleInstance.serviceId()));
                            // 更新服务实例状态为告警
                            serviceInstanceService.updateServiceInstanceState(serviceInstance.id(), 
                                    ServiceState.EXISTS_ALARM);
                            // 更新服务角色实例状态为告警
                            roleInstanceService.updateServiceRoleInstanceState(roleInstance.id(), 
                                    ServiceRoleState.EXISTS_ALARM);
                            if (!hasEnabledAlertHistory) {
                                ClusterAlertHistory clusterAlertHistory = ClusterAlertHistory.builder()
                                        .clusterId(clusterId)
                                        .alertGroupName(labels.getJob())
                                        .alertTargetName(alertname)
                                        .serviceInstanceId(serviceInstance.id())
                                        .serviceRoleInstanceId(roleInstance.id())
                                        .createTime(new Date())
                                        .updateTime(new Date())
                                        .alertLevel(WARNING.equals(labels.getSeverity()) ? AlertLevel.WARN
                                                : AlertLevel.EXCEPTION)
                                        .alertInfo(alertInfo.getAnnotations().getDescription())
                                        .alertAdvice(alertInfo.getAnnotations().getSummary())
                                        .hostname(hostname)
                                        .isEnabled(1)
                                        .build();

                                alertHistoryService.save(clusterAlertHistory);
                            }
                            if (EXCEPTION.equals(labels.getSeverity())) {
                                // 异常告警，更新为异常状态
                                serviceInstanceService.updateServiceInstanceState(serviceInstance.id(), 
                                        ServiceState.EXISTS_EXCEPTION);
                                roleInstanceService.updateServiceRoleInstanceState(roleInstance.id(), 
                                        ServiceRoleState.STOP);
                            }
                        }
                    }

                }
                if (RESOLVED.equals(status)) {
                    AlertHistory alertHistory = alertHistoryGateway.getEnabledAlertHistory(alertname, clusterId,
                            hostname);
                    if (Objects.nonNull(alertHistory)) {
                        boolean nodeHasWarnAlertList = alertHistoryGateway.nodeHasWarnAlertList(hostname,
                                serviceRoleName, alertHistory.getId());

                        if (EXCEPTION.equals(labels.getSeverity())) {// 异常告警处理
                            if (NODE.equals(serviceRoleName)) {
                                // 置为正常
                                ClusterHostDO clusterHost = hostService.getClusterHostByHostname(hostname);
                                clusterHost.setHostState(
                                        nodeHasWarnAlertList ? HostState.EXISTS_ALARM : HostState.RUNNING);
                                hostService.updateById(clusterHost);
                            } else {
                                // 查询服务角色实例
                                ClusterServiceRoleInstanceDTO roleInstance = roleInstanceService
                                        .getOneServiceRole(labels.getServiceRoleName(), hostname, clusterId);
                                if (!Objects.equals(ServiceRoleState.RUNNING.getValue(), roleInstance.serviceRoleState())) {
                                    // 恢复服务角色状态
                                    ServiceRoleState newState = nodeHasWarnAlertList ? 
                                            ServiceRoleState.EXISTS_ALARM : ServiceRoleState.RUNNING;
                                    roleInstanceService.updateServiceRoleInstanceState(roleInstance.id(), newState);
                                }
                            }
                        } else {
                            // 警告告警处理
                            if (NODE.equals(serviceRoleName)) {
                                // 置为正常
                                ClusterHostDO clusterHost = hostService.getClusterHostByHostname(hostname);
                                clusterHost.setHostState(
                                        nodeHasWarnAlertList ? HostState.EXISTS_ALARM : HostState.RUNNING);
                                hostService.updateById(clusterHost);
                            } else {
                                // 查询服务角色实例
                                ClusterServiceRoleInstanceDTO roleInstance = roleInstanceService
                                        .getOneServiceRole(labels.getServiceRoleName(), hostname, clusterId);
                                if (!Objects.equals(ServiceRoleState.RUNNING.getValue(), roleInstance.serviceRoleState())) {
                                    // 恢复服务角色状态（警告告警处理）
                                    ServiceRoleState newState = nodeHasWarnAlertList ? 
                                            ServiceRoleState.EXISTS_ALARM : ServiceRoleState.RUNNING;
                                    roleInstanceService.updateServiceRoleInstanceState(roleInstance.id(), newState);
                                }
                            }
                        }
                        alertHistoryGateway.updateAlertHistoryToDisabled(alertHistory.getId());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("处理告警消息时出错", e);
        }
    }
}
