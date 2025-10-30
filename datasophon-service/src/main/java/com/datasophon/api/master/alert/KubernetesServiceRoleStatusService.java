package com.datasophon.api.master.alert;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ServiceStateManagementService;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.AlertLevel;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.kubernetes.actor.handler.KubernetesStatusHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

        logger.info("start to check service status {} in {}", roleInstanceDto.serviceRoleName(), roleInstanceDto.hostname());

        //准备调用参数
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(roleInstanceDto.clusterId());
        String namespace = ClusterInfoUtils.getKubernetesNamespace(roleInstanceDto.clusterId());

        try {
            // 直接调用KubernetesStatusHandler检查状态
            KubernetesStatusHandler statusHandler = new KubernetesStatusHandler(
                    roleInstanceDto.serviceName(), 
                    roleInstanceDto.serviceRoleName());
            ExecResult execResult = statusHandler.status(namespace, kubeConfig, roleInstanceDto.hostname());
            
            ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);

            //处理状态告警
            if (execResult.getExecResult()) {
                //状态正常   恢复alert
                serviceStateManagementService.recoverAlert(roleInstanceDto);
                logger.info("Service role {} on {} is healthy", roleInstanceDto.serviceRoleName(), roleInstanceDto.hostname());
            } else {
                //保存alert
                String alertTargetName = roleInstanceDto.serviceRoleName() + " Survive";
                serviceStateManagementService.saveAlert(roleInstanceDto, alertTargetName, AlertLevel.EXCEPTION, "restart");
                logger.warn("Service role {} on {} is unhealthy", roleInstanceDto.serviceRoleName(), roleInstanceDto.hostname());
            }

        } catch (Exception e) {
            logger.error("Failed to check service status {} on {}", roleInstanceDto.serviceRoleName(), roleInstanceDto.hostname(), e);
            ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
            // save alert
            String alertTargetName = roleInstanceDto.serviceRoleName() + " Survive";
            serviceStateManagementService.saveAlert(roleInstanceDto, alertTargetName, AlertLevel.EXCEPTION, "restart");
        }


    }
}
