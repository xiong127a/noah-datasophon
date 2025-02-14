package com.datasophon.k8s.actor.handler;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.datasophon.common.Constants.DEPLOYMENT;
import static com.datasophon.common.Constants.STATEFULSET;

@Data
public class K8sServiceHandler {

    private static final Long timeout = 300L;
    private String serviceName;
    private String serviceRoleName;
    private String serviceRoleFullName;
    private Logger logger;

    public K8sServiceHandler(String serviceName, String serviceRoleName) {
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

    public ExecResult start(K8sServiceRoleOperateCommand command) {
        if (CommandType.INSTALL_SERVICE.equals(command.getCommandType())) {
            return install(command);
        } else {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            return execResult;
        }
    }

    public ExecResult install(K8sServiceRoleOperateCommand command) {
        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(command.getKubeConfig());
             InputStream yamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {

            Map<String, Object> yamlData = loadYamlData(yamlFile);
            String kind = (String) yamlData.get("kind");
            logger.info("kind: {}", kind);

            if (DEPLOYMENT.equals(kind)) {
                handleDeployment(client, yamlData, yamlInputStream);
            } else if (STATEFULSET.equals(kind)) {
                handleStatefulSet(client, yamlData, yamlInputStream);
            } else {
                throw new IllegalArgumentException("Unsupported resource kind: " + kind);
            }


        } catch (IOException e) {
            handleException(execResult, "文件操作时发生异常", e);
        } catch (KubernetesClientException e) {
            handleException(execResult, "与 Kubernetes 交互时发生异常", e);
        } catch (Exception e) {
            handleException(execResult, "启动资源时发生异常", e);
        }

        return execResult;
    }

    private Map<String, Object> loadYamlData(String yamlFile) {
        try (InputStream yamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {
            Yaml yaml = new Yaml();
            return yaml.load(yamlInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDeployment(KubernetesClient client, Map<String, Object> yamlData, InputStream yamlInputStream) throws Exception {
        handleResource(client, yamlData, yamlInputStream, client.apps().deployments()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withName(serviceRoleFullName), DEPLOYMENT);

    }

    private void handleStatefulSet(KubernetesClient client, Map<String, Object> yamlData, InputStream yamlInputStream) throws Exception {
        handleResource(client, yamlData, yamlInputStream, client.apps().statefulSets()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withName(serviceRoleFullName), STATEFULSET);
    }

    private <T extends HasMetadata> void handleResource(KubernetesClient client, Map<String, Object> yamlData, InputStream yamlInputStream,
                                                        RollableScalableResource<T> resource, String resourceKind) throws Exception {
        T existingResource = resource.get();
        boolean isExistingResource = existingResource != null;

        if (isExistingResource) {
            if (DEPLOYMENT.equals(resourceKind)) {
                handleExistingDeployment(yamlData, client, (Deployment) existingResource);
            } else if (STATEFULSET.equals(resourceKind)) {
                handleExistingStatefulSet(yamlData, client, (StatefulSet) existingResource);
            }
        } else {
            addProcessStatus();
            if (isFinalNode()) {
                handleNewResource(client, yamlInputStream, resource);
            }
        }
    }


    private void handleExistingDeployment(Map<String, Object> yamlData, KubernetesClient client, Deployment existingDeployment) throws IOException {
        Integer replicas = existingDeployment.getSpec().getReplicas() != null ? existingDeployment.getSpec().getReplicas() : 0;
        logger.info("当前 Deployment: {} Replicas: {}", serviceRoleFullName, replicas);

        updateField(yamlData, "spec.replicas", replicas + 1);

        try (InputStream updatedYamlInputStream = new ByteArrayInputStream(new Yaml().dump(yamlData).getBytes())) {
            // 使用 client.load 加载资源并更新
            client.load(updatedYamlInputStream)
                    .inNamespace(Constant.K8S_NAMESPACE)
                    .createOrReplace();
        }
    }

    private void handleExistingStatefulSet(Map<String, Object> yamlData, KubernetesClient client, StatefulSet existingStatefulSet) throws IOException {
        Integer replicas = existingStatefulSet.getSpec().getReplicas() != null ? existingStatefulSet.getSpec().getReplicas() : 0;
        logger.info("当前 StatefulSet: {} Replicas: {}", serviceRoleFullName, replicas);

        updateField(yamlData, "spec.replicas", replicas + 1);

        try (InputStream updatedYamlInputStream = new ByteArrayInputStream(new Yaml().dump(yamlData).getBytes())) {
            // 使用 client.load 加载资源并更新
            client.load(updatedYamlInputStream)
                    .inNamespace(Constant.K8S_NAMESPACE)
                    .createOrReplace();
        }
    }


    private <T extends HasMetadata> void handleNewResource(KubernetesClient client, InputStream yamlInputStream, RollableScalableResource<T> resource) {

            logger.info("CURRENT_NODE_CNT置空: {}", serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT);
            CacheUtils.removeKey(serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT);
            List<HasMetadata> metadata = client.load(yamlInputStream).inNamespace(Constant.K8S_NAMESPACE).create();
            String resourceName = metadata.get(0).getMetadata().getName();
            logger.info("在k8s上启动资源: {} ,使用本地资源文件: {}", resourceName, CommonUtil.k8sYamlFilePath(serviceRoleFullName));

            resource.waitUntilReady(20, TimeUnit.MINUTES);
            logger.info(resource.getLog());
        }



    private void handleException(ExecResult execResult, String message, Exception e) {
        logger.error("{}: {}", message, e.getMessage(), e);
        execResult.setExecErrOut(message + ": " + e.getMessage());
        execResult.setExecResult(false);
    }


    public ExecResult stop(String kubeConfig) {
        ExecResult execResult = new ExecResult();
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
        logger.info("本地资源文件: {}", yamlFile);

        File yamlFileObj = new File(yamlFile);

        if (!yamlFileObj.exists()) {
            logger.error("k8s资源文件不存在: {}", yamlFile);
            execResult.setExecErrOut("k8s资源文件不存在: " + yamlFile);
            execResult.setExecOut("k8s资源文件不存在: " + yamlFile);
            execResult.setExecResult(false);
            return execResult;
        } else {
            logger.info("在k8s上停止deployment ,使用本地资源文件: {}", yamlFile);
            try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig);
                 FileInputStream fis = new FileInputStream(yamlFileObj)) {
                client.load(fis)
                        .inNamespace(Constant.K8S_NAMESPACE)
                        .delete();
                execResult.setExecResult(true);
                CacheUtils.removeKey(serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT);
            } catch (Exception e) {
                logger.error("停止deployment时发生异常: {}", e.getMessage(), e);
                execResult.setExecErrOut("停止deployment时发生异常: " + e.getMessage());
                execResult.setExecOut("停止deployment时发生异常: " + e.getMessage());
            }
        }
        return execResult;
    }


    private void addProcessStatus() {
        Integer nodeCount = (Integer) CacheUtils.get(serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT);
        if (Objects.isNull(nodeCount)) {
            CacheUtils.put(serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT, 1);
        } else {
            CacheUtils.put(serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT, nodeCount + 1);
        }
    }

    private Boolean isFinalNode() {
        Integer nodeCount = (Integer) CacheUtils.get(serviceRoleFullName + "_" + Constant.ROLE_NODE_CNT);
        Integer currentCount = (Integer) CacheUtils.get(serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT);
        logger.info("当前{}: {}个，所需{}: {}个", serviceRoleFullName, currentCount, serviceRoleFullName, nodeCount);
        return currentCount.equals(nodeCount);
    }

}
