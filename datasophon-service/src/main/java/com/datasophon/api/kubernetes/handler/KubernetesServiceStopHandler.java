package com.datasophon.api.kubernetes.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.KubernetesStopServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class KubernetesServiceStopHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceStopHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        logger.info("start to stop service {} in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
        KubernetesServiceRoleOperateCommand kubernetesServiceRoleOperateCommand = new KubernetesServiceRoleOperateCommand();
        kubernetesServiceRoleOperateCommand.setServiceName(serviceRoleInfo.getParentName());
        kubernetesServiceRoleOperateCommand.setServiceRoleName(serviceRoleInfo.getName());

        ClusterInfoService clusterInfoService =
                SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setKubeConfig(kubeConfig);
        String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setNamespace(namespace);

        if (serviceRoleInfo.getRoleType() == ServiceRoleType.CLIENT) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            if (Objects.nonNull(getNext())) {
                return getNext().handlerRequest(serviceRoleInfo);
            }
            return execResult;
        }

        ActorRef stopActor =
                ActorUtils.getLocalActor(KubernetesStopServiceActor.class, ActorUtils.getActorRefName(KubernetesStopServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(stopActor, kubernetesServiceRoleOperateCommand, timeout);
        try {
            ExecResult startResult = (ExecResult) Await.result(startFuture, timeout.duration());
            if (Objects.nonNull(startResult) && startResult.getExecResult()) {
                // 角色删除成功
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
