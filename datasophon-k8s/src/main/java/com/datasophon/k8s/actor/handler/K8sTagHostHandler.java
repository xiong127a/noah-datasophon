package com.datasophon.k8s.actor.handler;

import com.datasophon.common.Constants;
import com.datasophon.common.enums.K8sHostTagOperation;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class K8sTagHostHandler {

    private String serviceName;

    private String serviceRoleName;

    private String serviceRoleFullName;

    private Logger logger;

    public K8sTagHostHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public ExecResult operateTag(String hostName, String kubeConfig, K8sHostTagOperation tagOperation) {
        ExecResult execResult = new ExecResult();
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            NodeList list = client.nodes().list();
            List<Node> items = list.getItems();
            List<ClusterHostDO> collect = items.stream().map(KubeUtil::getNodeInfo).collect(Collectors.toList());

            // 新增：检查其他节点是否有标签但无Pod的情况
            for (ClusterHostDO host : collect) {
                String otherHostName = host.getHostname();
                // 跳过当前主机
                if (otherHostName.equals(hostName)) {
                    continue;
                }

                List<Pod> otherPods = client.pods()
                        .inNamespace(Constants.DATASOPHON)
                        .withLabel(serviceRoleFullName, "true")
                        .withField("spec.nodeName", otherHostName)
                        .list()
                        .getItems();

                // 有标签但无Pod，删除无效标签
                if (otherPods.isEmpty()) {
                    logger.info("Host {} has label but no pod, removing invalid label", otherHostName);
                    cancelTag(otherHostName, client);
                }

            }

            switch (tagOperation) {
                case ADD_TAG:
                    addTag(hostName, client);
                    break;
                case CANCEL_TAG:
                    cancelTag(hostName, client);
                    break;
                default:
                    break;
            }
            execResult.setExecOut("Tag operate successfully to host " + hostName);
            execResult.setExecResult(true);
        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("{} tag at host {} error!", serviceRoleName, hostName, e);
        }
        return execResult;
    }

    private void addTag(String hostName, KubernetesClient client) {
        client.nodes().withName(hostName)
                .edit(r -> new NodeBuilder(r)
                        .editMetadata()
                        .removeFromLabels(serviceRoleFullName)
                        .addToLabels(serviceRoleFullName, "true")
                        .endMetadata()
                        .build());
    }

    private void cancelTag(String hostname, KubernetesClient client) {
        client.nodes().withName(hostname)
                .edit(r -> new NodeBuilder(r)
                        .editMetadata()
                        .removeFromLabels(serviceRoleFullName)
                        .endMetadata()
                        .build());
    }

}