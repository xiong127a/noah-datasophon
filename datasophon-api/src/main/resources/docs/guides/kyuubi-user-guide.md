# Kyuubi 用户指南

## 环境准备

### 前置要求

在开始使用 Kyuubi 之前，请确保以下组件已经正确安装和配置：

- Java 8 或更高版本
- Apache Spark 3.0.0 或更高版本
- Apache Hadoop（可选，如果使用 HDFS）
- ZooKeeper 3.4.6 或更高版本

### 安装配置

1. **下载和解压**
   ```bash
   wget https://downloads.apache.org/kyuubi/latest/apache-kyuubi-{version}-bin.tar.gz
   tar -xzvf apache-kyuubi-{version}-bin.tar.gz
   cd apache-kyuubi-{version}-bin
   ```

2. **配置环境变量**
   ```bash
   export KYUUBI_HOME=/path/to/kyuubi
   export PATH=$KYUUBI_HOME/bin:$PATH
   ```

3. **基本配置**
   编辑 `conf/kyuubi-defaults.conf`：
   ```properties
   kyuubi.frontend.bind.host=0.0.0.0
   kyuubi.frontend.bind.port=10009
   kyuubi.engine.share.level=USER
   kyuubi.ha.enabled=true
   kyuubi.ha.zookeeper.quorum=zk1:2181,zk2:2181,zk3:2181
   ```

## 服务管理

### 启动服务

1. **启动 Kyuubi Server**
   ```bash
   $KYUUBI_HOME/bin/kyuubi start
   ```

2. **验证服务状态**
   ```bash
   $KYUUBI_HOME/bin/kyuubi status
   ```

3. **查看日志**
   ```bash
   tail -f $KYUUBI_HOME/logs/kyuubi-{username}-server.log
   ```

### 停止服务

```bash
$KYUUBI_HOME/bin/kyuubi stop
```

## 客户端连接

### JDBC 连接

1. **添加依赖**
   ```xml
   <dependency>
     <groupId>org.apache.kyuubi</groupId>
     <artifactId>kyuubi-hive-jdbc</artifactId>
     <version>${kyuubi.version}</version>
   </dependency>
   ```

2. **连接示例**
   ```java
   import java.sql.Connection;
   import java.sql.DriverManager;

   public class KyuubiJdbcExample {
       public static void main(String[] args) {
           String url = "jdbc:hive2://kyuubi-server:10009";
           try (Connection conn = DriverManager.getConnection(url, "user", "password")) {
               // 执行查询
               Statement stmt = conn.createStatement();
               ResultSet rs = stmt.executeQuery("SELECT * FROM example_table");
               while (rs.next()) {
                   // 处理结果
               }
           }
       }
   }
   ```

### Beeline 使用

1. **连接服务器**
   ```bash
   $KYUUBI_HOME/bin/beeline -u "jdbc:hive2://kyuubi-server:10009"
   ```

2. **执行查询**
   ```sql
   -- 查看数据库
   SHOW DATABASES;
   
   -- 使用数据库
   USE example_db;
   
   -- 查询数据
   SELECT * FROM example_table LIMIT 10;
   ```

## 引擎管理

### 引擎配置

1. **基本配置**
   编辑 `conf/spark-defaults.conf`：
   ```properties
   spark.master=yarn
   spark.submit.deployMode=cluster
   spark.driver.memory=4g
   spark.executor.memory=4g
   spark.executor.cores=4
   spark.executor.instances=2
   ```

2. **高级配置**
   ```properties
   # 动态资源分配
   spark.dynamicAllocation.enabled=true
   spark.dynamicAllocation.minExecutors=1
   spark.dynamicAllocation.maxExecutors=10
   
   # 内存管理
   spark.memory.fraction=0.6
   spark.memory.storageFraction=0.5
   ```

### 引擎监控

1. **查看引擎状态**
   ```sql
   SHOW ENGINES;
   ```

2. **终止引擎**
   ```sql
   STOP ENGINE '{engine_id}';
   ```

## 安全配置

### Kerberos 认证

1. **服务端配置**
   编辑 `conf/kyuubi-defaults.conf`：
   ```properties
   kyuubi.authentication=KERBEROS
   kyuubi.authentication.kerberos.principal=kyuubi/_HOST@EXAMPLE.COM
   kyuubi.authentication.kerberos.keytab=/path/to/kyuubi.keytab
   ```

2. **客户端配置**
   ```bash
   kinit -kt /path/to/user.keytab user@EXAMPLE.COM
   beeline -u "jdbc:hive2://kyuubi-server:10009/;principal=kyuubi/_HOST@EXAMPLE.COM"
   ```

### LDAP 认证

1. **启用 LDAP**
   ```properties
   kyuubi.authentication=LDAP
   kyuubi.authentication.ldap.url=ldap://ldap-server:389
   kyuubi.authentication.ldap.baseDN=dc=example,dc=com
   ```

2. **用户映射**
   ```properties
   kyuubi.authentication.ldap.userDNPattern=uid=%s,ou=people,dc=example,dc=com
   ```

## 性能优化

### 查询优化

