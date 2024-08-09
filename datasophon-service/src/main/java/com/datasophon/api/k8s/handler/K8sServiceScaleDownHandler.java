package com.datasophon.api.k8s.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.K8sServiceScaleCommand;
import com.datasophon.common.enums.K8sScaleType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.K8sScaleServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class K8sServiceScaleDownHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(K8sServiceScaleDownHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        logger.info("start to scale down service role {}", serviceRoleInfo.getName());
        K8sServiceScaleCommand k8sServiceScaleCommand = new K8sServiceScaleCommand();
        k8sServiceScaleCommand.setServiceName(serviceRoleInfo.getParentName());
        k8sServiceScaleCommand.setServiceRoleName(serviceRoleInfo.getName());
        k8sServiceScaleCommand.setScaleType(K8sScaleType.SCALE_DOWN);

        ClusterInfoService clusterInfoService =
                SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        k8sServiceScaleCommand.setKubeConfig(kubeConfig);

        ActorRef startActor =
                ActorUtils.getLocalActor(K8sScaleServiceActor.class, ActorUtils.getActorRefName(K8sScaleServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(startActor, k8sServiceScaleCommand, timeout);
        try {
            ExecResult startResult = (ExecResult) Await.result(startFuture, timeout.duration());
            if (Objects.nonNull(startResult) && startResult.getExecResult()) {
                // 角色停止成功
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
