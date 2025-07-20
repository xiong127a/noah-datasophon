package com.datasophon.kubernetes.actor;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import com.datasophon.common.KubernetesServiceScaleCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesScaleServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesScaleServiceActor extends AbstractActor {

        private static final Logger logger = LoggerFactory.getLogger(KubernetesScaleServiceActor.class);

        @Override
        public Receive createReceive() {
                return ReceiveBuilder.create()
                                .match(KubernetesServiceScaleCommand.class, command -> {
                                        logger.info("start scale service role {}", command.getServiceRoleName());
                                        KubernetesScaleServiceHandler serviceHandler = new KubernetesScaleServiceHandler(
                                                        command.getServiceName(), command.getServiceRoleName());
                                        ExecResult startResult = serviceHandler.scaleService(
                                                        command.getNamespace(),
                                                        command.getKubeConfig(),
                                                        command.getCommandType());
                                        getSender().tell(startResult, getSelf());

                                        logger.info("{} scale {}",
                                                        command.getServiceRoleName(),
                                                        startResult.getExecResult() ? "success" : "failed");
                                })
                                .matchAny(this::unhandled)
                                .build();
        }
}
