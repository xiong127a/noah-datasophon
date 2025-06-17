package com.datasophon.k8s.strategy;

import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sKerberosUtils;
import com.datasophon.k8s.util.K8sMinaUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.datasophon.common.Constants.UNDERLINE;

public class K8sNameNodeHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sNameNodeHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws IOException {
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        String hostname = command.getHostname();
        ExecResult execResult = new ExecResult();

        if (command.getEnableKerberos()) {
            logger.info("Start to get namenode keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/nn.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "nn/" + hostname, "nn.service.keytab");
            }
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/spnego.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "HTTP/" + hostname, "spnego.service.keytab");
            }
        }

        if (command.getEnableRangerPlugin()) {
            // logger.info("Start to enable ranger hdfs plugin");
            // ArrayList<String> commands = new ArrayList<>();
            // commands.add("sh");
            // commands.add(workPath + "/ranger-hdfs-plugin/enable-hdfs-plugin.sh");
            // if (!FileUtil.exist(workPath + "/ranger-hdfs-plugin/success.id")) {
            // ExecResult execResult = ShellUtils.execWithStatus(workPath +
            // "/ranger-hdfs-plugin", commands, 30L, logger);
            // if (execResult.getExecResult()) {
            // logger.info("Enable ranger hdfs plugin success");
            // // 写入ranger plugin集成成功标识
            // FileUtil.writeUtf8String("success", workPath +
            // "/ranger-hdfs-plugin/success.id");
            // } else {
            // logger.info("Enable ranger hdfs plugin failed");
            // return execResult;
            // }
            // }
        }

        return serviceHandler.start(command);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        if (list.isEmpty()) {
            return;
        }

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

        logger.info("开始更新HDFS NameNode配置，适配Kubernetes服务...");

        // 定义服务名常量
        final String NAMENODE_SERVICE = "hdfs-namenode";
        final String JOURNALNODE_SERVICE = "hdfs-journalnode";
        final String ZOOKEEPER_SERVICE = "zookeeper-zkserver";
        final String DATANODE_SERVICE = "hdfs-datanode";


        // 当前服务角色名称
        String serviceRoleName = "";
        for (ServiceConfig config : list) {
            if ("dfs.namenode.name.dir".equals(config.getName())) {
                serviceRoleName = config.getConfigTargetRoles();
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
                    // NameNode1 RPC地址使用完整的FQDN格式
                    String newValue = NAMENODE_SERVICE + "-0." + NAMENODE_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8020";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "dfs.namenode.rpc-address.nameservice1.nn2": {
                    // NameNode2 RPC地址使用完整的FQDN格式
                    String newValue = NAMENODE_SERVICE + "-1." + NAMENODE_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8020";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "dfs.namenode.http-address.nameservice1.nn1": {
                    // NameNode1 HTTP地址使用完整的FQDN格式
                    String newValue = NAMENODE_SERVICE + "-0." + NAMENODE_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":9870";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "dfs.namenode.http-address.nameservice1.nn2": {
                    // NameNode2 HTTP地址使用完整的FQDN格式
                    String newValue = NAMENODE_SERVICE + "-1." + NAMENODE_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":9870";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "dfs.namenode.shared.edits.dir": {
                    // NameNode共享编辑目录，使用完整的FQDN格式以确保Java DNS解析
                    StringBuilder newValue = new StringBuilder();
                    // 构建形如：qjournal://hdfs-journalnode-0.hdfs-journalnode.datasophon.svc.cluster.local:8485;...
                    newValue.append("qjournal://");
                    for (int i = 0; i < 3; i++) { // 假设3个JournalNode节点
                        if (i > 0) {
                            newValue.append(";");
                        }
                        newValue.append(JOURNALNODE_SERVICE).append("-").append(i)
                                .append(".").append(JOURNALNODE_SERVICE).append(".")
                                .append(NAMESPACE).append(".")
                                .append(CLUSTER_DOMAIN).append(":8485");
                    }
                    newValue.append("/nameservice1");
                    config.setValue(newValue.toString());
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                // 处理ZooKeeper地址 - ha.zookeeper.quorum
                case "ha.zookeeper.quorum": {
                    // 构建ZooKeeper服务地址列表，使用完整的FQDN格式
                    StringBuilder zkServers = new StringBuilder();
                    for (int i = 0; i < zkNodeCount; i++) {
                        if (i > 0) {
                            zkServers.append(",");
                        }
                        zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                                .append(".").append(ZOOKEEPER_SERVICE).append(".")
                                .append(NAMESPACE).append(".")
                                .append(CLUSTER_DOMAIN).append(":2181");
                    }
                    config.setValue(zkServers.toString());
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                // 处理ZooKeeper地址 - hadoop.zk.address
                case "hadoop.zk.address": {
                    // 构建ZooKeeper服务地址列表，使用完整的FQDN格式
                    StringBuilder zkServers = new StringBuilder();
                    for (int i = 0; i < zkNodeCount; i++) {
                        if (i > 0) {
                            zkServers.append(",");
                        }
                        zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                                .append(".").append(ZOOKEEPER_SERVICE).append(".")
                                .append(NAMESPACE).append(".")
                                .append(CLUSTER_DOMAIN).append(":2181");
                    }
                    config.setValue(zkServers.toString());
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                // 处理DataNode数据传输地址 - 使用完整的FQDN格式
                case "dfs.datanode.address": {
                    // 使用DataNode服务的完整FQDN
                    String newValue = DATANODE_SERVICE + "." + NAMESPACE + "." + CLUSTER_DOMAIN + ":1026";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                // 处理DataNode HTTP地址 - 使用完整的FQDN格式
                case "dfs.datanode.http.address": {
                    // 使用DataNode服务的完整FQDN
                    String newValue = DATANODE_SERVICE + "." + NAMESPACE + "." + CLUSTER_DOMAIN + ":1025";
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

        logger.info("HDFS NameNode配置更新完成，已适配Kubernetes服务，所有服务地址均使用完整FQDN格式");
    }
}
