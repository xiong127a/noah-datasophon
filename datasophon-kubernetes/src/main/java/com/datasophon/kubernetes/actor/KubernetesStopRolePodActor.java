
package com.datasophon.kubernetes.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesStopRolePodHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesStopRolePodActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesStopRolePodActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof KubernetesServiceRoleOperateCommand) {
            KubernetesServiceRoleOperateCommand command = (KubernetesServiceRoleOperateCommand) msg;
            logger.info("start to stop service role {} on Kubernetes", command.getServiceRoleName());
            ExecResult startResult = new ExecResult();
            KubernetesStopRolePodHandler kubernetesStopRolePodHandler =
                    new KubernetesStopRolePodHandler(command.getServiceName(), command.getServiceRoleName());

            startResult = kubernetesStopRolePodHandler.stop(command.getNamespace(),command.getKubeConfig(), command.getHostname());

            getSender().tell(startResult, getSelf());
            logger.info("service role {} stop on Kubernetes result {}", command.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }

    }
}
