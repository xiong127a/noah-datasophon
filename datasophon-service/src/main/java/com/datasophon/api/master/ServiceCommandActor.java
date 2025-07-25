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
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuis;
import com.datasophon.dao.enums.ClusterState;
import com.datasophon.dao.enums.CommandState;
import com.mybatisflex.core.query.QueryChain;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        logger.info("service command actor restart because {}", reason.getMessage());
        super.preRestart(reason, message);
    }

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

            ClusterServiceCommandHostEntity commandHost = QueryChain.of(ClusterServiceCommandHostEntity.class)
                    .where(ClusterServiceCommandHostEntity::getCommandHostId).eq(message.getCommandHostId())
                    .one();

            long size = service.getHostCommandSizeByHostnameAndCommandHostId(message.getHostname(),
                    message.getCommandHostId());
            Integer totalProgress = service.getHostCommandTotalProgressByHostnameAndCommandHostId(message.getHostname(),
                    message.getCommandHostId());
            long progress = totalProgress / size;
            commandHost.setCommandProgress(progress);

            if (progress == 100) {
                List<ClusterServiceCommandHostCommandEntity> failedList = service
                        .findFailedHostCommand(message.getHostname(), message.getCommandHostId());
                List<ClusterServiceCommandHostCommandEntity> cancelList = service
                        .findCanceledHostCommand(message.getHostname(), message.getCommandHostId());

                if (!failedList.isEmpty()) {
                    commandHost.setCommandState(CommandState.FAILED);
                    logger.info("主机命令 {} 包含失败子命令，状态设为失败", message.getCommandHostId());
                } else if (!cancelList.isEmpty()) {
                    commandHost.setCommandState(CommandState.CANCEL);
                    logger.info("主机命令 {} 包含取消子命令，状态设为取消", message.getCommandHostId());
                } else {
                    commandHost.setCommandState(CommandState.SUCCESS);
                    logger.info("主机命令 {} 所有子命令成功，状态设为成功", message.getCommandHostId());
                }
            }

            commandHostService.updateById(commandHost);

            Long size1 = commandHostService.getCommandHostSizeByCommandId(message.getCommandId());
            Integer totalProgress1 = commandHostService.getCommandHostTotalProgressByCommandId(message.getCommandId());
            long progress1 = totalProgress1 / size1;
            ClusterServiceCommandEntity command = QueryChain.of(ClusterServiceCommandEntity.class)
                    .where(ClusterServiceCommandEntity::getCommandId).eq(message.getCommandId())
                    .one();

            command.setCommandProgress(progress1);

            if (progress1 == 100) {
                List<ClusterServiceCommandHostEntity> failedHosts = commandHostService
                        .findFailedCommandHost(message.getCommandId());
                List<ClusterServiceCommandHostEntity> canceledHosts = commandHostService
                        .findCanceledCommandHost(message.getCommandId());

                if (!failedHosts.isEmpty()) {
                    command.setCommandState(CommandState.FAILED);
                    logger.info("命令 {} 包含失败主机命令，状态设为失败", message.getCommandId());
                } else if (!canceledHosts.isEmpty()) {
                    command.setCommandState(CommandState.CANCEL);
                    logger.info("命令 {} 包含取消主机命令，状态设为取消", message.getCommandId());
                } else {
                    command.setCommandState(CommandState.SUCCESS);
                    logger.info("命令 {} 所有主机命令成功，状态设为成功", message.getCommandId());
                }

                command.setEndTime(new Date());

                String serviceName = command.getServiceName();
                ClusterInfoEntity clusterInfo = clusterInfoService.getById(command.getClusterId());

                // commandType : 1：安装服务 2：启动服务 3：停止服务 4：重启服务 5：更新配置后启动 6：更新配置后重启
                if (command.getCommandType() == 4 && HDFS.equalsIgnoreCase(serviceName)) {
                    // update web ui
                    updateHDFSWebUi(clusterInfo.getId(), command.getServiceInstanceId());
                }

                // update cluster state
                if (command.getCommandType() == 1) {

                    if (ClusterState.NEED_CONFIG.equals(clusterInfo.getClusterState())) {
                        clusterInfo.setClusterState(ClusterState.RUNNING);
                        clusterInfoService.updateById(clusterInfo);
                    }

                    if (HDFS.equalsIgnoreCase(serviceName)) {
                        ActorRef hdfsECActor = ActorUtils.getLocalActor(HdfsECActor.class,
                                ActorUtils.getActorRefName(HdfsECActor.class));
                        HdfsEcCommand hdfsEcCommand = new HdfsEcCommand();
                        hdfsEcCommand.setServiceInstanceId(command.getServiceInstanceId());
                        hdfsECActor.tell(hdfsEcCommand, getSelf());

                    }
                    logger.info("start to generate prometheus config");
                    ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,
                            ActorUtils.getActorRefName(PrometheusActor.class));
                    if (STARROCKS.equalsIgnoreCase(serviceName) || DORIS.equalsIgnoreCase(serviceName)) {
                        GenerateSRPromConfigCommand prometheusConfigCommand = new GenerateSRPromConfigCommand();
                        prometheusConfigCommand.setServiceInstanceId(command.getServiceInstanceId());
                        prometheusConfigCommand.setClusterFrame(clusterInfo.getClusterFrame());
                        prometheusConfigCommand.setClusterId(clusterInfo.getId());
                        prometheusConfigCommand.setFilename(serviceName.toLowerCase() + ".json");
                        prometheusActor.tell(prometheusConfigCommand, getSelf());
                    } else {
                        GeneratePrometheusConfigCommand prometheusConfigCommand = new GeneratePrometheusConfigCommand();
                        prometheusConfigCommand.setServiceInstanceId(command.getServiceInstanceId());
                        prometheusConfigCommand.setClusterFrame(clusterInfo.getClusterFrame());
                        prometheusConfigCommand.setClusterId(clusterInfo.getId());
                        prometheusActor.tell(prometheusConfigCommand, getSelf());
                        enableAlertConfig(NODE, clusterInfo.getId());
                    }
                    enableAlertConfig(serviceName, clusterInfo.getId());
                }
            }
            commandService.updateById(command);
        } catch (Exception e) {
            logger.error("处理UpdateCommandHostMessage消息时出错", e);
        }
    }

    private void enableAlertConfig(String serviceName, Integer clusterId) {
        ClusterAlertQuotaService alertQuotaService = SpringUtil
                .getBean(ClusterAlertQuotaService.class);
        List<ClusterAlertQuota> list = alertQuotaService.listAlertQuotaByServiceName(serviceName);
        List<Integer> ids = list.stream().map(ClusterAlertQuota::getId).collect(Collectors.toList());
        String alertQuotaIds = StringUtils.join(ids, ",");
        alertQuotaService.start(clusterId, alertQuotaIds);
    }

    private void updateHDFSWebUi(Integer clusterId, Integer serviceInstanceId) {
        Map<String, String> variables = GlobalVariables.get(clusterId);
        if (variables.containsKey(ENABLE_HDFS_KERBEROS)) {
            ClusterServiceRoleInstanceWebuisService webuisService = SpringUtil
                    .getBean(ClusterServiceRoleInstanceWebuisService.class);
            List<ClusterServiceRoleInstanceWebuis> webUis = webuisService
                    .listWebUisByServiceInstanceId(serviceInstanceId);
            for (ClusterServiceRoleInstanceWebuis webUi : webUis) {
                if (TRUE.equals(variables.get(ENABLE_HDFS_KERBEROS)) && webUi.getWebUrl().contains("9870")) {
                    String newWebUi = webUi.getWebUrl().replace(HTTP, HTTPS).replace("9870", "9871");
                    webUi.setWebUrl(newWebUi);
                    webuisService.updateById(webUi);
                }
                if (FALSE.equals(variables.get(ENABLE_HDFS_KERBEROS)) && webUi.getWebUrl().contains("9871")) {
                    String newWebUi = webUi.getWebUrl().replace(HTTPS, HTTP).replace("9871", "9870");
                    webUi.setWebUrl(newWebUi);
                    webuisService.updateById(webUi);
                }
            }
        }
    }
}
