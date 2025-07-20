package com.datasophon.api.master.alert;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.kubernetes.actor.KubernetesStatusServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;


/**
 * 服务实例状态检查 和 告警
 */
public class KubernetesServiceRoleStatusService {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceRoleStatusService.class);

    public void checkStatusAndOpAlert(ClusterServiceRoleInstanceEntity roleInstanceEntity) {

        //logger.info("start to check service status {} in {}", roleInstanceEntity.getServiceRoleName(), roleInstanceEntity.getHostname());

        //准备调用参数
        KubernetesServiceRoleOperateCommand kubernetesServiceRoleOperateCommand = new KubernetesServiceRoleOperateCommand();
        kubernetesServiceRoleOperateCommand.setClusterId(roleInstanceEntity.getClusterId());
        kubernetesServiceRoleOperateCommand.setServiceName(roleInstanceEntity.getServiceName());
        kubernetesServiceRoleOperateCommand.setServiceRoleName(roleInstanceEntity.getServiceRoleName());
        kubernetesServiceRoleOperateCommand.setHostname(roleInstanceEntity.getHostname());
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(roleInstanceEntity.getClusterId());
        kubernetesServiceRoleOperateCommand.setKubeConfig(kubeConfig);
    String namespace = ClusterInfoUtils.getKubernetesNamespace(roleInstanceEntity.getClusterId());
        kubernetesServiceRoleOperateCommand.setNamespace(namespace);

        //调用查询状态
        ActorRef startActor = ActorUtils.getLocalActor(KubernetesStatusServiceActor.class, ActorUtils.getActorRefName(KubernetesStatusServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(startActor, kubernetesServiceRoleOperateCommand, timeout);

        try {

            //处理状态告警
            ExecResult execResult = (ExecResult) Await.result(startFuture, timeout.duration());
            if (execResult.getExecResult()) {
                //状态正常   设置alert
                ProcessUtils.recoverAlert(roleInstanceEntity);
            } else {
                //保存alert
                String alertTargetName = roleInstanceEntity.getServiceRoleName() + " Survive";
                ProcessUtils.saveAlert(roleInstanceEntity, alertTargetName, AlertLevel.EXCEPTION, "restart");
            }

        } catch (Exception e) {
            // save alert
            String alertTargetName = roleInstanceEntity.getServiceRoleName() + " Survive";
            ProcessUtils.saveAlert(roleInstanceEntity, alertTargetName, AlertLevel.EXCEPTION, "restart");
        }


    }
}
