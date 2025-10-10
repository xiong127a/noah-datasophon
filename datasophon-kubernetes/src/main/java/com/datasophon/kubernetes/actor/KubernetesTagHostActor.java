package com.datasophon.kubernetes.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.KubernetesGenerateHostTagCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesTagHostHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesTagHostActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesTagHostActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof KubernetesGenerateHostTagCommand) {

            KubernetesGenerateHostTagCommand command = (KubernetesGenerateHostTagCommand) msg;
            logger.info("start add service tag {}", command.getServiceRoleName());
            KubernetesTagHostHandler serviceHandler = new KubernetesTagHostHandler(command.getNamespace(),command.getServiceName(), command.getServiceRoleName());
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
