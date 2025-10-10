#!/bin/bash

# Doris命令行操作示例
# 依赖: Doris命令行工具
# 使用说明: 本文档展示了常用的Doris命令行操作，包括MySQL客户端连接和Doris自带工具的使用

# MySQL客户端连接
TIP> 使用MySQL客户端连接Doris FE
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'doris'}]# 
CMD> mysql -h ${data.getConnectInfoValue('feHost', 'localhost')} -P ${data.getConnectInfoValue('feQueryPort', '9030')} -u root<#if data.getSecurityInfoValue('password.enabled', 'false') == 'true'> -p"${data.getSecurityInfoValue('root.password', '')}"</#if>
RES> Welcome to the MySQL monitor.  Commands end with ; or \g.
RES> Your MySQL connection id is 119741
RES> Server version: 5.7.37 Doris version: 1.2.3-release-b56fea803
RES> 
RES> Copyright (c) 2000, 2022, Oracle and/or its affiliates.
RES> 
RES> Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.
RES> 
RES> mysql> 
<--->

# 基本信息查询
TIP> 查看Doris版本信息
PRT> mysql> 
CMD> SELECT * FROM information_schema.`DORIS_VARIABLES` WHERE VARIABLE_NAME = 'version';
RES> +---------------+----------------+
RES> | VARIABLE_NAME | VARIABLE_VALUE |
RES> +---------------+----------------+
RES> | version       | 1.2.3          |
RES> +---------------+----------------+
RES> 1 row in set (0.01 sec)
<--->

# 数据库操作
TIP> 创建数据库
PRT> mysql> 
CMD> CREATE DATABASE IF NOT EXISTS example_db;
RES> Query OK, 1 row affected (0.01 sec)
<--->

TIP> 查看所有数据库
PRT> mysql> 
CMD> SHOW DATABASES;
RES> +--------------------+
RES> | Database           |
RES> +--------------------+
RES> | example_db         |
RES> | information_schema |
RES> +--------------------+
RES> 2 rows in set (0.01 sec)
<--->

TIP> 切换到指定数据库
PRT> mysql> 
CMD> USE example_db;
RES> Database changed
<--->

# 表操作
TIP> 创建表(Aggregate模型)
PRT> mysql> 
CMD> CREATE TABLE example_tbl (
    user_id LARGEINT NOT NULL COMMENT '用户ID',
    date DATE NOT NULL COMMENT '数据灌入日期',
    city VARCHAR(20) COMMENT '用户所在城市',
    age SMALLINT COMMENT '用户年龄',
    sex TINYINT COMMENT '用户性别',
    last_visit_date DATETIME REPLACE DEFAULT '1970-01-01 00:00:00' COMMENT '用户最后一次访问时间',
    cost BIGINT SUM DEFAULT '0' COMMENT '用户总消费',
    max_dwell_time INT MAX DEFAULT '0' COMMENT '用户最大停留时间',
    min_dwell_time INT MIN DEFAULT '99999' COMMENT '用户最小停留时间'
) ENGINE=OLAP
AGGREGATE KEY(user_id, date, city, age, sex)
COMMENT '示例表'
DISTRIBUTED BY HASH(user_id) BUCKETS 10
PROPERTIES ('replication_num' = '3');
RES> Query OK, 0 rows affected (0.10 sec)
<--->

TIP> 查看表结构
PRT> mysql> 
CMD> DESC example_tbl;
RES> +------------------+-------------+------+-------+---------+-------+
RES> | Field            | Type        | Null | Key   | Default | Extra |
RES> +------------------+-------------+------+-------+---------+-------+
RES> | user_id          | LARGEINT    | No   | true  | NULL    |       |
RES> | date             | DATE        | No   | true  | NULL    |       |
RES> | city             | VARCHAR(20) | Yes  | true  | NULL    |       |
RES> | age              | SMALLINT    | Yes  | true  | NULL    |       |
RES> | sex              | TINYINT     | Yes  | true  | NULL    |       |
RES> | last_visit_date  | DATETIME    | Yes  | false | NULL    | REPLACE |
RES> | cost             | BIGINT      | Yes  | false | 0       | SUM   |
RES> | max_dwell_time   | INT         | Yes  | false | 0       | MAX   |
RES> | min_dwell_time   | INT         | Yes  | false | 99999   | MIN   |
RES> +------------------+-------------+------+-------+---------+-------+
RES> 9 rows in set (0.01 sec)
<--->

