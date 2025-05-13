package com.datasophon.api.strategy;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.*;
import java.util.stream.Collectors;

public class RedisHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {


    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        List<ServiceConfig> collect = list.stream()
                .filter(config -> "redisMasterPort".equals(config.getName()) || "redisSlavePort".equals(config.getName())).collect(Collectors.toList());
        Map<String, Object> portConfigValues =   collect.stream().collect(Collectors.toMap(
                        ServiceConfig::getName, // 键：配置名称
                        ServiceConfig::getValue // 值：相应的端口值
                ));
        // 直接从 Map 中提取 masterPort 和 slavePort
        String masterPort = (String) portConfigValues.get("redisMasterPort");
        String slavePort = (String) portConfigValues.get("redisSlavePort");
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        String hostMapKey =
                clusterInfo.getClusterCode()
                        + Constants.UNDERLINE
                        + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> map = (HashMap<String, List<String>>) CacheOperateUtils.get(hostMapKey);

        List<String> masterHostList = map.get("RedisMaster");
        List<String> workerHostList = map.get("RedisWorker");


        List<String> adjustedWorker = new ArrayList<>(workerHostList);

        // 循环位移直到无冲突 或 尝试次数耗尽
        int maxAttempts = adjustedWorker.size();
        boolean conflictFound;
        int attempts = 0;

        do {
            conflictFound = false;
            // 检查所有下标
            for (int i = 0; i < Math.min(masterHostList.size(), adjustedWorker.size()); i++) {
                if (masterHostList.get(i).equals(adjustedWorker.get(i))) {
                    conflictFound = true;
                    break;
                }
            }

            // 存在冲突则右移一位
            if (conflictFound && !adjustedWorker.isEmpty()) {
                Collections.rotate(adjustedWorker, 1); // 右移
                attempts++;
            }
        } while (conflictFound && attempts < maxAttempts);

        for (ServiceConfig serviceConfig : list) {
            if ("RedisMasterAddr".equals(serviceConfig.getName())) {
                String masterAddr = masterHostList.stream()
                        .map(t -> t + ":" + masterPort)
                        .collect(Collectors.joining(" "));
                serviceConfig.setRequired(true);
                serviceConfig.setValue(masterAddr);
            }
            if ("RedisSlaveAddr".equals(serviceConfig.getName())) {
                if (adjustedWorker.isEmpty()) {
                    adjustedWorker = workerHostList;
                }
                String workerAddr = adjustedWorker.stream()
                        .map(t -> t + ":" + slavePort)
                        .collect(Collectors.joining(" "));
                serviceConfig.setRequired(true);
                serviceConfig.setValue(workerAddr);
            }
            if ("redisMetricHosts".equals(serviceConfig.getName())) {
                List<String> masters = masterHostList.stream().map(t -> "\"redis://" + t + ":" + masterPort + "\"").collect(Collectors.toList());
                List<String> workers = workerHostList.stream().map(t -> "\"redis://" + t + ":" + slavePort + "\"").collect(Collectors.toList());
                masters.addAll(workers);
                serviceConfig.setValue(StrUtil.join(",", masters));
            }
        }
    }

}
