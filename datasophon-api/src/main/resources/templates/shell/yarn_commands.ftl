#!/bin/bash

# YARN（ResourceManager）命令行操作示例
# 依赖: hadoop, yarn 命令行工具

# 进入Hadoop安装目录
TIP> 进入Hadoop安装目录
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ~]# 
CMD> cd ${data.serviceHome}
<--->

# 获取YARN集群状态
TIP> 查看YARN集群状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn cluster -list
RES> 17/01/01 00:00:00 INFO client.RMProxy: Connecting to ResourceManager at ${data.getBasicInfoValue('connectString', 'localhost:8032')}
RES> Total Clusters:1
RES> 
RES>                                 YARN Cluster ID: cluster
<--->

# 查看YARN节点状态
TIP> 查看所有NodeManager节点状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn node -list -all
RES> 17/01/01 00:00:00 INFO client.RMProxy: Connecting to ResourceManager at ${data.getBasicInfoValue('connectString', 'localhost:8032')}
RES> Total Nodes:3
RES> Node-Id                     Node-State Node-Http-Address       Number-of-Running-Containers
RES> node1:45454                 RUNNING    node1:8042              5
RES> node2:45454                 RUNNING    node2:8042              3
RES> node3:45454                 RUNNING    node3:8042              2
<--->

# 查看YARN应用列表
TIP> 查看正在运行的应用列表
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn application -list
RES> 17/01/01 00:00:00 INFO client.RMProxy: Connecting to ResourceManager at ${data.getBasicInfoValue('connectString', 'localhost:8032')}
RES> Total number of applications (application-types: [] and states: [SUBMITTED, ACCEPTED, RUNNING]):3
RES> 
RES>                 Application-Id      Application-Name        Application-Type          User           Queue                   State         Final-State         Progress                       Tracking-URL
RES> application_1626344812393_0006     Spark Example                 SPARK           hadoop        default               RUNNING         UNDEFINED             90%                  http://node1:4040
RES> application_1626344812393_0005     MapReduce Job               MAPREDUCE           hdfs        default               RUNNING         UNDEFINED             75%            http://node1:8088/proxy/...
RES> application_1626344812393_0004     Tez Job                        TEZ            hive        default               RUNNING         UNDEFINED             50%            http://node2:8088/proxy/...
<--->

# 查看特定应用的详细信息
TIP> 查看特定应用的详细信息（替换为实际的Application-Id）
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn application -status application_1626344812393_0006
RES> 17/01/01 00:00:00 INFO client.RMProxy: Connecting to ResourceManager at ${data.getBasicInfoValue('connectString', 'localhost:8032')}
RES> Application Report : 
RES> 	Application-Id : application_1626344812393_0006
RES> 	Application-Name : Spark Example
RES> 	Application-Type : SPARK
RES> 	User : hadoop
RES> 	Queue : default
RES> 	Start-Time : 1626345678910
RES> 	Finish-Time : 0
RES> 	Progress : 90%
RES> 	State : RUNNING
RES> 	Final-State : UNDEFINED
RES> 	Tracking-URL : http://node1:4040
RES> 	RPC Port : 38913
RES> 	AM Host : node1
RES> 	Aggregate Resource Allocation : 63400 MB-seconds, 32 vcore-seconds
RES> 	Aggregate Resource Preempted : 0 MB-seconds, 0 vcore-seconds
RES> 	Log Aggregation Status : DISABLED
RES> 	Diagnostics : 
RES> 	Unmanaged Application : false
RES> 	Application Node Label Expression : <DEFAULT_PARTITION>
RES> 	AM container Node Label Expression : <DEFAULT_PARTITION>
<--->

# 终止应用
TIP> 终止运行中的应用（替换为实际的Application-Id）
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn application -kill application_1626344812393_0006
RES> 17/01/01 00:00:00 INFO client.RMProxy: Connecting to ResourceManager at ${data.getBasicInfoValue('connectString', 'localhost:8032')}
RES> Killing application application_1626344812393_0006
RES> 17/01/01 00:00:00 INFO client.RMProxy: Connecting to ResourceManager at ${data.getBasicInfoValue('connectString', 'localhost:8032')}
RES> Application application_1626344812393_0006 has been killed successfully
<--->

