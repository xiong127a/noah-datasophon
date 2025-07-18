
package com.datasophon.kubernetes.actor;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesStatusHandler;

public class KubernetesStatusServiceActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(KubernetesServiceRoleOperateCommand.class, command -> {
                    // 执行状态检查
                    KubernetesStatusHandler kubernetesStatusHandler = new KubernetesStatusHandler(
                            command.getServiceName(), command.getServiceRoleName());
                    ExecResult startResult = kubernetesStatusHandler.status(command.getNamespace(),
                            command.getKubeConfig(), command.getHostname());

                    // 回调
                    getSender().tell(startResult, getSelf());
                })
                .matchAny(this::unhandled)
                .build();
    }
}
