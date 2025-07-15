package com.datasophon.kubernetes.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesInstallServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesInstallServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesInstallServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof InstallServiceRoleCommand) {
            InstallServiceRoleCommand command = (InstallServiceRoleCommand) msg;
            ExecResult installResult = new ExecResult();
            KubernetesInstallServiceHandler serviceHandler = new KubernetesInstallServiceHandler(command.getServiceName(), command.getServiceRoleName());

            logger.info("Start install package {}", command.getPackageName());
            installResult = serviceHandler.install(command);
            getSender().tell(installResult, getSelf());
            logger.info("Install {} {}", command.getPackageName(), installResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
