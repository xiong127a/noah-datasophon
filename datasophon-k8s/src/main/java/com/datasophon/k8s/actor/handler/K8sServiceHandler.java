package com.datasophon.k8s.actor.handler;

import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class K8sServiceHandler {

    private String serviceName;

    private String serviceRoleName;

    private String serviceRoleFullName;

    private Logger logger;

    private static final Long timeout = 300L;

    public K8sServiceHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
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
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
        addProcessStatus();
        execResult.setExecResult(true);

        if (isFinalNode()) {
            try (KubernetesClient client = KubeUtil.getKubeClientByConfig(command.getKubeConfig());
                 InputStream yamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {
                List<HasMetadata> metadata = client.load(yamlInputStream).inNamespace(Constant.K8S_NAMESPACE).create();
                String deploymentName = metadata.get(0).getMetadata().getName();
                final Deployment deployment = client.apps().deployments().inNamespace(Constant.K8S_NAMESPACE).withName(deploymentName).get();
                Resource<Deployment> resource = client.resource(deployment).inNamespace(Constant.K8S_NAMESPACE);

                log.info("在k8s上启动deployment: {} ,使用本地资源文件: {}", deploymentName, yamlFile);
                resource.waitUntilReady(20, TimeUnit.MINUTES);

                log.info("开始打印deployment: {} 的输出日志", deploymentName);
                RollableScalableResource<Deployment> scalableResource = client.apps().deployments().inNamespace(Constant.K8S_NAMESPACE).withName(deploymentName);
                log.info(scalableResource.getLog());
            } catch (Exception e) {
                log.error("启动deployment时发生异常: {}", e.getMessage(), e);
                execResult.setExecErrOut("启动deployment时发生异常: " + e.getMessage());
                execResult.setExecOut("启动deployment时发生异常: " + e.getMessage());
                execResult.setExecResult(false);
            }
        }

        return execResult;
    }

    public ExecResult stop(String kubeConfig) {
        ExecResult execResult = new ExecResult();
        Boolean status = (Boolean) CacheUtils.get(serviceRoleFullName);
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
        log.info("本地资源文件: {}", yamlFile);

        if (Objects.isNull(status)) {
            CacheUtils.put(serviceRoleFullName, false);
            File yamlFileObj = new File(yamlFile);
            if (!yamlFileObj.exists()) {
                log.error("k8s资源文件不存在: {}", yamlFile);
                execResult.setExecErrOut("k8s资源文件不存在: " + yamlFile);
                execResult.setExecOut("k8s资源文件不存在: " + yamlFile);
                return execResult;
            }

            log.info("在k8s上停止deployment ,使用本地资源文件: {}", yamlFile);
            try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig);
                 FileInputStream fis = new FileInputStream(yamlFileObj)) {
                client.load(fis)
                        .inNamespace(Constant.K8S_NAMESPACE)
                        .delete();
                execResult.setExecResult(true);
                CacheUtils.put(serviceRoleFullName, true);
            } catch (Exception e) {
                log.error("停止deployment时发生异常: {}", e.getMessage(), e);
                execResult.setExecErrOut("停止deployment时发生异常: " + e.getMessage());
                execResult.setExecOut("停止deployment时发生异常: " + e.getMessage());
            }
        } else {
            execResult.setExecResult(status);
        }

        return execResult;
    }

    /**
     * @param timeoutDuration 等待时长
     * @param timeUnit        市场单位
     */
    private boolean waitForCondition(long timeoutDuration, TimeUnit timeUnit) {
        boolean conditionMet = false;
        long startTime = System.currentTimeMillis();
        long timeout = timeUnit.toMillis(timeoutDuration);

        while (System.currentTimeMillis() - startTime < timeout) {
            // 假设这里是检查条件是否变为 true 的逻辑
            conditionMet = (Boolean) CacheUtils.get(serviceRoleFullName);

            if (conditionMet) {
                return true;
            }

            try {
                TimeUnit.SECONDS.sleep(5); // 每隔5秒检查一次
            } catch (InterruptedException e) {
                log.error("等待过程中被中断", e);
                Thread.currentThread().interrupt(); // 保留中断状态
                return false;
            }
        }

        log.info("超时等待结束，状态未变为 true，将 execResult 设为 false");
        return false;
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
        return currentCount.equals(nodeCount);
    }

}
