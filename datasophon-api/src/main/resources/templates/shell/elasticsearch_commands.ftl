#!/bin/bash

# ElasticSearch命令行操作示例
# 依赖: curl、jq (可选，但推荐安装用于格式化JSON输出)



# 检查ElasticSearch集群状态
TIP> 检查ElasticSearch集群状态
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -s <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('httpPort', '9200')}/_cluster/health?pretty
RES> {
RES>   "cluster_name" : "${data.getBasicInfoValue('clusterName', 'elasticsearch')}",
RES>   "status" : "green",
RES>   "timed_out" : false,
RES>   "number_of_nodes" : 3,
RES>   "number_of_data_nodes" : 3,
RES>   "active_primary_shards" : 6,
RES>   "active_shards" : 12,
RES>   "relocating_shards" : 0,
RES>   "initializing_shards" : 0,
RES>   "unassigned_shards" : 0,
RES>   "delayed_unassigned_shards" : 0,
RES>   "number_of_pending_tasks" : 0,
RES>   "number_of_in_flight_fetch" : 0,
RES>   "task_max_waiting_in_queue_millis" : 0,
RES>   "active_shards_percent_as_number" : 100.0
RES> }
<--->

# 获取集群信息
TIP> 获取集群信息
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -s <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('httpPort', '9200')}
RES> {
RES>   "name" : "node-1",
RES>   "cluster_name" : "${data.getBasicInfoValue('clusterName', 'elasticsearch')}",
RES>   "cluster_uuid" : "xxxxxxxxxxxxxxxxxxx",
RES>   "version" : {
RES>     "number" : "7.16.2",
RES>     "build_flavor" : "default",
RES>     "build_type" : "tar",
RES>     "build_hash" : "xxxxxxxxx",
RES>     "build_date" : "2021-12-18T19:42:46.604893Z",
RES>     "build_snapshot" : false,
RES>     "lucene_version" : "8.10.1",
RES>     "minimum_wire_compatibility_version" : "6.8.0",
RES>     "minimum_index_compatibility_version" : "6.0.0-beta1"
RES>   },
RES>   "tagline" : "You Know, for Search"
RES> }
<--->

# 获取节点信息
TIP> 获取节点信息
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -s <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('httpPort', '9200')}/_cat/nodes?v
RES> ip         heap.percent ram.percent cpu load_1m load_5m load_15m node.role master name
RES> 172.17.0.2          49          89   1    0.00    0.01     0.05 dilmrt    *      node-1
RES> 172.17.0.3          52          93   1    0.00    0.02     0.07 dilmrt    -      node-2
RES> 172.17.0.4          48          91   1    0.00    0.02     0.09 dilmrt    -      node-3
<--->

# 创建索引
TIP> 创建索引
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -X PUT <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('httpPort', '9200')}/test_index -H "Content-Type: application/json" -d '{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2
  },
  "mappings": {
    "properties": {
      "name": { "type": "text" },
      "age": { "type": "integer" },
      "email": { "type": "keyword" },
      "created": { "type": "date" }
    }
  }
}'
RES> {"acknowledged":true,"shards_acknowledged":true,"index":"test_index"}
<--->

# 查看索引列表
TIP> 查看索引列表
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -s <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('httpPort', '9200')}/_cat/indices?v
RES> health status index                           uuid                   pri rep docs.count docs.deleted store.size pri.store.size
RES> green  open   test_index                      abcdefgh               3   2          0            0       795b           795b
RES> green  open   .kibana_task_manager_7.16.2     abcdefgh               1   2         12            1     124.1kb         58.4kb
RES> green  open   .kibana_7.16.2                  abcdefgh               1   2         10            0      1.9mb        645.9kb
<--->

