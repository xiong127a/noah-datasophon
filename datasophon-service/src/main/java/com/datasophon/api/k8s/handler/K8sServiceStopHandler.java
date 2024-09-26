package com.datasophon.api.k8s.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.K8sStopServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class K8sServiceStopHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(K8sServiceStopHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        logger.info("start to stop service {} in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
        K8sServiceRoleOperateCommand k8sServiceRoleOperateCommand = new K8sServiceRoleOperateCommand();
        k8sServiceRoleOperateCommand.setServiceName(serviceRoleInfo.getParentName());
        k8sServiceRoleOperateCommand.setServiceRoleName(serviceRoleInfo.getName());

        ClusterInfoService clusterInfoService =
                SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        k8sServiceRoleOperateCommand.setKubeConfig(kubeConfig);

        if (serviceRoleInfo.getRoleType() == ServiceRoleType.CLIENT) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            if (Objects.nonNull(getNext())) {
                return getNext().handlerRequest(serviceRoleInfo);
            }
            return execResult;
        }

        ActorRef stopActor =
                ActorUtils.getLocalActor(K8sStopServiceActor.class, ActorUtils.getActorRefName(K8sStopServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(stopActor, k8sServiceRoleOperateCommand, timeout);
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
