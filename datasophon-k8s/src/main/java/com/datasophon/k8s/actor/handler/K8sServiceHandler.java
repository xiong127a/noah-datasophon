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
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class K8sServiceHandler {

    private static final Long timeout = 300L;
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

    // 更新指定字段的值
    public static void updateField(Map<String, Object> yamlData, String fieldPath, Object newValue) {
        // 将字段路径按 '.' 分割以支持嵌套字段
        String[] keys = fieldPath.split("\\.");

        // 遍历路径以找到目标字段
        Map<String, Object> currentMap = yamlData;

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];

            // 检查当前地图是否包含该键
            if (currentMap.containsKey(key)) {
                // 获取下一个层级的 Map
                currentMap = (Map<String, Object>) currentMap.get(key);
            } else {
                // 如果路径不存在，直接返回
                System.out.println("Field path does not exist: " + fieldPath);
                return;
            }
        }

        // 设置新值
        currentMap.put(keys[keys.length - 1], newValue);
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
        execResult.setExecResult(true);
        boolean isExistingDeployment = false;
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(command.getKubeConfig());
             InputStream yamlInputStream = Files.newInputStream(Paths.get(yamlFile))) {
            RollableScalableResource<Deployment> resource =
                    client.apps().deployments().inNamespace(Constant.K8S_NAMESPACE).withName(serviceRoleFullName);
            Deployment existingDeployment = resource.get();
            //如果之前已经deployment，执行了添加实例，replicas+1 不再次提交deployment
            if (Objects.nonNull(existingDeployment)) {
                isExistingDeployment = true;
                Integer replicas = existingDeployment.getSpec().getReplicas() != null ? existingDeployment.getSpec().getReplicas() : 0;

                // 读取 YAML 文件并加载数据
                Yaml yaml = new Yaml();

                Map<String, Object> yamlData = yaml.load(yamlInputStream);

                log.info("当前 deployment: {} Replicas: {}", serviceRoleFullName, replicas);

                // 更新 replicas 字段
                updateField(yamlData, "spec.replicas", replicas + 1);

                // 将更新后的 YAML 应用到 Kubernetes
                try (InputStream updatedYamlInputStream = new ByteArrayInputStream(yaml.dump(yamlData).getBytes())) {
                    client.load(updatedYamlInputStream).createOrReplace();
                } catch (IOException e) {
                    log.error("更新 Kubernetes Deployment 失败: {}", e.getMessage());
                    return execResult; // 处理更新失败的异常
                }
            }
            addProcessStatus();
            //多个副本同时启动提升安装启动速度
            if (isFinalNode() && !isExistingDeployment) {
                log.info("CURRENT_NODE_CNT置空: {}", serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT);

                CacheUtils.removeKey(serviceRoleFullName + "_" + Constant.CURRENT_NODE_CNT);

                List<HasMetadata> metadata = client.load(yamlInputStream).inNamespace(Constant.K8S_NAMESPACE).create();

                String deploymentName = metadata.get(0).getMetadata().getName();
                log.info("在k8s上启动deployment: {} ,使用本地资源文件: {}", deploymentName, yamlFile);
                // 等待Pod准备就绪
                resource.waitUntilReady(20, TimeUnit.MINUTES);
                log.info(resource.getLog());
            }


        } catch (IOException e) {
            log.error("文件操作时发生异常: {}", e.getMessage(), e);
            execResult.setExecErrOut("文件操作时发生异常: " + e.getMessage());
            execResult.setExecResult(false);
        } catch (KubernetesClientException e) {
            log.error("与 Kubernetes 交互时发生异常: {}", e.getMessage(), e);
            execResult.setExecErrOut("与 Kubernetes 交互时发生异常: " + e.getMessage());
            execResult.setExecResult(false);
        } catch (Exception e) {
            log.error("启动deployment时发生异常: {}", e.getMessage(), e);
            execResult.setExecErrOut("启动deployment时发生异常: " + e.getMessage());
            execResult.setExecResult(false);
        }

        return execResult;
    }

    public ExecResult stop(String kubeConfig) {
        ExecResult execResult = new ExecResult();
        Boolean status = (Boolean) CacheUtils.get(serviceRoleFullName);
        String yamlFile = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
        log.info("本地资源文件: {}", yamlFile);

        File yamlFileObj = new File(yamlFile);
        if (Objects.isNull(status)) {
            CacheUtils.put(serviceRoleFullName, false);
        }
        if (!yamlFileObj.exists()) {
            log.error("k8s资源文件不存在: {}", yamlFile);
            execResult.setExecErrOut("k8s资源文件不存在: " + yamlFile);
            execResult.setExecOut("k8s资源文件不存在: " + yamlFile);
            execResult.setExecResult(false);
            return execResult;
        } else {
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
        log.info("当前{}: {}个，所需{}: {}个", serviceRoleFullName, currentCount, serviceRoleFullName, nodeCount);
        return currentCount.equals(nodeCount);
    }

}
