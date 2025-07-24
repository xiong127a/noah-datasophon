package com.datasophon.kubernetes.strategy;

import cn.hutool.core.util.StrUtil;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.datasophon.common.Constants.UNDERLINE;

public class KubernetesHiveServer2HandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesHiveServer2HandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws IOException {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        // 旧的、手动的HDFS目录检查和Kerberos处理逻辑在此处被移除，
        // 因为这些功能现在由initContainer和更通用的Kubernetes配置流程处理。
        return serviceHandler.start(command);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        if (Objects.isNull(list) || list.isEmpty()) {
            logger.warn("配置列表为空，无法更新HiveMetaStore服务地址");
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

        // 动态获取HiveMetaStore节点数量
        int metaNodeCount = getRoleInstallCount(clusterId, "HiveMetaStore");

        // 构建Metastore URIs
        final String METASTORE_SERVICE = "hive-hivemetastore";
        StringBuilder metastoreUris = new StringBuilder();
        for (int i = 0; i < metaNodeCount; i++) {
            if (i > 0) {
                metastoreUris.append(",");
            }
            metastoreUris.append("thrift://")
                    .append(METASTORE_SERVICE).append("-").append(i)
                    .append(".").append(METASTORE_SERVICE).append(".")
                    .append(namespace).append(".")
                    .append(CLUSTER_DOMAIN).append(":9083");
        }
        
        logger.info("开始更新Hive配置，适配Kubernetes服务...");

        for (ServiceConfig config : list) {
            if ("hive.metastore.uris".equals(config.getName())) {
                String finalUris = StrUtil.isNotBlank(metastoreUris) ? metastoreUris.toString() : "";
                logger.info("检测到hive.metastore.uris，将值从 {} 更新为 Kubernetes 服务地址 {}", config.getValue(), finalUris);
                config.setValue(finalUris);
            } else if ("hive.zookeeper.quorum".equals(config.getName())) {
                final String ZOOKEEPER_SERVICE = "zookeeper-zkserver";
                StringBuilder zkServers = new StringBuilder();
                for (int i = 0; i < zkNodeCount; i++) {
                    if (i > 0) {
                        zkServers.append(",");
                    }
                    zkServers.append(ZOOKEEPER_SERVICE).append("-").append(i)
                            .append(".").append(ZOOKEEPER_SERVICE).append(".")
                            .append(namespace).append(".")
                            .append(CLUSTER_DOMAIN);
                }
                logger.info("检测到hive.zookeeper.quorum，将值从 {} 更新为 Kubernetes 服务地址 {}", config.getValue(), zkServers);
                config.setValue(zkServers.toString());
            }
        }
        logger.info("Hive配置更新完成，已适配Kubernetes服务发现");
    }
}