TIP> 插入数据
PRT> mysql> 
CMD> INSERT INTO example_tbl VALUES
    (10000, '2022-01-01', '北京', 20, 1, '2022-01-01 12:00:00', 100, 60, 30),
    (10001, '2022-01-01', '上海', 30, 0, '2022-01-01 13:00:00', 200, 120, 45),
    (10002, '2022-01-02', '广州', 25, 1, '2022-01-02 10:30:00', 150, 90, 40);
RES> Query OK, 3 rows affected (0.05 sec)
<--->

TIP> 查询数据
PRT> mysql> 
CMD> SELECT * FROM example_tbl LIMIT 10;
RES> +---------+------------+--------+------+------+---------------------+------+----------------+----------------+
RES> | user_id | date       | city   | age  | sex  | last_visit_date     | cost | max_dwell_time | min_dwell_time |
RES> +---------+------------+--------+------+------+---------------------+------+----------------+----------------+
RES> | 10000   | 2022-01-01 | 北京   |   20 |    1 | 2022-01-01 12:00:00 |  100 |             60 |             30 |
RES> | 10001   | 2022-01-01 | 上海   |   30 |    0 | 2022-01-01 13:00:00 |  200 |            120 |             45 |
RES> | 10002   | 2022-01-02 | 广州   |   25 |    1 | 2022-01-02 10:30:00 |  150 |             90 |             40 |
RES> +---------+------------+--------+------+------+---------------------+------+----------------+----------------+
RES> 3 rows in set (0.02 sec)
<--->

TIP> 聚合查询
PRT> mysql> 
CMD> SELECT city, SUM(cost) as total_cost FROM example_tbl GROUP BY city ORDER BY total_cost DESC;
RES> +--------+------------+
RES> | city   | total_cost |
RES> +--------+------------+
RES> | 上海   |        200 |
RES> | 广州   |        150 |
RES> | 北京   |        100 |
RES> +--------+------------+
RES> 3 rows in set (0.03 sec)
<--->

# 系统管理
TIP> 查看FE节点
PRT> mysql> 
CMD> SHOW PROC '/frontends';
RES> +------+---------------+-------------+--------+----------+----------+------------+------------------+----------------+---------------------+----------------+
RES> | Name | IP            | EditLogPort | HttpPort | QueryPort | RpcPort | Role       | IsMaster | ClusterId | Join             | Alive | ReplayedJournalId |
RES> +------+---------------+-------------+--------+----------+----------+------------+------------------+----------------+---------------------+----------------+
RES> | fe1  | 192.168.1.101 | 9010        | 8030   | 9030     | 9020     | FOLLOWER   | true            | 935313509    | 2022-01-01 10:00:00 | true  | 5000            |
RES> +------+---------------+-------------+--------+----------+----------+------------+------------------+----------------+---------------------+----------------+
RES> 1 row in set (0.02 sec)
<--->

TIP> 查看BE节点
PRT> mysql> 
CMD> SHOW PROC '/backends';
RES> +--------+---------------+------+----------+----------+---------------+----------+----------+---------------------+---------------------+-------+----------------------+
RES> | NodeId | IP            | HeartbeatPort | BePort | HttpPort | BrpcPort | LastStartTime | LastHeartbeat | Alive | SystemDecommissioned | TabletNum | DataUsedCapacity |
RES> +--------+---------------+------+----------+----------+---------------+----------+----------+---------------------+---------------------+-------+----------------------+
RES> | 10003  | 192.168.1.102 | 9050          | 9060  | 8040     | 8060     | 2022-01-01 10:00:00 | 2022-01-01 14:00:00 | true  | false              | 10       | 1.00 GB           |
RES> +--------+---------------+------+----------+----------+---------------+----------+----------+---------------------+---------------------+-------+----------------------+
RES> 1 row in set (0.02 sec)
<--->

# 返回Shell终端执行其他命令
TIP> 退出MySQL客户端
PRT> mysql> 
CMD> exit
RES> Bye
<--->

TIP> 使用HTTP接口检查FE状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'doris'}]# 
CMD> curl -s "http://${data.getConnectInfoValue('feHost', 'localhost')}:${data.getConnectInfoValue('feHttpPort', '8030')}/api/bootstrap" | python -m json.tool
RES> {
RES>     "msg": "success",
RES>     "code": 0,
RES>     "data": {
RES>         "queryPort": 9030,
RES>         "rpcPort": 9020,
RES>         "maxConnections": 1024,
RES>         "currentConnections": 42,
RES>         "user": "root",
RES>         "version": "1.2.3",
RES>         "jdk": "1.8.0_301",
RES>         "cluster": "default_cluster",
RES>         "isDeveloperEdition": false
RES>     },
RES>     "count": 0
RES> }
<--->

