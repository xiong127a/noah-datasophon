package com.datasophon.api.kubernetes.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.kubernetes.actor.KubernetesInstallServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class KubernetesServiceInstallHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceInstallHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        ClusterServiceRoleInstanceService roleInstanceService =
                SpringUtil.getBean(ClusterServiceRoleInstanceService.class);
        ClusterServiceRoleInstanceEntity serviceRole = roleInstanceService.getOneServiceRole(serviceRoleInfo.getName(),
                serviceRoleInfo.getHostname(), serviceRoleInfo.getClusterId());
        Map<Generators, List<ServiceConfig>> configFileMap = serviceRoleInfo.getConfigFileMap();
        if (Objects.nonNull(serviceRole)) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            execResult.setExecOut("already installed");
            return execResult;
        }
        if (ObjectUtils.isEmpty(serviceRoleInfo.getRunAs())) {
            RunAs runAs = new RunAs();
            runAs.setUser(Constants.ROOT);
            runAs.setGroup(Constants.ROOT);
            serviceRoleInfo.setRunAs(runAs);
        }
        InstallServiceRoleCommand installServiceRoleCommand = new InstallServiceRoleCommand();
        installServiceRoleCommand.setServiceName(serviceRoleInfo.getParentName());
        installServiceRoleCommand.setServiceRoleName(serviceRoleInfo.getName());
        installServiceRoleCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        installServiceRoleCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        installServiceRoleCommand.setRunAs(serviceRoleInfo.getRunAs());
        installServiceRoleCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        installServiceRoleCommand.setPackageName(serviceRoleInfo.getPackageName());
        installServiceRoleCommand.setHostName(serviceRoleInfo.getHostname());
        installServiceRoleCommand.setLogFile(serviceRoleInfo.getLogFile());
        installServiceRoleCommand.setCofigFileMap(configFileMap);

        ActorRef actorRef =
                ActorUtils.getLocalActor(KubernetesInstallServiceActor.class, ActorUtils.getActorRefName(KubernetesInstallServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> future = Patterns.ask(actorRef, installServiceRoleCommand, timeout);
        try {
            ExecResult installResult = (ExecResult) Await.result(future, timeout.duration());
            if (Objects.nonNull(installResult) && installResult.getExecResult()) {
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(serviceRoleInfo);
                }
            }
            return installResult;
        } catch (Exception e) {
            return new ExecResult();
        }
    }
}
