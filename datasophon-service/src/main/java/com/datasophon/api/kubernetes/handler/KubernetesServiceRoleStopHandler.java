package com.datasophon.api.kubernetes.handler;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.kubernetes.actor.KubernetesStopRolePodActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class KubernetesServiceRoleStopHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceRoleStopHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        logger.info("start to stop service {} in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
        KubernetesServiceRoleOperateCommand kubernetesServiceRoleOperateCommand = new KubernetesServiceRoleOperateCommand();
        kubernetesServiceRoleOperateCommand.setClusterId(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setServiceName(serviceRoleInfo.getParentName());
        kubernetesServiceRoleOperateCommand.setServiceRoleName(serviceRoleInfo.getName());
        kubernetesServiceRoleOperateCommand.setHostname(serviceRoleInfo.getHostname());
        String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setNamespace(namespace);
        ClusterInfoService clusterInfoService =
                SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setKubeConfig(kubeConfig);

        if (serviceRoleInfo.getRoleType() == ServiceRoleType.CLIENT) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            if (Objects.nonNull(getNext())) {
                return getNext().handlerRequest(serviceRoleInfo);
            }
            return execResult;
        }

        ActorRef startActor =
                ActorUtils.getLocalActor(KubernetesStopRolePodActor.class, ActorUtils.getActorRefName(KubernetesStopRolePodActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(startActor, kubernetesServiceRoleOperateCommand, timeout);
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
