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

package com.datasophon.kubernetes.actor;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import com.datasophon.common.command.KubernetesGetLogCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.kubernetes.util.KubeUtil;
import com.datasophon.kubernetes.util.KubernetesUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesLogActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesLogActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(KubernetesGetLogCommand.class, command -> {
                    logger.info("get query log command");

                    ExecResult logResult = new ExecResult();
                    try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                        logResult = KubernetesUtil.getContainerLog(
                                command.getNamespace(),
                                kubeClient,
                                command.getServiceRoleFullName(),
                                command.getHostname(),
                                PropertyUtils.getInt("rows"));
                        getSender().tell(logResult, getSelf());
                    } catch (Exception e) {
                        logger.error("Get container log error: ", e);
                        logResult.setExecResult(false);
                        logResult.setExecErrOut("Failed to get container log: " + e.getMessage());
                        getSender().tell(logResult, getSelf());
                    }
                })
                .matchAny(this::unhandled)
                .build();
    }
}
