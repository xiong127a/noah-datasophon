package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ClickHouse服务处理策略
 */
@Slf4j
public class ClickhouseHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        ClusterInfoService clusterInfoService = SpringUtil
                .getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE
                + Constants.SERVICE_ROLE_HOST_MAPPING;
        Map<String, List<String>> hostMap = CacheOperateUtils.getGeneric(hostMapKey, TypeRefs.MAP_STRING_LIST_STRING);

        if (Objects.nonNull(hostMap)) {
            List<String> hostList = hostMap.get("ClickHouse");
            for (ServiceConfig serviceConfig : list) {
                if ("ckShardAddress".equals(serviceConfig.getName())) {
                    serviceConfig.setValue(hostList.stream().map(t -> t + ":9010")
                            .collect(Collectors.toList()));
                }
                if ("ckZkAddress".equals(serviceConfig.getName())) {
                    Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                    String zkUrls = globalVariables.get("${zkUrls}");
                    List<String> zkUrlList = StrUtil.splitTrim(zkUrls, ",");
                    serviceConfig.setValue(zkUrlList);
                }
            }
        }
    }

    @Override
    protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
            Integer clusterId, Integer serviceInstanceId, Map<String, String> configMap) {
        try {
            // 获取ClickHouse节点列表
            List<String> clickhouseNodes = getRoleHosts(clusterId, serviceInstanceId, "ClickHouse");
            if (CollUtil.isEmpty(clickhouseNodes)) {
                log.warn("未找到ClickHouse节点，集群ID: {}", clusterId);
                return ConnectionInfo.builder();
            }

            // 获取端口配置
            String httpPort = configMap.getOrDefault("ckHttpPort", "8123");
            String tcpPort = configMap.getOrDefault("ckTcpPort", "9000");
            String mysqlPort = configMap.getOrDefault("ckMysqlPort", "9004");

            // 处理端口参数
            String portParam = "9000".equals(tcpPort) ? "" : "--port=" + tcpPort;

            // 判断是否启用了安全认证
            boolean enableSecurity = !StrUtil.isBlank(configMap.get("ckUsername")) &&
                    !StrUtil.isBlank(configMap.get("ckPassword"));
            String securityUser = configMap.getOrDefault("ckUsername", "default");
            String securityPassword = configMap.getOrDefault("ckPassword", "");

            // 获取数据库名称
            String databaseName = configMap.getOrDefault("ckDatabase", "default");

            // 获取第一个节点作为主要连接节点
            String primaryNode = clickhouseNodes.getFirst();

            // 构建基本信息项列表 - 使用InfoItem替代Map
            List<InfoItem> basicInfoItems = new ArrayList<>();
            basicInfoItems.add(new InfoItem("host", "主机", primaryNode));
            basicInfoItems.add(new InfoItem("tcpPort", "TCP端口", tcpPort));
            basicInfoItems.add(new InfoItem("httpPort", "HTTP端口", httpPort));
            basicInfoItems.add(new InfoItem("mysqlPort", "MySQL端口", mysqlPort));
            basicInfoItems.add(new InfoItem("database", "数据库", databaseName));

            // 构建安全信息项列表 - 使用InfoItem替代Map
            List<InfoItem> securityInfoItems = new ArrayList<>();
            if (enableSecurity) {
                securityInfoItems.add(new InfoItem("authMode", "认证模式", "用户名密码"));
                securityInfoItems.add(new InfoItem("username", "用户名", securityUser));
                securityInfoItems.add(new InfoItem("password", "密码", securityPassword));
            } else {
                securityInfoItems.add(new InfoItem("authMode", "认证模式", "无认证"));
                securityInfoItems.add(new InfoItem("username", "用户名", "default"));
                securityInfoItems.add(new InfoItem("password", "密码", ""));
            }

            // 构建连接信息项列表 - 使用InfoItem替代Map
            List<InfoItem> connectInfoItems = new ArrayList<>();
            String jdbcUrl = "jdbc:clickhouse://" + primaryNode + ":" + tcpPort + "/" + databaseName;
            String httpUrl = "http://" + primaryNode + ":" + httpPort;
            String commandLine;

            if (enableSecurity) {
                commandLine = "clickhouse-client --host=" + primaryNode + " " + portParam +
                        " --user=" + securityUser + " --password=" + securityPassword +
                        " --database=" + databaseName;
            } else {
                commandLine = "clickhouse-client --host=" + primaryNode + " " + portParam +
                        " --database=" + databaseName;
            }

            String mysqlConnect = "mysql -h " + primaryNode + " -P " + mysqlPort + " -u " + securityUser +
                    (enableSecurity ? " -p" + securityPassword : "");

            connectInfoItems.add(new InfoItem("jdbcUrl", "JDBC URL", jdbcUrl));
            connectInfoItems.add(new InfoItem("httpUrl", "HTTP URL", httpUrl));
            connectInfoItems.add(new InfoItem("commandLine", "命令行连接", commandLine));
            connectInfoItems.add(new InfoItem("mysqlConnect", "MySQL兼容模式连接", mysqlConnect));

            // 构建重要键列表
            List<String> importantKeys = Arrays.asList("jdbcUrl", "httpUrl", "commandLine");

            // 构建连接信息对象
            return ConnectionInfo.builder()
                    .basicInfoItems(basicInfoItems)
                    .securityInfoItems(securityInfoItems)
                    .connectInfoItems(connectInfoItems)
                    .hostName(primaryNode)
                    .importantKeys(importantKeys);
        } catch (Exception e) {
            log.error("获取ClickHouse连接信息失败: {}", e.getMessage(), e);
            return ConnectionInfo.builder();
        }
    }
}
