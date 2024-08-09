package com.datasophon.k8s.actor.handler;

import com.datasophon.common.enums.K8sHostTagOperation;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            switch (tagOperation) {
                case ADD_TAG:
                    addTag(hostName, client);
                    execResult.setExecOut("Tag added successfully to host " + hostName);
                    break;
                case CANCEL_TAG:
                    cancelTag(hostName, client);
                    execResult.setExecOut("Tag canceled successfully from host " + hostName);
                    break;
                default:
                    break;
            }
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