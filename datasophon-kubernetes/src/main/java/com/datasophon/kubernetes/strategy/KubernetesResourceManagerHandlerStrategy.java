package com.datasophon.kubernetes.strategy;

import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubernetesKerberosUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.datasophon.common.Constants.UNDERLINE;

public class KubernetesResourceManagerHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesResourceManagerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command)
            throws SQLException, ClassNotFoundException, IOException {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        String hostname = command.getHostname();
        if (command.getEnableKerberos()) {
            logger.info("start to get resourcemanager keytab file");
            KubernetesKerberosUtils.createKeytabDir(hostname);
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/rm.service.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname, "rm/" + hostname, "rm.service.keytab");
            }
        }
        return serviceHandler.start(command);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("配置列表为空，无法更新服务配置");
            return;
        }

        // 动态获取ZK节点数量
        int zkNodeCount = 0;
        String zkNodeCountKey = clusterId + UNDERLINE + "zookeeper_node_count";
        Object zkCountObj = CacheUtils.get(zkNodeCountKey);

        if (Objects.nonNull(zkCountObj)) {
            zkNodeCount = (Integer) zkCountObj;
            logger.info("从缓存 zookeeper_node_count 中获取到ZK节点数量为: {}", zkNodeCount);
        } else {
            logger.warn("缓存中未找到 ZK 节点数 (key: {}), ZK quorum 将为空。", zkNodeCountKey);
        }

        logger.info("开始更新YARN ResourceManager配置，适配Kubernetes服务...");

        // 定义服务名常量
        final String RESOURCEMANAGER_SERVICE = "yarn-resourcemanager";
        final String HISTORYSERVER_SERVICE = "yarn-historyserver";
        final String TIMELINESERVER_SERVICE = "yarn-timelineserver";
        final String ZOOKEEPER_SERVICE = "zookeeper-zkserver";

        // 遍历所有配置
        for (ServiceConfig config : list) {
            String name = config.getName();
            String namespace = getKubernetesNamespace(clusterId);
            // 处理ResourceManager相关配置
            switch (name) {
                case "yarn.resourcemanager.hostname.rm1": {
                    // ResourceManager1主机名使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN;
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.hostname.rm2": {
                    // ResourceManager2主机名使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN;
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.address.rm1": {
                    // ResourceManager1地址使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8032";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.address.rm2": {
                    // ResourceManager2地址使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8032";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.scheduler.address.rm1": {
                    // ResourceManager1调度器地址使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8030";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.scheduler.address.rm2": {
                    // ResourceManager2调度器地址使用完整的FQDN格式
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8030";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.resource-tracker.address.rm1": {
                    // ResourceManager1 资源跟踪地址
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8031";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.resource-tracker.address.rm2": {
                    // ResourceManager2 资源跟踪地址
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8031";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.webapp.address.rm1": {
                    // ResourceManager1 Web地址
                    String newValue = RESOURCEMANAGER_SERVICE + "-0." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8088";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.webapp.address.rm2": {
                    // ResourceManager2 Web地址
                    String newValue = RESOURCEMANAGER_SERVICE + "-1." + RESOURCEMANAGER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8088";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.resourcemanager.zk-address": {
                    // ZooKeeper地址列表
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
                    break;
                }
                case "mapreduce.jobhistory.address": {
                    // JobHistoryServer RPC地址
                    String newValue = HISTORYSERVER_SERVICE + "-0." + HISTORYSERVER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":10020";
                    config.setValue(newValue);
                    break;
                }
                case "mapreduce.jobhistory.webapp.address": {
                    // JobHistoryServer Web UI地址
                    String newValue = HISTORYSERVER_SERVICE + "-0." + HISTORYSERVER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":19888";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.log.server.url": {
                    // HistoryServer日志服务URL
                    String newValue = "http://" + HISTORYSERVER_SERVICE + "-0." + HISTORYSERVER_SERVICE + "."
                            + namespace
                            + "." + CLUSTER_DOMAIN + ":19888/jobhistory/logs";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.timeline-service.hostname": {
                    // TimelineServer主机名
                    String newValue = TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN;
                    config.setValue(newValue);
                    break;
                }
                case "yarn.timeline-service.address": {
                    // TimelineServer地址
                    String newValue = TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":10200";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.timeline-service.webapp.address": {
                    // TimelineServer Web地址
                    String newValue = TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8188";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.timeline-service.webapp.https.address": {
                    // TimelineServer HTTPS Web地址
                    String newValue = TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN + ":8190";
                    config.setValue(newValue);
                    break;
                }
                case "yarn.timeline-service.bind-host": {
                    // TimelineServer绑定主机
                    config.setValue(TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN);
                    break;
                }
                case "yarn.timeline-service.address.application.history.bind-host": {
                    // ApplicationHistoryService绑定主机
                    config.setValue(TIMELINESERVER_SERVICE + "-0." + TIMELINESERVER_SERVICE + "." + namespace + "."
                            + CLUSTER_DOMAIN);
                    break;
                }
            }
        }

        logger.info("YARN ResourceManager配置更新完成，已适配Kubernetes服务");
    }
}
