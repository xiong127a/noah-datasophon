package com.datasophon.k8s.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.common.command.GenerateDeploymentYamlCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.K8sYamlDeploymentActor;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class K8sDeploymentYamlHandler extends ServiceHandler {

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        GenerateDeploymentYamlCommand generateDeploymentYamlCommand = new GenerateDeploymentYamlCommand();
        generateDeploymentYamlCommand.setServiceName(serviceRoleInfo.getParentName());
        generateDeploymentYamlCommand.setCofigFileMap(serviceRoleInfo.getConfigFileMap());
        generateDeploymentYamlCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        generateDeploymentYamlCommand.setRunAs(serviceRoleInfo.getRunAs());
        generateDeploymentYamlCommand.setServiceRoleName(serviceRoleInfo.getName());
        generateDeploymentYamlCommand.setHostName(serviceRoleInfo.getHostname());
        generateDeploymentYamlCommand.setClusterId(serviceRoleInfo.getClusterId());

        ActorRef actorRef =
                ActorUtils.getLocalActor(K8sYamlDeploymentActor.class, ActorUtils.getActorRefName(K8sYamlDeploymentActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> configureFuture = Patterns.ask(actorRef, generateDeploymentYamlCommand, timeout);
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
