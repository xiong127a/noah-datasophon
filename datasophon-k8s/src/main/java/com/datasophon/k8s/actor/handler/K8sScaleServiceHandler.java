package com.datasophon.k8s.actor.handler;

import com.datasophon.common.enums.K8sScaleType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

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

    private synchronized void scaleUp(KubernetesClient client) throws IOException {
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);

        // 获取当前Deployment
        RollableScalableResource<Deployment> resource = client.apps()
                .deployments()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withName(serviceRoleFullName);

        Deployment existingDeployment = resource.get();
        if (existingDeployment == null) {
            log.error("Deployment {} 不存在", serviceRoleFullName);
            return;
        }

        Integer replicas = existingDeployment.getSpec().getReplicas();
        if (replicas == null) {
            replicas = 0;
        }
        log.info("当前deployment: {} Replicas: {}", serviceRoleFullName, replicas);

        // 加载和更新 YAML 文件
        Yaml yaml = new Yaml();
        Map<String, Object> yamlData;

        try (InputStream yamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {
            yamlData = yaml.load(yamlInputStream);
        }

        // 更新 replicas 字段
        updateField(yamlData, "spec.replicas", replicas + 1);

        // 将更新后的 YAML 应用到 Kubernetes
        try (InputStream updatedYamlInputStream = new ByteArrayInputStream(yaml.dump(yamlData).getBytes())) {
            client.load(updatedYamlInputStream).createOrReplace();
        }
        log.info("scale up deployment 为: {}", replicas+1);
    }

    private synchronized void scaleDown(KubernetesClient client) {
        RollableScalableResource<Deployment> resource =
                client.apps().deployments().inNamespace(Constant.K8S_NAMESPACE).withName(serviceRoleFullName);

        if (resource == null || resource.get() == null) {
            log.warn("Deployment {} 在命名空间 {} 中不存在，无法缩容", serviceRoleFullName, Constant.K8S_NAMESPACE);
            return;
        }

        Integer replicas = resource.get().getSpec().getReplicas();
        log.info("当前 deployment: {} Spec Replicas: {}", serviceRoleFullName, replicas);

        if (replicas != null && replicas > 0) {
            int count = replicas - 1;
            log.info("缩容 deployment 为: {}", count);
            int maxRetries = 3;
            int retries = 0;
            boolean updated = false;

            while (!updated && retries < maxRetries) {
                try {
                    resource.scale(count);
                    updated = true; // 更新成功
                } catch (KubernetesClientException e) {
                    if (e.getCode() == 409) { // 处理冲突
                        retries++;
                        if (retries >= maxRetries) {
                            throw e; // 达到最大重试次数，抛出异常
                        }
                    } else {
                        throw e; // 抛出其他异常
                    }
                }
            }

        }
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


}