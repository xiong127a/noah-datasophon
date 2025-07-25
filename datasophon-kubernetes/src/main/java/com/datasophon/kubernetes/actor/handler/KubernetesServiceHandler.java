package com.datasophon.kubernetes.actor.handler;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.util.ColorLogUtils;
import com.datasophon.kubernetes.util.CommonUtil;
import com.datasophon.kubernetes.util.KubeUtil;
import com.datasophon.kubernetes.util.KubernetesFreeMakerUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.datasophon.common.Constants.DEPLOYMENT;
import static com.datasophon.common.Constants.KUBERNETES_CLUSTERIP_MAPPING;
import static com.datasophon.common.Constants.KUBERNETES_CLUSTER_IP;
import static com.datasophon.common.Constants.KUBERNETES_CONFIG_SERVICES;
import static com.datasophon.common.Constants.KUBERNETES_LOADBALANCER_MAPPING;
import static com.datasophon.common.Constants.KUBERNETES_LOAD_BALANCER;
import static com.datasophon.common.Constants.KUBERNETES_NODEPORT_MAPPING;
import static com.datasophon.common.Constants.KUBERNETES_NODE_PORT;
import static com.datasophon.common.Constants.STATEFULSET;
import static com.datasophon.common.utils.HostUtils.GetMasterHost;

@Data
@Slf4j
public class KubernetesServiceHandler {

    private static final Long timeout = 300L;
    private String serviceName;
    private String serviceRoleName;
    private String serviceRoleFullName;
    private Logger logger;

    public KubernetesServiceHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constants.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    // 保存ConfigMap的YAML配置到本地文件
    public static void saveConfigMapYaml(ConfigMap configMap) {
        try {
            // 创建保存目录，使用java.nio.file.Paths处理路径拼接
            Path dirPath = Paths.get(StrUtil.blankToDefault(Constants.YAML_PATH, Constants.INSTALL_PATH),
                    "kubernetesYaml",
                    "configmaps");

            // 确保目录存在
            if (BooleanUtil.isFalse(ensureDirectoryExists(dirPath))) {
                log.error("无法创建ConfigMap YAML保存目录: {}", dirPath);
                return;
            }

            // 生成文件名，使用Paths.get拼接路径
            Path filePath = Paths.get(dirPath.toString(),
                    configMap.getMetadata().getName() + ".yaml");

            // 使用kubernetes客户端序列化为YAML
            String yamlContent = KubeUtil.getKubernetesYaml(configMap);

            // 写入文件
            Files.write(filePath, yamlContent.getBytes());

            log.info("保存ConfigMap YAML文件成功: {}", filePath);
        } catch (Exception e) {
            log.error("保存ConfigMap YAML文件失败: {}", e.getMessage(),
                    e);
        }
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
                // 获取下一个层级的 Map，并进行安全的类型检查
                Object nextLevel = currentMap.get(key);
                if (!(nextLevel instanceof Map)) {
                    logger.warn("字段路径中的元素不是Map类型: {} 在 {}", key, fieldPath);
                    return;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> nextMap = (Map<String, Object>) nextLevel;
                currentMap = nextMap;
            } else {
                // 如果路径不存在，直接返回
                logger.info("Field path does not exist: {}", fieldPath);
                return;
            }
        }

