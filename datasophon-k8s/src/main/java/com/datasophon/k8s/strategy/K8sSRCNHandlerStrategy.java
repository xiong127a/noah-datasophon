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

package com.datasophon.k8s.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ThrowableUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

public class K8sSRCNHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sSRCNHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        startResult = serviceHandler.start(command);
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            logger.info("add cn to cluster");
            if (startResult.getExecResult()) {
                try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                    // 获取当前循环索引、总循环次数和角色安装数量
                    int currentRoleLoopIndex = getCurrentRoleLoopIndex();
                    int totalRoleLoopCount = getTotalRoleLoopCount();
                    Integer roleInstallCount = getRoleInstallCount(command.getClusterId());

                    // 计算已经存在的节点数量
                    int existingNodesCount = roleInstallCount - totalRoleLoopCount;

                    logger.info("当前角色 [{}] 处理状态：当前循环次数={}, 本次安装数量={}, 总安装数量={}, 已存在节点数量={}",
                            serviceRoleFullName, currentRoleLoopIndex, totalRoleLoopCount, roleInstallCount,
                            existingNodesCount);

                    logger.info("开始执行添加CN操作");

                    // 计算当前节点索引
                    int cnPodIndex = existingNodesCount + currentRoleLoopIndex - 1;

                    // 构建完整的节点地址
                    String cnHostPort = String.format("%s-%d.%s.%s.svc.cluster.local:9020",
                            serviceRoleFullName, cnPodIndex, serviceRoleFullName, Constants.DATASOPHON);

                    logger.info("添加当前CN节点: {}", cnHostPort);

                    // 获取masterHost
                    String masterHost = getMasterHost(kubeClient, "starrocks-srfe");
                    logger.info("使用masterHost: {}", masterHost);

                    // 构建FE master pod名称
                    String masterPodName = "starrocks-srfe-0";
                    logger.info("使用Master Pod: {}", masterPodName);

                    // 直接使用SQL语句
                    String sql = String.format("ALTER SYSTEM ADD COMPUTE NODE \"%s\"", cnHostPort);

                    // 直接在master pod中执行SQL命令
                    startResult = executeMySqlInPod(
                            Constants.DATASOPHON,
                            kubeClient,
                            masterPodName, // 使用master pod名称
                            sql);

                    if (!startResult.getExecResult()) {
                        logger.error("添加CN节点 {} 失败", cnHostPort);
                        return startResult;
                    }

                    logger.info("CN节点添加成功");
                } catch (Exception e) {
                    logger.error("Add CN failed: {}", ThrowableUtils.getStackTrace(e));
                    startResult.setExecResult(false);
                    startResult.setExecOut(e.getMessage());
                }
                logger.info("CN节点添加完成");
            } else {
                logger.error("slave cn start failed");
            }
        }
        return startResult;
    }
}
