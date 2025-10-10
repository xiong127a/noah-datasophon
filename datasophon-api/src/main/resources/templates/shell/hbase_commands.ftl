#!/bin/bash

# HBase命令行操作示例
# 依赖: hbase-client


# 查看HBase服务状态
TIP> 查看HBase服务状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'hbase'}]# 
CMD> ./bin/hbase-daemon.sh status master
RES> hbase-master is running as process ${r'${RANDOM % 10000 + 10000}'}. Stop it first.
<--->

# 启动HBase Shell
TIP> 启动HBase Shell
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'hbase'}]# 
CMD> ./bin/hbase shell
RES> HBase Shell
RES> Use "help" to get list of supported commands.
RES> Use "exit" to quit this interactive shell.
RES> For Reference, please visit: http://hbase.apache.org/2.0/book.html#shell
RES> Version ${r'${RANDOM % 10 + 2}'}.${r'${RANDOM % 10}'}.${r'${RANDOM % 10}'}, r${r'${RANDOM % 1000 + 1000}'}, Wed ${r'${RANDOM % 30 + 1}'} ${r'["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][RANDOM % 12]'} ${r'${RANDOM % 5 + 2020}'} ${r'${RANDOM % 12 + 10}'}:${r'${RANDOM % 60}'}:${r'${RANDOM % 60}'} UTC
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds 
RES> hbase(main):001:0> 
<--->

# 查看服务器状态
TIP> 查看服务器状态
PRT> hbase(main):001:0> 
CMD> status
RES> ${data.getBasicInfoValue('host', 'localhost')}: ${r'${RANDOM % 10000 + 10000}'} ${r'${RANDOM % 100 + 1}'} active, ${r'${RANDOM % 10}'} dead, ${r'${RANDOM % 10}'} average load
<--->

# 获取集群状态
TIP> 获取集群状态
PRT> hbase(main):002:0> 
CMD> status 'detailed'
RES> version ${r'${RANDOM % 10 + 2}'}.${r'${RANDOM % 10}'}.${r'${RANDOM % 10}'}
RES> ${r'${RANDOM % 10 + 1}'} regionsInTransition
RES> ${r'${RANDOM % 100 + 100}'} live servers, ${r'${RANDOM % 10}'} dead servers, ${r'${RANDOM % 10}'} average load
RES> active master: ${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '16000')}
<#if data.getBasicInfoValue('highAvailability', '否') == '是'>
RES> backup masters: ${data.getBasicInfoValue('masterNodes', 'master2.example.com:16000,master3.example.com:16000')?split(',')?first}
</#if>
RES> 0 backup masters
RES> List of dead servers:
RES> none
<--->

# 列出所有表
TIP> 列出所有表
PRT> hbase(main):003:0> 
CMD> list
RES> TABLE                                                                                                                                                          
RES> my_table                                                                                                                   
RES> test_table                                                                                                                        
RES> transactions                                                                                                                            
RES> user_profile                                                                                                                              
RES> 4 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                           
<--->

# 创建新表
TIP> 创建新表
PRT> hbase(main):004:0> 
CMD> create 'test_table_new', {NAME => 'cf1', VERSIONS => 3}, {NAME => 'cf2', TTL => 86400}
RES> Created table test_table_new
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
RES> => Hbase::Table - test_table_new
<--->

# 查看表结构
TIP> 查看表结构
PRT> hbase(main):005:0> 
CMD> describe 'test_table_new'
RES> Table test_table_new is ENABLED                                                                                                                     
RES> test_table_new                                                                                                                                      
RES> COLUMN FAMILIES DESCRIPTION                                                                                                                         
RES> {NAME => 'cf1', BLOOMFILTER => 'ROW', VERSIONS => '3', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => 'FOREVER', COMPRESSION => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}                                                                                                                                                 
RES> {NAME => 'cf2', BLOOMFILTER => 'ROW', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => '86400', COMPRESSION => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}                                                                                                                                                     
RES> 2 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 插入数据
TIP> 插入数据
PRT> hbase(main):006:0> 
CMD> put 'test_table_new', 'row1', 'cf1:name', '张三'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

TIP> 插入更多数据
PRT> hbase(main):007:0> 
CMD> put 'test_table_new', 'row1', 'cf1:age', '30'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds  
<--->

TIP> 插入更多数据
PRT> hbase(main):008:0> 
CMD> put 'test_table_new', 'row1', 'cf2:email', 'zhangsan@example.com'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds  
<--->

TIP> 插入第二行数据
PRT> hbase(main):009:0> 
CMD> put 'test_table_new', 'row2', 'cf1:name', '李四'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds
<--->

TIP> 插入更多数据
PRT> hbase(main):010:0> 
CMD> put 'test_table_new', 'row2', 'cf1:age', '25'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds
<--->

TIP> 插入更多数据
PRT> hbase(main):011:0> 
CMD> put 'test_table_new', 'row2', 'cf2:email', 'lisi@example.com'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds
<--->