# 查看应用日志
TIP> 查看应用的聚合日志（替换为实际的Application-Id）
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn logs -applicationId application_1626344812393_0005
RES> 17/01/01 00:00:00 INFO client.RMProxy: Connecting to ResourceManager at ${data.getBasicInfoValue('connectString', 'localhost:8032')}
RES> 17/01/01 00:00:00 INFO client.AHSProxy: Connecting to Application History server at ${data.getBasicInfoValue('host', 'localhost')}:10200
RES> ===================================
RES> Container: container_1626344812393_0005_01_000001 on node1_45454
RES> LogType: stdout
RES> LogLength: 2032
RES> Log Contents:
RES> ...
RES> 
RES> ...
RES> Container: container_1626344812393_0005_01_000002 on node2_45454
RES> LogType: stderr
RES> LogLength: 986
RES> Log Contents:
RES> ...
<--->

# 查看队列
TIP> 查看YARN队列信息
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn queue -status default
RES> 17/01/01 00:00:00 INFO client.RMProxy: Connecting to ResourceManager at ${data.getBasicInfoValue('connectString', 'localhost:8032')}
RES> Queue Information : 
RES> Queue Name : default
RES> 	State : RUNNING
RES> 	Capacity : 100%
RES> 	Current Capacity : 30.0%
RES> 	Maximum Capacity : 100%
RES> 	Default Node Label expression : <DEFAULT_PARTITION>
RES> 	Accessible Node Labels : *
RES> 	Preemption : disabled
RES> 	Intra-queue Preemption : disabled
<--->

<#if data.getBasicInfoValue('deployMode', '单节点模式') == '高可用模式'>
# 查看ResourceManager HA状态
TIP> 查看ResourceManager的高可用状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn rmadmin -getServiceState rm1
RES> active
<--->

TIP> 查看备用ResourceManager的状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn rmadmin -getServiceState rm2
RES> standby
<--->

TIP> 查看所有ResourceManager节点信息
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/yarn rmadmin -getAllServiceState
RES> ${data.getBasicInfoValue('rm1', 'rm1')}: active
RES> ${data.getBasicInfoValue('rm2', 'rm2')}: standby
<--->
</#if>

<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
# Kerberos相关操作
TIP> 获取Kerberos票据
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> kinit -kt ${data.getSecurityInfoValue('keytab', '/etc/security/keytabs/yarn.keytab')} ${data.getSecurityInfoValue('principal', 'yarn/localhost@EXAMPLE.COM')}
<--->

TIP> 查看当前Kerberos票据
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> klist
RES> Ticket cache: FILE:/tmp/krb5cc_0
RES> Default principal: ${data.getSecurityInfoValue('principal', 'yarn/localhost@EXAMPLE.COM')}
RES> 
RES> Valid starting       Expires              Service principal
RES> 01/01/2023 00:00:00  01/02/2023 00:00:00  krbtgt/EXAMPLE.COM@EXAMPLE.COM
<--->
</#if>

# 提交示例应用
TIP> 提交示例MapReduce作业
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> bin/hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-*.jar pi 10 1000
RES> Number of Maps  = 10
RES> Samples per Map = 1000
RES> ...
RES> Job Finished in 38.572 seconds
RES> Estimated value of Pi is 3.14160000000000000000
<--->

# 查看YARN应用历史
TIP> 查看YARN应用历史服务
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> curl -s http://${data.getBasicInfoValue('host', 'localhost')}:8188/ws/v1/applicationhistory/apps | head -20
RES> {
RES>   "apps" : {
RES>     "app" : [ {
RES>       "appId" : "application_1626344812393_0006",
RES>       "user" : "hadoop",
RES>       "name" : "Spark Example",
RES>       "queue" : "default",
RES>       "state" : "FINISHED",
RES>       "finalStatus" : "SUCCEEDED",
RES>       "progress" : 100,
RES>       "trackingUrl" : "http://node1:8088/proxy/application_1626344812393_0006/",
RES>       "diagnostics" : "",
RES>       "clusterId" : 1626344812393,
RES>       "applicationType" : "SPARK",
RES>       "applicationTags" : "",
RES>       "startedTime" : 1626345678910,
RES>       "finishedTime" : 1626345778910,
RES>       "elapsedTime" : 100000
RES>     }, {
<--->

# 查看ResourceManager Web UI
TIP> 使用浏览器访问ResourceManager Web UI
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome}]# 
CMD> echo "在浏览器中访问: ${data.getBasicInfoValue('webAddress', 'http://localhost:8088')}"
RES> 在浏览器中访问: ${data.getBasicInfoValue('webAddress', 'http://localhost:8088')}
<---> 