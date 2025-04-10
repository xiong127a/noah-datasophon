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
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.fastjson.JSONArray;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HiveServer2HandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        CacheUtils.put("enableHiveServer2HA", false);
        if (CollUtil.isNotEmpty(hosts)) {
            CacheUtils.put("enableHiveServer2HA", true);
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${masterHiveServer2}", hosts.get(0));
            ProcessUtils.generateClusterVariable(globalVariables, clusterId,
                    "${masterHiveServer2Principal}", "hive/" + hosts.get(0) + "@HADOOP.COM");
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

    private List<ServiceConfig> listServiceConfigByServiceInstance(Integer serviceInstanceId) {
        ClusterServiceInstanceRoleGroupService roleGroupService = SpringUtil
                .getBean(ClusterServiceInstanceRoleGroupService.class);
        ClusterServiceRoleGroupConfigService groupConfigService = SpringUtil
                .getBean(ClusterServiceRoleGroupConfigService.class);
        ClusterServiceInstanceRoleGroup roleGroup = roleGroupService.getRoleGroupByServiceInstanceId(serviceInstanceId);
        ClusterServiceRoleGroupConfig config = groupConfigService.getConfigByRoleGroupId(roleGroup.getId());
        return JSONArray.parseArray(config.getConfigJson(), ServiceConfig.class);
    }

    @Override
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId) {
        List<ServiceConfig> serviceConfigs = listServiceConfigByServiceInstance(serviceInstanceId);

        ClusterServiceRoleInstanceService clusterServiceRoleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        List<ClusterServiceRoleInstanceEntity> hiveServer2 = clusterServiceRoleInstanceService
                .getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, "HiveServer2");

        List<String> hiveServer2Hosts = CollUtil.map(hiveServer2, ClusterServiceRoleInstanceEntity::getHostname, true);
        // 获取所有HiveServer2节点的主机名

        try {
            // 从serviceConfigs中解析配置到map，方便快速查询
            Map<String, Object> configMap = new HashMap<>();
            for (ServiceConfig config : serviceConfigs) {
                // 如果配置值包含${host}占位符，并且有HiveServer2实例，则替换为实际主机名
                if (config.getValue() != null && config.getValue().toString().contains("${host}")
                        && CollUtil.isNotEmpty(hiveServer2Hosts)) {
                    String value = config.getValue().toString().replace("${host}", hiveServer2Hosts.get(0));
                    config.setValue(value);
                }
                configMap.put(config.getName(), config.getValue());
            }

            // 获取globalVariables用于isEnableKerberos方法
            Map<String, String> globalVariables = GlobalVariables.get(clusterId);

            // 判断是否启用了Kerberos
            boolean enableKerberos = false;
            for (ServiceConfig config : serviceConfigs) {
                if ("enableKerberos".equals(config.getName())) {
                    enableKerberos = isEnableKerberos(clusterId, globalVariables, enableKerberos, config, "HIVE");
                    break;
                }
            }

            // 从configMap中解析判断是否启用了HiveServer2高可用
            boolean enableHiveServer2HA = "true".equalsIgnoreCase(String.valueOf(
                    configMap.getOrDefault("hive.server2.support.dynamic.service.discovery", "false"))) ||
                    "true".equalsIgnoreCase(String.valueOf(
                            configMap.getOrDefault("hive.server2.active.passive.ha.enable", "false")));

            // 解析高可用相关配置
            boolean dynamicServiceDiscovery = "true".equalsIgnoreCase(String.valueOf(
                    configMap.getOrDefault("hive.server2.support.dynamic.service.discovery", "false")));
            boolean activePassiveHA = "true".equalsIgnoreCase(String.valueOf(
                    configMap.getOrDefault("hive.server2.active.passive.ha.enable", "false")));
            String zkNamespace = StrUtil.isNotBlank(String.valueOf(configMap.get("hive.server2.zookeeper.namespace")))
                    ? String.valueOf(configMap.get("hive.server2.zookeeper.namespace"))
                    : "hiveserver2";

            // 如果是主被动HA模式，获取特定的命名空间
            if (activePassiveHA && configMap.containsKey("hive.server2.active.passive.ha.registry.namespace")) {
                String registryNamespace = String
                        .valueOf(configMap.get("hive.server2.active.passive.ha.registry.namespace"));
                if (StrUtil.isNotBlank(registryNamespace)) {
                    zkNamespace = registryNamespace;
                }
            }

            // 确定高可用模式
            String haMode = Constants.HA_MODE_STANDALONE;
            if (dynamicServiceDiscovery) {
                if (activePassiveHA) {
                    haMode = Constants.HA_MODE_ZOOKEEPER_HA; // 主备模式
                } else {
                    haMode = Constants.HA_MODE_ZOOKEEPER; // 动态服务发现模式
                }
            }

            // 基本信息收集
            Map<String, String> basicInfo = new HashMap<>();

            // 获取HiveServer2主机地址和端口（从configMap中获取）
            String hiveServer2Host = String.valueOf(configMap.getOrDefault("hive.server2.thrift.bind.host", ""));
            String hiveServer2Port = String.valueOf(configMap.getOrDefault("hive.server2.thrift.port", "10000"));

            // 如果配置中找不到或是${host}占位符，使用HiveServer2实例的主机名
            if (StrUtil.isBlank(hiveServer2Host) || "${host}".equals(hiveServer2Host)) {
                if (CollUtil.isNotEmpty(hiveServer2Hosts)) {
                    hiveServer2Host = hiveServer2Hosts.get(0);
                } else {
                    hiveServer2Host = ProcessUtils.getServiceRoleHostname(clusterId, "HIVE", "HiveServer2");
                }
            }

            // 如果仍然无法获取到Hive服务主机地址，则返回空对象
            if (StrUtil.isBlank(hiveServer2Host)) {
                log.warn("无法获取HiveServer2主机地址，集群ID: {}", clusterId);
                return ConnectionInfo.builder().build();
            }

            // 获取ZooKeeper地址（从configMap中获取）
            String zkQuorum = String.valueOf(configMap.getOrDefault("hive.zookeeper.quorum", ""));

            // 如果配置中找不到，尝试从服务信息中获取
            if (StrUtil.isBlank(zkQuorum)) {
                // 使用全局变量中的值
                zkQuorum = globalVariables.getOrDefault("${zkQuorum}", "");
            }

            // 获取HiveMetastore地址（从configMap中获取）
            String metastoreUris = String.valueOf(configMap.getOrDefault("hive.metastore.uris", ""));

            // 如果配置中找不到，尝试构建一个
            if (StrUtil.isBlank(metastoreUris)) {
                String hiveMetastoreHost = ProcessUtils.getServiceRoleHostname(clusterId, "HIVE", "HiveMetaStore");
                if (StrUtil.isNotBlank(hiveMetastoreHost)) {
                    metastoreUris = "thrift://" + hiveMetastoreHost + ":9083";
                }
            }

            // 生成JDBC URL
            String jdbcUrl;

            // 根据高可用模式生成JDBC URL
            if (enableHiveServer2HA) {
                switch (haMode) {
                    case Constants.HA_MODE_ZOOKEEPER:
                        // 基于ZooKeeper的动态服务发现模式（负载均衡）
                        if (StrUtil.isNotBlank(zkQuorum)) {
                            jdbcUrl = String.format(
                                    "jdbc:hive2://%s/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=%s",
                                    zkQuorum, zkNamespace);

                            // 高可用信息添加到基本信息中
                            basicInfo.put("HiveServer2高可用", "true");
                            basicInfo.put("高可用模式", "ZooKeeper服务发现(负载均衡)");
                        } else {
                            // 回退到单实例模式
                            jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                            basicInfo.put("HiveServer2高可用", "false");
                            basicInfo.put("高可用模式", "ZooKeeper服务发现(配置不完整)");
                        }
                        break;

                    case Constants.HA_MODE_ZOOKEEPER_HA:
                        // Active-Passive模式（主备切换）
                        if (StrUtil.isNotBlank(zkQuorum)) {
                            jdbcUrl = String.format(
                                    "jdbc:hive2://%s/;serviceDiscoveryMode=zooKeeperHA;zooKeeperNamespace=%s",
                                    zkQuorum, zkNamespace);

                            // 高可用信息添加到基本信息中
                            basicInfo.put("HiveServer2高可用", "true");
                            basicInfo.put("高可用模式", "ZooKeeper主备切换(Active-Passive)");
                        } else {
                            // 回退到单实例模式
                            jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                            basicInfo.put("HiveServer2高可用", "false");
                            basicInfo.put("高可用模式", "ZooKeeper主备切换(配置不完整)");
                        }
                        break;

                    case Constants.HA_MODE_HTTP:
                        // HTTP负载均衡模式
                        String loadBalancer = String
                                .valueOf(configMap.getOrDefault("hiveserver2.http.loadbalancer.hosts", ""));
                        String loadBalancerPort = String
                                .valueOf(configMap.getOrDefault("hiveserver2.http.loadbalancer.port", "10000"));

                        if (StrUtil.isNotBlank(loadBalancer)) {
                            jdbcUrl = "jdbc:hive2://" + loadBalancer + ":" + loadBalancerPort;

                            // 高可用信息添加到基本信息中
                            basicInfo.put("HiveServer2高可用", "true");
                            basicInfo.put("高可用模式", "HTTP负载均衡");
                            basicInfo.put("负载均衡地址", loadBalancer + ":" + loadBalancerPort);
                        } else {
                            // 回退到单实例模式
                            jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                            basicInfo.put("HiveServer2高可用", "false");
                            basicInfo.put("高可用模式", "HTTP负载均衡(配置不完整)");
                        }
                        break;

                    default:
                        // 默认模式，直接使用HiveServer2
                        jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                        basicInfo.put("HiveServer2高可用", "true");
                        basicInfo.put("高可用模式", "单实例模式");
                        break;
                }
            } else {
                // 非高可用模式
                jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                basicInfo.put("HiveServer2高可用", "false");
                basicInfo.put("高可用模式", "单实例模式");
            }

            // 添加Kerberos支持（如果启用）
            if (enableKerberos) {
                String principal = String.valueOf(configMap.getOrDefault(
                        "hive.server2.authentication.kerberos.principal",
                        "hive/" + hiveServer2Host + "@HADOOP.COM"));

                // 替换principal中的${host}为实际主机名
                if (principal.contains("${host}")) {
                    principal = principal.replace("${host}", hiveServer2Host);
                }

                jdbcUrl += ";principal=" + principal + ";auth=kerberos";
            }

            // 补充基本连接信息
            if (StrUtil.isNotBlank(metastoreUris)) {
                basicInfo.put("HiveMetastore地址", metastoreUris);
            }
            basicInfo.put("启用Kerberos", enableKerberos ? "true" : "false");

            // 调整基本信息顺序
            List<Map<String, String>> basicInfoList = new ArrayList<>();

            // 按照固定的顺序添加信息
            String[] orderedKeys = {
                    "HiveServer2高可用",
                    "高可用模式",
                    "HiveServer2主节点",
                    // 从节点将动态添加
                    "启用Kerberos",
                    "HiveMetastore地址",
                    "负载均衡地址" // 如果有的话
            };

            // 先按照固定顺序添加
            for (String key : orderedKeys) {
                if (basicInfo.containsKey(key)) {
                    Map<String, String> item = new HashMap<>(2);
                    item.put("label", key);
                    item.put("value", basicInfo.get(key));
                    basicInfoList.add(item);
                }
            }

            // 添加从节点信息（按照节点序号排序）
            List<String> slaveKeys = new ArrayList<>();
            for (String key : basicInfo.keySet()) {
                if (key.startsWith("HiveServer2从节点")) {
                    slaveKeys.add(key);
                }
            }

            // 确保按照数字顺序排序（HiveServer2从节点1, HiveServer2从节点2, ...）
            Collections.sort(slaveKeys, (a, b) -> {
                // 提取数字部分并比较
                try {
                    int numA = Integer.parseInt(a.substring("HiveServer2从节点".length()));
                    int numB = Integer.parseInt(b.substring("HiveServer2从节点".length()));
                    return Integer.compare(numA, numB);
                } catch (NumberFormatException e) {
                    return a.compareTo(b);
                }
            });

            // 按顺序添加从节点信息
            for (String key : slaveKeys) {
                Map<String, String> item = new HashMap<>(2);
                item.put("label", key);
                item.put("value", basicInfo.get(key));
                basicInfoList.add(item);

                // 将从节点信息插入到主节点后面（而不是放在最后）
                int mainNodeIndex = -1;
                for (int i = 0; i < basicInfoList.size(); i++) {
                    if (basicInfoList.get(i).get("label").equals("HiveServer2主节点")) {
                        mainNodeIndex = i;
                        break;
                    }
                }

                if (mainNodeIndex >= 0 && basicInfoList.size() > 1) {
                    // 移除刚添加的项
                    Map<String, String> lastItem = basicInfoList.remove(basicInfoList.size() - 1);
                    // 插入到主节点后面
                    basicInfoList.add(mainNodeIndex + 1, lastItem);
                }
            }

            // 确保只保留我们已经添加的信息
            Map<String, Boolean> processedKeys = new HashMap<>();
            for (Map<String, String> item : basicInfoList) {
                processedKeys.put(item.get("label"), true);
            }

            // 添加任何未处理的信息到列表末尾
            for (Map.Entry<String, String> entry : basicInfo.entrySet()) {
                if (!processedKeys.containsKey(entry.getKey())) {
                    Map<String, String> item = new HashMap<>(2);
                    item.put("label", entry.getKey());
                    item.put("value", entry.getValue());
                    basicInfoList.add(item);
                }
            }

            // 修正JDBC URL列表
            List<Map<String, String>> jdbcUrls = new ArrayList<>();

            // 先更新主节点和从节点信息
            if (enableHiveServer2HA && CollUtil.isNotEmpty(hiveServer2Hosts)) {
                // 按照主机名排序（保持稳定的显示顺序）
                List<String> sortedHosts = new ArrayList<>(hiveServer2Hosts);
                Collections.sort(sortedHosts);

                // 添加节点信息到basicInfo
                for (int i = 0; i < sortedHosts.size(); i++) {
                    String host = sortedHosts.get(i);
                    if (i == 0) {
                        basicInfo.put("HiveServer2主节点", host + ":" + hiveServer2Port);
                    } else {
                        basicInfo.put("HiveServer2从节点" + i, host + ":" + hiveServer2Port);
                    }
                }
            }

            // 对于ZooKeeper服务发现模式，使用ZooKeeper地址
            if (haMode.equals(Constants.HA_MODE_ZOOKEEPER) || haMode.equals(Constants.HA_MODE_ZOOKEEPER_HA)) {
                if (StrUtil.isNotBlank(zkQuorum)) {
                    // 主JDBC URL
                    Map<String, String> jdbcUrlItem = new HashMap<>(2);
                    jdbcUrlItem.put("label", "Hive JDBC URL");
                    jdbcUrlItem.put("value", jdbcUrl);
                    jdbcUrls.add(jdbcUrlItem);
                }
            } else {
                // 对于其他模式，使用原有逻辑
                Map<String, String> jdbcUrlItem = new HashMap<>(2);
                jdbcUrlItem.put("label", "Hive JDBC URL");
                jdbcUrlItem.put("value", jdbcUrl);
                jdbcUrls.add(jdbcUrlItem);
            }

            // 添加所有HiveServer2节点的URL
            if (enableHiveServer2HA && CollUtil.isNotEmpty(hiveServer2Hosts)) {
                // 按照主机名排序（保持稳定的显示顺序）
                List<String> sortedHosts = new ArrayList<>(hiveServer2Hosts);
                Collections.sort(sortedHosts);

                // 添加单节点URL
                for (int i = 0; i < sortedHosts.size(); i++) {
                    String host = sortedHosts.get(i);

                    // 添加单节点连接URL
                    String nodeUrl = "jdbc:hive2://" + host + ":" + hiveServer2Port;
                    if (enableKerberos) {
                        String principal = String.valueOf(configMap.getOrDefault(
                                "hive.server2.authentication.kerberos.principal",
                                "hive/" + host + "@HADOOP.COM"));
                        // 替换principal中的${host}为实际主机名
                        if (principal.contains("${host}")) {
                            principal = principal.replace("${host}", host);
                        }
                        nodeUrl += ";principal=" + principal + ";auth=kerberos";
                    }

                    Map<String, String> nodeUrlItem = new HashMap<>(2);
                    nodeUrlItem.put("label", "节点" + (i + 1) + " URL");
                    nodeUrlItem.put("value", nodeUrl);
                    jdbcUrls.add(nodeUrlItem);
                }
            }

            // 生成Java示例代码
            String javaCode = generateJavaCode(jdbcUrl, enableKerberos);

            // 生成Python示例代码
            String pythonCode = generatePythonCode(hiveServer2Host, hiveServer2Port, enableKerberos);

            // 生成Beeline命令行连接示例
            String beelineCommand = "beeline -u \"" + jdbcUrl + "\"";
            if (!enableKerberos) {
                beelineCommand += " -n <username> -p <password>";
            }

            List<Map<String, String>> commandLines = new ArrayList<>();
            Map<String, String> beelineItem = new HashMap<>(2);
            beelineItem.put("label", "Beeline命令");
            beelineItem.put("value", beelineCommand);
            commandLines.add(beelineItem);

            // 构建并返回ConnectionInfo对象
            return ConnectionInfo.builder()
                    .basicInfo(basicInfo)
                    .basicInfoList(basicInfoList)
                    .jdbcUrl(jdbcUrl)
                    .jdbcUrls(jdbcUrls)
                    .javaCode(javaCode)
                    .pythonCode(pythonCode)
                    .beelineCommand(beelineCommand)
                    .commandLines(commandLines)
                    .build();
        } catch (Exception e) {
            log.error("获取Hive连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder().build();
        }
    }

    /**
     * 生成Java示例代码
     */
    private String generateJavaCode(String jdbcUrl, boolean enableKerberos) {
        return "import java.sql.Connection;\n" +
                "import java.sql.DriverManager;\n" +
                "import java.sql.ResultSet;\n" +
                "import java.sql.Statement;\n\n" +
                "public class HiveJdbcClient {\n" +
                "    public static void main(String[] args) throws Exception {\n" +
                "        try {\n" +
                "            Class.forName(\"org.apache.hive.jdbc.HiveDriver\");\n" +
                "        } catch (ClassNotFoundException e) {\n" +
                "            e.printStackTrace();\n" +
                "            System.exit(1);\n" +
                "        }\n\n" +
                "        // JDBC URL\n" +
                "        String jdbcURL = \"" + jdbcUrl + "\";\n" +
                (enableKerberos ? "        // Kerberos认证需要设置以下系统属性\n" +
                        "        System.setProperty(\"java.security.krb5.conf\", \"/etc/krb5.conf\");\n" +
                        "        System.setProperty(\"javax.security.auth.useSubjectCredsOnly\", \"false\");\n"
                        : "")
                +
                "\n" +
                "        Connection conn = DriverManager.getConnection(jdbcURL);\n" +
                "        Statement stmt = conn.createStatement();\n" +
                "        String sql = \"SHOW DATABASES\";\n" +
                "        ResultSet rs = stmt.executeQuery(sql);\n" +
                "        while (rs.next()) {\n" +
                "            System.out.println(rs.getString(1));\n" +
                "        }\n" +
                "        rs.close();\n" +
                "        stmt.close();\n" +
                "        conn.close();\n" +
                "    }\n" +
                "}";
    }

    /**
     * 生成Python示例代码
     */
    private String generatePythonCode(String hiveServer2Host, String hiveServer2Port, boolean enableKerberos) {
        return "from pyhive import hive\n\n" +
                "# 连接Hive\n" +
                "conn = hive.Connection(\n" +
                "    host='" + hiveServer2Host + "',\n" +
                "    port=" + hiveServer2Port + ",\n" +
                (enableKerberos ? "    auth='KERBEROS',\n" +
                        "    kerberos_service_name='hive',\n" : "")
                +
                "    database='default'\n" +
                ")\n\n" +
                "# 创建游标\n" +
                "cursor = conn.cursor()\n\n" +
                "# 执行查询\n" +
                "cursor.execute('SHOW DATABASES')\n\n" +
                "# 获取结果\n" +
                "for result in cursor.fetchall():\n" +
                "    print(result[0])\n\n" +
                "# 关闭连接\n" +
                "cursor.close()\n" +
                "conn.close()";
    }

}
