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
import com.datasophon.k8s.util.K8sUtil;
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

                    // 判断是否是最后一次循环
                    boolean isLastLoop = (currentRoleLoopIndex == totalRoleLoopCount);
                    if (!isLastLoop) {
                        logger.info("当前不是最后一次循环，跳过添加CN操作");
                        return startResult;
                    }

                    logger.info("当前是最后一次循环，开始执行添加CN操作");

                    // 构建批量注册命令
                    StringBuilder cmdBuilder = new StringBuilder();

                    // 计算需要添加的节点范围（Pod索引从0开始）
                    int startPodIndex = existingNodesCount; // 新节点的起始Pod索引
                    int endPodIndex = existingNodesCount + totalRoleLoopCount - 1; // 新节点的结束Pod索引

                    logger.info("需要添加CN节点Pod索引范围: {} 到 {}", startPodIndex, endPodIndex);

                    // 检查是否有节点需要添加
                    if (startPodIndex > endPodIndex) {
                        logger.warn("没有CN节点需要添加");
                        return startResult;
                    }

                    // 遍历需要添加的所有节点
                    for (int i = startPodIndex; i <= endPodIndex; i++) {
                        // 构建完整的节点地址（Pod名称格式是serviceName-podIndex）
                        String cnHostPort = String.format("%s-%d.%s.%s.svc.cluster.local:9020",
                                serviceRoleFullName, i, serviceRoleFullName, Constants.DATASOPHON);
                        String cnHost = String.format("%s-%d.%s.%s.svc.cluster.local",
                                serviceRoleFullName, i, serviceRoleFullName, Constants.DATASOPHON);

                        logger.info("添加CN节点 {}: {}:{}", i - startPodIndex + 1, cnHost, "9020");

                        // 为每个节点生成连接检测命令
                        String checkCmd = generateConnectionCheckCommand(cnHost, 9020);

                        // 执行连接检测命令
                        logger.info("检测CN节点 {} 连接是否可用", cnHost);
                        ExecResult checkResult = K8sUtil.runCmd(
                                Constants.DATASOPHON,
                                kubeClient,
                                "starrocks-srfe",
                                command.getMasterHost(),
                                checkCmd);

                        if (!checkResult.getExecResult()) {
                            logger.error("CN节点 {} 连接检测失败，跳过添加此节点", cnHost);
                            logger.error("错误信息: {}", checkResult.getExecErrOut());
                            continue;
                        }

                        logger.info("CN节点 {} 连接检测成功，准备添加节点", cnHost);

                        // 使用单引号而非双引号，避免多层转义问题
                        String singleCmd = String.format(
                                "mysql -h127.0.0.1 -P9030 -uroot --connect-timeout=10 -e 'ALTER SYSTEM ADD COMPUTE NODE \"%s\"'",
                                cnHostPort);

                        // 添加命令分隔符
                        if (cmdBuilder.length() > 0) {
                            cmdBuilder.append(" && ");
                        }

                        cmdBuilder.append(singleCmd);
                    }

                    if (cmdBuilder.length() <= 0) {
                        logger.warn("没有需要添加的CN节点");
                        return startResult;
                    }

                    String finalCmd = cmdBuilder.toString();
                    logger.info("执行添加CN命令: {}", finalCmd);

                    startResult = K8sUtil.runCmd(
                            Constants.DATASOPHON,
                            kubeClient,
                            "starrocks-srfe",
                            command.getMasterHost(), // 在主FE上执行注册命令
                            finalCmd);
                } catch (Exception e) {
                    logger.error("Add CN failed: {}", ThrowableUtils.getStackTrace(e));
                    startResult.setExecResult(false);
                    startResult.setExecOut(e.getMessage());
                }
                logger.info("slave cn start success");
            } else {
                logger.error("slave cn start failed");
            }
        }
        return startResult;
    }
}