# 获取单行数据
TIP> 获取单行数据
PRT> hbase(main):012:0> 
CMD> get 'test_table_new', 'row1'
RES> COLUMN                                        CELL                                                                                                   
RES>  cf1:age                                      timestamp=${r'${RANDOM % 10000000000000}'}, value=30                                                          
RES>  cf1:name                                     timestamp=${r'${RANDOM % 10000000000000}'}, value=张三                                                        
RES>  cf2:email                                    timestamp=${r'${RANDOM % 10000000000000}'}, value=zhangsan@example.com                                        
RES> 3 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 获取特定列族的数据
TIP> 获取特定列族的数据
PRT> hbase(main):013:0> 
CMD> get 'test_table_new', 'row1', {COLUMN => 'cf1'}
RES> COLUMN                                        CELL                                                                                                   
RES>  cf1:age                                      timestamp=${r'${RANDOM % 10000000000000}'}, value=30                                                          
RES>  cf1:name                                     timestamp=${r'${RANDOM % 10000000000000}'}, value=张三                                                        
RES> 2 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 获取特定列的数据
TIP> 获取特定列的数据
PRT> hbase(main):014:0> 
CMD> get 'test_table_new', 'row1', {COLUMN => 'cf1:name'}
RES> COLUMN                                        CELL                                                                                                   
RES>  cf1:name                                     timestamp=${r'${RANDOM % 10000000000000}'}, value=张三                                                        
RES> 1 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 扫描表
TIP> 扫描表中的所有数据
PRT> hbase(main):015:0> 
CMD> scan 'test_table_new'
RES> ROW                                           COLUMN+CELL                                                                                             
RES>  row1                                         column=cf1:age, timestamp=${r'${RANDOM % 10000000000000}'}, value=30                                            
RES>  row1                                         column=cf1:name, timestamp=${r'${RANDOM % 10000000000000}'}, value=张三                                          
RES>  row1                                         column=cf2:email, timestamp=${r'${RANDOM % 10000000000000}'}, value=zhangsan@example.com                        
RES>  row2                                         column=cf1:age, timestamp=${r'${RANDOM % 10000000000000}'}, value=25                                            
RES>  row2                                         column=cf1:name, timestamp=${r'${RANDOM % 10000000000000}'}, value=李四                                          
RES>  row2                                         column=cf2:email, timestamp=${r'${RANDOM % 10000000000000}'}, value=lisi@example.com                            
RES> 2 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 扫描表（设置范围）
TIP> 扫描表中的部分数据（行键范围）
PRT> hbase(main):016:0> 
CMD> scan 'test_table_new', {STARTROW => 'row1', ENDROW => 'row2'}
RES> ROW                                           COLUMN+CELL                                                                                             
RES>  row1                                         column=cf1:age, timestamp=${r'${RANDOM % 10000000000000}'}, value=30                                            
RES>  row1                                         column=cf1:name, timestamp=${r'${RANDOM % 10000000000000}'}, value=张三                                          
RES>  row1                                         column=cf2:email, timestamp=${r'${RANDOM % 10000000000000}'}, value=zhangsan@example.com                        
RES> 1 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 计数表中的行数
TIP> 计数表中的行数
PRT> hbase(main):017:0> 
CMD> count 'test_table_new'
RES> 2 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 更新数据
TIP> 更新数据（覆盖现有数据）
PRT> hbase(main):018:0> 
CMD> put 'test_table_new', 'row1', 'cf1:age', '31'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 验证更新
TIP> 验证数据更新
PRT> hbase(main):019:0> 
CMD> get 'test_table_new', 'row1', {COLUMN => 'cf1:age'}
RES> COLUMN                                        CELL                                                                                                   
RES>  cf1:age                                      timestamp=${r'${RANDOM % 10000000000000}'}, value=31                                                          
RES> 1 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 删除特定列
TIP> 删除特定列
PRT> hbase(main):020:0> 
CMD> delete 'test_table_new', 'row2', 'cf2:email'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 验证列删除
TIP> 验证列删除
PRT> hbase(main):021:0> 
CMD> get 'test_table_new', 'row2'
RES> COLUMN                                        CELL                                                                                                   
RES>  cf1:age                                      timestamp=${r'${RANDOM % 10000000000000}'}, value=25                                                          
RES>  cf1:name                                     timestamp=${r'${RANDOM % 10000000000000}'}, value=李四                                                        
RES> 2 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 删除整行
TIP> 删除整行
PRT> hbase(main):022:0> 
CMD> deleteall 'test_table_new', 'row2'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 验证行删除
TIP> 验证行删除
PRT> hbase(main):023:0> 
CMD> scan 'test_table_new'
RES> ROW                                           COLUMN+CELL                                                                                             
RES>  row1                                         column=cf1:age, timestamp=${r'${RANDOM % 10000000000000}'}, value=31                                            
RES>  row1                                         column=cf1:name, timestamp=${r'${RANDOM % 10000000000000}'}, value=张三                                          
RES>  row1                                         column=cf2:email, timestamp=${r'${RANDOM % 10000000000000}'}, value=zhangsan@example.com                        
RES> 1 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 高级操作：显示表的区域信息
TIP> 显示表的区域信息
PRT> hbase(main):024:0> 
CMD> list_regions 'test_table_new'
RES> REGION                                        STARTKEY                          ENDKEY                            REGION_SERVER                                           
RES> test_table_new,,1680195873396.${r'${RANDOM % 10000 + 10000}'}.               -                                   -                                      ${data.getBasicInfoValue('host', 'localhost')}:16020                                  
RES> 1 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 高级操作：禁用表
TIP> 禁用表（进行管理操作前需要先禁用）
PRT> hbase(main):025:0> 
CMD> disable 'test_table_new'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 验证表已禁用
TIP> 验证表已禁用
PRT> hbase(main):026:0> 
CMD> is_disabled 'test_table_new'
RES> true
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 高级操作：修改表结构
TIP> 修改表结构（添加新列族）
PRT> hbase(main):027:0> 
CMD> alter 'test_table_new', {NAME => 'cf3', COMPRESSION => 'SNAPPY'}
RES> Updating all regions with the new schema...
RES> 1/1 regions updated.
RES> Done.
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 重新启用表
TIP> 重新启用表
PRT> hbase(main):028:0> 
CMD> enable 'test_table_new'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 验证表已启用
TIP> 验证表已启用
PRT> hbase(main):029:0> 
CMD> is_enabled 'test_table_new'
RES> true
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 验证表结构已更新
TIP> 验证表结构已更新
PRT> hbase(main):030:0> 
CMD> describe 'test_table_new'
RES> Table test_table_new is ENABLED                                                                                                                     
RES> test_table_new                                                                                                                                      
RES> COLUMN FAMILIES DESCRIPTION                                                                                                                         
RES> {NAME => 'cf1', BLOOMFILTER => 'ROW', VERSIONS => '3', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => 'FOREVER', COMPRESSION => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}                                                                                                                                                 
RES> {NAME => 'cf2', BLOOMFILTER => 'ROW', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => '86400', COMPRESSION => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}
RES> {NAME => 'cf3', BLOOMFILTER => 'ROW', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => 'FOREVER', COMPRESSION => 'SNAPPY', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}                                                                                                                                                
RES> 3 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 删除表（先禁用）
TIP> 删除表（先禁用）
PRT> hbase(main):031:0> 
CMD> disable 'test_table_new'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

