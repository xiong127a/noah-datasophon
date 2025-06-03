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
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.datasophon.common.Constants.DATASOPHON;
import static com.datasophon.common.Constants.DEPLOYMENT;
import static com.datasophon.common.Constants.K8S_CLUSTERIP_MAPPING;
import static com.datasophon.common.Constants.K8S_CLUSTER_IP;
import static com.datasophon.common.Constants.K8S_NODEPORT_MAPPING;
import static com.datasophon.common.Constants.K8S_NODE_PORT;
import static com.datasophon.common.Constants.K8S_SVC_CONF;
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
                logger.info("Field path does not exist: {}", fieldPath);
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

    // 生成服务配置的核心方法
    private ArrayList<ServicePort> generateSvcConfig(Map<Generators, List<ServiceConfig>> configFileMap) {
        // 防御性校验
        if (configFileMap == null || configFileMap.isEmpty()) {
            logger.warn("Empty configFileMap received");
            return new ArrayList<>();
        }

        // 获取指定配置生成器
        Generators svcGenerator = configFileMap.keySet().stream()
                .filter(g -> g != null && K8S_SVC_CONF.equals(g.getFilename()))
                .findFirst()
                .orElseGet(() -> {
                    logger.warn("No {} configuration generator found", K8S_SVC_CONF);
                    return null;
                });

        if (svcGenerator == null) {
            return null;
        }

        // 获取关联配置项
        List<ServiceConfig> svcConfigs = configFileMap.get(svcGenerator);
        if (svcConfigs == null || svcConfigs.isEmpty()) {
            logger.warn("No configurations found under {}", K8S_SVC_CONF);
            return null;
        }

        ArrayList<ServicePort> servicePorts = new ArrayList<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, String>>>() {
        }.getType();

        // 处理NodePort端口映射
        ServiceConfig nodePortMappingConfig = svcConfigs.stream()
                .filter(config -> K8S_NODEPORT_MAPPING.equals(config.getName()))
                .findFirst()
                .orElse(null);

        if (nodePortMappingConfig != null && nodePortMappingConfig.getValue() != null) {
            try {
                List<Map<String, String>> portMappings = gson.fromJson(
                        nodePortMappingConfig.getValue().toString(),
                        listType);

                int index = 0;
                for (Map<String, String> mapping : portMappings) {
                    for (Map.Entry<String, String> entry : mapping.entrySet()) {
                        ServicePort servicePort = new ServicePort();
                        int port = Integer.parseInt(entry.getKey());
                        int nodePort = Integer.parseInt(entry.getValue());

                        servicePort.setPort(port);
                        servicePort.setTargetPort(new IntOrString(port));
                        servicePort.setName("nodeport-" + index++);
                        servicePort.setNodePort(nodePort);

                        servicePorts.add(servicePort);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to parse node_port_mappings: {}", e.getMessage());
            }
        }

        // 处理ClusterIP端口映射
        ServiceConfig clusterPortMappingConfig = svcConfigs.stream()
                .filter(config -> K8S_CLUSTERIP_MAPPING.equals(config.getName()))
                .findFirst()
                .orElse(null);

        if (clusterPortMappingConfig != null && clusterPortMappingConfig.getValue() != null) {
            try {
                List<Map<String, String>> portMappings = gson.fromJson(
                        clusterPortMappingConfig.getValue().toString(),
                        listType);

                int index = 0;
                for (Map<String, String> mapping : portMappings) {
                    for (Map.Entry<String, String> entry : mapping.entrySet()) {
                        ServicePort servicePort = new ServicePort();
                        int port = Integer.parseInt(entry.getKey());

                        servicePort.setPort(port);
                        servicePort.setTargetPort(new IntOrString(port));
                        servicePort.setName("clusterport-" + index++);

                        servicePorts.add(servicePort);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to parse cluster_port_mappings: {}", e.getMessage());
            }
        }

        return servicePorts;
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

        List<ServicePort> basePorts = new ArrayList<>(); // 基础服务端口（Headless/ClusterIP）
        List<ServicePort> nodePorts = new ArrayList<>(); // NodePort服务端口

        for (ServicePort originalPort : servicePorts) {
            // 创建基础服务端口副本
            ServicePort basePort = cloneServicePort(originalPort);
            basePort.setNodePort(null); // 基础服务不使用NodePort

            // 根据工作负载类型添加到对应集合
            if (STATEFULSET.equals(kind)) {
                basePorts.add(basePort);
            } else if (DEPLOYMENT.equals(kind)) {
                basePorts.add(basePort);
            }

            // 保留原始NodePort配置
            if (originalPort.getNodePort() != null) {
                nodePorts.add(originalPort);
            }
        }

        // 创建基础服务
        if (!basePorts.isEmpty()) {
            if (STATEFULSET.equals(kind)) {
                createHeadlessService(basePorts, client);
            } else if (DEPLOYMENT.equals(kind)) {
                createClusterIPService(basePorts, client);
            }
        }

        // 创建独立NodePort服务
        if (!nodePorts.isEmpty()) {
            createNodePortServices(nodePorts, client);
        }
    }

    // 深拷贝ServicePort对象
    private ServicePort cloneServicePort(ServicePort original) {
        ServicePort copy = new ServicePort();
        copy.setName(original.getName());
        copy.setPort(original.getPort());
        copy.setTargetPort(original.getTargetPort());
        copy.setNodePort(original.getNodePort());
        return copy;
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
            // 确保NodePort在有效范围（30000-32767）
            if (port.getNodePort() != null) {
                if (port.getNodePort() < 30000 || port.getNodePort() > 32767) {
                    logger.warn("Invalid NodePort {} for {}, using random port",
                            port.getNodePort(), serviceRoleFullName);
                    port.setNodePort(null);
                }
            }
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

    private void handleDeployment(KubernetesClient client, Map<String, Object> yamlData, InputStream yamlInputStream,
            Map<Generators, List<ServiceConfig>> configFileMap) throws Exception {
        handleResource(client, yamlData, yamlInputStream, client.apps().deployments()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withName(serviceRoleFullName), DEPLOYMENT, configFileMap);

    }

    private void handleStatefulSet(KubernetesClient client, Map<String, Object> yamlData, InputStream yamlInputStream,
            Map<Generators, List<ServiceConfig>> configFileMap) throws Exception {
        handleResource(client, yamlData, yamlInputStream, client.apps().statefulSets()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withName(serviceRoleFullName), STATEFULSET, configFileMap);
    }

    private <T extends HasMetadata> void handleResource(KubernetesClient client, Map<String, Object> yamlData,
            InputStream yamlInputStream,
            RollableScalableResource<T> resource, String resourceKind,
            Map<Generators, List<ServiceConfig>> configFileMap) throws Exception {
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

    private void handleExistingDeployment(Map<String, Object> yamlData, KubernetesClient client,
            Deployment existingDeployment) throws IOException {
        Integer replicas = existingDeployment.getSpec().getReplicas() != null
                ? existingDeployment.getSpec().getReplicas()
                : 0;
        logger.info("当前 Deployment: {} Replicas: {}", serviceRoleFullName, replicas);

        updateField(yamlData, "spec.replicas", replicas + 1);

        try (InputStream updatedYamlInputStream = new ByteArrayInputStream(new Yaml().dump(yamlData).getBytes())) {
            // 使用 client.load 加载资源并更新
            client.load(updatedYamlInputStream)
                    .inNamespace(Constant.K8S_NAMESPACE)
                    .createOrReplace();
        }
    }

    private void handleExistingStatefulSet(Map<String, Object> yamlData, KubernetesClient client,
            StatefulSet existingStatefulSet) throws IOException {
        Integer replicas = existingStatefulSet.getSpec().getReplicas() != null
                ? existingStatefulSet.getSpec().getReplicas()
                : 0;
        logger.info("当前 StatefulSet: {} Replicas: {}", serviceRoleFullName, replicas);

        updateField(yamlData, "spec.replicas", replicas + 1);

        try (InputStream updatedYamlInputStream = new ByteArrayInputStream(new Yaml().dump(yamlData).getBytes())) {
            // 使用 client.load 加载资源并更新
            client.load(updatedYamlInputStream)
                    .inNamespace(Constant.K8S_NAMESPACE)
                    .createOrReplace();
        }
    }

    private <T extends HasMetadata> void handleNewResource(KubernetesClient client, InputStream yamlInputStream,
            RollableScalableResource<T> resource) {

        logger.info("CURRENT_NODE_CNT置空: {}", serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT);
        CacheUtils.removeKey(serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT);
        List<HasMetadata> metadata = client.load(yamlInputStream).inNamespace(Constant.K8S_NAMESPACE).create();
        String resourceName = metadata.get(0).getMetadata().getName();
        logger.info("在k8s上启动资源: {} ,使用本地资源文件: {}", resourceName, CommonUtil.k8sYamlFilePath(serviceRoleFullName));

        resource.waitUntilReady(20, TimeUnit.MINUTES);

        // 获取Pod列表（新增代码）
        List<Pod> pods = client.pods()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withLabel("app", serviceRoleFullName) // 与Service/Deployment共享的标签
                .list()
                .getItems();

        // 提取Pod名称
        List<String> podNames = pods.stream()
                .map(pod -> pod.getMetadata().getName())
                .collect(Collectors.toList());

        logger.info("已启动的Pod列表: {}", podNames);

        CacheUtils.put(serviceRoleFullName + "_" + Constant.POD_NAME, podNames);

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
