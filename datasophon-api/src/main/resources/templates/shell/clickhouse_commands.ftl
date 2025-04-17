# ClickHouse命令行示例
# 格式说明：
# CMD> 开头的行表示命令输入
# RES> 开头的行表示命令输出结果
# TIP> 开头的行表示提示信息
# <---> 表示命令分隔符
# PRT> 开头的行表示命令提示符 (例如: [root@hostname ~]#)
#
# 注意：不要修改这些格式标记，它们会被系统解析用于生成交互式命令示例


# 连接到ClickHouse服务器
TIP> 使用clickhouse-client连接到服务器<#if data.getBasicInfoValue('tcpPort', '9000') != "9000">（使用非默认端口${data.getBasicInfoValue('tcpPort', '9000')}）</#if>
CMD> ${data.getConnectInfoValue('commandLine', 'clickhouse-client --host=localhost')}
RES> ClickHouse client version 21.8.10.1 (official build).
RES> Connecting to ${data.hostName}:${data.getBasicInfoValue('tcpPort', '9000')} as user ${data.getSecurityInfoValue('username', 'default')}.
RES> Connected to ClickHouse server version 21.8.10.

<--->

# 查看数据库列表
TIP> 查看所有可用的数据库
PRT> :) 
CMD> SHOW DATABASES;
RES> SHOW DATABASES
RES> 
RES> ┌─name───────────────┐
RES> │ default            │
RES> │ system             │
RES> └────────────────────┘
RES> 
RES> 2 rows in set. Elapsed: 0.003 sec.

<--->

# 创建数据库
TIP> 创建新的数据库
PRT> :) 
CMD> CREATE DATABASE IF NOT EXISTS example;
RES> CREATE DATABASE IF NOT EXISTS example
RES> 
RES> Query id: 43a17990-34a2-4e0e-930c-50a7cc9f9c65
RES> 
RES> Ok.
RES> 
RES> 0 rows in set. Elapsed: 0.004 sec.

<--->

# 使用数据库
TIP> 切换到指定数据库
PRT> :) 
CMD> USE example;
RES> USE example
RES> 
RES> Ok.
RES> 
RES> 0 rows in set. Elapsed: 0.001 sec.

<--->

# 创建表
TIP> 创建一个简单的表
PRT> :) 
CMD> CREATE TABLE IF NOT EXISTS example_table
CMD> (
CMD>     id UInt32,
CMD>     name String,
CMD>     value Float64,
CMD>     timestamp DateTime
CMD> ) ENGINE = MergeTree()
CMD> ORDER BY id;
RES> CREATE TABLE IF NOT EXISTS example_table
RES> (
RES>     id UInt32, 
RES>     name String, 
RES>     value Float64, 
RES>     timestamp DateTime
RES> ) ENGINE = MergeTree()
RES> ORDER BY id
RES> 
RES> Query id: ef73e4bd-c2dd-4ffe-b19a-0dc725e94f2d
RES> 
RES> Ok.
RES> 
RES> 0 rows in set. Elapsed: 0.008 sec.

<--->

# 插入数据
TIP> 向表中插入数据
PRT> :) 
CMD> INSERT INTO example_table VALUES
CMD> (1, 'test1', 10.5, now()),
CMD> (2, 'test2', 20.5, now());
RES> INSERT INTO example_table VALUES
RES> 
RES> Query id: 11f84ae1-0c9f-4c39-bec7-8a8a90c56cb8
RES> 
RES> Ok.
RES> 
RES> 2 rows in set. Elapsed: 0.005 sec.

<--->

# 查询数据
TIP> 从表中查询数据
PRT> :) 
CMD> SELECT * FROM example_table;
RES> SELECT *
RES> FROM example_table
RES> 
RES> ┌─id─┬─name──┬─value─┬───────────timestamp─┐
RES> │  1 │ test1 │  10.5 │ 2023-06-01 12:34:56 │
RES> │  2 │ test2 │  20.5 │ 2023-06-01 12:34:56 │
RES> └────┴───────┴───────┴─────────────────────┘
RES> 
RES> 2 rows in set. Elapsed: 0.003 sec.

<--->

# 退出ClickHouse客户端
TIP> 退出ClickHouse客户端返回到Shell
PRT> :) 
CMD> exit
RES> Bye.

<--->

# 使用HTTP接口
TIP> 通过HTTP接口查询数据
CMD> curl -s 'http://${data.hostName}:${data.getBasicInfoValue('httpPort', '8123')}/?query=SELECT+*+FROM+example.example_table+FORMAT+JSONEachRow'
RES> {"id":1,"name":"test1","value":10.5,"timestamp":"2023-06-01 12:34:56"}
RES> {"id":2,"name":"test2","value":20.5,"timestamp":"2023-06-01 12:34:56"}

<--->

# 再次连接并删除表
TIP> 重新连接ClickHouse并删除表
CMD> ${data.getConnectInfoValue('commandLine', 'clickhouse-client --host=localhost')}
RES> ClickHouse client version 21.8.10.1 (official build).
RES> Connecting to ${data.hostName}:${data.getBasicInfoValue('tcpPort', '9000')} as user ${data.getSecurityInfoValue('username', 'default')}.
RES> Connected to ClickHouse server version 21.8.10.

<--->

# 删除表
TIP> 删除表
PRT> :) 
CMD> DROP TABLE IF EXISTS example.example_table;
RES> DROP TABLE IF EXISTS example.example_table
RES> 
RES> Query id: 6d9c0ca1-1f1a-4d9f-8b27-6e8f0b7e9d34
RES> 
RES> Ok.
RES> 
RES> 0 rows in set. Elapsed: 0.006 sec.

<--->

# 删除数据库
TIP> 删除数据库
PRT> :) 
CMD> DROP DATABASE IF EXISTS example;
RES> DROP DATABASE IF EXISTS example
RES> 
RES> Query id: d4e75a3b-5a83-4e0e-b6d3-8f4bef9c7f90
RES> 
RES> Ok.
RES> 
RES> 0 rows in set. Elapsed: 0.005 sec.

<--->

# 退出ClickHouse客户端
TIP> 最后退出ClickHouse客户端
PRT> :) 
CMD> exit
RES> Bye. 