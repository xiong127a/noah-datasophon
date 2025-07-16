package com.datasophon.api.kubernetes.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.command.KubernetesGenerateHostTagCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.KubernetesTagHostActor;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class KubernetesHostTagHandler extends ServiceHandler {

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
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
        kubernetesGenerateHostTagCommand.setClusterId(clusterId);
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
