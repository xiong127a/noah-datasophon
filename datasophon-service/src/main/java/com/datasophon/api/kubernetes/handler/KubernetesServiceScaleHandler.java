package com.datasophon.api.kubernetes.handler;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.KubernetesServiceScaleCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.kubernetes.actor.KubernetesScaleServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class KubernetesServiceScaleHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceScaleHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        KubernetesServiceScaleCommand kubernetesServiceScaleCommand = new KubernetesServiceScaleCommand();
        kubernetesServiceScaleCommand.setServiceName(serviceRoleInfo.getParentName());
        kubernetesServiceScaleCommand.setServiceRoleName(serviceRoleInfo.getName());
        String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
        kubernetesServiceScaleCommand.setNamespace(namespace);

        ClusterInfoService clusterInfoService =
                SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        kubernetesServiceScaleCommand.setKubeConfig(kubeConfig);
        kubernetesServiceScaleCommand.setCommandType(serviceRoleInfo.getCommandType());
        ActorRef startActor =
                ActorUtils.getLocalActor(KubernetesScaleServiceActor.class, ActorUtils.getActorRefName(KubernetesScaleServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(startActor, kubernetesServiceScaleCommand, timeout);
        try {
            ExecResult startResult = (ExecResult) Await.result(startFuture, timeout.duration());
            if (Objects.nonNull(startResult) && startResult.getExecResult()) {
                // 角色启动成功
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(serviceRoleInfo);
                }
            }
            return startResult;
        } catch (Exception e) {
            return new ExecResult();
        }
    }
}