1. **分区裁剪**
   ```sql
   -- 使用分区过滤
   SELECT * FROM events 
   WHERE date_partition >= '2024-01-01' 
   AND date_partition < '2024-02-01';
   ```

2. **广播优化**
   ```sql
   -- 小表广播
   SELECT /*+ BROADCAST(small_table) */ *
   FROM large_table l
   JOIN small_table s ON l.id = s.id;
   ```

### 资源调优

1. **内存配置**
   ```properties
   # 执行内存
   spark.executor.memory=8g
   spark.executor.memoryOverhead=2g
   
   # 存储内存
   spark.memory.storageFraction=0.5
   ```

2. **并行度调整**
   ```properties
   spark.sql.shuffle.partitions=200
   spark.default.parallelism=200
   ```

## 监控与运维

### 指标监控

1. **Metrics 配置**
   编辑 `conf/metrics.properties`：
   ```properties
   *.sink.graphite.class=org.apache.spark.metrics.sink.GraphiteSink
   *.sink.graphite.host=graphite.example.com
   *.sink.graphite.port=2003
   *.sink.graphite.period=10
   ```

2. **关键指标**
   - 活跃会话数
   - 引擎数量
   - 查询延迟
   - 资源使用率

### 日志管理

1. **日志配置**
   编辑 `conf/log4j2.xml`：
   ```xml
   <Configuration>
     <Appenders>
       <RollingFile name="RollingFile">
         <FileName>${env:KYUUBI_LOG_DIR}/kyuubi.log</FileName>
         <FilePattern>${env:KYUUBI_LOG_DIR}/kyuubi-%d{yyyy-MM-dd}-%i.log.gz</FilePattern>
         <PatternLayout>
           <Pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %p %c{1}: %m%n</Pattern>
         </PatternLayout>
         <Policies>
           <TimeBasedTriggeringPolicy />
           <SizeBasedTriggeringPolicy size="250 MB"/>
         </Policies>
       </RollingFile>
     </Appenders>
   </Configuration>
   ```

2. **日志分析**
   ```bash
   # 查看错误日志
   grep ERROR $KYUUBI_HOME/logs/kyuubi-*.log
   
   # 分析慢查询
   grep "Slow Query" $KYUUBI_HOME/logs/kyuubi-*.log
   ```

## 开发指南

### 自定义函数

1. **创建 UDF**
   ```scala
   import org.apache.spark.sql.SparkSession
   import org.apache.spark.sql.expressions.UserDefinedFunction
   import org.apache.spark.sql.functions.udf
   
   object CustomUDF {
     def myFunction(input: String): String = {
       // 实现函数逻辑
       input.toUpperCase
     }
     
     val myUDF: UserDefinedFunction = udf(myFunction _)
   }
   ```

2. **注册和使用**
   ```sql
   CREATE TEMPORARY FUNCTION my_function AS 'com.example.CustomUDF';
   
   SELECT my_function(column_name) FROM table;
   ```

### REST API 使用

1. **API 认证**
   ```bash
   # 获取认证令牌
   curl -X POST http://kyuubi-server:10099/api/v1/token \
     -H "Content-Type: application/json" \
     -d '{"username": "user", "password": "password"}'
   ```

2. **查询执行**
   ```bash
   curl -X POST http://kyuubi-server:10099/api/v1/operations \
     -H "Authorization: Bearer ${TOKEN}" \
     -H "Content-Type: application/json" \
     -d '{
       "statement": "SELECT * FROM example_table LIMIT 10",
       "runAsync": true
     }'
   ```

## 故障排除

### 常见问题

1. **连接问题**
   - 检查网络连接
   - 验证认证配置
   - 确认服务状态

2. **性能问题**
   - 检查资源配置
   - 分析慢查询日志
   - 优化查询语句

### 诊断工具

1. **健康检查**
   ```bash
   $KYUUBI_HOME/bin/kyuubi diagnose
   ```

2. **日志分析**
   ```bash
   $KYUUBI_HOME/bin/kyuubi-ctl analyze-log
   ```

## 最佳实践

### 查询设计

1. **高效查询**
   ```sql
   -- 使用适当的过滤条件
   SELECT /*+ COALESCE(1) */ 
     date_col,
     COUNT(DISTINCT user_id) AS unique_users
   FROM events
   WHERE date_col >= DATE_SUB(CURRENT_DATE(), 30)
   GROUP BY date_col
   HAVING COUNT(*) > 100;
   ```

2. **数据倾斜处理**
   ```sql
   -- 使用 DISTRIBUTE BY 重分布数据
   SELECT /*+ REPARTITION(key) */
     key,
     COUNT(*) as cnt
   FROM skewed_table
   DISTRIBUTE BY key;
   ```

### 资源管理

1. **资源隔离**
   ```properties
   # 为不同用户组配置不同的资源池
   spark.yarn.queue=production
   spark.dynamicAllocation.maxExecutors=20
   ```

2. **并发控制**
   ```properties
   kyuubi.frontend.max.connections=100
   kyuubi.backend.session.max.lifetime=7d
   ```

本指南涵盖了 Kyuubi 的主要使用方法和最佳实践。随着 Kyuubi 的持续发展，建议定期查看官方文档以获取最新信息和更新。