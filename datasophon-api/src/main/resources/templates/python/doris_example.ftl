DEPENDENCIES_START
# pip依赖：
pymysql==1.1.0
pandas==2.0.3
requests==2.31.0
DEPENDENCIES_END

#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Doris Python连接示例
演示如何使用PyMySQL和HTTP方式连接Apache Doris并执行操作
"""

import pymysql
import pandas as pd
import requests
import json
import time
from datetime import datetime

def main():
    # 连接参数
    host = "${data.getBasicInfoValue('host', 'localhost')}"
    fe_port = ${data.getBasicInfoValue('fePort', '9030')}
    http_port = ${data.getBasicInfoValue('httpPort', '8030')}
    
    # 安全认证配置
    user = "${data.getSecurityInfoValue('username', 'root')}"
    password = "${data.getSecurityInfoValue('password', '')}"
    
    # 数据库名称
    database = "example_db"
    
    try:
        print("=== 使用PyMySQL(MySQL协议)连接 ===")
        
        # 创建数据库
        conn = connect_to_doris(host, fe_port, user, password)
        create_database(conn, database)
        conn.close()
        
        # 连接到创建的数据库
        conn = connect_to_doris(host, fe_port, user, password, database)
        print("成功连接到Doris服务器!")
        
        # 创建示例表
        create_example_table(conn)
        
        # 插入数据
        insert_data(conn)
        
        # 查询数据
        query_data(conn)
        
        # 分区表操作示例
        partition_table_example(conn)
        
        # 使用HTTP API查询
        print("\n=== 使用HTTP API连接 ===")
        http_query_example(host, http_port, database, user, password)
        
        # 使用Pandas操作Doris
        print("\n=== 使用Pandas操作Doris ===")
        pandas_example(conn)
        
        # 清理数据库
        cleanup_database(conn, database)
        
    except Exception as e:
        print(f"操作失败: {str(e)}")
        raise
    finally:
        # 关闭连接
        if 'conn' in locals() and conn:
            conn.close()
            print("数据库连接已关闭")

def connect_to_doris(host, port, user, password, database=None):
    """连接到Doris数据库"""
    conn = pymysql.connect(
        host=host,
        port=int(port),
        user=user,
        password=password,
        database=database,
        charset='utf8mb4'
    )
    return conn

def create_database(conn, database):
    """创建示例数据库"""
    cursor = conn.cursor()
    try:
        # 先删除可能存在的数据库
        cursor.execute(f"DROP DATABASE IF EXISTS {database}")
        # 创建新数据库
        cursor.execute(f"CREATE DATABASE {database}")
        print(f"成功创建数据库: {database}")
    finally:
        cursor.close()

def create_example_table(conn):
    """创建示例表"""
    cursor = conn.cursor()
    try:
        create_table_sql = """
        CREATE TABLE IF NOT EXISTS example_table (
            id INT,
            name VARCHAR(50),
            value DOUBLE,
            create_time DATETIME
        ) ENGINE=OLAP
        DUPLICATE KEY(id)
        COMMENT 'Doris示例表'
        DISTRIBUTED BY HASH(id) BUCKETS 3
        """
        cursor.execute(create_table_sql)
        print("成功创建表 example_table")
    finally:
        cursor.close()

def insert_data(conn):
    """插入数据"""
    cursor = conn.cursor()
    try:
        # 准备插入数据
        data = [
            (1, '测试1', 10.5, datetime.now()),
            (2, '测试2', 20.5, datetime.now()),
            (3, '测试3', 30.5, datetime.now())
        ]
        
        # 执行插入
        insert_sql = "INSERT INTO example_table (id, name, value, create_time) VALUES (%s, %s, %s, %s)"
        cursor.executemany(insert_sql, data)
        conn.commit()
        
        print(f"成功插入{len(data)}条数据")
    finally:
        cursor.close()

def query_data(conn):
    """查询数据"""
    cursor = conn.cursor()
    try:
        query_sql = "SELECT * FROM example_table ORDER BY id"
        cursor.execute(query_sql)
        results = cursor.fetchall()
        
        print("查询结果:")
        print("+---------+---------+-----------+------------------------+")
        print("| id      | name    | value     | create_time            |")
        print("+---------+---------+-----------+------------------------+")
        
        for row in results:
            id, name, value, create_time = row
            print(f"| {id:<7} | {name:<7} | {value:<9.2f} | {create_time} |")
        
        print("+---------+---------+-----------+------------------------+")
    finally:
        cursor.close()

def partition_table_example(conn):
    """分区表操作示例"""
    print("\n=== 分区表操作示例 ===")
    cursor = conn.cursor()
    
    try:
        # 创建分区表
        create_partition_table_sql = """
        CREATE TABLE IF NOT EXISTS partition_example (
            event_day DATE,
            event_hour SMALLINT,
            event_type VARCHAR(20),
            event_count INT
        ) ENGINE=OLAP
        DUPLICATE KEY(event_day, event_hour, event_type)
        PARTITION BY RANGE(event_day) (
            PARTITION p20230101 VALUES [('2023-01-01'), ('2023-01-02')),
            PARTITION p20230102 VALUES [('2023-01-02'), ('2023-01-03')),
            PARTITION p20230103 VALUES [('2023-01-03'), ('2023-01-04'))
        )
        DISTRIBUTED BY HASH(event_type) BUCKETS 3
        """
        cursor.execute(create_partition_table_sql)
        print("成功创建分区表 partition_example")
        
        # 插入数据到不同分区
        partition_days = ['2023-01-01', '2023-01-02', '2023-01-03']
        event_types = ['click', 'view', 'purchase']
        
        for day in partition_days:
            for hour in range(0, 24, 6):
                for event_type in event_types:
                    import random
                    count = 100 + random.randint(0, 900)
                    insert_sql = f"INSERT INTO partition_example VALUES ('{day}', {hour}, '{event_type}', {count})"
                    cursor.execute(insert_sql)
        
        conn.commit()
        print("成功插入分区数据")
        
        # 查询特定分区数据
        query_sql = """
        SELECT event_day, event_hour, event_type, event_count
        FROM partition_example
        WHERE event_day = '2023-01-02'
        ORDER BY event_hour, event_type
        """
        cursor.execute(query_sql)
        results = cursor.fetchall()
        
        print("\n分区查询结果 (2023-01-02):")
        print("+------------+------------+------------+-------------+")
        print("| event_day  | event_hour | event_type | event_count |")
        print("+------------+------------+------------+-------------+")
        
        for row in results:
            event_day, event_hour, event_type, event_count = row
            print(f"| {event_day} | {event_hour:<10} | {event_type:<10} | {event_count:<11} |")
        
        print("+------------+------------+------------+-------------+")
        
        # 添加新分区
        cursor.execute("ALTER TABLE partition_example ADD PARTITION p20230104 VALUES [('2023-01-04'), ('2023-01-05'))")
        print("\n成功添加新分区 p20230104")
        
        # 显示分区信息
        cursor.execute("SHOW PARTITIONS FROM partition_example")
        partitions = cursor.fetchall()
        
        print("\n分区信息:")
        print("+----------------+----------------+")
        print("| PartitionName  | PartitionRange |")
        print("+----------------+----------------+")
        
        for partition in partitions:
            partition_name = partition[0]  # 分区名称通常在第一列
            print(f"| {partition_name:<14} | {'[...]':<14} |")
        
        print("+----------------+----------------+")
        
        # 删除分区表
        cursor.execute("DROP TABLE IF EXISTS partition_example")
        print("\n成功删除分区表 partition_example")
        
    finally:
        cursor.close()

def http_query_example(host, http_port, database, user, password):
    """HTTP API查询示例"""
    try:
        # 构建HTTP请求URL
        url = f"http://{host}:{http_port}/api/{database}/query"
        
        # 构建查询SQL
        query = "SELECT * FROM example_table FORMAT JSON"
        
        # 构建请求头和认证
        headers = {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
        
        # 如果提供了用户名和密码，添加认证
        auth = None
        if user and password:
            auth = (user, password)
        
        # 发送请求
        response = requests.post(
            url,
            data={"query": query},
            headers=headers,
            auth=auth
        )
        
        # 处理响应
        if response.status_code == 200:
            result = response.json()
            print("HTTP查询成功，状态码:", response.status_code)
            
            if "data" in result:
                data = result["data"]
                
                print("HTTP查询结果:")
                print("+---------+---------+-----------+------------------------+")
                print("| id      | name    | value     | create_time            |")
                print("+---------+---------+-----------+------------------------+")
                
                for row in data:
                    id = row["id"]
                    name = row["name"]
                    value = row["value"]
                    create_time = row["create_time"]
                    
                    print(f"| {id:<7} | {name:<7} | {value:<9.2f} | {create_time} |")
                
                print("+---------+---------+-----------+------------------------+")
        else:
            print(f"HTTP查询失败，状态码: {response.status_code}")
            print(f"错误信息: {response.text}")
    
    except Exception as e:
        print(f"HTTP查询出错: {str(e)}")

def pandas_example(conn):
    """使用Pandas操作Doris示例"""
    try:
        # 使用Pandas读取数据
        df = pd.read_sql("SELECT * FROM example_table", conn)
        print("Pandas DataFrame读取结果:")
        print(df)
        
        # 创建新表用于Pandas写入
        cursor = conn.cursor()
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS pandas_example (
            id INT,
            name VARCHAR(50),
            value DOUBLE,
            timestamp DATETIME
        ) ENGINE=OLAP
        DUPLICATE KEY(id)
        DISTRIBUTED BY HASH(id) BUCKETS 3
        """)
        cursor.close()
        
        # 创建示例DataFrame
        data = {
            'id': [101, 102, 103],
            'name': ['pandas1', 'pandas2', 'pandas3'],
            'value': [15.75, 25.75, 35.75],
            'timestamp': [datetime.now(), datetime.now(), datetime.now()]
        }
        new_df = pd.DataFrame(data)
        print("\n将写入Doris的DataFrame:")
        print(new_df)
        
        # 将DataFrame写入Doris
        # 注意：Doris要求使用executemany方式或流式导入，这里简化为单行插入
        cursor = conn.cursor()
        for index, row in new_df.iterrows():
            cursor.execute(
                "INSERT INTO pandas_example (id, name, value, timestamp) VALUES (%s, %s, %s, %s)",
                (row['id'], row['name'], row['value'], row['timestamp'])
            )
        conn.commit()
        cursor.close()
        
        # 验证写入结果
        result_df = pd.read_sql("SELECT * FROM pandas_example", conn)
        print("\nPandas写入后读取结果:")
        print(result_df)
        
        # 删除示例表
        cursor = conn.cursor()
        cursor.execute("DROP TABLE IF EXISTS pandas_example")
        cursor.close()
        print("\n成功删除表 pandas_example")
    
    except Exception as e:
        print(f"Pandas操作出错: {str(e)}")

def cleanup_database(conn, database):
    """清理数据库"""
    cursor = conn.cursor()
    try:
        # 删除表
        cursor.execute("DROP TABLE IF EXISTS example_table")
        print("成功删除表 example_table")
        
        # 删除数据库
        cursor.execute(f"DROP DATABASE IF EXISTS {database}")
        print(f"成功删除数据库 {database}")
    finally:
        cursor.close()

if __name__ == '__main__':
    main() 