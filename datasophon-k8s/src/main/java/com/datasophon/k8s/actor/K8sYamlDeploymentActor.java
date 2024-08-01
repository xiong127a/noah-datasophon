package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.GenerateDeploymentYamlCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sYamlDeploymentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sYamlDeploymentActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sYamlDeploymentActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof GenerateDeploymentYamlCommand) {

            GenerateDeploymentYamlCommand command = (GenerateDeploymentYamlCommand) msg;
            logger.info("start configure {}", command.getServiceName());
            K8sYamlDeploymentHandler serviceHandler = new K8sYamlDeploymentHandler(command.getServiceName(), command.getServiceRoleName());
            ExecResult startResult = serviceHandler.configure(
                    command.getCofigFileMap(),
                    command.getDecompressPackageName(),
                    command.getHostName(),
                    command.getClusterId()
                    );
            getSender().tell(startResult, getSelf());

            logger.info("{} configure result {}", command.getServiceName(),
                    startResult.getExecResult() ? "success" : "failed");
        } else {
            unhandled(msg);
        }
    }
}
