
package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sStopRolePodHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sStopRolePodActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sStopRolePodActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof K8sServiceRoleOperateCommand) {
            K8sServiceRoleOperateCommand command = (K8sServiceRoleOperateCommand) msg;
            logger.info("start to stop service role {} on k8s", command.getServiceRoleName());
            ExecResult startResult = new ExecResult();
            K8sStopRolePodHandler k8SStopRolePodHandler =
                    new K8sStopRolePodHandler(command.getServiceName(), command.getServiceRoleName());

            startResult = k8SStopRolePodHandler.stop(command.getKubeConfig(), command.getHostname());

            getSender().tell(startResult, getSelf());
            logger.info("service role {} stop on k8s result {}", command.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }

    }
}
