package com.datasophon.kubernetes.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.GenerateServiceConfigCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesConfigureServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesConfigureServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesConfigureServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof GenerateServiceConfigCommand) {

            GenerateServiceConfigCommand command = (GenerateServiceConfigCommand) msg;
            logger.info("start configure {}", command.getServiceName());
            KubernetesConfigureServiceHandler serviceHandler = new KubernetesConfigureServiceHandler(command.getServiceName(), command.getServiceRoleName());
            ExecResult startResult = serviceHandler.configure(
                    command.getNamespace(),
                    command.getCofigFileMap(),
                    command.getDecompressPackageName(),
                    command.getMyid(),
                    command.getServiceRoleName(),
                    command.getRunAs(),
                    command.getHostName()
                    );
            getSender().tell(startResult, getSelf());

            logger.info("{} configure result {}", command.getServiceName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
