package com.datasophon.k8s.strategy;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class K8sServiceRoleStrategyContext {

    private static final Map<String, K8sServiceRoleStrategy> map = new ConcurrentHashMap<>();

    static {
        map.put("ZkServer", new K8sZkServerHandlerStrategy("ZOOKEEPER", "ZkServer"));
    }

    public static K8sServiceRoleStrategy getServiceRoleHandler(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        return map.get(type);
    }
}
