package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
            Integer clusterId, Integer serviceInstanceId, Map<String, String> configMap) {
        try {
            // 获取服务节点列表
            List<String> sentinelNodes = getRoleHosts(clusterId, serviceInstanceId, "RedisSentinel");
            List<String> masterNodes = getRoleHosts(clusterId, serviceInstanceId, "RedisSentinelMaster");
            List<String> slaveNodes = getRoleHosts(clusterId, serviceInstanceId, "RedisSentinelSlave");

            String primaryNode = masterNodes.isEmpty() ? (sentinelNodes.isEmpty() ? "localhost" : sentinelNodes.get(0))
                    : masterNodes.get(0);
            String sentinelHost = sentinelNodes.isEmpty() ? "localhost" : sentinelNodes.get(0);

            // 获取端口和配置信息
            String sentinelPort = configMap.getOrDefault("redisSentinelPort", "26379");
            String masterPort = configMap.getOrDefault("redisSentinelMasterPort", "6379");
            String slavePort = configMap.getOrDefault("redisSentinelSlavePort", "6380");
            String masterName = configMap.getOrDefault("redisSentinelMasterName", "mymaster");

            // 判断是否启用安全认证
            boolean authEnabled = !StrUtil.isBlank(configMap.get("redisSentinelPassword"));
            String password = configMap.getOrDefault("redisSentinelPassword", "");

            // 构建信息项列表
            List<InfoItem> basicInfoItems = new ArrayList<>();
            basicInfoItems.add(new InfoItem("host", "主机", sentinelHost));
            basicInfoItems.add(new InfoItem("port", "哨兵端口", sentinelPort));
            basicInfoItems.add(new InfoItem("masterName", "主节点名称", masterName));
            basicInfoItems.add(new InfoItem("sentinelNodes", "哨兵节点列表", String.join(",", sentinelNodes)));
            basicInfoItems.add(new InfoItem("sentinelPort", "哨兵端口", sentinelPort));

            // 将Python模板需要的变量添加到基本信息中
            basicInfoItems.add(new InfoItem("sentinelHost1", "哨兵节点1",
                    sentinelNodes.isEmpty() ? "localhost" : sentinelNodes.get(0)));
            basicInfoItems.add(new InfoItem("sentinelPort1", "哨兵端口1", sentinelPort));

            if (sentinelNodes.size() > 1) {
                basicInfoItems.add(new InfoItem("sentinelHost2", "哨兵节点2", sentinelNodes.get(1)));
            } else {
                basicInfoItems.add(new InfoItem("sentinelHost2", "哨兵节点2", "localhost"));
            }
            basicInfoItems.add(new InfoItem("sentinelPort2", "哨兵端口2", sentinelPort));

            if (sentinelNodes.size() > 2) {
                basicInfoItems.add(new InfoItem("sentinelHost3", "哨兵节点3", sentinelNodes.get(2)));
            } else {
                basicInfoItems.add(new InfoItem("sentinelHost3", "哨兵节点3", "localhost"));
            }
            basicInfoItems.add(new InfoItem("sentinelPort3", "哨兵端口3", sentinelPort));

            // 如果有主节点，添加主节点信息
            if (!masterNodes.isEmpty()) {
                basicInfoItems.add(new InfoItem("masterHost", "主节点主机", masterNodes.get(0)));
                basicInfoItems.add(new InfoItem("masterPort", "主节点端口", masterPort));
            }

            List<InfoItem> securityInfoItems = new ArrayList<>();
            if (authEnabled) {
                securityInfoItems.add(new InfoItem("authEnabled", "启用认证", "是"));
                securityInfoItems.add(new InfoItem("password", "密码", password));
            } else {
                securityInfoItems.add(new InfoItem("authEnabled", "启用认证", "否"));
            }

            List<InfoItem> connectInfoItems = new ArrayList<>();
            connectInfoItems.add(new InfoItem("sentinelUrl", "哨兵连接地址",
                    String.format("redis://%s:%s", sentinelHost, sentinelPort)));
            if (!masterNodes.isEmpty()) {
                connectInfoItems.add(new InfoItem("masterUrl", "主节点连接地址",
                        String.format("redis://%s:%s", masterNodes.get(0), masterPort)));
            }

            // 构建连接信息对象
            return ConnectionInfo.builder()
                    .basicInfoItems(basicInfoItems)
                    .securityInfoItems(securityInfoItems)
                    .connectInfoItems(connectInfoItems)
                    .hostName(sentinelHost);
        } catch (Exception e) {
            logger.error("获取Redis Sentinel连接信息失败: {}", e.getMessage(), e);
            return ConnectionInfo.builder();
        }
    }

}
