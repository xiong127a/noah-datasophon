package com.datasophon.kubernetes.actor;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.strategy.KubernetesServiceRoleStrategy;
import com.datasophon.kubernetes.strategy.KubernetesServiceRoleStrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class KubernetesStartServiceActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesStartServiceActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(KubernetesServiceRoleOperateCommand.class, command -> {
                    logger.info("start to start service role {} on Kubernetes", command.getServiceRoleName());
                    new ExecResult();
                    ExecResult startResult;
                    KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                            command.getServiceRoleName());

                    KubernetesServiceRoleStrategy serviceRoleHandler = KubernetesServiceRoleStrategyContext
                            .getServiceRoleHandler(command.getServiceRoleName());
                    if (Objects.nonNull(serviceRoleHandler)) {
                        startResult = serviceRoleHandler.handler(command);
                    } else {
                        startResult = serviceHandler.start(command);
                    }

                    getSender().tell(startResult, getSelf());
                    logger.info("service role {} start on Kubernetes result {}", command.getServiceRoleName(),
                            startResult.getExecResult() ? "success" : "failed");
                })
                .matchAny(this::unhandled)
                .build();
    }
}
