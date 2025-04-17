TIP> 以下是Hive命令行连接和常用操作示例
TIP> 命令前缀说明: 
TIP>   CMD> - 表示在Linux终端中输入的命令
TIP>   RES> - 表示命令执行后的输出结果
TIP>   PRT> - 表示终端提示符
TIP>   <--- - 表示命令说明

<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
TIP> 您的Hive集群启用了Kerberos认证，请先进行身份认证

PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ~]$ 
CMD> kinit -kt ${data.getSecurityInfoValue('keytab.path', '/path/to/user.keytab')} ${data.getSecurityInfoValue('principal', 'user@EXAMPLE.COM')}
RES> 
TIP> 验证Kerberos票据是否获取成功
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ~]$ 
CMD> klist
RES> Ticket cache: FILE:/tmp/krb5cc_1000
RES> Default principal: ${data.getSecurityInfoValue('principal', 'user@EXAMPLE.COM')}
RES> Valid starting       Expires              Service principal
RES> 01/01/2023 00:00:00  01/02/2023 00:00:00  krbtgt/EXAMPLE.COM@EXAMPLE.COM
</#if>

TIP> 连接到Hive服务器
TIP> 连接参数: 
TIP>  - 主机: ${data.getBasicInfoValue('host', 'localhost')}
TIP>  - 端口: ${data.getBasicInfoValue('port', '10000')}
TIP>  - 数据库: ${data.getConnectInfoValue('database', 'default')}

PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'hive'}]#
CMD> beeline -u "${data.getConnectInfoValue('jdbcUrl', 'jdbc:hive2://localhost:10000/default')}" <#if data.getSecurityInfoValue('username', '') != ''> -n "${data.getSecurityInfoValue('username', '')}" -p "${data.getSecurityInfoValue('password', '')}"</#if>
RES> Beeline version 3.1.2 by Apache Hive
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
RES> Connecting to jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')};principal=hive/_HOST@EXAMPLE.COM
<#else>
RES> Connecting to jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}
</#if>
RES> Connected to: Apache Hive (version 3.1.2)
RES> Driver: Hive JDBC (version 3.1.2)
RES> Transaction isolation: TRANSACTION_REPEATABLE_READ
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> !help
RES> !all                   执行由分号分隔的所有命令，忽略错误
RES> !connect <url>         连接到指定的数据库URL
RES> !dbinfo                显示当前连接的信息
RES> !quit                  退出beeline
RES> !history               显示命令历史记录

TIP> 1. 查看所有可用数据库
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> SHOW DATABASES;
RES> +----------------+
RES> | database_name  |
RES> +----------------+
RES> | default        |
RES> | examples       |
RES> | ${data.getConnectInfoValue('database', 'default')} |
RES> +----------------+
RES> 3 rows selected (0.145 seconds)

TIP> 2. 使用特定数据库
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> USE ${data.getConnectInfoValue('database', 'default')};
RES> No rows affected (0.152 seconds)

TIP> 3. 查看当前数据库中的表
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> SHOW TABLES;
RES> +--------------+
RES> |   tab_name   |
RES> +--------------+
RES> | customers    |
RES> | orders       |
RES> | products     |
RES> +--------------+
RES> 3 rows selected (0.152 seconds)

TIP> 4. 创建新表
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> CREATE TABLE IF NOT EXISTS example_table (
>   id INT,
>   name STRING,
>   value DOUBLE,
>   create_time TIMESTAMP
> )
> COMMENT 'Example table for demonstration'
> ROW FORMAT DELIMITED
> FIELDS TERMINATED BY ','
> STORED AS TEXTFILE;
RES> No rows affected (0.654 seconds)

TIP> 5. 查看表结构
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> DESCRIBE example_table;
RES> +-------------+------------+----------+
RES> | col_name    | data_type  | comment  |
RES> +-------------+------------+----------+
RES> | id          | int        |          |
RES> | name        | string     |          |
RES> | value       | double     |          |
RES> | create_time | timestamp  |          |
RES> +-------------+------------+----------+
RES> 4 rows selected (0.223 seconds)

