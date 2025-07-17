package com.datasophon.api.k8s.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sGenerateDeploymentYamlCommand;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.K8sYamlDeploymentActor;
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

public class K8sDeploymentYamlHandler extends ServiceHandler {

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        K8sGenerateDeploymentYamlCommand k8SGenerateDeploymentYamlCommand = new K8sGenerateDeploymentYamlCommand();
        k8SGenerateDeploymentYamlCommand.setEnableRangerPlugin(serviceRoleInfo.getEnableRangerPlugin());
        k8SGenerateDeploymentYamlCommand.setServiceName(serviceRoleInfo.getParentName());
        k8SGenerateDeploymentYamlCommand.setServiceRoleName(serviceRoleInfo.getName());
        k8SGenerateDeploymentYamlCommand.setCofigFileMap(serviceRoleInfo.getConfigFileMap());
        k8SGenerateDeploymentYamlCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        k8SGenerateDeploymentYamlCommand.setHostName(serviceRoleInfo.getHostname());
        k8SGenerateDeploymentYamlCommand.setStartRunner(serviceRoleInfo.getStartRunner());
        k8SGenerateDeploymentYamlCommand.setStopRunner(serviceRoleInfo.getStopRunner());
        k8SGenerateDeploymentYamlCommand.setStatusRunner(serviceRoleInfo.getStatusRunner());
        k8SGenerateDeploymentYamlCommand.setLogFile(serviceRoleInfo.getLogFile());
        k8SGenerateDeploymentYamlCommand.setMasterHost(Objects.nonNull(serviceRoleInfo.getMasterHost())?serviceRoleInfo.getMasterHost():serviceRoleInfo.getHostname());
        if (Objects.nonNull(serviceRoleInfo.getRunAs())) {
            k8SGenerateDeploymentYamlCommand.setRunAs(serviceRoleInfo.getRunAs());
        } else {
            RunAs runAs = new RunAs();
            runAs.setUser(Constants.ROOT);
            runAs.setGroup(Constants.ROOT);
            k8SGenerateDeploymentYamlCommand.setRunAs(runAs);
        }

//        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
//        ClusterInfoEntity clusterInfo = clusterInfoService.getById(serviceRoleInfo.getClusterId());
        String hostMapKey =
                serviceRoleInfo.getClusterId()
                        + Constants.UNDERLINE
                        + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> map = (HashMap<String, List<String>>) CacheOperateUtils.get(hostMapKey);
        if (ObjectUtils.isEmpty(map)){

        }
        List<String> hostList =
                map==null?new ArrayList<>():map.get(serviceRoleInfo.getName());
        k8SGenerateDeploymentYamlCommand.setRoleNodeCnt(hostList==null?0:hostList.size());

        k8SGenerateDeploymentYamlCommand.setEnableKerberos(enableKerberos(serviceRoleInfo.getClusterId(),serviceRoleInfo.getParentName()));
        k8SGenerateDeploymentYamlCommand.setEnableRangerPlugin(enableRangerPlugin(serviceRoleInfo.getClusterId(),serviceRoleInfo.getParentName()));
        ActorRef actorRef =
                ActorUtils.getLocalActor(K8sYamlDeploymentActor.class, ActorUtils.getActorRefName(K8sYamlDeploymentActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> configureFuture = Patterns.ask(actorRef, k8SGenerateDeploymentYamlCommand, timeout);
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
