DEPENDENCIES_START
<!-- 依赖信息
<dependency>
    <groupId>org.apache.hive</groupId>
    <artifactId>hive-jdbc</artifactId>
    <version>3.1.2</version>
</dependency>

<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
<!-- Kerberos 认证相关依赖 -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-common</artifactId>
    <version>3.3.1</version>
</dependency>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-auth</artifactId>
    <version>3.3.1</version>
</dependency>
</#if>
-->
DEPENDENCIES_END

import java.sql.*;
import java.util.Properties;
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import java.io.IOException;
</#if>

/**
 * Hive JDBC 连接示例
 * 演示如何通过JDBC连接Hive并执行SQL查询
 */
public class HiveJdbcExample {

    // 连接配置
    private static final String HOST = "${data.getConnectInfoValue('host', 'localhost')}";
    private static final String PORT = "${data.getConnectInfoValue('port', '10000')}";
    private static final String DATABASE = "${data.getConnectInfoValue('database', 'default')}";
    private static final String USERNAME = "${data.getSecurityInfoValue('username', '')}";
    private static final String PASSWORD = "${data.getSecurityInfoValue('password', '')}";
    
    // Kerberos配置
    private static final boolean KERBEROS_ENABLED = ${data.getSecurityInfoValue('kerberos.enabled', 'false')};
    <#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
    private static final String KRB5_CONF = "${data.getSecurityInfoValue('krb5.conf.path', '/etc/krb5.conf')}";
    private static final String KEYTAB_PATH = "${data.getSecurityInfoValue('keytab.path', '')}";
    private static final String PRINCIPAL = "${data.getSecurityInfoValue('principal', '')}";
    </#if>
    
    // JDBC URL
    <#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
    private static final String JDBC_URL = "jdbc:hive2://" + HOST + ":" + PORT + "/" + DATABASE + ";principal=hive/_HOST@REALM";
    <#else>
    private static final String JDBC_URL = "jdbc:hive2://" + HOST + ":" + PORT + "/" + DATABASE;
    </#if>

