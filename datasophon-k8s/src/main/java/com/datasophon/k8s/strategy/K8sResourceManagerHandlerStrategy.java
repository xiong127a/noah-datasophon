package com.datasophon.k8s.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sKerberosUtils;
import com.datasophon.k8s.util.K8sMinaUtils;
import com.datasophon.common.model.ServiceConfig;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class K8sResourceManagerHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sResourceManagerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command)
            throws SQLException, ClassNotFoundException, IOException {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String hostname = command.getHostname();
        String jobCmd = "";
        if (command.getEnableKerberos()) {
            logger.info("start to get resourcemanager keytab file");
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/rm.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "rm/" + hostname, "rm.service.keytab");
            }
        }
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            // 存在 tez 则创建软连接
            final String tezHomePath = Constants.INSTALL_PATH + Constants.SLASH + "tez";
            // if (K8sMinaUtils.checkPathExists(hostname, tezHomePath)) {
            // K8sMinaUtils.execCmdWithResult(hostname,
            // "ln -s " + tezHomePath + "/conf/tez-site.xml " + workPath +
            // "/etc/hadoop/tez-site.xml");
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

        logger.info("开始更新YARN ResourceManager配置，适配Kubernetes服务...");

        // 定义服务名常量
        final String RESOURCEMANAGER_SERVICE = "yarn-resourcemanager";
        final String HISTORYSERVER_SERVICE = "yarn-historyserver";
        final String TIMELINESERVER_SERVICE = "yarn-timelineserver";
        final String ZOOKEEPER_SERVICE = "zookeeper-zkserver";
        // 命名空间
        final String NAMESPACE = "datasophon";
        // 集群域名后缀
        final String CLUSTER_DOMAIN = "svc.cluster.local";

        // 遍历所有配置
        for (ServiceConfig config : list) {
            String name = config.getName();
            Object value = config.getValue();

            // 处理ResourceManager相关配置
            switch (name) {
                case "yarn.resourcemanager.hostname.rm1": {
                    // ResourceManager1主机名使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN;
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.hostname.rm2": {
                    // ResourceManager2主机名使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN;
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.address.rm1": {
                    // ResourceManager1地址使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8032";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.address.rm2": {
                    // ResourceManager2地址使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8032";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.scheduler.address.rm1": {
                    // ResourceManager1调度器地址使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8030";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.scheduler.address.rm2": {
                    // ResourceManager2调度器地址使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8030";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.resource-tracker.address.rm1": {
                    // ResourceManager1 资源跟踪地址
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8031";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.resource-tracker.address.rm2": {
                    // ResourceManager2 资源跟踪地址
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8031";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.webapp.address.rm1": {
                    // ResourceManager1 Web地址
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8088";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.webapp.address.rm2": {
                    // ResourceManager2 Web地址
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8088";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.resourcemanager.zk-address": {
                    // ZooKeeper地址列表
                    StringBuilder zkServers = new StringBuilder();
                    for (int i = 0; i < 3; i++) { // 假设3个ZooKeeper节点
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
                case "yarn.log.server.url": {
                    // HistoryServer日志服务URL
                    String newValue = "http://" + HISTORYSERVER_SERVICE + "-0." + HISTORYSERVER_SERVICE + "."
                            + NAMESPACE
                            + "." + CLUSTER_DOMAIN + ":19888/jobhistory/logs";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.timeline-service.hostname": {
                    // TimelineServer主机名
                    String newValue = TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN;
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.timeline-service.address": {
                    // TimelineServer地址
                    String newValue = TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":10200";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.timeline-service.webapp.address": {
                    // TimelineServer Web地址
                    String newValue = TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8188";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.timeline-service.webapp.https.address": {
                    // TimelineServer HTTPS Web地址
                    String newValue = TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN + ":8190";
                    config.setValue(newValue);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.timeline-service.bind-host": {
                    // TimelineServer绑定主机
                    config.setValue(TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
                case "yarn.timeline-service.address.application.history.bind-host": {
                    // ApplicationHistoryService绑定主机
                    config.setValue(TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + NAMESPACE + "."
                            + CLUSTER_DOMAIN);
                    logger.info("更新配置 {}: {} -> {}", name, value, config.getValue());
                    break;
                }
            }
        }

        logger.info("YARN ResourceManager配置更新完成，已适配Kubernetes服务");
    }
}
