DEPENDENCIES_START
# pip依赖：
requests==2.28.1
pyhdfs==0.3.1
yarn-api-client==1.0.3
DEPENDENCIES_END

#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
YARN（ResourceManager）Python连接示例
"""

import sys
import json
import time
import requests
from datetime import datetime
from urllib.parse import urljoin
try:
    from yarn_api_client import ApplicationMaster, HistoryServer, NodeManager, ResourceManager
    has_yarn_client = True
except ImportError:
    has_yarn_client = False

def main():
    # 连接参数
    rm_address = "${data.getBasicInfoValue('connectString', 'localhost:8032')}"
    web_address = "${data.getBasicInfoValue('webAddress', 'http://localhost:8088')}"
    deploy_mode = "${data.getBasicInfoValue('deployMode', '单节点模式')}"
    scheduler_type = "${data.getBasicInfoValue('schedulerType', 'Capacity Scheduler')}"
    
    # 安全认证配置
    enable_kerberos = "${data.getSecurityInfoValue('kerberos', '否')}" == "是"
    principal = "${data.getSecurityInfoValue('principal', '')}"
    keytab_path = "${data.getSecurityInfoValue('keytab', '')}"
    
    print("YARN ResourceManager连接示例")
    print(f"ResourceManager地址: {rm_address}")
    print(f"Web UI地址: {web_address}")
    print(f"部署模式: {deploy_mode}")
    print(f"调度器类型: {scheduler_type}")
    print(f"启用Kerberos: {enable_kerberos}")
    
    try:
        # 使用REST API连接
        print("\n----- 使用REST API连接 -----")
        use_rest_api(web_address, enable_kerberos)
        
        # 使用yarn-api-client库连接（如果可用）
        if has_yarn_client:
            print("\n----- 使用yarn-api-client库连接 -----")
            use_yarn_client(web_address, enable_kerberos)
        else:
            print("\n----- yarn-api-client库未安装，跳过此部分 -----")
    
    except Exception as e:
        print(f"YARN连接失败: {str(e)}", file=sys.stderr)
        raise

def use_rest_api(web_address, enable_kerberos=False):
    """使用REST API访问YARN ResourceManager"""
    
    # 确保web_address以http开头
    if not web_address.startswith('http'):
        web_address = 'http://' + web_address
    
    # 构建API基础URL
    api_base = urljoin(web_address, '/ws/v1/cluster/')
    
    # 设置请求会话
    session = requests.Session()
    
    # 如果启用了Kerberos，需要使用Kerberos认证
    # 这里需要requests_kerberos库
    if enable_kerberos:
        try:
            from requests_kerberos import HTTPKerberosAuth
            session.auth = HTTPKerberosAuth()
            print("已启用Kerberos认证")
        except ImportError:
            print("警告: 启用了Kerberos但未安装requests_kerberos库")
    
    # 获取集群信息
    print("\n=== 集群信息 ===")
    try:
        response = session.get(api_base + 'info')
        response.raise_for_status()
        cluster_info = response.json()
        info = cluster_info.get('clusterInfo', {})
        print(f"集群ID: {info.get('id', 'N/A')}")
        print(f"ResourceManager版本: {info.get('resourceManagerVersion', 'N/A')}")
        print(f"启动时间: {datetime.fromtimestamp(info.get('startedOn', 0)/1000).strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"状态: {info.get('state', 'N/A')}")
        print(f"HA状态: {info.get('haState', 'N/A')}")
    except Exception as e:
        print(f"获取集群信息失败: {str(e)}")
    
    # 获取集群指标
    print("\n=== 集群指标 ===")
    try:
        response = session.get(api_base + 'metrics')
        response.raise_for_status()
        metrics = response.json().get('clusterMetrics', {})
        print(f"Node Manager数量: {metrics.get('nodeManagers', 0)}")
        print(f"活跃节点数: {metrics.get('activeNodes', 0)}")
        print(f"已分配容器数: {metrics.get('containersAllocated', 0)}")
        print(f"已分配内存: {metrics.get('allocatedMB', 0)} MB")
        print(f"已分配CPU: {metrics.get('allocatedVirtualCores', 0)} vCores")
        print(f"挂起应用数: {metrics.get('appsPending', 0)}")
        print(f"运行中应用数: {metrics.get('appsRunning', 0)}")
        print(f"已完成应用数: {metrics.get('appsCompleted', 0)}")
    except Exception as e:
        print(f"获取集群指标失败: {str(e)}")
    
    # 获取节点列表
    print("\n=== 节点列表 ===")
    try:
        response = session.get(api_base + 'nodes')
        response.raise_for_status()
        nodes = response.json().get('nodes', {}).get('node', [])
        print(f"找到 {len(nodes)} 个节点")
        
        for i, node in enumerate(nodes[:5], 1):  # 只显示前5个节点
            print(f"\n节点 {i}:")
            print(f"  节点ID: {node.get('id', 'N/A')}")
            print(f"  主机名: {node.get('nodeHostName', 'N/A')}")
            print(f"  状态: {node.get('state', 'N/A')}")
            print(f"  机架: {node.get('rack', 'N/A')}")
            print(f"  内存总量: {node.get('totalMemoryMB', 0)} MB")
            print(f"  可用内存: {node.get('availMemoryMB', 0)} MB")
            print(f"  vCores总量: {node.get('totalVirtualCores', 0)}")
            print(f"  可用vCores: {node.get('availableVirtualCores', 0)}")
        
        if len(nodes) > 5:
            print(f"\n... 省略其他 {len(nodes) - 5} 个节点 ...")
    except Exception as e:
        print(f"获取节点列表失败: {str(e)}")
    
    # 获取应用列表
    print("\n=== 应用列表 ===")
    try:
        response = session.get(api_base + 'apps')
        response.raise_for_status()
        
        apps_data = response.json().get('apps', {})
        if apps_data is None:
            print("当前没有正在运行的应用")
        else:
            apps = apps_data.get('app', [])
            print(f"找到 {len(apps)} 个应用")
            
            for i, app in enumerate(apps[:5], 1):  # 只显示前5个应用
                print(f"\n应用 {i}:")
                print(f"  应用ID: {app.get('id', 'N/A')}")
                print(f"  名称: {app.get('name', 'N/A')}")
                print(f"  类型: {app.get('applicationType', 'N/A')}")
                print(f"  用户: {app.get('user', 'N/A')}")
                print(f"  队列: {app.get('queue', 'N/A')}")
                print(f"  状态: {app.get('state', 'N/A')}")
                print(f"  进度: {app.get('progress', 0):.1f}%")
                
                start_time = app.get('startedTime', 0)
                if start_time > 0:
                    start_time_str = datetime.fromtimestamp(start_time/1000).strftime('%Y-%m-%d %H:%M:%S')
                    print(f"  启动时间: {start_time_str}")
                
                tracking_url = app.get('trackingUrl', 'N/A')
                print(f"  跟踪URL: {tracking_url}")
            
            if len(apps) > 5:
                print(f"\n... 省略其他 {len(apps) - 5} 个应用 ...")
    except Exception as e:
        print(f"获取应用列表失败: {str(e)}")
    
    # 获取调度器信息
    print("\n=== 调度器信息 ===")
    try:
        response = session.get(api_base + 'scheduler')
        response.raise_for_status()
        
        scheduler = response.json().get('scheduler', {}).get('schedulerInfo', {})
        print(f"调度器类型: {scheduler.get('type', 'N/A')}")
        
        # Capacity Scheduler特有信息
        if 'capacityScheduler' in scheduler.get('type', '').lower():
            print("\n容量调度器信息:")
            print(f"  总容量百分比: {scheduler.get('capacity', 0):.1f}%")
            print(f"  已使用容量百分比: {scheduler.get('usedCapacity', 0):.1f}%")
            
            # 打印根队列的子队列信息
            queues = scheduler.get('queues', {}).get('queue', [])
            for queue in queues:
                print_queue_info(queue)
    except Exception as e:
        print(f"获取调度器信息失败: {str(e)}")

def print_queue_info(queue, indent="  "):
    """递归打印队列信息"""
    print(f"{indent}队列: {queue.get('queueName', 'N/A')}")
    print(f"{indent}  状态: {queue.get('state', 'N/A')}")
    print(f"{indent}  容量百分比: {queue.get('capacity', 0):.1f}%")
    print(f"{indent}  已使用容量: {queue.get('usedCapacity', 0):.1f}%")
    print(f"{indent}  绝对容量: {queue.get('absoluteCapacity', 0):.1f}%")
    print(f"{indent}  已使用绝对容量: {queue.get('absoluteUsedCapacity', 0):.1f}%")
    print(f"{indent}  最大容量: {queue.get('maxCapacity', 0):.1f}%")
    print(f"{indent}  绝对最大容量: {queue.get('absoluteMaxCapacity', 0):.1f}%")
    print(f"{indent}  正在运行的应用数: {queue.get('numApplications', 0)}")
    
    # 递归打印子队列
    if 'queues' in queue and queue['queues'] is not None:
        child_queues = queue['queues'].get('queue', [])
        for child_queue in child_queues:
            print_queue_info(child_queue, indent + "  ")

def use_yarn_client(web_address, enable_kerberos=False):
    """使用yarn_api_client库访问YARN ResourceManager"""
    
    # 解析主机和端口
    if "://" in web_address:
        web_address = web_address.split("://")[1]
    
    host, port = web_address.split(':')
    port = int(port)
    
    # 创建ResourceManager客户端
    rm = ResourceManager(host=host, port=port, timeout=30)
    
    # 获取集群信息
    print("\n=== 使用yarn_api_client获取集群信息 ===")
    cluster_info = rm.cluster_information().data
    info = cluster_info.get('clusterInfo', {})
    print(f"集群ID: {info.get('id', 'N/A')}")
    print(f"ResourceManager版本: {info.get('resourceManagerVersion', 'N/A')}")
    print(f"状态: {info.get('state', 'N/A')}")
    
    # 获取集群指标
    metrics = rm.cluster_metrics().data
    cluster_metrics = metrics.get('clusterMetrics', {})
    print(f"\n节点数: {cluster_metrics.get('nodeManagers', 0)}")
    print(f"活跃节点数: {cluster_metrics.get('activeNodes', 0)}")
    print(f"总内存: {cluster_metrics.get('totalMB', 0)} MB")
    print(f"总CPU: {cluster_metrics.get('totalVirtualCores', 0)} vCores")
    
    # 获取应用列表
    print("\n=== 应用列表 ===")
    apps = rm.cluster_applications().data
    apps_data = apps.get('apps', {})
    
    if apps_data is None:
        print("当前没有正在运行的应用")
    else:
        app_list = apps_data.get('app', [])
        print(f"找到 {len(app_list)} 个应用")
        
        for i, app in enumerate(app_list[:3], 1):  # 只显示前3个应用
            print(f"\n应用 {i}:")
            print(f"  应用ID: {app.get('id', 'N/A')}")
            print(f"  名称: {app.get('name', 'N/A')}")
            print(f"  类型: {app.get('applicationType', 'N/A')}")
            print(f"  用户: {app.get('user', 'N/A')}")
            print(f"  状态: {app.get('state', 'N/A')}")
    
    # 示例：获取特定应用信息
    print("\n=== 应用详细信息示例 ===")
    print("# 要获取特定应用的详细信息，可以使用以下代码：")
    print("app_id = 'application_1234567890_0001'")
    print("app_info = rm.cluster_application(application_id=app_id).data")
    print("print(f\"应用名称: {app_info['app']['name']}\")")
    print("print(f\"应用状态: {app_info['app']['state']}\")")
    
    # 示例：获取应用尝试信息
    print("\n=== 应用尝试信息示例 ===")
    print("# 要获取应用的尝试信息，可以使用以下代码：")
    print("app_attempts = rm.cluster_application_attempts(application_id=app_id).data")
    print("attempts = app_attempts['appAttempts']['appAttempt']")
    print("for attempt in attempts:")
    print("    print(f\"尝试ID: {attempt['id']}\")")
    print("    print(f\"容器ID: {attempt['containerId']}\")")

def yarn_application_submission_example():
    """YARN应用提交示例（仅示例代码，不实际执行）"""
    print("\n=== YARN应用提交示例代码 ===")
    print("# 要提交一个应用到YARN，需要以下步骤：")
    print("# 1. 准备应用JAR包及相关资源")
    print("# 2. 使用Hadoop客户端API或使用命令行工具进行提交")
    print("\n# 命令行提交示例:")
    print('hadoop jar /path/to/your/app.jar MainClass \\')
    print('    -D mapreduce.job.name="My Job" \\')
    print('    -D yarn.resourcemanager.address=rm-host:8032 \\')
    print('    --input /user/input \\')
    print('    --output /user/output')
    
    print("\n# 使用Spark提交YARN应用示例:")
    print('spark-submit \\')
    print('    --master yarn \\')
    print('    --deploy-mode cluster \\')
    print('    --driver-memory 1g \\')
    print('    --executor-memory 1g \\')
    print('    --executor-cores 1 \\')
    print('    --num-executors 2 \\')
    print('    --queue default \\')
    print('    /path/to/your/spark-app.py')
    
    print("\n# 使用Python代码调用shell命令提交应用:")
    print('import subprocess')
    print('cmd = ["hadoop", "jar", "/path/to/your/app.jar", "MainClass",')
    print('       "-D", "mapreduce.job.name=My Job",')
    print('       "--input", "/user/input",')
    print('       "--output", "/user/output"]')
    print('subprocess.run(cmd, check=True)')

if __name__ == '__main__':
    main()
    
    # 显示应用提交示例（仅示例代码）
    yarn_application_submission_example() 