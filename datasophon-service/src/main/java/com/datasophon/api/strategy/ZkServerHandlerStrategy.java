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

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.datasophon.api.utils.ProcessUtils.getDepMode;
import static com.datasophon.common.utils.HostUtils.generateHosts;

public class ZkServerHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ZkServerHandlerStrategy.class);

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        // 保存zkUrls到全局变量
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        String join = String.join(":2181,", hosts);
        String zkUrls = join + ":2181";
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${zkUrls}", zkUrls);
        // 保存hbaseZkUrls到全局变量
        String hbaseZkUrls = String.join(",", hosts);
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${zkHostsUrl}", hbaseZkUrls);
    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        boolean enableKerberos = false;
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);

        for (ServiceConfig config : list) {
            if ("enableKerberos".equals(config.getName())) {
                if ((Boolean) config.getValue()) {
                    enableKerberos = true;
                    ProcessUtils.generateClusterVariable(globalVariables, clusterId,
                            "${enableZOOKEEPERKerberos}",
                            "true");
                } else {
                    ProcessUtils.generateClusterVariable(globalVariables, clusterId,
                            "${enableZOOKEEPERKerberos}",
                            "false");
                }
            }
        }

        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "ZOOKEEPER" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);
        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
        if (enableKerberos) {
            for (ServiceConfig serviceConfig : configs) {
                if (serviceConfig.isConfigWithKerberos()) {
                    if (map.containsKey(serviceConfig.getName())) {
                        ServiceConfig config = map.get(serviceConfig.getName());
                        config.setRequired(true);
                        config.setHidden(false);
                        String value = PlaceholderUtils.replacePlaceholders(
                                (String) serviceConfig.getValue(),
                                globalVariables, Constants.REGEX_VARIABLE);
                        logger.info("the value is {}", value);
                        config.setValue(value);
                    } else {
                        serviceConfig.setRequired(true);
                        serviceConfig.setHidden(false);
                        String value = PlaceholderUtils.replacePlaceholders(
                                (String) serviceConfig.getValue(),
                                globalVariables, Constants.REGEX_VARIABLE);
                        serviceConfig.setValue(value);
                        kbConfigs.add(serviceConfig);
                    }
                }
            }
        } else {
            for (ServiceConfig serviceConfig : configs) {
                if (serviceConfig.isConfigWithKerberos()) {
                    if (map.containsKey(serviceConfig.getName())) {
                        list.remove(map.get(serviceConfig.getName()));
                    }
                }
            }
        }
        list.addAll(kbConfigs);
    }

    /**
     * @param clusterId 集群ID
     * @param list      服务配置列表
     */
    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        // add server.x config
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext()
                .getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE
                + Constants.SERVICE_ROLE_HOST_MAPPING;
        // HashMap<String, List<String>> hostMap = (HashMap<String, List<String>>)
        // CacheOperateUtils.get(hostMapKey);
        HashMap<String, List<String>> hostMap = CacheOperateUtils.getWithType(
                hostMapKey,
                new TypeReference<HashMap<String, List<String>>>() {
                });
        if (Objects.nonNull(hostMap)) {
            List<String> zkServers = hostMap.get("ZkServer");

            String depMode = getDepMode(clusterId);

            if (!Constants.PVM_MODE.equals(depMode)) {
                zkServers = generateHosts(zkServers, "zookeeper-zkserver");
            }

            Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);

            Integer myid = 1;
            for (String server : zkServers) {
                ServiceConfig serviceConfig = new ServiceConfig();
                serviceConfig.setName("server." + myid);
                serviceConfig.setLabel("server." + myid);
                // TODO:
                // 在PVM环境中使用域名通信，在K8S中使用DNS域名通信，避免直接使用IP地址。为了提高系统的灵活性和可维护性，因为直接使用IP地址可能会导致在IP变更时需要大量修改配置，而使用域名可以通过DNS解析动态获取IP，减少维护成本。
                serviceConfig.setValue(server + ":2888:3888");
                serviceConfig.setHidden(false);
                serviceConfig.setRequired(true);
                serviceConfig.setType("input");
                serviceConfig.setDefaultValue("");
                serviceConfig.setConfigType("zkserver");
                if (map.containsKey("server." + myid)) {
                    logger.info("set zk server {}", myid);
                    ServiceConfig config = map.get("server." + myid);
                    BeanUtils.copyProperties(serviceConfig, config);
                } else {
                    logger.info("add zk server.x config");
                    list.add(serviceConfig);
                }
                CacheUtils.put("zkserver_" + server, myid);
                myid++;
            }
            /*
             * ServiceConfig clusterIp = map.get(K8S_CLUSTER_IP);
             * ArrayList<Map<String, String>> clusterIpLists = new ArrayList<>();
             * clusterIpLists.add(new HashMap<String, String>() {{
             * put("zookeeper-zkserver", "2181");
             * }});
             * clusterIpLists.add(new HashMap<String, String>() {{
             * put("zookeeper-zkserver", "2888");
             * }});
             * clusterIpLists.add(new HashMap<String, String>() {{
             * put("zookeeper-zkserver", "3888");
             * }});
             * clusterIp.setValue(clusterIpLists);
             * ServiceConfig targetPort = map.get(K8S_NODE_PORT);
             *
             * ArrayList<Map<String, String>> targetPortLists = new ArrayList<>();
             * targetPortLists.add(new HashMap<String, String>() {{
             * put("zookeeper-zkserver", "2181:32181");
             * }});
             * targetPort.setValue(targetPortLists);
             */
        }
    }

    @Override
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId,String serviceHome,Map<String, String> configMap) {
        try {
            // 1. 获取服务配置
            // 3. 获取ZooKeeper节点列表
            List<String> zkServerList = getRoleHosts(clusterId, serviceInstanceId, "ZkServer");
            if (zkServerList == null || zkServerList.isEmpty()) {
                logger.warn("未找到ZooKeeper节点，集群ID: {}", clusterId);
                return ConnectionInfo.builder().build();
            }

            // 4. 获取全局变量
            Map<String, String> globalVariables = GlobalVariables.get(clusterId);

            // 5. 判断是否启用了Kerberos
            boolean enableKerberos = false;
            // 从配置映射中获取Kerberos启用状态
            if (configMap.containsKey("enableKerberos")) {
                enableKerberos = Boolean.parseBoolean(configMap.get("enableKerberos"));
            }

            // 6. 获取ZooKeeper端口，默认为2181
            String zkPort = "2181";

            // 7. 构建ZooKeeper连接字符串
            StringBuilder zkConnectString = new StringBuilder();
            for (int i = 0; i < zkServerList.size(); i++) {
                String zkServer = zkServerList.get(i);
                zkConnectString.append(zkServer).append(":").append(zkPort);
                if (i < zkServerList.size() - 1) {
                    zkConnectString.append(",");
                }
            }

            // 8. 构建基本连接信息
            Map<String, String> basicInfo = new HashMap<>();
            basicInfo.put("ZooKeeper连接字符串", zkConnectString.toString());
            basicInfo.put("客户端端口", zkPort);
            basicInfo.put("节点数量", String.valueOf(zkServerList.size()));
            basicInfo.put("启用Kerberos", enableKerberos ? "是" : "否");

            // 9. 构建有序的基本连接信息列表（用于前端表格显示）
            List<Map<String, String>> basicInfoList = new ArrayList<>();

            // ZooKeeper连接字符串
            Map<String, String> connectStringItem = new HashMap<>();
            connectStringItem.put("label", "ZooKeeper连接字符串");
            connectStringItem.put("value", zkConnectString.toString());
            basicInfoList.add(connectStringItem);

            // 客户端端口
            Map<String, String> portItem = new HashMap<>();
            portItem.put("label", "客户端端口");
            portItem.put("value", zkPort);
            basicInfoList.add(portItem);

            // 节点数量
            Map<String, String> nodeCountItem = new HashMap<>();
            nodeCountItem.put("label", "节点数量");
            nodeCountItem.put("value", String.valueOf(zkServerList.size()));
            basicInfoList.add(nodeCountItem);

            // Kerberos状态
            Map<String, String> kerberosItem = new HashMap<>();
            kerberosItem.put("label", "启用Kerberos");
            kerberosItem.put("value", enableKerberos ? "是" : "否");
            basicInfoList.add(kerberosItem);

            // 10. 生成Java代码示例
            String javaCode = generateJavaCode(zkConnectString.toString(), enableKerberos);

            // 11. 生成Python代码示例
            String pythonCode = generatePythonCode(zkConnectString.toString(), enableKerberos);

            // 12. 获取ZooKeeper安装目录
            String zkHome = globalVariables.get("${ZOOKEEPER_HOME}");

            // 13. 生成命令行示例
            List<CommandLineItem> commandLines = generateCommandLines(serviceHome, zkConnectString.toString(),
                    zkServerList.get(0), enableKerberos);

            // 14. 返回构建好的连接信息
            return ConnectionInfo.builder()
                    .basicInfo(basicInfo)
                    .basicInfoList(basicInfoList)
                    .javaCode(javaCode)
                    .pythonCode(pythonCode)
                    .commandLines(commandLines)

                    .hostName(zkServerList.get(0))
                    .build();
        } catch (Exception e) {
            logger.error("获取ZooKeeper连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder().build();
        }
    }

    /**
     * 生成Java示例代码
     *
     * @param zkConnectString ZooKeeper连接字符串
     * @param enableKerberos  是否启用Kerberos
     * @return Java代码示例
     */
    private String generateJavaCode(String zkConnectString, boolean enableKerberos) {
        StringBuilder code = new StringBuilder();
        code.append("// ZooKeeper Java API 示例代码\n")
                .append("import org.apache.zookeeper.*;\n")
                .append("import java.util.concurrent.CountDownLatch;\n")
                .append("import java.nio.charset.StandardCharsets;\n\n");

        if (enableKerberos) {
            code.append("// Kerberos配置\n")
                    .append("import javax.security.auth.login.AppConfigurationEntry;\n")
                    .append("import javax.security.auth.login.Configuration;\n\n");
        }

        code.append("public class ZookeeperExample {\n")
                .append("    private static final String CONNECT_STRING = \"").append(zkConnectString)
                .append("\";\n")
                .append("    private static final int SESSION_TIMEOUT = 30000;\n\n")
                .append("    public static void main(String[] args) {\n")
                .append("        final CountDownLatch connectedSignal = new CountDownLatch(1);\n\n");

        if (enableKerberos) {
            code.append("        // 设置Kerberos认证\n")
                    .append("        System.setProperty(\"java.security.auth.login.config\", \"/path/to/jaas.conf\");\n")
                    .append("        System.setProperty(\"java.security.krb5.conf\", \"/etc/krb5.conf\");\n\n");
        }

        code.append("        try {\n")
                .append("            // 创建ZooKeeper客户端\n")
                .append("            ZooKeeper zk = new ZooKeeper(CONNECT_STRING, SESSION_TIMEOUT, new Watcher() {\n")
                .append("                @Override\n")
                .append("                public void process(WatchedEvent event) {\n")
                .append("                    if (event.getState() == Event.KeeperState.SyncConnected) {\n")
                .append("                        connectedSignal.countDown();\n")
                .append("                        System.out.println(\"已连接到ZooKeeper服务器\");\n")
                .append("                    }\n")
                .append("                }\n")
                .append("            });\n\n")
                .append("            // 等待连接建立\n")
                .append("            connectedSignal.await();\n\n")
                .append("            // 创建节点\n")
                .append("            String path = \"/example\";\n")
                .append("            if (zk.exists(path, false) == null) {\n")
                .append("                String createdPath = zk.create(path, \"节点数据\".getBytes(StandardCharsets.UTF_8),\n")
                .append("                                            ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);\n")
                .append("                System.out.println(\"创建节点成功：\" + createdPath);\n")
                .append("            }\n\n")
                .append("            // 获取节点数据\n")
                .append("            byte[] data = zk.getData(path, false, null);\n")
                .append("            System.out.println(\"节点数据：\" + new String(data, StandardCharsets.UTF_8));\n\n")
                .append("            // 设置节点数据\n")
                .append("            zk.setData(path, \"更新的数据\".getBytes(StandardCharsets.UTF_8), -1);\n")
                .append("            data = zk.getData(path, false, null);\n")
                .append("            System.out.println(\"更新后的节点数据：\" + new String(data, StandardCharsets.UTF_8));\n\n")
                .append("            // 获取子节点\n")
                .append("            String childPath = path + \"/child\";\n")
                .append("            zk.create(childPath, \"子节点数据\".getBytes(StandardCharsets.UTF_8),\n")
                .append("                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);\n\n")
                .append("            // 列出子节点\n")
                .append("            System.out.println(\"子节点列表：\");\n")
                .append("            for (String child : zk.getChildren(path, false)) {\n")
                .append("                System.out.println(child);\n")
                .append("            }\n\n")
                .append("            // 删除节点\n")
                .append("            zk.delete(childPath, -1);\n")
                .append("            zk.delete(path, -1);\n")
                .append("            System.out.println(\"节点删除成功\");\n\n")
                .append("            // 关闭连接\n")
                .append("            zk.close();\n\n")
                .append("        } catch (Exception e) {\n")
                .append("            e.printStackTrace();\n")
                .append("        }\n")
                .append("    }\n")
                .append("}\n");

        if (enableKerberos) {
            code.append("\n// jaas.conf示例内容\n")
                    .append("/*\n")
                    .append("Client {\n")
                    .append("    com.sun.security.auth.module.Krb5LoginModule required\n")
                    .append("    useKeyTab=true\n")
                    .append("    storeKey=true\n")
                    .append("    keyTab=\"/path/to/zookeeper.keytab\"\n")
                    .append("    principal=\"zookeeper/host@REALM\";\n")
                    .append("};\n")
                    .append("*/\n");
        }

        return code.toString();
    }

    /**
     * 生成Python示例代码
     *
     * @param zkConnectString ZooKeeper连接字符串
     * @param enableKerberos  是否启用Kerberos
     * @return Python代码示例
     */
    private String generatePythonCode(String zkConnectString, boolean enableKerberos) {
        StringBuilder code = new StringBuilder();
        code.append("# ZooKeeper Python API 示例代码\n")
                .append("# 需要安装以下包：\n")
                .append("# pip install kazoo\n\n")
                .append("from kazoo.client import KazooClient\n")
                .append("from kazoo.exceptions import NodeExistsError\n")
                .append("import time\n\n");

        if (enableKerberos) {
            code.append("# Kerberos认证设置\n")
                    .append("import os\n")
                    .append("os.environ['KRB5_CONFIG'] = '/etc/krb5.conf'\n")
                    .append("os.environ['JAVA_HOME'] = '/path/to/java_home'  # 需要设置Java环境\n")
                    .append("os.environ['JAAS_CONF'] = '/path/to/jaas.conf'  # JAAS配置文件\n\n");
        }

        code.append("# 连接到ZooKeeper服务器\n")
                .append("zk = KazooClient(\n")
                .append("    hosts='").append(zkConnectString).append("',\n");

        if (enableKerberos) {
            code.append("    # Kerberos设置 - 需要特殊的客户端支持\n")
                    .append("    sasl_options={\n")
                    .append("        'mechanism': 'GSSAPI',\n")
                    .append("        'service': 'zookeeper',\n")
                    .append("        'principalWithRealm': 'zookeeper/host@REALM'\n")
                    .append("    },\n");
        }

        code.append("    timeout=10.0\n")
                .append(")\n\n")
                .append("# 启动连接\n")
                .append("zk.start()\n")
                .append("print(\"已连接到ZooKeeper服务器\")\n\n")
                .append("try:\n")
                .append("    # 创建节点\n")
                .append("    path = \"/example\"\n")
                .append("    if not zk.exists(path):\n")
                .append("        zk.create(path, b\"节点数据\")\n")
                .append("        print(f\"创建节点成功：{path}\")\n\n")
                .append("    # 获取节点数据\n")
                .append("    data, stat = zk.get(path)\n")
                .append("    print(f\"节点数据：{data.decode('utf-8')}\")\n\n")
                .append("    # 设置节点数据\n")
                .append("    zk.set(path, b\"更新的数据\")\n")
                .append("    data, stat = zk.get(path)\n")
                .append("    print(f\"更新后的节点数据：{data.decode('utf-8')}\")\n\n")
                .append("    # 创建子节点\n")
                .append("    child_path = path + \"/child\"\n")
                .append("    try:\n")
                .append("        zk.create(child_path, b\"子节点数据\")\n")
                .append("        print(f\"创建子节点成功：{child_path}\")\n")
                .append("    except NodeExistsError:\n")
                .append("        print(f\"节点已存在：{child_path}\")\n\n")
                .append("    # 列出子节点\n")
                .append("    children = zk.get_children(path)\n")
                .append("    print(\"子节点列表：\")\n")
                .append("    for child in children:\n")
                .append("        print(child)\n\n")
                .append("    # 删除节点\n")
                .append("    zk.delete(child_path)\n")
                .append("    zk.delete(path)\n")
                .append("    print(\"节点删除成功\")\n\n")
                .append("except Exception as e:\n")
                .append("    print(f\"发生错误：{e}\")\n\n")
                .append("finally:\n")
                .append("    # 关闭连接\n")
                .append("    zk.stop()\n")
                .append("    zk.close()\n");

        return code.toString();
    }

    /**
     * 生成命令行示例
     *
     * @param zkHome          ZooKeeper安装目录
     * @param zkConnectString ZooKeeper连接字符串
     * @param hostname        主机名
     * @param enableKerberos  是否启用Kerberos
     * @return 命令行示例列表
     */
    private List<CommandLineItem> generateCommandLines(String zkHome, String zkConnectString, String hostname,
                                                       boolean enableKerberos) {
        List<CommandLineItem> commandLines = new ArrayList<>();

        // 命令提示符
        String serviceName = zkHome != null ? zkHome.substring(zkHome.lastIndexOf('/') + 1) : "zookeeper";
        String shellPrompt = "[root@" + hostname + " " + serviceName + "]# ";

        // 1. 查看ZooKeeper服务状态
        CommandLineItem statusCmd = CommandLineItem.builder()
                .label("查看ZooKeeper服务状态")
                .value("bin/zkServer.sh status")
                .commandPrompt(shellPrompt)
                .commandResult(
                        "ZooKeeper JMX enabled by default\n" +
                                "Using config: /etc/zookeeper/conf/zoo.cfg\n" +
                                "Client port found: 2181. Client address: 0.0.0.0. Client SSL: false.\n"
                                +
                                "Mode: follower")
                .build();
        commandLines.add(statusCmd);

        // 2. 连接ZooKeeper服务
        StringBuilder connectCmd = new StringBuilder("bin/zkCli.sh -server ").append(zkConnectString);
        if (enableKerberos) {
            connectCmd.append(" -Djava.security.auth.login.config=/path/to/jaas.conf");
        }
        CommandLineItem connectCmdItem = CommandLineItem.builder()
                .label("连接ZooKeeper服务")
                .value(connectCmd.toString())
                .commandPrompt(shellPrompt)
                .commandResult(
                        "Connecting to " + zkConnectString + "\n" +
                                "Welcome to ZooKeeper!\n" +
                                "JLine support is enabled\n" +
                                "WATCHER::\n" +
                                "WatchedEvent state:SyncConnected type:None path:null\n")
                .build();
        commandLines.add(connectCmdItem);

        // ZooKeeper CLI提示符
        String zkCliPrompt = "[zk: " + zkConnectString + "(CONNECTED) 0] ";

        // 3. 创建ZNode
        CommandLineItem createCmd = CommandLineItem.builder()
                .label("创建ZNode")
                .value("create /mynode mydata")
                .commandPrompt(zkCliPrompt)
                .commandResult("Created /mynode")
                .build();
        commandLines.add(createCmd);

        // 4. 查看ZNode数据
        CommandLineItem getCmd = CommandLineItem.builder()
                .label("查看ZNode数据")
                .value("get /mynode")
                .commandPrompt(zkCliPrompt)
                .commandResult(
                        "mydata\n" +
                                "cZxid = 0x100000003\n" +
                                "ctime = Fri May 12 10:00:00 CST 2023\n" +
                                "mZxid = 0x100000003\n" +
                                "mtime = Fri May 12 10:00:00 CST 2023\n" +
                                "pZxid = 0x100000003\n" +
                                "cversion = 0\n" +
                                "dataVersion = 0\n" +
                                "aclVersion = 0\n" +
                                "ephemeralOwner = 0x0\n" +
                                "dataLength = 6\n" +
                                "numChildren = 0")
                .build();
        commandLines.add(getCmd);

        // 5. 设置ZNode数据
        CommandLineItem setCmd = CommandLineItem.builder()
                .label("设置ZNode数据")
                .value("set /mynode newdata")
                .commandPrompt(zkCliPrompt)
                .commandResult(
                        "cZxid = 0x100000003\n" +
                                "ctime = Fri May 12 10:00:00 CST 2023\n" +
                                "mZxid = 0x100000004\n" +
                                "mtime = Fri May 12 10:15:00 CST 2023\n" +
                                "pZxid = 0x100000003\n" +
                                "cversion = 0\n" +
                                "dataVersion = 1\n" +
                                "aclVersion = 0\n" +
                                "ephemeralOwner = 0x0\n" +
                                "dataLength = 7\n" +
                                "numChildren = 0")
                .build();
        commandLines.add(setCmd);

        // 6. 列出ZNode子节点
        CommandLineItem lsCmd = CommandLineItem.builder()
                .label("列出ZNode子节点")
                .value("ls /")
                .commandPrompt(zkCliPrompt)
                .commandResult("[zookeeper, mynode, kafka, hbase]")
                .build();
        commandLines.add(lsCmd);

        // 7. 创建顺序节点
        CommandLineItem createSeqCmd = CommandLineItem.builder()
                .label("创建顺序节点")
                .value("create -s /mynode/seq sequence")
                .commandPrompt(zkCliPrompt)
                .commandResult("Created /mynode/seq0000000000")
                .build();
        commandLines.add(createSeqCmd);

        // 8. 创建临时节点
        CommandLineItem createEphCmd = CommandLineItem.builder()
                .label("创建临时节点")
                .value("create -e /mynode/temp ephemeral")
                .commandPrompt(zkCliPrompt)
                .commandResult("Created /mynode/temp")
                .build();
        commandLines.add(createEphCmd);

        // 9. 查看ZNode详细信息
        CommandLineItem statCmd = CommandLineItem.builder()
                .label("查看ZNode详细信息")
                .value("stat /mynode")
                .commandPrompt(zkCliPrompt)
                .commandResult(
                        "cZxid = 0x100000003\n" +
                                "ctime = Fri May 12 10:00:00 CST 2023\n" +
                                "mZxid = 0x100000004\n" +
                                "mtime = Fri May 12 10:15:00 CST 2023\n" +
                                "pZxid = 0x100000007\n" +
                                "cversion = 2\n" +
                                "dataVersion = 1\n" +
                                "aclVersion = 0\n" +
                                "ephemeralOwner = 0x0\n" +
                                "dataLength = 7\n" +
                                "numChildren = 2")
                .build();
        commandLines.add(statCmd);

        // 10. 删除ZNode
        CommandLineItem deleteCmd = CommandLineItem.builder()
                .label("删除ZNode")
                .value("delete /mynode/seq0000000000")
                .commandPrompt(zkCliPrompt)
                .commandResult("")
                .build();
        commandLines.add(deleteCmd);

        // 11. 递归删除ZNode
        CommandLineItem deleteallCmd = CommandLineItem.builder()
                .label("递归删除ZNode")
                .value("deleteall /mynode")
                .commandPrompt(zkCliPrompt)
                .commandResult("")
                .build();
        commandLines.add(deleteallCmd);

        // 12. 查看ZooKeeper服务器列表
        CommandLineItem confCmd = CommandLineItem.builder()
                .label("查看ZooKeeper服务器列表")
                .value("config")
                .commandPrompt(zkCliPrompt)
                .commandResult("server.1=" + zkConnectString.split(",")[0] + ":2888:3888:participant\n"
                        +
                        (zkConnectString.contains(",")
                                ? "server.2=" + zkConnectString.split(",")[1]
                                + ":2888:3888:participant\n"
                                : "")
                        +
                        (zkConnectString.split(",").length > 2
                                ? "server.3=" + zkConnectString.split(",")[2]
                                + ":2888:3888:participant"
                                : ""))
                .build();
        commandLines.add(confCmd);

        // 13. 查看ZooKeeper配额信息
        CommandLineItem listquotaCmd = CommandLineItem.builder()
                .label("查看ZooKeeper配额信息")
                .value("listquota /mynode")
                .commandPrompt(zkCliPrompt)
                .commandResult("absolute path is /zookeeper/quota/mynode/zookeeper_limits\n" +
                        "Output quota for /mynode count=10,bytes=1024\n" +
                        "Output stat for /mynode count=2,bytes=16")
                .build();
        commandLines.add(listquotaCmd);

        // 14. 设置ZooKeeper配额
        CommandLineItem setquotaCmd = CommandLineItem.builder()
                .label("设置ZooKeeper配额")
                .value("setquota -n 10 -b 1024 /mynode")
                .commandPrompt(zkCliPrompt)
                .commandResult("")
                .build();
        commandLines.add(setquotaCmd);

        // 15. 查看ZNode ACL信息
        CommandLineItem getaclCmd = CommandLineItem.builder()
                .label("查看ZNode ACL信息")
                .value("getAcl /mynode")
                .commandPrompt(zkCliPrompt)
                .commandResult("'world,'anyone\n" +
                        ": cdrwa")
                .build();
        commandLines.add(getaclCmd);

        // 16. 退出ZooKeeper客户端
        // 获取当前时间（年份为当前年份）
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String currentYear = String.valueOf(now.getYear());
        String currentMonth = String.format("%02d", now.getMonthValue());
        String currentDay = String.format("%02d", now.getDayOfMonth());

        // 为了让时间有差异，生成几个不同的时间点
        java.time.LocalDateTime time1 = now.minusSeconds(45);
        java.time.LocalDateTime time2 = now.minusSeconds(44);

        String timeStr1 = String.format("%s-%s-%s %02d:%02d:%02d",
                currentYear, currentMonth, currentDay,
                time1.getHour(), time1.getMinute(), time1.getSecond());

        String timeStr2 = String.format("%s-%s-%s %02d:%02d:%02d",
                currentYear, currentMonth, currentDay,
                time2.getHour(), time2.getMinute(), time2.getSecond());

        CommandLineItem quitCmd = CommandLineItem.builder()
                .label("退出ZooKeeper客户端")
                .value("quit")
                .commandPrompt(zkCliPrompt)
                .commandResult("WATCHER::\n\n" +
                        "WatchedEvent state:Closed type:None path:null\n" +
                        timeStr1
                        + ",713 [myid:] - INFO  [main:ZooKeeper@1422] - Session: 0x10a8fdd22602a6 closed\n"
                        +
                        timeStr2
                        + ",713 [myid:] - INFO  [main-EventThread:ClientCnxn$EventThread@521] - EventThread shut down for session: 0x10a8fdd22602a6\n"
                        +
                        "You have mail in /var/spool/mail/root")
                .build();
        commandLines.add(quitCmd);

        return addFinalPrompt(commandLines, zkHome, hostname);
    }
}
