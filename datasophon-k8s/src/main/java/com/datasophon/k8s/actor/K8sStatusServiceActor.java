
package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sStatusHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class K8sStatusServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sStatusServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof K8sServiceRoleOperateCommand) {

            K8sServiceRoleOperateCommand command = (K8sServiceRoleOperateCommand) msg;
            logger.info("start to check service role status {} on k8s", command.getServiceRoleName());

            //执行状态检查
            K8sStatusHandler k8sStatusHandler = new K8sStatusHandler(command.getServiceName(), command.getServiceRoleName());
            ExecResult startResult = k8sStatusHandler.status(command.getKubeConfig(), command.getHostname());

            //回调
            getSender().tell(startResult, getSelf());
            logger.info("service role {} status check  on k8s result {}", command.getServiceRoleName(), startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }

    }
}
