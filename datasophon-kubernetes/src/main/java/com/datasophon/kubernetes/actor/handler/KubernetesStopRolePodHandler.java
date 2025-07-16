package com.datasophon.kubernetes.actor.handler;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.constants.Constant;
import com.datasophon.kubernetes.util.CommonUtil;
import com.datasophon.kubernetes.util.KubeUtil;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class KubernetesStopRolePodHandler {

    private String serviceName;

    private String serviceRoleName;

    private String serviceRoleFullName;

    private Logger logger;

    public KubernetesStopRolePodHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public ExecResult stop(String namespace,String kubeConfig, String hostname) {
        ExecResult execResult = new ExecResult();
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            List<Pod> pods = client.pods().inNamespace(namespace).withLabel("app", serviceRoleFullName).list().getItems();
            if (CollUtil.isNotEmpty(pods)) {
                List<String> hostList = pods.stream().map(pod -> pod.getSpec().getNodeName()).collect(Collectors.toList());
                if (CollUtil.isEmpty(pods) || !hostList.contains(hostname)) {
                    execResult.setExecResult(true);
                    return execResult;
                }
                for (Pod pod : pods) {
                    String nodeName = pod.getSpec().getNodeName();
                    if (nodeName != null && nodeName.equals(hostname)) {
                        String podName = pod.getMetadata().getName();
                        logger.info("删除节点 {} 上的pod: {}", hostname, podName);
                        client.pods().delete(pod);
                    }
                }
            }
            execResult.setExecResult(true);
            return execResult;
        } catch (Exception e) {
            logger.error("删除节点 {} 上的pod失败", hostname);
            execResult.setExecOut(e.getMessage());
            execResult.setExecErrOut(e.getMessage());
            execResult.setExecResult(false);
            return execResult;
        }
    }

}
