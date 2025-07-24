/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.util.JdbcUtils;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HiveServer2HandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        CacheUtils.put("enableHiveServer2HA", false);
        if (CollUtil.isNotEmpty(hosts)) {
            CacheUtils.put("enableHiveServer2HA", true);
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${masterHiveServer2}", hosts.getFirst());
            ProcessUtils.generateClusterVariable(globalVariables, clusterId,
                    "${masterHiveServer2Principal}", "hive/" + hosts.getFirst() + "@HADOOP.COM");
        }
    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        boolean enableKerberos = false;
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
        for (ServiceConfig config : list) {
            if ("enableKerberos".equals(config.getName())) {
                enableKerberos = isEnableKerberos(clusterId, globalVariables, enableKerberos, config, "HIVE");
            }
            if (StrUtil.equals("javax.jdo.option.ConnectionURL", config.getName())) {
                String jdbcUrl = config.getValue().toString();
                String dbType = JdbcUtils.getDbType(jdbcUrl, "");
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${HiveMetaStore-dbType}",
                        dbType);
                config.setValue(jdbcUrl);
            }

        }
        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "HIVE" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);
        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
        if (enableKerberos) {
            addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
        } else {
            removeConfigWithKerberos(list, map, configs);
        }
        list.addAll(kbConfigs);

    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        // if enabled hiveserver2 ha
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        List<ServiceConfig> serviceConfigs = ServiceConfigMap
                .get(clusterInfo.getClusterFrame() + Constants.UNDERLINE + "HIVE" + Constants.CONFIG);
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if ((Boolean) CacheUtils.get("enableHiveServer2HA")) {
            for (ServiceConfig serviceConfig : serviceConfigs) {
                if ("ha".equals(serviceConfig.getConfigType())) {
                    serviceConfig.setRequired(true);
                    serviceConfig.setHidden(false);
                    if (Constants.INPUT.equals(serviceConfig.getType())) {
                        String value = PlaceholderUtils.replacePlaceholders((String) serviceConfig.getValue(),
                                globalVariables, Constants.REGEX_VARIABLE);
                        serviceConfig.setValue(value);
                    }
                    list.add(serviceConfig);
                }
            }
        } else {
            for (ServiceConfig serviceConfig : serviceConfigs) {
                if ("ha".equals(serviceConfig.getConfigType())) {
                    serviceConfig.setRequired(false);
                    serviceConfig.setHidden(true);
                }
            }
        }
    }

    /**
     * 获取Hive服务特定的连接信息
     */
    @Override
    protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
            Integer clusterId, Integer serviceInstanceId, Map<String, String> configMap) {
        try {
            // 获取HiveServer2节点列表
            List<String> hiveServer2Hosts = getRoleHosts(clusterId, serviceInstanceId, "HiveServer2");

            // 获取全局变量用于Kerberos判断
            Map<String, String> globalVariables = GlobalVariables.get(clusterId);

            // 判断是否启用Kerberos认证
            boolean enableKerberos = false;
            if (configMap.containsKey("enableKerberos")) {
                enableKerberos = Boolean.parseBoolean(String.valueOf(configMap.get("enableKerberos")));
                if (enableKerberos) {
                    enableKerberos = isEnableKerberos(clusterId, globalVariables, true, null, "HIVE");
                }
            }

            // 判断是否启用HiveServer2高可用
            boolean enableHiveServer2HA = "true".equalsIgnoreCase(String.valueOf(
                    configMap.getOrDefault("hive.server2.support.dynamic.service.discovery", "false"))) ||
                    "true".equalsIgnoreCase(String.valueOf(
                            configMap.getOrDefault("hive.server2.active.passive.ha.enable", "false")));

            // 解析高可用模式
            boolean dynamicServiceDiscovery = "true".equalsIgnoreCase(String.valueOf(
                    configMap.getOrDefault("hive.server2.support.dynamic.service.discovery", "false")));
            boolean activePassiveHA = "true".equalsIgnoreCase(String.valueOf(
                    configMap.getOrDefault("hive.server2.active.passive.ha.enable", "false")));

            // 获取ZooKeeper命名空间
            String zkNamespace = StrUtil.isNotBlank(String.valueOf(configMap.get("hive.server2.zookeeper.namespace")))
                    ? String.valueOf(configMap.get("hive.server2.zookeeper.namespace"))
                    : "hiveserver2";

            // 确定高可用模式
            String haMode = Constants.HA_MODE_STANDALONE;
            if (dynamicServiceDiscovery) {
                if (activePassiveHA) {
                    haMode = Constants.HA_MODE_ZOOKEEPER_HA; // 主备模式
                } else {
                    haMode = Constants.HA_MODE_ZOOKEEPER; // 动态服务发现模式
                }
            }

            // 获取HiveServer2主机地址和端口
            String hiveServer2Host = String.valueOf(configMap.getOrDefault("hive.server2.thrift.bind.host", ""));
            String hiveServer2Port = String.valueOf(configMap.getOrDefault("hive.server2.thrift.port", "10000"));

            // 如果配置中找不到主机名，使用HiveServer2实例的主机名
            if (StrUtil.isBlank(hiveServer2Host) || "${hostname}".equals(hiveServer2Host)) {
                if (CollUtil.isNotEmpty(hiveServer2Hosts)) {
                    hiveServer2Host = hiveServer2Hosts.getFirst();
                } else {
                    hiveServer2Host = ProcessUtils.getServiceRoleHostname(clusterId, "HIVE", "HiveServer2");
                }
            }

            // 如果无法获取到主机地址，返回空对象
            if (StrUtil.isBlank(hiveServer2Host)) {
                log.warn("无法获取HiveServer2主机地址，集群ID: {}", clusterId);
                return ConnectionInfo.builder();
            }

            // 获取ZooKeeper地址
            String zkQuorum = String.valueOf(configMap.getOrDefault("hive.zookeeper.quorum", ""));
            if (StrUtil.isBlank(zkQuorum)) {
                zkQuorum = globalVariables.getOrDefault("${zkQuorum}", "");
            }

            // 获取HiveMetastore地址
            String metastoreUris = String.valueOf(configMap.getOrDefault("hive.metastore.uris", ""));
            if (StrUtil.isBlank(metastoreUris)) {
                String hiveMetastoreHost = ProcessUtils.getServiceRoleHostname(clusterId, "HIVE", "HiveMetaStore");
                if (StrUtil.isNotBlank(hiveMetastoreHost)) {
                    metastoreUris = "thrift://" + hiveMetastoreHost + ":9083";
                }
            }

            // 根据高可用模式生成JDBC URL
            String jdbcUrl;
            String haDescription;

            if (enableHiveServer2HA) {
                switch (haMode) {
                    case Constants.HA_MODE_ZOOKEEPER:
                        // 基于ZooKeeper的动态服务发现模式（负载均衡）
                        if (StrUtil.isNotBlank(zkQuorum)) {
                            jdbcUrl = String.format(
                                    "jdbc:hive2://%s/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=%s",
                                    zkQuorum, zkNamespace);
                            haDescription = "ZooKeeper服务发现(负载均衡)";
                        } else {
                            // 回退到单实例模式
                            jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                            haDescription = "ZooKeeper服务发现(配置不完整)";
                            enableHiveServer2HA = false;
                        }
                        break;

                    case Constants.HA_MODE_ZOOKEEPER_HA:
                        // Active-Passive模式（主备切换）
                        if (StrUtil.isNotBlank(zkQuorum)) {
                            jdbcUrl = String.format(
                                    "jdbc:hive2://%s/;serviceDiscoveryMode=zooKeeperHA;zooKeeperNamespace=%s",
                                    zkQuorum, zkNamespace);
                            haDescription = "ZooKeeper主备切换(Active-Passive)";
                        } else {
                            // 回退到单实例模式
                            jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                            haDescription = "ZooKeeper主备切换(配置不完整)";
                            enableHiveServer2HA = false;
                        }
                        break;

                    default:
                        // 默认模式，直接使用HiveServer2
                        jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                        haDescription = "单实例模式";
                        break;
                }
            } else {
                // 非高可用模式
                jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                haDescription = "单实例模式";
            }

            // 处理Kerberos认证的URL
            String principal = "";
            if (enableKerberos) {
                principal = String.valueOf(configMap.getOrDefault(
                        "hive.server2.authentication.kerberos.principal",
                        "hive/" + hiveServer2Host + "@HADOOP.COM"));

                // 替换principal中的${hostname}为实际主机名
                if (principal.contains("${hostname}")) {
                    principal = principal.replace("${hostname}", hiveServer2Host);
                }

                jdbcUrl += ";principal=" + principal + ";auth=kerberos";
            }

            // Hive JDBC 认证参数
            String authType = String.valueOf(configMap.getOrDefault("hive.server2.authentication", "NONE"));
            // 正确判断是否启用密码认证：当认证类型为NONE时，不启用密码认证
            // 当认证类型为CUSTOM、LDAP或PAM时，启用密码认证
            boolean enablePasswordAuth = !enableKerberos &&
                    ("CUSTOM".equalsIgnoreCase(authType) ||
                            "LDAP".equalsIgnoreCase(authType) ||
                            "PAM".equalsIgnoreCase(authType) ||
                            "NOSASL".equalsIgnoreCase(authType));

            // 设置默认用户名密码（即使不启用密码认证也需要显示）
            String username = String.valueOf(configMap.getOrDefault("hive.server2.authentication.username", ""));
            String password = String.valueOf(configMap.getOrDefault("hive.server2.authentication.password", ""));

            // 检查其他可能的用户名密码配置项
            if (StrUtil.isBlank(username)) {
                username = String.valueOf(configMap.getOrDefault("hive.server2.thrift.http.username", ""));
            }
            if (StrUtil.isBlank(password)) {
                password = String.valueOf(configMap.getOrDefault("hive.server2.thrift.http.password", ""));
            }

            // 如果用户名密码为空，使用默认值
            if (StrUtil.isBlank(username)) {
                username = "hive"; // 默认用户名
            }
            if (StrUtil.isBlank(password)) {
                password = "hive"; // 默认密码
            }

            // 如果开启了密码认证，但没有在JDBC URL中配置，添加认证参数
            if (enablePasswordAuth && !jdbcUrl.contains("user=") && !jdbcUrl.contains("password=")) {
                if (jdbcUrl.contains(";")) {
                    jdbcUrl += ";user=" + username + ";password=" + password;
                } else {
                    jdbcUrl += ";user=" + username + ";password=" + password;
                }
            }

            // 构建基本信息项列表
            List<InfoItem> basicInfoItems = new ArrayList<>();
            basicInfoItems.add(new InfoItem("host", "主机", hiveServer2Host));
            basicInfoItems.add(new InfoItem("port", "端口", hiveServer2Port));
            basicInfoItems.add(new InfoItem("highAvailability", "高可用", enableHiveServer2HA ? "true" : "false"));
            basicInfoItems.add(new InfoItem("haMode", "高可用模式", haDescription));

            // 添加主节点信息（明确标识为主节点）
            if (CollUtil.isNotEmpty(hiveServer2Hosts)) {
                basicInfoItems
                        .add(new InfoItem("masterNode", "主节点服务器", hiveServer2Hosts.getFirst() + ":" + hiveServer2Port));
            }

            // 添加从节点信息
            if (enableHiveServer2HA && CollUtil.isNotEmpty(hiveServer2Hosts) && hiveServer2Hosts.size() > 1) {
                // 按照主机名排序
                List<String> sortedHosts = new ArrayList<>(hiveServer2Hosts);
                Collections.sort(sortedHosts);

                // 跳过第一个节点（主节点）
                for (int i = 1; i < sortedHosts.size(); i++) {
                    String host = sortedHosts.get(i);
                    basicInfoItems.add(new InfoItem("slaveNode" + i, "HiveServer2从节点" + i,
                            host + ":" + hiveServer2Port));
                }
            }

            // 添加元数据服务地址
            if (StrUtil.isNotBlank(metastoreUris)) {
                basicInfoItems.add(new InfoItem("metastoreUri", "HiveMetastore地址", metastoreUris));
            }

            // 构建安全信息项列表
            List<InfoItem> securityInfoItems = new ArrayList<>();

            // 总是添加用户名密码信息，无论是否启用了密码认证
            securityInfoItems.add(new InfoItem("auth.enabled", "启用认证", enablePasswordAuth ? "true" : "false"));
            securityInfoItems.add(new InfoItem("username", "用户名", username));
            securityInfoItems.add(new InfoItem("password", "密码", password));
            if (enablePasswordAuth) {
                securityInfoItems.add(new InfoItem("auth.type", "认证类型", authType));
            }

            // Kerberos 认证信息
            securityInfoItems.add(new InfoItem("kerberos.enabled", "启用Kerberos", enableKerberos ? "true" : "false"));

            if (enableKerberos) {
                securityInfoItems.add(new InfoItem("principal", "服务主体", principal));
                // 将krb5配置文件路径添加到安全信息中
                securityInfoItems.add(new InfoItem("krb5.conf.path", "Kerberos配置文件", "/etc/krb5.conf"));

                // 如果配置中有keytab相关配置，也添加到安全信息中
                String keytabPath = configMap.getOrDefault("hive.server2.authentication.kerberos.keytab", "");
                if (StrUtil.isNotBlank(keytabPath)) {
                    securityInfoItems.add(new InfoItem("keytab.path", "密钥表文件", keytabPath));
                }
            }

            // 构建连接信息项列表
            List<InfoItem> connectInfoItems = new ArrayList<>();
            connectInfoItems.add(new InfoItem("jdbcUrl", "JDBC URL", jdbcUrl));

            // 将HiveMetastore地址添加到连接信息中
            if (StrUtil.isNotBlank(metastoreUris)) {
                connectInfoItems.add(new InfoItem("jdbc.metastoreUri", "HiveMetastore地址", metastoreUris));
            }

            // 如果有ZooKeeper信息，添加到连接信息中
            if (StrUtil.isNotBlank(zkQuorum)) {
                connectInfoItems.add(new InfoItem("zkConnect", "ZooKeeper连接", zkQuorum));
            }

            // 添加数据库信息
            connectInfoItems.add(new InfoItem("database", "默认数据库", "default"));

            // 设置单节点JDBC URL（用于直接连接特定节点）
            if (enableHiveServer2HA && CollUtil.isNotEmpty(hiveServer2Hosts)) {
                for (int i = 0; i < hiveServer2Hosts.size(); i++) {
                    String host = hiveServer2Hosts.get(i);
                    String nodeJdbcUrl = "jdbc:hive2://" + host + ":" + hiveServer2Port;
                    if (enableKerberos) {
                        String nodePrincipal = principal;
                        if (nodePrincipal.contains(hiveServer2Host)) {
                            nodePrincipal = nodePrincipal.replace(hiveServer2Host, host);
                        }
                        nodeJdbcUrl += ";principal=" + nodePrincipal + ";auth=kerberos";
                    }
                    connectInfoItems.add(new InfoItem("node" + (i + 1) + "JdbcUrl",
                            "节点" + (i + 1) + " JDBC URL", nodeJdbcUrl));
                }
            }

            // 构建并返回ConnectionInfo.ConnectionInfoBuilder对象
            return ConnectionInfo.builder()
                    .basicInfoItems(basicInfoItems)
                    .securityInfoItems(securityInfoItems)
                    .connectInfoItems(connectInfoItems)
                    .hostName(hiveServer2Host)
                    // 添加重要键列表，将JDBC URL和连接信息中metastore地址设置为高亮显示
                    .importantKeys(Arrays.asList("jdbcUrl", "jdbc.metastoreUri"));
        } catch (Exception e) {
            log.error("获取Hive连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder();
        }
    }

}
