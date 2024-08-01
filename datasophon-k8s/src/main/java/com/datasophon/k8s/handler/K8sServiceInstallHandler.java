package com.datasophon.k8s.handler;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.k8s.actor.K8sInstallServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class K8sServiceInstallHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(K8sServiceInstallHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        ClusterServiceRoleInstanceService roleInstanceService =
                SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
        ClusterServiceRoleInstanceEntity serviceRole = roleInstanceService.getOneServiceRole(serviceRoleInfo.getName(),
                serviceRoleInfo.getHostname(), serviceRoleInfo.getClusterId());
        if (Objects.nonNull(serviceRole)) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            execResult.setExecOut("already installed");
            return execResult;
        }
        InstallServiceRoleCommand installServiceRoleCommand = new InstallServiceRoleCommand();
        installServiceRoleCommand.setServiceName(serviceRoleInfo.getParentName());
        installServiceRoleCommand.setServiceRoleName(serviceRoleInfo.getName());
        installServiceRoleCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        installServiceRoleCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        installServiceRoleCommand.setRunAs(serviceRoleInfo.getRunAs());
        installServiceRoleCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        installServiceRoleCommand.setPackageName(serviceRoleInfo.getPackageName());
        installServiceRoleCommand.setHostName(serviceRoleInfo.getHostname());

        ActorRef actorRef =
                ActorUtils.getLocalActor(K8sInstallServiceActor.class, ActorUtils.getActorRefName(K8sInstallServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> future = Patterns.ask(actorRef, installServiceRoleCommand, timeout);
        try {
            ExecResult installResult = (ExecResult) Await.result(future, timeout.duration());
            if (Objects.nonNull(installResult) && installResult.getExecResult()) {
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(serviceRoleInfo);
                }
            }
            return installResult;
        } catch (Exception e) {
            return new ExecResult();
        }
    }
}
