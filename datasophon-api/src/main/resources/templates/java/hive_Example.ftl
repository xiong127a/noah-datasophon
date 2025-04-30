DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>org.apache.hive</groupId>
    <artifactId>hive-jdbc</artifactId>
    <version>3.1.3</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>1.7.36</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.11</version>
</dependency>
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
    <dependency>
        <groupId>org.apache.hadoop</groupId>
        <artifactId>hadoop-auth</artifactId>
        <version>3.3.4</version>
    </dependency>
</#if>
DEPENDENCIES_END

package com.example.hive;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Hive JDBC连接示例
*/
public class HiveExample {

private static final Logger logger = LoggerFactory.getLogger(HiveExample.class);

// Hive连接参数
private static final String JDBC_URL = "${data.getConnectInfoValue('jdbcUrl', 'jdbc:hive2://localhost:10000')}";
private static final String USERNAME = "${data.getSecurityInfoValue('username', '')}";
private static final String PASSWORD = "${data.getSecurityInfoValue('password', '')}";

// HiveMetastore地址（仅在使用HCatalog等直接访问元数据时使用）
private static final String METASTORE_URI = "${data.getConnectInfoValue('metastoreUri', 'thrift://localhost:9083')}";

// 是否启用Kerberos认证
private static final boolean ENABLE_KERBEROS = ${data.getSecurityInfoValue('kerberos.enabled', 'false')};

public static void main(String[] args) {
logger.info("Hive JDBC连接示例");
logger.info("JDBC URL: {}", JDBC_URL);

if (ENABLE_KERBEROS) {
logger.info("启用Kerberos认证");
setupKerberos();
}

try {
// 1. 注册JDBC驱动程序
Class.forName("org.apache.hive.jdbc.HiveDriver");

// 2. 建立连接
logger.info("正在连接到 {}", JDBC_URL);

// 根据是否有用户名和密码来创建连接
Connection connection;
if (!USERNAME.isEmpty() && !PASSWORD.isEmpty()) {
Properties connectionProps = new Properties();
connectionProps.setProperty("user", USERNAME);
connectionProps.setProperty("password", PASSWORD);
connection = DriverManager.getConnection(JDBC_URL, connectionProps);
} else {
connection = DriverManager.getConnection(JDBC_URL);
}

logger.info("连接成功!");

// 3. 执行示例查询
performBasicQueries(connection);

// 4. 关闭连接
connection.close();
logger.info("连接已关闭");

} catch (ClassNotFoundException e) {
logger.error("找不到Hive JDBC驱动程序: {}", e.getMessage());
} catch (SQLException e) {
logger.error("SQL错误: {}", e.getMessage());
} catch (Exception e) {
logger.error("发生未预期的错误: {}", e.getMessage());
}
}

/**
* 设置Kerberos认证环境
*/
private static void setupKerberos() {
// 设置Kerberos配置
System.setProperty("java.security.krb5.conf", "${data.getSecurityInfoValue('krb5.conf.path', '/etc/krb5.conf')}");

// 如果使用keytab文件进行身份验证
String keytabPath = "${data.getSecurityInfoValue('keytab.path', '')}";
String principal = "${data.getSecurityInfoValue('principal', '')}";

if (!keytabPath.isEmpty() && !principal.isEmpty()) {
logger.info("使用Keytab文件认证: {}, 主体: {}", keytabPath, principal);
System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

// 实际项目中可能需要使用UserGroupInformation.loginUserFromKeytab(principal, keytabPath)
// 但这里仅展示配置过程，实际的登录会在JDBC URL中通过principal参数完成
}
}

/**
* 执行基本查询示例
*/
private static void performBasicQueries(Connection connection) throws SQLException {
// 创建语句对象
try (Statement statement = connection.createStatement()) {

// 1. 查看所有数据库
logger.info("查询所有数据库:");
try (ResultSet rs = statement.executeQuery("SHOW DATABASES")) {
while (rs.next()) {
logger.info("  数据库: {}", rs.getString(1));
}
}

// 2. 使用default数据库
logger.info("使用default数据库");
statement.execute("USE default");

// 3. 查看数据库中的表
logger.info("查询所有表:");
try (ResultSet rs = statement.executeQuery("SHOW TABLES")) {
int tableCount = 0;
while (rs.next()) {
tableCount++;
logger.info("  表: {}", rs.getString(1));

// 只显示前5个表
if (tableCount >= 5) {
logger.info("  ... (更多表)");
break;
}
}

if (tableCount == 0) {
logger.info("数据库中没有表，创建示例表");
createExampleTable(statement);
}
}

// 4. 执行查询示例
// 尝试查询已有的表，如果示例表不存在则跳过
try {
logger.info("查询示例表数据:");
try (ResultSet rs = statement.executeQuery("SELECT * FROM example_table LIMIT 10")) {
// 获取列信息
int columnCount = rs.getMetaData().getColumnCount();
StringBuilder header = new StringBuilder();
for (int i = 1; i <= columnCount; i++) {
if (i > 1) header.append(" | ");
header.append(rs.getMetaData().getColumnName(i));
}
logger.info("  {}", header.toString());

// 获取数据行
while (rs.next()) {
StringBuilder row = new StringBuilder();
for (int i = 1; i <= columnCount; i++) {
if (i > 1) row.append(" | ");
row.append(rs.getString(i));
}
logger.info("  {}", row.toString());
}
}
} catch (SQLException e) {
logger.warn("未能查询示例表: {}", e.getMessage());
}

// 5. 更高级的查询示例 - 这里仅展示SQL语法
logger.info("高级查询示例 (仅展示SQL语法):");
logger.info("  JOIN示例: SELECT a.id, a.name, b.value FROM table_a a JOIN table_b b ON a.id = b.id");
logger.info("  聚合示例: SELECT dept, COUNT(*) as count, AVG(salary) as avg_salary FROM employees GROUP BY dept");
logger.info("  窗口函数: SELECT name, dept, salary, RANK() OVER (PARTITION BY dept ORDER BY salary DESC) as rank FROM employees");
}
}

/**
* 创建示例表和数据
*/
private static void createExampleTable(Statement statement) throws SQLException {
// 1. 创建示例表
logger.info("创建示例表 example_table");
statement.execute("CREATE TABLE IF NOT EXISTS example_table (" +
"id INT, " +
"name STRING, " +
"value DOUBLE, " +
"create_time TIMESTAMP) " +
"ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' " +
"STORED AS TEXTFILE");

// 2. 插入示例数据
logger.info("插入示例数据");
statement.execute("INSERT INTO example_table VALUES " +
"(1, 'Item 1', 10.5, CURRENT_TIMESTAMP), " +
"(2, 'Item 2', 20.75, CURRENT_TIMESTAMP), " +
"(3, 'Item 3', 30.25, CURRENT_TIMESTAMP)");
}
} 