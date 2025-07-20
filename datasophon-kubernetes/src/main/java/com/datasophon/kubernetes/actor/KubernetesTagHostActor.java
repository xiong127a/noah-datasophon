package com.datasophon.kubernetes.actor;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import com.datasophon.common.command.KubernetesGenerateHostTagCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesTagHostHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesTagHostActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesTagHostActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(KubernetesGenerateHostTagCommand.class, command -> {
                    logger.info("start add service tag {}", command.getServiceRoleName());
                    KubernetesTagHostHandler serviceHandler = new KubernetesTagHostHandler(command.getNamespace(),
                            command.getServiceName(), command.getServiceRoleName());
                    ExecResult startResult = serviceHandler.operateTag(
                            command.getClusterId(),
                            command.getHostName(),
                            command.getKubeConfig(),
                            command.getCommandType());
                    getSender().tell(startResult, getSelf());

                    logger.info("{} tag at host {} {}",
                            command.getServiceRoleName(),
                            command.getHostName(),
                            startResult.getExecResult() ? "success" : "failed");
                })
                .matchAny(this::unhandled)
                .build();
    }
}
