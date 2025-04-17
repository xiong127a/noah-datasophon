#!/bin/bash

# Redis Sentinel命令行操作示例
# 依赖: redis-cli
# 需要安装Redis命令行工具


# 连接Sentinel节点
TIP> 连接Sentinel节点
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'redis-sentinel'}]# 
CMD> bin/redis-cli -h ${data.getBasicInfoValue('host', 'localhost')} -p ${data.getBasicInfoValue('port', '26379')} <#if data.getSecurityInfoValue('password', '') != ''>-a ${data.getSecurityInfoValue('password', '')}</#if>
RES> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '26379')}> 
<--->

# 测试Sentinel连接
TIP> 测试Sentinel连接
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '26379')}> 
CMD> ping
RES> PONG
<--->

# 获取主节点信息
TIP> 获取主节点信息
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '26379')}> 
CMD> SENTINEL get-master-addr-by-name ${data.getBasicInfoValue('masterName', 'mymaster')}
RES> 1) "127.0.0.1"
RES> 2) "6379"
<--->

# 获取从节点信息
TIP> 获取从节点信息
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '26379')}> 
CMD> SENTINEL slaves ${data.getBasicInfoValue('masterName', 'mymaster')}
RES> 1) 1) "name"
RES>    2) "127.0.0.1:6380"
RES>    3) "ip"
RES>    4) "127.0.0.1"
RES>    5) "port"
RES>    6) "6380"
<--->

# 获取哨兵节点信息
TIP> 获取哨兵节点信息
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '26379')}> 
CMD> SENTINEL sentinels ${data.getBasicInfoValue('masterName', 'mymaster')}
RES> 1) 1) "name"
RES>    2) "127.0.0.1:26380"
RES>    3) "ip"
RES>    4) "127.0.0.1"
RES>    5) "port"
RES>    6) "26380"
<--->

# 连接到主节点
TIP> 连接到主节点
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'redis-sentinel'}]# 
CMD> redis-cli -h 127.0.0.1 -p 6379 <#if data.getSecurityInfoValue('password', '') != ''>-a ${data.getSecurityInfoValue('password', '')}</#if>
RES> 127.0.0.1:6379> 
<--->

# 测试主节点连接
TIP> 测试主节点连接
PRT> 127.0.0.1:6379> 
CMD> ping
RES> PONG
<--->

# 基本操作示例
TIP> 设置键值对
PRT> 127.0.0.1:6379> 
CMD> set test_key "哨兵模式测试"
RES> OK
<--->

TIP> 获取键值
PRT> 127.0.0.1:6379> 
CMD> get test_key
RES> "哨兵模式测试"
<--->

TIP> 设置过期时间
PRT> 127.0.0.1:6379> 
CMD> setex temp_key 60 "一分钟后过期"
RES> OK
<--->

TIP> 查看过期时间
PRT> 127.0.0.1:6379> 
CMD> ttl temp_key
RES> 60
<--->

TIP> 哈希操作
PRT> 127.0.0.1:6379> 
CMD> hset user:1 name "张三" age "30" city "北京"
RES> 3
<--->

TIP> 获取哈希数据
PRT> 127.0.0.1:6379> 
CMD> hgetall user:1
RES> 1) "name"
RES> 2) "张三"
RES> 3) "age"
RES> 4) "30"
RES> 5) "city"
RES> 6) "北京"
<--->

# 清理测试数据
TIP> 清理测试数据
PRT> 127.0.0.1:6379> 
CMD> del test_key temp_key user:1
RES> 3
<--->

# 退出Redis客户端
TIP> 退出Redis客户端
PRT> 127.0.0.1:6379> 
CMD> quit
RES> OK 