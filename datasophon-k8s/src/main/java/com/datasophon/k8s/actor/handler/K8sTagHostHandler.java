package com.datasophon.k8s.actor.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.K8sHostTagOperation;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.datasophon.common.Constants.SERVICE_ROLE_HOST_MAPPING;
import static com.datasophon.common.Constants.UNDERLINE;

@Data
public class K8sTagHostHandler {

    // ANSI颜色代码
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";

    // 使用ConcurrentHashMap保存每个集群和服务角色的处理计数
    private static final ConcurrentHashMap<String, AtomicInteger> processCounters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> totalCounters = new ConcurrentHashMap<>();

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

    /**
     * 执行标签操作
     *
     * @param clusterId       集群ID
     * @param hostName        主机名
     * @param kubeConfig      Kubernetes配置
     * @param isLastExecution 是否是最后一次执行
     * @return 执行结果
     */
    public ExecResult operateTag(Integer clusterId, ExecResult execResult, String hostName, String kubeConfig,
            boolean isLastExecution) {

        // 处理添加标签操作
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            // 1. 获取要打标签的所有主机列表
            JSONArray targetHosts = getTargetHosts(clusterId);
            if (targetHosts == null) {
                System.out.println(ANSI_RED + "❌ 无法获取集群 " + clusterId + " 的服务角色主机映射信息" + ANSI_RESET);
                execResult.setExecErrOut("Failed to get service role host mapping for cluster " + clusterId);
                execResult.setExecResult(false);
                return execResult;
            }

            // 2. 记录处理状态
            logProcessingInfo(hostName, isLastExecution, targetHosts.size());

            // 3. 如果是最后一次执行，则同步所有标签状态
            if (isLastExecution) {
                System.out.println(ANSI_BLUE + "🔍 这是最后一个主机，开始执行全面标签同步..." + ANSI_RESET);
                synchronizeLabels(client, targetHosts);
                System.out.println(ANSI_GREEN + "✅ 标签同步完成" + ANSI_RESET);
            } else {
                System.out.println(ANSI_CYAN + "ℹ️ 非最后一次执行，跳过标签操作，等待最终调用..." + ANSI_RESET);
            }

            System.out.println(ANSI_GREEN + "✅ 完成主机 " + hostName + " 的标签检查" + ANSI_RESET);
            execResult.setExecOut("Tag operation check completed for host " + hostName);
            execResult.setExecResult(true);

        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ 处理主机 " + hostName + " 的标签操作失败: " + e.getMessage() + ANSI_RESET);
            logger.error("Failed to perform tag operation on host {}: {}", hostName, e.getMessage(), e);
            execResult.setExecErrOut(e.getMessage());
            execResult.setExecResult(false);
        }

