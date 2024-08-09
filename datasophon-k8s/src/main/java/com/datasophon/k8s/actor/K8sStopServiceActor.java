
package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.strategy.K8sServiceRoleStrategy;
import com.datasophon.k8s.strategy.K8sServiceRoleStrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class K8sStopServiceActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sStopServiceActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof K8sServiceRoleOperateCommand) {
            K8sServiceRoleOperateCommand command = (K8sServiceRoleOperateCommand) msg;
            logger.info("start to stop service role {} on k8s", command.getServiceRoleName());
            ExecResult startResult = new ExecResult();
            K8sServiceHandler serviceHandler =
                    new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());

            K8sServiceRoleStrategy serviceRoleHandler =
                    K8sServiceRoleStrategyContext.getServiceRoleHandler(command.getServiceRoleName());
            if (Objects.nonNull(serviceRoleHandler)) {
                startResult = serviceRoleHandler.handler(command);
            } else {
                startResult = serviceHandler.stop(command.getKubeConfig());
            }

            getSender().tell(startResult, getSelf());
            logger.info("service role {} stop on k8s result {}", command.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }

    }
}
