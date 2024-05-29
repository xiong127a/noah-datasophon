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

import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.RollingRestartUtils;
import com.datasophon.common.command.SubmitActiveTaskNodeCommand;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.RollingRestartInfo;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;

import scala.Option;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class SubmitTaskNodeActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(SubmitTaskNodeActor.class);

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        logger.info("service command actor restart because {}", reason.getMessage());
        super.preRestart(reason, message);
    }

    @Override
    public void onReceive(Object message) throws Throwable {

        if (message instanceof SubmitActiveTaskNodeCommand) {
            SubmitActiveTaskNodeCommand submitActiveTaskNodeCommand = (SubmitActiveTaskNodeCommand) message;
            DAGGraph<String, ServiceNode, String> dag = submitActiveTaskNodeCommand.getDag();
            Map<String, ServiceExecuteState> activeTaskList = submitActiveTaskNodeCommand.getActiveTaskList();
            Map<String, String> errorTaskList = submitActiveTaskNodeCommand.getErrorTaskList();
            Map<String, String> readyToSubmitTaskList = submitActiveTaskNodeCommand.getReadyToSubmitTaskList();
            Map<String, String> completeTaskList = submitActiveTaskNodeCommand.getCompleteTaskList();
            // dag
            if (readyToSubmitTaskList.size() > 0) {
                for (String node : readyToSubmitTaskList.keySet()) {
                    Set<String> previousNodes = dag.getPreviousNodes(node);
                    for (String previousNode : previousNodes) {
                        if (errorTaskList.containsKey(previousNode)) {
                            readyToSubmitTaskList.remove(node);
                        }
                        if (!completeTaskList.containsKey(previousNode)) {
                            readyToSubmitTaskList.remove(node);
                            continue;
                        }
                    }
                    if (activeTaskList.containsKey(node)) {
                        continue;
                    }
                    if (completeTaskList.containsKey(node)) {
                        continue;
                    }
                    ServiceNode serviceNode = dag.getNode(node);
                    List<ServiceRoleInfo> masterRoles = serviceNode.getMasterRoles();

                    activeTaskList.put(node, ServiceExecuteState.RUNNING);

                    if (masterRoles.size() > 0) {
                        logger.info("start to submit {} master roles", node);
                        ActorRef serviceActor = ActorUtils.getLocalActor(MasterServiceActor.class,
                                submitActiveTaskNodeCommand.getClusterCode() + "-serviceActor-" + node);
                        ProcessUtils.buildExecuteServiceRoleCommand(
                                submitActiveTaskNodeCommand.getClusterId(),
                                submitActiveTaskNodeCommand.getCommandType(),
                                submitActiveTaskNodeCommand.getClusterCode(),
                                dag,
                                activeTaskList,
                                errorTaskList,
                                readyToSubmitTaskList,
                                completeTaskList,
                                node,
                                masterRoles,
                                null,
                                serviceActor,
                                ServiceRoleType.MASTER);

                    } else if (serviceNode.getElseRoles().size() > 0) {
                        logger.info("{} does not has master roles , start to submit worker or client roles", node);

                        //滚动重启
                        RollingRestartInfo rollingRestartInfo = submitActiveTaskNodeCommand.getRollingRestartInfo();
                        int errorCount = 0;
                        List<ServiceRoleInfo> batchList = new ArrayList<>(); // 批次集合

                        for (ServiceRoleInfo elseRole : serviceNode.getElseRoles()) {
                            ActorRef serviceActor = ActorUtils.getLocalActor(WorkerServiceActor.class,
                                    submitActiveTaskNodeCommand.getClusterCode() + "-serviceActor-" + node + "-"
                                            + elseRole.getHostname());

                            ProcessUtils.buildExecuteServiceRoleCommand(
                                    submitActiveTaskNodeCommand.getClusterId(),
                                    submitActiveTaskNodeCommand.getCommandType(),
                                    submitActiveTaskNodeCommand.getClusterCode(),
                                    dag,
                                    activeTaskList,
                                    errorTaskList,
                                    readyToSubmitTaskList,
                                    completeTaskList,
                                    node,
                                    serviceNode.getElseRoles(),
                                    elseRole,
                                    serviceActor,
                                    ServiceRoleType.WORKER);

                            //滚动重启控制
                            batchList.add(elseRole);
                            if (Objects.nonNull(rollingRestartInfo) && batchList.size() == rollingRestartInfo.getBatchCount()) {

                                //批量等待
                                for (ServiceRoleInfo serviceRoleInfo : batchList) {
                                    RollingRestartUtils.getCountDownLatchByServiceKey(serviceRoleInfo.getHostname() + serviceRoleInfo.getServiceInstanceId()).await();
                                    //错误计数
                                    errorCount = errorCount + RollingRestartUtils.getErrorCount(serviceRoleInfo.getHostname() + serviceRoleInfo.getServiceInstanceId());
                                }

                                //清除缓存
//                                RollingRestartUtils.clean();

                                // 启动失败数量大于阈值 停止后边的任务
                                if (errorCount > rollingRestartInfo.getTaskFailureTolerance()) {
                                    return;//停止循环
                                }
                                logger.info("批次实例滚动重启结束");
                                logger.info("滚动重启批次等待:{} s", rollingRestartInfo.getBatchSeparationInSeconds());
                                Thread.sleep(rollingRestartInfo.getBatchSeparationInSeconds()*1000);
                                logger.info("滚动重启批次等待结束");

                                batchList.clear();// 进行下一批次计数
                            }
                        }

                        logger.info("----------------------{}", Thread.currentThread().getName());

                    } else {
                        continue;
                    }
                }
            }
        }
    }

}
