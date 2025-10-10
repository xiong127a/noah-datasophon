#!/bin/bash

# Redis命令行操作示例
# 依赖: redis-cli
# 需要安装Redis命令行工具


# 连接Redis服务器
TIP> 连接Redis服务器
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'redis'}]# 
CMD> bin/redis-cli -h ${data.getBasicInfoValue('host', 'localhost')} -p ${data.getBasicInfoValue('port', '6379')} <#if data.getSecurityInfoValue('password', '') != ''>-a ${data.getSecurityInfoValue('password', '')}</#if>
RES> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
<--->

# 测试连接
TIP> 测试连接
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
CMD> ping
RES> PONG
<--->

# 基本操作示例
TIP> 设置键值对
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
CMD> set test_key "Redis测试"
RES> OK
<--->

TIP> 获取键值
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
CMD> get test_key
RES> "Redis测试"
<--->

TIP> 设置过期时间
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
CMD> setex temp_key 60 "一分钟后过期"
RES> OK
<--->

TIP> 查看过期时间
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
CMD> ttl temp_key
RES> 60
<--->

TIP> 哈希操作
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
CMD> hset user:1 name "张三" age "30" city "北京"
RES> 3
<--->

TIP> 获取哈希数据
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
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
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
CMD> del test_key temp_key user:1
RES> 3
<--->

# 退出Redis客户端
TIP> 退出Redis客户端
PRT> ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '6379')}> 
CMD> quit
RES> OK 