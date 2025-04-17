DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.28</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.2.1</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
DEPENDENCIES_END

package com.example.doris;

/*
 * Doris Java连接示例
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Properties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Doris Java连接示例
 * 演示如何使用JDBC和HTTP API连接Doris数据库
 */
public class DorisExample {

    public static void main(String[] args) {
        // 连接参数
        String host = "${data.getBasicInfoValue('host', 'localhost')}";
        int fePort = ${data.getBasicInfoValue('fePort', '9030')};
        int httpPort = ${data.getBasicInfoValue('httpPort', '8030')};
        String database = "example_db";
        
        // 安全认证配置
        String user = "${data.getSecurityInfoValue('username', 'root')}";
        String password = "${data.getSecurityInfoValue('password', '')}";
        
        // 构建JDBC URL
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", host, fePort, database);
        
        try {
            System.out.println("=== 使用JDBC(MySQL协议)连接 ===");
            
            // 建立连接
            try (Connection conn = getConnection(jdbcUrl, user, password)) {
                System.out.println("成功连接到Doris服务器!");
                
                // 创建数据库
                createDatabase(conn, database);
                
                // 创建示例表
                createExampleTable(conn);
                
                // 插入数据
                insertData(conn);
                
                // 查询数据
                queryData(conn);
                
                // 分区操作示例
                partitionExample(conn);
                
                // 使用HTTP API查询
                System.out.println("\n=== 使用HTTP API连接 ===");
                httpQueryExample(host, httpPort, database, user, password);
                
                // 删除表和数据库
                cleanupDatabase(conn, database);
            }
        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库连接
     */
    private static Connection getConnection(String url, String user, String password) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", user);
        if (password != null && !password.isEmpty()) {
            props.setProperty("password", password);
        }
        
        // 配置连接属性
        props.setProperty("useSSL", "false");
        props.setProperty("allowPublicKeyRetrieval", "true");
        props.setProperty("rewriteBatchedStatements", "true");
        
        // 注册驱动并建立连接
        return DriverManager.getConnection(url, props);
    }

    /**
     * 创建数据库
     */
    private static void createDatabase(Connection conn, String database) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 先删除可能存在的数据库
            stmt.execute("DROP DATABASE IF EXISTS " + database);
            // 创建新数据库
            stmt.execute("CREATE DATABASE " + database);
            // 使用该数据库
            stmt.execute("USE " + database);
            System.out.println("成功创建并切换到数据库: " + database);
        }
    }

    /**
     * 创建示例表
     */
    private static void createExampleTable(Connection conn) throws SQLException {
        String createTableSql = 
            "CREATE TABLE IF NOT EXISTS example_table (" +
            "    id INT, " +
            "    name VARCHAR(50), " +
            "    value DOUBLE, " +
            "    create_time DATETIME" +
            ") ENGINE=OLAP " +
            "DUPLICATE KEY(id) " +
            "COMMENT 'Doris示例表' " +
            "DISTRIBUTED BY HASH(id) BUCKETS 3";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            System.out.println("成功创建表 example_table");
        }
    }

    /**
     * 插入数据
     */
    private static void insertData(Connection conn) throws SQLException {
        // 使用PreparedStatement插入数据
        String insertSql = "INSERT INTO example_table (id, name, value, create_time) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            // 第一行数据
            pstmt.setInt(1, 1);
            pstmt.setString(2, "测试1");
            pstmt.setDouble(3, 10.5);
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.addBatch();
            
            // 第二行数据
            pstmt.setInt(1, 2);
            pstmt.setString(2, "测试2");
            pstmt.setDouble(3, 20.5);
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.addBatch();
            
            // 第三行数据
            pstmt.setInt(1, 3);
            pstmt.setString(2, "测试3");
            pstmt.setDouble(3, 30.5);
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.addBatch();
            
            // 执行批量插入
            int[] rows = pstmt.executeBatch();
            System.out.printf("成功插入%d条数据%n", rows.length);
        }
    }

    /**
     * 查询数据
     */
    private static void queryData(Connection conn) throws SQLException {
        String querySql = "SELECT * FROM example_table ORDER BY id";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySql)) {
            
            System.out.println("查询结果:");
            System.out.println("+---------+---------+-----------+------------------------+");
            System.out.println("| id      | name    | value     | create_time            |");
            System.out.println("+---------+---------+-----------+------------------------+");
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double value = rs.getDouble("value");
                Timestamp createTime = rs.getTimestamp("create_time");
                
                System.out.printf("| %-7d | %-7s | %-9.2f | %s |%n",
                    id, name, value, createTime);
            }
            
            System.out.println("+---------+---------+-----------+------------------------+");
        }
    }
    
    /**
     * 分区表操作示例
     */
    private static void partitionExample(Connection conn) throws SQLException {
        System.out.println("\n=== 分区表操作示例 ===");
        
        // 创建分区表
        String createPartitionTableSql = 
            "CREATE TABLE IF NOT EXISTS partition_example (" +
            "    event_day DATE, " +
            "    event_hour SMALLINT, " +
            "    event_type VARCHAR(20), " +
            "    event_count INT" +
            ") ENGINE=OLAP " +
            "DUPLICATE KEY(event_day, event_hour, event_type) " +
            "PARTITION BY RANGE(event_day) (" +
            "    PARTITION p20230101 VALUES [('2023-01-01'), ('2023-01-02')), " +
            "    PARTITION p20230102 VALUES [('2023-01-02'), ('2023-01-03')), " +
            "    PARTITION p20230103 VALUES [('2023-01-03'), ('2023-01-04'))" +
            ") " +
            "DISTRIBUTED BY HASH(event_type) BUCKETS 3";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createPartitionTableSql);
            System.out.println("成功创建分区表 partition_example");
            
            // 插入数据到不同分区
            String[] partitionDays = {"2023-01-01", "2023-01-02", "2023-01-03"};
            String[] eventTypes = {"click", "view", "purchase"};
            
            for (String day : partitionDays) {
                for (int hour = 0; hour < 24; hour += 6) {
                    for (String type : eventTypes) {
                        String insertSql = String.format(
                            "INSERT INTO partition_example VALUES " +
                            "('%s', %d, '%s', %d)", 
                            day, hour, type, 100 + (int)(Math.random() * 900));
                        stmt.execute(insertSql);
                    }
                }
            }
            System.out.println("成功插入分区数据");
            
            // 查询特定分区数据
            String querySql = "SELECT event_day, event_hour, event_type, event_count " +
                             "FROM partition_example " +
                             "WHERE event_day = '2023-01-02' " +
                             "ORDER BY event_hour, event_type";
            
            try (ResultSet rs = stmt.executeQuery(querySql)) {
                System.out.println("\n分区查询结果 (2023-01-02):");
                System.out.println("+------------+------------+------------+-------------+");
                System.out.println("| event_day  | event_hour | event_type | event_count |");
                System.out.println("+------------+------------+------------+-------------+");
                
                while (rs.next()) {
                    String eventDay = rs.getDate("event_day").toString();
                    int eventHour = rs.getInt("event_hour");
                    String eventType = rs.getString("event_type");
                    int eventCount = rs.getInt("event_count");
                    
                    System.out.printf("| %-10s | %-10d | %-10s | %-11d |%n",
                        eventDay, eventHour, eventType, eventCount);
                }
                
                System.out.println("+------------+------------+------------+-------------+");
            }
            
            // 添加新分区
            stmt.execute("ALTER TABLE partition_example ADD PARTITION p20230104 VALUES [('2023-01-04'), ('2023-01-05'))");
            System.out.println("\n成功添加新分区 p20230104");
            
            // 显示分区信息
            try (ResultSet rs = stmt.executeQuery("SHOW PARTITIONS FROM partition_example")) {
                System.out.println("\n分区信息:");
                System.out.println("+----------------+----------------+");
                System.out.println("| PartitionName  | PartitionRange |");
                System.out.println("+----------------+----------------+");
                
                while (rs.next()) {
                    String partitionName = rs.getString(1); // 分区名称通常在第一列
                    String partitionRange = "[...]"; // 实际应用中需要查看更多列
                    
                    System.out.printf("| %-14s | %-14s |%n", partitionName, partitionRange);
                }
                
                System.out.println("+----------------+----------------+");
            }
            
            // 删除分区表
            stmt.execute("DROP TABLE IF EXISTS partition_example");
            System.out.println("\n成功删除分区表 partition_example");
        }
    }
    
    /**
     * 使用HTTP API查询示例
     */
    private static void httpQueryExample(String host, int httpPort, String database, 
                                       String user, String password) {
        try {
            // 创建HTTP客户端
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            
            // 构建查询URL
            String query = URLEncoder.encode("SELECT * FROM example_table FORMAT JSON", 
                                            StandardCharsets.UTF_8);
            String url = String.format("http://%s:%d/api/%s/query", 
                                      host, httpPort, database);
            
            // 构建请求体
            String requestBody = "query=" + query;
            
            // 构建请求
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));
            
            // 添加Basic认证（如果提供了用户名和密码）
            if (user != null && !user.isEmpty()) {
                String auth = user + ":" + password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
            }
            
            // 发送请求
            HttpResponse<String> response = client.send(
                    requestBuilder.build(), 
                    HttpResponse.BodyHandlers.ofString());
            
            // 处理响应
            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                System.out.println("HTTP查询成功，状态码: " + response.statusCode());
                
                // 使用Jackson解析JSON
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> result = mapper.readValue(jsonResponse, Map.class);
                
                if (result.containsKey("data")) {
                    // 获取数据列表
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> data = (java.util.List<Map<String, Object>>) result.get("data");
                    
                    System.out.println("HTTP查询结果:");
                    System.out.println("+---------+---------+-----------+------------------------+");
                    System.out.println("| id      | name    | value     | create_time            |");
                    System.out.println("+---------+---------+-----------+------------------------+");
                    
                    for (Map<String, Object> row : data) {
                        int id = ((Number) row.get("id")).intValue();
                        String name = (String) row.get("name");
                        double value = ((Number) row.get("value")).doubleValue();
                        String createTime = (String) row.get("create_time");
                        
                        System.out.printf("| %-7d | %-7s | %-9.2f | %s |%n",
                            id, name, value, createTime);
                    }
                    
                    System.out.println("+---------+---------+-----------+------------------------+");
                }
            } else {
                System.err.println("HTTP查询失败，状态码: " + response.statusCode());
                System.err.println("错误信息: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("HTTP查询出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清理数据库
     */
    private static void cleanupDatabase(Connection conn, String database) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 删除表
            stmt.execute("DROP TABLE IF EXISTS example_table");
            System.out.println("成功删除表 example_table");
            
            // 删除数据库
            stmt.execute("DROP DATABASE IF EXISTS " + database);
            System.out.println("成功删除数据库 " + database);
        }
    }
} 