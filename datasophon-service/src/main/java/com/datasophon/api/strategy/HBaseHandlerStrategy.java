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
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HBaseHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(HBaseHandlerStrategy.class);

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        boolean enableKerberos = false;
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
        for (ServiceConfig config : list) {
            if ("enableKerberos".equals(config.getName())) {
                enableKerberos = isEnableKerberos(clusterId, globalVariables, enableKerberos, config, "HBASE");
            }
        }
        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "HBASE" + Constants.CONFIG;
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
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId) {
        try {
            // 1. 获取服务配置
            List<ServiceConfig> serviceConfigs = listServiceConfigByServiceInstance(serviceInstanceId);

            // 2. 从配置中解析配置到map，方便快速查询
            Map<String, String> configMap = new HashMap<>();
            for (ServiceConfig config : serviceConfigs) {
                if (config.getValue() != null) {
                    configMap.put(config.getName(), String.valueOf(config.getValue()));
                }
            }

            // 3. 获取HBase Master和RegionServer节点列表
            List<String> masterList = getRoleHosts(clusterId, "HbaseMaster");

            // 如果没有找到Master节点，返回空信息
            if (CollUtil.isEmpty(masterList)) {
                logger.warn("未找到HBase Master节点，集群ID: {}", clusterId);
                return ConnectionInfo.builder().build();
            }

            // 4. 获取第一个Master节点作为主节点
            String masterNode = masterList.get(0);

            // 5. 判断是否启用了Kerberos
            boolean enableKerberos = false;
            for (ServiceConfig config : serviceConfigs) {
                if ("enableKerberos".equals(config.getName())) {
                    enableKerberos = isEnableConfig(config);
                    break;
                }
            }

            // 6. 获取ZooKeeper连接信息
            String zkQuorum = configMap.getOrDefault("hbase.zookeeper.quorum",
                    GlobalVariables.get(clusterId).getOrDefault("ZOOKEEPER_NODES", ""));
            String zkPort = configMap.getOrDefault("hbase.zookeeper.property.clientPort", "2181");
            String zkRootNode = configMap.getOrDefault("zookeeper.znode.parent", "/hbase");

            // 判断是否启用了高可用
            boolean isHA = false;

            // 1. 检查是否启用了分布式模式
            boolean isDistributed = "true"
                    .equalsIgnoreCase(configMap.getOrDefault("hbase.cluster.distributed", "false"));

            // 2. 检查ZooKeeper集群是否配置了多个节点
            boolean hasMultipleZK = zkQuorum.contains(",");

            // 3. 检查是否配置了多个Master节点
            boolean hasMultipleMasters = masterList.size() > 1;

            // 4. 检查是否显式启用了Master高可用
            boolean masterHAEnabled = "true"
                    .equalsIgnoreCase(configMap.getOrDefault("hbase.master.ha.enable", "false"));

            // 5. 检查是否启用了跨集群复制
            boolean replicationEnabled = "true".equalsIgnoreCase(configMap.getOrDefault("hbase.replication", "false"));

            // 综合判断是否高可用
            // 分布式模式是基础，同时需要满足以下条件之一：
            // 1. 配置了多个Master节点
            // 2. 显式启用了Master高可用
            // 3. 启用了跨集群复制
            isHA = isDistributed && (hasMultipleMasters || masterHAEnabled || replicationEnabled);

            // 记录高可用判断的详细信息
            logger.info("HBase高可用判断: 分布式模式={}, 多ZK节点={}, 多Master节点={}, Master高可用={}, 跨集群复制={}, 最终结果={}",
                    isDistributed, hasMultipleZK, hasMultipleMasters, masterHAEnabled, replicationEnabled, isHA);

            // 7. 构建基本连接信息
            Map<String, String> basicInfo = new HashMap<>();
            basicInfo.put("HBase主节点", masterNode + ":16000");
            basicInfo.put("ZooKeeper集群地址", zkQuorum);
            basicInfo.put("ZooKeeper客户端端口", zkPort);
            basicInfo.put("ZooKeeper根节点", zkRootNode);
            basicInfo.put("高可用", isHA ? "true" : "false");
            basicInfo.put("显式启用了Master高可用", masterHAEnabled ? "true" : "false");
            basicInfo.put("启用了跨集群复制", replicationEnabled ? "true" : "false");
            basicInfo.put("启用Kerberos", enableKerberos ? "true" : "false");

            // 8. 构建有序的基本连接信息列表（用于前端表格显示）
            List<Map<String, String>> basicInfoList = new ArrayList<>();

            // 按照固定的顺序添加信息
            String[] orderedKeys = {
                    "HBase主节点",
                    "ZooKeeper集群地址",
                    "ZooKeeper客户端端口",
                    "ZooKeeper根节点",
                    "高可用",
                    "显式启用了Master高可用",
                    "启用了跨集群复制",
                    "启用Kerberos"
            };

            // 按顺序添加到basicInfoList
            for (String key : orderedKeys) {
                if (basicInfo.containsKey(key)) {
                    Map<String, String> item = new HashMap<>();
                    item.put("label", key);
                    item.put("value", basicInfo.get(key));
                    basicInfoList.add(item);
                }
            }

            // 9. 获取服务主目录
            String hbaseHome = GlobalVariables.get(clusterId).getOrDefault("HBASE_HOME", "/opt/datasophon/hbase");

            // 10. 返回构建好的连接信息
            return ConnectionInfo.builder()
                    .basicInfo(basicInfo)
                    .basicInfoList(basicInfoList)
                    .javaCode(generateJavaCode(zkQuorum, zkPort, zkRootNode, enableKerberos))
                    .pythonCode(generatePythonCode(zkQuorum, zkPort, zkRootNode, enableKerberos))
                    .commandLines(generateCommandLines(hbaseHome, enableKerberos, masterNode))
                    .serviceHome(hbaseHome)
                    .hostName(masterNode)
                    .build();

        } catch (Exception e) {
            logger.error("获取HBase连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder().build();
        }
    }

    /**
     * 判断配置项是否启用
     */
    private boolean isEnableConfig(ServiceConfig config) {
        if (config.getValue() instanceof Boolean) {
            return (Boolean) config.getValue();
        } else if (config.getValue() instanceof String) {
            return "true".equalsIgnoreCase((String) config.getValue());
        }
        return false;
    }

    /**
     * 生成Java代码示例
     */
    private String generateJavaCode(String zkQuorum, String zkPort, String zkRootNode, boolean enableKerberos) {
        StringBuilder sb = new StringBuilder();
        sb.append("import org.apache.hadoop.conf.Configuration;\n");
        sb.append("import org.apache.hadoop.hbase.*;\n");
        sb.append("import org.apache.hadoop.hbase.client.*;\n");
        sb.append("import org.apache.hadoop.hbase.util.Bytes;\n");
        sb.append("\n");
        sb.append("public class HBaseClient {\n");
        sb.append("    public static void main(String[] args) throws Exception {\n");

        if (enableKerberos) {
            sb.append("        // 设置Kerberos认证\n");
            sb.append("        System.setProperty(\"java.security.krb5.conf\", \"/etc/krb5.conf\");\n");
            sb.append("        org.apache.hadoop.conf.Configuration conf = new Configuration();\n");
            sb.append("        conf.set(\"hadoop.security.authentication\", \"kerberos\");\n");
            sb.append("        conf.set(\"hbase.security.authentication\", \"kerberos\");\n");
            sb.append("        conf.set(\"hbase.master.kerberos.principal\", \"hbase/_HOST@EXAMPLE.COM\");\n");
            sb.append("        conf.set(\"hbase.regionserver.kerberos.principal\", \"hbase/_HOST@EXAMPLE.COM\");\n");
            sb.append("        // 使用kinit命令进行身份验证，或者使用keytab文件方式：\n");
            sb.append(
                    "        // UserGroupInformation.loginUserFromKeytab(\"user@EXAMPLE.COM\", \"/path/to/user.keytab\");\n");
        }

        sb.append("\n");
        sb.append("        // 创建HBase配置\n");
        sb.append("        Configuration config = HBaseConfiguration.create();\n");
        sb.append("        config.set(\"hbase.zookeeper.quorum\", \"").append(zkQuorum).append("\");\n");
        sb.append("        config.set(\"hbase.zookeeper.property.clientPort\", \"").append(zkPort).append("\");\n");
        sb.append("        config.set(\"zookeeper.znode.parent\", \"").append(zkRootNode).append("\");\n");
        sb.append("\n");
        sb.append("        // 创建连接\n");
        sb.append("        try (Connection connection = ConnectionFactory.createConnection(config)) {\n");
        sb.append("            // 获取Admin对象\n");
        sb.append("            try (Admin admin = connection.getAdmin()) {\n");
        sb.append("                // 列出所有表\n");
        sb.append("                TableName[] tableNames = admin.listTableNames();\n");
        sb.append("                System.out.println(\"HBase中的表：\");\n");
        sb.append("                for (TableName tableName : tableNames) {\n");
        sb.append("                    System.out.println(tableName.getNameAsString());\n");
        sb.append("                }\n");
        sb.append("\n");
        sb.append("                // 表是否存在\n");
        sb.append("                TableName testTable = TableName.valueOf(\"test\");\n");
        sb.append("                boolean exists = admin.tableExists(testTable);\n");
        sb.append("                System.out.println(\"test表是否存在: \" + exists);\n");
        sb.append("\n");
        sb.append("                // 创建表示例\n");
        sb.append("                if (!exists) {\n");
        sb.append("                    System.out.println(\"创建test表...\");\n");
        sb.append(
                "                    TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(testTable);\n");
        sb.append("                    \n");
        sb.append("                    // 添加列族\n");
        sb.append(
                "                    ColumnFamilyDescriptorBuilder cfBuilder = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(\"cf\"));\n");
        sb.append("                    cfBuilder.setMaxVersions(3);\n");
        sb.append("                    tableBuilder.setColumnFamily(cfBuilder.build());\n");
        sb.append("                    \n");
        sb.append("                    // 创建表\n");
        sb.append("                    admin.createTable(tableBuilder.build());\n");
        sb.append("                    System.out.println(\"表创建成功!\");\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("\n");
        sb.append("            // 表操作示例\n");
        sb.append("            try (Table table = connection.getTable(TableName.valueOf(\"test\"))) {\n");
        sb.append("                // 写入数据\n");
        sb.append("                Put put = new Put(Bytes.toBytes(\"row1\"));\n");
        sb.append(
                "                put.addColumn(Bytes.toBytes(\"cf\"), Bytes.toBytes(\"col1\"), Bytes.toBytes(\"value1\"));\n");
        sb.append("                table.put(put);\n");
        sb.append("\n");
        sb.append("                // 读取数据\n");
        sb.append("                Get get = new Get(Bytes.toBytes(\"row1\"));\n");
        sb.append("                Result result = table.get(get);\n");
        sb.append("                byte[] value = result.getValue(Bytes.toBytes(\"cf\"), Bytes.toBytes(\"col1\"));\n");
        sb.append("                System.out.println(\"获取到的值: \" + Bytes.toString(value));\n");
        sb.append("\n");
        sb.append("                // 扫描表\n");
        sb.append("                Scan scan = new Scan();\n");
        sb.append("                ResultScanner scanner = table.getScanner(scan);\n");
        sb.append("                System.out.println(\"表扫描结果:\");\n");
        sb.append("                for (Result scanResult : scanner) {\n");
        sb.append("                    System.out.println(\"行: \" + Bytes.toString(scanResult.getRow()));\n");
        sb.append("                }\n");
        sb.append("                scanner.close();\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 生成Python代码示例
     */
    private String generatePythonCode(String zkQuorum, String zkPort, String zkRootNode, boolean enableKerberos) {
        StringBuilder sb = new StringBuilder();
        sb.append("import happybase\n");

        if (enableKerberos) {
            sb.append("import os\n");
            sb.append("\n");
            sb.append("# 设置Kerberos认证\n");
            sb.append("os.environ['KRB5_CONFIG'] = '/etc/krb5.conf'\n");
            sb.append("# 需要先使用kinit命令进行身份验证或配置keytab\n");
            sb.append("# 例如: kinit -kt /path/to/user.keytab user@EXAMPLE.COM\n");
            sb.append("\n");
        }

        sb.append("\n");
        sb.append("# 连接HBase\n");
        sb.append("connection = happybase.Connection(\n");
        sb.append("    host='").append(zkQuorum.split(",")[0]).append("',\n");
        sb.append("    port=9090,  # 注意：这里使用ThriftServer端口，默认为9090\n");
        sb.append("    transport='buffered',\n");
        sb.append("    protocol='binary'\n");
        sb.append(")\n");
        sb.append("\n");
        sb.append("# 列出所有表\n");
        sb.append("print('HBase中的表：')\n");
        sb.append("for table_name in connection.tables():\n");
        sb.append("    print(table_name.decode('utf-8'))\n");
        sb.append("\n");
        sb.append("# 创建表(如果不存在)\n");
        sb.append("table_name = 'test'\n");
        sb.append("if table_name.encode('utf-8') not in connection.tables():\n");
        sb.append("    print(f'创建表 {table_name}')\n");
        sb.append("    connection.create_table(\n");
        sb.append("        table_name,\n");
        sb.append("        {'cf': dict(max_versions=3)}\n");
        sb.append("    )\n");
        sb.append("\n");
        sb.append("# 获取表对象\n");
        sb.append("table = connection.table(table_name)\n");
        sb.append("\n");
        sb.append("# 写入数据\n");
        sb.append("table.put(\n");
        sb.append("    b'row1',\n");
        sb.append("    {b'cf:col1': b'value1', b'cf:col2': b'value2'}\n");
        sb.append(")\n");
        sb.append("\n");
        sb.append("# 读取单行数据\n");
        sb.append("row = table.row(b'row1')\n");
        sb.append("print('读取row1的数据：')\n");
        sb.append("for key, value in row.items():\n");
        sb.append("    # 解码byte类型为字符串以便打印\n");
        sb.append("    print(f'{key.decode(\"utf-8\")} = {value.decode(\"utf-8\")}')\n");
        sb.append("\n");
        sb.append("# 扫描表\n");
        sb.append("print('\\n表扫描结果：')\n");
        sb.append("for key, data in table.scan():\n");
        sb.append("    print(f'行: {key.decode(\"utf-8\")}')\n");
        sb.append("    for column, value in data.items():\n");
        sb.append("        print(f'  {column.decode(\"utf-8\")} = {value.decode(\"utf-8\")}')\n");
        sb.append("\n");
        sb.append("# 关闭连接\n");
        sb.append("connection.close()\n");

        return sb.toString();
    }

    /**
     * 生成命令行示例
     */
    private List<CommandLineItem> generateCommandLines(String hbaseHome, boolean enableKerberos, String hostname) {
        List<CommandLineItem> commandLines = new ArrayList<>();

        // 确保有shell执行环境
        String shellPrefix = enableKerberos ? "kinit -kt /path/to/user.keytab user@EXAMPLE.COM && " : "";

        // 进入HBase Shell
        CommandLineItem shellCmd = new CommandLineItem();
        shellCmd.setLabel("进入HBase Shell");
        shellCmd.setValue(shellPrefix + "bin/hbase shell");
        shellCmd.setCommandResult(
                "HBase Shell\nUse \"help\" to get list of supported commands.\nVersion 2.5.3, ...\nhbase(main):001:0> ");
        shellCmd.setCommandPrompt(null);
        commandLines.add(shellCmd);

        // 列出所有表
        CommandLineItem listCmd = new CommandLineItem();
        listCmd.setLabel("列出所有表");
        listCmd.setValue("list");
        listCmd.setCommandResult("TABLE\ntest\ntest2\n2 row(s)\nTook 0.0498 seconds");
        listCmd.setCommandPrompt("hbase(main):002:0> ");
        commandLines.add(listCmd);

        // 创建表
        CommandLineItem createCmd = new CommandLineItem();
        createCmd.setLabel("创建表");
        createCmd.setValue("create 'mytable', 'cf1', 'cf2'");
        createCmd.setCommandResult("Created table mytable\nTook 1.0498 seconds\n=> Hbase::Table - mytable");
        createCmd.setCommandPrompt("hbase(main):003:0> ");
        commandLines.add(createCmd);

        // 查看表结构
        CommandLineItem describeCmd = new CommandLineItem();
        describeCmd.setLabel("查看表结构");
        describeCmd.setValue("describe 'mytable'");
        describeCmd.setCommandResult(
                "Table mytable is ENABLED\nmytable\nCOLUMN FAMILIES DESCRIPTION\ncf1 {NAME => 'cf1', BLOOMFILTER => 'ROW', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => 'FOREVER', COMPRESSION => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}\ncf2 {NAME => 'cf2', BLOOMFILTER => 'ROW', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => 'FOREVER', COMPRESSION => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}\n2 row(s)\nTook 0.0458 seconds");
        describeCmd.setCommandPrompt("hbase(main):004:0> ");
        commandLines.add(describeCmd);

        // 写入数据
        CommandLineItem putCmd = new CommandLineItem();
        putCmd.setLabel("写入数据");
        putCmd.setValue("put 'mytable', 'row1', 'cf1:col1', 'value1'");
        putCmd.setCommandResult("Took 0.0646 seconds");
        putCmd.setCommandPrompt("hbase(main):005:0> ");
        commandLines.add(putCmd);

        // 读取数据
        CommandLineItem getCmd = new CommandLineItem();
        getCmd.setLabel("读取数据");
        getCmd.setValue("get 'mytable', 'row1'");
        getCmd.setCommandResult(
                "COLUMN                CELL\ncf1:col1              timestamp=1636540888795, value=value1\n1 row(s)\nTook 0.0221 seconds");
        getCmd.setCommandPrompt("hbase(main):006:0> ");
        commandLines.add(getCmd);

        // 扫描表
        CommandLineItem scanCmd = new CommandLineItem();
        scanCmd.setLabel("扫描表");
        scanCmd.setValue("scan 'mytable'");
        scanCmd.setCommandResult(
                "ROW                   COLUMN+CELL\nrow1                  column=cf1:col1, timestamp=1636540888795, value=value1\n1 row(s)\nTook 0.0468 seconds");
        scanCmd.setCommandPrompt("hbase(main):007:0> ");
        commandLines.add(scanCmd);

        // 统计表行数
        CommandLineItem countCmd = new CommandLineItem();
        countCmd.setLabel("统计表行数");
        countCmd.setValue("count 'mytable'");
        countCmd.setCommandResult("1 row(s)\nTook 0.0421 seconds\n=> 1");
        countCmd.setCommandPrompt("hbase(main):008:0> ");
        commandLines.add(countCmd);

        // 禁用表
        CommandLineItem disableCmd = new CommandLineItem();
        disableCmd.setLabel("禁用表");
        disableCmd.setValue("disable 'mytable'");
        disableCmd.setCommandResult("Took 0.7459 seconds");
        disableCmd.setCommandPrompt("hbase(main):009:0> ");
        commandLines.add(disableCmd);

        // 删除表
        CommandLineItem dropCmd = new CommandLineItem();
        dropCmd.setLabel("删除表");
        dropCmd.setValue("drop 'mytable'");
        dropCmd.setCommandResult("Took 0.3564 seconds");
        dropCmd.setCommandPrompt("hbase(main):010:0> ");
        commandLines.add(dropCmd);

        // 退出HBase Shell
        CommandLineItem exitCmd = new CommandLineItem();
        exitCmd.setLabel("退出HBase Shell");
        exitCmd.setValue("exit");
        exitCmd.setCommandResult("Took 0.0012 seconds");
        exitCmd.setCommandPrompt("hbase(main):011:0> ");
        commandLines.add(exitCmd);

        // 命令行方式执行HBase命令
        CommandLineItem shellExecCmd = new CommandLineItem();
        shellExecCmd.setLabel("命令行方式执行HBase命令");
        shellExecCmd.setValue("bin/hbase org.apache.hadoop.hbase.client.RowCounter 'mytable'");
        shellExecCmd.setCommandResult("COUNTER: 1 row(s)");
        shellExecCmd.setCommandPrompt(null);
        commandLines.add(shellExecCmd);

        // 添加最后一个空命令，显示提示符
        CommandLineItem lastCmd = new CommandLineItem();
        lastCmd.setLabel("#");
        lastCmd.setValue(null);
        lastCmd.setCommandResult(null);
        lastCmd.setCommandPrompt(
                "[root@" + hostname + " " + hbaseHome.substring(hbaseHome.lastIndexOf('/') + 1) + "]# ");
        commandLines.add(lastCmd);

        // 添加通用命令
        return addFinalPrompt(commandLines, hbaseHome,hostname);
    }
}
