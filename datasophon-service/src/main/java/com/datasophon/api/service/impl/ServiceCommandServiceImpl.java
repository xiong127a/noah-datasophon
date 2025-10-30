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

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.*;
import com.datasophon.common.command.GeneratePrometheusConfigCommand;
import com.datasophon.common.command.GenerateSRPromConfigCommand;
import com.datasophon.common.command.HdfsEcCommand;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceCommandDTO;
import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.common.dto.ClusterServiceCommandHostDTO;
import com.datasophon.common.enums.ClusterState;
import com.datasophon.common.enums.CommandState;
import com.datasophon.common.model.UpdateCommandHostMessage;
import com.datasophon.dao.entity.ClusterAlertQuotaEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 服务命令处理服务实现
 * 替代ServiceCommandActor，使用Spring Service实现
 */
@Service
public class ServiceCommandServiceImpl implements ServiceCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCommandServiceImpl.class);

    private static final String STARROCKS = "starrocks";
    private static final String DORIS = "doris";
    private static final String HDFS = "hdfs";
    private static final String ENABLE_HDFS_KERBEROS = "${enableHDFSKerberos}";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String NODE = "NODE";

    @Autowired
    private ClusterInfoService clusterInfoService;
    
    @Autowired
    private ClusterServiceCommandHostCommandService hostCommandService;
    
    @Autowired
    private ClusterServiceCommandHostService commandHostService;
    
    @Autowired
    private ClusterServiceCommandService commandService;
    
    @Autowired
    private ClusterServiceRoleInstanceWebuisService webuisService;
    
    @Autowired
    private ClusterAlertQuotaService alertQuotaService;
    
    @Autowired(required = false)
    private HdfsEcService hdfsEcService;
    
    @Autowired(required = false)
    private PrometheusIntegrationService prometheusService;

    @Override
    @Async("taskExecutor")
    public void handleUpdateCommandHostMessage(UpdateCommandHostMessage message) {
        try {
            // 更新主机命令进度
            long size = hostCommandService.getHostCommandSizeByHostnameAndCommandHostId(
                    message.getHostname(), message.getCommandHostId());
            Integer totalProgress = hostCommandService.getHostCommandTotalProgressByHostnameAndCommandHostId(
                    message.getHostname(), message.getCommandHostId());
            long progress = totalProgress / size;
            
            commandHostService.updateCommandHostProgress(message.getCommandHostId(), progress);

            if (progress == 100) {
                updateCommandHostStateOnComplete(message);
            }

            // 更新整体命令进度
            updateOverallCommandProgress(message);

        } catch (Exception e) {
            logger.error("处理UpdateCommandHostMessage消息时出错", e);
        }
    }

    private void updateCommandHostStateOnComplete(UpdateCommandHostMessage message) {
        List<ClusterServiceCommandHostCommandDTO> failedList = hostCommandService
                .findFailedHostCommand(message.getHostname(), message.getCommandHostId());
        List<ClusterServiceCommandHostCommandDTO> cancelList = hostCommandService
                .findCanceledHostCommand(message.getHostname(), message.getCommandHostId());

        CommandState newState;
        if (!failedList.isEmpty()) {
            newState = CommandState.FAILED;
            logger.info("主机命令 {} 包含失败子命令，状态设为失败", message.getCommandHostId());
        } else if (!cancelList.isEmpty()) {
            newState = CommandState.CANCEL;
            logger.info("主机命令 {} 包含取消子命令，状态设为取消", message.getCommandHostId());
        } else {
            newState = CommandState.SUCCESS;
            logger.info("主机命令 {} 所有子命令成功，状态设为成功", message.getCommandHostId());
        }
        
        commandHostService.updateCommandHostState(message.getCommandHostId(), newState);
    }

    private void updateOverallCommandProgress(UpdateCommandHostMessage message) {
        Long size1 = commandHostService.getCommandHostSizeByCommandId(message.getCommandId());
        Integer totalProgress1 = commandHostService.getCommandHostTotalProgressByCommandId(
                message.getCommandId());
        long progress1 = totalProgress1 / size1;
        ClusterServiceCommandDTO command = commandService.getCommandById(message.getCommandId());

        commandService.updateCommandProgress(message.getCommandId(), progress1);

        if (progress1 == 100) {
            handleCommandComplete(message, command);
        }
    }

    private void handleCommandComplete(UpdateCommandHostMessage message, ClusterServiceCommandDTO command) {
        List<ClusterServiceCommandHostDTO> failedHosts = commandHostService
                .findFailedCommandHost(message.getCommandId());
        List<ClusterServiceCommandHostDTO> canceledHosts = commandHostService
                .findCanceledCommandHost(message.getCommandId());

        CommandState newCommandState;
        if (!failedHosts.isEmpty()) {
            newCommandState = CommandState.FAILED;
            logger.info("命令 {} 包含失败主机命令，状态设为失败", message.getCommandId());
        } else if (!canceledHosts.isEmpty()) {
            newCommandState = CommandState.CANCEL;
            logger.info("命令 {} 包含取消主机命令，状态设为取消", message.getCommandId());
        } else {
            newCommandState = CommandState.SUCCESS;
            logger.info("命令 {} 所有主机命令成功，状态设为成功", message.getCommandId());
        }

        commandService.updateCommandStateAndEndTime(message.getCommandId(), 
                newCommandState, LocalDateTime.now());

        String serviceName = command.serviceName();
        ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(command.clusterId());

        handlePostCommandActions(command, serviceName, clusterInfo);
    }

    private void handlePostCommandActions(ClusterServiceCommandDTO command, 
                                         String serviceName, ClusterInfoDTO clusterInfo) {
        // commandType : 1：安装服务 2：启动服务 3：停止服务 4：重启服务 5：更新配置后启动 6：更新配置后重启
        if (command.commandType() == 4 && HDFS.equalsIgnoreCase(serviceName)) {
            updateHDFSWebUi(clusterInfo.id(), command.serviceInstanceId());
        }

        // 更新集群状态
        if (command.commandType() == 1) {
            if (ClusterState.NEED_CONFIG.getValue() == clusterInfo.clusterState()) {
                clusterInfoService.updateClusterState(clusterInfo.id(), ClusterState.RUNNING.getValue());
            }
        }

        // 处理服务安装后的操作
        if (command.commandType() == 1) {
            handleServiceInstallComplete(command, serviceName, clusterInfo);
        }
    }

    private void handleServiceInstallComplete(ClusterServiceCommandDTO command,
                                            String serviceName, ClusterInfoDTO clusterInfo) {
        // HDFS纠删码配置
        if (HDFS.equalsIgnoreCase(serviceName) && hdfsEcService != null) {
            HdfsEcCommand hdfsEcCommand = new HdfsEcCommand();
            hdfsEcCommand.setServiceInstanceId(command.serviceInstanceId());
            hdfsEcService.handleHdfsEcCommand(hdfsEcCommand);
        }

        // 生成Prometheus配置
        logger.info("开始生成Prometheus配置");
        if (prometheusService != null) {
            if (STARROCKS.equalsIgnoreCase(serviceName) || DORIS.equalsIgnoreCase(serviceName)) {
                GenerateSRPromConfigCommand prometheusConfigCommand = new GenerateSRPromConfigCommand();
                prometheusConfigCommand.setServiceInstanceId(command.serviceInstanceId());
                prometheusConfigCommand.setClusterFrame(clusterInfo.clusterFrame());
                prometheusConfigCommand.setClusterId(clusterInfo.id());
                prometheusConfigCommand.setFilename(serviceName.toLowerCase() + ".json");
                prometheusService.generateStarRocksPrometheusConfig(prometheusConfigCommand);
            } else {
                GeneratePrometheusConfigCommand prometheusConfigCommand = new GeneratePrometheusConfigCommand();
                prometheusConfigCommand.setServiceInstanceId(command.serviceInstanceId());
                prometheusConfigCommand.setClusterFrame(clusterInfo.clusterFrame());
                prometheusConfigCommand.setClusterId(clusterInfo.id());
                prometheusService.generatePrometheusConfig(prometheusConfigCommand);
                enableAlertConfig(NODE, clusterInfo.id());
            }
            enableAlertConfig(serviceName, clusterInfo.id());
        }
    }

    private void enableAlertConfig(String serviceName, Long clusterId) {
        List<ClusterAlertQuotaEntity> list = alertQuotaService.listAlertQuotaByServiceName(serviceName);
        var ids = list.stream().map(ClusterAlertQuotaEntity::getId).toList();
        String alertQuotaIds = StringUtils.join(ids, ",");
        alertQuotaService.start(clusterId, alertQuotaIds);
    }

    private void updateHDFSWebUi(Long clusterId, Long serviceInstanceId) {
        Map<String, String> variables = GlobalVariables.get(clusterId);
        if (!variables.containsKey(ENABLE_HDFS_KERBEROS)) {
            return;
        }

        var webUiDTOs = webuisService.listWebUisByServiceInstanceId(serviceInstanceId);
        for (var webUiDTO : webUiDTOs) {
            String webUrl = webUiDTO.webUrl();
            String newWebUi = null;

            if (TRUE.equals(variables.get(ENABLE_HDFS_KERBEROS)) && webUrl.contains("9870")) {
                newWebUi = webUrl.replace(HTTP, HTTPS).replace("9870", "9871");
            } else if (FALSE.equals(variables.get(ENABLE_HDFS_KERBEROS)) && webUrl.contains("9871")) {
                newWebUi = webUrl.replace(HTTPS, HTTP).replace("9871", "9870");
            }

            if (newWebUi != null) {
                var webuisConverter = org.springframework.beans.factory.BeanFactoryUtils
                        .beanOfType(org.springframework.context.ApplicationContext.class,
                                com.datasophon.api.converter.ClusterServiceRoleInstanceWebuisConverter.class);
                if (webuisConverter != null) {
                    var webuisEntity = webuisConverter.dtoToEntity(webUiDTO);
                    webuisEntity.setWebUrl(newWebUi);
                    var updatedDTO = webuisConverter.entityToDto(webuisEntity);
                    webuisService.updateWebUI(updatedDTO);
                }
            }
        }
    }
}

