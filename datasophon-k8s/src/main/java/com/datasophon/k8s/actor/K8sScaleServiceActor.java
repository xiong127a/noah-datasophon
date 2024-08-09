package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.K8sServiceScaleCommand;
import com.datasophon.common.command.K8sGenerateHostTagCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sScaleServiceHandler;
import com.datasophon.k8s.actor.handler.K8sTagHostHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sScaleServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sScaleServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof K8sServiceScaleCommand) {

            K8sServiceScaleCommand command = (K8sServiceScaleCommand) msg;
            logger.info("start scale service role {}", command.getServiceRoleName());
            K8sScaleServiceHandler serviceHandler = new K8sScaleServiceHandler(command.getServiceName(), command.getServiceRoleName());
            ExecResult startResult = serviceHandler.scaleService(
                    command.getKubeConfig(),
                    command.getScaleType()
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
