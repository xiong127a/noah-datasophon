package com.datasophon.api.master.alert;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ServiceStateManagementService;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
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
 * Kubernetes服务角色状态检查和告警处理服务
 * 负责检查Kubernetes环境下的服务角色状态并处理相关告警
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-05
 */
public class KubernetesServiceRoleStatusService {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceRoleStatusService.class);

    public void checkStatusAndOpAlert(ClusterServiceRoleInstanceDTO roleInstanceDto) {

        //logger.info("start to check service status {} in {}", roleInstanceDto.serviceRoleName(), roleInstanceDto.hostname());

        //准备调用参数
        KubernetesServiceRoleOperateCommand kubernetesServiceRoleOperateCommand = new KubernetesServiceRoleOperateCommand();
        kubernetesServiceRoleOperateCommand.setClusterId(roleInstanceDto.clusterId());
        kubernetesServiceRoleOperateCommand.setServiceName(roleInstanceDto.serviceName());
        kubernetesServiceRoleOperateCommand.setServiceRoleName(roleInstanceDto.serviceRoleName());
        kubernetesServiceRoleOperateCommand.setHostname(roleInstanceDto.hostname());
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(roleInstanceDto.clusterId());
        kubernetesServiceRoleOperateCommand.setKubeConfig(kubeConfig);
        String namespace = ClusterInfoUtils.getKubernetesNamespace(roleInstanceDto.clusterId());
        kubernetesServiceRoleOperateCommand.setNamespace(namespace);

        //调用查询状态
        ActorRef startActor = ActorUtils.getLocalActor(KubernetesStatusServiceActor.class, ActorUtils.getActorRefName(KubernetesStatusServiceActor.class));
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> startFuture = Patterns.ask(startActor, kubernetesServiceRoleOperateCommand, timeout);

        try {
            ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);

            //处理状态告警
            ExecResult execResult = (ExecResult) Await.result(startFuture, timeout.duration());
            if (execResult.getExecResult()) {
                //状态正常   恢复alert
                serviceStateManagementService.recoverAlert(roleInstanceDto);
            } else {
                //保存alert
                String alertTargetName = roleInstanceDto.serviceRoleName() + " Survive";
                serviceStateManagementService.saveAlert(roleInstanceDto, alertTargetName, AlertLevel.EXCEPTION, "restart");
            }

        } catch (Exception e) {
            ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
            // save alert
            String alertTargetName = roleInstanceDto.serviceRoleName() + " Survive";
            serviceStateManagementService.saveAlert(roleInstanceDto, alertTargetName, AlertLevel.EXCEPTION, "restart");
        }


    }
}