TIP> 6. 插入数据
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> INSERT INTO TABLE example_table VALUES 
>   (1, 'Item 1', 10.5, CURRENT_TIMESTAMP),
>   (2, 'Item 2', 20.75, CURRENT_TIMESTAMP),
>   (3, 'Item 3', 30.25, CURRENT_TIMESTAMP);
RES> No rows affected (12.485 seconds)

TIP> 7. 查询数据
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> SELECT * FROM example_table;
RES> +------------------+------------------+------------------+-------------------------------+
RES> | example_table.id | example_table.name | example_table.value | example_table.create_time |
RES> +------------------+------------------+------------------+-------------------------------+
RES> | 1                | Item 1           | 10.5             | 2023-01-01 12:00:00.000      |
RES> | 2                | Item 2           | 20.75            | 2023-01-01 12:00:00.000      |
RES> | 3                | Item 3           | 30.25            | 2023-01-01 12:00:00.000      |
RES> +------------------+------------------+------------------+-------------------------------+
RES> 3 rows selected (0.332 seconds)

TIP> 8. 使用条件过滤查询数据
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> SELECT * FROM example_table WHERE value > 15.0;
RES> +------------------+------------------+------------------+-------------------------------+
RES> | example_table.id | example_table.name | example_table.value | example_table.create_time |
RES> +------------------+------------------+------------------+-------------------------------+
RES> | 2                | Item 2           | 20.75            | 2023-01-01 12:00:00.000      |
RES> | 3                | Item 3           | 30.25            | 2023-01-01 12:00:00.000      |
RES> +------------------+------------------+------------------+-------------------------------+
RES> 2 rows selected (0.302 seconds)

TIP> 9. 计算聚合值
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> SELECT AVG(value) AS average_value, MAX(value) AS max_value, MIN(value) AS min_value FROM example_table;
RES> +--------------------+--------------------+--------------------+
RES> |    average_value   |     max_value      |     min_value      |
RES> +--------------------+--------------------+--------------------+
RES> | 20.5              | 30.25             | 10.5              |
RES> +--------------------+--------------------+--------------------+
RES> 1 row selected (0.398 seconds)

TIP> 10. 修改表数据 (注意：标准Hive不直接支持UPDATE操作，除非启用了事务表功能)
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> UPDATE example_table SET value = 11.0 WHERE id = 1;
RES> Error: Error while compiling statement: FAILED: SemanticException [Error 10294]: Attempt to do update or delete using transaction manager that does not support these operations. (state=42000,code=10294)

TIP> 作为替代，可以使用INSERT OVERWRITE重写数据
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> INSERT OVERWRITE TABLE example_table
> SELECT id, name, 
>   CASE WHEN id = 1 THEN 11.0 ELSE value END AS value,
>   create_time
> FROM example_table;
RES> No rows affected (10.124 seconds)

TIP> 11. 删除表
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> DROP TABLE IF EXISTS temp_example_table;
RES> No rows affected (0.354 seconds)

TIP> 12. 退出Hive客户端
PRT> 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}> 
CMD> !quit
RES> Closing: 0: jdbc:hive2://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '10000')}/${data.getConnectInfoValue('database', 'default')}

TIP> 其他有用的Hive命令:
TIP> - 加载数据到表:
TIP>   LOAD DATA LOCAL INPATH '/path/to/file.csv' INTO TABLE example_table;
TIP> - 导出数据到文件:
TIP>   INSERT OVERWRITE LOCAL DIRECTORY '/path/to/output' SELECT * FROM example_table;
TIP> - 查看表详细信息:
TIP>   DESCRIBE FORMATTED example_table;
TIP> - 分区操作(如果表有分区):
TIP>   SHOW PARTITIONS example_table;
TIP> - 设置Hive参数:
TIP>   SET hive.execution.engine=tez; 