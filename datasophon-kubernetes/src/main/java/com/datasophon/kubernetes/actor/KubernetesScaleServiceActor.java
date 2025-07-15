package com.datasophon.kubernetes.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.KubernetesServiceScaleCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesScaleServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesScaleServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesScaleServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof KubernetesServiceScaleCommand) {

            KubernetesServiceScaleCommand command = (KubernetesServiceScaleCommand) msg;
            logger.info("start scale service role {}", command.getServiceRoleName());
            KubernetesScaleServiceHandler serviceHandler = new KubernetesScaleServiceHandler(command.getServiceName(), command.getServiceRoleName());
            ExecResult startResult = serviceHandler.scaleService(
                    command.getKubeConfig(),
                    command.getCommandType()
            );
            getSender().tell(startResult, getSelf());

            logger.info("{} scale {}",
                    command.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
