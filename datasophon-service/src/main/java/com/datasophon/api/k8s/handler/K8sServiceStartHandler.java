package com.datasophon.api.k8s.handler;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.K8sStartServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.datasophon.api.utils.ProcessUtils.enableKerberos;

public class K8sServiceStartHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(K8sServiceStartHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        logger.info("start to start service {} in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
        // 启动
        K8sServiceRoleOperateCommand k8sServiceRoleOperateCommand = new K8sServiceRoleOperateCommand();
        k8sServiceRoleOperateCommand.setServiceName(serviceRoleInfo.getParentName());
        k8sServiceRoleOperateCommand.setServiceRoleName(serviceRoleInfo.getName());
        k8sServiceRoleOperateCommand.setStartRunner(serviceRoleInfo.getStartRunner());
        k8sServiceRoleOperateCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        k8sServiceRoleOperateCommand.setStatusRunner(serviceRoleInfo.getStatusRunner());
        k8sServiceRoleOperateCommand.setSlave(serviceRoleInfo.isSlave());
        k8sServiceRoleOperateCommand.setCommandType(serviceRoleInfo.getCommandType());
        k8sServiceRoleOperateCommand.setMasterHost(serviceRoleInfo.getMasterHost());
        k8sServiceRoleOperateCommand.setManagerHost(serviceRoleInfo.getMasterHost());
        k8sServiceRoleOperateCommand.setClusterId(serviceRoleInfo.getClusterId());
        k8sServiceRoleOperateCommand.setConfigFileMap(serviceRoleInfo.getConfigFileMap());
        k8sServiceRoleOperateCommand.setHostname(serviceRoleInfo.getHostname());
        k8sServiceRoleOperateCommand.setRunAs(serviceRoleInfo.getRunAs());
        k8sServiceRoleOperateCommand.setEnableRangerPlugin(serviceRoleInfo.getEnableRangerPlugin());
        k8sServiceRoleOperateCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        ClusterInfoService clusterInfoService =
                SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        k8sServiceRoleOperateCommand.setKubeConfig(kubeConfig);
        Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
        String nnHost = globalVariables.get("${nn1}");
        k8sServiceRoleOperateCommand.setNnHost(nnHost);
        Boolean enableKerberos = enableKerberos(serviceRoleInfo.getClusterId(),serviceRoleInfo.getParentName());
        logger.info("{} enable kerberos is {}", serviceRoleInfo.getParentName(), enableKerberos);
        k8sServiceRoleOperateCommand.setEnableKerberos(enableKerberos);

        if (serviceRoleInfo.getRoleType() == ServiceRoleType.CLIENT&&!"SPARK3".equals(serviceRoleInfo.getParentName())&&!"FLINK".equals(serviceRoleInfo.getParentName())) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            if (Objects.nonNull(getNext())) {
                return getNext().handlerRequest(serviceRoleInfo);
            }
            return execResult;
        }

        ActorRef startActor =
                ActorUtils.getLocalActor(K8sStartServiceActor.class, ActorUtils.getActorRefName(K8sStartServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(startActor, k8sServiceRoleOperateCommand, timeout);
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
