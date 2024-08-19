package com.datasophon.k8s.actor.handler;

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
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class K8sServiceHandler {

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

    public ExecResult start(String kubeConfig) {
        ExecResult execResult = new ExecResult();
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);

        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig);
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

            execResult.setExecResult(true);
        } catch (IOException e) {
            log.error("k8s资源文件加载失败: {}", yamlFile, e);
            execResult.setExecErrOut("k8s资源文件加载失败: " + e.getMessage());
            execResult.setExecOut("k8s资源文件加载失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("启动deployment时发生异常: {}", e.getMessage(), e);
            execResult.setExecErrOut("启动deployment时发生异常: " + e.getMessage());
            execResult.setExecOut("启动deployment时发生异常: " + e.getMessage());
        }

        return execResult;
    }

    public ExecResult stop(String kubeConfig) {
        ExecResult execResult = new ExecResult();
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
        log.info("本地资源文件: {}", yamlFile);

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
        } catch (Exception e) {
            log.error("停止deployment时发生异常: {}", e.getMessage(), e);
            execResult.setExecErrOut("停止deployment时发生异常: " + e.getMessage());
            execResult.setExecOut("停止deployment时发生异常: " + e.getMessage());
        }

        return execResult;
    }


}
