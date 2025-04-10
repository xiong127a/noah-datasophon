package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.helper.RedisConnectionInfoHelper;
import com.datasophon.api.load.GlobalVariables;
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
import java.util.Objects;
import java.util.stream.Collectors;

public class RedisSentinelHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {
    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelHandlerStrategy.class);

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        String redisSentinelMasterHost = globalVariables.get("${redisSentinelMasterHost}");
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
        ServiceConfig redisMasterHostConfig = map.get("redisSentinelMasterHost");

        if (Objects.nonNull(redisSentinelMasterHost)) {
            redisMasterHostConfig.setValue(redisSentinelMasterHost);
        }

        // 获取Redis Sentinel服务角色主机映射
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> hostMap = CacheOperateUtils.getWithType(hostMapKey,
                new TypeReference<HashMap<String, List<String>>>() {
                });

        // 获取各角色的主机列表
        List<String> masterHostList = hostMap.get("RedisSentinelMaster");
        List<String> slaveHostList = hostMap.get("RedisSentinelSlave");
        List<String> sentinelHostList = hostMap.get("RedisSentinel");

        // 更新配置
        for (ServiceConfig serviceConfig : list) {
            // 获取端口
            List<ServiceConfig> portConfigs = list.stream()
                    .filter(config -> "redisSentinelMasterPort".equals(config.getName())
                            || "redisSentinelSlavePort".equals(config.getName())
                            || "redisSentinelPort".equals(config.getName()))
                    .collect(Collectors.toList());

            Map<String, Object> portConfigValues = portConfigs.stream().collect(Collectors.toMap(
                    ServiceConfig::getName,
                    ServiceConfig::getValue));

            // 获取各种端口
            String masterPort = (String) portConfigValues.getOrDefault("redisSentinelMasterPort", "6379");
            String slavePort = (String) portConfigValues.getOrDefault("redisSentinelSlavePort", "6379");
            String sentinelPort = (String) portConfigValues.getOrDefault("redisSentinelPort", "26379");

            // 设置主节点地址
            if ("redisSentinelMasterAddr".equals(serviceConfig.getName()) && CollUtil.isNotEmpty(masterHostList)) {
                String masterAddr = masterHostList.stream()
                        .map(t -> t + ":" + masterPort)
                        .collect(Collectors.joining(" "));
                serviceConfig.setRequired(true);
                serviceConfig.setValue(masterAddr);
            }

            // 设置从节点地址
            if ("redisSentinelSlaveAddr".equals(serviceConfig.getName()) && CollUtil.isNotEmpty(slaveHostList)) {
                String slaveAddr = slaveHostList.stream()
                        .map(t -> t + ":" + slavePort)
                        .collect(Collectors.joining(" "));
                serviceConfig.setRequired(true);
                serviceConfig.setValue(slaveAddr);
            }

            // 设置哨兵节点地址
            if ("redisSentinelAddr".equals(serviceConfig.getName()) && CollUtil.isNotEmpty(sentinelHostList)) {
                String sentinelAddr = sentinelHostList.stream()
                        .map(t -> t + ":" + sentinelPort)
                        .collect(Collectors.joining(" "));
                serviceConfig.setRequired(true);
                serviceConfig.setValue(sentinelAddr);
            }
        }
    }

    @Override
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId) {
        return SpringUtil.getBean(RedisConnectionInfoHelper.class).getRedisConnectionInfo(clusterId, serviceInstanceId, this);
    }
}
