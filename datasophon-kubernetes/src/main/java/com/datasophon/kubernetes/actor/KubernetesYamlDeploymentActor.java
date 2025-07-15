package com.datasophon.kubernetes.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.KubernetesGenerateDeploymentYamlCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesYamlDeploymentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesYamlDeploymentActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesYamlDeploymentActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof KubernetesGenerateDeploymentYamlCommand) {
            KubernetesGenerateDeploymentYamlCommand command = (KubernetesGenerateDeploymentYamlCommand) msg;
            logger.info("start configure {} Kubernetes yaml file", command.getServiceRoleName());
            KubernetesYamlDeploymentHandler serviceHandler = new KubernetesYamlDeploymentHandler(command.getServiceName(), command.getServiceRoleName());
            ExecResult startResult = serviceHandler.configure(
                    command.getCofigFileMap(),
                    command.getRunAs(),
                    command.getStartRunner(),
                    command.getStatusRunner(),
                    command.getRoleNodeCnt(),
                    command.getDecompressPackageName(),
                    command.getLogFile(),
                    command.getHostName(),
                    command.getServiceRoleName(),
                    command.getMasterHost(),
                    command.getEnableKerberos(),
                    command.getEnableRangerPlugin(),
                    command.getCommandType()
            );
            getSender().tell(startResult, getSelf());
            logger.info("{} configure Kubernetes yaml file result {}", command.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
