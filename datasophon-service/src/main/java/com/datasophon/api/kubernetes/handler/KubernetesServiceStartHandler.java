package com.datasophon.api.kubernetes.handler;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.kubernetes.actor.KubernetesStartServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.datasophon.api.utils.ProcessUtils.enableKerberos;

public class KubernetesServiceStartHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceStartHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        logger.info("start to start service {} in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
        // 启动
        Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
        KubernetesServiceRoleOperateCommand kubernetesServiceRoleOperateCommand = new KubernetesServiceRoleOperateCommand();
        kubernetesServiceRoleOperateCommand.setServiceName(serviceRoleInfo.getParentName());
        kubernetesServiceRoleOperateCommand.setServiceRoleName(serviceRoleInfo.getName());
        kubernetesServiceRoleOperateCommand.setStartRunner(serviceRoleInfo.getStartRunner());
        kubernetesServiceRoleOperateCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        kubernetesServiceRoleOperateCommand.setStatusRunner(serviceRoleInfo.getStatusRunner());
        kubernetesServiceRoleOperateCommand.setSlave(serviceRoleInfo.isSlave());
        kubernetesServiceRoleOperateCommand.setCommandType(serviceRoleInfo.getCommandType());
        kubernetesServiceRoleOperateCommand.setMasterHost(serviceRoleInfo.getMasterHost());
        kubernetesServiceRoleOperateCommand.setManagerHost(serviceRoleInfo.getMasterHost());
        kubernetesServiceRoleOperateCommand.setClusterId(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setConfigFileMap(serviceRoleInfo.getConfigFileMap());
        kubernetesServiceRoleOperateCommand.setHostname(serviceRoleInfo.getHostname());
        String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setNamespace(namespace);
        kubernetesServiceRoleOperateCommand.setRunAs(serviceRoleInfo.getRunAs());
        kubernetesServiceRoleOperateCommand.setEnableRangerPlugin(serviceRoleInfo.getEnableRangerPlugin());
        kubernetesServiceRoleOperateCommand.setServiceRoleType(serviceRoleInfo.getRoleType());
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setKubeConfig(kubeConfig);
        kubernetesServiceRoleOperateCommand.setGraphHost(globalVariables.get("${nebulaGraphHost}"));
        kubernetesServiceRoleOperateCommand.setNnHost(globalVariables.get("${nn1}"));
        Boolean enableKerberos = enableKerberos(serviceRoleInfo.getClusterId(), serviceRoleInfo.getParentName());
        logger.info("{} enable kerberos is {}", serviceRoleInfo.getParentName(), enableKerberos);
        kubernetesServiceRoleOperateCommand.setEnableKerberos(enableKerberos);

        List<String> needClientService = Arrays.asList("SPARK3", "FLINK", "FLUME", "JUICEFS", "LOGSTASH");
        if (serviceRoleInfo.getRoleType() == ServiceRoleType.CLIENT
                && !needClientService.contains(serviceRoleInfo.getParentName())) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            if (Objects.nonNull(getNext())) {
                return getNext().handlerRequest(serviceRoleInfo);
            }
            return execResult;
        }

        ActorRef startActor = ActorUtils.getLocalActor(KubernetesStartServiceActor.class,
                ActorUtils.getActorRefName(KubernetesStartServiceActor.class));
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
