/*
 *
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
 *
 */

package com.datasophon.api.master;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.common.enums.Status;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.command.HostCheckCommand;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.mybatisflex.core.query.QueryChain;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public class HostConnectActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(HostConnectActor.class);

    @Override
    public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
        logger.info("or restart because {}", reason.getMessage());
        super.preRestart(reason, message);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(HostCheckCommand.class, this::handleHostCheck)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleHostCheck(HostCheckCommand hostCheckCommand) {
        try {
            HostInfo hostInfo = hostCheckCommand.getHostInfo();
            String clusterCode = hostCheckCommand.getClusterCode();

            logger.info("start host check: {} for cluster: {}", hostInfo.getHostname(), clusterCode);

            ClusterHostService clusterHostService = SpringUtil.getBean(ClusterHostService.class);

            // 通过clusterCode查找集群信息
            ClusterInfoEntity clusterInfo = QueryChain.of(ClusterInfoEntity.class)
                    .where(ClusterInfoEntity::getClusterCode).eq(clusterCode)
                    .one();

            if (clusterInfo == null) {
                logger.error("Cluster not found for clusterCode: {}", clusterCode);
                hostInfo.setCheckResult(
                        new CheckResult(
                                Status.CONNECTION_FAILED.getCode(),
                                "集群信息未找到"));
                return;
            }

            boolean isKubernetesMode = clusterInfo.getDepType() != null && clusterInfo.getDepType().isKubernetes();

            if (isKubernetesMode) {
                // Kubernetes模式：不进行SSH连接测试，基于受管状态给出校验结果
                logger.info("Kubernetes mode detected for host: {}, skipping SSH check", hostInfo.getHostname());

                // 检查主机是否已受管
                ClusterHostEntity existingHost = clusterHostService.getClusterHostByHostname(hostInfo.getHostname());

                if (existingHost != null && existingHost.getClusterId().equals(clusterInfo.getId())) {
                    // 主机已在当前集群中受管 - 重复添加
                    hostInfo.setCheckResult(
                            new CheckResult(
                                    Status.CONNECTION_FAILED.getCode(),
                                    "主机已在当前集群中受管，请勿重复添加"));
                    logger.info("Host {} is already managed in current Kubernetes cluster {}",
                            hostInfo.getHostname(), clusterInfo.getId());
                } else if (existingHost != null) {
                    // 主机已在其他集群中受管
                    hostInfo.setCheckResult(
                            new CheckResult(
                                    Status.CONNECTION_FAILED.getCode(),
                                    "主机已在其他集群中受管"));
                    logger.info("Host {} is already managed in another cluster {}",
                            hostInfo.getHostname(), existingHost.getClusterId());
                } else {
                    // 主机未受管，可以添加
                    hostInfo.setCheckResult(
                            new CheckResult(
                                    Status.CHECK_HOST_SUCCESS.getCode(),
                                    Status.CHECK_HOST_SUCCESS.getMsg()));
                    logger.info("Host {} is not managed in Kubernetes mode, can be added", hostInfo.getHostname());
                }
            } else {
                // PVM模式：进行SSH连接测试
                logger.info("PVM mode detected for host: {}, performing SSH check", hostInfo.getHostname());
                ClientSession session = MinaUtils.openConnection(hostInfo);
                if (ObjectUtil.isNotNull(session)) {
                    hostInfo.setCheckResult(
                            new CheckResult(
                                    Status.CHECK_HOST_SUCCESS.getCode(),
                                    Status.CHECK_HOST_SUCCESS.getMsg()));
                } else {
                    hostInfo.setCheckResult(
                            new CheckResult(
                                    Status.CONNECTION_FAILED.getCode(),
                                    Status.CONNECTION_FAILED.getMsg()));
                }
                MinaUtils.closeConnection(session);
            }

            logger.info("end host check: {}", hostInfo.getHostname());
        } catch (Exception e) {
            logger.error("Error handling HostCheckCommand", e);
        }
    }
}
