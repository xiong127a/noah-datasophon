package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.K8sGenerateDeploymentYamlCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sYamlDeploymentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sYamlDeploymentActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sYamlDeploymentActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof K8sGenerateDeploymentYamlCommand) {
            K8sGenerateDeploymentYamlCommand command = (K8sGenerateDeploymentYamlCommand) msg;
            logger.info("start configure {} k8s yaml file", command.getServiceRoleName());
            K8sYamlDeploymentHandler serviceHandler = new K8sYamlDeploymentHandler(command.getServiceName(), command.getServiceRoleName());
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
            logger.info("{} configure k8s yaml file result {}", command.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
