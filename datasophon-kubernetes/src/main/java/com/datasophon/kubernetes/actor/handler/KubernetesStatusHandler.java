package com.datasophon.kubernetes.actor.handler;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.util.CommonUtil;
import com.datasophon.kubernetes.util.KubeUtil;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 查询组件实例的状态
 * true 为 运行正常
 */

@Data
public class KubernetesStatusHandler {

    private String serviceName;

    private String serviceRoleName;

    private String serviceRoleFullName;

    private Logger logger;

    public KubernetesStatusHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constants.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public ExecResult status(String namespace,String kubeConfig, String hostname) {

        ExecResult execResult = new ExecResult();
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            //获取服务对应的所有pod
            List<Pod> pods = client.pods().inNamespace(namespace).withLabel("app", serviceRoleFullName).list().getItems();

            //没有对应的pod ，状态为 false
            List<String> hostList = pods.stream().map(pod -> pod.getSpec().getNodeName()).toList();
            if (CollUtil.isEmpty(pods) || !hostList.contains(hostname)) {
                execResult.setExecResult(false);
                return execResult;
            }

            execResult.setExecResult(false);

            /*
             * 受限于原有逻辑，此处 获取pod 状态效率不高，
             * 可考虑批量获取，批量状态告警
             */
            //遍历找到对应hostname 上的pod
            for (Pod pod : pods) {
                String nodeName = pod.getSpec().getNodeName();
                if (nodeName != null && nodeName.equals(hostname)) {
                    String podName = pod.getMetadata().getName();
                    String phase = pod.getStatus().getPhase();
                    //logger.info("check pod status  service role instants: {}, pod: {} , status:{}", serviceRoleFullName, podName, phase);

                    //判断pod 状态是否正常
                    execResult.setExecResult("Running".equals(phase) || "Ready".equals(phase));
                }
            }
            return execResult;

        } catch (Exception e) {
            logger.error("check service role instance error ,host {}, instance name: {}, e: {}", hostname, serviceRoleFullName, e.getMessage());
            execResult.setExecOut(e.getMessage());
            execResult.setExecErrOut(e.getMessage());
            execResult.setExecResult(false);
            return execResult;
        }
    }

}
