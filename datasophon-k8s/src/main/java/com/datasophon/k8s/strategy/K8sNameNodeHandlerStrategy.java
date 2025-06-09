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
            if (name.equals("dfs.namenode.rpc-address.nameservice1.nn1")) {
                // NameNode1 RPC地址使用StatefulSet的0号Pod
                StringBuilder newValue = new StringBuilder();
                newValue.append(NAMENODE_SERVICE).append("-0.")
                        .append(NAMENODE_SERVICE).append(":8020");
                config.setValue(newValue.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
            } else if (name.equals("dfs.namenode.rpc-address.nameservice1.nn2")) {
                // NameNode2 RPC地址使用StatefulSet的1号Pod
                StringBuilder newValue = new StringBuilder();
                newValue.append(NAMENODE_SERVICE).append("-1.")
                        .append(NAMENODE_SERVICE).append(":8020");
                config.setValue(newValue.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
            } else if (name.equals("dfs.namenode.http-address.nameservice1.nn1")) {
                // NameNode1 HTTP地址
                StringBuilder newValue = new StringBuilder();
                newValue.append(NAMENODE_SERVICE).append("-0.")
                        .append(NAMENODE_SERVICE).append(":9870");
                config.setValue(newValue.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
            } else if (name.equals("dfs.namenode.http-address.nameservice1.nn2")) {
                // NameNode2 HTTP地址
                StringBuilder newValue = new StringBuilder();
                newValue.append(NAMENODE_SERVICE).append("-1.")
                        .append(NAMENODE_SERVICE).append(":9870");
                config.setValue(newValue.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
            } else if (name.equals("dfs.journalnode.edits.dir")) {
                // JournalNode数据目录
                StringBuilder newValue = new StringBuilder();
                newValue.append("/data/journalnode/");
                config.setValue(newValue.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
            } else if (name.equals("dfs.namenode.shared.edits.dir")) {
                // NameNode共享编辑目录
                StringBuilder newValue = new StringBuilder();
                // 构建形如：qjournal://hdfs-journalnode-0.hdfs-journalnode:8485;hdfs-journalnode-1.hdfs-journalnode:8485;hdfs-journalnode-2.hdfs-journalnode:8485/nameservice1
                newValue.append("qjournal://");
                for (int i = 0; i < 3; i++) { // 假设3个JournalNode节点
                    if (i > 0) {
                        newValue.append(";");
                    }
                    newValue.append(JOURNALNODE_SERVICE).append("-").append(i)
                            .append(".").append(JOURNALNODE_SERVICE).append(":8485");
                }
                newValue.append("/nameservice1");
                config.setValue(newValue.toString());
                logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
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
                StringBuilder newValue = new StringBuilder();
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
            // 处理dfs.ha.namenode.id配置
            else if (name.equals("dfs.ha.namenode.id")) {
                // 对于NameNode，保持原配置不变，因为在启动脚本中动态确定
                // 对于ZKFC，在启动脚本中动态确定
                if ("NameNode".equals(serviceRoleName)) {
                    // 确保该配置存在，但不修改值，由启动脚本根据Pod索引决定
                    if (value == null || String.valueOf(value).isEmpty()) {
                        // 默认值，会在启动脚本中被替换
                        config.setValue("to_be_determined_by_pod");
                        logger.info("添加默认配置 {}: {}", name, config.getValue());
                    }
                } else if ("ZKFC".equals(serviceRoleName)) {
                    // 确保该配置存在，但不修改值，由启动脚本根据Pod索引决定
                    if (value == null || String.valueOf(value).isEmpty()) {
                        // 默认值，会在启动脚本中被替换
                        config.setValue("to_be_determined_by_pod");
                        logger.info("添加默认配置 {}: {}", name, config.getValue());
                    }
                }
            }
        }

        logger.info("HDFS NameNode配置更新完成，已适配Kubernetes服务");
    }
}
