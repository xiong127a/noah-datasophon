package com.datasophon.k8s.actor.handler;

import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.datasophon.common.Constants.*;

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
    public void updateField(Map<String, Object> yamlData, String fieldPath, Object newValue) {
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
                logger.info("Field path does not exist: " + fieldPath);
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
        Map<Generators, List<ServiceConfig>> configFileMap = command.getConfigFileMap();
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(command.getKubeConfig());
             InputStream yamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {

            Map<String, Object> yamlData = loadYamlData(yamlFile);
            String kind = (String) yamlData.get("kind");
            logger.info("kind: {}", kind);


            if (DEPLOYMENT.equals(kind)) {
                handleDeployment(client, yamlData, yamlInputStream, configFileMap);
            } else if (STATEFULSET.equals(kind)) {
                handleStatefulSet(client, yamlData, yamlInputStream, configFileMap);
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

    private ArrayList<ServicePort> generateSvcConfig(Map<Generators, List<ServiceConfig>> configFileMap) {
        // 仅保留文件名为 K8S_SVC_CONF 的配置
        configFileMap.entrySet().removeIf(entry -> !entry.getKey().getFilename().equals(K8S_SVC_CONF));

        // 如果配置映射中没有配置或有多个配置，返回 null
        if (configFileMap.size() != 1) {
            return null;
        }

        // 获取唯一的配置
        List<ServiceConfig> serviceConfigs = configFileMap.values().iterator().next();

        // 获取 K8S_CLUSTER_IP 和 K8S_NODE_PORT 的配置并解析
        Map<Integer, Integer> clusterIpMappings = getTargetValues(serviceConfigs, K8S_CLUSTER_IP);
        Map<Integer, Integer> nodePortMappings = getTargetValues(serviceConfigs, K8S_NODE_PORT);
        if (clusterIpMappings.size() == 0) {
            logger.warn("ClusterIpMappings is empty. No ClusterIp configuration found in svcConfig. Please check the configuration.");
            return null;
        }
        ArrayList<ServicePort> ServicePorts = new ArrayList<>();
        int i = 0;
        // 遍历所有 clusterIp 并生成相应的 ServicePort
        for (Integer clusterIp : clusterIpMappings.keySet()) {
            Integer targetPort = clusterIpMappings.get(clusterIp);

            ServicePort ServicePort = new ServicePort();
            ServicePort.setPort(clusterIp);
            ServicePort.setTargetPort(new IntOrString(targetPort));
            if (!ObjectUtils.isEmpty(nodePortMappings)) {
                Integer nodePort = nodePortMappings.get(targetPort);
                ServicePort.setNodePort(nodePort);
            }
            ServicePort.setName("port-" + i++);
            ServicePorts.add(ServicePort);
        }

        return ServicePorts;
    }

    // 通用方法：获取并解析目标配置
    public Map<Integer, Integer> getTargetValues(List<ServiceConfig> serviceConfigs, String targetKey) {
        // 过滤出指定键的服务配置
        Map<String, Object> targetConfig = serviceConfigs.stream()
                .filter(config -> targetKey.equals(config.getName()))
                .collect(Collectors.toMap(ServiceConfig::getName, ServiceConfig::getValue, (v1, v2) -> v1));

        // 解析目标配置的值为一个 Integer -> Integer 映射
        return parseTargetValues(targetConfig, targetKey);
    }

    // 解析目标配置中的值
    private Map<Integer, Integer> parseTargetValues(Map<String, Object> targetConfig, String targetKey) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, String>>>() {
        }.getType();

        // 解析目标键的 JSON 数据
        List<Map<String, String>> parsedList = gson.fromJson(targetConfig.get(targetKey).toString(), listType);

        // 获取所有包含 serviceRoleFullName 的值并转换为 Integer -> Integer 映射
        return parsedList.stream()
                .filter(map -> map.containsKey(serviceRoleFullName))
                .map(map -> map.get(serviceRoleFullName))
                .map(this::parseKeyValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // 解析一个 "key:value" 字符串为 Map.Entry<Integer, Integer>
    private Map.Entry<Integer, Integer> parseKeyValue(String targetValue) {
        String[] parts = targetValue.split(":");
        if (parts.length == 2) {
            Integer key = Integer.parseInt(parts[0]);
            Integer value = Integer.parseInt(parts[1]);
            return new AbstractMap.SimpleEntry<>(key, value);
        } else if (parts.length == 1) {
            Integer keyAndValue = Integer.parseInt(parts[0]);
            return new AbstractMap.SimpleEntry<>(keyAndValue, keyAndValue);
        }
        return null;
    }

    private void handleNewSvc(ArrayList<ServicePort> servicePorts,
                              String kind,
                              KubernetesClient client) {
        if (servicePorts == null || servicePorts.isEmpty()) {
            return;
        }

        // 分离三类端口
        List<ServicePort> headlessPorts = new ArrayList<>();  // StatefulSet 无头服务端口
        List<ServicePort> clusterIPPorts = new ArrayList<>(); // Deployment 的 ClusterIP 端口
        List<ServicePort> nodePortPorts = new ArrayList<>();  // 所有需要 NodePort 的端口

        for (ServicePort port : servicePorts) {
            if (port.getNodePort() != null) {
                nodePortPorts.add(port);
            }
            if (STATEFULSET.equals(kind)) {
                port.setNodePort(null);
                headlessPorts.add(port);
            } else if (DEPLOYMENT.equals(kind)) {
                port.setNodePort(null);
                clusterIPPorts.add(port);
            }

        }

        // 创建 Headless Service（StatefulSet 专用）
        if (STATEFULSET.equals(kind) && !headlessPorts.isEmpty()) {
            createHeadlessService(headlessPorts, client);
        }

        // 创建 ClusterIP Service（Deployment 专用）
        if (DEPLOYMENT.equals(kind) && !clusterIPPorts.isEmpty()) {
            createClusterIPService(clusterIPPorts, client);
        }

        // 创建 NodePort Service（所有工作负载类型）
        if (!nodePortPorts.isEmpty()) {
            createNodePortServices(nodePortPorts, client);
        }
    }

    // 创建 Headless Service（StatefulSet）
    private void createHeadlessService(List<ServicePort> ports, KubernetesClient client) {
        ServiceSpec spec = new ServiceSpecBuilder()
                .withClusterIP("None")
                .withPublishNotReadyAddresses(true)
                .withSelector(Collections.singletonMap("app", serviceRoleFullName))
                .withPorts(ports)
                .withPublishNotReadyAddresses()
                .build();

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceRoleFullName)
                .withLabels(Collections.singletonMap("app", serviceRoleFullName + "-svc"))
                .withNamespace(DATASOPHON)
                .endMetadata()
                .withSpec(spec)
                .build();

        executeServiceCreation(client, service);
    }

    // 创建 ClusterIP Service（Deployment）
    private void createClusterIPService(List<ServicePort> ports, KubernetesClient client) {
        ServiceSpec spec = new ServiceSpecBuilder()
                .withType(K8S_CLUSTER_IP)
                .withSelector(Collections.singletonMap("app", serviceRoleFullName))
                .withPorts(ports)
                .withPublishNotReadyAddresses()
                .build();

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceRoleFullName)
                .withLabels(Collections.singletonMap("app", serviceRoleFullName + "-svc"))
                .withNamespace(DATASOPHON)
                .endMetadata()
                .withSpec(spec)
                .build();

        executeServiceCreation(client, service);
    }

    // 创建 NodePort Service（通用）
    private void createNodePortServices(List<ServicePort> ports, KubernetesClient client) {
        for (ServicePort port : ports) {
            ServiceSpecBuilder specBuilder = new ServiceSpecBuilder()
                    .withType(K8S_NODE_PORT)
                    .withSelector(Collections.singletonMap("app", serviceRoleFullName))
                    .withPorts(port)
                    .withPublishNotReadyAddresses();

            // 动态生成服务名称
            String serviceName = serviceRoleFullName + "-nodeport-" + port.getPort();

            Service service = new ServiceBuilder()
                    .withNewMetadata()
                    .withName(serviceName)
                    .withLabels(Collections.singletonMap("app", serviceRoleFullName + "-svc"))
                    .withNamespace(DATASOPHON)
                    .endMetadata()
                    .withSpec(specBuilder.build())
                    .build();

            executeServiceCreation(client, service);
        }
    }

    // 统一执行服务操作
    private void executeServiceCreation(KubernetesClient client, Service service) {
        try {
            client.services().inNamespace(DATASOPHON).createOrReplace(service);
            logger.info("Service created/updated: " + service.getMetadata().getName());
        } catch (KubernetesClientException e) {
            logger.error("Error creating service " + service.getMetadata().getName() + ": " + e.getMessage());
        }
    }


    private Map<String, Object> loadYamlData(String yamlFile) {
        try (InputStream yamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {
            Yaml yaml = new Yaml();
            return yaml.load(yamlInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDeployment(KubernetesClient client, Map<String, Object> yamlData, InputStream yamlInputStream, Map<Generators, List<ServiceConfig>> configFileMap) throws Exception {
        handleResource(client, yamlData, yamlInputStream, client.apps().deployments()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withName(serviceRoleFullName), DEPLOYMENT, configFileMap);

    }

    private void handleStatefulSet(KubernetesClient client, Map<String, Object> yamlData, InputStream yamlInputStream, Map<Generators, List<ServiceConfig>> configFileMap) throws Exception {
        handleResource(client, yamlData, yamlInputStream, client.apps().statefulSets()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withName(serviceRoleFullName), STATEFULSET, configFileMap);
    }

    private <T extends HasMetadata> void handleResource(KubernetesClient client, Map<String, Object> yamlData, InputStream yamlInputStream,
                                                        RollableScalableResource<T> resource, String resourceKind, Map<Generators, List<ServiceConfig>> configFileMap) throws Exception {
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
                ArrayList<ServicePort> ServicePorts = generateSvcConfig(configFileMap);
                handleNewSvc(ServicePorts, resourceKind, client);
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
                client.services()
                        .inNamespace(Constant.K8S_NAMESPACE)
                        .withLabelSelector("app=" + serviceRoleFullName + "-svc")
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
