#!/bin/bash

# ZooKeeper命令行操作示例
# 依赖: ZooKeeper客户端工具zkCli.sh
# 需要安装ZooKeeper客户端工具

# 连接ZooKeeper服务器
TIP> 连接ZooKeeper服务器
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'zookeeper'}]# 
CMD> bin/zkCli.sh -server ${data.getBasicInfoValue('connectString', 'localhost:2181')}
RES> Connecting to ${data.getBasicInfoValue('connectString', 'localhost:2181')}
RES> Welcome to ZooKeeper!
RES> JLine support is enabled
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 0]
<--->

# 检查服务器状态
TIP> 检查服务器状态
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 0] 
CMD> stat
RES> Zookeeper version: 3.5.10-76f19f5a606749fb3ed82c30c87d6b648ba853c2, built on 02/25/2022 13:18 GMT
RES> Clients:
RES> /127.0.0.1:61527[0](queued=0,recved=1,sent=0)
RES> 
RES> Latency min/avg/max: 0/0/0
RES> Received: 2
RES> Sent: 1
RES> Connections: 1
RES> Outstanding: 0
RES> Zxid: 0x0
RES> Mode: standalone
RES> Node count: 5
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 1]
<--->

# 列出根节点下的子节点
TIP> 列出根节点下的子节点
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 1] 
CMD> ls /
RES> [zookeeper]
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 2]
<--->

# 创建ZNode节点
TIP> 创建ZNode节点
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 2] 
CMD> create /example "测试数据"
RES> Created /example
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 3]
<--->

# 获取节点数据
TIP> 获取节点数据
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 3] 
CMD> get /example
RES> 测试数据
RES> 
RES> cZxid = 0x2
RES> ctime = Wed Jul 05 12:34:56 CST 2023
RES> mZxid = 0x2
RES> mtime = Wed Jul 05 12:34:56 CST 2023
RES> pZxid = 0x2
RES> cversion = 0
RES> dataVersion = 0
RES> aclVersion = 0
RES> ephemeralOwner = 0x0
RES> dataLength = 9
RES> numChildren = 0
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 4]
<--->

# 修改节点数据
TIP> 修改节点数据
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 4] 
CMD> set /example "更新后的数据"
RES> 
RES> cZxid = 0x2
RES> ctime = Wed Jul 05 12:34:56 CST 2023
RES> mZxid = 0x3
RES> mtime = Wed Jul 05 12:36:12 CST 2023
RES> pZxid = 0x2
RES> cversion = 0
RES> dataVersion = 1
RES> aclVersion = 0
RES> ephemeralOwner = 0x0
RES> dataLength = 15
RES> numChildren = 0
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 5]
<--->

# 创建子节点
TIP> 创建子节点
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 5] 
CMD> create /example/child "子节点数据"
RES> Created /example/child
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 6]
<--->

# 列出子节点
TIP> 列出子节点
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 6] 
CMD> ls /example
RES> [child]
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 7]
<--->

# 递归删除节点
TIP> 递归删除节点
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 7] 
CMD> deleteall /example
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 8]
<--->

# 验证节点已删除
TIP> 验证节点已删除
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 8] 
CMD> ls /example
RES> Node does not exist: /example
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 9]
<--->

<#if data.getBasicInfoValue('deployMode', '单节点模式') == '集群模式'>
# 查看集群配置
TIP> 查看集群配置(仅在集群模式可用)
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 9] 
CMD> config
RES> server.1=${data.getBasicInfoValue('server1', 'localhost')}:2888:3888:participant
RES> server.2=${data.getBasicInfoValue('server2', 'localhost')}:2888:3888:participant
RES> server.3=${data.getBasicInfoValue('server3', 'localhost')}:2888:3888:participant
RES> version=0
RES> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) 10]
<--->
</#if>

# 退出ZooKeeper客户端
TIP> 退出ZooKeeper客户端
PRT> [zk: ${data.getBasicInfoValue('connectString', 'localhost:2181')}(CONNECTED) <#if data.getBasicInfoValue('deployMode', '单节点模式') == '集群模式'>10<#else>9</#if>] 
CMD> quit
RES> Quitting...
RES> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'zookeeper'}]#
<--->

# 使用四字命令检查服务器状态
TIP> 使用四字命令检查服务器状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'zookeeper'}]# 
CMD> echo stat | nc localhost 2181
RES> Zookeeper version: 3.5.10-76f19f5a606749fb3ed82c30c87d6b648ba853c2, built on 02/25/2022 13:18 GMT
RES> Clients:
RES> /127.0.0.1:61527[0](queued=0,recved=1,sent=0)
RES> 
RES> Latency min/avg/max: 0/0/0
RES> Received: 2
RES> Sent: 1
RES> Connections: 1
RES> Outstanding: 0
RES> Zxid: 0x0
RES> Mode: standalone
RES> Node count: 5
<--->

# 查看ZooKeeper服务状态
TIP> 查看ZooKeeper服务状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'zookeeper'}]# 
CMD> bin/zkServer.sh status
RES> ZooKeeper JMX enabled by default
RES> Using config: /opt/zookeeper/bin/../conf/zoo.cfg
RES> Mode: standalone
<--->

<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
# Kerberos环境下的操作
TIP> Kerberos环境下的操作 - 获取票据
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'zookeeper'}]# 
CMD> kinit -kt /etc/security/keytabs/zookeeper.keytab ${data.getSecurityInfoValue('principal', 'zookeeper/localhost@EXAMPLE.COM')}
<--->

TIP> 查看当前的Kerberos票据
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.serviceHome!'zookeeper'}]# 
CMD> klist
RES> Ticket cache: FILE:/tmp/krb5cc_0
RES> Default principal: ${data.getSecurityInfoValue('principal', 'zookeeper/localhost@EXAMPLE.COM')}
RES> 
RES> Valid starting     Expires            Service principal
RES> 07/05/23 12:00:00  07/06/23 12:00:00  krbtgt/EXAMPLE.COM@EXAMPLE.COM
<--->
</#if>