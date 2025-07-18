
package com.datasophon.kubernetes.actor;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesStopServiceActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesStopServiceActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(KubernetesServiceRoleOperateCommand.class, command -> {
                    logger.info("start to stop service role {} on Kubernetes", command.getServiceRoleName());
                    ExecResult startResult;
                    KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                            command.getServiceRoleName());
                    startResult = serviceHandler.stop(command);

                    getSender().tell(startResult, getSelf());
                    logger.info("service role {} stop on Kubernetes result {}", command.getServiceRoleName(),
                            startResult.getExecResult() ? "success" : "failed");
                })
                .matchAny(this::unhandled)
                .build();
    }
}
