package com.datasophon.api.kubernetes.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class KubernetesServiceStopHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceStopHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) throws Exception {
        logger.info("start to stop service {} in {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
        KubernetesServiceRoleOperateCommand kubernetesServiceRoleOperateCommand = new KubernetesServiceRoleOperateCommand();
        kubernetesServiceRoleOperateCommand.setServiceName(serviceRoleInfo.getParentName());
        kubernetesServiceRoleOperateCommand.setServiceRoleName(serviceRoleInfo.getName());

        ClusterInfoService clusterInfoService =
                SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setKubeConfig(kubeConfig);
        String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
        kubernetesServiceRoleOperateCommand.setNamespace(namespace);

        if (serviceRoleInfo.getRoleType() == ServiceRoleType.CLIENT) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            if (Objects.nonNull(getNext())) {
                return getNext().handlerRequest(serviceRoleInfo);
            }
            return execResult;
        }

        // 直接调用KubernetesServiceHandler处理，无需通过Actor
        try {
            logger.info("start to stop service role {} on Kubernetes", kubernetesServiceRoleOperateCommand.getServiceRoleName());
            
            com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler serviceHandler = 
                    new com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler(
                            kubernetesServiceRoleOperateCommand.getServiceName(),
                            kubernetesServiceRoleOperateCommand.getServiceRoleName());
            
            ExecResult startResult = serviceHandler.stop(kubernetesServiceRoleOperateCommand);
            
            logger.info("service role {} stop on Kubernetes result: {}", 
                    kubernetesServiceRoleOperateCommand.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
            
            if (Objects.nonNull(startResult) && startResult.getExecResult()) {
                // 角色删除成功
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(serviceRoleInfo);
                }
            }
            return startResult;
        } catch (Exception e) {
            logger.error("停止服务失败", e);
            return new ExecResult();
        }
    }
}
