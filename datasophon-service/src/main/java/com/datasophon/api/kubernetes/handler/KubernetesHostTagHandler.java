/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.kubernetes.handler;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.command.KubernetesGenerateHostTagCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.KubernetesTagHostActor;
import com.datasophon.api.utils.ClusterInfoUtils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Kubernetes主机标签处理器
 * 负责为Kubernetes环境下的服务角色生成主机标签
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-05
 */
public class KubernetesHostTagHandler extends ServiceHandler {

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) {
        KubernetesGenerateHostTagCommand kubernetesGenerateHostTagCommand = new KubernetesGenerateHostTagCommand();
        kubernetesGenerateHostTagCommand.setHostName(serviceRoleInfo.getHostname());
        kubernetesGenerateHostTagCommand.setServiceName(serviceRoleInfo.getParentName());
        kubernetesGenerateHostTagCommand.setServiceRoleName(serviceRoleInfo.getName());
        kubernetesGenerateHostTagCommand.setCommandType(serviceRoleInfo.getCommandType());
        Integer clusterId = serviceRoleInfo.getClusterId();
        ClusterInfoService clusterInfoService =
                SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(clusterId);
        kubernetesGenerateHostTagCommand.setClusterId(clusterId);
        kubernetesGenerateHostTagCommand.setKubeConfig(kubeConfig);
        String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterId);
        kubernetesGenerateHostTagCommand.setNamespace(namespace);
        ActorRef actorRef =
                ActorUtils.getLocalActor(KubernetesTagHostActor.class, ActorUtils.getActorRefName(KubernetesTagHostActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> configureFuture = Patterns.ask(actorRef, kubernetesGenerateHostTagCommand, timeout);
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