TIP> 查看BE状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'doris'}]# 
CMD> curl -s "http://${data.getConnectInfoValue('beHost', 'localhost')}:${data.getConnectInfoValue('beHttpPort', '8040')}/api/health" | python -m json.tool
RES> {
RES>     "msg": "OK",
RES>     "code": 0,
RES>     "be_status": {
RES>         "start_time": "Wed Jan 11 14:37:48 2023",
RES>         "mem_limit": "8.00 GB",
RES>         "cpu_count": 8
RES>     }
RES> }
<--->

# 数据导入导出
TIP> 准备数据文件
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'doris'}]# 
CMD> cat > example_data.csv << EOF
RES> 10003,2022-01-03,深圳,22,1,2022-01-03 12:30:45,120,75,35
RES> 10004,2022-01-03,杭州,28,0,2022-01-03 14:20:10,180,100,42
RES> EOF
<--->

TIP> 使用Stream Load导入数据
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'doris'}]# 
CMD> curl --location-trusted -u root<#if data.getSecurityInfoValue('password.enabled', 'false') == 'true'>:${data.getSecurityInfoValue('root.password', '')}</#if> -H "label:example_label" -H "column_separator:," -T example_data.csv http://${data.getConnectInfoValue('feHost', 'localhost')}:${data.getConnectInfoValue('feHttpPort', '8030')}/api/example_db/example_tbl/_stream_load
RES> {
RES>     "TxnId": 100,
RES>     "Label": "example_label",
RES>     "Status": "Success",
RES>     "Message": "OK",
RES>     "NumberTotalRows": 2,
RES>     "NumberLoadedRows": 2,
RES>     "NumberFilteredRows": 0,
RES>     "NumberUnselectedRows": 0,
RES>     "LoadBytes": 120,
RES>     "LoadTimeMs": 154,
RES>     "BeginTxnTimeMs": 15,
RES>     "StreamLoadPutTimeMs": 13,
RES>     "ReadDataTimeMs": 12,
RES>     "WriteDataTimeMs": 15,
RES>     "CommitAndPublishTimeMs": 42
RES> }
<--->

# 再次登录MySQL查看导入结果
TIP> 再次登录MySQL客户端
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'doris'}]# 
CMD> mysql -h ${data.getConnectInfoValue('feHost', 'localhost')} -P ${data.getConnectInfoValue('feQueryPort', '9030')} -u root<#if data.getSecurityInfoValue('password.enabled', 'false') == 'true'> -p"${data.getSecurityInfoValue('root.password', '')}"</#if>
RES> Welcome to the MySQL monitor.  Commands end with ; or \g.
RES> Your MySQL connection id is 119742
RES> Server version: 5.7.37 Doris version: 1.2.3-release-b56fea803
RES> 
RES> Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.
RES> 
RES> mysql> 
<--->

TIP> 查询导入结果
PRT> mysql> 
CMD> USE example_db;
RES> Database changed
<--->

PRT> mysql> 
CMD> SELECT COUNT(*) FROM example_tbl;
RES> +----------+
RES> | COUNT(*) |
RES> +----------+
RES> |        5 |
RES> +----------+
RES> 1 row in set (0.02 sec)
<--->

TIP> 导出查询结果
PRT> mysql> 
CMD> SELECT * FROM example_tbl INTO OUTFILE '/tmp/query_result.csv' FORMAT AS CSV;
RES> Query OK, 5 rows affected (0.05 sec)
<--->

TIP> 退出MySQL客户端
PRT> mysql> 
CMD> exit
RES> Bye
<--->

TIP> 查看导出结果
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'doris'}]# 
CMD> head /tmp/query_result.csv
RES> 10000,2022-01-01,北京,20,1,2022-01-01 12:00:00,100,60,30
RES> 10001,2022-01-01,上海,30,0,2022-01-01 13:00:00,200,120,45
RES> 10002,2022-01-02,广州,25,1,2022-01-02 10:30:00,150,90,40
RES> 10003,2022-01-03,深圳,22,1,2022-01-03 12:30:45,120,75,35
RES> 10004,2022-01-03,杭州,28,0,2022-01-03 14:20:10,180,100,42
<--->

