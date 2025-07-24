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

package com.datasophon.api.master.handler.service;

import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateServiceConfigCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.api.utils.ClusterInfoUtils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ServiceConfigureHandler extends ServiceHandler {

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) {
        // config
        GenerateServiceConfigCommand generateServiceConfigCommand = new GenerateServiceConfigCommand();
        generateServiceConfigCommand.setServiceName(serviceRoleInfo.getParentName());
        String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
        generateServiceConfigCommand.setNamespace(namespace);
        generateServiceConfigCommand.setCofigFileMap(serviceRoleInfo.getConfigFileMap());
        generateServiceConfigCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        generateServiceConfigCommand.setRunAs(serviceRoleInfo.getRunAs());
        if ("zkserver".equalsIgnoreCase(serviceRoleInfo.getName())) {
            generateServiceConfigCommand.setMyid((Integer) CacheUtils.get("zkserver_" + serviceRoleInfo.getHostname()));
        }
        generateServiceConfigCommand.setServiceRoleName(serviceRoleInfo.getName());
        ActorSelection configActor = ActorUtils.actorSystem.actorSelection(
                "akka.tcp://datasophon@" + serviceRoleInfo.getHostname() + ":2552/user/worker/configureServiceActor");

        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> configureFuture = Patterns.ask(configActor, generateServiceConfigCommand, timeout);
        try {
            ExecResult configResult = (ExecResult) Await.result(configureFuture, timeout.duration());
            if (Objects.nonNull(configResult) && configResult.getExecResult()) {
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(serviceRoleInfo);
                }
            }
            return configResult;
        } catch (Exception e) {
            return new ExecResult();
        }
    }
}
