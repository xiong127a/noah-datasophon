DEPENDENCIES_START
# pip依赖：
redis==4.6.0
aioredis==2.0.1
DEPENDENCIES_END

#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Redis Python连接示例
"""

import redis
from redis.sentinel import Sentinel
from redis.cluster import RedisCluster
from redis.commands.json.path import Path
import aioredis
import asyncio
import sys
import json
import time
from datetime import timedelta
from contextlib import asynccontextmanager

def main():
    # 连接参数
    host = "${data.getBasicInfoValue('host', 'localhost')}"
    port = int("${data.getBasicInfoValue('port', '6379')}")
    deploy_mode = "${data.getBasicInfoValue('deployMode', '主从模式')}"
    
    # 安全认证配置
    auth_enabled = "${data.getSecurityInfoValue('authEnabled', '否')}" == "是"
    password = "${data.getSecurityInfoValue('password', '')}" if auth_enabled else None
    
    # 构建Redis URI
    redis_uri = f"redis://{'':'' if not password else f':{password}@'}{host}:{port}/0"
    print(f"Redis连接URI: {redis_uri}")
    
    try:
        # 根据部署模式选择连接方式
        if deploy_mode == "哨兵模式":
            print("\n----- 使用哨兵模式连接 -----")
            sentinel_mode_example(host, port, password)
        elif deploy_mode == "集群模式":
            print("\n----- 使用集群模式连接 -----")
            cluster_mode_example(host, port, password)
        else:
            print("\n----- 使用单节点/主从模式连接 -----")
            
            # 1. 使用传统连接方式
            print("\n=== 1. 传统连接方式 ===")
            single_node_example(host, port, password)
            
            # 2. 使用URI连接
            print("\n=== 2. 使用URI连接 ===")
            uri_connection_example(redis_uri)
            
            # 3. 使用异步连接
            print("\n=== 3. 使用异步连接 ===")
            asyncio.run(async_connection_example(redis_uri))
            
    except Exception as e:
        print("Redis连接失败:", str(e), file=sys.stderr)
        raise

def single_node_example(host, port, password=None):
    """单节点/主从模式连接示例"""
    print("连接到Redis单节点/主从模式...")
    
    # 使用连接池
    pool = redis.ConnectionPool(
        host=host,
        port=port,
        password=password,
        decode_responses=True,  # 自动将响应解码为字符串
        max_connections=10,     # 最大连接数
        socket_timeout=5,       # 套接字超时时间
        socket_connect_timeout=5, # 连接超时时间
        health_check_interval=30  # 健康检查间隔
    )
    
    # 创建客户端
    redis_client = redis.Redis(connection_pool=pool)
    
    # 测试连接
    ping_response = redis_client.ping()
    print(f"连接成功! 服务器响应: {ping_response}")
    
    # 执行基本操作示例
    basic_operations_example(redis_client)
    
    # 执行高级操作示例
    advanced_operations_example(redis_client)
    
    # 关闭连接
    redis_client.close()
    print("Redis连接已关闭")

def uri_connection_example(redis_uri):
    """使用URI连接示例"""
    print(f"使用Redis URI连接: {redis_uri}")
    
    # 使用URI创建Redis客户端
    redis_client = redis.from_url(
        redis_uri,
        decode_responses=True,
        max_connections=10
    )
    
    try:
        # 测试连接
        ping_response = redis_client.ping()
        print(f"URI连接成功! 服务器响应: {ping_response}")
        
        # 简单操作
        redis_client.set("uri_test", "通过URI连接成功")
        value = redis_client.get("uri_test")
        print(f"获取 uri_test = {value}")
        
    finally:
        # 关闭连接
        redis_client.close()
        print("Redis URI连接已关闭")

async def async_connection_example(redis_uri):
    """异步连接示例"""
    print("使用异步客户端连接Redis...")
    
    # 创建异步Redis客户端
    redis = await aioredis.from_url(
        redis_uri,
        decode_responses=True,  # 自动将响应解码为字符串
        max_connections=10      # 最大连接数
    )
    
    try:
        # 测试连接
        ping_response = await redis.ping()
        print(f"异步连接成功! 服务器响应: {ping_response}")
        
        # 异步事务示例
        async with redis.pipeline(transaction=True) as pipe:
            pipe.set("async_key1", "异步值1")
            pipe.set("async_key2", "异步值2")
            pipe.incr("async_counter")
            results = await pipe.execute()
            print(f"异步事务执行结果: {results}")
        
        # 异步获取结果
        value1 = await redis.get("async_key1")
        value2 = await redis.get("async_key2")
        counter = await redis.get("async_counter")
        
        print(f"异步获取 async_key1 = {value1}")
        print(f"异步获取 async_key2 = {value2}")
        print(f"异步获取 counter = {counter}")
        
    finally:
        # 关闭连接
        await redis.close()
        print("Redis异步连接已关闭")

def sentinel_mode_example(host, port, password=None):
    """哨兵模式连接示例"""
    print("连接到Redis哨兵模式...")
    
    # 获取哨兵节点信息
    sentinel_nodes_str = "${data.getBasicInfoValue('sentinelNodes', '')}"
    sentinel_port = int("${data.getBasicInfoValue('sentinelPort', '26379')}")
    
    if not sentinel_nodes_str:
        print("未配置哨兵节点，使用单节点模式连接")
        single_node_example(host, port, password)
        return
    
    # 解析哨兵节点
    sentinel_nodes = []
    for node in sentinel_nodes_str.split(","):
        host_port = node.split(":")
        if len(host_port) == 2:
            sentinel_nodes.append((host_port[0], int(host_port[1])))
        else:
            sentinel_nodes.append((node, sentinel_port))
    
    # 创建哨兵连接
    sentinel = Sentinel(
        sentinel_nodes,
        socket_timeout=1.0,
        password=password,  # 哨兵认证密码，如果设置
        sentinel_kwargs={'password': password}  # 哨兵节点密码，如果设置
    )
    
    # 获取主节点连接
    master_name = "mymaster"  # 默认主节点名称，实际应从配置中获取
    master = sentinel.master_for(
        master_name,
        socket_timeout=0.5,
        password=password,
        db=0,
        decode_responses=True
    )
    
    # 测试连接
    ping_response = master.ping()
    print(f"成功连接到主节点! 服务器响应: {ping_response}")
    
    # 获取当前主节点信息
    master_info = master.info("replication")
    print("当前主节点信息:")
    for key, value in master_info.items():
        print(f"  {key}: {value}")
    
    # 执行基本操作示例
    basic_operations_example(master)
    
    # 关闭连接
    master.close()
    print("Redis哨兵连接已关闭")

def cluster_mode_example(host, port, password=None):
    """集群模式连接示例"""
    print("连接到Redis集群模式...")
    
    # 添加主节点
    startup_nodes = [{"host": host, "port": port}]
    
    # 添加从节点（如果有）
    slave_nodes_str = "${data.getBasicInfoValue('slaveNodes', '')}"
    if slave_nodes_str:
        for node in slave_nodes_str.split(","):
            host_port = node.split(":")
            if len(host_port) == 2:
                startup_nodes.append({"host": host_port[0], "port": int(host_port[1])})
    
    # 创建集群连接
    redis_cluster = RedisCluster(
        startup_nodes=startup_nodes,
        password=password,
        decode_responses=True,
        skip_full_coverage_check=True,  # 跳过完整覆盖检查
        socket_timeout=5,            # 套接字超时时间
        socket_connect_timeout=5     # 连接超时时间
    )
    
    # 测试连接
    ping_response = redis_cluster.ping()
    print(f"集群连接成功! 服务器响应: {ping_response}")
    
    # 执行集群操作示例
    cluster_operations_example(redis_cluster)
    
    # 关闭连接
    redis_cluster.close()
    print("Redis集群连接已关闭")

def basic_operations_example(redis_client):
    """基本操作示例（适用于单节点和哨兵模式）"""
    print("\n===== 基本操作示例 =====")
    
    # 字符串操作
    redis_client.set("mykey", "Hello Redis")
    print("设置 mykey = 'Hello Redis'")
    
    value = redis_client.get("mykey")
    print(f"获取 mykey = {value}")
    
    # 设置过期时间
    redis_client.setex("temp_key", 60, "一分钟后过期")
    ttl = redis_client.ttl("temp_key")
    print(f"temp_key 过期时间: {ttl} 秒")
    
    # 哈希操作
    redis_client.hset("user:1", mapping={
        "name": "张三",
        "age": "30",
        "city": "北京"
    })
    print("设置 user:1 的哈希数据")
    
    user_info = redis_client.hgetall("user:1")
    print(f"获取 user:1 信息: {user_info}")
    
    # 列表操作
    redis_client.delete("mylist")  # 确保列表为空
    redis_client.lpush("mylist", "世界", "你好")
    redis_client.rpush("mylist", "Redis")
    print("列表操作: 左侧添加'你好'和'世界'，右侧添加'Redis'")
    
    list_content = redis_client.lrange("mylist", 0, -1)
    print(f"列表内容: {list_content}")
    
    # 集合操作
    redis_client.sadd("myset", "A", "B", "C")
    print("集合操作: 添加元素 A, B, C")
    
    set_members = redis_client.smembers("myset")
    print(f"集合内容: {set_members}")
    
    # 有序集合操作
    redis_client.zadd("ranking", {"张三": 100, "李四": 85, "王五": 95})
    print("有序集合操作: 添加成绩排名")
    
    top_scores = redis_client.zrevrange("ranking", 0, 2, withscores=True)
    print(f"前三名: {top_scores}")
    
    # 键操作
    print("\n----- 键操作示例 -----")
    keys = redis_client.keys("*")
    print(f"所有键: {keys}")
    
    exists = redis_client.exists("mykey")
    print(f"mykey 是否存在: {exists}")
    
    key_type = redis_client.type("user:1")
    print(f"user:1 类型: {key_type}")

def advanced_operations_example(redis_client):
    """高级操作示例"""
    print("\n===== 高级操作示例 =====")
    
    # 管道操作（批量执行命令）
    print("\n----- 管道操作 -----")
    pipe = redis_client.pipeline()
    pipe.set("pipe_key1", "管道值1")
    pipe.set("pipe_key2", "管道值2")
    pipe.incr("pipe_counter")
    pipe.get("pipe_key1")
    pipe.get("pipe_key2")
    pipe.get("pipe_counter")
    results = pipe.execute()
    print(f"管道执行结果: {results}")
    
    # 事务操作
    print("\n----- 事务操作 -----")
    transaction = redis_client.pipeline(transaction=True)
    transaction.set("tx_key1", "事务值1")
    transaction.set("tx_key2", "事务值2")
    transaction.incr("tx_counter")
    results = transaction.execute()
    print(f"事务执行结果: {results}")
    print(f"事务后key1值: {redis_client.get('tx_key1')}")
    print(f"事务后key2值: {redis_client.get('tx_key2')}")
    print(f"事务后计数器值: {redis_client.get('tx_counter')}")
    
    # 发布订阅示例 (模拟)
    print("\n----- 发布订阅示例 (模拟) -----")
    print("实际应用中，发布和订阅应在不同的线程或进程中运行")
    print("模拟发布消息: redis_client.publish('channel1', 'Hello subscribers!')")
    print("模拟订阅: pubsub = redis_client.pubsub()")
    print("模拟订阅: pubsub.subscribe('channel1')")
    print("模拟接收: for message in pubsub.listen(): print(message)")
    
    # Lua脚本执行
    print("\n----- Lua脚本执行 -----")
    lua_script = """
    local key = KEYS[1]
    local value = ARGV[1]
    redis.call('SET', key, value)
    return redis.call('GET', key)
    """
    script_result = redis_client.eval(lua_script, 1, "lua_key", "Lua脚本设置的值")
    print(f"Lua脚本执行结果: {script_result}")
    
    # 位图操作
    print("\n----- 位图操作 -----")
    redis_client.setbit("bitmap", 0, 1)  # 设置第0位为1
    redis_client.setbit("bitmap", 2, 1)  # 设置第2位为1
    redis_client.setbit("bitmap", 4, 1)  # 设置第4位为1
    print(f"bitmap第0位: {redis_client.getbit('bitmap', 0)}")
    print(f"bitmap第1位: {redis_client.getbit('bitmap', 1)}")
    print(f"bitmap中设置为1的位数: {redis_client.bitcount('bitmap')}")
    
    # 地理位置操作
    print("\n----- 地理位置操作 -----")
    redis_client.geoadd("geo:cities", 
                      longitude=116.397128, latitude=39.916527, member="北京",
                      longitude=121.473701, latitude=31.230416, member="上海",
                      longitude=113.264385, latitude=23.129112, member="广州")
    distance = redis_client.geodist("geo:cities", "北京", "上海", unit="km")
    print(f"北京到上海的距离: {distance} 公里")
    
    # 扫描迭代
    print("\n----- 扫描迭代 -----")
    print("使用scan代替keys，避免阻塞Redis服务器:")
    count = 0
    cursor = 0
    print("前5个键:")
    while True:
        cursor, keys = redis_client.scan(cursor=cursor, count=2)
        for key in keys:
            print(f"  - {key}")
            count += 1
            if count >= 5:
                break
        if cursor == 0 or count >= 5:
            break

def cluster_operations_example(redis_cluster):
    """集群操作示例"""
    print("\n===== 集群操作示例 =====")
    
    # 字符串操作
    redis_cluster.set("cluster:key1", "集群值1")
    print("设置 cluster:key1 = '集群值1'")
    
    value = redis_cluster.get("cluster:key1")
    print(f"获取 cluster:key1 = {value}")
    
    # 哈希操作
    redis_cluster.hset("cluster:hash", mapping={
        "field1": "值1",
        "field2": "值2"
    })
    print("设置 cluster:hash 哈希数据")
    
    hash_data = redis_cluster.hgetall("cluster:hash")
    print(f"获取 cluster:hash 数据: {hash_data}")
    
    # 列表操作
    redis_cluster.lpush("cluster:list", "集群", "列表")
    redis_cluster.rpush("cluster:list", "示例")
    print("列表操作: 添加数据到集群列表")
    
    list_data = redis_cluster.lrange("cluster:list", 0, -1)
    print(f"列表内容: {list_data}")
    
    # 集群信息
    print("\n----- 集群信息 -----")
    try:
        # 注意：这些操作可能不适用于所有Redis集群版本
        print("集群模式下某些操作受限制，例如:");
        print("- 事务操作只能在相同槽中的键上执行")
        print("- 不支持跨槽的多键操作")
        print("- keys命令不可用，应使用scan命令代替")
    except Exception as e:
        print(f"获取集群信息出错: {e}")

if __name__ == '__main__':
    main() 