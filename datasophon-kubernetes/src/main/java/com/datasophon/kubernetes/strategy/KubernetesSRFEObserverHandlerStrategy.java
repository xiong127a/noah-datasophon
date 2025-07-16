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

package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;
import java.util.stream.Collectors;

public class KubernetesSRFEObserverHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesSRFEObserverHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        logger.info("Start FE Observer installation");
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        startResult = serviceHandler.start(command);
        if (command.getCommandType() == CommandType.INSTALL_SERVICE) {
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
                        logger.info("当前不是最后一次循环，跳过添加Observer操作");
                        return startResult;
                    }

                    logger.info("当前是最后一次循环，开始执行添加Observer操作");

                    // 计算需要添加的节点范围（Pod索引从0开始）
                    int startPodIndex = existingNodesCount; // 新节点的起始Pod索引
                    int endPodIndex = existingNodesCount + totalRoleLoopCount - 1; // 新节点的结束Pod索引

                    logger.info("需要添加Observer节点Pod索引范围: {} 到 {}", startPodIndex, endPodIndex);

                    // 检查是否有节点需要添加
                    if (startPodIndex > endPodIndex) {
                        logger.warn("没有Observer节点需要添加");
                        return startResult;
                    }

                    // 创建SQL语句列表 - 使用IntStream更优雅地生成
                    List<String> sqlStatements = java.util.stream.IntStream.rangeClosed(startPodIndex, endPodIndex)
                            .mapToObj(i -> {
                                // 构建完整的节点地址（Pod名称格式是serviceName-podIndex）
                                String observerAddr = String.format("%s-%d.%s.%s.svc.cluster.local:9010",
                                        serviceRoleFullName, i, serviceRoleFullName, getKubernetesNamespace(command.getClusterId()));
                                String observerHost = String.format("%s-%d.%s.%s.svc.cluster.local",
                                        serviceRoleFullName, i, serviceRoleFullName, getKubernetesNamespace(command.getClusterId()));

                                logger.info("添加Observer节点 {}: {}", i - startPodIndex + 1, observerAddr);

                                // 创建SQL语句（不需要MySQL命令行前缀，executeMySqlInPod会添加）
                                return String.format("ALTER SYSTEM ADD OBSERVER \"%s\"", observerAddr);
                            })
                            .collect(Collectors.toList());

                    if (sqlStatements.isEmpty()) {
                        logger.warn("没有需要添加的Observer节点");
                        return startResult;
                    }

                    logger.info("执行添加Observer命令，共 {} 条SQL语句", sqlStatements.size());

                    // 使用executeMySqlInPod批量执行SQL语句
                    startResult = executeMySqlInPod(
                            command.getClusterId(),
                            kubeClient,
                            "starrocks-srfe-0", // 使用第一个FE节点执行命令
                            sqlStatements);
                } catch (Exception e) {
                    logger.error("Add Observer failed", e);
                    startResult.setExecResult(false);
                    startResult.setExecOut(e.getMessage());
                }

            }
        } else {
            startResult = serviceHandler.start(command);
        }

        logger.info("FE Observer installation {}", startResult.getExecResult() ? "succeeded" : "failed");
        return startResult;
    }

}