# 退出第一个节点
TIP> 退出第一个节点服务器
PRT> [root@${data.getBasicInfoValue('host', 'localhost')} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> exit
RES> Connection to ${data.getBasicInfoValue('host', 'localhost')} closed.
<--->

# 第二个SSH连接 - 连接到数据节点
TIP> SSH连接到数据节点服务器
PRT> [user@localhost ~]$ 
CMD> ssh root@${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'}
<--->

TIP> 登录成功到数据节点
PRT> [root@${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'} ~]# 
CMD> cd ${data.installPath!''}${data.serviceHome!'elasticsearch'}
<--->

# 检查节点状态
TIP> 查看数据节点状态
PRT> [root@${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -s <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://localhost:${data.getBasicInfoValue('httpPort', '9200')}
RES> {
RES>   "name" : "node-2",
RES>   "cluster_name" : "${data.getBasicInfoValue('clusterName', 'elasticsearch')}",
RES>   "cluster_uuid" : "xxxxxxxxxxxxxxxxxxx",
RES>   "version" : {
RES>     "number" : "7.16.2",
RES>     "build_flavor" : "default",
RES>     "build_type" : "tar",
RES>     "build_hash" : "xxxxxxxxx",
RES>     "build_date" : "2021-12-18T19:42:46.604893Z",
RES>     "build_snapshot" : false,
RES>     "lucene_version" : "8.10.1",
RES>     "minimum_wire_compatibility_version" : "6.8.0",
RES>     "minimum_index_compatibility_version" : "6.0.0-beta1"
RES>   },
RES>   "tagline" : "You Know, for Search"
RES> }
<--->

# 查看数据节点索引分片
TIP> 查看数据节点上的分片分配
PRT> [root@${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -s <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://localhost:${data.getBasicInfoValue('httpPort', '9200')}/_cat/shards?v | grep $(hostname)
RES> index                    shard prirep state      docs  store ip         node
RES> test_index               2     p      STARTED       0  283b  172.17.0.3 node-2
RES> test_index               1     r      STARTED       1 3.9kb  172.17.0.3 node-2
RES> test_index               0     r      STARTED       1 3.9kb  172.17.0.3 node-2
<--->

# 查看节点统计信息
TIP> 查看数据节点统计信息
PRT> [root@${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -s <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://localhost:${data.getBasicInfoValue('httpPort', '9200')}/_nodes/stats/jvm,fs,os?pretty
RES> {
RES>   "_nodes" : {
RES>     "total" : 1,
RES>     "successful" : 1,
RES>     "failed" : 0
RES>   },
RES>   "cluster_name" : "${data.getBasicInfoValue('clusterName', 'elasticsearch')}",
RES>   "nodes" : {
RES>     "uz9p5NiuT1CRXYznp1VvLQ" : {
RES>       "name" : "node-2",
RES>       "transport_address" : "172.17.0.3:9300",
RES>       "host" : "172.17.0.3",
RES>       "ip" : "172.17.0.3:9300",
RES>       "jvm" : {
RES>         "timestamp" : 1626180856981,
RES>         "uptime_in_millis" : 1089156981,
RES>         "mem" : {
RES>           "heap_used_in_bytes" : 548291280,
RES>           "heap_used_percent" : 52,
RES>           "heap_committed_in_bytes" : 1040187392,
RES>           "heap_max_in_bytes" : 1040187392
RES>         }
RES>       },
RES>       "fs" : {
RES>         "timestamp" : 1626180856981,
RES>         "total" : {
RES>           "total_in_bytes" : 62725623808,
RES>           "free_in_bytes" : 45283926016,
RES>           "available_in_bytes" : 42258731008
RES>         }
RES>       },
RES>       "os" : {
RES>         "timestamp" : 1626180856981,
RES>         "cpu" : {
RES>           "percent" : 1,
RES>           "load_average" : {
RES>             "1m" : 0.02,
RES>             "5m" : 0.07,
RES>             "15m" : 0.09
RES>           }
RES>         }
RES>       }
RES>     }
RES>   }
RES> }
<--->

# 索引文档
TIP> 在数据节点上索引一个文档
PRT> [root@${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -X POST <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://localhost:${data.getBasicInfoValue('httpPort', '9200')}/test_index/_doc/4 -H "Content-Type: application/json" -d '{
  "name": "赵六",
  "age": 28,
  "email": "zhaoliu@example.com",
  "created": "2023-04-05T14:25:40Z"
}'
RES> {"_index":"test_index","_type":"_doc","_id":"4","_version":1,"result":"created","_shards":{"total":3,"successful":3,"failed":0},"_seq_no":5,"_primary_term":1}
<--->

# 验证文档添加
TIP> 验证文档是否成功添加
PRT> [root@${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> curl -s <#if data.getSecurityInfoValue('username', '') != ''>-u ${data.getSecurityInfoValue('username', '')}:${data.getSecurityInfoValue('password', '')} </#if>http://localhost:${data.getBasicInfoValue('httpPort', '9200')}/test_index/_doc/4?pretty
RES> {
RES>   "_index" : "test_index",
RES>   "_type" : "_doc",
RES>   "_id" : "4",
RES>   "_version" : 1,
RES>   "_seq_no" : 5,
RES>   "_primary_term" : 1,
RES>   "found" : true,
RES>   "_source" : {
RES>     "name" : "赵六",
RES>     "age" : 28,
RES>     "email" : "zhaoliu@example.com",
RES>     "created" : "2023-04-05T14:25:40Z"
RES>   }
RES> }
<--->

# 查看日志
TIP> 查看ElasticSearch节点日志
PRT> [root@${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> tail -n 20 logs/elasticsearch.log | grep -v DEBUG
RES> [2023-06-15T10:21:45,123][INFO ][o.e.c.m.MetadataIndexTemplateService] [node-2] adding template [.kibana_task_manager] for index patterns [.kibana_task_manager_7.16.2]
RES> [2023-06-15T10:21:49,876][INFO ][o.e.c.r.a.AllocationService] [node-2] Cluster health status changed from [YELLOW] to [GREEN] (reason: [shards started [[.kibana_task_manager_7.16.2][0]] ...]).
RES> [2023-06-16T08:45:36,321][INFO ][o.e.a.b.TransportShardBulkAction] [node-2] [test_index][0] processing [index {[test_index][_doc][4], source[{"name":"赵六","age":28,"email":"zhaoliu@example.com","created":"2023-04-05T14:25:40Z"}]}]
<--->

# 退出第二个节点
TIP> 退出数据节点服务器
PRT> [root@${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'} ${data.installPath!''}${data.serviceHome!'elasticsearch'}]# 
CMD> exit
RES> Connection to ${data.getBasicInfoValue('nodeList', '172.17.0.3')?split(',')?first?split(':')?first!'172.17.0.3'} closed. 