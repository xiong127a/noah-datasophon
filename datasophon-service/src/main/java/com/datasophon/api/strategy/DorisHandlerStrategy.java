package com.datasophon.api.strategy;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DorisHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        getConfig(clusterId, list);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        String priority_networks = globalVariables.get("${priority_networks}");
        for (ServiceConfig serviceConfig : list) {
            if (StrUtil.equals(serviceConfig.getName(), "priority_networks")) {
                serviceConfig.setValue(priority_networks);
            }
        }
    }

    @Override
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId, String serviceHome,
            Map<String, String> configMap) {

        // 获取DorisFE节点信息 (master)
        List<String> feNodes = getRoleHosts(clusterId, serviceInstanceId, "DorisFE");

        // 获取DorisFEObserver节点信息 (slave)
        List<String> feObserverNodes = getRoleHosts(clusterId, serviceInstanceId, "DorisFEObserver");

        // 获取DorisBE节点信息
        List<String> beNodes = getRoleHosts(clusterId, serviceInstanceId, "DorisBE");

        // 获取主节点和从节点信息
        String dorisMaster = feNodes.isEmpty() ? "" : feNodes.get(0);
        String dorisSlave = feObserverNodes.isEmpty() ? "" : feObserverNodes.get(0);

        // 获取端口配置
        String dorisFEPort = configMap.getOrDefault("doris.fe.port", "9030");
        String dorisBEPort = configMap.getOrDefault("doris.be.port", "8040");
        String dorisHttpPort = configMap.getOrDefault("doris.http.port", "8030");

        // 构建连接信息
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .basicInfo(new LinkedHashMap<>())
                .javaCode(generateJavaCode(dorisMaster, dorisFEPort))
                .pythonCode(generatePythonCode(dorisMaster, dorisHttpPort))
                .commandLines(generateCommandLines(serviceHome, dorisMaster, dorisFEPort))
                .build();

        // 添加基本连接信息
        Map<String, String> basicInfo = connectionInfo.getBasicInfo();
        basicInfo.put("Doris FE地址", dorisMaster + ":" + dorisFEPort);
        basicInfo.put("Doris HTTP接口", "http://" + dorisMaster + ":" + dorisHttpPort);
        basicInfo.put("JDBC URL", "jdbc:mysql://" + dorisMaster + ":" + dorisFEPort);

        // 添加BE节点信息
        if (!beNodes.isEmpty()) {
            basicInfo.put("BE节点数量", String.valueOf(beNodes.size()));
            for (int i = 0; i < Math.min(beNodes.size(), 3); i++) {
                basicInfo.put("BE节点" + (i + 1), beNodes.get(i) + ":" + dorisBEPort);
            }
        }

        // 如果有从节点，添加到基本信息中
        if (StrUtil.isNotBlank(dorisSlave)) {
            basicInfo.put("高可用", "启用");
            basicInfo.put("FE Observer节点", dorisSlave + ":" + dorisFEPort);
        } else {
            basicInfo.put("高可用", "未启用");
        }

        return connectionInfo;
    }

    /**
     * 生成Java示例代码
     */
    private String generateJavaCode(String host, String port) {
        StringBuilder code = new StringBuilder();

        code.append("import java.sql.Connection;\n");
        code.append("import java.sql.DriverManager;\n");
        code.append("import java.sql.ResultSet;\n");
        code.append("import java.sql.Statement;\n\n");

        code.append("public class DorisExample {\n");
        code.append("    public static void main(String[] args) {\n");

        code.append("        // JDBC连接参数\n");
        code.append("        String jdbcUrl = \"jdbc:mysql://").append(host).append(":").append(port)
                .append("/example_db\";\n");
        code.append("        String username = \"root\";\n");
        code.append("        String password = \"\";\n\n");

        code.append("        try {\n");
        code.append("            // 加载JDBC驱动\n");
        code.append("            Class.forName(\"com.mysql.cj.jdbc.Driver\");\n\n");

        code.append("            // 创建连接\n");
        code.append("            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);\n");
        code.append("            Statement stmt = conn.createStatement();\n\n");

        code.append("            // 执行查询\n");
        code.append("            String sql = \"SELECT * FROM example_table LIMIT 10\";\n");
        code.append("            ResultSet rs = stmt.executeQuery(sql);\n\n");

        code.append("            // 处理结果\n");
        code.append("            while (rs.next()) {\n");
        code.append("                System.out.println(rs.getString(1));\n");
        code.append("            }\n\n");

        code.append("            // 关闭资源\n");
        code.append("            rs.close();\n");
        code.append("            stmt.close();\n");
        code.append("            conn.close();\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            e.printStackTrace();\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("}\n");

        return code.toString();
    }

    /**
     * 生成Python示例代码
     */
    private String generatePythonCode(String host, String httpPort) {
        StringBuilder code = getCode();

        code.append("import pymysql\n");
        code.append("import requests\n");
        code.append("import json\n\n");

        code.append("# MySQL连接方式\n");
        code.append("def connect_mysql():\n");
        code.append("    # 创建连接\n");
        code.append("    conn = pymysql.connect(\n");
        code.append("        host=\"").append(host).append("\",\n");
        code.append("        port=").append(httpPort).append(",\n");
        code.append("        user=\"root\",\n");
        code.append("        password=\"\",\n");
        code.append("        database=\"example_db\"\n");
        code.append("    )\n\n");

        code.append("    # 执行查询\n");
        code.append("    with conn.cursor() as cursor:\n");
        code.append("        cursor.execute(\"SELECT * FROM example_table LIMIT 10\")\n");
        code.append("        results = cursor.fetchall()\n");
        code.append("        for row in results:\n");
        code.append("            print(row)\n\n");

        code.append("    # 关闭连接\n");
        code.append("    conn.close()\n\n");

        code.append("# HTTP API方式\n");
        code.append("def connect_http_api():\n");
        code.append("    # 设置API地址\n");
        code.append("    base_url = \"http://").append(host).append(":").append(httpPort).append("\"\n");
        code.append("    api_endpoint = \"/api/query\"\n\n");

        code.append("    # 准备查询数据\n");
        code.append("    payload = {\n");
        code.append("        \"sql\": \"SELECT * FROM example_table LIMIT 10\",\n");
        code.append("        \"format\": \"json\"\n");
        code.append("    }\n\n");

        code.append("    # 发送请求\n");
        code.append("    response = requests.post(\n");
        code.append("        base_url + api_endpoint,\n");
        code.append("        json=payload,\n");
        code.append("        auth=(\"root\", \"\")\n");
        code.append("    )\n\n");

        code.append("    # 处理响应\n");
        code.append("    if response.status_code == 200:\n");
        code.append("        data = response.json()\n");
        code.append("        print(json.dumps(data, indent=2))\n");
        code.append("    else:\n");
        code.append("        print(f\"Error: {response.status_code}\")\n");
        code.append("        print(response.text)\n\n");

        code.append("# 调用示例\n");
        code.append("connect_mysql()\n");
        code.append("# connect_http_api()\n");

        return code.toString();
    }

    private static StringBuilder getCode() {
        return new StringBuilder();
    }

    /**
     * 生成命令行示例
     */
    private List<CommandLineItem> generateCommandLines(String serviceHome, String hostname, String port) {
        // 定义命令行提示符
        final String MYSQL_PROMPT = "mysql> ";
        final String serviceHomePrompt = "[root@" + hostname + " " + serviceHome + "]# ";
        List<CommandLineItem> commandLines = new ArrayList<>();

        // 添加连接命令
        CommandLineItem connectCmd = new CommandLineItem();
        connectCmd.setLabel("连接Doris MySQL客户端");
        connectCmd.setValue(String.format("mysql -h %s -P %s -u root", hostname, port));
        connectCmd.setCommandPrompt(serviceHomePrompt);
        connectCmd.setCommandResult(
                "Welcome to the MySQL monitor. Commands end with ; or \\g.\nYour MySQL connection id is 3\nServer version: 5.7.37 Doris version 1.2.6");
        commandLines.add(connectCmd);

        // 添加查询命令
        CommandLineItem queryCmd = new CommandLineItem();
        queryCmd.setLabel("执行查询");
        queryCmd.setValue("SELECT * FROM example_db.example_table LIMIT 10;");
        queryCmd.setCommandPrompt(MYSQL_PROMPT);
        queryCmd.setCommandResult(
                "+---------+------------+------------+\n| id      | name       | create_time |\n+---------+------------+------------+\n| 1001    | 测试数据1   | 2023-06-01 |\n| 1002    | 测试数据2   | 2023-06-02 |\n+---------+------------+------------+\n2 rows in set (0.02 sec)");
        commandLines.add(queryCmd);

        // 添加创建表命令
        CommandLineItem createTableCmd = new CommandLineItem();
        createTableCmd.setLabel("创建表");
        createTableCmd.setValue("CREATE TABLE example_db.new_table (\n" +
                "  `id` INT,\n" +
                "  `name` VARCHAR(100),\n" +
                "  `create_time` DATETIME\n" +
                ") ENGINE=OLAP\n" +
                "DISTRIBUTED BY HASH(`id`) BUCKETS 10;");
        createTableCmd.setCommandPrompt(MYSQL_PROMPT);
        createTableCmd.setCommandResult("Query OK, 0 rows affected (0.35 sec)");
        commandLines.add(createTableCmd);

        // 添加显示数据库命令
        CommandLineItem showDbCmd = new CommandLineItem();
        showDbCmd.setLabel("显示所有数据库");
        showDbCmd.setValue("SHOW DATABASES;");
        showDbCmd.setCommandPrompt(MYSQL_PROMPT);
        showDbCmd.setCommandResult(
                "+--------------------+\n| Database           |\n+--------------------+\n| information_schema |\n| example_db         |\n| test               |\n+--------------------+\n3 rows in set (0.00 sec)");
        commandLines.add(showDbCmd);

        // 添加显示表命令
        CommandLineItem showTablesCmd = new CommandLineItem();
        showTablesCmd.setLabel("显示数据库中的表");
        showTablesCmd.setValue("USE example_db; SHOW TABLES;");
        showTablesCmd.setCommandPrompt(MYSQL_PROMPT);
        showTablesCmd.setCommandResult(
                "Database changed\n+----------------------+\n| Tables_in_example_db |\n+----------------------+\n| example_table        |\n| new_table            |\n+----------------------+\n2 rows in set (0.01 sec)");
        commandLines.add(showTablesCmd);

        // 添加查看表结构命令
        CommandLineItem descTableCmd = new CommandLineItem();
        descTableCmd.setLabel("查看表结构");
        descTableCmd.setValue("DESC example_db.example_table;");
        descTableCmd.setCommandPrompt(MYSQL_PROMPT);
        descTableCmd.setCommandResult(
                "+-------------+--------------+------+-------+---------+-------+\n| Field       | Type         | Null | Key   | Default | Extra |\n+-------------+--------------+------+-------+---------+-------+\n| id          | INT          | Yes  | true  | NULL    |       |\n| name        | VARCHAR(100) | Yes  | false | NULL    |       |\n| create_time | DATETIME     | Yes  | false | NULL    |       |\n+-------------+--------------+------+-------+---------+-------+\n3 rows in set (0.00 sec)");
        commandLines.add(descTableCmd);

        // 添加数据插入命令
        CommandLineItem insertCmd = new CommandLineItem();
        insertCmd.setLabel("插入数据");
        insertCmd.setValue("INSERT INTO example_db.example_table VALUES (1003, '测试数据3', '2023-06-03');");
        insertCmd.setCommandPrompt(MYSQL_PROMPT);
        insertCmd.setCommandResult("Query OK, 1 row affected (0.08 sec)");
        commandLines.add(insertCmd);

        // 添加批量数据插入命令
        CommandLineItem batchInsertCmd = new CommandLineItem();
        batchInsertCmd.setLabel("批量插入数据");
        batchInsertCmd.setValue(
                "INSERT INTO example_db.example_table VALUES \n(1004, '测试数据4', '2023-06-04'),\n(1005, '测试数据5', '2023-06-05');");
        batchInsertCmd.setCommandPrompt(MYSQL_PROMPT);
        batchInsertCmd.setCommandResult("Query OK, 2 rows affected (0.10 sec)");
        commandLines.add(batchInsertCmd);

        // 添加查看BE状态命令
        CommandLineItem beStatusCmd = new CommandLineItem();
        beStatusCmd.setLabel("查看BE节点状态");
        beStatusCmd.setValue("SHOW BACKENDS;");
        beStatusCmd.setCommandPrompt(MYSQL_PROMPT);
        beStatusCmd.setCommandResult(
                "+----------+-----------------+------+--------------+----------+----------+----------+---------------------+---------------------+-------+\n| BackendId | Host            | Port | HttpPort      | BePort   | AlivePort | BrpcPort | LastStartTime       | LastUpdateTime      | Alive |\n+----------+-----------------+------+--------------+----------+----------+----------+---------------------+---------------------+-------+\n| 10000     | 192.168.1.101   | 9050 | 8040         | 8060     | 9070      | 8060     | 2023-06-01 10:00:00 | 2023-06-10 09:30:00 | true  |\n| 10001     | 192.168.1.102   | 9050 | 8040         | 8060     | 9070      | 8060     | 2023-06-01 10:05:00 | 2023-06-10 09:30:05 | true  |\n+----------+-----------------+------+--------------+----------+----------+----------+---------------------+---------------------+-------+");
        commandLines.add(beStatusCmd);

        // 添加查看FE状态命令
        CommandLineItem feStatusCmd = new CommandLineItem();
        feStatusCmd.setLabel("查看FE节点状态");
        feStatusCmd.setValue("SHOW FRONTENDS;");
        feStatusCmd.setCommandPrompt(MYSQL_PROMPT);
        feStatusCmd.setCommandResult(
                "+------------------+-----------------+-------------+----------+-----------+---------+----------+----------+\n| Name             | Host            | EditLogPort | HttpPort | QueryPort | RpcPort | Role     | IsMaster |\n+------------------+-----------------+-------------+----------+-----------+---------+----------+----------+\n| 192.168.1.101_9010 | 192.168.1.101   | 9010        | 8030     | 9030      | 9020    | FOLLOWER | true     |\n| 192.168.1.102_9010 | 192.168.1.102   | 9010        | 8030     | 9030      | 9020    | OBSERVER | false    |\n+------------------+-----------------+-------------+----------+-----------+---------+----------+----------+");
        commandLines.add(feStatusCmd);

        // 添加MySQL退出命令
        CommandLineItem exitCmd = new CommandLineItem();
        exitCmd.setLabel("退出MySQL");
        exitCmd.setValue("exit;");
        exitCmd.setCommandPrompt(MYSQL_PROMPT);
        exitCmd.setCommandResult("Bye");
        commandLines.add(exitCmd);

        // 添加HTTP API命令
        CommandLineItem httpCmd = new CommandLineItem();
        httpCmd.setLabel("使用HTTP API查询");
        httpCmd.setValue(String.format(
                "curl -u root: -X POST http://%s:8030/api/query -d '{\"sql\":\"SELECT * FROM example_db.example_table LIMIT 10\",\"format\":\"json\"}'",
                hostname));
        httpCmd.setCommandPrompt(serviceHomePrompt);
        httpCmd.setCommandResult(
                "{\n  \"status\": \"success\",\n  \"data\": [\n    {\"id\":1001,\"name\":\"测试数据1\",\"create_time\":\"2023-06-01\"},\n    {\"id\":1002,\"name\":\"测试数据2\",\"create_time\":\"2023-06-02\"}\n  ]\n}");
        commandLines.add(httpCmd);

        return addFinalPrompt(commandLines, serviceHome, hostname);
    }
}
