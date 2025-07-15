package com.datasophon.kubernetes.strategy;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KubernetesServiceRoleStrategyContext {

    private static final Map<String, KubernetesServiceRoleStrategy> map = new ConcurrentHashMap<>();

    static {
        map.put("ZkServer", new KubernetesZkServerHandlerStrategy("ZOOKEEPER", "ZkServer"));
        map.put("NameNode", new KubernetesNameNodeHandlerStrategy("HDFS", "NameNode"));
        map.put("ZKFC", new KubernetesZKFCHandlerStrategy("HDFS", "ZKFC"));
        map.put("JournalNode", new KubernetesJournalNodeHandlerStrategy("HDFS", "JournalNode"));
        map.put("DataNode", new KubernetesDataNodeHandlerStrategy("HDFS", "DataNode"));
        map.put("ResourceManager", new KubernetesResourceManagerHandlerStrategy("YARN", "ResourceManager"));
        map.put("NodeManager", new KubernetesNodeManagerHandlerStrategy("YARN", "NodeManager"));
        map.put("HistoryServer", new KubernetesHistoryServerHandlerStrategy("YARN", "HistoryServer"));
        map.put("TimelineServer", new KubernetesTimelineServerHandlerStrategy("YARN", "TimelineServer"));
        map.put("HiveServer2", new KubernetesHiveServer2HandlerStrategy("HIVE", "HiveServer2"));
        map.put("HbaseMaster", new KubernetesHbaseHandlerStrategy("HBASE", "HbaseMaster"));
        map.put("RegionServer", new KubernetesHbaseHandlerStrategy("HBASE", "RegionServer"));
        map.put("KafkaBroker", new KubernetesKafkaHandlerStrategy("KAFKA", "KafkaBroker"));
        map.put("RangerAdmin", new KubernetesRangerAdminHandlerStrategy("RANGER", "RangerAdmin"));
        map.put("RangerUsersync", new KubernetesRangerAdminHandlerStrategy("RANGER", "RangerUsersync"));
        map.put("RangerKms", new KubernetesRangerAdminHandlerStrategy("RANGER", "RangerKms"));
        map.put("Grafana", new KubernetesGrafanaHandlerStrategy("GRAFANA", "Grafana"));

        map.put("OpenldapServer", new kubernetesOpenldapHandlerStrategy("OPENLDAP", "OpenldapServer"));

        map.put("HueMaster", new KubernetesHueHandlerStrategy("HUE", "HueMaster"));

        map.put("RedisWorker", new KubernetesRedisHandlerStrategy("REDIS", "RedisWorker"));

        map.put("PostgresqlMaster", new KubernetesPostgresqlHandlerStrategy("POSTGRESQL", "PostgresqlMaster"));
        map.put("PostgresqlWorker", new KubernetesPostgresqlHandlerStrategy("POSTGRESQL", "PostgresqlWorker"));

        map.put("SRBE", new KubernetesSRBEHandlerStrategy("STARROCKS", "SRBE"));
        map.put("SRCN", new KubernetesSRCNHandlerStrategy("STARROCKS", "SRCN"));
        map.put("SRFE", new KubernetesSRFEHandlerStrategy("STARROCKS", "SRFE"));
        map.put("SRFEObserver", new KubernetesSRFEObserverHandlerStrategy("STARROCKS", "SRFEObserver"));

        map.put("Storage", new KubernetesStorageHandlerStrategy("NEBULAGRAPH", "Storage"));
        map.put("TezServer", new KubernetesTezServerHandlerStrategy("TEZ", "TezServer"));

        // TODO 添加其他组件
        map.put("ZOOKEEPER", new KubernetesZkServerHandlerStrategy("ZOOKEEPER", "ZkServer"));
        map.put("HDFS", new KubernetesNameNodeHandlerStrategy("HDFS", "NameNode"));
        map.put("YARN", new KubernetesResourceManagerHandlerStrategy("YARN", "ResourceManager"));
        map.put("KAFKA", new KubernetesKafkaHandlerStrategy("KAFKA", "KafkaBroker"));
        map.put("HIVE", new KubernetesHiveServer2HandlerStrategy("HIVE", "HiveServer2"));
        map.put("HBASE", new KubernetesHbaseHandlerStrategy("HBASE", "HbaseMaster"));
        map.put("TRINO", new KubernetesTrinoHandlerStrategy("TRINO", "TrinoCoordinator"));
        map.put("GRAFANA", new KubernetesGrafanaHandlerStrategy("GRAFANA", "Grafana"));
        map.put("REDIS", new KubernetesRedisHandlerStrategy("REDIS", "RedisMaster"));
        map.put("STARROCKS", new KubernetesSRFEHandlerStrategy("STARROCKS", "SRFE"));
        map.put("FlinkOperator", new KubernetesFlinkOperatorHandlerStrategy("FlinkOperator", "FlinkOperator"));
    }

    public static KubernetesServiceRoleStrategy getServiceRoleHandler(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        return map.get(type);
    }
}
