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
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ThrowableUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Iterator;
import java.util.List;

public class K8sSRFEHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sSRFEHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        ExecResult startResult;
        logger.info("FEHandlerStrategy start fe");
        Integer clusterId = command.getClusterId();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
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

                logger.info("当前节点需要作为follower添加到集群");

                try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                    // 计算当前节点索引
                    int followerPodIndex = existingNodesCount + currentRoleLoopIndex - 1;

                    // 构建完整的节点地址
                    String followerAddr = String.format("%s-%d.%s.%s.svc.cluster.local:9010",
                            serviceRoleFullName, followerPodIndex, serviceRoleFullName, Constants.DATASOPHON);

                    logger.info("添加当前follower节点: {}", followerAddr);

                    // 获取masterHost
                    String masterHost = getMasterHost(kubeClient, serviceRoleFullName);
                    logger.info("使用masterHost: {}", masterHost);

                    // 构建FE master pod名称
                    String masterPodName = serviceRoleFullName + "-0";
                    logger.info("使用Master Pod: {}", masterPodName);

                    // 直接使用SQL语句
                    String sql = String.format("ALTER SYSTEM ADD FOLLOWER \"%s\"", followerAddr);

                    // 直接在master pod中执行SQL命令
                    startResult = executeMySqlInPod(
                            Constants.DATASOPHON,
                            kubeClient,
                            masterPodName, // 使用master pod名称
                            sql);

                    if (!startResult.getExecResult()) {
                        logger.error("添加follower节点 {} 失败", followerAddr);
                        return startResult;
                    }

                    logger.info("follower节点添加成功");
                } catch (Exception e) {
                    logger.error("Add follower FE failed: {}", ThrowableUtils.getStackTrace(e));
                    startResult.setExecResult(false);
                    startResult.setExecOut(e.getMessage());
                }
            } else {
                logger.error("slave fe start failed");
            }
        }

        return startResult;
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        // 移除在K8s环境中不需要的参数
        logger.info("开始移除StarRocks在K8s环境下不需要的参数...");

        if (list != null && !list.isEmpty()) {
            // 使用迭代器遍历，方便删除元素
            Iterator<ServiceConfig> iterator = list.iterator();
            while (iterator.hasNext()) {
                ServiceConfig config = iterator.next();
                String paramName = config.getName();

                // 检查是否为priority_networks参数
                if ("priority_networks".equals(paramName)) {
                    logger.info("在K8s环境中移除 priority_networks 参数，该参数在Pod网络中不适用");
                    iterator.remove();
                }

                // 如果有其他需要移除的参数，可以在这里添加更多条件
            }
        }

        logger.info("StarRocks参数调整完成，已适配Kubernetes环境");
    }

}
