package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sInstallServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sInstallServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sInstallServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof InstallServiceRoleCommand) {
            InstallServiceRoleCommand command = (InstallServiceRoleCommand) msg;
            ExecResult installResult = new ExecResult();
            K8sInstallServiceHandler serviceHandler = new K8sInstallServiceHandler(command.getServiceName(), command.getServiceRoleName());

            logger.info("Start install package {}", command.getPackageName());
            installResult = serviceHandler.install(command);
            getSender().tell(installResult, getSelf());
            logger.info("Install {} {}", command.getPackageName(), installResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
