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
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ThrowableUtils;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class KubernetesSRFEHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesSRFEHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) {
        ExecResult startResult;
        logger.info("FEHandlerStrategy start fe");
        Integer clusterId = command.getClusterId();
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        startResult = serviceHandler.start(command);

        if (command.getCommandType() == CommandType.INSTALL_SERVICE) {

            int currentRoleLoopIndex = getCurrentRoleLoopIndex();
            int totalRoleLoopCount = getTotalRoleLoopCount();
            Integer roleInstallCount = getRoleInstallCount(clusterId);

            // 计算已经存在的节点数量
            int existingNodesCount = roleInstallCount - totalRoleLoopCount;

            logger.info("当前角色 [{}] 处理状态：当前循环次数={}, 本次安装数量={}, 总安装数量={}, 已存在节点数量={}",
                    serviceRoleFullName, currentRoleLoopIndex, totalRoleLoopCount, roleInstallCount,
                    existingNodesCount);

            if (startResult.getExecResult()) {
                // 检查是否已经存在master节点
                boolean masterExists = existingNodesCount > 0;

                // 如果不存在master节点，且当前是第一个循环，则当前节点是master
                boolean isCurrentNodeMaster = !masterExists && currentRoleLoopIndex == 1;

                // 如果当前节点是master，不需要执行添加follower操作
                if (isCurrentNodeMaster) {
                    logger.info("当前是主节点，无需执行添加follower操作");
                    return startResult;
                }

                // 判断是否是最后一次循环，只在最后一次循环添加follower
                boolean isLastLoop = (currentRoleLoopIndex == totalRoleLoopCount);
                if (!isLastLoop) {
                    logger.info("当前不是最后一次循环，跳过添加follower操作");
                    return startResult;
                }

                logger.info("当前是最后一次循环，执行添加follower操作");

                // 如果需要添加follower，继续执行
                try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                    StringBuilder cmdBuilder = new StringBuilder();

                    // 先确定是否是新集群安装还是扩容
                    boolean isNewCluster = existingNodesCount == 0;

                    // 确定master节点索引（Pod索引从0开始，但在DNS中我们需要使用从0开始的索引）
                    int masterPodIndex = isNewCluster ? 0 : existingNodesCount - 1;

                    logger.info("集群状态: {}，已有节点数: {}, 主节点Pod索引: {}",
                            (isNewCluster ? "新集群" : "扩容"), existingNodesCount, masterPodIndex);

                    // 计算需要添加的follower节点范围（Pod索引从0开始）
                    int followerStartPodIndex = isNewCluster ? 1 : existingNodesCount;
                    int followerEndPodIndex = existingNodesCount + totalRoleLoopCount - 1;

                    logger.info("需要添加follower节点Pod索引范围: {} 到 {}", followerStartPodIndex, followerEndPodIndex);

                    // 检查是否有follower需要添加
                    if (followerStartPodIndex > followerEndPodIndex) {
                        logger.info("没有follower节点需要添加");
                        return startResult;
                    }

                    // 创建SQL语句列表 - 使用IntStream更优雅地生成
                    List<String> sqlStatements = java.util.stream.IntStream
                            .rangeClosed(followerStartPodIndex, followerEndPodIndex)
                            .mapToObj(i -> {
                                // 构建完整的节点地址（Pod名称格式是serviceName-podIndex）
                                String followerAddr = String.format("%s-%d.%s.%s.svc.cluster.local:9010",
                                        serviceRoleFullName, i, serviceRoleFullName, getKubernetesNamespace(clusterId));
                                String followerHost = String.format("%s-%d.%s.%s.svc.cluster.local",
                                        serviceRoleFullName, i, serviceRoleFullName, getKubernetesNamespace(clusterId));

                                logger.info("添加follower节点: {}", followerAddr);

                                // 创建SQL语句（不需要MySQL命令行前缀，executeMySqlInPod会添加）
                                return String.format("ALTER SYSTEM ADD FOLLOWER \"%s\"", followerAddr);
                            })
                            .collect(Collectors.toList());

                    if (sqlStatements.isEmpty()) {
                        logger.info("没有需要执行的添加follower命令");
                        return startResult;
                    }

                    logger.info("执行添加follower命令，共 {} 条SQL语句", sqlStatements.size());

                    // 使用executeMySqlInPod批量执行SQL语句
                    startResult = executeMySqlInPod(
                            clusterId,
                            kubeClient,
                            serviceRoleFullName + "-0", // 使用第一个FE节点执行命令
                            sqlStatements);
                } catch (Exception e) {
                    logger.error("Add slave FE failed: {}", ThrowableUtils.getStackTrace(e));
                    startResult.setExecResult(false);
                    startResult.setExecOut(e.getMessage());
                }
                logger.info("slave fe start success");
            } else {
                logger.error("slave fe start failed");
            }
        }

        return startResult;
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        // 移除在Kubernetes环境中不需要的参数
        logger.info("开始移除StarRocks在Kubernetes环境下不需要的参数...");

        if (list != null && !list.isEmpty()) {
            // 使用迭代器遍历，方便删除元素
            Iterator<ServiceConfig> iterator = list.iterator();
            while (iterator.hasNext()) {
                ServiceConfig config = iterator.next();
                String paramName = config.getName();

                // 检查是否为priority_networks参数
                if ("priority_networks".equals(paramName)) {
                    logger.info("在Kubernetes环境中移除 priority_networks 参数，该参数在Pod网络中不适用");
                    iterator.remove();
                }

                // 如果有其他需要移除的参数，可以在这里添加更多条件
            }
        }

        logger.info("StarRocks参数调整完成，已适配Kubernetes环境");
    }
}
