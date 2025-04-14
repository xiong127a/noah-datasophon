package com.datasophon.api.strategy;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.helper.RedisConnectionInfoHelper;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RedisHandlerStrategy.class);

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        List<ServiceConfig> collect = list.stream()
                .filter(config -> "redisMasterPort".equals(config.getName())
                        || "redisSlavePort".equals(config.getName()))
                .collect(Collectors.toList());
        Map<String, Object> portConfigValues = collect.stream().collect(Collectors.toMap(
                ServiceConfig::getName, // 键：配置名称
                ServiceConfig::getValue // 值：相应的端口值
        ));
        // 直接从 Map 中提取 masterPort 和 slavePort
        String masterPort = (String) portConfigValues.get("redisMasterPort");
        String slavePort = (String) portConfigValues.get("redisSlavePort");
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        String hostMapKey = clusterInfo.getClusterCode()
                + Constants.UNDERLINE
                + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> map = CacheOperateUtils.getWithType(hostMapKey,
                new TypeReference<HashMap<String, List<String>>>() {
                });

        List<String> masterHostList = map.get("RedisMaster");
        List<String> workerHostList = map.get("RedisWorker");

        for (ServiceConfig serviceConfig : list) {
            if ("RedisMasterAddr".equals(serviceConfig.getName())) {
                String masterAddr = masterHostList.stream()
                        .map(t -> t + ":" + masterPort)
                        .collect(Collectors.joining(" "));
                serviceConfig.setRequired(true);
                serviceConfig.setValue(masterAddr);
            }
            if ("RedisSlaveAddr".equals(serviceConfig.getName())) {
                String workerAddr = workerHostList.stream()
                        .map(t -> t + ":" + slavePort)
                        .collect(Collectors.joining(" "));
                serviceConfig.setRequired(true);
                serviceConfig.setValue(workerAddr);
            }
            if ("redisMetricHosts".equals(serviceConfig.getName())) {
                List<String> masters = masterHostList.stream().map(t -> "\"redis://" + t + ":" + masterPort + "\"")
                        .collect(Collectors.toList());
                List<String> workers = workerHostList.stream().map(t -> "\"redis://" + t + ":" + slavePort + "\"")
                        .collect(Collectors.toList());
                masters.addAll(workers);
                serviceConfig.setValue(StrUtil.join(",", masters));
            }
        }
    }

    @Override
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId,String serviceHome,Map<String, String> configMap) {
        return SpringUtil.getBean(RedisConnectionInfoHelper.class).getRedisConnectionInfo(clusterId, serviceInstanceId, serviceHome,configMap,this);
    }
}
