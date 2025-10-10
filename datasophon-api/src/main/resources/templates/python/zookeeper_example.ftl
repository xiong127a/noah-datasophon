DEPENDENCIES_START
# pip依赖：
kazoo==2.9.0
<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
pykerb==0.2.1
gssapi==1.8.2
kerberos==1.3.1
</#if>
DEPENDENCIES_END

#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
ZooKeeper Python连接示例
使用kazoo客户端库连接ZooKeeper服务器
"""

import time
import logging
from kazoo.client import KazooClient
from kazoo.client import KazooState
from kazoo.exceptions import KazooException
from kazoo.security import make_digest_acl

def main():
    # 连接参数
    connect_string = "${data.getBasicInfoValue('connectString', 'localhost:2181')}"
    deploy_mode = "${data.getBasicInfoValue('deployMode', '单节点模式')}"
    
    # 安全认证配置
    enable_kerberos = "${data.getSecurityInfoValue('kerberos', '否')}" == "是"
    
    print("ZooKeeper连接字符串:", connect_string)
    print("部署模式:", deploy_mode)
    
    try:
        # 创建连接示例
        print("\n----- 创建ZooKeeper连接 -----")
        
        # 设置基本日志
        logging.basicConfig(level=logging.INFO)
        
        # 创建连接
        zk = create_connection(connect_string, enable_kerberos)
        
        if zk and zk.connected:
            # 执行基本操作
            basic_operations_example(zk)
            
            # 执行监听器操作
            watcher_example(zk)
            
            # 执行节点操作
            node_operations_example(zk)
            
            # 关闭连接
            zk.stop()
            zk.close()
            print("ZooKeeper连接已关闭")
        
    except Exception as e:
        print("ZooKeeper操作失败:", str(e))
        import traceback
        traceback.print_exc()

def create_connection(connect_string, enable_kerberos=False):
    """
    创建ZooKeeper连接
    """
    print("连接到ZooKeeper服务器...")
    
    # 创建连接参数
    conn_kwargs = {
        "timeout": 10.0,
        "randomize_hosts": True,
        "retry_delay": 1.0,
        "retry_backoff": 2.0,
        "max_retries": 3
    }
    
    # 处理Kerberos认证
    if enable_kerberos:
        principal = "${data.getSecurityInfoValue('principal', 'zookeeper/_HOST@EXAMPLE.COM')}"
        keytab = "${data.getSecurityInfoValue('keytab', '/etc/security/keytabs/zookeeper.keytab')}"
        print(f"启用Kerberos认证 - 主体: {principal}")
        
        # 在实际环境中，您需要适当配置Kerberos认证
        # 这里仅作示例，实际实现可能需要调整
        conn_kwargs["sasl_options"] = {
            "mechanism": "GSSAPI",
            "service": "zookeeper",
            "principal": principal
        }
    
    # 创建客户端
    zk = KazooClient(hosts=connect_string, **conn_kwargs)
    
    # 定义连接状态监听函数
    def connection_listener(state):
        if state == KazooState.LOST:
            print("连接丢失")
        elif state == KazooState.SUSPENDED:
            print("连接挂起")
        else:
            print("连接已建立")
    
    # 添加状态监听器
    zk.add_listener(connection_listener)
    
    # 启动连接
    print("正在启动连接...")
    zk.start(timeout=10)
    
    if zk.connected:
        print("ZooKeeper连接成功")
        return zk
    else:
        print("ZooKeeper连接失败")
        return None

def basic_operations_example(zk):
    """
    基本操作示例
    """
    print("\n===== 基本操作示例 =====")
    
    # 检查ZooKeeper状态
    print("ZooKeeper状态:", "已连接" if zk.connected else "未连接")
    
    # 获取根节点下的所有子节点
    root_nodes = zk.get_children("/")
    print("根节点下的子节点:", root_nodes)
    
    # 创建测试目录
    test_path = "/python_example"
    if not zk.exists(test_path):
        zk.create(test_path, b"测试数据", makepath=True)
        print(f"创建节点: {test_path}")
    else:
        print(f"节点已存在: {test_path}")
    
    # 读取节点数据
    try:
        data, stat = zk.get(test_path)
        print(f"节点{test_path}数据: {data.decode('utf-8')}")
        print("节点状态:")
        print(f"  创建时间: {stat.created}")
        print(f"  修改时间: {stat.last_modified}")
        print(f"  数据版本: {stat.version}")
        print(f"  子节点版本: {stat.children_version}")
    except Exception as e:
        print(f"读取节点数据失败: {str(e)}")

def watcher_example(zk):
    """
    监听器示例
    """
    print("\n===== 监听器示例 =====")
    
    watch_path = "/python_example"
    
    # 确保节点存在
    if not zk.exists(watch_path):
        zk.create(watch_path, b"原始数据", makepath=True)
    
    # 定义监听回调函数
    def data_watch_func(data, stat, event):
        if event:
            print(f"检测到数据变化，事件: {event}")
        print(f"节点数据: {data.decode('utf-8') if data else None}")
        print(f"节点状态: {stat}")
    
    # 设置数据监听器
    print("设置监听器监控数据变化")
    zk.get(watch_path, watch=data_watch_func)
    
    # 更新数据以触发监听器
    print("更新数据以触发监听器")
    zk.set(watch_path, b"更新后的数据")
    
    # 给监听器一些时间来处理事件
    time.sleep(1)
    
    # 子节点监听器示例
    def child_watch_func(children):
        print(f"子节点列表变化: {children}")
    
    # 设置子节点监听器
    print("\n设置子节点监听器")
    zk.get_children(watch_path, watch=child_watch_func)
    
    # 创建子节点以触发监听器
    child_path = f"{watch_path}/child1"
    if not zk.exists(child_path):
        zk.create(child_path, b"子节点数据")
        print(f"创建子节点: {child_path}")
    
    # 给监听器一些时间来处理事件
    time.sleep(1)

def node_operations_example(zk):
    """
    节点操作示例
    """
    print("\n===== 节点操作示例 =====")
    
    # 创建临时节点
    temp_path = "/python_example/temp"
    if zk.exists(temp_path):
        zk.delete(temp_path)
    
    zk.create(temp_path, b"临时节点数据", ephemeral=True)
    print(f"创建临时节点: {temp_path}")
    
    # 创建顺序节点
    seq_path = "/python_example/seq"
    created_path = zk.create(seq_path, b"顺序节点数据", sequence=True)
    print(f"创建顺序节点: {created_path}")
    
    # 设置ACL
    acl_path = "/python_example/acl"
    if not zk.exists(acl_path):
        # 创建带ACL的节点
        acl = make_digest_acl("user", "password", read=True, write=True, create=True, delete=True, admin=True)
        zk.create(acl_path, b"ACL节点数据", acls=[acl])
        print(f"创建ACL节点: {acl_path}")
    
    # 列出子节点
    try:
        children = zk.get_children("/python_example")
        print("/python_example 的子节点:", children)
    except Exception as e:
        print(f"列出子节点失败: {str(e)}")
    
    # 事务操作示例
    print("\n----- 事务操作示例 -----")
    transaction = zk.transaction()
    transaction.create("/python_example/tx1", b"事务节点1")
    transaction.create("/python_example/tx2", b"事务节点2")
    transaction.set_data("/python_example", b"事务更新的数据")
    results = transaction.commit()
    print("事务执行结果:", results)
    
    # 清理示例节点
    print("\n----- 清理示例节点 -----")
    try:
        # 删除所有子节点
        children = zk.get_children("/python_example")
        for child in children:
            child_path = f"/python_example/{child}"
            zk.delete(child_path)
            print(f"删除节点: {child_path}")
        
        # 删除父节点
        zk.delete("/python_example")
        print("删除节点: /python_example")
    except Exception as e:
        print(f"清理节点失败: {str(e)}")

if __name__ == "__main__":
    main() 