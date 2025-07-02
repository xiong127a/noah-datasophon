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

import java.util.List;
import java.util.stream.Collectors;

public class K8sSRBEHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sSRBEHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        startResult = serviceHandler.start(command);
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            logger.info("add be to cluster");
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

                    // 判断是否是最后一次循环
                    boolean isLastLoop = (currentRoleLoopIndex == totalRoleLoopCount);
                    if (!isLastLoop) {
                        logger.info("当前不是最后一次循环，跳过添加BE操作");
                        return startResult;
                    }

                    logger.info("当前是最后一次循环，开始执行添加BE操作");

                    // 计算需要添加的节点范围（Pod索引从0开始）
                    int startPodIndex = existingNodesCount; // 新节点的起始Pod索引
                    int endPodIndex = existingNodesCount + totalRoleLoopCount - 1; // 新节点的结束Pod索引

                    logger.info("需要添加BE节点Pod索引范围: {} 到 {}", startPodIndex, endPodIndex);

                    // 检查是否有节点需要添加
                    if (startPodIndex > endPodIndex) {
                        logger.warn("没有BE节点需要添加");
                        return startResult;
                    }

                    // 创建SQL语句列表 - 使用IntStream更优雅地生成
                    List<String> sqlStatements = java.util.stream.IntStream.rangeClosed(startPodIndex, endPodIndex)
                            .mapToObj(i -> {
                                // 构建完整的节点地址（Pod名称格式是serviceName-podIndex）
                                String beHostPort = String.format("%s-%d.%s.%s.svc.cluster.local:9050",
                                        serviceRoleFullName, i, serviceRoleFullName, Constants.DATASOPHON);
                                String beHost = String.format("%s-%d.%s.%s.svc.cluster.local",
                                        serviceRoleFullName, i, serviceRoleFullName, Constants.DATASOPHON);

                                logger.info("添加BE节点 {}: {}:{}", i - startPodIndex + 1, beHost, "9050");

                                // 创建SQL语句（不需要MySQL命令行前缀，executeMySqlInPod会添加）
                                return String.format("ALTER SYSTEM ADD BACKEND \"%s\"", beHostPort);
                            })
                            .collect(Collectors.toList());

                    if (sqlStatements.isEmpty()) {
                        logger.warn("没有需要添加的BE节点");
                        return startResult;
                    }

                    logger.info("执行添加BE命令，共 {} 条SQL语句", sqlStatements.size());

                    // 使用executeMySqlInPod批量执行SQL语句
                    startResult = executeMySqlInPod(
                            Constants.DATASOPHON,
                            kubeClient,
                            "starrocks-srfe-0", // 使用第一个FE节点执行命令
                            sqlStatements);
                } catch (Exception e) {
                    logger.error("Add BE failed: {}", ThrowableUtils.getStackTrace(e));
                    startResult.setExecResult(false);
                    startResult.setExecOut(e.getMessage());
                }
                logger.info("slave be start success");
            } else {
                logger.error("slave be start failed");
            }
        }
        return startResult;
    }
}
