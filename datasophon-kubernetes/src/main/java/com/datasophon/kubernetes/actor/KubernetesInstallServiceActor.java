package com.datasophon.kubernetes.actor;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesInstallServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesInstallServiceActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesInstallServiceActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(InstallServiceRoleCommand.class, command -> {
                    ExecResult installResult = new ExecResult();
                    KubernetesInstallServiceHandler serviceHandler = new KubernetesInstallServiceHandler(
                            command.getServiceName(), command.getServiceRoleName());

                    logger.info("Start install package {}", command.getPackageName());
                    installResult = serviceHandler.install(command);
                    getSender().tell(installResult, getSelf());
                    logger.info("Install {} {}", command.getPackageName(),
                            installResult.getExecResult() ? "success" : "failed");
                })
                .matchAny(this::unhandled)
                .build();
    }
}
