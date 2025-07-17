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

import com.datasophon.api.enums.Status;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.HostCheckCommand;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterInfoEntity;

import org.apache.sshd.client.session.ClientSession;

import scala.Option;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;
import cn.hutool.core.util.ObjectUtil;

public class HostConnectActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(HostConnectActor.class);

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        logger.info("or restart because {}", reason.getMessage());
        super.preRestart(reason, message);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof HostCheckCommand) {
            HostCheckCommand hostCheckCommand = (HostCheckCommand) message;
            HostInfo hostInfo = hostCheckCommand.getHostInfo();
            String clusterCode = hostCheckCommand.getClusterCode();

            logger.info("start host check: {} for cluster: {}", hostInfo.getHostname(), clusterCode);

            // 获取集群信息以判断部署模式
            ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
            ClusterHostService clusterHostService = SpringUtil.getBean(ClusterHostService.class);

            // 通过clusterCode查找集群信息
            ClusterInfoEntity clusterInfo = clusterInfoService.getOne(
                    new QueryWrapper<ClusterInfoEntity>().eq("cluster_code", clusterCode));

            if (clusterInfo == null) {
                logger.error("Cluster not found for clusterCode: {}", clusterCode);
                hostInfo.setCheckResult(
                        new CheckResult(
                                Status.CONNECTION_FAILED.getCode(),
                                "集群信息未找到"));
                return;
            }

            boolean isKubernetesMode = Constants.KUBERNETES_MODE.equals(clusterInfo.getDepType());

            if (isKubernetesMode) {
                // Kubernetes模式：不进行SSH连接测试，基于受管状态给出校验结果
                logger.info("Kubernetes mode detected for host: {}, skipping SSH check", hostInfo.getHostname());

                // 检查主机是否已受管
                ClusterHostDO existingHost = clusterHostService.getClusterHostByHostname(hostInfo.getHostname());

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
        } else {
            unhandled(message);
        }
    }
}