TIP> 删除表
PRT> hbase(main):032:0> 
CMD> drop 'test_table_new'
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                                   
<--->

# 验证表已删除
TIP> 验证表已删除
PRT> hbase(main):033:0> 
CMD> list
RES> TABLE                                                                                                                                                          
RES> my_table                                                                                                                   
RES> test_table                                                                                                                        
RES> transactions                                                                                                                            
RES> user_profile                                                                                                                              
RES> 4 row(s)
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds                                                                                            
<--->

# 查看HBase状态
TIP> 查看HBase状态
PRT> hbase(main):034:0> 
CMD> status 'simple'
RES> 1 active master, 0 backup masters, ${r'${RANDOM % 10 + 1}'} regionservers, 0 dead, ${r'${RANDOM % 10 + 1}'}.00 average load
<--->

# 退出HBase Shell
TIP> 退出HBase Shell
PRT> hbase(main):035:0> 
CMD> exit
RES> Took ${r'${RANDOM % 10 + 1}'}.${r'${RANDOM % 1000}'} seconds 
<--->

# 查看HBase日志
TIP> 查看HBase日志
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'hbase'}]# 
CMD> tail -n 20 logs/hbase-root-master-${data.getBasicInfoValue('host', 'localhost')}.log
RES> ${r'${RANDOM % 2020 + 2020}'}-${r'${RANDOM % 12 + 1}'}-${r'${RANDOM % 30 + 1}'} ${r'${RANDOM % 24}'}:${r'${RANDOM % 60}'}:${r'${RANDOM % 60}'} INFO  [master:${data.getBasicInfoValue('host', 'localhost')}:16000] regionserver.HRegionServer: regionserver:${data.getBasicInfoValue('host', 'localhost')}:16020 reported a successful scan of 10 regions
RES> ${r'${RANDOM % 2020 + 2020}'}-${r'${RANDOM % 12 + 1}'}-${r'${RANDOM % 30 + 1}'} ${r'${RANDOM % 24}'}:${r'${RANDOM % 60}'}:${r'${RANDOM % 60}'} INFO  [master:${data.getBasicInfoValue('host', 'localhost')}:16000] snapshot.SnapshotManager: Snapshot manager loaded
RES> ${r'${RANDOM % 2020 + 2020}'}-${r'${RANDOM % 12 + 1}'}-${r'${RANDOM % 30 + 1}'} ${r'${RANDOM % 24}'}:${r'${RANDOM % 60}'}:${r'${RANDOM % 60}'} INFO  [master:${data.getBasicInfoValue('host', 'localhost')}:16000] master.ServerManager: Successfully processed report from regionserver:${data.getBasicInfoValue('host', 'localhost')}:16020
<--->

# 退出服务器
TIP> 退出服务器
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'hbase'}]# 
CMD> exit
RES> Connection to ${data.getBasicInfoValue('host', 'localhost')} closed. 