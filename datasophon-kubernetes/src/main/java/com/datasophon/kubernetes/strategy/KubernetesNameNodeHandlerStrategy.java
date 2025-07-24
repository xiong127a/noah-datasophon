package com.datasophon.kubernetes.strategy;

import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubernetesKerberosUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.datasophon.common.Constants.SERVICE_ROLE_HOST_MAPPING;
import static com.datasophon.common.Constants.UNDERLINE;

public class KubernetesNameNodeHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesNameNodeHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws IOException {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        String hostname = command.getHostname();

        if (command.getEnableKerberos()) {
            logger.info("Start to get namenode keytab file");
            KubernetesKerberosUtils.createKeytabDir(hostname);
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/nn.service.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname, "nn/" + hostname, "nn.service.keytab");
            }
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/spnego.service.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname, "HTTP/" + hostname, "spnego.service.keytab");
            }
        }
        return serviceHandler.start(command);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        if (list.isEmpty()) {
            return;
        }
        String namespace = getKubernetesNamespace(clusterId);
        // 动态获取ZK节点数量 - 直接从缓存获取，不再使用备用逻辑
        int zkNodeCount = 0;
        String zkNodeCountKey = clusterId + UNDERLINE + "zookeeper_node_count";
        Object zkCountObj = CacheUtils.get(zkNodeCountKey);

        if (Objects.nonNull(zkCountObj)) {
            zkNodeCount = (Integer) zkCountObj;
            logger.info("从缓存 zookeeper_node_count 中获取到ZK节点数量为: {}", zkNodeCount);
        } else {
            logger.warn("缓存中未找到 ZK 节点数 (key: {}), ZK quorum 将为空。", zkNodeCountKey);
        }

        // 动态获取JournalNode节点数量
        int jnNodeCount = 0;
        final String serviceRoleHostMappingKey = clusterId + UNDERLINE + SERVICE_ROLE_HOST_MAPPING;
        Object mappingObj = CacheUtils.get(serviceRoleHostMappingKey);
        if (Objects.nonNull(mappingObj)) {
            JSONObject mapping = JSONUtil.parseObj(mappingObj);
            String roleName = "JournalNode";
            if (mapping.containsKey(roleName)) {
                jnNodeCount = mapping.getJSONArray(roleName).size();
                logger.info("从 {} 中获取到 {} 节点数量为: {}", serviceRoleHostMappingKey, roleName, jnNodeCount);
            } else {
                logger.warn("在 {} 中未找到 {} 角色", serviceRoleHostMappingKey, roleName);
            }
        } else {
            logger.warn("缓存中未找到 {}", serviceRoleHostMappingKey);
        }

        logger.info("开始更新HDFS NameNode配置，适配Kubernetes服务...");

        // 定义服务名常量
        final String NAMENODE_SERVICE = "hdfs-namenode";
        final String JOURNALNODE_SERVICE = "hdfs-journalnode";
        final String ZOOKEEPER_SERVICE = "zookeeper-zkserver";
        final String DATANODE_SERVICE = "hdfs-datanode";

        // 当前服务角色名称
        for (ServiceConfig config : list) {
            if ("dfs.namenode.name.dir".equals(config.getName())) {
                break;
            }
        }

        // 遍历所有配置
        for (ServiceConfig config : list) {
            String name = config.getName();
            Object value = config.getValue();

            // 处理NameNode HA相关配置
            switch (name) {
                case "dfs.namenode.rpc-address.nameservice1.nn1": {
                    // NameNode1 RPC地址使用pod-0的FQDN格式
                    String newValue = NAMENODE_SERVICE + "-0." + NAMENODE_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8020";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "dfs.namenode.rpc-address.nameservice1.nn2": {
                    // NameNode2 RPC地址使用pod-1的FQDN格式
                    String newValue = NAMENODE_SERVICE + "-1." + NAMENODE_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8020";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "dfs.namenode.http-address.nameservice1.nn1": {
                    // NameNode1 HTTP地址使用pod-0的FQDN格式
                    String newValue = NAMENODE_SERVICE + "-0." + NAMENODE_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":9870";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "dfs.namenode.http-address.nameservice1.nn2": {
                    // NameNode2 HTTP地址使用pod-1的FQDN格式
                    String newValue = NAMENODE_SERVICE + "-1." + NAMENODE_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":9870";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "dfs.namenode.shared.edits.dir": {
                    // NameNode共享编辑目录，使用JournalNode各个Pod的FQDN列表
                    StringBuilder newValue = new StringBuilder("qjournal://");
                    // 使用动态获取的JournalNode节点数量
                    for (int i = 0; i < jnNodeCount; i++) {
                        if (i > 0) {
                            newValue.append(";");
                        }
                        newValue.append(JOURNALNODE_SERVICE).append("-").append(i)
                                .append(".").append(JOURNALNODE_SERVICE).append(".")
                                .append(namespace).append(".")
                                .append(CLUSTER_DOMAIN).append(":8485");
                    }
                    newValue.append("/nameservice1");
                    config.setValue(newValue.toString());
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                // 处理ZooKeeper地址 - ha.zookeeper.quorum
                case "ha.zookeeper.quorum": {
                    // 构建ZooKeeper服务地址列表，使用各个Pod的FQDN
                    StringBuilder zkServers = new StringBuilder();
                    for (int i = 0; i < zkNodeCount; i++) {
                        if (i > 0) {
                            zkServers.append(",");
                        }
                        zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                                .append(".").append(ZOOKEEPER_SERVICE).append(".")
                                .append(namespace).append(".")
                                .append(CLUSTER_DOMAIN).append(":2181");
                    }
                    config.setValue(zkServers.toString());
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                // 处理ZooKeeper地址 - hadoop.zk.address
                case "hadoop.zk.address": {
                    // 构建ZooKeeper服务地址列表，使用各个Pod的FQDN
                    StringBuilder zkServers = new StringBuilder();
                    for (int i = 0; i < zkNodeCount; i++) {
                        if (i > 0) {
                            zkServers.append(",");
                        }
                        zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                                .append(".").append(ZOOKEEPER_SERVICE).append(".")
                                .append(namespace).append(".")
                                .append(CLUSTER_DOMAIN).append(":2181");
                    }
                    config.setValue(zkServers.toString());
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                // 处理DataNode数据传输地址 - 使用完整的FQDN格式
                case "dfs.datanode.address": {
                    // 使用DataNode服务的完整FQDN
                    String newValue = DATANODE_SERVICE + "." + namespace + "." + CLUSTER_DOMAIN + ":1026";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                // 处理DataNode HTTP地址 - 使用完整的FQDN格式
                case "dfs.datanode.http.address": {
                    // 使用DataNode服务的完整FQDN
                    String newValue = DATANODE_SERVICE + "." + namespace + "." + CLUSTER_DOMAIN + ":1025";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "dfs.nameservices": {
                    String clusterName = CacheUtils.getString("cluster_name");
                    config.setValue(clusterName);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
            }
        }

        logger.info("HDFS NameNode配置更新完成，已适配Kubernetes服务，所有服务地址均使用独立的Pod FQDN格式");
    }
}
