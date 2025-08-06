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

package com.datasophon.api.master;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterAlertQuotaService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.ClusterServiceCommandHostService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.common.command.GeneratePrometheusConfigCommand;
import com.datasophon.common.command.GenerateSRPromConfigCommand;
import com.datasophon.common.command.HdfsEcCommand;
import com.datasophon.common.model.UpdateCommandHostMessage;
import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceCommandDTO;
import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.common.dto.ClusterServiceCommandHostDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceWebuisDTO;
import com.datasophon.dao.enums.ClusterState;
import com.datasophon.dao.enums.CommandState;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
// 移除未使用的import，因为已使用JDK21的toList()方法

/**
 * 服务命令处理Actor
 * 负责处理服务命令的执行状态更新、监控配置生成等操作
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-05
 */
public class ServiceCommandActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCommandActor.class);

    private static final String STARROCKS = "starrocks";

    private static final String DORIS = "doris";

    private static final String HDFS = "hdfs";

    private static final String ENABLE_HDFS_KERBEROS = "${enableHDFSKerberos}";

    private static final String TRUE = "true";

    private static final String FALSE = "false";

    private static final String HTTP = "http";

    private static final String HTTPS = "https";

    private static final String NODE = "NODE";

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(UpdateCommandHostMessage.class, this::handleUpdateCommandHostMessage)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleUpdateCommandHostMessage(UpdateCommandHostMessage message) {
        try {
            ClusterInfoService clusterInfoService = SpringUtil
                    .getBean(ClusterInfoService.class);
            ClusterServiceCommandHostCommandService service = SpringUtil
                    .getBean(ClusterServiceCommandHostCommandService.class);
            ClusterServiceCommandHostService commandHostService = SpringUtil
                    .getBean(ClusterServiceCommandHostService.class);
            ClusterServiceCommandService commandService = SpringUtil
                    .getBean(ClusterServiceCommandService.class);

            // 获取命令主机信息（用于后续扩展功能）
            ClusterServiceCommandHostDTO commandHost = commandHostService
                    .getCommandHostByCommandHostId(message.getCommandHostId());

            long size = service.getHostCommandSizeByHostnameAndCommandHostId(message.getHostname(),
                    message.getCommandHostId());
            Integer totalProgress = service.getHostCommandTotalProgressByHostnameAndCommandHostId(message.getHostname(),
                    message.getCommandHostId());
            long progress = totalProgress / size;
            
            commandHostService.updateCommandHostProgress(message.getCommandHostId(), progress);

            if (progress == 100) {
                List<ClusterServiceCommandHostCommandDTO> failedList = service
                        .findFailedHostCommand(message.getHostname(), message.getCommandHostId());
                List<ClusterServiceCommandHostCommandDTO> cancelList = service
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

            Long size1 = commandHostService.getCommandHostSizeByCommandId(message.getCommandId());
            Integer totalProgress1 = commandHostService.getCommandHostTotalProgressByCommandId(message.getCommandId());
            long progress1 = totalProgress1 / size1;
            ClusterServiceCommandDTO command = commandService.getCommandById(message.getCommandId());

            commandService.updateCommandProgress(message.getCommandId(), progress1);

            if (progress1 == 100) {
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

                commandService.updateCommandStateAndEndTime(message.getCommandId(), newCommandState, new Date());

                String serviceName = command.serviceName();
                ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(command.clusterId());

                // commandType : 1：安装服务 2：启动服务 3：停止服务 4：重启服务 5：更新配置后启动 6：更新配置后重启
                if (command.commandType() == 4 && HDFS.equalsIgnoreCase(serviceName)) {
                    // update web ui
                    updateHDFSWebUi(clusterInfo.id(), command.serviceInstanceId());
                }

                // update cluster state
                if (command.commandType() == 1) {

                    if (ClusterState.NEED_CONFIG.getValue()==(clusterInfo.clusterState())) {
                        clusterInfoService.updateClusterState(clusterInfo.id(), ClusterState.RUNNING.getValue());
                    }

                    if (HDFS.equalsIgnoreCase(serviceName)) {
                        ActorRef hdfsECActor = ActorUtils.getLocalActor(HdfsECActor.class,
                                ActorUtils.getActorRefName(HdfsECActor.class));
                        HdfsEcCommand hdfsEcCommand = new HdfsEcCommand();
                        hdfsEcCommand.setServiceInstanceId(command.serviceInstanceId());
                        hdfsECActor.tell(hdfsEcCommand, getSelf());

                    }
                    logger.info("start to generate prometheus config");
                    ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,
                            ActorUtils.getActorRefName(PrometheusActor.class));
                    if (STARROCKS.equalsIgnoreCase(serviceName) || DORIS.equalsIgnoreCase(serviceName)) {
                        GenerateSRPromConfigCommand prometheusConfigCommand = new GenerateSRPromConfigCommand();
                        prometheusConfigCommand.setServiceInstanceId(command.serviceInstanceId());
                        prometheusConfigCommand.setClusterFrame(clusterInfo.clusterFrame());
                        prometheusConfigCommand.setClusterId(clusterInfo.id());
                        prometheusConfigCommand.setFilename(serviceName.toLowerCase() + ".json");
                        prometheusActor.tell(prometheusConfigCommand, getSelf());
                    } else {
                        GeneratePrometheusConfigCommand prometheusConfigCommand = new GeneratePrometheusConfigCommand();
                        prometheusConfigCommand.setServiceInstanceId(command.serviceInstanceId());
                        prometheusConfigCommand.setClusterFrame(clusterInfo.clusterFrame());
                        prometheusConfigCommand.setClusterId(clusterInfo.id());
                        prometheusActor.tell(prometheusConfigCommand, getSelf());
                        enableAlertConfig(NODE, clusterInfo.id());
                    }
                    enableAlertConfig(serviceName, clusterInfo.id());
                }
            }
            // 命令已通过上面的方法更新，这里不需要重复更新
        } catch (Exception e) {
            logger.error("处理UpdateCommandHostMessage消息时出错", e);
        }
    }

    private void enableAlertConfig(String serviceName, Integer clusterId) {
        ClusterAlertQuotaService alertQuotaService = SpringUtil
                .getBean(ClusterAlertQuotaService.class);
        List<ClusterAlertQuota> list = alertQuotaService.listAlertQuotaByServiceName(serviceName);
        var ids = list.stream().map(ClusterAlertQuota::getId).toList(); // JDK21特性
        String alertQuotaIds = StringUtils.join(ids, ",");
        alertQuotaService.start(clusterId, alertQuotaIds);
    }

    private void updateHDFSWebUi(Integer clusterId, Integer serviceInstanceId) {
        Map<String, String> variables = GlobalVariables.get(clusterId);
        if (variables.containsKey(ENABLE_HDFS_KERBEROS)) {
            ClusterServiceRoleInstanceWebuisService webuisService = SpringUtil
                    .getBean(ClusterServiceRoleInstanceWebuisService.class);
            var webUiDTOs = webuisService.listWebUisByServiceInstanceId(serviceInstanceId); // JDK21特性
            for (var webUiDTO : webUiDTOs) {
                if (TRUE.equals(variables.get(ENABLE_HDFS_KERBEROS)) && webUiDTO.webUrl().contains("9870")) {
                    var newWebUi = webUiDTO.webUrl().replace(HTTP, HTTPS).replace("9870", "9871");
                    // 创建更新后的DTO - JDK21 Record特性
                    var updatedDTO = new ClusterServiceRoleInstanceWebuisDTO(
                        webUiDTO.id(),
                        webUiDTO.serviceRoleInstanceId(),
                        newWebUi,
                        webUiDTO.serviceInstanceId(),
                        webUiDTO.name()
                    );
                    webuisService.updateWebUI(updatedDTO);
                }
                if (FALSE.equals(variables.get(ENABLE_HDFS_KERBEROS)) && webUiDTO.webUrl().contains("9871")) {
                    var newWebUi = webUiDTO.webUrl().replace(HTTPS, HTTP).replace("9871", "9870");
                    // 创建更新后的DTO - JDK21 Record特性
                    var updatedDTO = new ClusterServiceRoleInstanceWebuisDTO(
                        webUiDTO.id(),
                        webUiDTO.serviceRoleInstanceId(),
                        newWebUi,
                        webUiDTO.serviceInstanceId(),
                        webUiDTO.name()
                    );
                    webuisService.updateWebUI(updatedDTO);
                }
            }
        }
    }
}
