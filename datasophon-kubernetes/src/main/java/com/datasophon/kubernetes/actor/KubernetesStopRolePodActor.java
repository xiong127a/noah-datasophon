
package com.datasophon.kubernetes.actor;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesStopRolePodHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesStopRolePodActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesStopRolePodActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(KubernetesServiceRoleOperateCommand.class, command -> {
                    logger.info("start to stop service role {} on Kubernetes", command.getServiceRoleName());
                    ExecResult startResult;
                    KubernetesStopRolePodHandler kubernetesStopRolePodHandler = new KubernetesStopRolePodHandler(
                            command.getServiceName(), command.getServiceRoleName());

                    startResult = kubernetesStopRolePodHandler.stop(command.getNamespace(), command.getKubeConfig(),
                            command.getHostname());

                    getSender().tell(startResult, getSelf());
                    logger.info("service role {} stop on Kubernetes result {}", command.getServiceRoleName(),
                            startResult.getExecResult() ? "success" : "failed");
                })
                .matchAny(this::unhandled)
                .build();
    }
}
