package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.utils.CacheOperateUtils;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

        private static final Logger logger = LoggerFactory.getLogger(RedisHandlerStrategy.class);

        @Override
        public void getConfig(Integer clusterId, List<ServiceConfig> list) {
                List<ServiceConfig> collect = list.stream()
                        .filter(config -> "redisMasterPort".equals(config.getName()) || "redisSlavePort".equals(config.getName())).toList();
                Map<String, Object> portConfigValues =   collect.stream().collect(Collectors.toMap(
                        ServiceConfig::getName, // 键：配置名称
                        ServiceConfig::getValue // 值：相应的端口值
                ));
                // 直接从 Map 中提取 masterPort 和 slavePort
                String masterPort = (String) portConfigValues.get("redisMasterPort");
                String slavePort = (String) portConfigValues.get("redisSlavePort");
                ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
                ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
                String hostMapKey =
                        clusterInfo.getClusterCode()
                                + Constants.UNDERLINE
                                + Constants.SERVICE_ROLE_HOST_MAPPING;
                Map<String, List<String>> map = CacheOperateUtils.getGeneric(hostMapKey, TypeRefs.MAP_STRING_LIST_STRING);

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
                        if (conflictFound) {
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
                                List<String> workers = workerHostList.stream().map(t -> "\"redis://" + t + ":" + slavePort + "\"").toList();
                                masters.addAll(workers);
                                serviceConfig.setValue(StrUtil.join(",", masters));
                        }
                }
        }

        /**
         * 获取Redis模板变量
         *
         * @param clusterId         集群ID
         * @param serviceInstanceId 服务实例ID
         * @param configMap         配置映射
         * @return ConnectionInfo对象
         */
        @Override
        protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
                        Integer clusterId, Integer serviceInstanceId, Map<String, String> configMap) {
                try {
                        logger.info("开始获取Redis服务连接信息，集群ID: {}, 服务实例ID: {}", clusterId, serviceInstanceId);

                        // 根据服务类型设置角色名称
                        String masterRoleName = "RedisMaster";
                        String slaveRoleName = "RedisWorker";

                        // 获取Redis Master和Slave节点列表
                        List<String> masterList = getRoleHosts(clusterId, serviceInstanceId, masterRoleName);
                        List<String> slaveList = getRoleHosts(clusterId, serviceInstanceId, slaveRoleName);

                        // 如果没有找到Master节点，返回空信息
                        if (CollUtil.isEmpty(masterList)) {
                                logger.warn("未找到Redis Master节点，集群ID: {}", clusterId);
                                return ConnectionInfo.builder();
                        }

                        logger.info("Redis主节点数量: {}, 从节点数量: {}", masterList.size(), slaveList.size());

                        // 获取端口配置
                        String masterPort = configMap.getOrDefault("redisMasterPort", "6379");
                        String slavePort = configMap.getOrDefault("redisSlavePort", "6379");

                        // 主节点信息
                        String masterNode = masterList.getFirst();
                        logger.info("Redis主节点: {}:{}", masterNode, masterPort);

                        // 判断是否启用了密码认证
                        String redisPassword = configMap.get("requirepass");
                        boolean hasPassword = StrUtil.isNotBlank(redisPassword);

                        // 判断是否为集群模式（主从复制模式也算作单节点模式）
                        boolean isClusterMode = "yes".equalsIgnoreCase(configMap.getOrDefault("cluster-enabled", "no"));

                        // 构建Redis URI
                        String redisUri = "redis://";
                        if (hasPassword) {
                                redisUri += ":" + redisPassword + "@";
                        }
                        redisUri += masterNode + ":" + masterPort;

                        // 部署模式
                        String deployMode = isClusterMode ? "集群模式" : "主从模式";

                        // 构建基本信息项列表
                        List<InfoItem> basicInfoItems = new ArrayList<>();
                        basicInfoItems.add(new InfoItem("host", "主机", masterNode));
                        basicInfoItems.add(new InfoItem("port", "端口", masterPort));
                        basicInfoItems.add(new InfoItem("redisUri", "连接URI", redisUri));
                        basicInfoItems.add(new InfoItem("deployMode", "部署模式", deployMode));

                        if (!slaveList.isEmpty()) {
                                String slaveNodes = slaveList.stream()
                                                .map(node -> node + ":" + slavePort)
                                                .collect(Collectors.joining(","));
                                basicInfoItems.add(new InfoItem("slaveNodes", "从节点", slaveNodes));
                        }

                        // 构建安全信息项列表
                        List<InfoItem> securityInfoItems = new ArrayList<>();
                        securityInfoItems.add(new InfoItem("authEnabled", "启用认证", hasPassword ? "是" : "否"));
                        if (hasPassword) {
                                securityInfoItems.add(new InfoItem("password", "密码", redisPassword));
                        }

                        // 构建连接信息项列表
                        List<InfoItem> connectInfoItems = new ArrayList<>();

                        // Redis CLI连接命令
                        String cliCommand = "redis-cli -h " + masterNode + " -p " + masterPort;
                        if (hasPassword) {
                                cliCommand += " -a " + redisPassword;
                        }
                        connectInfoItems.add(new InfoItem("cliCommand", "CLI连接命令", cliCommand));

                        // Redis连接URI
                        connectInfoItems.add(new InfoItem("redisUri", "Redis URI", redisUri));

                        // 对于集群模式，添加集群连接信息
                        if (isClusterMode) {
                                String clusterCli = "redis-cli -c -h " + masterNode + " -p " + masterPort;
                                if (hasPassword) {
                                        clusterCli += " -a " + redisPassword;
                                }
                                connectInfoItems.add(new InfoItem("clusterCli", "集群CLI连接", clusterCli));
                        }

                        // 构建重要键列表
                        List<String> importantKeys = Arrays.asList("redisUri", "cliCommand");

                        // 构建连接信息对象
                        logger.info("Redis连接信息生成成功");
                        return ConnectionInfo.builder()
                                        .basicInfoItems(basicInfoItems)
                                        .securityInfoItems(securityInfoItems)
                                        .connectInfoItems(connectInfoItems)
                                        .hostName(masterNode)
                                        .importantKeys(importantKeys);
                } catch (Exception e) {
                        logger.error("获取Redis模板变量出错: {}", e.getMessage(), e);
                        return ConnectionInfo.builder();
                }
        }
}
