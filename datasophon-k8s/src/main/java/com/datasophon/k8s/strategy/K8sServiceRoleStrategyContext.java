package com.datasophon.k8s.strategy;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class K8sServiceRoleStrategyContext {

    private static final Map<String, K8sServiceRoleStrategy> map = new ConcurrentHashMap<>();

    static {
        map.put("ZkServer", new K8sZkServerHandlerStrategy("ZOOKEEPER", "ZkServer"));
        map.put("NameNode", new K8sNameNodeHandlerStrategy("HDFS", "NameNode"));
        map.put("ZKFC", new K8sZKFCHandlerStrategy("HDFS", "ZKFC"));
        map.put("JournalNode", new K8sJournalNodeHandlerStrategy("HDFS", "JournalNode"));
        map.put("DataNode", new K8sDataNodeHandlerStrategy("HDFS", "DataNode"));
        map.put("ResourceManager", new K8sResourceManagerHandlerStrategy("YARN", "ResourceManager"));
        map.put("NodeManager", new K8sNodeManagerHandlerStrategy("YARN", "NodeManager"));
        map.put("HistoryServer", new K8sHistoryServerHandlerStrategy("YARN", "HistoryServer"));
        map.put("TimelineServer", new K8sTimelineServerHandlerStrategy("YARN", "TimelineServer"));
        map.put("HiveServer2", new K8sHiveServer2HandlerStrategy("HIVE", "HiveServer2"));
        map.put("HbaseMaster", new K8sHbaseHandlerStrategy("HBASE", "HbaseMaster"));
        map.put("RegionServer", new K8sHbaseHandlerStrategy("HBASE", "RegionServer"));
        map.put("KafkaBroker", new K8sKafkaHandlerStrategy("KAFKA", "KafkaBroker"));
        map.put("RangerAdmin", new K8sRangerAdminHandlerStrategy("RANGER", "RangerAdmin"));
        map.put("RangerUsersync", new K8sRangerAdminHandlerStrategy("RANGER", "RangerUsersync"));
        map.put("RangerKms", new K8sRangerAdminHandlerStrategy("RANGER", "RangerKms"));
        map.put("OpenldapServer", new k8sOpenldapHandlerStrategy("OPENLDAP", "OpenldapServer"));
        map.put("HueMaster", new K8sHueHandlerStrategy("HUE", "HueMaster"));
    }

    public static K8sServiceRoleStrategy getServiceRoleHandler(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        return map.get(type);
    }
}
