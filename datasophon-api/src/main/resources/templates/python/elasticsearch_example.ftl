DEPENDENCIES_START
# pip依赖：
elasticsearch==7.16.2
DEPENDENCIES_END

#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
ElasticSearch Python连接示例
"""

from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk, scan
from elasticsearch.exceptions import NotFoundError, ConnectionError
import json
import time
import logging
import sys
from datetime import datetime

# 配置日志
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger('elasticsearch_example')

def main():
    """主函数"""
    # 连接参数
    host = "${data.getBasicInfoValue('host', 'localhost')}"
    http_port = int("${data.getBasicInfoValue('httpPort', '9200')}")
    cluster_name = "${data.getBasicInfoValue('clusterName', 'elasticsearch')}"
    
    # 如果有多个节点，使用nodeList
    node_list = "${data.getBasicInfoValue('nodeList', '')}"
    
    # 安全认证配置
    auth_mode = "${data.getSecurityInfoValue('authMode', '无认证')}"
    username = "${data.getSecurityInfoValue('username', '')}"
    password = "${data.getSecurityInfoValue('password', '')}"
    
    print("===== ElasticSearch连接示例 =====")
    print(f"主机: {host}")
    print(f"HTTP端口: {http_port}")
    print(f"集群名称: {cluster_name}")
    
    try:
        # 创建客户端连接
        es = create_client(host, http_port, node_list, auth_mode, username, password)
        print("ElasticSearch连接创建成功")
        
        # 检查集群健康状态
        check_cluster_health(es)
        
        # 索引操作示例
        index_operations_example(es)
        
        # 文档操作示例
        document_operations_example(es)
        
        # 搜索操作示例
        search_operations_example(es)
        
        # 批量操作示例
        bulk_operations_example(es)
        
    except ConnectionError as e:
        logger.error(f"连接ElasticSearch失败: {str(e)}")
        sys.exit(1)
    except Exception as e:
        logger.error(f"操作ElasticSearch时出错: {str(e)}")
        sys.exit(1)

def create_client(host, port, node_list, auth_mode, username, password):
    """创建ElasticSearch客户端"""
    
    # 构建主机列表
    hosts = []
    
    if node_list and node_list.strip():
        # 多节点模式
        for node in node_list.split(","):
            if ":" in node:
                parts = node.split(":")
                hosts.append({"host": parts[0], "port": int(parts[1])})
            else:
                hosts.append({"host": node, "port": port})
    else:
        # 单节点模式
        hosts.append({"host": host, "port": port})
    
    # 准备连接参数
    es_params = {
        "hosts": hosts,
        "timeout": 60,
        "retry_on_timeout": True,
        "max_retries": 3
    }
    
    # 如果启用了用户名密码认证
    if auth_mode == "用户名密码" and username:
        es_params["http_auth"] = (username, password)
    
    # 创建客户端
    es = Elasticsearch(**es_params)
    
    # 验证连接
    if not es.ping():
        raise ConnectionError("无法ping通ElasticSearch服务")
    
    return es

def check_cluster_health(es):
    """检查集群健康状态"""
    print("\n===== 集群健康状态 =====")
    
    # 获取集群信息
    info = es.info()
    print(f"集群名称: {info['cluster_name']}")
    print(f"集群版本: {info['version']['number']}")
    print(f"Lucene版本: {info['version']['lucene_version']}")
    
    # 获取集群健康状态
    health = es.cluster.health()
    print(f"集群状态: {health['status']}")
    print(f"节点数量: {health['number_of_nodes']}")
    print(f"数据节点数量: {health['number_of_data_nodes']}")
    print(f"分片数量: {health['active_shards']}")
    print(f"主分片数量: {health['active_primary_shards']}")

def index_operations_example(es):
    """索引操作示例"""
    print("\n===== 索引操作示例 =====")
    
    index_name = "user_index"
    
    # 检查索引是否存在
    if es.indices.exists(index=index_name):
        print(f"索引 {index_name} 已存在，删除它")
        es.indices.delete(index=index_name)
    
    # 创建索引
    index_settings = {
        "settings": {
            "number_of_shards": 3,
            "number_of_replicas": 2
        },
        "mappings": {
            "properties": {
                "name": {
                    "type": "text",
                    "analyzer": "standard"
                },
                "age": {
                    "type": "integer"
                },
                "email": {
                    "type": "keyword"
                },
                "birthday": {
                    "type": "date",
                    "format": "yyyy-MM-dd"
                }
            }
        }
    }
    
    response = es.indices.create(index=index_name, body=index_settings)
    print(f"创建索引响应: {response['acknowledged']}")
    
    # 获取索引信息
    index_info = es.indices.get(index=index_name)
    print(f"索引信息:\n{json.dumps(index_info[index_name]['mappings'], indent=2)}")
    
    # 查看索引列表
    indices = es.indices.get_alias("*")
    print(f"所有索引列表: {', '.join(indices.keys())}")

def document_operations_example(es):
    """文档操作示例"""
    print("\n===== 文档操作示例 =====")
    
    index_name = "user_index"
    document_id = "1"
    
    # 创建文档
    document = {
        "name": "张三",
        "age": 30,
        "email": "zhangsan@example.com",
        "birthday": "1990-01-01"
    }
    
    # 索引文档
    response = es.index(index=index_name, id=document_id, body=document)
    print(f"索引文档响应: {response['result']}")
    
    # 强制刷新索引，确保文档可搜索
    es.indices.refresh(index=index_name)
    
    # 获取文档
    response = es.get(index=index_name, id=document_id)
    print(f"获取文档: {response['_source']}")
    
    # 更新文档
    update_data = {
        "doc": {
            "age": 31
        }
    }
    
    response = es.update(index=index_name, id=document_id, body=update_data)
    print(f"更新文档响应: {response['result']}")
    
    # 再次获取文档
    response = es.get(index=index_name, id=document_id)
    print(f"更新后的文档: {response['_source']}")
    
    # 检查文档是否存在
    exists = es.exists(index=index_name, id=document_id)
    print(f"文档是否存在: {exists}")
    
    # 删除文档
    response = es.delete(index=index_name, id=document_id)
    print(f"删除文档响应: {response['result']}")

def search_operations_example(es):
    """搜索操作示例"""
    print("\n===== 搜索操作示例 =====")
    
    index_name = "user_index"
    
    # 创建一些测试数据
    users = [
        {
            "id": "1",
            "name": "张三",
            "age": 30,
            "email": "zhangsan@example.com",
            "birthday": "1990-01-01"
        },
        {
            "id": "2",
            "name": "李四",
            "age": 25,
            "email": "lisi@example.com",
            "birthday": "1995-05-05"
        },
        {
            "id": "3",
            "name": "王五",
            "age": 35,
            "email": "wangwu@example.com",
            "birthday": "1985-10-10"
        }
    ]
    
    # 批量索引文档
    actions = []
    for user in users:
        actions.append({
            "_index": index_name,
            "_id": user["id"],
            "_source": {k: v for k, v in user.items() if k != "id"}
        })
    
    bulk(es, actions)
    
    # 强制刷新索引，确保文档可搜索
    es.indices.refresh(index=index_name)
    
    # 执行搜索 - 精确查询
    print("\n----- 精确查询 -----")
    query = {
        "query": {
            "match": {
                "name": "李四"
            }
        }
    }
    
    response = es.search(index=index_name, body=query)
    print("精确查询结果:")
    for hit in response["hits"]["hits"]:
        print(f"{hit['_source']}")
    
    # 执行搜索 - 范围查询
    print("\n----- 范围查询 -----")
    query = {
        "query": {
            "range": {
                "age": {
                    "gte": 25,
                    "lte": 33
                }
            }
        }
    }
    
    response = es.search(index=index_name, body=query)
    print("范围查询结果:")
    for hit in response["hits"]["hits"]:
        print(f"{hit['_source']}")
    
    # 执行搜索 - 排序
    print("\n----- 排序查询 -----")
    query = {
        "query": {
            "match_all": {}
        },
        "sort": [
            {"age": {"order": "desc"}}
        ]
    }
    
    response = es.search(index=index_name, body=query)
    print("排序查询结果 (按年龄降序):")
    for hit in response["hits"]["hits"]:
        print(f"{hit['_source']}")
    
    # 执行搜索 - 模糊查询
    print("\n----- 模糊查询 -----")
    query = {
        "query": {
            "fuzzy": {
                "name": {
                    "value": "张山",
                    "fuzziness": "AUTO"
                }
            }
        }
    }
    
    response = es.search(index=index_name, body=query)
    print("模糊查询结果 (搜索'张山'，实际找到'张三'):")
    for hit in response["hits"]["hits"]:
        print(f"{hit['_source']}")

def bulk_operations_example(es):
    """批量操作示例"""
    print("\n===== 批量操作示例 =====")
    
    index_name = "product_index"
    
    # 删除之前的索引(如果存在)
    if es.indices.exists(index=index_name):
        es.indices.delete(index=index_name)
    
    # 创建新索引
    es.indices.create(index=index_name)
    
    # 准备批量操作
    actions = []
    
    # 添加多条数据
    for i in range(1, 11):
        product = {
            "name": f"产品{i}",
            "price": 100 + i * 10,
            "category": "电子" if i % 3 == 0 else "家具" if i % 3 == 1 else "服装",
            "createTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }
        
        actions.append({
            "_index": index_name,
            "_id": str(i),
            "_source": product
        })
    
    # 执行批量操作
    start_time = time.time()
    success, failed = bulk(es, actions)
    end_time = time.time()
    
    print(f"批量操作响应: 成功 {success} 条, 失败 {len(failed)} 条")
    print(f"批量操作耗时: {(end_time - start_time) * 1000:.2f}ms")
    
    if failed:
        print(f"批量操作中有失败: {failed}")
    
    # 强制刷新索引，确保文档可搜索
    es.indices.refresh(index=index_name)
    
    # 查询所有产品
    query = {
        "query": {
            "match_all": {}
        }
    }
    
    response = es.search(index=index_name, body=query, size=10)
    print("\n所有产品列表:")
    for hit in response["hits"]["hits"]:
        print(f"{hit['_source']}")
    
    # 使用scan方法获取大量数据
    print("\n----- 使用scan扫描大量数据 -----")
    print("scan方法适用于从索引中检索大量文档")
    count = 0
    for doc in scan(es, query={"query": {"match_all": {}}}, index=index_name):
        count += 1
    print(f"索引中共有 {count} 条数据")

if __name__ == "__main__":
    main() 