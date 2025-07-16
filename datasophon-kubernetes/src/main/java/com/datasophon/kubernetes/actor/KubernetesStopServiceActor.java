
package com.datasophon.kubernetes.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesStopServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesStopServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof KubernetesServiceRoleOperateCommand) {
            KubernetesServiceRoleOperateCommand command = (KubernetesServiceRoleOperateCommand) msg;
            logger.info("start to stop service role {} on Kubernetes", command.getServiceRoleName());
            ExecResult startResult = new ExecResult();
            KubernetesServiceHandler serviceHandler =
                    new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
            startResult = serviceHandler.stop(command);

            getSender().tell(startResult, getSelf());
            logger.info("service role {} stop on Kubernetes result {}", command.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }

    }
}
