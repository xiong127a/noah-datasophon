# Redis Sentinel连接示例
# 依赖: redis-py
# pip install redis

DEPENDENCIES_START
# 依赖包：
redis==4.5.1
DEPENDENCIES_END

import redis
from redis.sentinel import Sentinel
import time

def connect_to_redis_sentinel():
    """
    连接到Redis Sentinel
    """
    try:
        # 连接参数
        sentinel_nodes = [
            ('${sentinelHost1!'localhost'}', ${sentinelPort1!26379}),
            ('${sentinelHost2!'localhost'}', ${sentinelPort2!26379}),
            ('${sentinelHost3!'localhost'}', ${sentinelPort3!26379})
        ]
        
        # 创建Sentinel连接
        sentinel = Sentinel(
            sentinel_nodes,
            socket_timeout=0.5,
            password='${password!''}'
        )
        
        # 获取主节点连接
        master = sentinel.master_for(
            '${masterName!'mymaster'}',
            socket_timeout=0.5,
            password='${password!''}'
        )
        
        # 获取从节点连接
        slave = sentinel.slave_for(
            '${masterName!'mymaster'}',
            socket_timeout=0.5,
            password='${password!''}'
        )
        
        return master, slave, sentinel
        
    except Exception as e:
        print(f"连接失败: {str(e)}")
        return None, None, None

def basic_operations(master, slave):
    """
    基本操作示例
    """
    try:
        # 写入数据到主节点
        master.set('test_key', 'test_value')
        print("写入数据成功")
        
        # 从从节点读取数据
        value = slave.get('test_key')
        print(f"读取数据: {value}")
        
        # 删除数据
        master.delete('test_key')
        print("删除数据成功")
        
    except Exception as e:
        print(f"操作失败: {str(e)}")

def show_master_info(sentinel):
    """
    显示主节点信息
    """
    try:
        # 获取主节点信息
        master_info = sentinel.discover_master('${masterName!'mymaster'}')
        print(f"主节点信息: {master_info}")
        
        # 获取从节点信息
        slaves_info = sentinel.discover_slaves('${masterName!'mymaster'}')
        print(f"从节点信息: {slaves_info}")
        
    except Exception as e:
        print(f"获取信息失败: {str(e)}")

def sentinel_commands(sentinel):
    """
    Sentinel命令示例
    """
    try:
        # 获取所有主节点
        masters = sentinel.sentinel_masters()
        print("所有主节点:")
        for master in masters:
            print(f"- {master}")
            
        # 获取指定主节点的从节点
        slaves = sentinel.sentinel_slaves('${masterName!'mymaster'}')
        print(f"主节点 ${masterName!'mymaster'} 的从节点:")
        for slave in slaves:
            print(f"- {slave}")
            
        # 获取主节点地址
        master_addr = sentinel.sentinel_get_master_addr_by_name('${masterName!'mymaster'}')
        print(f"主节点地址: {master_addr}")
        
    except Exception as e:
        print(f"执行Sentinel命令失败: {str(e)}")

def main():
    """
    主函数
    """
    # 连接到Redis Sentinel
    master, slave, sentinel = connect_to_redis_sentinel()
    if not master or not slave or not sentinel:
        return
        
    try:
        # 基本操作
        basic_operations(master, slave)
        
        # 显示主节点信息
        show_master_info(sentinel)
        
        # Sentinel命令
        sentinel_commands(sentinel)
        
    except Exception as e:
        print(f"操作失败: {str(e)}")
    finally:
        # 关闭连接
        if master:
            master.close()
        if slave:
            slave.close()

if __name__ == '__main__':
    main() 