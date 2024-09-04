package com.datasophon.k8s.actor.handler;

import com.datasophon.common.enums.K8sScaleType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@Slf4j
public class K8sScaleServiceHandler {

    private String serviceName;

    private String serviceRoleName;

    private String serviceRoleFullName;

    private Logger logger;

    public K8sScaleServiceHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public ExecResult scaleService(String kubeConfig, K8sScaleType scaleType) {
        ExecResult execResult = new ExecResult();
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            switch (scaleType) {
                case SCALE_UP:
                    scaleUp(client);
                    break;
                case SCALE_DOWN:
                    scaleDown(client);
                    break;
                default:
                    break;
            }
            execResult.setExecResult(true);
        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("{} {} error!", serviceRoleName, scaleType.name(), e);
        }
        return execResult;
    }

    private void scaleUp(KubernetesClient client) {
        RollableScalableResource<Deployment> resource =
                client.apps().deployments().inNamespace(Constant.K8S_NAMESPACE).withName(serviceRoleFullName);
        Integer replicas = resource.get().getStatus().getReplicas();
        if (replicas == null) {
            replicas = 0;
        }
        log.info("当前deployment: {} Replicas: {}", serviceRoleFullName, replicas);
        int scaleNum = replicas + 1;
        resource.scale(scaleNum);
        log.info("scale up deployment 为: " + scaleNum);
    }

    private void scaleDown(KubernetesClient client) {
        RollableScalableResource<Deployment> resource =
                client.apps().deployments().inNamespace(Constant.K8S_NAMESPACE).withName(serviceRoleFullName);

        if (resource == null || resource.get() == null) {
            log.warn("Deployment {} 在命名空间 {} 中不存在，无法缩容", serviceRoleFullName, Constant.K8S_NAMESPACE);
            return;
        }

        Integer replicas = resource.get().getStatus().getReplicas();
        log.info("当前 deployment: {} Replicas: {}", serviceRoleFullName, replicas);

        if (replicas > 0) {
            int count = replicas - 1;
            log.info("缩容 deployment 为: {}", count);
            resource.scale(count);
        }
    }


}