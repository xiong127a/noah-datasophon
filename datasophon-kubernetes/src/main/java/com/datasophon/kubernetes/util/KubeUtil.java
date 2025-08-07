package com.datasophon.kubernetes.util;

import com.datasophon.kubernetes.model.K8sNodeInfo;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KubeUtil {

    public static KubernetesClient getKubeClientByConfig(String kubeConfig) {
        Config config = Config.fromKubeconfig(kubeConfig);
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    public static void testConnect(KubernetesClient client) {
        client.getKubernetesVersion();
        log.info("成功连接Kubernetes集群：{}", client.getMasterUrl());
    }

    public static boolean checkNamespace(KubernetesClient kubernetesClient, String namespace) {
        return kubernetesClient.namespaces().withName(namespace).get() != null;
    }

    /**
     * 创建命名空间
     */
    public static boolean createNamespace(KubernetesClient kubernetesClient, String namespace) {
        try {
            // 检查命名空间是否已存在
            if (checkNamespace(kubernetesClient, namespace)) {
                log.info("命名空间 {} 已存在，跳过创建", namespace);
                return true;
            }

            // 创建命名空间
            io.fabric8.kubernetes.api.model.Namespace ns = new io.fabric8.kubernetes.api.model.NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespace)
                    .endMetadata()
                    .build();

            kubernetesClient.namespaces().resource(ns).create();
            log.info("成功创建命名空间：{}", namespace);
            return true;
        } catch (Exception e) {
            log.error("创建命名空间 {} 失败", namespace, e);
            return false;
        }
    }

    /**
     * 获取Kubernetes节点列表
     */
    public static List<K8sNodeInfo> getHostListByConfig(String kubeConfig) {
        KubernetesClient client = getKubeClientByConfig(kubeConfig);
        NodeList list = client.nodes().list();
        List<Node> items = list.getItems();
        return items.stream().map(KubeUtil::getNodeInfo).collect(Collectors.toList());
    }

    /**
     * 从Kubernetes Node对象中提取节点信息并转换为K8sNodeInfo对象
     * 
     * @param node Kubernetes Node对象
     * @return 转换后的K8sNodeInfo对象
     */
    public static K8sNodeInfo getNodeInfo(Node node) {
        if (node == null) {
            log.warn("传入的Node对象为null，无法提取节点信息");
            return null;
        }

        try {
            // 提取CPU、内存和磁盘资源
            int coreNum = getResourceIntValue(node);
            long totalMemory = getResourceLongValue(node, "memory", "capacity");
            long allowMemory = getResourceLongValue(node, "memory", "allocatable");
            long totalDisk = getResourceLongValue(node, "ephemeral-storage", "capacity");
            long allowDisk = getResourceLongValue(node, "ephemeral-storage", "allocatable");

            // 提取IP地址和主机名
            String ip = getNodeAddress(node, "InternalIP");
            String hostname = getNodeAddress(node, "Hostname");

            // 提取架构信息
            String architecture = node.getStatus() != null && node.getStatus().getNodeInfo() != null
                    ? node.getStatus().getNodeInfo().getArchitecture()
                    : "unknown";

            // 提取节点状态
            String status = getNodeStatus(node);

            // 计算已使用的内存和磁盘
            long usedMem = totalMemory > allowMemory ? totalMemory - allowMemory : 0L;
            long usedDisk = totalDisk > allowDisk ? totalDisk - allowDisk : 0L;

            // 提取节点角色信息
            String roles = getNodeRoles(node);

            // 提取Kubernetes版本
            String kubeVersion = node.getStatus() != null && node.getStatus().getNodeInfo() != null
                    ? node.getStatus().getNodeInfo().getKubeletVersion()
                    : "unknown";

            // 计算节点年龄
            String age = calculateNodeAge(node);

            // 构建并返回K8sNodeInfo对象
            return K8sNodeInfo.builder()
                    .ip(ip)
                    .hostname(hostname)
                    .coreNum(coreNum)
                    .totalMem(ByteConverter.convertKBToGB(totalMemory))
                    .totalDisk(ByteConverter.convertKBToGB(totalDisk))
                    .usedMem(ByteConverter.convertKBToGB(usedMem))
                    .usedDisk(ByteConverter.convertKBToGB(usedDisk))
                    .cpuArchitecture(architecture)
                    .status(status)
                    .createTime(new Date())
                    .allocatableCpu(String.valueOf(coreNum))
                    .allocatableMemory(ByteConverter.convertKBToGB(allowMemory) + "GB")
                    .allocatableStorage(ByteConverter.convertKBToGB(allowDisk) + "GB")
                    .roles(roles)
                    .kubeVersion(kubeVersion)
                    .age(age)
                    .build();
        } catch (Exception e) {
            log.error("提取节点信息时发生异常", e);
            return null;
        }
    }

    /**
     * 从Node对象中安全地获取指定类型的地址
     * 
     * @param node        Node对象
     * @param addressType 地址类型（如InternalIP, Hostname等）
     * @return 地址字符串，如果未找到则返回"unknown"
     */
    private static String getNodeAddress(Node node, String addressType) {
        if (node == null || node.getStatus() == null || node.getStatus().getAddresses() == null) {
            return "unknown";
        }

        return node.getStatus().getAddresses().stream()
                .filter(addr -> addressType.equals(addr.getType()))
                .findFirst()
                .map(NodeAddress::getAddress)
                .orElse("unknown");
    }

    /**
     * 从Node对象中获取节点状态
     * 
     * @param node Node对象
     * @return 节点状态字符串（Ready, NotReady等）
     */
    private static String getNodeStatus(Node node) {
        if (node == null || node.getStatus() == null || node.getStatus().getConditions() == null) {
            return "Unknown";
        }

        return node.getStatus().getConditions().stream()
                .filter(condition -> "Ready".equals(condition.getType()))
                .findFirst()
                .map(condition -> "True".equals(condition.getStatus()) ? "Ready" : "NotReady")
                .orElse("Unknown");
    }

    /**
     * 从Node对象中安全地获取资源的整数值
     *
     * @param node Node对象
     * @return 资源的整数值
     */
    private static int getResourceIntValue(Node node) {
        try {
            if (node == null || node.getStatus() == null) {
                return 1;
            }

            if (node.getStatus().getCapacity() != null &&
                    node.getStatus().getCapacity().get("cpu") != null) {
                return node.getStatus().getCapacity().get("cpu").getNumericalAmount().intValue();
            }

            return 1;
        } catch (Exception e) {
            log.warn("获取资源{}的{}值时出错，使用默认值{}", "cpu", "capacity", 1, e);
            return 1;
        }
    }

    /**
     * 从Node对象中安全地获取资源的长整型值
     *
     * @param node         Node对象
     * @param resourceName 资源名称（如cpu, memory等）
     * @param resourceType 资源类型（capacity或allocatable）
     * @return 资源的长整型值
     */
    private static long getResourceLongValue(Node node, String resourceName, String resourceType) {
        try {
            if (node == null || node.getStatus() == null) {
                return 0L;
            }

            if ("capacity".equals(resourceType) && node.getStatus().getCapacity() != null &&
                    node.getStatus().getCapacity().get(resourceName) != null) {
                return node.getStatus().getCapacity().get(resourceName).getNumericalAmount().longValue();
            } else if ("allocatable".equals(resourceType) && node.getStatus().getAllocatable() != null &&
                    node.getStatus().getAllocatable().get(resourceName) != null) {
                return node.getStatus().getAllocatable().get(resourceName).getNumericalAmount().longValue();
            }

            return 0L;
        } catch (Exception e) {
            log.warn("获取资源{}的{}值时出错，使用默认值{}", resourceName, resourceType, 0L, e);
            return 0L;
        }
    }

    /**
     * 将Kubernetes对象序列化为YAML格式
     * 
     * @param obj Kubernetes对象
     * @return YAML格式字符串
     */
    public static String getKubernetesYaml(HasMetadata obj) {
        return Serialization.asYaml(obj);
    }

    /**
     * 获取节点角色信息
     * 
     * @param node K8S节点对象
     * @return 节点角色字符串
     */
    private static String getNodeRoles(Node node) {
        if (node == null || node.getMetadata() == null || node.getMetadata().getLabels() == null) {
            return "<none>";
        }

        Map<String, String> labels = node.getMetadata().getLabels();
        
        // 检查控制平面节点标签
        if (labels.containsKey("node-role.kubernetes.io/control-plane") ||
            labels.containsKey("node-role.kubernetes.io/master")) {
            return "control-plane";
        }
        
        // 检查其他角色标签（node-role.kubernetes.io/xxx）
        List<String> roles = labels.keySet().stream()
                .filter(s -> s.startsWith("node-role.kubernetes.io/"))
                .map(s -> s.substring("node-role.kubernetes.io/".length()))
                .filter(role -> !role.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        
        if (!roles.isEmpty()) {
            return String.join(",", roles);
        }
        
        return "<none>";
    }

    /**
     * 计算节点年龄
     * 
     * @param node K8S节点对象
     * @return 节点年龄字符串（如：43d, 2h30m）
     */
    private static String calculateNodeAge(Node node) {
        if (node == null || node.getMetadata() == null || node.getMetadata().getCreationTimestamp() == null) {
            return "unknown";
        }

        try {
            // Kubernetes时间戳通常是ISO 8601格式的字符串，需要解析
            String timestampStr = node.getMetadata().getCreationTimestamp();
            Instant creationTime = Instant.parse(timestampStr);
            Instant now = Instant.now();
            java.time.Duration duration = java.time.Duration.between(creationTime, now);

            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;

            if (days > 0) {
                return days + "d";
            } else if (hours > 0) {
                if (minutes > 0) {
                    return hours + "h" + minutes + "m";
                } else {
                    return hours + "h";
                }
            } else {
                return minutes + "m";
            }
        } catch (Exception e) {
            log.warn("计算节点年龄时出错", e);
            return "unknown";
        }
    }

}
