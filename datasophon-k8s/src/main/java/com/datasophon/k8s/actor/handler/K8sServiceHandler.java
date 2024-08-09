package com.datasophon.k8s.actor.handler;

import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
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
            // 加载YAML文件
            List<HasMetadata> metadata = client.load(yamlInputStream).get();

            if (metadata.isEmpty() || !(metadata.get(0) instanceof Deployment)) {
                throw new IllegalStateException("YAML文件中没有找到有效的Deployment资源");
            }

            String deploymentName = metadata.get(0).getMetadata().getName();
            log.info("检查deployment: {} 是否已经存在", deploymentName);

            // 获取Deployment资源
            RollableScalableResource<Deployment> resource = client.apps()
                    .deployments()
                    .inNamespace(Constant.K8S_NAMESPACE)
                    .withName(deploymentName);

            Deployment existingDeployment = resource.get();
            if (existingDeployment != null && existingDeployment.getStatus().getReplicas() > 0) {
                log.info("deployment: {} 已经存在且运行中，跳过启动逻辑", deploymentName);
                execResult.setExecResult(true);
                execResult.setExecOut("deployment已经存在且运行中");
                return execResult;
            }

            // 加载YAML文件并创建资源
            try (InputStream newYamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {
                metadata = client.load(newYamlInputStream)
                        .inNamespace(Constant.K8S_NAMESPACE)
                        .create();
            }

            log.info("在k8s上启动deployment: {} ,使用本地资源文件: {}", deploymentName, yamlFile);

            // 等待Deployment资源准备就绪
            resource.waitUntilReady(20, TimeUnit.MINUTES);

            // 打印Deployment的输出日志
            log.info("开始打印deployment: {} 的输出日志", deploymentName);
            log.info(resource.getLog());

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
