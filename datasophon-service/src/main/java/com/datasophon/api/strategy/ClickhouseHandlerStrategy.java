package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
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
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> hostMap = CacheOperateUtils.getWithType(hostMapKey, new TypeReference<HashMap<String, List<String>>>() {});

        if (Objects.nonNull(hostMap)) {
            List<String> hostList = hostMap.get("ClickHouse");
            for (ServiceConfig serviceConfig : list) {
                if ("ckShardAddress".equals(serviceConfig.getName())) {
                    serviceConfig.setValue(hostList.stream().map(t -> t + ":9010").collect(Collectors.toList()));
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
    /**
     * 获取ClickHouse连接信息
     * 
     * @param clusterId         集群ID
     * @param serviceInstanceId 服务实例ID
     * @return 连接信息
     */
    @Override
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId,String serviceHome,Map<String, String> configMap) {
        try {
            // 1. 获取服务配置

            // 3. 获取ClickHouse节点列表
            List<String> clickhouseNodes = getRoleHosts(clusterId, serviceInstanceId, "ClickHouse");

            // 如果没有找到ClickHouse节点，返回空信息
            if (CollUtil.isEmpty(clickhouseNodes)) {
                log.warn("未找到ClickHouse节点，集群ID: {}", clusterId);
                return ConnectionInfo.builder().build();
            }

            // 4. 获取端口配置
            String httpPort = configMap.getOrDefault("http_port", "8123");
            String tcpPort = configMap.getOrDefault("tcp_port", "9000");
            String mysqlPort = configMap.getOrDefault("mysql_port", "9004");

            // 5. 判断是否启用了安全认证
            boolean enableSecurity = "true".equalsIgnoreCase(configMap.getOrDefault("enable_security", "false"));
            String securityUser = configMap.getOrDefault("default_user", "default");
            String securityPassword = configMap.getOrDefault("default_password", "");
            
            // 6. 获取数据库名称
            String databaseName = configMap.getOrDefault("default_database", "default");

            // 7. 构建连接地址
            // 构建HTTP连接地址
            String httpAddresses = clickhouseNodes.stream()
                    .map(node -> node + ":" + httpPort)
                    .collect(Collectors.joining(","));

            // 构建TCP连接地址
            String tcpAddresses = clickhouseNodes.stream()
                    .map(node -> node + ":" + tcpPort)
                    .collect(Collectors.joining(","));
            
            // 构建MySQL协议连接地址
            String mysqlAddresses = clickhouseNodes.stream()
                    .map(node -> node + ":" + mysqlPort)
                    .collect(Collectors.joining(","));

            // 8. 构建基本连接信息
            Map<String, String> basicInfo = new HashMap<>();
            basicInfo.put("数据库名称", databaseName);
            basicInfo.put("HTTP接口", httpAddresses);
            basicInfo.put("TCP接口", tcpAddresses);
            basicInfo.put("MySQL接口", mysqlAddresses);
            basicInfo.put("节点列表", StrUtil.join(",", clickhouseNodes));
            basicInfo.put("安全认证", enableSecurity ? "是" : "否");

            // 9. 构建有序的基本连接信息列表（用于前端表格显示）
            List<Map<String, String>> basicInfoList = new ArrayList<>();
            String[] orderedKeys = {
                    "数据库名称", "HTTP接口", "TCP接口", "MySQL接口", "节点列表", "安全认证"
            };

            for (String key : orderedKeys) {
                if (basicInfo.containsKey(key)) {
                    Map<String, String> item = new HashMap<>();
                    item.put("label", key);
                    item.put("value", basicInfo.get(key));
                    basicInfoList.add(item);
                }
            }

            // 10. 构建完整的连接信息
            return ConnectionInfo.builder()
                    .basicInfo(basicInfo)
                    .basicInfoList(basicInfoList)
                    .javaCode(generateJavaCode(tcpAddresses, databaseName, enableSecurity, securityUser, securityPassword))
                    .pythonCode(generatePythonCode(httpAddresses, tcpAddresses, databaseName, enableSecurity, securityUser, securityPassword))
                    .commandLines(generateCommandLines(serviceHome,clickhouseNodes.get(0), httpAddresses, tcpAddresses, databaseName, enableSecurity, securityUser, securityPassword))
                    .hostName(clickhouseNodes.get(0))
                    .build();

        } catch (Exception e) {
            log.error("获取ClickHouse连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder().build();
        }
    }

    /**
     * 生成Java代码示例
     */
    private String generateJavaCode(String tcpAddresses, String databaseName, boolean enableSecurity,
                                  String username, String password) {
        StringBuilder code = new StringBuilder();

        // 导入包
        code.append("import java.sql.Connection;\n");
        code.append("import java.sql.DriverManager;\n");
        code.append("import java.sql.ResultSet;\n");
        code.append("import java.sql.SQLException;\n");
        code.append("import java.sql.Statement;\n");
        code.append("import java.util.Properties;\n\n");

        // 类定义
        code.append("/**\n");
        code.append(" * ClickHouse Java客户端示例\n");
        code.append(" */\n");
        code.append("public class ClickHouseExample {\n\n");

        // main方法
        code.append("    public static void main(String[] args) {\n");
        code.append("        // JDBC连接参数\n");
        String firstNode = tcpAddresses.split(",")[0];
        String host = firstNode.split(":")[0];
        String port = firstNode.split(":")[1];
        
        code.append("        String host = \"").append(host).append("\";\n");
        code.append("        int port = ").append(port).append(";\n");
        code.append("        String database = \"").append(databaseName).append("\";\n");
        
        if (enableSecurity) {
            code.append("        String user = \"").append(username).append("\";\n");
            code.append("        String password = \"").append(password).append("\";\n\n");
        }
        
        // 构建连接字符串
        code.append("        // 构建JDBC URL\n");
        code.append("        String jdbcUrl = \"jdbc:clickhouse://\" + host + \":\" + port + \"/\" + database;\n\n");
        
        // 设置连接属性
        code.append("        // 设置连接属性\n");
        code.append("        Properties properties = new Properties();\n");
        if (enableSecurity) {
            code.append("        properties.setProperty(\"user\", user);\n");
            code.append("        properties.setProperty(\"password\", password);\n");
        }
        code.append("        properties.setProperty(\"socket_timeout\", \"300000\");\n");
        code.append("        properties.setProperty(\"connect_timeout\", \"5000\");\n\n");
        
        // 执行查询
        code.append("        try (Connection connection = DriverManager.getConnection(jdbcUrl, properties);\n");
        code.append("             Statement statement = connection.createStatement()) {\n\n");
        
        code.append("            // 示例1：执行简单查询\n");
        code.append("            try (ResultSet rs = statement.executeQuery(\"SELECT 1\")) {\n");
        code.append("                if (rs.next()) {\n");
        code.append("                    System.out.println(\"连接测试成功: \" + rs.getInt(1));\n");
        code.append("                }\n");
        code.append("            }\n\n");
        
        code.append("            // 示例2：获取数据库列表\n");
        code.append("            try (ResultSet rs = statement.executeQuery(\"SHOW DATABASES\")) {\n");
        code.append("                System.out.println(\"数据库列表:\");\n");
        code.append("                while (rs.next()) {\n");
        code.append("                    System.out.println(rs.getString(1));\n");
        code.append("                }\n");
        code.append("            }\n\n");
        
        code.append("            // 示例3：创建表\n");
        code.append("            statement.executeUpdate(\"CREATE TABLE IF NOT EXISTS example_table (\\n\" +\n");
        code.append("                    \"    id UInt32,\\n\" +\n");
        code.append("                    \"    name String,\\n\" +\n");
        code.append("                    \"    value Float64,\\n\" +\n");
        code.append("                    \"    event_date Date,\\n\" +\n");
        code.append("                    \"    event_time DateTime\\n\" +\n");
        code.append("                    \") ENGINE = MergeTree()\\n\" +\n");
        code.append("                    \"ORDER BY (id, event_date)\");\n\n");
        
        code.append("            // 示例4：插入数据\n");
        code.append("            statement.executeUpdate(\"INSERT INTO example_table (id, name, value, event_date, event_time) \" +\n");
        code.append("                    \"VALUES (1, 'Test', 123.45, '2023-01-01', '2023-01-01 12:00:00')\");\n\n");
        
        code.append("            // 示例5：查询数据\n");
        code.append("            try (ResultSet rs = statement.executeQuery(\"SELECT * FROM example_table\")) {\n");
        code.append("                System.out.println(\"查询结果:\");\n");
        code.append("                while (rs.next()) {\n");
        code.append("                    System.out.printf(\"ID: %d, Name: %s, Value: %.2f, Date: %s, Time: %s%n\",\n");
        code.append("                            rs.getInt(\"id\"),\n");
        code.append("                            rs.getString(\"name\"),\n");
        code.append("                            rs.getDouble(\"value\"),\n");
        code.append("                            rs.getDate(\"event_date\"),\n");
        code.append("                            rs.getTimestamp(\"event_time\"));\n");
        code.append("                }\n");
        code.append("            }\n");
        
        code.append("        } catch (SQLException e) {\n");
        code.append("            System.err.println(\"SQL错误: \" + e.getMessage());\n");
        code.append("            e.printStackTrace();\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("}\n");

        return code.toString();
    }

    /**
     * 生成Python代码示例
     */
    private String generatePythonCode(String httpAddresses, String tcpAddresses, String databaseName, 
                                     boolean enableSecurity, String username, String password) {
        StringBuilder code = new StringBuilder();

        // 导入包
        code.append("# 方式1: 使用clickhouse-driver（原生TCP协议）\n");
        code.append("from clickhouse_driver import Client\n");
        code.append("import pandas as pd\n");
        code.append("from datetime import datetime\n\n");

        // TCP协议示例
        code.append("def tcp_client_example():\n");
        code.append("    \"\"\"使用clickhouse-driver连接ClickHouse示例\"\"\"\n");
        code.append("    # 连接参数\n");
        String[] tcpParts = tcpAddresses.split(",")[0].split(":");
        String tcpHost = tcpParts[0];
        String tcpPort = tcpParts[1];
        
        code.append("    client = Client(\n");
        code.append("        host='").append(tcpHost).append("',\n");
        code.append("        port=").append(tcpPort).append(",\n");
        code.append("        database='").append(databaseName).append("',\n");
        
        if (enableSecurity) {
            code.append("        user='").append(username).append("',\n");
            code.append("        password='").append(password).append("',\n");
        }
        
        code.append("        settings={'use_numpy': True}\n");
        code.append("    )\n\n");
        
        code.append("    # 示例1：执行简单查询\n");
        code.append("    result = client.execute('SELECT 1')\n");
        code.append("    print(f\"连接测试: {result}\")\n\n");
        
        code.append("    # 示例2：获取数据库列表\n");
        code.append("    databases = client.execute('SHOW DATABASES')\n");
        code.append("    print(\"数据库列表:\")\n");
        code.append("    for db in databases:\n");
        code.append("        print(f\"  - {db[0]}\")\n\n");
        
        code.append("    # 示例3：创建表\n");
        code.append("    client.execute(\"\"\"\n");
        code.append("        CREATE TABLE IF NOT EXISTS example_table (\n");
        code.append("            id UInt32,\n");
        code.append("            name String,\n");
        code.append("            value Float64,\n");
        code.append("            event_date Date,\n");
        code.append("            event_time DateTime\n");
        code.append("        ) ENGINE = MergeTree()\n");
        code.append("        ORDER BY (id, event_date)\n");
        code.append("    \"\"\")\n\n");
        
        code.append("    # 示例4：插入数据\n");
        code.append("    data = [\n");
        code.append("        (1, 'Test 1', 123.45, datetime.now().date(), datetime.now()),\n");
        code.append("        (2, 'Test 2', 678.90, datetime.now().date(), datetime.now())\n");
        code.append("    ]\n");
        code.append("    client.execute(\n");
        code.append("        'INSERT INTO example_table (id, name, value, event_date, event_time) VALUES',\n");
        code.append("        data\n");
        code.append("    )\n\n");
        
        code.append("    # 示例5：查询数据并转换为DataFrame\n");
        code.append("    result = client.execute(\n");
        code.append("        'SELECT * FROM example_table',\n");
        code.append("        with_column_types=True\n");
        code.append("    )\n");
        code.append("    rows, columns = result\n");
        code.append("    column_names = [col[0] for col in columns]\n");
        code.append("    df = pd.DataFrame(rows, columns=column_names)\n");
        code.append("    print(\"\\n查询结果:\")\n");
        code.append("    print(df)\n\n");
        
        // HTTP客户端示例
        code.append("# 方式2: 使用clickhouse-connect（HTTP协议）\n");
        code.append("import clickhouse_connect\n\n");
        
        code.append("def http_client_example():\n");
        code.append("    \"\"\"使用clickhouse-connect连接ClickHouse示例\"\"\"\n");
        
        String[] httpParts = httpAddresses.split(",")[0].split(":");
        String httpHost = httpParts[0];
        String httpPort = httpParts[1];
        
        code.append("    # 创建HTTP客户端\n");
        code.append("    client = clickhouse_connect.get_client(\n");
        code.append("        host='").append(httpHost).append("',\n");
        code.append("        port=").append(httpPort).append(",\n");
        code.append("        database='").append(databaseName).append("',\n");
        
        if (enableSecurity) {
            code.append("        username='").append(username).append("',\n");
            code.append("        password='").append(password).append("'\n");
        } else {
            code.append("        username='default'\n");
        }
        
        code.append("    )\n\n");
        
        code.append("    # 执行查询并获取结果\n");
        code.append("    result = client.query('SELECT * FROM example_table')\n");
        code.append("    print(\"\\nHTTP客户端查询结果:\")\n");
        code.append("    print(result.result_set)\n\n");
        
        code.append("    # 转换为DataFrame\n");
        code.append("    df = result.to_pandas()\n");
        code.append("    print(\"\\n转换为DataFrame:\")\n");
        code.append("    print(df)\n\n");
        
        // 主方法
        code.append("if __name__ == \"__main__\":\n");
        code.append("    try:\n");
        code.append("        # 使用TCP客户端\n");
        code.append("        print(\"=== 使用TCP客户端 ===\")\n");
        code.append("        tcp_client_example()\n\n");
        
        code.append("        # 使用HTTP客户端\n");
        code.append("        print(\"\\n=== 使用HTTP客户端 ===\")\n");
        code.append("        http_client_example()\n\n");
        
        code.append("    except Exception as e:\n");
        code.append("        print(f\"执行出错: {e}\")\n");

        return code.toString();
    }

    /**
     * 生成命令行示例
     */
    private List<CommandLineItem> generateCommandLines(String clickhouseHome,String hostname ,String httpAddresses,
                                                    String tcpAddresses, String databaseName,
                                                    boolean enableSecurity, String username, String password) {
        List<CommandLineItem> commands = new ArrayList<>();
        String[] addresses = tcpAddresses.split(",");
        String tcpPort = addresses[0].split(":")[1];
        String httpPort = httpAddresses.split(",")[0].split(":")[1];

        // 构建基础命令
        String authParam = enableSecurity ? " --user=" + username + " --password=" + password : "";

        // 添加进入clickhouse目录的提示符
        String clickhousePrompt = "[root@" + hostname + " " + clickhouseHome.substring(clickhouseHome.lastIndexOf('/') + 1) + "]# ";
        String clickhouseClientPrompt = hostname + ":" + tcpPort + " :) ";

        // 1. 连接到ClickHouse客户端
        CommandLineItem connectItem = new CommandLineItem();
        connectItem.setLabel("连接到ClickHouse客户端");
        connectItem.setValue("bin/clickhouse-client" + authParam + " --host=" + hostname + " --port=" + tcpPort + " --database=" + databaseName);
        connectItem.setCommandResult("ClickHouse client version 23.3.1.2823 (official build).\nConnecting to " + hostname + ":" + tcpPort + " as user " + (enableSecurity ? username : "default") + ".\nConnected to ClickHouse server version 23.3.1 revision 54466.");
        connectItem.setCommandPrompt(clickhousePrompt);
        commands.add(connectItem);

        // 2. 显示数据库列表
        CommandLineItem showDbsItem = new CommandLineItem();
        showDbsItem.setLabel("显示数据库列表");
        showDbsItem.setValue("SHOW DATABASES");
        showDbsItem.setCommandResult("┌─name───────┐\n│ default    │\n│ system     │\n│ information_schema │\n└────────────┘");
        showDbsItem.setCommandPrompt(clickhouseClientPrompt);
        commands.add(showDbsItem);

        // 3. 创建测试表
        CommandLineItem createTableItem = new CommandLineItem();
        createTableItem.setLabel("创建测试表");
        createTableItem.setValue("CREATE TABLE IF NOT EXISTS example_table (\n" +
                "    id UInt32,\n" +
                "    name String,\n" +
                "    value Float64,\n" +
                "    event_date Date,\n" +
                "    event_time DateTime\n" +
                ") ENGINE = MergeTree()\n" +
                "ORDER BY (id, event_date)");
        createTableItem.setCommandResult("Ok.");
        createTableItem.setCommandPrompt(clickhouseClientPrompt);
        commands.add(createTableItem);

        // 4. 插入测试数据
        CommandLineItem insertDataItem = new CommandLineItem();
        insertDataItem.setLabel("插入测试数据");
        insertDataItem.setValue("INSERT INTO example_table (id, name, value, event_date, event_time) VALUES\n" +
                "(1, 'Test 1', 123.45, '2023-01-01', '2023-01-01 12:00:00'),\n" +
                "(2, 'Test 2', 678.90, '2023-01-02', '2023-01-02 13:30:00')");
        insertDataItem.setCommandResult("Ok.\n\n2 rows in set. Elapsed: 0.005 sec.");
        insertDataItem.setCommandPrompt(clickhouseClientPrompt);
        commands.add(insertDataItem);

        // 5. 查询数据
        CommandLineItem queryDataItem = new CommandLineItem();
        queryDataItem.setLabel("查询数据");
        queryDataItem.setValue("SELECT * FROM example_table");
        queryDataItem.setCommandResult("┌─id─┬─name───┬─value─┬─event_date─┬─────────event_time─┐\n│  1 │ Test 1 │ 123.45 │ 2023-01-01 │ 2023-01-01 12:00:00 │\n│  2 │ Test 2 │ 678.90 │ 2023-01-02 │ 2023-01-02 13:30:00 │\n└────┴────────┴───────┴────────────┴─────────────────────┘\n\n2 rows in set. Elapsed: 0.003 sec.");
        queryDataItem.setCommandPrompt(clickhouseClientPrompt);
        commands.add(queryDataItem);

        // 6. 聚合查询
        CommandLineItem aggregateItem = new CommandLineItem();
        aggregateItem.setLabel("执行聚合查询");
        aggregateItem.setValue("SELECT\n" +
                "    event_date,\n" +
                "    count() as count,\n" +
                "    avg(value) as avg_value,\n" +
                "    min(value) as min_value,\n" +
                "    max(value) as max_value\n" +
                "FROM example_table\n" +
                "GROUP BY event_date\n" +
                "ORDER BY event_date");
        aggregateItem.setCommandResult("┌─event_date─┬─count─┬─avg_value─┬─min_value─┬─max_value─┐\n│ 2023-01-01 │     1 │     123.45 │     123.45 │     123.45 │\n│ 2023-01-02 │     1 │      678.9 │      678.9 │      678.9 │\n└────────────┴───────┴───────────┴───────────┴───────────┘\n\n2 rows in set. Elapsed: 0.004 sec.");
        aggregateItem.setCommandPrompt(clickhouseClientPrompt);
        commands.add(aggregateItem);

        // 7. 使用HTTP接口查询
        CommandLineItem httpQueryItem = new CommandLineItem();
        httpQueryItem.setLabel("通过HTTP接口查询");
        String httpAuthParam = enableSecurity ? " -u " + username + ":" + password : "";
        httpQueryItem.setValue("curl" + httpAuthParam + " -s 'http://" + hostname + ":" + httpPort + "/?database=" + databaseName + "&query=SELECT+*+FROM+example_table+FORMAT+JSONEachRow'");
        httpQueryItem.setCommandResult("{\"id\":1,\"name\":\"Test 1\",\"value\":123.45,\"event_date\":\"2023-01-01\",\"event_time\":\"2023-01-01 12:00:00\"}\n{\"id\":2,\"name\":\"Test 2\",\"value\":678.9,\"event_date\":\"2023-01-02\",\"event_time\":\"2023-01-02 13:30:00\"}");
        httpQueryItem.setCommandPrompt(clickhousePrompt);
        commands.add(httpQueryItem);

        // 8. 查看表结构
        CommandLineItem describeTableItem = new CommandLineItem();
        describeTableItem.setLabel("查看表结构");
        describeTableItem.setValue("DESCRIBE TABLE example_table");
        describeTableItem.setCommandResult("┌─name───────┬─type───────┬─default_type─┬─default_expression─┬─comment─┬─codec_expression─┬─ttl_expression─┐\n│ id         │ UInt32      │              │                   │         │                  │                │\n│ name       │ String      │              │                   │         │                  │                │\n│ value      │ Float64     │              │                   │         │                  │                │\n│ event_date │ Date        │              │                   │         │                  │                │\n│ event_time │ DateTime    │              │                   │         │                  │                │\n└────────────┴─────────────┴──────────────┴───────────────────┴─────────┴──────────────────┴────────────────┘");
        describeTableItem.setCommandPrompt(clickhouseClientPrompt);
        commands.add(describeTableItem);

        // 9. 显示系统信息
        CommandLineItem systemInfoItem = new CommandLineItem();
        systemInfoItem.setLabel("查看系统信息");
        systemInfoItem.setValue("SELECT * FROM system.build_options");
        systemInfoItem.setCommandResult("┌─name────────────────┬─value───────────────────────────────────────────────┐\n│ VERSION_FULL       │ 23.3.1.2823 (official build)                     │\n│ VERSION_DESCRIBE   │ v23.3.1-stable                                   │\n│ VERSION_INTEGER    │ 23003001                                          │\n│ VERSION_MAJOR      │ 23                                                │\n│ VERSION_MINOR      │ 3                                                 │\n│ VERSION_PATCH      │ 1                                                 │\n│ VERSION_REVISION   │ 54466                                             │\n│ VERSION_GITHASH    │ e6e1c7e2b1b96385b5985d70ff5bee30aa20ea8a         │\n└─────────────────────┴────────────────────────────────────────────────┘");
        systemInfoItem.setCommandPrompt(clickhouseClientPrompt);
        commands.add(systemInfoItem);

        // 10. 退出客户端
        CommandLineItem exitItem = new CommandLineItem();
        exitItem.setLabel("退出ClickHouse客户端");
        exitItem.setValue("exit");
        exitItem.setCommandResult("Bye.");
        exitItem.setCommandPrompt(clickhouseClientPrompt);
        commands.add(exitItem);

        return addFinalPrompt(commands,clickhouseHome,hostname);
    }
}
