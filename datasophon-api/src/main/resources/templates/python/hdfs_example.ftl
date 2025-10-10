DEPENDENCIES_START
# pip依赖：
pyarrow==10.0.0
hdfs==2.7.0
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>requests-kerberos==0.14.0</#if>
DEPENDENCIES_END

# HDFS Python API 示例代码
#
# 本示例展示如何使用Python API操作HDFS文件系统
#
# 连接信息:
# HDFS URI: ${data.getConnectInfoValue('hdfsUri', 'hdfs://localhost:8020')}
# 主机: ${data.getBasicInfoValue('host', 'localhost')}
# 端口: ${data.getBasicInfoValue('port', '8020')}
<#if data.getBasicInfoValue('highAvailability', 'false') == 'true'># HA模式: 已启用<#if data.getBasicInfoValue('nameservice', '') != ''>, Nameservice: ${data.getBasicInfoValue('nameservice', '')}</#if></#if>
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'># Kerberos认证: 已启用</#if>

##############################################################
# 方法一: 使用PyArrow访问HDFS (推荐)
##############################################################
import pyarrow as pa
import pyarrow.fs as fs
import os
import time

print("=== PyArrow HDFS 示例 ===")

# PyArrow配置参数
hdfs_host = "${data.getBasicInfoValue('host', 'localhost')}"
hdfs_port = ${data.getBasicInfoValue('port', '8020')}

try:
    # 创建HDFS连接
    print(f"连接到HDFS: {hdfs_host}:{hdfs_port}")
    
    # 创建HDFS文件系统对象
    hdfs = fs.HadoopFileSystem(
        host=hdfs_host,
        port=hdfs_port,
        <#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
        # Kerberos认证配置
        kerb_ticket=None  # 使用当前登录的Kerberos票据
        # 注意: 请先使用kinit命令获取Kerberos票据
        </#if>
    )
    
    # 测试路径
    test_dir = "/user/example"
    test_file = f"{test_dir}/test.txt"
    
    print(f"\n1. 创建目录: {test_dir}")
    try:
        hdfs.create_dir(test_dir)
        print(f"   目录创建成功")
    except Exception as e:
        print(f"   目录可能已存在: {str(e)}")
    
    print(f"\n2. 写入文件: {test_file}")
    with hdfs.open_output_stream(test_file) as writer:
        content = f"Hello, HDFS!\n这是一个Python测试文件。\n当前时间: {time.ctime()}"
        writer.write(content.encode('utf-8'))
    print(f"   文件写入成功")
    
    print(f"\n3. 读取文件: {test_file}")
    with hdfs.open_input_stream(test_file) as reader:
        content = reader.read()
        print("   文件内容:")
        print("   " + content.decode('utf-8').replace('\n', '\n   '))
    
    print(f"\n4. 获取文件信息")
    file_info = hdfs.get_file_info(test_file)
    print(f"   路径: {file_info.path}")
    print(f"   大小: {file_info.size} 字节")
    print(f"   类型: {'目录' if file_info.type == fs.FileType.Directory else '文件'}")
    print(f"   修改时间: {time.ctime(file_info.mtime)}")
    
    print(f"\n5. 列出目录: {test_dir}")
    file_selector = fs.FileSelector(test_dir)
    dir_listing = hdfs.get_file_info(file_selector)
    print(f"   目录内容 ({len(dir_listing)} 项):")
    for item in dir_listing:
        file_type = "目录" if item.type == fs.FileType.Directory else "文件"
        print(f"   - {file_type}: {os.path.basename(item.path)} ({item.size} 字节)")
    
    print(f"\n6. 删除文件: {test_file}")
    hdfs.delete_file(test_file)
    print(f"   文件删除成功")
    
    print(f"\n7. 删除目录: {test_dir}")
    hdfs.delete_dir(test_dir)
    print(f"   目录删除成功")
    
except Exception as e:
    print(f"PyArrow HDFS 操作出错: {str(e)}")

##############################################################
# 方法二: 使用hdfs包访问HDFS
##############################################################
print("\n\n=== hdfs包 HDFS 示例 ===")

from hdfs import InsecureClient
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
# 注意: hdfs包不直接支持Kerberos，建议使用PyArrow
# 或者使用kinit命令先获取Kerberos票据
</#if>

