package com.datasophon.k8s.util;

import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.domain.host.enums.HostState;
import com.datasophon.domain.host.enums.MANAGED;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KubeUtil {

    public static KubernetesClient getKubeClientByConfig(String kubeConfig) {
        Config config = Config.fromKubeconfig(kubeConfig);
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    public static void testConnect(KubernetesClient client) {
        String minor = client.getKubernetesVersion().getMinor();
        log.info("成功连接k8s集群：{}", client.getMasterUrl());
    }

    public static boolean checkNamespace(KubernetesClient kubernetesClient, String namespace) {
        return kubernetesClient.namespaces().withName(namespace).get() != null;
    }

    /**
     * 获取k8s节点列表
     */
    public static List<ClusterHostDO> getHostListByConfig(String kubeConfig) {
        KubernetesClient client = getKubeClientByConfig(kubeConfig);
        NodeList list = client.nodes().list();
        List<Node> items = list.getItems();
        return items.stream().map(KubeUtil::getNodeInfo).collect(Collectors.toList());
    }

    public static ClusterHostDO getNodeInfo(Node node) {
        int coreNum = node.getStatus().getCapacity().get("cpu").getNumericalAmount().intValue();
        long totalMemory = node.getStatus().getCapacity().get("memory").getNumericalAmount().longValue();
        long allowMemory = node.getStatus().getAllocatable().get("memory").getNumericalAmount().longValue();
        long totalDisk = node.getStatus().getCapacity().get("ephemeral-storage").getNumericalAmount().longValue();
        long allowDisk = node.getStatus().getAllocatable().get("ephemeral-storage").getNumericalAmount().longValue();
        String ip = node.getStatus().getAddresses().stream().filter(n -> n.getType().equals("InternalIP")).findFirst()
                .get().getAddress();
        String hostname = node.getStatus().getAddresses().stream().filter(n -> n.getType().equals("Hostname"))
                .findFirst().get().getAddress();
        String architecture = node.getStatus().getNodeInfo().getArchitecture();

        return ClusterHostDO.builder()
                .ip(ip)
                .createTime(new Date())
                .hostname(hostname)
                .coreNum(coreNum)
                .totalMem(ByteConverter.convertKBToGB(totalMemory))
                .totalDisk(ByteConverter.convertKBToGB(totalDisk))
                .usedMem(ByteConverter.convertKBToGB(totalMemory - allowMemory))
                .usedDisk(ByteConverter.convertKBToGB(totalDisk - allowDisk))
                .hostState(HostState.RUNNING)
                .managed(MANAGED.YES)
                .cpuArchitecture(architecture)
                .build();
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

}
