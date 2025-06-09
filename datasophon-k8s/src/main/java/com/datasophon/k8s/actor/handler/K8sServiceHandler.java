package com.datasophon.k8s.actor.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import org.apache.commons.lang.math.IntRange;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * 生成服务配置的核心方法
     * 
     * @param configFileMap 配置文件映射
     * @return 服务端口列表
     */
    private ArrayList<ServicePort> generateSvcConfig(Map<Generators, List<ServiceConfig>> configFileMap) {
        // 初始化返回列表
        ArrayList<ServicePort> servicePorts = new ArrayList<>();

        // 1. 获取配置项列表
        List<ServiceConfig> svcConfigs = getSvcConfigs(configFileMap);
        if (svcConfigs == null) {
            return servicePorts; // 返回空列表
        }

        // 2. 处理NodePort端口映射
        processPortMappings(
                svcConfigs,
                serviceRoleName.toLowerCase() + "_" + K8S_NODEPORT_MAPPING,
                servicePorts,
                true // 是否为NodePort类型
        );

        // 3. 处理ClusterIP端口映射
        processPortMappings(
                svcConfigs,
                serviceRoleName.toLowerCase() + "_" + K8S_CLUSTERIP_MAPPING,
                servicePorts,
                false // 不是NodePort类型
        );

        return servicePorts;
    }

    /**
     * 获取服务配置项列表
     * 
     * @param configFileMap 配置文件映射
     * @return 配置项列表，如果未找到则返回null
     */
    private List<ServiceConfig> getSvcConfigs(Map<Generators, List<ServiceConfig>> configFileMap) {
        // 防御性校验
        if (configFileMap == null || configFileMap.isEmpty()) {
            logger.warn("Empty configFileMap received");
            return null;
        }

        // 查找服务配置生成器
        Generators svcGenerator = null;
        for (Generators generator : configFileMap.keySet()) {
            if (generator != null && K8S_SVC_CONF.equals(generator.getFilename())) {
                svcGenerator = generator;
                break;
            }
        }

        if (svcGenerator == null) {
            logger.warn("No {} configuration generator found", K8S_SVC_CONF);
            return null;
        }

        // 获取配置项列表
        List<ServiceConfig> svcConfigs = configFileMap.get(svcGenerator);
        if (CollUtil.isEmpty(svcConfigs)) {
            logger.warn("No configurations found under {}", K8S_SVC_CONF);
            return null;
        }

        return svcConfigs;
    }

    /**
     * 处理端口映射配置
     * 
     * @param svcConfigs   配置项列表
     * @param configName   配置项名称
     * @param servicePorts 结果集，解析的端口将添加到此列表
     * @param isNodePort   是否为NodePort类型
     */
    private void processPortMappings(
            List<ServiceConfig> svcConfigs,
            String configName,
            List<ServicePort> servicePorts,
            boolean isNodePort) {

        // 定义有效端口范围
        final IntRange VALID_PORT_RANGE = new IntRange(1, 65535);
        final IntRange VALID_NODEPORT_RANGE = new IntRange(30000, 32767);

        // 1. 查找指定配置
        ServiceConfig mappingConfig = findServiceConfig(svcConfigs, configName);
        if (mappingConfig == null || mappingConfig.getValue() == null) {
            return;
        }

        // 2. 解析端口映射
        List<Map<String, String>> portMappings = parsePortMappings(mappingConfig);
        if (portMappings == null) {
            return;
        }

        // 3. 创建ServicePort对象
        int index = 0;
        String portType = isNodePort ? "nodeport" : "clusterport";

        for (Map<String, String> mapping : portMappings) {
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                try {
                    // 解析端口值
                    int port = Integer.parseInt(entry.getKey());

                    // 验证端口范围
                    if (!VALID_PORT_RANGE.containsInteger(port)) {
                        logger.warn("Invalid port {} in configuration {}, skipping",
                                port, configName);
                        continue;
                    }

                    // 创建端口对象
                    ServicePort servicePort = new ServicePort();
                    servicePort.setPort(port);
                    servicePort.setTargetPort(new IntOrString(port));
                    servicePort.setName(portType + "-" + index++);

                    // 对于NodePort类型，设置NodePort值
                    if (isNodePort) {
                        int nodePort = Integer.parseInt(entry.getValue());

                        // 验证NodePort范围
                        if (!VALID_NODEPORT_RANGE.containsInteger(nodePort)) {
                            logger.warn("Invalid NodePort {} in configuration {}, using random port",
                                    nodePort, configName);
                        } else {
                            servicePort.setNodePort(nodePort);
                        }
                    }

                    // 添加到结果列表
                    servicePorts.add(servicePort);
                } catch (NumberFormatException e) {
                    logger.error("Failed to parse port mapping [{}:{}]: {}",
                            entry.getKey(), entry.getValue(), e.getMessage());
                }
            }
        }
    }

    /**
     * 在配置列表中查找指定名称的配置
     * 
     * @param configs    配置列表
     * @param configName 要查找的配置名
     * @return 找到的配置，未找到时返回null
     */
    private ServiceConfig findServiceConfig(List<ServiceConfig> configs, String configName) {
        for (ServiceConfig config : configs) {
            if (configName.equals(config.getName())) {
                return config;
            }
        }
        return null;
    }

    /**
     * 解析端口映射配置
     * 
     * @param config 包含端口映射的配置项
     * @return 解析后的端口映射列表，解析失败时返回null
     */
    private List<Map<String, String>> parsePortMappings(ServiceConfig config) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, String>>>() {
        }.getType();

        try {
            return gson.fromJson(config.getValue().toString(), listType);
        } catch (Exception e) {
            logger.error("Failed to parse port mappings for {}: {}",
                    config.getName(), e.getMessage());
            return null;
        }
    }

    private void handleNewSvc(ArrayList<ServicePort> servicePorts,
            String kind,
            KubernetesClient client) {
        if (servicePorts == null || servicePorts.isEmpty()) {
            return;
        }

        List<ServicePort> basePorts = new ArrayList<>(); // 基础服务端口（Headless/ClusterIP）
        List<ServicePort> nodePorts = new ArrayList<>(); // NodePort服务端口

        // 使用Range来表示有效的端口范围

        for (ServicePort originalPort : servicePorts) {
            // 创建基础服务端口副本
            ServicePort basePort = ObjectUtil.cloneByStream(originalPort);
            basePort.setNodePort(null); // 基础服务不使用NodePort

            // 不管是StatefulSet还是Deployment，都添加到基础端口集合
            basePorts.add(basePort);

            // 保留原始NodePort配置
            if (originalPort.getNodePort() != null) {
                nodePorts.add(originalPort);
            }
        }

        // 创建基础服务
        if (STATEFULSET.equals(kind)) {
            createHeadlessService(basePorts, client);
        } else {
            createClusterIPService(basePorts, client);
        }

        // 创建独立NodePort服务
        if (!nodePorts.isEmpty()) {
            createNodePortServices(nodePorts, client);
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

        // 保存YAML文件到本地
        saveServiceYaml(service, "headless");

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

        // 保存YAML文件到本地
        saveServiceYaml(service, "clusterip");

        executeServiceCreation(client, service);
    }

    // 创建 NodePort Service（通用）
    private void createNodePortServices(List<ServicePort> ports, KubernetesClient client) {
        // 定义NodePort的有效范围常量
        final int MIN_NODEPORT = 30000;
        final int MAX_NODEPORT = 32767;

        // 创建NodePort有效范围对象
        final IntRange VALID_NODEPORT_RANGE = new IntRange(MIN_NODEPORT, MAX_NODEPORT);

        for (ServicePort port : ports) {
            // 确保NodePort在有效范围（30000-32767）
            if (port.getNodePort() != null) {
                Integer nodePort = port.getNodePort();
                // 使用Range检查端口是否在有效范围内
                if (!VALID_NODEPORT_RANGE.containsInteger(nodePort)) {
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

            // 保存YAML文件到本地
            saveServiceYaml(service, "nodeport");

            executeServiceCreation(client, service);
        }
    }

    // 统一执行服务操作
    private void executeServiceCreation(KubernetesClient client, Service service) {
        try {
            client.services().inNamespace(DATASOPHON).createOrReplace(service);
            logger.info("Service created/updated: {}", service.getMetadata().getName());
        } catch (KubernetesClientException e) {
            logger.error("Error creating service {}: {}", service.getMetadata().getName(), e.getMessage());
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
        int replicas = existingDeployment.getSpec().getReplicas() != null
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
        int replicas = existingStatefulSet.getSpec().getReplicas() != null
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

        // resource.waitUntilReady(10, TimeUnit.MINUTES);

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

    // 保存Service的YAML配置到本地文件
    private void saveServiceYaml(Service service, String serviceType) {
        try {
            // 创建保存目录，使用Paths.get正确处理路径拼接
            Path dirPath = Paths.get(Constants.INSTALL_PATH, "k8sDep", "servers");
            File dir = dirPath.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名，使用Paths.get拼接路径
            Path filePath = Paths.get(dirPath.toString(),
                    service.getMetadata().getName() + "-" + serviceType + ".yaml");

            // 使用K8s客户端序列化为YAML
            String yamlContent = KubeUtil.getKubernetesYaml(service);

            // 写入文件
            Files.write(filePath, yamlContent.getBytes());

            logger.info("保存Service YAML文件成功: {}", filePath);
        } catch (Exception e) {
            logger.error("保存Service YAML文件失败: {}", e.getMessage(), e);
        }
    }

}
