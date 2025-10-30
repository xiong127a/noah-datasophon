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

import com.datasophon.api.service.HostConnectCheckService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.common.command.HostCheckCommand;
import com.datasophon.common.enums.Status;
import com.datasophon.common.model.CommonResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import com.mybatisflex.core.query.QueryChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 主机连接检查服务实现
 * 替代HostConnectActor，负责检查主机SSH连接和受管状态
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-30
 */
@Service
public class HostConnectCheckServiceImpl implements HostConnectCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HostConnectCheckServiceImpl.class);

    @Autowired
    private ClusterHostService clusterHostService;

    // SSH连接服务
    private final SshConnectionService sshService = 
            SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();

    @Override
    @Async("taskExecutor")
    public void checkHostConnection(HostCheckCommand command) {
        try {
            HostInfo hostInfo = command.getHostInfo();
            String clusterCode = command.getClusterCode();

            logger.info("开始检查主机: {} 集群: {}", hostInfo.getHostname(), clusterCode);

            // 查找集群信息
            ClusterInfoEntity clusterInfo = QueryChain.of(ClusterInfoEntity.class)
                    .where(ClusterInfoEntity::getClusterCode).eq(clusterCode)
                    .one();

            if (clusterInfo == null) {
                logger.error("未找到集群信息, clusterCode: {}", clusterCode);
                hostInfo.setCommonResult(
                        new CommonResult(
                                Status.CONNECTION_FAILED.getCode(),
                                "集群信息未找到"));
                return;
            }

            // 判断部署类型
            boolean isKubernetesMode = clusterInfo.getDepType() != null && 
                                      clusterInfo.getDepType().isKubernetes();

            if (isKubernetesMode) {
                checkKubernetesHost(hostInfo, clusterInfo);
            } else {
                checkPvmHost(hostInfo);
            }

            logger.info("主机检查完成: {}", hostInfo.getHostname());

        } catch (Exception e) {
            logger.error("主机连接检查异常", e);
        }
    }

    /**
     * 检查Kubernetes模式主机
     * 不进行SSH连接测试，基于受管状态给出校验结果
     */
    private void checkKubernetesHost(HostInfo hostInfo, ClusterInfoEntity clusterInfo) {
        logger.info("Kubernetes模式检测到主机: {}, 跳过SSH检查", hostInfo.getHostname());

        // 检查主机是否已受管
        ClusterHostEntity existingHost = clusterHostService.getClusterHostByHostname(hostInfo.getHostname());

        if (existingHost != null && existingHost.getClusterId().equals(clusterInfo.getId())) {
            // 主机已在当前集群中受管 - 重复添加
            hostInfo.setCommonResult(
                    new CommonResult(
                            Status.CONNECTION_FAILED.getCode(),
                            "主机已在当前集群中受管，请勿重复添加"));
            logger.info("主机 {} 已在当前Kubernetes集群 {} 中受管",
                    hostInfo.getHostname(), clusterInfo.getId());
        } else if (existingHost != null) {
            // 主机已在其他集群中受管
            hostInfo.setCommonResult(
                    new CommonResult(
                            Status.CONNECTION_FAILED.getCode(),
                            "主机已在其他集群中受管"));
            logger.info("主机 {} 已在其他集群 {} 中受管",
                    hostInfo.getHostname(), existingHost.getClusterId());
        } else {
            // 主机未受管，可以添加
            hostInfo.setCommonResult(
                    new CommonResult(
                            Status.CHECK_HOST_SUCCESS.getCode(),
                            Status.CHECK_HOST_SUCCESS.getMsg()));
            logger.info("主机 {} 未受管（Kubernetes模式），可以添加", hostInfo.getHostname());
        }
    }

    /**
     * 检查PVM模式主机
     * 进行SSH连接测试
     */
    private void checkPvmHost(HostInfo hostInfo) {
        logger.info("PVM模式检测到主机: {}, 执行SSH检查", hostInfo.getHostname());

        try {
            // 通过SSH插件适配器测试连接
            HostCheckContext context = buildHostCheckContext(hostInfo);
            CommandResult connectionTest = sshService.testConnection(context);
            boolean connectionSuccess = connectionTest.isSuccess();

            if (connectionSuccess) {
                hostInfo.setCommonResult(
                        new CommonResult(
                                Status.CHECK_HOST_SUCCESS.getCode(),
                                Status.CHECK_HOST_SUCCESS.getMsg()));
                logger.info("SSH连接测试成功: {}", hostInfo.getHostname());
            } else {
                hostInfo.setCommonResult(
                        new CommonResult(
                                Status.CONNECTION_FAILED.getCode(),
                                Status.CONNECTION_FAILED.getMsg()));
                logger.warn("SSH连接测试失败: {}", hostInfo.getHostname());
            }
        } catch (Exception e) {
            logger.error("SSH连接测试异常: {} -> {}", hostInfo.getHostname(), e.getMessage(), e);
            hostInfo.setCommonResult(
                    new CommonResult(
                            Status.CONNECTION_FAILED.getCode(),
                            "SSH连接异常: " + e.getMessage()));
        }
    }

    /**
     * 构建SSH检查上下文
     */
    private HostCheckContext buildHostCheckContext(HostInfo hostInfo) {
        return HostCheckContext.builder()
                .hostIp(hostInfo.getIp())
                .sshPort(hostInfo.getSshPort())
                .sshUser(hostInfo.getSshUser())
                .sshPassword(hostInfo.getSshPassword())
                .build();
    }
}