# Broker操作
TIP> 登录MySQL查看Broker
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'doris'}]# 
CMD> mysql -h ${data.getConnectInfoValue('feHost', 'localhost')} -P ${data.getConnectInfoValue('feQueryPort', '9030')} -u root<#if data.getSecurityInfoValue('password.enabled', 'false') == 'true'> -p"${data.getSecurityInfoValue('root.password', '')}"</#if>
RES> Welcome to the MySQL monitor.  Commands end with ; or \g.
RES> Your MySQL connection id is 119743
RES> Server version: 5.7.37 Doris version: 1.2.3-release-b56fea803
RES> 
RES> Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.
RES> 
RES> mysql> 
<--->

TIP> 查看Broker
PRT> mysql> 
CMD> SHOW BROKER;
RES> +--------+---------------+------+-------+---------------+-------+---------+-------+
RES> | Name   | IP            | Port | Alive | LastStartTime | LastUpdateTime | ErrMsg |
RES> +--------+---------------+------+-------+---------------+-------+---------+-------+
RES> | hdfs_broker | 192.168.1.103 | 8000 | true  | 2022-01-01 10:00:00 | 2022-01-01 14:00:00 | NULL |
RES> +--------+---------------+------+-------+---------------+-------+---------+-------+
RES> 1 row in set (0.01 sec)
<--->

TIP> 使用Broker Export导出数据
PRT> mysql> 
CMD> EXPORT TABLE example_db.example_tbl 
    TO 'hdfs://hadoop-cluster/user/doris/export/example_data'
    PROPERTIES (
        'column_separator' = ',',
        'load_mem_limit' = '2147483648',
        'timeout' = '3600'
    ) WITH BROKER 'hdfs_broker' (
        'username' = 'hdfs',
        'password' = ''
    );
RES> +-------+-------------------------------------------+
RES> | JobId | State                                     |
RES> +-------+-------------------------------------------+
RES> | 10098 | PENDING                                   |
RES> +-------+-------------------------------------------+
RES> 1 row in set (0.03 sec)
<--->

# 用户权限管理
TIP> 创建新用户
PRT> mysql> 
CMD> CREATE USER 'test_user'@'%' IDENTIFIED BY 'test_password';
RES> Query OK, 0 rows affected (0.02 sec)
<--->

TIP> 授予权限
PRT> mysql> 
CMD> GRANT SELECT_PRIV ON example_db.* TO 'test_user'@'%';
RES> Query OK, 0 rows affected (0.01 sec)
<--->

TIP> 查看用户权限
PRT> mysql> 
CMD> SHOW GRANTS FOR 'test_user'@'%';
RES> +----------------------------------------------+
RES> | Grants for test_user@%                       |
RES> +----------------------------------------------+
RES> | GRANT SELECT_PRIV ON 'example_db'.* TO 'test_user'@'%' |
RES> +----------------------------------------------+
RES> 1 row in set (0.01 sec)
<--->

# 常用管理命令
TIP> 取消查询
PRT> mysql> 
CMD> KILL QUERY 12345; # 12345替换为实际的查询ID
RES> Query OK, 0 rows affected (0.01 sec)
<--->

TIP> 查看正在运行的查询
PRT> mysql> 
CMD> SHOW PROC '/queries';
RES> +----------+------+------+------------+-------------+---------------+-------------------------------+--------------------+--------------------+
RES> | QueryId  | ConnectionId | Database | User | JobMode | State | StartTime | Time | ScanBytes |
RES> +----------+------+------+------------+-------------+---------------+-------------------------------+--------------------+--------------------+
RES> | 100001   | 10001        | example_db | root | Query  | Running | 2022-01-01 15:00:00            | 10                 | 1024000            |
RES> +----------+------+------+------------+-------------+---------------+-------------------------------+--------------------+--------------------+
RES> 1 row in set (0.01 sec)
<--->

TIP> 查看内存使用
PRT> mysql> 
CMD> SHOW PROC '/cluster_balance/memory_stat';
RES> +------+---------------+---------------+----------------+------------------+
RES> | NodeId | TotalCapacity | AvailableCapacity | TotalUsedBytes | EstimatedAvailableBytes |
RES> +------+---------------+---------------+----------------+------------------+
RES> | 10003 | 8589934592    | 6442450944      | 2147483648     | 5368709120          |
RES> +------+---------------+---------------+----------------+------------------+
RES> 1 row in set (0.01 sec)
<--->

TIP> 退出MySQL客户端
PRT> mysql> 
CMD> exit
RES> Bye 