        // 设置新值
        currentMap.put(keys[keys.length - 1], newValue);
    }

    public ExecResult start(KubernetesServiceRoleOperateCommand command) {
        if (CommandType.INSTALL_SERVICE.equals(command.getCommandType())) {
            return install(command);
        } else {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            return execResult;
        }
    }

    public ExecResult install(KubernetesServiceRoleOperateCommand command) {
        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);
        Map<Generators, List<ServiceConfig>> configFileMap = command.getConfigFileMap();
        String yamlFile = CommonUtil.KubernetesYamlFilePath(serviceRoleFullName);
        Path path = Paths.get(yamlFile);
        if (Files.exists(path) && yamlFile.toLowerCase().contains("operator")) {
            String s = KubernetesMinaUtils.execCmdWithResult(GetMasterHost().getFirst(),
                    "kubectl apply -f " + yamlFile);
            execResult.setExecResult(!StrUtil.equals(s, Constants.FAILED));
            logger.info("start operator: {}", s);
            return execResult;
        }

        if (StrUtil.equalsIgnoreCase(serviceRoleFullName, "hdfs-zkfc")) {
            execResult.setExecResult(true);
            logger.info("ZKFC作为NameNode Pod的Sidecar容器部署，不加载kubernetes yaml文件");
            return execResult;
        }
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(command.getKubeConfig());
                InputStream yamlInputStream = Files.newInputStream(path)) {

            Map<String, Object> yamlData = loadYamlData(yamlFile);
            String kind = (String) yamlData.get("kind");
            logger.info("kind: {}", kind);
            String namespace = command.getNamespace();
            if (DEPLOYMENT.equals(kind)) {
                handleDeployment(namespace, client, yamlData, yamlInputStream, configFileMap);
            } else if (STATEFULSET.equals(kind)) {
                handleStatefulSet(namespace, client, yamlData, yamlInputStream, configFileMap);
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

        // 防御性检查
        if (configFileMap == null || configFileMap.isEmpty()) {
            logger.warn("收到空的配置映射");
            return servicePorts;
        }

        // 1. 获取配置项列表
        List<ServiceConfig> svcConfigs = null;
        for (Map.Entry<Generators, List<ServiceConfig>> entry : configFileMap.entrySet()) {
            Generators generator = entry.getKey();
            if (generator != null && KUBERNETES_CONFIG_SERVICES.equals(generator.getFilename())) {
                svcConfigs = entry.getValue();
                break;
            }
        }

        // 验证是否找到配置
        if (svcConfigs == null || svcConfigs.isEmpty()) {
            logger.warn("未找到{}配置生成器", KUBERNETES_CONFIG_SERVICES);
            return servicePorts;
        }

        // 2. 处理NodePort端口映射
        processPortMappings(
                svcConfigs,
                serviceRoleName.toLowerCase() + "_" + KUBERNETES_NODEPORT_MAPPING,
                servicePorts,
                true // 是否为NodePort类型
        );

        // 3. 处理ClusterIP端口映射
        processPortMappings(
                svcConfigs,
                serviceRoleName.toLowerCase() + "_" + KUBERNETES_CLUSTERIP_MAPPING,
                servicePorts,
                false // 不是NodePort类型
        );

        // 4. 处理LoadBalancer端口映射
        processPortMappings(
                svcConfigs,
                serviceRoleName.toLowerCase() + "_" + KUBERNETES_LOADBALANCER_MAPPING,
                servicePorts,
                false // 不是NodePort类型
        );

        return servicePorts;
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
        final Range<Integer> VALID_PORT_RANGE =  Range.of(1, 65535);
        final Range<Integer> VALID_NODEPORT_RANGE = Range.of(30000, 32767);

        // 用于ClusterIP的重复端口检查
        List<Integer> processedClusterPorts = new ArrayList<>();

        // 1. 查找指定配置
        ServiceConfig mappingConfig = findServiceConfig(svcConfigs, configName);
        if (mappingConfig == null || mappingConfig.getValue() == null) {
            logger.info("未找到配置项：{}", configName);
            return;
        }

        // 2. 解析端口映射
        List<Map<String, String>> portMappings = parsePortMappings(mappingConfig);
        if (portMappings == null || portMappings.isEmpty()) {
            logger.info("配置项{}中没有有效的端口映射", configName);
            return;
        }

        // 3. 创建ServicePort对象
        int index = 0;
        String portType;

        // 根据配置名称和参数确定端口类型
        // 注意: isNodePort是由方法参数决定的
        if (configName.contains(KUBERNETES_NODEPORT_MAPPING)) {
            portType = "nodeport";
        } else if (configName.contains(KUBERNETES_LOADBALANCER_MAPPING)) {
            portType = "loadbalancer";
        } else {
            portType = "clusterport";
        }

        for (Map<String, String> mapping : portMappings) {
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                // 尝试解析端口值
                int port;
                try {
                    if (ObjUtil.isEmpty(entry.getValue())) {
                        logger.warn("端口映射[{}]的值为空，跳过处理", entry.getKey());
                        continue;
                    }
                    port = Integer.parseInt(entry.getKey());
                } catch (NumberFormatException e) {
                    logger.error("无法解析端口映射 [{}:{}]: {}", entry.getKey(), entry.getValue(), e.getMessage());
                    continue;
                }

                // 验证端口范围
                if (!VALID_PORT_RANGE.contains(port)) {
                    logger.warn("配置{}中的端口{}无效，已跳过", configName, port);
                    continue;
                }

                // 确定是否是特殊端口类型
                boolean isLoadBalancer = configName.contains(KUBERNETES_LOADBALANCER_MAPPING);

                // 对于ClusterIP类型，检查重复端口
                if (!isNodePort && !isLoadBalancer) {
                    if (processedClusterPorts.contains(port)) {
                        logger.info("跳过重复的ClusterIP端口配置: {}", port);
                        continue;
                    }
                    processedClusterPorts.add(port);
                }

                // 处理NodePort的多个值
                if (isNodePort) {
                    int[] nodePorts = StrUtil.splitToInt(entry.getValue(), ',');
                    for (int nodePort : nodePorts) {
                        if (!VALID_NODEPORT_RANGE.contains(nodePort)) {
                            logger.warn("NodePort值{}无效，已跳过", nodePort);
                            continue;
                        }
                        // 为每个NodePort值创建独立的ServicePort
                        ServicePort servicePort = createServicePort(port, String.valueOf(nodePort), portType, index++,
                                true,
                                VALID_NODEPORT_RANGE);
                        servicePorts.add(servicePort);
                    }
                } else {
                    // 创建ClusterIP或LoadBalancer的ServicePort
                    ServicePort servicePort = createServicePort(port, entry.getValue(), portType, index++, false,
                            VALID_NODEPORT_RANGE);
                    servicePorts.add(servicePort);
                }
            }
        }
    }

    /**
     * 创建服务端口对象
     */
    private ServicePort createServicePort(int port, String nodePortValue, String portType, int index,
            boolean isNodePort, Range<Integer> validNodePortRange) {
        ServicePort servicePort = new ServicePort();
        servicePort.setPort(port);
        servicePort.setTargetPort(new IntOrString(port));
        servicePort.setName(portType + "-" + index);

        // 对于NodePort类型，设置NodePort值
        if (isNodePort) {
            try {
                int nodePort = Integer.parseInt(nodePortValue);

                // 验证NodePort范围
                if (!validNodePortRange.contains(nodePort)) {
                    logger.warn("NodePort值{}无效，将使用随机端口", nodePort);
                } else {
                    servicePort.setNodePort(nodePort);
                }
            } catch (NumberFormatException e) {
                logger.error("无法解析NodePort值 {}: {}", nodePortValue, e.getMessage());
            }
        }

        return servicePort;
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
        if (config == null || config.getValue() == null) {
            logger.warn("配置项为空，无法解析端口映射");
            return null;
        }

        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Map<String, String>>>() {
            }.getType();
            List<Map<String, String>> result = gson.fromJson(config.getValue().toString(), listType);

            if (result == null || result.isEmpty()) {
                logger.warn("配置项{}中没有有效的端口映射", config.getName());
            }

            return result;
        } catch (Exception e) {
            logger.error("解析{}的端口映射时失败: {}", config.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 处理服务创建
     *
     * @param servicePorts 服务端口列表
     * @param kind         资源类型
     * @param client       Kubernetes客户端
     */
    private void handleNewSvc(String namespace, ArrayList<ServicePort> servicePorts,
            String kind,
            KubernetesClient client) {
        if (servicePorts == null || servicePorts.isEmpty()) {
            logger.info("没有需要创建的服务端口");
            return;
        }

        // 分离基础服务端口、NodePort服务端口和LoadBalancer服务端口
        List<ServicePort> basePorts = new ArrayList<>(); // 基础服务端口（Headless/ClusterIP）
        List<ServicePort> nodePorts = new ArrayList<>(); // NodePort服务端口
        List<ServicePort> loadBalancerPorts = new ArrayList<>(); // LoadBalancer服务端口

        // 跟踪已添加的ClusterIP端口，防止重复
        List<Integer> addedClusterPorts = new ArrayList<>();

        // 处理所有端口
        for (ServicePort originalPort : servicePorts) {
            // 检查端口名称以确定类型
            String portName = originalPort.getName();

            // 处理LoadBalancer端口
            if (portName != null && portName.startsWith("loadbalancer-")) {
                loadBalancerPorts.add(originalPort);
                continue;
            }

            // 处理基础端口
            if (!addedClusterPorts.contains(originalPort.getPort())) {
                // 创建基础服务端口副本
                ServicePort basePort = ObjectUtil.cloneByStream(originalPort);
                basePort.setNodePort(null); // 基础服务不使用NodePort

                // 添加到基础端口集合
                basePorts.add(basePort);

                // 记录已添加的端口
                addedClusterPorts.add(originalPort.getPort());
            } else {
                logger.info("跳过重复的ClusterIP端口: {}", originalPort.getPort());
            }

            // 保留原始NodePort配置
            if (originalPort.getNodePort() != null) {
                nodePorts.add(originalPort);
            }
        }

        // 创建基础服务
        if (STATEFULSET.equals(kind)) {
            createHeadlessService(namespace, basePorts, client);
        } else {
            createClusterIPService(namespace, basePorts, client);
        }

        // 创建独立NodePort服务
        if (!nodePorts.isEmpty()) {
            createNodePortServices(namespace, nodePorts, client);
        }

        // 创建LoadBalancer服务
        if (!loadBalancerPorts.isEmpty()) {
            createLoadBalancerServices(namespace, loadBalancerPorts, client);
        }
    }

    // 创建 Headless Service（StatefulSet）
    private void createHeadlessService(String namespace, List<ServicePort> ports, KubernetesClient client) {
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
                .withNamespace(namespace)
                .endMetadata()
                .withSpec(spec)
                .build();

        // 保存YAML文件到本地
        saveServiceYaml(service, "headless");

        executeServiceCreation(namespace, client, service);
    }

    // 创建 ClusterIP Service（Deployment）
    private void createClusterIPService(String namespace, List<ServicePort> ports, KubernetesClient client) {
        ServiceSpec spec = new ServiceSpecBuilder()
                .withType(KUBERNETES_CLUSTER_IP)
                .withSelector(Collections.singletonMap("app", serviceRoleFullName))
                .withPorts(ports)
                .withPublishNotReadyAddresses()
                .build();

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceRoleFullName)
                .withLabels(Collections.singletonMap("app", serviceRoleFullName + "-svc"))
                .withNamespace(namespace)
                .endMetadata()
                .withSpec(spec)
                .build();

        // 保存YAML文件到本地
        saveServiceYaml(service, "clusterip");

        executeServiceCreation(namespace, client, service);
    }

    /**
     * 创建NodePort服务（通用）
     */
    private void createNodePortServices(String namespace, List<ServicePort> ports, KubernetesClient client) {
        // 定义NodePort的有效范围常量
        final int MIN_NODEPORT = 30000;
        final int MAX_NODEPORT = 32767;
        final Range<Integer> VALID_NODEPORT_RANGE = Range.of(MIN_NODEPORT, MAX_NODEPORT);

        // 统计每个容器端口对应的NodePort数量
        Map<Integer, Integer> portToNodePortCountMap = new HashMap<>();
        for (ServicePort port : ports) {
            if (port.getNodePort() != null) {
                int containerPort = port.getPort();
                portToNodePortCountMap.put(containerPort, portToNodePortCountMap.getOrDefault(containerPort, 0) + 1);
            }
        }

        // 用于生成pod索引
        Map<Integer, Integer> portToPodIndexMap = new HashMap<>();

        for (ServicePort port : ports) {
            // 确保NodePort在有效范围（30000-32767）
            Integer nodePort = port.getNodePort();
            if (nodePort == null) {
                continue; // 跳过没有NodePort的端口
            }

            // 检查端口是否在有效范围内
            if (!VALID_NODEPORT_RANGE.contains(nodePort)) {
                logger.warn("无效的NodePort值 {} 用于 {}，将使用随机端口", nodePort, serviceRoleFullName);
                port.setNodePort(null);
            }

            // 获取容器端口对应的NodePort数量
            int containerPort = port.getPort();
            int nodePortCount = portToNodePortCountMap.getOrDefault(containerPort, 0);

            // 创建服务规范
            ServiceSpecBuilder specBuilder = new ServiceSpecBuilder()
                    .withType(KUBERNETES_NODE_PORT)
                    .withPorts(port)
                    .withPublishNotReadyAddresses();

            // 根据NodePort数量决定选择器策略
            if (nodePortCount > 1) {
                // 多个NodePort映射到同一个容器端口，每个NodePort选择一个特定的pod
                // 获取当前端口的pod索引，并递增
                int podIndex = portToPodIndexMap.getOrDefault(containerPort, 0);
                portToPodIndexMap.put(containerPort, podIndex + 1);

                // 构建完整的pod名称
                String podName = serviceRoleFullName + "-" + podIndex;

                // 使用statefulset.kubernetes.io/pod-name标签选择特定pod
                specBuilder.withSelector(Collections.singletonMap("statefulset.kubernetes.io/pod-name", podName));

                logger.info("为容器端口 {} 的NodePort {} 绑定到特定pod: {}",
                        containerPort, nodePort, podName);
            } else {
                // 单个NodePort映射，选择所有pod
                specBuilder.withSelector(Collections.singletonMap("app", serviceRoleFullName));
                logger.info("为容器端口 {} 的NodePort {} 创建通用pod选择器", containerPort, nodePort);
            }

            // 动态生成服务名称
            String serviceName = serviceRoleFullName + "-nodeport-" + port.getPort() + "-" + nodePort;

            // 创建服务对象
            Service service = new ServiceBuilder()
                    .withNewMetadata()
                    .withName(serviceName)
                    .withLabels(Collections.singletonMap("app", serviceRoleFullName + "-svc"))
                    .withNamespace(namespace)
                    .endMetadata()
                    .withSpec(specBuilder.build())
                    .build();

            // 保存YAML文件到本地
            saveServiceYaml(service, "nodeport");

            // 在集群上创建服务
            executeServiceCreation(namespace, client, service);
        }
    }

    /**
     * 创建LoadBalancer服务，为StatefulSet中的每个Pod创建独立的LoadBalancer服务
     * 并将分配的外部IP存入ConfigMap
     */
    private void createLoadBalancerServices(String namespace, List<ServicePort> ports, KubernetesClient client) {
        if (ports == null || ports.isEmpty()) {
            logger.info("没有需要创建的LoadBalancer端口");
            return;
        }

        // 获取StatefulSet的副本数
        Integer replicaCount = getCountInKey(Constants.ROLE_NODE_CNT);
        if (replicaCount == null || replicaCount <= 0) {
            logger.warn("无法获取{}的副本数，无法创建LoadBalancer服务", serviceRoleFullName);
            return;
        }

        logger.info("为{}的{}个Pod创建LoadBalancer服务", serviceRoleFullName, replicaCount);

        // 创建ConfigMap用于存储外部IP映射
        String configMapName = serviceRoleFullName + "-external";
        Map<String, String> externalIpMap = new HashMap<>();

        // 为每个Pod创建独立的LoadBalancer服务
        for (int podIndex = 0; podIndex < replicaCount; podIndex++) {
            // 构建完整的pod名称
            String podName = serviceRoleFullName + "-" + podIndex;

            for (ServicePort port : ports) {
                // 创建服务规范
                ServiceSpecBuilder specBuilder = new ServiceSpecBuilder()
                        .withType(KUBERNETES_LOAD_BALANCER)
                        .withPorts(port)
                        .withPublishNotReadyAddresses();

                // 使用statefulset.kubernetes.io/pod-name标签选择特定pod
                specBuilder.withSelector(Collections.singletonMap("statefulset.kubernetes.io/pod-name", podName));

                // 动态生成服务名称
                String serviceName = serviceRoleFullName + "-lb-" + podIndex + "-" + port.getPort();

                // 创建服务对象
                Service service = new ServiceBuilder()
                        .withNewMetadata()
                        .withName(serviceName)
                        .withLabels(Collections.singletonMap("app", serviceRoleFullName + "-svc"))
                        .withNamespace(namespace)
                        .endMetadata()
                        .withSpec(specBuilder.build())
                        .build();

                // 保存YAML文件到本地
                saveServiceYaml(service, "loadbalancer");

                // 在集群上创建服务
                executeServiceCreationWithReturnValue(namespace, client, service);

                // 等待LoadBalancer分配外部IP
                String externalIP = waitForLoadBalancerIP(namespace, client, serviceName);
                if (externalIP != null) {
                    logger.info("为Pod {} 创建LoadBalancer服务 {} 映射端口 {}，分配的外部IP: {}",
                            podName, serviceName, port.getPort(), externalIP);

                    // 将Pod名称和外部IP的映射存入Map
                    externalIpMap.put(podName, externalIP);
                } else {
                    logger.error("无法获取LoadBalancer服务 {} 的外部IP", serviceName);
                }
            }
        }

        // 创建或更新ConfigMap，存储Pod名称到外部IP的映射
        if (!externalIpMap.isEmpty()) {
            createOrUpdateExternalIpConfigMap(namespace, client, configMapName, externalIpMap);
        } else {
            logger.warn("没有获取到任何外部IP，不创建ConfigMap");
        }
    }

    /**
     * 执行服务创建并返回创建的服务对象
     */
    private void executeServiceCreationWithReturnValue(String namespace, KubernetesClient client, Service service) {
        try {
            // 创建或更新服务，并返回创建的服务对象
            client.services().inNamespace(namespace).resource(service).serverSideApply();

            // 添加彩色日志输出
            ColorLogUtils.printResourceCreated("Service", service.getMetadata().getName(), namespace);

        } catch (Exception e) {
            logger.error("创建服务失败: {}", e.getMessage(), e);
            ColorLogUtils.printError("创建服务 " + service.getMetadata().getName() + " 失败: " + e.getMessage());
        }
    }

    /**
     * 等待LoadBalancer服务分配外部IP
     */
    private String waitForLoadBalancerIP(String namespace, KubernetesClient client, String serviceName) {
        logger.info("等待LoadBalancer服务 {} 分配外部IP...", serviceName);

        final int MAX_RETRIES = 60; // 最大重试次数
        final int RETRY_INTERVAL_SECONDS = 5; // 重试间隔（秒）

        for (int i = 0; i < MAX_RETRIES; i++) {
            Service service = client.services().inNamespace(namespace).withName(serviceName).get();

            if (service != null && service.getStatus() != null && service.getStatus().getLoadBalancer() != null) {
                List<LoadBalancerIngress> ingresses = service.getStatus().getLoadBalancer().getIngress();

                if (ingresses != null && !ingresses.isEmpty()) {
                    LoadBalancerIngress ingress = ingresses.getFirst();

                    // 优先使用IP地址，如果没有则使用主机名
                    if (StrUtil.isNotBlank(ingress.getIp())) {
                        return ingress.getIp();
                    } else if (StrUtil.isNotBlank(ingress.getHostname())) {
                        return ingress.getHostname();
                    }
                }
            }

            logger.info("LoadBalancer服务 {} 外部IP尚未分配，等待 {} 秒后重试 ({}/{})",
                    serviceName, RETRY_INTERVAL_SECONDS, i + 1, MAX_RETRIES);

            try {
                Thread.sleep(RETRY_INTERVAL_SECONDS * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("等待过程被中断", e);
                return null;
            }
        }

        logger.error("等待LoadBalancer服务 {} 分配外部IP超时", serviceName);
        return null;
    }

    /**
     * 创建或更新ConfigMap，存储Pod名称到外部IP的映射
     */
    private void createOrUpdateExternalIpConfigMap(String namespace, KubernetesClient client, String configMapName,
            Map<String, String> externalIpMap) {
        try {
            // 检查ConfigMap是否已存在
            ConfigMap existingConfigMap = client.configMaps()
                    .inNamespace(namespace)
                    .withName(configMapName)
                    .get();

            if (existingConfigMap != null) {
                logger.info("更新现有ConfigMap: {}", configMapName);

                // 更新现有的ConfigMap
                existingConfigMap.setData(externalIpMap);
                client.configMaps()
                        .inNamespace(namespace)
                        .resource(existingConfigMap)
                        .serverSideApply();

                // 保存ConfigMap的YAML文件
                saveConfigMapYaml(existingConfigMap);

                logger.info("成功更新ConfigMap: {}", configMapName);
                ColorLogUtils.printResourceUpdated("ConfigMap", configMapName, namespace);
            } else {
                logger.info("创建新的ConfigMap: {}", configMapName);

                // 创建新的ConfigMap
                ConfigMap configMap = new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName(configMapName)
                        .withNamespace(namespace)
                        .endMetadata()
                        .withData(externalIpMap)
                        .build();

                client.configMaps()
                        .inNamespace(namespace)
                        .resource(configMap)
                        .serverSideApply();

                // 保存ConfigMap的YAML文件
                saveConfigMapYaml(configMap);

                logger.info("成功创建ConfigMap: {}", configMapName);
                ColorLogUtils.printResourceCreated("ConfigMap", configMapName, namespace);
            }
        } catch (Exception e) {
            logger.error("创建或更新ConfigMap失败: {}", e.getMessage(), e);
            ColorLogUtils.printError("创建或更新ConfigMap " + configMapName + " 失败: " + e.getMessage());
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

    private void handleDeployment(String namespace, KubernetesClient client, Map<String, Object> yamlData,
            InputStream yamlInputStream,
            Map<Generators, List<ServiceConfig>> configFileMap) {
        handleResource(namespace, client, yamlData, yamlInputStream, client.apps().deployments()
                .inNamespace(namespace)
                .withName(serviceRoleFullName), DEPLOYMENT, configFileMap);

    }

    private void handleStatefulSet(String namespace, KubernetesClient client, Map<String, Object> yamlData,
            InputStream yamlInputStream,
            Map<Generators, List<ServiceConfig>> configFileMap) {
        handleResource(namespace, client, yamlData, yamlInputStream, client.apps().statefulSets()
                .inNamespace(namespace)
                .withName(serviceRoleFullName), STATEFULSET, configFileMap);
    }

    private <T extends HasMetadata> void handleResource(String namespace, KubernetesClient client,
            Map<String, Object> yamlData,
            InputStream yamlInputStream,
            RollableScalableResource<T> resource, String resourceKind,
            Map<Generators, List<ServiceConfig>> configFileMap) {
        T existingResource = resource.get();
        boolean isExistingResource = existingResource != null;

        if (isExistingResource) {
            if (DEPLOYMENT.equals(resourceKind)) {
                handleExistingDeployment(namespace, yamlData, client, (Deployment) existingResource);
            } else if (STATEFULSET.equals(resourceKind)) {
                handleExistingStatefulSet(namespace, yamlData, client, (StatefulSet) existingResource);
            }
        } else {
            addProcessStatus();
            if (isFinalNode()) {

                // 确保ConfigMap在其他资源之前创建
                logger.info("开始创建ConfigMap...");

                handleConfigMap(namespace, client, serviceRoleFullName);

                // 生成服务配置和创建服务
                ArrayList<ServicePort> ServicePorts = generateSvcConfig(configFileMap);
                handleNewSvc(namespace, ServicePorts, resourceKind, client);

                // 创建共享PVC
                handlePvc(namespace, client, configFileMap);

                // 直接使用原始YAML创建资源，不修改挂载配置

                if ("hdfs-zkfc".equalsIgnoreCase(serviceRoleFullName)) {
                    // ZKFC作为NameNode Pod的Sidecar容器部署
                    logger.info("ZKFC作为NameNode Pod的Sidecar容器部署，不创建资源");
                    return;
                }
                handleNewResource(namespace, client, yamlInputStream, resource);
            }
        }
    }

    private void handleConfigMap(String namespace, KubernetesClient client, String serviceRoleFullName) {
        // 创建ConfigMap
        KubernetesFreeMakerUtils.createConfigMap(namespace, serviceRoleFullName, client);

        // 创建Secret(如果有)
        KubernetesFreeMakerUtils.createSecrets(namespace, serviceRoleFullName, client);
    }

    private void handleExistingDeployment(String namespace, Map<String, Object> yamlData, KubernetesClient client,
            Deployment existingDeployment) {
        int replicas = existingDeployment.getSpec().getReplicas() != null
                ? existingDeployment.getSpec().getReplicas()
                : 0;
        logger.info("当前 Deployment: {} Replicas: {}", serviceRoleFullName, replicas);

        // 获取期望的节点数
        Integer nodeCount = getCountInKey(Constants.ROLE_NODE_CNT);

        // 如果当前副本数已经等于期望节点数，则直接返回，不执行更新操作
        if (nodeCount != null && replicas == nodeCount) {
            logger.info("Deployment {} 当前副本数 {} 已等于期望节点数 {}，无需修改", serviceRoleFullName, replicas, nodeCount);
            ColorLogUtils.printWarning("Deployment " + serviceRoleFullName + " 副本数已达到预期值 " + nodeCount + "，跳过更新");
            return;
        }

        // 需要更新副本数
        logger.info("修改 Deployment: {} 副本数 {} -> {}", serviceRoleFullName, replicas, replicas + 1);
        updateField(yamlData, "spec.replicas", replicas + 1);

        // 使用公共方法创建或更新资源
        createOrUpdateResource(namespace, client, yamlData, "Deployment", serviceRoleFullName);
    }

    private void handleExistingStatefulSet(String namespace, Map<String, Object> yamlData, KubernetesClient client,
            StatefulSet existingStatefulSet) {
        int replicas = existingStatefulSet.getSpec().getReplicas() != null
                ? existingStatefulSet.getSpec().getReplicas()
                : 0;
        logger.info("当前 StatefulSet: {} Replicas: {}", serviceRoleFullName, replicas);

        // 获取期望的节点数
        Integer nodeCount = getCountInKey(Constants.ROLE_NODE_CNT);

        // 如果当前副本数已经等于期望节点数，则直接返回，不执行更新操作
        if (nodeCount != null && replicas == nodeCount) {
            logger.info("StatefulSet {} 当前副本数 {} 已等于期望节点数 {}，无需修改", serviceRoleFullName, replicas, nodeCount);
            ColorLogUtils.printWarning("StatefulSet " + serviceRoleFullName + " 副本数已达到预期值 " + nodeCount + "，跳过更新");
            return;
        }

        // 需要更新副本数
        logger.info("修改 StatefulSet: {} 副本数 {} -> {}", serviceRoleFullName, replicas, replicas + 1);
        updateField(yamlData, "spec.replicas", replicas + 1);

        // 使用公共方法创建或更新资源
        createOrUpdateResource(namespace, client, yamlData, "StatefulSet", serviceRoleFullName);
    }

    private <T extends HasMetadata> void handleNewResource(String namespace, KubernetesClient client,
            InputStream yamlInputStream,
            RollableScalableResource<T> resource) {

        logger.info("CURRENT_NODE_CNT置空: {}", serviceRoleFullName + "_" + Constants.CURRENT_NODE_CNT);
        CacheUtils.removeKey(serviceRoleFullName + "_" + Constants.CURRENT_NODE_CNT);
        List<HasMetadata> metadata = client.load(yamlInputStream).inNamespace(namespace).serverSideApply();
        String resourceName = metadata.getFirst().getMetadata().getName();
        String resourceKind = metadata.getFirst().getKind();
        logger.info("在kubernetes上启动资源: {} ,使用本地资源文件: {}", resourceName,
                CommonUtil.KubernetesYamlFilePath(serviceRoleFullName));

        // 添加彩色日志
        ColorLogUtils.printResourceCreated(resourceKind, resourceName, namespace);

        resource.waitUntilReady(10, TimeUnit.MINUTES);

        // 获取Pod列表（新增代码）
        List<Pod> pods = client.pods()
                .inNamespace(namespace)
                .withLabel("app", serviceRoleFullName) // 与Service/Deployment共享的标签
                .list()
                .getItems();

        // 提取Pod名称
        List<String> podNames = pods.stream()
                .map(pod -> pod.getMetadata().getName())
                .collect(Collectors.toList());

        logger.info("已启动的Pod列表: {}", podNames);

        CacheUtils.put(serviceRoleFullName + "_" + Constants.POD_NAME, podNames);
    }

    private void handleException(ExecResult execResult, String message, Exception e) {
        logger.error("{}: {}", message, e.getMessage(), e);
        execResult.setExecErrOut(message + ": " + e.getMessage());
        execResult.setExecResult(false);
    }

    public ExecResult stop(KubernetesServiceRoleOperateCommand command) {
        ExecResult execResult = new ExecResult();
        String yamlFile = CommonUtil.KubernetesYamlFilePath(serviceRoleFullName);
        logger.info("本地资源文件: {}", yamlFile);

        File yamlFileObj = new File(yamlFile);

        if (!yamlFileObj.exists()) {
            logger.error("Kubernetes资源文件不存在: {}", yamlFile);
            execResult.setExecErrOut("Kubernetes资源文件不存在: " + yamlFile);
            execResult.setExecOut("Kubernetes资源文件不存在: " + yamlFile);
            execResult.setExecResult(false);
            return execResult;
        } else {
            logger.info("在Kubernetes上停止deployment ,使用本地资源文件: {}", yamlFile);
            if (Files.exists(Paths.get(yamlFile)) && yamlFile.toLowerCase().contains("operator")) {
                String s = KubernetesMinaUtils.execCmdWithResult(GetMasterHost().getFirst(),
                        "kubectl delete -f " + yamlFile);
                logger.info("stop operator: {}", s);
                execResult.setExecResult(true);
                return execResult;
            }
            String namespace = command.getNamespace();
            String kubeConfig = command.getKubeConfig();
            try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig);
                    FileInputStream fis = new FileInputStream(yamlFileObj)) {
                client.load(fis)
                        .inNamespace(namespace)
                        .delete();
                client.services()
                        .inNamespace(namespace)
                        .withLabelSelector("app=" + serviceRoleFullName + "-svc")
                        .delete();
                execResult.setExecResult(true);
                CacheUtils.removeKey(serviceRoleFullName + "_" + Constants.CURRENT_NODE_CNT);
                CacheUtils.removeKey(serviceRoleFullName + "_" + Constants.POD_NAME);
            } catch (Exception e) {
                logger.error("停止deployment时发生异常: {}", e.getMessage(), e);
                execResult.setExecErrOut("停止deployment时发生异常: " + e.getMessage());
                execResult.setExecOut("停止deployment时发生异常: " + e.getMessage());
            }
        }
        return execResult;
    }

    private void addProcessStatus() {
        Integer nodeCount = getCountInKey(Constants.CURRENT_NODE_CNT);
        if (Objects.isNull(nodeCount)) {
            CacheUtils.put(serviceRoleFullName + "_" + Constants.CURRENT_NODE_CNT, 1);
        } else {
            CacheUtils.put(serviceRoleFullName + "_" + Constants.CURRENT_NODE_CNT, nodeCount + 1);
        }
    }

    private Boolean isFinalNode() {
        Integer nodeCount = getCountInKey(Constants.ROLE_NODE_CNT);
        Integer currentCount = getCountInKey(Constants.CURRENT_NODE_CNT);
        logger.info("当前{}: {}个，所需{}: {}个", serviceRoleFullName, currentCount, serviceRoleFullName, nodeCount);
        return currentCount.equals(nodeCount);
    }

    private Integer getCountInKey(String key) {
        return (Integer) CacheUtils.get(serviceRoleFullName + "_" + key);
    }

    // 保存Service的YAML配置到本地文件
    private void saveServiceYaml(Service service, String serviceType) {
        try {
            // 创建保存目录，使用java.nio.file.Paths处理路径拼接
            Path dirPath = Paths.get(StrUtil.blankToDefault(Constants.YAML_PATH, Constants.INSTALL_PATH),
                    "kubernetesYaml",
                    "servers");

            // 确保目录存在
            if (BooleanUtil.isFalse(ensureDirectoryExists(dirPath))) {
                logger.error("无法创建Service YAML保存目录: {}", dirPath);
                return;
            }

            // 生成文件名，使用Paths.get拼接路径
            Path filePath = Paths.get(dirPath.toString(),
                    service.getMetadata().getName() + "-" + serviceType + ".yaml");

            // 使用Kubernetes客户端序列化为YAML
            String yamlContent = KubeUtil.getKubernetesYaml(service);

            // 写入文件
            Files.write(filePath, yamlContent.getBytes());

            logger.info("保存Service YAML文件成功: {}", filePath);
        } catch (Exception e) {
            logger.error("保存Service YAML文件失败: {}", e.getMessage(), e);
        }
    }

    // 保存PVC的YAML配置到本地文件
    private void savePvcYaml(PersistentVolumeClaim pvc) {
        try {
            // 创建保存目录，使用java.nio.file.Paths处理路径拼接
            Path dirPath = Paths.get(StrUtil.blankToDefault(Constants.YAML_PATH, Constants.INSTALL_PATH),
                    "kubernetesYaml",
                    "volumes");

            // 确保目录存在
            if (BooleanUtil.isFalse(ensureDirectoryExists(dirPath))) {
                logger.error("无法创建PVC YAML保存目录: {}", dirPath);
                return;
            }

            // 生成文件名，使用Paths.get拼接路径
            Path filePath = Paths.get(dirPath.toString(),
                    pvc.getMetadata().getName() + ".yaml");

            // 使用Kubernetes客户端序列化为YAML
            String yamlContent = KubeUtil.getKubernetesYaml(pvc);

            // 写入文件
            Files.write(filePath, yamlContent.getBytes());

            logger.info("保存PVC YAML文件成功: {}", filePath);
        } catch (Exception e) {
            logger.error("保存PVC YAML文件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 为服务角色创建PersistentVolumeClaim
     *
     * @param client Kubernetes客户端
     */
    private void handlePvc(String namespace, KubernetesClient client,
            Map<Generators, List<ServiceConfig>> configFileMap) {
        try {
            // 在配置映射中寻找PVC配置生成器
            Generators pvcConfigGenerator = null;
            for (Generators key : configFileMap.keySet()) {
                if (StrUtil.equals(key.getFilename(), "kubernetes.config.persistent-volume-claims")) {
                    pvcConfigGenerator = key;
                    break; // 找到配置后立即退出循环
                }
            }

            // 如果找不到配置生成器，则抛出异常
            if (pvcConfigGenerator == null) {
                String errorMsg = String.format("找不到服务%s的PVC配置生成器", serviceRoleFullName);
                logger.error(errorMsg);
                return;
                // throw new IllegalArgumentException(errorMsg);
            }

            // 获取配置列表
            List<ServiceConfig> serviceConfigs = configFileMap.get(pvcConfigGenerator);
            String storageClassName = null;
            String storageSize = null;
            String mountPath = null;

            // 从配置中提取值（使用变量简化配置名称判断）
            String storageClassKey = serviceRoleName.toLowerCase() + "_storage_classes";
            String storageSizeKey = serviceRoleName.toLowerCase() + "_storage_size";
            String mountPathKey = serviceRoleName.toLowerCase() + "_mount_path";

            // 遍历配置查找所需值
            for (ServiceConfig serviceConfig : serviceConfigs) {
                String configName = serviceConfig.getName();

                if (storageClassName == null && StrUtil.equalsIgnoreCase(configName, storageClassKey)) {
                    storageClassName = serviceConfig.getValue().toString();
                } else if (storageSize == null && StrUtil.equalsIgnoreCase(configName, storageSizeKey)) {
                    storageSize = serviceConfig.getValue().toString();
                } else if (mountPath == null && StrUtil.equalsIgnoreCase(configName, mountPathKey)) {
                    mountPath = serviceConfig.getValue().toString();
                }

                // 如果所有值都已找到，提前结束循环
                if (storageClassName != null && storageSize != null && mountPath != null) {
                    break;
                }
            }

            // 验证所需配置值
            if (storageClassName == null || storageSize == null || mountPath == null) {
                String errorMsg = String.format("服务%s缺少必要的PVC配置。存储类=%s，存储大小=%s，挂载路径=%s",
                        serviceRoleFullName, storageClassName, storageSize, mountPath);
                logger.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // 记录创建信息
            logger.info("正在创建PVC，存储类：{}，存储大小：{}，挂载路径：{}",
                    storageClassName, storageSize, mountPath);

            // PVC名称: serviceRoleFullName (同一个Deployment/StatefulSet的所有pod共享)
            String pvcName = serviceRoleFullName;

            // 构建PVC对象
            PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder()
                    .withNewMetadata()
                    .withName(pvcName)
                    .withNamespace(namespace)
                    .withLabels(Collections.singletonMap("app", serviceRoleFullName))
                    .endMetadata()
                    .withNewSpec()
                    .withAccessModes(Collections.singletonList("ReadWriteMany")) // 使用ReadWriteMany实现共享访问
                    .withNewResources()
                    .withRequests(Collections.singletonMap("storage", new Quantity(storageSize)))
                    .endResources()
                    // 应用存储类名称
                    .withStorageClassName(storageClassName)
                    .endSpec()
                    .build();

            // 保存PVC的YAML文件到本地
            savePvcYaml(pvc);

            // 检查PVC是否已存在
            PersistentVolumeClaim existingPvc = client.persistentVolumeClaims()
                    .inNamespace(namespace)
                    .withName(pvcName)
                    .get();

            if (existingPvc != null) {
                // PVC已存在，记录日志并跳过创建
                logger.info("PVC已存在：{}，跳过创建", pvcName);

                // 在日志中添加提示信息
                ColorLogUtils.printWarning(
                        "PersistentVolumeClaim " + pvcName + " 已存在，跳过创建");

                // 将PVC名称和挂载路径存储在缓存中，以便后续使用
                CacheUtils.put(serviceRoleFullName + "_PVC_NAME", pvcName);
                CacheUtils.put(serviceRoleFullName + "_MOUNT_PATH", mountPath);

                return;
            }

            // 在Kubernetes集群上创建PVC
            PersistentVolumeClaim createdPvc = client.persistentVolumeClaims()
                    .inNamespace(namespace)
                    .resource(pvc)
                    .serverSideApply();

            // 在日志中添加有关共享PVC方法的说明
            logger.info("已创建共享PVC：{}。Pod将挂载子路径：{}/[pod名称]",
                    createdPvc.getMetadata().getName(), namespace);

            // 添加彩色日志输出
            ColorLogUtils.printResourceCreated(
                    "PersistentVolumeClaim",
                    createdPvc.getMetadata().getName(),
                    namespace);

            // 将PVC名称和挂载路径存储在缓存中，以便后续使用
            CacheUtils.put(serviceRoleFullName + "_PVC_NAME", pvcName);
            CacheUtils.put(serviceRoleFullName + "_MOUNT_PATH", mountPath);

        } catch (Exception e) {
            logger.error("创建PVC失败: {}", e.getMessage(), e);
            ColorLogUtils.printError("创建PVC失败: " + e.getMessage());
        }
    }

    /**
     * 统一执行服务创建
     *
     * @param client  Kubernetes客户端
     * @param service 要创建的服务
     */
    private void executeServiceCreation(String namespace, KubernetesClient client, Service service) {
        try {
            // 创建Service
            client.services().inNamespace(namespace).resource(service).serverSideApply();
            logger.info("成功创建服务: {}", service.getMetadata().getName());

            // 添加彩色日志输出
            ColorLogUtils.printResourceCreated("Service", service.getMetadata().getName(), namespace);

            // 保存Service的YAML文件
            String serviceType = service.getSpec().getType();
            saveServiceYaml(service, serviceType);
        } catch (Exception e) {
            logger.error("创建服务失败: {}", e.getMessage(), e);
            ColorLogUtils.printError("创建服务 " + service.getMetadata().getName() + " 失败: " + e.getMessage());
        }
    }

    /**
     * 创建或更新Kubernetes资源
     * 
     * @param namespace    命名空间
     * @param client       Kubernetes客户端
     * @param yamlData     资源配置数据
     * @param resourceKind 资源类型
     * @param resourceName 资源名称
     */
    private void createOrUpdateResource(String namespace, KubernetesClient client, Map<String, Object> yamlData,
            String resourceKind, String resourceName) {
        try (InputStream yamlInputStream = new ByteArrayInputStream(new Yaml().dump(yamlData).getBytes())) {
            // 使用 client.load 加载资源并更新，使用serverSideApply()替代已弃用的createOrReplace
            client.load(yamlInputStream)
                    .inNamespace(namespace)
                    .serverSideApply();

            // 添加彩色日志
            ColorLogUtils.printResourceUpdated(resourceKind, resourceName, namespace);
        } catch (IOException e) {
            logger.error("更新 {} 资源失败: {}", resourceKind, e.getMessage(), e);
            ColorLogUtils.printError("更新 " + resourceKind + " " + resourceName + " 失败: " + e.getMessage());
        }
    }

    /**
     * 确保目录存在，如果不存在则创建
     * 
     * @param dirPath 目录路径
     * @return 如果目录存在或创建成功则返回true，否则返回false
     */
    private static boolean ensureDirectoryExists(Path dirPath) {
        File dir = dirPath.toFile();
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                log.warn("路径已存在但不是目录: {}", dirPath);
                return false;
            }
            return true;
        }

        boolean created = dir.mkdirs();
        if (!created) {
            log.error("无法创建目录: {}", dirPath);
            return false;
        }

        log.debug("成功创建目录: {}", dirPath);
        return true;
    }

}
