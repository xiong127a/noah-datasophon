package com.datasophon.k8s.actor.handler;

import com.datasophon.common.enums.K8sScaleType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Data
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

    // 更新指定字段的值
    public static void updateField(Map<String, Object> yamlData, String fieldPath, Object newValue) {
        // 将字段路径按 '.' 分割以支持嵌套字段
        String[] keys = fieldPath.split("\\.");

        // 遍历路径以找到目标字段
        Map<String, Object> currentMap = yamlData;

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];

            // 检查当前地图是否包含该键
            if (currentMap.containsKey(key)) {
                // 获取下一个层级的 Map
                currentMap = (Map<String, Object>) currentMap.get(key);
            } else {
                // 如果路径不存在，直接返回
                System.out.println("Field path does not exist: " + fieldPath);
                return;
            }
        }

        // 设置新值
        currentMap.put(keys[keys.length - 1], newValue);
    }

    public ExecResult scaleService(String kubeConfig, K8sScaleType scaleType) {
        ExecResult execResult = new ExecResult();
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
        if ("hdfs-zkfc".equalsIgnoreCase(serviceRoleFullName)) {
            // ZKFC作为NameNode Pod的Sidecar容器部署
            execResult.setExecResult(true);
            logger.info("ZKFC作为NameNode Pod的Sidecar容器部署，不加载yaml文件");
            return execResult;
        }
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            Map<String, Object> yamlData = loadYamlData(yamlFile);
            String kind = (String) yamlData.get("kind");
            logger.info("Detected resource kind: {}", kind);
            
            // 根据资源类型调用不同的扩缩容逻辑
            if ("Deployment".equals(kind)) {
                logger.info("Scaling Deployment: {}", serviceRoleFullName);
                scaleDeployment(client, scaleType);
            } else if ("StatefulSet".equals(kind)) {
                logger.info("Scaling StatefulSet: {}", serviceRoleFullName);
                scaleStatefulSet(client, scaleType);
            } else {
                String errorMsg = "Unsupported resource kind: " + kind;
                logger.error(errorMsg);
                throw new UnsupportedOperationException(errorMsg);
            }
            execResult.setExecResult(true);
        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("{} {} error! Reason: {}", serviceRoleName, scaleType.name(), e.getMessage(), e);
        }
        return execResult;
    }

    // 抽象Deployment扩缩容逻辑
    private void scaleDeployment(KubernetesClient client, K8sScaleType scaleType) {
        switch (scaleType) {
            case SCALE_UP:
                scaleResourceUp(client, client.apps().deployments(), "Deployment");
                break;
            case SCALE_DOWN:
                scaleResourceDown(client, client.apps().deployments(), "Deployment");
                break;
            default:
                logger.warn("Unsupported scale type: {}", scaleType);
        }
    }

    private void scaleStatefulSet(KubernetesClient client, K8sScaleType scaleType) {
        switch (scaleType) {
            case SCALE_UP:
                scaleResourceUp(client, client.apps().statefulSets(), "StatefulSet");
                break;
            case SCALE_DOWN:
                scaleResourceDown(client, client.apps().statefulSets(), "StatefulSet");
                break;
            default:
                logger.warn("Unsupported scale type: {}", scaleType);
        }
    }

    // 重构扩容方法 - 统一使用scale接口
    private synchronized <T> void scaleResourceUp(
            KubernetesClient client, 
            MixedOperation<T, ?, RollableScalableResource<T>> resourceApi, 
            String resourceType) {
    
    RollableScalableResource<T> resource = resourceApi
        .inNamespace(Constant.K8S_NAMESPACE)
        .withName(serviceRoleFullName);

    T existingResource = resource.get();
    if (existingResource == null) {
        logger.error("{} {} does not exist", resourceType, serviceRoleFullName);
        return;
    }

    // 获取当前副本数
    Integer replicas = getReplicas(existingResource);
    if (replicas == null) replicas = 0;
    logger.info("Current {}: {} Replicas: {}", resourceType, serviceRoleFullName, replicas);

    int newReplicas = replicas + 1;
    logger.info("Scaling up {} to: {}", resourceType, newReplicas);
    
    int maxRetries = 3;
    for (int retry = 0; retry < maxRetries; retry++) {
        try {
            resource.scale(newReplicas);
            logger.info("Successfully scaled up {} to {}", resourceType, newReplicas);
            return;
        } catch (KubernetesClientException e) {
            if (e.getCode() == 409) { // 冲突错误
                logger.warn("Scale conflict detected, retrying... ({}/{})", retry + 1, maxRetries);
                if (retry == maxRetries - 1) throw e;
            } else {
                throw e;
            }
        }
    }
}

    // 通用缩容方法（支持Deployment/StatefulSet）
    private synchronized <T> void scaleResourceDown(
            KubernetesClient client, 
            MixedOperation<T, ?, RollableScalableResource<T>> resourceApi, 
            String resourceType) {
    
    RollableScalableResource<T> resource = resourceApi
        .inNamespace(Constant.K8S_NAMESPACE)
        .withName(serviceRoleFullName);

        T existingResource = resource.get();
        if (existingResource == null ) {
            logger.warn("{} {} does not exist, cannot scale down", resourceType, serviceRoleFullName);
            return;
        }

        // 通过反射获取副本数
        Integer replicas = getReplicas(existingResource);
        logger.info("Current {}: {} Replicas: {}", resourceType, serviceRoleFullName, replicas);

        if (replicas != null && replicas > 0) {
            int newReplicas = replicas - 1;
            logger.info("Scaling down {} to: {}", resourceType, newReplicas);
            
            int maxRetries = 3;
            for (int retry = 0; retry < maxRetries; retry++) {
                try {
                    resource.scale(newReplicas);
                    logger.info("Successfully scaled down {} to {}", resourceType, newReplicas);
                    return;
                } catch (KubernetesClientException e) {
                    if (e.getCode() == 409) { // 冲突错误
                        logger.warn("Scale conflict detected, retrying... ({}/{})", retry + 1, maxRetries);
                        if (retry == maxRetries - 1) throw e;
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    // 通过反射获取副本数（兼容Deployment/StatefulSet）
    private <T> Integer getReplicas(T resource) {
        try {
            if (resource instanceof Deployment) {
                return ((Deployment) resource).getSpec().getReplicas();
            } else if (resource instanceof StatefulSet) {
                return ((StatefulSet) resource).getSpec().getReplicas();
            }
        } catch (Exception e) {
            logger.error("Failed to get replicas: {}", e.getMessage());
        }
        return null;
    }

    private Map<String, Object> loadYamlData(String yamlFile) {
        try (InputStream yamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {
            Yaml yaml = new Yaml();
            return yaml.load(yamlInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}