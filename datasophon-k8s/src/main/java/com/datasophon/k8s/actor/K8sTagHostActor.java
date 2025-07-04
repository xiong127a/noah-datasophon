package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.K8sGenerateHostTagCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sTagHostHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sTagHostActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sTagHostActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof K8sGenerateHostTagCommand) {

            K8sGenerateHostTagCommand command = (K8sGenerateHostTagCommand) msg;
            logger.info("start add service tag {}", command.getServiceRoleName());
            K8sTagHostHandler serviceHandler = new K8sTagHostHandler(command.getServiceName(), command.getServiceRoleName());
            ExecResult startResult = serviceHandler.operateTag(
                    command.getClusterId(),
                    command.getHostName(),
                    command.getKubeConfig(),
                    command.getCommandType()
            );
            getSender().tell(startResult, getSelf());

            logger.info("{} tag at host {} {}",
                    command.getServiceRoleName(),
                    command.getHostName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
