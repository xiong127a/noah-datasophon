package com.datasophon.api.kubernetes.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.KubernetesServiceScaleCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.kubernetes.actor.KubernetesScaleServiceActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class KubernetesServiceScaleHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceScaleHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) {
        KubernetesServiceScaleCommand kubernetesServiceScaleCommand = new KubernetesServiceScaleCommand();
        kubernetesServiceScaleCommand.setServiceName(serviceRoleInfo.getParentName());
        kubernetesServiceScaleCommand.setServiceRoleName(serviceRoleInfo.getName());
        String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
        kubernetesServiceScaleCommand.setNamespace(namespace);

        ClusterInfoService clusterInfoService =
                SpringUtil.getBean(ClusterInfoService.class);
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(serviceRoleInfo.getClusterId());
        kubernetesServiceScaleCommand.setKubeConfig(kubeConfig);
        kubernetesServiceScaleCommand.setCommandType(serviceRoleInfo.getCommandType());
        
        // 直接调用KubernetesScaleServiceHandler处理，无需通过Actor
        try {
            logger.info("start scale service role {}", kubernetesServiceScaleCommand.getServiceRoleName());
            
            com.datasophon.kubernetes.actor.handler.KubernetesScaleServiceHandler serviceHandler = 
                    new com.datasophon.kubernetes.actor.handler.KubernetesScaleServiceHandler(
                            kubernetesServiceScaleCommand.getServiceName(), 
                            kubernetesServiceScaleCommand.getServiceRoleName());
            
            ExecResult startResult = serviceHandler.scaleService(
                    kubernetesServiceScaleCommand.getNamespace(),
                    kubernetesServiceScaleCommand.getKubeConfig(),
                    kubernetesServiceScaleCommand.getCommandType());
            
            logger.info("{} scale: {}", kubernetesServiceScaleCommand.getServiceRoleName(),
                    startResult.getExecResult() ? "success" : "failed");
            
            if (Objects.nonNull(startResult) && startResult.getExecResult()) {
                // 角色启动成功
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(serviceRoleInfo);
                }
            }
            return startResult;
        } catch (Exception e) {
            logger.error("扩缩服务失败", e);
            return new ExecResult();
        }
    }
}