try:
    # 创建HDFS客户端
    client = InsecureClient('${data.getConnectInfoValue('hdfsUri', 'hdfs://localhost:8020')}', user='hdfs')
    print(f"连接到HDFS: ${data.getConnectInfoValue('hdfsUri', 'hdfs://localhost:8020')}")
    
    # 测试路径
    test_dir = "/user/example2"
    test_file = f"{test_dir}/test.txt"
    
    print(f"\n1. 创建目录: {test_dir}")
    client.makedirs(test_dir)
    print(f"   目录创建成功")
    
    print(f"\n2. 写入文件: {test_file}")
    content = f"Hello, HDFS!\n这是通过hdfs包创建的测试文件。\n当前时间: {time.ctime()}"
    client.write(test_file, data=content.encode('utf-8'))
    print(f"   文件写入成功")
    
    print(f"\n3. 读取文件: {test_file}")
    with client.read(test_file) as reader:
        content = reader.read()
        print("   文件内容:")
        print("   " + content.decode('utf-8').replace('\n', '\n   '))
    
    print(f"\n4. 获取文件状态")
    status = client.status(test_file)
    print(f"   路径: {test_file}")
    print(f"   大小: {status['length']} 字节")
    print(f"   类型: {status['type']}")
    print(f"   修改时间: {status['modificationTime']}")
    print(f"   权限: {status['permission']}")
    print(f"   所有者: {status['owner']}")
    print(f"   组: {status['group']}")
    
    print(f"\n5. 列出目录: {test_dir}")
    dir_listing = client.list(test_dir, status=True)
    print(f"   目录内容:")
    for name, status in dir_listing.items():
        print(f"   - {status['type']}: {name} ({status['length']} 字节)")
    
    print(f"\n6. 删除文件: {test_file}")
    client.delete(test_file)
    print(f"   文件删除成功")
    
    print(f"\n7. 删除目录: {test_dir}")
    client.delete(test_dir, recursive=True)
    print(f"   目录删除成功")
    
except Exception as e:
    print(f"hdfs包 HDFS 操作出错: {str(e)}")

##############################################################
# WebHDFS REST API 示例 (使用Python requests库)
##############################################################
<#if data.getConnectInfoValue('webhdfsUri', '') != ''>
print("\n\n=== WebHDFS REST API 示例 ===")

import requests
import json

# WebHDFS URI
webhdfs_uri = "${data.getConnectInfoValue('webhdfsUri', '')}"
print(f"连接到WebHDFS: {webhdfs_uri}")

try:
    # 测试路径
    test_dir = "/user/example3"
    test_file = f"{test_dir}/test.txt"
    
    print(f"\n1. 创建目录: {test_dir}")
    response = requests.put(f"{webhdfs_uri}{test_dir}?op=MKDIRS")
    result = response.json()
    if result.get('boolean', False):
        print(f"   目录创建成功")
    else:
        print(f"   目录创建失败: {json.dumps(result)}")
    
    print(f"\n2. 写入文件: {test_file}")
    # WebHDFS写入文件需要两步: 1) 获取重定向URL; 2) 发送数据到重定向URL
    init_response = requests.put(f"{webhdfs_uri}{test_file}?op=CREATE&overwrite=true", allow_redirects=False)
    if init_response.status_code == 307:
        redirect_url = init_response.headers['Location']
        content = f"Hello, WebHDFS!\n这是通过REST API创建的测试文件。\n当前时间: {time.ctime()}"
        put_response = requests.put(redirect_url, data=content.encode('utf-8'))
        if put_response.status_code == 201:
            print(f"   文件写入成功")
        else:
            print(f"   文件写入失败: {put_response.status_code} - {put_response.text}")
    else:
        print(f"   获取重定向URL失败: {init_response.status_code} - {init_response.text}")
    
    print(f"\n3. 读取文件: {test_file}")
    response = requests.get(f"{webhdfs_uri}{test_file}?op=OPEN")
    if response.status_code == 200:
        print("   文件内容:")
        print("   " + response.text.replace('\n', '\n   '))
    else:
        print(f"   文件读取失败: {response.status_code} - {response.text}")
    
    print(f"\n4. 获取文件状态")
    response = requests.get(f"{webhdfs_uri}{test_file}?op=GETFILESTATUS")
    if response.status_code == 200:
        status = response.json()['FileStatus']
        print(f"   路径: {test_file}")
        print(f"   大小: {status['length']} 字节")
        print(f"   类型: {status['type']}")
        print(f"   修改时间: {status['modificationTime']}")
        print(f"   权限: {status['permission']}")
        print(f"   所有者: {status['owner']}")
        print(f"   组: {status['group']}")
    else:
        print(f"   获取文件状态失败: {response.status_code} - {response.text}")
    
    print(f"\n5. 列出目录: {test_dir}")
    response = requests.get(f"{webhdfs_uri}{test_dir}?op=LISTSTATUS")
    if response.status_code == 200:
        statuses = response.json()['FileStatuses']['FileStatus']
        print(f"   目录内容 ({len(statuses)} 项):")
        for status in statuses:
            print(f"   - {status['type']}: {status['pathSuffix']} ({status['length']} 字节)")
    else:
        print(f"   列出目录失败: {response.status_code} - {response.text}")
    
    print(f"\n6. 删除文件: {test_file}")
    response = requests.delete(f"{webhdfs_uri}{test_file}?op=DELETE")
    result = response.json()
    if result.get('boolean', False):
        print(f"   文件删除成功")
    else:
        print(f"   文件删除失败: {json.dumps(result)}")
    
    print(f"\n7. 删除目录: {test_dir}")
    response = requests.delete(f"{webhdfs_uri}{test_dir}?op=DELETE&recursive=true")
    result = response.json()
    if result.get('boolean', False):
        print(f"   目录删除成功")
    else:
        print(f"   目录删除失败: {json.dumps(result)}")
    
except Exception as e:
    print(f"WebHDFS REST API 操作出错: {str(e)}")
<#else>
# WebHDFS REST API 需要配置WebHDFS URI才能使用
# 请在集群配置中启用WebHDFS并提供正确的URI
</#if>

print("\n\nHDFS Python示例执行完成") 