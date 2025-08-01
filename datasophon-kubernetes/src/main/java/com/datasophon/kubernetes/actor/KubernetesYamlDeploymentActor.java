package com.datasophon.kubernetes.actor;

import com.datasophon.common.command.KubernetesGenerateDeploymentYamlCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesYamlDeploymentHandler;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesYamlDeploymentActor extends AbstractActor {

        private static final Logger logger = LoggerFactory.getLogger(KubernetesYamlDeploymentActor.class);

        @Override
        public Receive createReceive() {
                return ReceiveBuilder.create()
                                .match(KubernetesGenerateDeploymentYamlCommand.class, command -> {
                                        logger.info("start configure {} Kubernetes yaml file",
                                                        command.getServiceRoleName());
                                        KubernetesYamlDeploymentHandler serviceHandler = new KubernetesYamlDeploymentHandler(
                                                        command.getServiceName(), command.getServiceRoleName());
                                        ExecResult startResult = serviceHandler.configure(
                                                        command.getNamespace(),
                                                        command.getCofigFileMap(),
                                                        command.getRunAs(),
                                                        command.getStartRunner(),
                                                        command.getStatusRunner(),
                                                        command.getRoleNodeCnt(),
                                                        command.getDecompressPackageName(),
                                                        command.getLogFile(),
                                                        command.getServiceRoleName(),
                                                        command.getMasterHost(),
                                                        command.getEnableKerberos(),
                                                        command.getEnableRangerPlugin(),
                                                        command.getCommandType());
                                        getSender().tell(startResult, getSelf());
                                        logger.info("{} configure Kubernetes yaml file result {}",
                                                        command.getServiceRoleName(),
                                                        startResult.getExecResult() ? "success" : "failed");
                                })
                                .matchAny(this::unhandled)
                                .build();
        }
}
