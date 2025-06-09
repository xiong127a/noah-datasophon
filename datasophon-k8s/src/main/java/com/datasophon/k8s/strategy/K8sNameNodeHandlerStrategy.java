package com.datasophon.k8s.strategy;

import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sKerberosUtils;
import com.datasophon.k8s.util.K8sMinaUtils;

import java.io.IOException;
import java.util.List;

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
        if (list == null || list.isEmpty()) {
            logger.warn("配置列表为空，无法更新服务配置");
            return;
        }

        logger.info("开始更新HDFS NameNode配置，适配Kubernetes服务...");

        // 定义服务名常量
        final String NAMENODE_SERVICE = "hdfs-namenode";
        final String JOURNALNODE_SERVICE = "hdfs-journalnode";
        final String ZOOKEEPER_SERVICE = "zookeeper";
        final String DATANODE_SERVICE = "hdfs-datanode";

        // 遍历所有配置
        for (ServiceConfig config : list) {
            String name = config.getName();
            String value = config.getValue() != null ? config.getValue().toString() : null;

            if (value == null || name == null) {
                continue;
            }

            // 处理NameNode RPC地址
            if (name.contains("dfs.namenode.rpc-address")) {
                if (name.contains(".nn1")) {
                    config.setValue(NAMENODE_SERVICE + "-0." + NAMENODE_SERVICE + ":8020");
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                } else if (name.contains(".nn2")) {
                    config.setValue(NAMENODE_SERVICE + "-1." + NAMENODE_SERVICE + ":8020");
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                }
            }

            // 处理NameNode HTTP地址
            else if (name.contains("dfs.namenode.http-address")) {
                if (name.contains(".nn1")) {
                    config.setValue(NAMENODE_SERVICE + "-0." + NAMENODE_SERVICE + ":9870");
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                } else if (name.contains(".nn2")) {
                    config.setValue(NAMENODE_SERVICE + "-1." + NAMENODE_SERVICE + ":9870");
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                }
            }

            // 处理JournalNode共享编辑日志目录
            else if (name.equals("dfs.namenode.shared.edits.dir") && value.startsWith("qjournal://")) {
                // 解析原始qjournal URL
                String[] parts = value.split("://");
                if (parts.length == 2) {
                    String protocol = parts[0]; // qjournal
                    String[] hostParts = parts[1].split("/");
                    if (hostParts.length >= 2) {
                        String hosts = hostParts[0]; // host1:8485;host2:8485;host3:8485
                        String journalId = hostParts[1]; // meta

                        // 构建新的服务地址列表
                        StringBuilder newHosts = new StringBuilder();
                        for (int i = 0; i < 3; i++) { // 假设3个JournalNode
                            if (i > 0) {
                                newHosts.append(";");
                            }
                            newHosts.append(JOURNALNODE_SERVICE).append("-").append(i)
                                    .append(".").append(JOURNALNODE_SERVICE).append(":8485");
                        }

                        // 重建qjournal URL
                        String newValue = protocol + "://" + newHosts.toString() + "/" + journalId;
                        config.setValue(newValue);
                        logger.info("更新配置 {}: {} -> {}", name, value, newValue);
                    }
                }
            }

            // 处理ZooKeeper地址 - ha.zookeeper.quorum
            else if (name.equals("ha.zookeeper.quorum")) {
                // 构建ZooKeeper服务地址列表
                StringBuilder zkServers = new StringBuilder();
                for (int i = 0; i < 3; i++) { // 假设3个ZooKeeper节点
                    if (i > 0) {
                        zkServers.append(",");
                    }
                    zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                            .append(".").append(ZOOKEEPER_SERVICE).append(":2181");
                }
                config.setValue(zkServers.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
            }

            // 处理ZooKeeper地址 - hadoop.zk.address
            else if (name.equals("hadoop.zk.address")) {
                // 构建ZooKeeper服务地址列表
                StringBuilder zkServers = new StringBuilder();
                for (int i = 0; i < 3; i++) { // 假设3个ZooKeeper节点
                    if (i > 0) {
                        zkServers.append(",");
                    }
                    zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                            .append(".").append(ZOOKEEPER_SERVICE).append(":2181");
                }
                config.setValue(zkServers.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
            }

            // 处理DataNode数据传输地址
            else if (name.equals("dfs.datanode.address")) {
                // DataNode数据传输地址使用StatefulSet的服务名
                StringBuilder newValue = new StringBuilder();
                // 假设使用无状态的Deployment，使用服务名
                newValue.append(DATANODE_SERVICE).append(":1026");
                config.setValue(newValue.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
            }

            // 处理DataNode HTTP地址
            else if (name.equals("dfs.datanode.http.address")) {
                // DataNode HTTP地址使用StatefulSet的服务名
                StringBuilder newValue = new StringBuilder();
                // 假设使用无状态的Deployment，使用服务名
                newValue.append(DATANODE_SERVICE).append(":1025");
                config.setValue(newValue.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
            }
        }

        logger.info("HDFS NameNode配置更新完成，已适配Kubernetes服务");
    }
}
