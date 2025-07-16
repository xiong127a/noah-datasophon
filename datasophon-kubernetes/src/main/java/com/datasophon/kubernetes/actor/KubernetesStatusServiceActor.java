
package com.datasophon.kubernetes.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesStatusHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KubernetesStatusServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesStatusServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof KubernetesServiceRoleOperateCommand) {

            KubernetesServiceRoleOperateCommand command = (KubernetesServiceRoleOperateCommand) msg;
//            logger.info("start to check service role status {} on Kubernetes", command.getServiceRoleName());

            //执行状态检查
            KubernetesStatusHandler kubernetesStatusHandler = new KubernetesStatusHandler(command.getServiceName(), command.getServiceRoleName());
            ExecResult startResult = kubernetesStatusHandler.status(command.getNamespace(),command.getKubeConfig(), command.getHostname());

            //回调
            getSender().tell(startResult, getSelf());
//            logger.info("service role {} status check  on Kubernetes result {}", command.getServiceRoleName(), startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }

    }
}
