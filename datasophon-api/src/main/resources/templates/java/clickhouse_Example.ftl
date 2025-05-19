DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>com.clickhouse</groupId>
    <artifactId>clickhouse-jdbc</artifactId>
    <version>0.5.0</version>
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

package com.example.clickhouse;

/*
 * ClickHouse Java连接示例
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import java.util.LinkedHashMap;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClickHouseExample {

    public static void main(String[] args) {
        // 连接参数
        String host = "${data.getBasicInfoValue('host', 'localhost')}";
        int tcpPort = ${data.getBasicInfoValue('tcpPort', '9000')};
        int httpPort = ${data.getBasicInfoValue('httpPort', '8123')};
        String database = "${data.getBasicInfoValue('database', 'default')}";
        
        // 安全认证配置
        String user = "${data.getSecurityInfoValue('username', 'default')}";
        String password = "${data.getSecurityInfoValue('password', '')}";
        
        // 构建JDBC URL
        String url = String.format("jdbc:clickhouse://%s:%d/%s", host, tcpPort, database);
        
        // 设置连接属性
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        
        try {
            System.out.println("=== 使用JDBC协议连接 ===");
            
            // 注册驱动
            Class.forName("com.clickhouse.jdbc.ClickHouseDriver");
            
            // 建立连接
            try (Connection conn = DriverManager.getConnection(url, properties)) {
                System.out.println("成功连接到ClickHouse服务器!");
                
                // 创建示例表
                createExampleTable(conn);
                
                // 插入数据
                insertData(conn);
                
                // 查询数据
                queryData(conn);
                
                // 使用HTTP协议查询
                System.out.println("\n=== 使用HTTP协议连接 ===");
                httpQueryExample(host, httpPort, database, user, password);
                
                // 删除表
                dropTable(conn);
            }
        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建示例表
     */
    private static void createExampleTable(Connection conn) throws SQLException {
        String createTableSql = 
            "CREATE TABLE IF NOT EXISTS example_table (" +
            "    id UInt32, " +
            "    name String, " +
            "    value Float64, " +
            "    timestamp DateTime" +
            ") ENGINE = MergeTree() " +
            "ORDER BY id";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            System.out.println("成功创建表 example_table");
        }
    }

    /**
     * 插入数据
     */
    private static void insertData(Connection conn) throws SQLException {
        String insertSql = 
            "INSERT INTO example_table (id, name, value, timestamp) VALUES " +
            "(1, 'test1', 10.5, now()), " +
            "(2, 'test2', 20.5, now())";
        
        try (Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate(insertSql);
            System.out.printf("成功插入%d条数据%n", rows);
        }
    }

    /**
     * 查询数据
     */
    private static void queryData(Connection conn) throws SQLException {
        String querySql = "SELECT * FROM example_table";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySql)) {
            
            System.out.println("查询结果:");
            System.out.println("+---------+---------+-----------+------------------------+");
            System.out.println("| id      | name    | value     | timestamp              |");
            System.out.println("+---------+---------+-----------+------------------------+");
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double value = rs.getDouble("value");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                
                System.out.printf("| %-7d | %-7s | %-9.2f | %s |%n",
                    id, name, value, timestamp);
            }
            
            System.out.println("+---------+---------+-----------+------------------------+");
        }
    }
    
    /**
     * 使用HTTP协议查询示例
     */
    private static void httpQueryExample(String host, int httpPort, String database, 
                                       String user, String password) {
        try {
            // 创建HTTP客户端
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            
            // 构建查询URL
            String query = URLEncoder.encode("SELECT * FROM example_table FORMAT JSONEachRow", 
                                            StandardCharsets.UTF_8);
            String url = String.format("http://%s:%d/?database=%s&query=%s", 
                                      host, httpPort, database, query);
            
            // 构建请求
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET();
            
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
                
                // 解析JSON（JSON每行格式）
                String[] lines = jsonResponse.split("\n");
                
                System.out.println("HTTP查询结果:");
                System.out.println("+---------+---------+-----------+------------------------+");
                System.out.println("| id      | name    | value     | timestamp              |");
                System.out.println("+---------+---------+-----------+------------------------+");
                
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> row = mapper.readValue(line, Map.class);
                    
                    int id = ((Number) row.get("id")).intValue();
                    String name = (String) row.get("name");
                    double value = ((Number) row.get("value")).doubleValue();
                    String timestamp = (String) row.get("timestamp");
                    
                    System.out.printf("| %-7d | %-7s | %-9.2f | %s |%n",
                        id, name, value, timestamp);
                }
                
                System.out.println("+---------+---------+-----------+------------------------+");
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
     * 删除表
     */
    private static void dropTable(Connection conn) throws SQLException {
        String dropTableSql = "DROP TABLE IF EXISTS example_table";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(dropTableSql);
            System.out.println("成功删除表 example_table");
        }
    }
} 