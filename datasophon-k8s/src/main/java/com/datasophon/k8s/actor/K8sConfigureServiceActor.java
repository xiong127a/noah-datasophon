package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.GenerateServiceConfigCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sConfigureServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sConfigureServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sConfigureServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof GenerateServiceConfigCommand) {

            GenerateServiceConfigCommand command = (GenerateServiceConfigCommand) msg;
            logger.info("start configure {}", command.getServiceName());
            K8sConfigureServiceHandler serviceHandler = new K8sConfigureServiceHandler(command.getServiceName(), command.getServiceRoleName());
            ExecResult startResult = serviceHandler.configure(
                    command.getCofigFileMap(),
                    command.getDecompressPackageName(),
                    command.getMyid(),
                    command.getServiceRoleName(),
                    command.getRunAs(),
                    command.getHostName(),
                    command.getKubeConfig()
                    );
            getSender().tell(startResult, getSelf());

            logger.info("{} configure result {}", command.getServiceName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
