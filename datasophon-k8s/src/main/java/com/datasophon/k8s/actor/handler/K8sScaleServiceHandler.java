package com.datasophon.k8s.actor.handler;

import com.datasophon.common.enums.CommandType;
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

    // ANSI颜色代码
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_CYAN = "\u001B[36m";

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

    public ExecResult scaleService(String kubeConfig, CommandType commandType) {
        ExecResult execResult = new ExecResult();
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
        if ("hdfs-zkfc".equalsIgnoreCase(serviceRoleFullName)) {
            // ZKFC作为NameNode Pod的Sidecar容器部署
            execResult.setExecResult(true);
            logger.info("ZKFC作为NameNode Pod的Sidecar容器部署，不加载yaml文件");
            return execResult;
        }

        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            // 先判断需要执行的操作类型
            switch (commandType) {
                case INSTALL_SERVICE:
                    // 安装服务时增加副本
                    System.out.println(ANSI_CYAN + "ℹ️ 执行服务安装操作，增加副本数" + ANSI_RESET);
                    handleResourceScaling(client, yamlFile, true);
                    break;
                case UNINSTALL_SERVICE:
                    // 卸载服务时减少副本
                    System.out.println(ANSI_CYAN + "ℹ️ 执行服务卸载操作，减少副本数" + ANSI_RESET);
                    handleResourceScaling(client, yamlFile, false);
                    break;
                case STOP_SERVICE:
                case START_SERVICE:
                case RESTART_SERVICE:
                    // 停止、启动、重启服务时不操作副本数
                    System.out.println(ANSI_CYAN + "ℹ️ 执行" + commandType.name() + "操作，不改变副本数" + ANSI_RESET);
                    logger.info("Operation {} does not affect replica count", commandType);
                    break;
                default:
                    System.out.println(ANSI_YELLOW + "⚠️ 未知的服务操作类型: " + commandType + ANSI_RESET);
                    logger.warn("Unsupported operation type: {}", commandType);
            }
            execResult.setExecResult(true);
        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            System.out.println(ANSI_RED + "❌ " + serviceRoleName + " " + commandType.name() + " 操作失败: "
                    + e.getMessage() + ANSI_RESET);
            logger.error("{} {} error! Reason: {}", serviceRoleName, commandType.name(), e.getMessage(), e);
        }
        return execResult;
    }

    /**
     * 处理资源扩缩容
     * 
     * @param client    Kubernetes客户端
     * @param yamlFile  YAML文件路径
     * @param isScaleUp 是否扩容（true表示扩容，false表示缩容）
     */
    private void handleResourceScaling(KubernetesClient client, String yamlFile, boolean isScaleUp) throws IOException {
        String kind = "Deployment"; // 默认为Deployment

        if (!yamlFile.toLowerCase().contains("operator")) {
            Map<String, Object> yamlData = loadYamlData(yamlFile);
            kind = (String) yamlData.get("kind");
        }

        logger.info("Detected resource kind: {}", kind);

        // 根据资源类型调用不同的扩缩容逻辑
        if ("Deployment".equals(kind)) {
            logger.info("{} Deployment: {}", isScaleUp ? "Scaling up" : "Scaling down", serviceRoleFullName);
            if (isScaleUp) {
                scaleResourceUp(client, client.apps().deployments(), "Deployment");
            } else {
                scaleResourceDown(client, client.apps().deployments(), "Deployment");
            }
        } else if ("StatefulSet".equals(kind)) {
            logger.info("{} StatefulSet: {}", isScaleUp ? "Scaling up" : "Scaling down", serviceRoleFullName);
            if (isScaleUp) {
                scaleResourceUp(client, client.apps().statefulSets(), "StatefulSet");
            } else {
                scaleResourceDown(client, client.apps().statefulSets(), "StatefulSet");
            }
        } else {
            String errorMsg = "Unsupported resource kind: " + kind;
            logger.error(errorMsg);
            throw new UnsupportedOperationException(errorMsg);
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
        if (replicas == null)
            replicas = 0;
        logger.info("Current {}: {} Replicas: {}", resourceType, serviceRoleFullName, replicas);

        int newReplicas = replicas + 1;
        logger.info("Scaling up {} to: {}", resourceType, newReplicas);
        System.out.println(ANSI_BLUE + "🔄 正在将 " + resourceType + " " + serviceRoleFullName + " 副本数从 " + replicas
                + " 增加到 " + newReplicas + ANSI_RESET);

        int maxRetries = 3;
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                resource.scale(newReplicas);
                System.out.println(ANSI_GREEN + "✅ 成功将 " + resourceType + " " + serviceRoleFullName + " 副本数增加到 "
                        + newReplicas + ANSI_RESET);
                logger.info("Successfully scaled up {} to {}", resourceType, newReplicas);
                return;
            } catch (KubernetesClientException e) {
                if (e.getCode() == 409) { // 冲突错误
                    logger.warn("Scale conflict detected, retrying... ({}/{})", retry + 1, maxRetries);
                    if (retry == maxRetries - 1)
                        throw e;
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
        if (existingResource == null) {
            logger.warn("{} {} does not exist, cannot scale down", resourceType, serviceRoleFullName);
            return;
        }

        // 通过反射获取副本数
        Integer replicas = getReplicas(existingResource);
        logger.info("Current {}: {} Replicas: {}", resourceType, serviceRoleFullName, replicas);

        if (replicas != null && replicas > 0) {
            int newReplicas = replicas - 1;
            logger.info("Scaling down {} to: {}", resourceType, newReplicas);
            System.out.println(ANSI_BLUE + "🔄 正在将 " + resourceType + " " + serviceRoleFullName + " 副本数从 " + replicas
                    + " 减少到 " + newReplicas + ANSI_RESET);

            int maxRetries = 3;
            for (int retry = 0; retry < maxRetries; retry++) {
                try {
                    resource.scale(newReplicas);
                    System.out.println(ANSI_GREEN + "✅ 成功将 " + resourceType + " " + serviceRoleFullName + " 副本数减少到 "
                            + newReplicas + ANSI_RESET);
                    logger.info("Successfully scaled down {} to {}", resourceType, newReplicas);
                    return;
                } catch (KubernetesClientException e) {
                    if (e.getCode() == 409) { // 冲突错误
                        logger.warn("Scale conflict detected, retrying... ({}/{})", retry + 1, maxRetries);
                        if (retry == maxRetries - 1)
                            throw e;
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

    private Map<String, Object> loadYamlData(String yamlFile) throws IOException {
        try (InputStream yamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {
            Yaml yaml = new Yaml();
            return yaml.load(yamlInputStream);
        }
    }
}