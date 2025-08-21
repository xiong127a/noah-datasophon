package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class DorisHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

        @Override
        public void handlerConfig(Long clusterId, List<ServiceConfig> list) {
                getConfig(clusterId, list);
        }

        @Override
        public void getConfig(Long clusterId, List<ServiceConfig> list) {
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                String priority_networks = globalVariables.get("${priority_networks}");
                for (ServiceConfig serviceConfig : list) {
                        if (StrUtil.equals(serviceConfig.getName(), "priority_networks")) {
                                serviceConfig.setValue(priority_networks);
                        }
                }
        }

        @Override
        protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
                        Long clusterId, Long serviceInstanceId, Map<String, String> configMap) {
                try {
                        // 获取DorisFE节点信息 (master)
                        List<String> feNodes = getRoleHosts(clusterId, serviceInstanceId, "DorisFE");

                        // 获取DorisFEObserver节点信息 (slave)
                        List<String> feObserverNodes = getRoleHosts(clusterId, serviceInstanceId, "DorisFEObserver");

                        // 获取DorisBE节点信息
                        List<String> beNodes = getRoleHosts(clusterId, serviceInstanceId, "DorisBE");

                        if (CollUtil.isEmpty(feNodes)) {
                                log.warn("未找到Doris FE节点，集群ID: {}", clusterId);
                                return ConnectionInfo.builder();
                        }

                        // 获取主节点和从节点信息
                        String dorisMaster = feNodes.isEmpty() ? "" : feNodes.getFirst();
                        String dorisSlave = feObserverNodes.isEmpty() ? "" : feObserverNodes.getFirst();

                        // 获取端口配置
                        String dorisFEPort = configMap.getOrDefault("doris.fe.port", "9030");
                        String dorisBEPort = configMap.getOrDefault("doris.be.port", "8040");
                        String dorisHttpPort = configMap.getOrDefault("doris.http.port", "8030");

                        // 构建基本信息项列表
                        List<InfoItem> basicInfoItems = new ArrayList<>();
                        basicInfoItems.add(new InfoItem("host", "主机", dorisMaster));
                        basicInfoItems.add(new InfoItem("fePort", "FE端口", dorisFEPort));
                        basicInfoItems.add(new InfoItem("bePort", "BE端口", dorisBEPort));
                        basicInfoItems.add(new InfoItem("httpPort", "HTTP端口", dorisHttpPort));
                        basicInfoItems.add(new InfoItem("jdbcUrl", "JDBC URL",
                                        "jdbc:mysql://" + dorisMaster + ":" + dorisFEPort));
                        basicInfoItems.add(new InfoItem("httpUrl", "HTTP URL",
                                        "http://" + dorisMaster + ":" + dorisHttpPort));

                        // 添加BE节点信息
                        if (!beNodes.isEmpty()) {
                                basicInfoItems.add(
                                                new InfoItem("beNodesCount", "BE节点数量", String.valueOf(beNodes.size())));
                                for (int i = 0; i < Math.min(beNodes.size(), 3); i++) {
                                        basicInfoItems.add(new InfoItem("beNode" + (i + 1), "BE节点" + (i + 1),
                                                        beNodes.get(i) + ":" + dorisBEPort));
                                }
                        }

                        // 如果有从节点，添加到基本信息中
                        if (StrUtil.isNotBlank(dorisSlave)) {
                                basicInfoItems.add(new InfoItem("highAvailability", "高可用", "启用"));
                                basicInfoItems.add(new InfoItem("feObserver", "FE Observer节点",
                                                dorisSlave + ":" + dorisFEPort));
                        } else {
                                basicInfoItems.add(new InfoItem("highAvailability", "高可用", "未启用"));
                        }

                        // 构建安全信息项列表
                        List<InfoItem> securityInfoItems = new ArrayList<>();
                        securityInfoItems.add(new InfoItem("authMode", "认证模式", "用户名密码"));
                        securityInfoItems.add(new InfoItem("username", "用户名", "root"));
                        securityInfoItems.add(new InfoItem("password", "密码", ""));

                        // 构建连接信息项列表
                        List<InfoItem> connectInfoItems = new ArrayList<>();
                        connectInfoItems.add(new InfoItem("jdbcUrl", "JDBC URL",
                                        "jdbc:mysql://" + dorisMaster + ":" + dorisFEPort));
                        connectInfoItems.add(new InfoItem("httpUrl", "HTTP URL",
                                        "http://" + dorisMaster + ":" + dorisHttpPort));
                        connectInfoItems.add(new InfoItem("mysqlConnect", "MySQL连接命令",
                                        "mysql -h " + dorisMaster + " -P " + dorisFEPort + " -u root"));

                        // 构建重要键列表
                        List<String> importantKeys = Arrays.asList("jdbcUrl", "httpUrl", "mysqlConnect");

                        // 构建连接信息对象
                        return ConnectionInfo.builder()
                                        .basicInfoItems(basicInfoItems)
                                        .securityInfoItems(securityInfoItems)
                                        .connectInfoItems(connectInfoItems)
                                        .hostName(dorisMaster)
                                        .importantKeys(importantKeys);
                } catch (Exception e) {
                        log.error("获取Doris连接信息失败: {}", e.getMessage(), e);
                        return ConnectionInfo.builder();
                }
        }
}
