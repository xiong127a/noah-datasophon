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
    }

    public static K8sServiceRoleStrategy getServiceRoleHandler(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        return map.get(type);
    }
}
