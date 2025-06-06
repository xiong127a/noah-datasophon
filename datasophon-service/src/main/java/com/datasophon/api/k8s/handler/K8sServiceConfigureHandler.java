package com.datasophon.api.k8s.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.util.ObjectUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateServiceConfigCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.K8sConfigureServiceActor;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class K8sServiceConfigureHandler extends ServiceHandler {

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        // config

        ServiceRoleInfo cloneByStream = ObjectUtil.cloneByStream(serviceRoleInfo);
        GenerateServiceConfigCommand generateServiceConfigCommand = new GenerateServiceConfigCommand();
        generateServiceConfigCommand.setServiceName(serviceRoleInfo.getParentName());
        generateServiceConfigCommand.setCofigFileMap(serviceRoleInfo.getConfigFileMap());
        generateServiceConfigCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        generateServiceConfigCommand.setRunAs(serviceRoleInfo.getRunAs());
        if ("zkserver".equalsIgnoreCase(serviceRoleInfo.getName())) {
            generateServiceConfigCommand.setMyid((Integer) CacheUtils.get("zkserver_" + serviceRoleInfo.getHostname()));
        }
        generateServiceConfigCommand.setServiceRoleName(serviceRoleInfo.getName());
        generateServiceConfigCommand.setHostName(serviceRoleInfo.getHostname());
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        generateServiceConfigCommand.setKubeConfig(kubeConfig);
        ActorRef actorRef = ActorUtils.getLocalActor(K8sConfigureServiceActor.class,
                ActorUtils.getActorRefName(K8sConfigureServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> configureFuture = Patterns.ask(actorRef, generateServiceConfigCommand, timeout);
        try {
            ExecResult configResult = (ExecResult) Await.result(configureFuture, timeout.duration());
            if (Objects.nonNull(configResult) && configResult.getExecResult()) {
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(cloneByStream);
                }
            }
            return configResult;
        } catch (Exception e) {
            return new ExecResult();
        }
    }
}
