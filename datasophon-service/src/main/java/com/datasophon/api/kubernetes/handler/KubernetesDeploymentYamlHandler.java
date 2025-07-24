package com.datasophon.api.kubernetes.handler;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.KubernetesGenerateDeploymentYamlCommand;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.kubernetes.actor.KubernetesYamlDeploymentActor;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.datasophon.api.utils.ProcessUtils.enableKerberos;
import static com.datasophon.api.utils.ProcessUtils.enableRangerPlugin;

@Slf4j
public class KubernetesDeploymentYamlHandler extends ServiceHandler {
    // 判断是否为安装服务

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) {
        KubernetesGenerateDeploymentYamlCommand kubernetesGenerateDeploymentYamlCommand = new KubernetesGenerateDeploymentYamlCommand();
        kubernetesGenerateDeploymentYamlCommand.setEnableRangerPlugin(serviceRoleInfo.getEnableRangerPlugin());
        kubernetesGenerateDeploymentYamlCommand.setServiceName(serviceRoleInfo.getParentName());
        kubernetesGenerateDeploymentYamlCommand.setServiceRoleName(serviceRoleInfo.getName());
        kubernetesGenerateDeploymentYamlCommand.setCofigFileMap(serviceRoleInfo.getConfigFileMap());
        kubernetesGenerateDeploymentYamlCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        kubernetesGenerateDeploymentYamlCommand.setHostName(serviceRoleInfo.getHostname());
        kubernetesGenerateDeploymentYamlCommand.setStartRunner(serviceRoleInfo.getStartRunner());
        kubernetesGenerateDeploymentYamlCommand.setStopRunner(serviceRoleInfo.getStopRunner());
        kubernetesGenerateDeploymentYamlCommand.setStatusRunner(serviceRoleInfo.getStatusRunner());
        kubernetesGenerateDeploymentYamlCommand.setLogFile(serviceRoleInfo.getLogFile());
        kubernetesGenerateDeploymentYamlCommand.setCommandType(serviceRoleInfo.getCommandType());
        kubernetesGenerateDeploymentYamlCommand.setClusterId(serviceRoleInfo.getClusterId());
        kubernetesGenerateDeploymentYamlCommand.setMasterHost(Objects.nonNull(serviceRoleInfo.getMasterHost())?serviceRoleInfo.getMasterHost():serviceRoleInfo.getHostname());
        if (Objects.nonNull(serviceRoleInfo.getRunAs())) {
            kubernetesGenerateDeploymentYamlCommand.setRunAs(serviceRoleInfo.getRunAs());
        } else {
            RunAs runAs = new RunAs();
            runAs.setUser(Constants.ROOT);
            runAs.setGroup(Constants.ROOT);
            kubernetesGenerateDeploymentYamlCommand.setRunAs(runAs);
        }

        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(serviceRoleInfo.getClusterId());
        String hostMapKey =
                clusterInfo.getClusterCode()
                        + Constants.UNDERLINE
                        + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> map = CacheOperateUtils.getWithType(hostMapKey, new TypeReference<>() {
        });
        if (ObjectUtils.isEmpty(map)){
            log.warn("hostMapKey is empty");
        }
        List<String> hostList =
                map==null?new ArrayList<>():map.get(serviceRoleInfo.getName());
        kubernetesGenerateDeploymentYamlCommand.setRoleNodeCnt(hostList==null?0:hostList.size());

        kubernetesGenerateDeploymentYamlCommand.setEnableKerberos(enableKerberos(serviceRoleInfo.getClusterId(),serviceRoleInfo.getParentName()));
        kubernetesGenerateDeploymentYamlCommand.setEnableRangerPlugin(enableRangerPlugin(serviceRoleInfo.getClusterId(),serviceRoleInfo.getParentName()));
        ActorRef actorRef =
                ActorUtils.getLocalActor(KubernetesYamlDeploymentActor.class, ActorUtils.getActorRefName(KubernetesYamlDeploymentActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> configureFuture = Patterns.ask(actorRef, kubernetesGenerateDeploymentYamlCommand, timeout);
        try {
            ExecResult configResult = (ExecResult) Await.result(configureFuture, timeout.duration());
            if (Objects.nonNull(configResult) && configResult.getExecResult()) {
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(serviceRoleInfo);
                }
            }
            return configResult;
        } catch (Exception e) {
            return new ExecResult();
        }
    }
}