        return execResult;
    }

    /**
     * 执行标签操作（兼容旧版本，内部自动判断是否为最后一次执行）
     */
    public ExecResult operateTag(Integer clusterId, String hostName, String kubeConfig,
            K8sHostTagOperation tagOperation) {
        ExecResult execResult = new ExecResult();

        // 如果是取消标签操作，直接执行
        if (tagOperation == K8sHostTagOperation.CANCEL_TAG) {
            try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
                System.out.println(ANSI_YELLOW + "⚠️ 正在从主机 " + hostName + " 移除标签: " + serviceRoleFullName + ANSI_RESET);
                cancelTag(hostName, client);
                System.out.println(ANSI_GREEN + "✅ 成功从主机 " + hostName + " 移除标签: " + serviceRoleFullName + ANSI_RESET);
                execResult.setExecOut("Successfully removed tag from host " + hostName);
                execResult.setExecResult(true);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "❌ 从主机 " + hostName + " 移除标签失败: " + e.getMessage() + ANSI_RESET);
                logger.error("Failed to remove tag from host {}: {}", hostName, e.getMessage(), e);
                execResult.setExecErrOut(e.getMessage());
                execResult.setExecResult(false);
            }
            return execResult;
        }

        // 获取要打标签的所有主机列表
        JSONArray targetHosts = getTargetHosts(clusterId);
        if (targetHosts == null) {
            execResult = new ExecResult();
            System.out.println(ANSI_RED + "❌ 无法获取集群 " + clusterId + " 的服务角色主机映射信息" + ANSI_RESET);
            execResult.setExecErrOut("Failed to get service role host mapping for cluster " + clusterId);
            execResult.setExecResult(false);
            return execResult;
        }

        // 判断是否为最后一次执行：比较当前主机名与列表最后一个元素是否相同
        String lastHost = CollUtil.getLast(targetHosts.toList(String.class));
        boolean isLastExecution = hostName.equals(lastHost);

        // 调用新版本方法
        return operateTag(clusterId, execResult, hostName, kubeConfig, isLastExecution);
    }

    /**
     * 获取目标主机列表
     */
    private JSONArray getTargetHosts(Integer clusterId) {
        Object obj = CacheUtils.get(clusterId + UNDERLINE + SERVICE_ROLE_HOST_MAPPING);
        if (obj == null) {
            return null;
        }

        JSONObject parseObj = JSONUtil.parseObj(obj);
        JSONArray serviceRoleNodes = parseObj.getJSONArray(serviceRoleName);

        if (serviceRoleNodes == null) {
            System.out.println(ANSI_YELLOW + "⚠️ 未找到服务角色 " + serviceRoleName + " 的主机列表" + ANSI_RESET);
            return new JSONArray();
        }

        return serviceRoleNodes;
    }

    /**
     * 记录处理状态信息
     */
    private void logProcessingInfo(String hostName, boolean isLastExecution, int totalSize) {
        System.out.println(ANSI_CYAN + "ℹ️ 正在处理主机 " + hostName
                + (isLastExecution ? " (最后一个)" : "") + ANSI_RESET);
        System.out.println(ANSI_BLUE + "🔍 开始处理主机 " + hostName + " 的标签操作..." + ANSI_RESET);
    }

    /**
     * 检查节点是否有标签
     */
    private boolean hasLabel(Node node) {
        return node.getMetadata().getLabels() != null &&
                node.getMetadata().getLabels().containsKey(serviceRoleFullName);
    }

    /**
     * 同步标签，确保集群状态与预期一致
     */
    private void synchronizeLabels(KubernetesClient client, JSONArray targetHosts) {
        try {
            // 1. 获取所有应该有标签的主机名
            Set<String> expectedLabeledHosts = new HashSet<>();
            for (int i = 0; i < targetHosts.size(); i++) {
                expectedLabeledHosts.add(targetHosts.getStr(i));
            }
            System.out.println(ANSI_CYAN + "ℹ️ 期望的标签主机列表: " + expectedLabeledHosts + ANSI_RESET);

            // 2. 获取集群中的所有节点，进行全面检查
            NodeList allNodes = client.nodes().list();
            System.out.println(ANSI_BLUE + "🔍 正在全面检查集群中的 " + allNodes.getItems().size() + " 个节点..." + ANSI_RESET);

            // 3. 遍历所有节点，同步标签状态
            for (Node node : allNodes.getItems()) {
                String nodeName = node.getMetadata().getName();
                boolean hasLabel = hasLabel(node);
                boolean shouldHaveLabel = expectedLabeledHosts.contains(nodeName);

                // Case 1: 节点有标签，但不应该有 -> 移除标签
                if (hasLabel && !shouldHaveLabel) {
                    System.out.println(ANSI_YELLOW + "⚠️ 节点 " + nodeName + " 不应有标签，正在移除..." + ANSI_RESET);
                    cancelTag(nodeName, client);
                    System.out.println(ANSI_GREEN + "✅ 成功从节点 " + nodeName + " 移除标签" + ANSI_RESET);
                }
                // Case 2: 节点没有标签，但应该有 -> 添加标签
                else if (!hasLabel && shouldHaveLabel) {
                    System.out.println(ANSI_PURPLE + "🏷️ 节点 " + nodeName + " 缺少标签，正在添加..." + ANSI_RESET);
                    addTag(nodeName, client);
                    System.out.println(ANSI_GREEN + "✅ 成功为节点 " + nodeName + " 添加标签" + ANSI_RESET);
                }
            }
            System.out.println(ANSI_CYAN + "ℹ️ 所有节点检查完毕。" + ANSI_RESET);

        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ 标签同步过程中发生错误: " + e.getMessage() + ANSI_RESET);
            logger.error("Error during label synchronization: {}", e.getMessage(), e);
        }
    }

    /**
     * 添加标签
     */
    private void addTag(String hostName, KubernetesClient client) {
        System.out.println(ANSI_BLUE + "🔄 执行添加标签: " + hostName + " -> " + serviceRoleFullName + "=true" + ANSI_RESET);
        client.nodes().withName(hostName)
                .edit(r -> new NodeBuilder(r)
                        .editMetadata()
                        .removeFromLabels(serviceRoleFullName)
                        .addToLabels(serviceRoleFullName, "true")
                        .endMetadata()
                        .build());
    }

    /**
     * 取消标签
     */
    private void cancelTag(String hostname, KubernetesClient client) {
        System.out.println(ANSI_BLUE + "🔄 执行移除标签: " + hostname + " -> 移除 " + serviceRoleFullName + ANSI_RESET);
        client.nodes().withName(hostname)
                .edit(r -> new NodeBuilder(r)
                        .editMetadata()
                        .removeFromLabels(serviceRoleFullName)
                        .endMetadata()
                        .build());
    }
}