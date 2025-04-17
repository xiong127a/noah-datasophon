DEPENDENCIES_START
# pip依赖：
clickhouse-driver==0.2.6
requests==2.31.0
DEPENDENCIES_END

#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
ClickHouse Python连接示例
"""

from clickhouse_driver import Client
import datetime
import requests
import json

def main():
    # 连接参数
    host = "${data.getBasicInfoValue('host', 'localhost')}"
    port = ${data.getBasicInfoValue('tcpPort', '9000')}
    http_port = ${data.getBasicInfoValue('httpPort', '8123')}
    database = "${data.getBasicInfoValue('database', 'default')}"
    
    # 安全认证配置
    user = "${data.getSecurityInfoValue('username', 'default')}"
    password = "${data.getSecurityInfoValue('password', '')}"
    
    # 创建ClickHouse客户端 - 原生协议
    client = Client(
        host=host,
        port=port,
        database=database,
        user=user,
        password=password
    )
    
    try:
        print("=== 使用原生协议连接 ===")
        print("成功连接到ClickHouse服务器!")
        
        # 创建示例表
        create_example_table(client)
        
        # 插入数据
        insert_data(client)
        
        # 查询数据
        query_data(client)
        
        # HTTP协议示例
        print("\n=== 使用HTTP协议连接 ===")
        http_query_example(host, http_port, database, user, password)
        
        # 删除表
        drop_table(client)
        
    except Exception as e:
        print(f"连接失败: {str(e)}")
        raise

def create_example_table(client):
    """创建示例表"""
    create_table_sql = """
    CREATE TABLE IF NOT EXISTS example_table (
        id UInt32,
        name String,
        value Float64,
        timestamp DateTime
    ) ENGINE = MergeTree()
    ORDER BY id
    """
    
    client.execute(create_table_sql)
    print("成功创建表 example_table")

def insert_data(client):
    """插入数据"""
    data = [
        (1, 'test1', 10.5, datetime.datetime.now()),
        (2, 'test2', 20.5, datetime.datetime.now())
    ]
    
    client.execute(
        'INSERT INTO example_table (id, name, value, timestamp) VALUES',
        data
    )
    print(f"成功插入{len(data)}条数据")

def query_data(client):
    """查询数据"""
    result = client.execute('SELECT * FROM example_table')
    
    print("查询结果:")
    print("+---------+---------+-----------+------------------------+")
    print("| id      | name    | value     | timestamp              |")
    print("+---------+---------+-----------+------------------------+")
    
    for row in result:
        id, name, value, timestamp = row
        print(f"| {id:<7} | {name:<7} | {value:<9.2f} | {timestamp} |")
    
    print("+---------+---------+-----------+------------------------+")

def http_query_example(host, http_port, database, user, password):
    """HTTP协议查询示例"""
    # 构建HTTP请求URL
    auth = (user, password) if user and password else None
    url = f"http://{host}:{http_port}"
    
    # 查询数据
    query = "SELECT * FROM example_table FORMAT JSON"
    params = {"database": database, "query": query}
    
    try:
        response = requests.get(url, params=params, auth=auth)
        response.raise_for_status()  # 如果请求失败，抛出异常
        
        # 解析JSON响应
        result = response.json()
        
        print("HTTP查询结果:")
        print(json.dumps(result, indent=2, ensure_ascii=False))
        
        # 输出格式化表格
        print("\n格式化HTTP查询结果:")
        print("+---------+---------+-----------+------------------------+")
        print("| id      | name    | value     | timestamp              |")
        print("+---------+---------+-----------+------------------------+")
        
        for row in result['data']:
            id = row['id']
            name = row['name']
            value = row['value']
            timestamp = row['timestamp']
            print(f"| {id:<7} | {name:<7} | {value:<9.2f} | {timestamp} |")
        
        print("+---------+---------+-----------+------------------------+")
        
    except Exception as e:
        print(f"HTTP查询失败: {str(e)}")

def drop_table(client):
    """删除表"""
    client.execute('DROP TABLE IF EXISTS example_table')
    print("成功删除表 example_table")

if __name__ == '__main__':
    main() 