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
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;

import java.util.ArrayList;
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

    @Override
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId) {
        try {
            Map<String, String> globalVariables = GlobalVariables.get(clusterId);
            ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);

            // 判断是否启用了Kerberos
            boolean enableKerberos = isKerberosEnabled(clusterId);

            // 检查是否启用了HiveServer2高可用
            boolean enableHiveServer2HA = CacheUtils.get("enableHiveServer2HA") != null &&
                    (Boolean) CacheUtils.get("enableHiveServer2HA");
            String haMode = globalVariables.getOrDefault("${hive-ha-mode}", "none");

            // 基本信息收集
            Map<String, String> basicInfo = new HashMap<>();
            String hiveServer2Host = "";
            String hiveServer2Port = "10000"; // 默认端口
            String jdbcUrl = "";

            // 获取HiveServer2主机地址
            hiveServer2Host = globalVariables.getOrDefault("${masterHiveServer2}", "");
            if (StrUtil.isBlank(hiveServer2Host)) {
                hiveServer2Host = ProcessUtils.getServiceRoleHostname(clusterId, "HIVE", "HiveServer2");
            }

            // 如果仍然无法获取到Hive服务主机地址，则返回空对象
            if (StrUtil.isBlank(hiveServer2Host)) {
                log.warn("无法获取HiveServer2主机地址，集群ID: {}", clusterId);
                return ConnectionInfo.builder().build();
            }

            // 获取HiveMetastore地址
            String metastoreUris = globalVariables.getOrDefault("${hive.metastore.uris}", "");
            if (StrUtil.isBlank(metastoreUris)) {
                // 如果全局变量中没有metastore地址，则尝试构建一个
                String hiveMetastoreHost = globalVariables.getOrDefault("${hiveMetastore}", "");
                if (StrUtil.isBlank(hiveMetastoreHost)) {
                    hiveMetastoreHost = ProcessUtils.getServiceRoleHostname(clusterId, "HIVE", "HiveMetaStore");
                }

                if (StrUtil.isNotBlank(hiveMetastoreHost)) {
                    metastoreUris = "thrift://" + hiveMetastoreHost + ":9083";
                }
            }

            // 根据高可用模式生成JDBC URL
            if (enableHiveServer2HA) {
                if ("zookeeper".equalsIgnoreCase(haMode)) {
                    // ZooKeeper HA模式
                    String zkQuorum = globalVariables.getOrDefault("${zkQuorum}", "");
                    String zkHaNamespace = globalVariables.getOrDefault("${hiveserver2-ha-zk-namespace}",
                            "hiveserver2");

                    if (StrUtil.isNotBlank(zkQuorum)) {
                        jdbcUrl = String.format("jdbc:hive2://%s/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=%s",
                                zkQuorum, zkHaNamespace);

                        // 高可用信息添加到基本信息中
                        basicInfo.put("HiveServer2高可用", "是");
                        basicInfo.put("高可用模式", "ZooKeeper服务发现");
                        basicInfo.put("ZooKeeper命名空间", zkHaNamespace);
                    } else {
                        // 回退到单实例模式
                        jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                        basicInfo.put("HiveServer2高可用", "是(配置不完整)");
                    }
                } else if ("httpHA".equalsIgnoreCase(haMode)) {
                    // HTTP负载均衡模式
                    String loadBalancer = globalVariables.getOrDefault("${hiveserver2-ha-lb-hosts}", "");
                    String loadBalancerPort = globalVariables.getOrDefault("${hiveserver2-ha-lb-port}", "10000");

                    if (StrUtil.isNotBlank(loadBalancer)) {
                        jdbcUrl = "jdbc:hive2://" + loadBalancer + ":" + loadBalancerPort;

                        // 高可用信息添加到基本信息中
                        basicInfo.put("HiveServer2高可用", "是");
                        basicInfo.put("高可用模式", "HTTP负载均衡");
                    } else {
                        // 回退到单实例模式
                        jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                        basicInfo.put("HiveServer2高可用", "是(配置不完整)");
                    }
                } else {
                    // 默认模式，直接使用第一个HiveServer2
                    jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                    basicInfo.put("HiveServer2高可用", "是(使用单实例)");
                }
            } else {
                // 非高可用模式
                jdbcUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                basicInfo.put("HiveServer2高可用", "否");
            }

            // 添加Kerberos支持（如果启用）
            if (enableKerberos) {
                jdbcUrl += ";principal=hive/" + hiveServer2Host + "@HADOOP.COM;auth=kerberos";
            }

            // 补充基本连接信息
            basicInfo.put("HiveServer2地址", hiveServer2Host + ":" + hiveServer2Port);
            if (StrUtil.isNotBlank(metastoreUris)) {
                basicInfo.put("HiveMetastore地址", metastoreUris);
            }
            basicInfo.put("启用Kerberos", enableKerberos ? "是" : "否");

            // 添加基本连接信息（新格式 - List<Map<String, String>>）
            List<Map<String, String>> basicInfoList = new ArrayList<>();
            basicInfo.forEach((key, value) -> {
                Map<String, String> item = new HashMap<>(2);
                item.put("label", key);
                item.put("value", value);
                basicInfoList.add(item);
            });

            // 生成JDBC URL列表
            List<Map<String, String>> jdbcUrls = new ArrayList<>();
            Map<String, String> jdbcUrlItem = new HashMap<>(2);
            jdbcUrlItem.put("label", "Hive JDBC URL");
            jdbcUrlItem.put("value", jdbcUrl);
            jdbcUrls.add(jdbcUrlItem);

            // 如果是高可用，添加单实例连接URL作为备选
            if (enableHiveServer2HA && StrUtil.isNotBlank(hiveServer2Host)) {
                String directUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;
                if (enableKerberos) {
                    directUrl += ";principal=hive/" + hiveServer2Host + "@HADOOP.COM;auth=kerberos";
                }

                Map<String, String> directUrlItem = new HashMap<>(2);
                directUrlItem.put("label", "直连单节点URL (备用)");
                directUrlItem.put("value", directUrl);
                jdbcUrls.add(directUrlItem);
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

    /**
     * 判断Hive是否启用了Kerberos
     */
    private boolean isKerberosEnabled(Integer clusterId) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        return "true".equalsIgnoreCase(globalVariables.getOrDefault("${enable-hive-kerberos}", "false"));
    }

}
