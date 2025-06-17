package com.datasophon.api.k8s.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.command.K8sGenerateHostTagCommand;
import com.datasophon.common.enums.K8sHostTagOperation;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.K8sTagHostActor;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class K8sHostCancelTagHandler extends ServiceHandler {

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        K8sGenerateHostTagCommand k8SGenerateHostTagCommand = new K8sGenerateHostTagCommand();
        k8SGenerateHostTagCommand.setHostName(serviceRoleInfo.getHostname());
        k8SGenerateHostTagCommand.setServiceName(serviceRoleInfo.getParentName());
        k8SGenerateHostTagCommand.setServiceRoleName(serviceRoleInfo.getName());
        k8SGenerateHostTagCommand.setTagOperation(K8sHostTagOperation.CANCEL_TAG);
        Integer clusterId = serviceRoleInfo.getClusterId();
        ClusterInfoService clusterInfoService =
                SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(clusterId);
        k8SGenerateHostTagCommand.setClusterId(clusterId);
        k8SGenerateHostTagCommand.setKubeConfig(kubeConfig);

        ActorRef actorRef =
                ActorUtils.getLocalActor(K8sTagHostActor.class, ActorUtils.getActorRefName(K8sTagHostActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> configureFuture = Patterns.ask(actorRef, k8SGenerateHostTagCommand, timeout);
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
