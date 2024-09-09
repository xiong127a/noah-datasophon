package com.datasophon.api.master.alert;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.k8s.actor.K8sStatusServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;


/**
 * 服务实例状态检查 和 告警
 */
public class K8sServiceRoleStatusService {

    private static final Logger logger = LoggerFactory.getLogger(K8sServiceRoleStatusService.class);

    public void checkStatusAndOpAlert(ClusterServiceRoleInstanceEntity roleInstanceEntity) {

        //logger.info("start to check service status {} in {}", roleInstanceEntity.getServiceRoleName(), roleInstanceEntity.getHostname());

        //准备调用参数
        K8sServiceRoleOperateCommand k8sServiceRoleOperateCommand = new K8sServiceRoleOperateCommand();
        k8sServiceRoleOperateCommand.setClusterId(roleInstanceEntity.getClusterId());
        k8sServiceRoleOperateCommand.setServiceName(roleInstanceEntity.getServiceName());
        k8sServiceRoleOperateCommand.setServiceRoleName(roleInstanceEntity.getServiceRoleName());
        k8sServiceRoleOperateCommand.setHostname(roleInstanceEntity.getHostname());
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(roleInstanceEntity.getClusterId());
        k8sServiceRoleOperateCommand.setKubeConfig(kubeConfig);


        //调用查询状态
        ActorRef startActor = ActorUtils.getLocalActor(K8sStatusServiceActor.class, ActorUtils.getActorRefName(K8sStatusServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(startActor, k8sServiceRoleOperateCommand, timeout);

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