    public static void main(String[] args) {
        System.out.println("Hive JDBC 连接示例");
        System.out.println("-----------------------------");
        
        // 初始化Kerberos
        initKerberos();
        
        // 使用try-with-resources确保资源关闭
        try (Connection conn = getConnection()) {
            System.out.println("成功连接到Hive服务器: " + HOST + ":" + PORT);
            
            // 1. 查看所有数据库
            queryDatabases(conn);
            
            // 2. 切换到指定数据库
            useDatabase(conn, DATABASE);
            
            // 3. 查看所有表
            queryTables(conn);
            
            // 4. 创建示例表和数据
            createExampleTable(conn);
            
            // 5. 查询示例数据
            queryExampleData(conn);
            
            // 6. 打印高级查询提示
            printAdvancedQueryTips();
            
        } catch (SQLException e) {
            System.err.println("数据库操作异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("-----------------------------");
        System.out.println("演示完成");
    }
    
    /**
     * 初始化Kerberos认证(如果启用)
     */
    private static void initKerberos() {
        <#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
        if (KERBEROS_ENABLED) {
            try {
                System.out.println("初始化Kerberos认证...");
                // 设置krb5.conf文件位置
                System.setProperty("java.security.krb5.conf", KRB5_CONF);
                
                Configuration conf = new Configuration();
                conf.set("hadoop.security.authentication", "kerberos");
                UserGroupInformation.setConfiguration(conf);
                
                // 使用keytab进行认证
                if (KEYTAB_PATH != null && !KEYTAB_PATH.isEmpty() && 
                    PRINCIPAL != null && !PRINCIPAL.isEmpty()) {
                    System.out.println("使用keytab文件认证: " + KEYTAB_PATH);
                    UserGroupInformation.loginUserFromKeytab(PRINCIPAL, KEYTAB_PATH);
                    System.out.println("Kerberos认证成功: " + UserGroupInformation.getCurrentUser());
                } else {
                    System.out.println("未指定keytab或principal，尝试使用缓存凭证");
                    UserGroupInformation.loginUserFromSubject(null);
                }
            } catch (IOException e) {
                System.err.println("Kerberos认证失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        </#if>
    }
    
    /**
     * 获取数据库连接
     */
    private static Connection getConnection() throws SQLException {
        System.out.println("连接到Hive: " + JDBC_URL);
        
        Properties properties = new Properties();
        
        // 设置认证信息
        if (!KERBEROS_ENABLED && USERNAME != null && !USERNAME.isEmpty()) {
            properties.setProperty("user", USERNAME);
            if (PASSWORD != null && !PASSWORD.isEmpty()) {
                properties.setProperty("password", PASSWORD);
            }
            System.out.println("使用用户名密码认证: " + USERNAME);
        }
        
        // 注册驱动
        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("Hive JDBC驱动未找到: " + e.getMessage());
            throw new SQLException("Hive JDBC驱动未找到", e);
        }
        
        return DriverManager.getConnection(JDBC_URL, properties);
    }
    
    /**
     * 查询所有数据库
     */
    private static void queryDatabases(Connection conn) throws SQLException {
        System.out.println("\n查询所有数据库:");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            
            int count = 0;
            while (rs.next() && count < 5) {
                System.out.println("  - " + rs.getString(1));
                count++;
            }
            
            if (rs.next()) {
                System.out.println("  ... (还有更多数据库)");
            }
        }
    }
    
    /**
     * 使用指定数据库
     */
    private static void useDatabase(Connection conn, String database) throws SQLException {
        System.out.println("\n使用数据库: " + database);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("USE " + database);
        }
    }
    
    /**
     * 查询所有表
     */
    private static void queryTables(Connection conn) throws SQLException {
        System.out.println("\n查询所有表:");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            
            boolean hasTables = false;
            int count = 0;
            
            while (rs.next() && count < 5) {
                System.out.println("  - " + rs.getString(1));
                hasTables = true;
                count++;
            }
            
            if (rs.next()) {
                System.out.println("  ... (还有更多表)");
            }
            
            if (!hasTables) {
                System.out.println("  没有找到表");
            }
        }
    }
    
    /**
     * 创建示例表和数据
     */
    private static void createExampleTable(Connection conn) {
        System.out.println("\n创建示例表和数据:");
        try (Statement stmt = conn.createStatement()) {
            // 创建示例表
            String createTableSQL = 
                "CREATE TABLE IF NOT EXISTS example_table (" +
                "  id INT, " +
                "  name STRING, " +
                "  value DOUBLE, " +
                "  create_time TIMESTAMP " +
                ") ROW FORMAT DELIMITED FIELDS TERMINATED BY ','";
            
            stmt.execute(createTableSQL);
            System.out.println("  创建表: example_table");
            
            // 插入示例数据
            String insertDataSQL = 
                "INSERT INTO TABLE example_table VALUES " +
                "(1, 'Item 1', 10.5, CURRENT_TIMESTAMP), " +
                "(2, 'Item 2', 20.75, CURRENT_TIMESTAMP), " +
                "(3, 'Item 3', 30.25, CURRENT_TIMESTAMP)";
            
            stmt.execute("INSERT OVERWRITE TABLE example_table " +
                         "SELECT * FROM example_table WHERE 1=0");  // 清空表
            stmt.execute(insertDataSQL);
            System.out.println("  插入示例数据");
            
        } catch (SQLException e) {
            System.err.println("创建示例表时出错: " + e.getMessage());
            // 继续执行，不中断示例流程
        }
    }
    
    /**
     * 查询示例数据
     */
    private static void queryExampleData(Connection conn) {
        System.out.println("\n查询示例数据:");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM example_table LIMIT 10")) {
            
            // 打印列名
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) header.append(" | ");
                header.append(metaData.getColumnName(i));
            }
            System.out.println("  " + header.toString());
            System.out.println("  " + "-".repeat(header.length()));
            
            // 打印数据
            int rowCount = 0;
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) row.append(" | ");
                    row.append(rs.getString(i));
                }
                System.out.println("  " + row.toString());
                rowCount++;
            }
            
            if (rowCount == 0) {
                System.out.println("  没有数据");
            }
            
        } catch (SQLException e) {
            System.err.println("查询示例数据时出错: " + e.getMessage());
        }
    }
    
    /**
     * 打印高级查询提示
     */
    private static void printAdvancedQueryTips() {
        System.out.println("\n高级查询示例和技巧:");
        System.out.println("  1. 预处理语句的使用 (防止SQL注入):");
        System.out.println("     PreparedStatement ps = conn.prepareStatement(\"SELECT * FROM table WHERE id = ?\");");
        System.out.println("     ps.setInt(1, myId);");
        System.out.println("     ResultSet rs = ps.executeQuery();");
        
        System.out.println("  2. 批量操作:");
        System.out.println("     PreparedStatement ps = conn.prepareStatement(\"INSERT INTO table VALUES(?, ?)\");");
        System.out.println("     for (MyRecord record : records) {");
        System.out.println("         ps.setInt(1, record.getId());");
        System.out.println("         ps.setString(2, record.getName());");
        System.out.println("         ps.addBatch();");
        System.out.println("     }");
        System.out.println("     ps.executeBatch();");
        
        System.out.println("  3. 使用ResultSetMetaData动态处理结果集:");
        System.out.println("     ResultSetMetaData metaData = rs.getMetaData();");
        System.out.println("     int columnCount = metaData.getColumnCount();");
        System.out.println("     for (int i = 1; i <= columnCount; i++) {");
        System.out.println("         String name = metaData.getColumnName(i);");
        System.out.println("         String type = metaData.getColumnTypeName(i);");
        System.out.println("     }");
    }
} 