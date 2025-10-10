DEPENDENCIES_START
# 依赖包：
happybase==1.2.0
thriftpy2==0.4.14
<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
kerberos==1.3.1
gssapi==1.7.0
</#if>
DEPENDENCIES_END

#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
HBase Python连接示例
使用happybase库连接HBase并执行各种操作
"""

import happybase
import time
import struct
import sys
from datetime import datetime
<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
import os
import kerberos
import gssapi
</#if>

def main():
    """
    主函数，演示HBase的连接和各种操作
    """
    print("===== HBase Python客户端示例 =====")

    # 连接参数
    host = "${data.getBasicInfoValue('host', 'localhost')}"
    port = ${data.getBasicInfoValue('port', '16020')}  # 注意：HBase Thrift服务默认端口是16020
    zk_quorum = "${data.getBasicInfoValue('zkQuorum', 'localhost')}"
    zk_port = "${data.getBasicInfoValue('zkPort', '2181')}"
    zk_root = "${data.getBasicInfoValue('zkRootNode', '/hbase')}"
    
    # 表名常量
    TABLE_NAME = 'test_table'
    COLUMN_FAMILY_1 = 'cf1'
    COLUMN_FAMILY_2 = 'cf2'
    
<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
    # Kerberos配置
    try:
        print("配置Kerberos认证...")
        
        # 设置KRB5_CONFIG环境变量
        os.environ['KRB5_CONFIG'] = '/etc/krb5.conf'
        
        # 可以使用kinit命令进行认证
        # 或者使用keytab文件认证:
        # import subprocess
        # subprocess.call(['kinit', '-kt', '/path/to/user.keytab', 'user@EXAMPLE.COM'])
        
        print("Kerberos认证已配置")
    except Exception as e:
        print(f"Kerberos认证配置失败: {str(e)}", file=sys.stderr)
        return
        
    # 使用支持Kerberos的连接选项
    connection_kwargs = {
        'host': host,
        'port': port,
        'table_prefix': None,
        'timeout': 30000,
        'transport': 'framed',
        'protocol': 'compact'
    }
<#else>
    # 标准连接选项
    connection_kwargs = {
        'host': host,
        'port': port,
        'table_prefix': None,
        'timeout': 30000
    }
</#if>

    try:
        # 创建连接
        print(f"连接到HBase: {host}:{port}")
        connection = happybase.Connection(**connection_kwargs)
        
        # 获取服务器版本
        print(f"HBase版本信息: {connection.client.getVersionString()}")
            
        # 列出已有的表
        list_tables(connection)
        
        # 表操作示例
        table_operations_example(connection, TABLE_NAME, COLUMN_FAMILY_1, COLUMN_FAMILY_2)
        
        # 数据操作示例
        data_operations_example(connection, TABLE_NAME, COLUMN_FAMILY_1, COLUMN_FAMILY_2)
        
        # 高级功能示例
        advanced_features_example(connection, TABLE_NAME, COLUMN_FAMILY_1, COLUMN_FAMILY_2)
        
        # 关闭连接
        connection.close()
        print("连接已关闭")
        
    except Exception as e:
        print(f"HBase操作失败: {str(e)}", file=sys.stderr)
        raise

def list_tables(connection):
    """
    列出HBase中的所有表
    """
    print("\n----- 列出HBase中的表 -----")
    tables = connection.tables()
    if not tables:
        print("HBase中没有表")
    else:
        print("HBase中的表：")
        for table in tables:
            # 将字节转换为字符串（如果需要）
            table_name = table.decode('utf-8') if isinstance(table, bytes) else table
            print(f"  - {table_name}")

def table_operations_example(connection, table_name, cf1, cf2):
    """
    表操作示例
    """
    print(f"\n----- 表操作示例 -----")
    
    # 检查表是否存在（将字符串转换为字节类型，如果需要）
    table_name_bytes = table_name.encode('utf-8') if not isinstance(table_name, bytes) else table_name
    tables = connection.tables()
    table_exists = table_name_bytes in tables
    print(f"表 {table_name} 是否存在: {table_exists}")
    
    # 如果表存在，先删除
    if table_exists:
        print(f"删除已存在的表: {table_name}")
        connection.delete_table(table_name, disable=True)
        print(f"表 {table_name} 已删除")
    
    # 创建新表
    print(f"创建新表: {table_name}")
    # 创建列族
    families = {
        cf1: dict(max_versions=3),  # 保留3个版本
        cf2: dict(time_to_live=86400)  # TTL 1天 (秒)
    }
    connection.create_table(table_name, families)
    print(f"表 {table_name} 创建成功")
    
    # 获取表描述信息
    table = connection.table(table_name)
    families = connection.client.getColumnDescriptors(table_name_bytes)
    
    print(f"表 {table_name} 描述信息:")
    print(f"  - 列族数量: {len(families)}")
    for family_name, family_attrs in families.items():
        # 将字节转换为字符串
        family_name_str = family_name.decode('utf-8') if isinstance(family_name, bytes) else family_name
        print(f"  - 列族: {family_name_str}")
        
        # 获取最大版本数和TTL（如果有）
        max_versions = family_attrs.get(b'VERSIONS', b'1')
        max_versions = max_versions.decode('utf-8') if isinstance(max_versions, bytes) else max_versions
        print(f"    - 最大版本数: {max_versions}")
        
        ttl = family_attrs.get(b'TTL', b'FOREVER')
        ttl = ttl.decode('utf-8') if isinstance(ttl, bytes) else ttl
        print(f"    - TTL: {ttl}")

def data_operations_example(connection, table_name, cf1, cf2):
    """
    数据操作示例
    """
    print(f"\n----- 数据操作示例 -----")
    
    # 获取表对象
    table = connection.table(table_name)
    
    # 写入数据示例
    print("写入数据...")
    
    # 格式化列名
    def format_column(family, qualifier):
        return f"{family}:{qualifier}"
    
    # 用户1数据
    table.put(b'row1', {
        format_column(cf1, 'name').encode('utf-8'): '张三'.encode('utf-8'),
        format_column(cf1, 'age').encode('utf-8'): '30'.encode('utf-8'),
        format_column(cf2, 'email').encode('utf-8'): 'zhangsan@example.com'.encode('utf-8')
    })
    
    # 用户2数据
    table.put(b'row2', {
        format_column(cf1, 'name').encode('utf-8'): '李四'.encode('utf-8'),
        format_column(cf1, 'age').encode('utf-8'): '25'.encode('utf-8'),
        format_column(cf2, 'email').encode('utf-8'): 'lisi@example.com'.encode('utf-8')
    })
    
    # 用户3数据
    table.put(b'row3', {
        format_column(cf1, 'name').encode('utf-8'): '王五'.encode('utf-8'),
        format_column(cf1, 'age').encode('utf-8'): '35'.encode('utf-8'),
        format_column(cf2, 'email').encode('utf-8'): 'wangwu@example.com'.encode('utf-8')
    })
    
    print("成功插入3条数据")
    
    # 读取单行数据
    print("\n获取单行数据:")
    row = table.row(b'row1')
    print_row(b'row1', row)
    
    # 扫描表数据
    print("\n扫描表数据:")
    for key, data in table.scan():
        print_row(key, data)
    
    # 更新数据
    print("\n更新row1的age值:")
    table.put(b'row1', {
        format_column(cf1, 'age').encode('utf-8'): '31'.encode('utf-8')
    })
    
    # 验证更新
    row = table.row(b'row1')
    age = row.get(format_column(cf1, 'age').encode('utf-8'))
    age_str = age.decode('utf-8') if isinstance(age, bytes) else age
    print(f"row1的age更新为: {age_str}")
    
    # 删除数据
    print("\n删除row3:")
    table.delete(b'row3')
    
    # 验证删除
    row = table.row(b'row3')
    print(f"row3是否存在: {len(row) > 0}")
    
    # 再次扫描表数据
    print("\n删除后的表数据:")
    for key, data in table.scan():
        print_row(key, data)

def advanced_features_example(connection, table_name, cf1, cf2):
    """
    高级功能示例
    """
    print(f"\n----- 高级功能示例 -----")
    
    # 获取表对象
    table = connection.table(table_name)
    
    # 1. 使用过滤器进行查询
    print("\n使用过滤器进行查询:")
    print("查询年龄大于25的用户:")
    
    # 创建过滤器
    from happybase.hbase.ttypes import TScan, TColumn
    
    # 注意：happybase的过滤器语法使用的是HBase过滤器字符串
    scan_filter = "SingleColumnValueFilter('{cf}', 'age', >, 'binary:25')".format(cf=cf1)
    
    # 扫描并应用过滤器
    for key, data in table.scan(filter=scan_filter):
        print_row(key, data)
    
    # 2. 批量操作示例
    print("\n批量操作示例:")
    
    # 批量获取数据
    print("批量获取行:")
    rows = table.rows([b'row1', b'row2'])
    for key, data in rows:
        print_row(key, data)
    
    # 批量写入数据
    print("\n批量写入数据:")
    batch = table.batch()
    
    batch.put(b'batch1', {
        f"{cf1}:name".encode('utf-8'): '赵六'.encode('utf-8'),
        f"{cf1}:age".encode('utf-8'): '28'.encode('utf-8')
    })
    
    batch.put(b'batch2', {
        f"{cf1}:name".encode('utf-8'): '钱七'.encode('utf-8'),
        f"{cf1}:age".encode('utf-8'): '32'.encode('utf-8')
    })
    
    # 提交批量操作
    batch.send()
    print("批量写入成功")
    
    # 验证批量写入
    print("\n验证批量写入:")
    for key, data in table.scan(row_prefix=b'batch'):
        print_row(key, data)
    
    # 3. 计数器操作
    print("\n计数器操作示例:")
    
    # 创建或重置计数器
    table.put(b'counter', {
        f"{cf1}:visits".encode('utf-8'): struct.pack('>q', 0)  # 打包为长整数
    })
    
    # 递增计数器
    print("递增计数器5次")
    for i in range(5):
        table.counter_inc(b'counter', f"{cf1}:visits".encode('utf-8'))
    
    # 获取计数器值
    counter_value = table.counter_get(b'counter', f"{cf1}:visits".encode('utf-8'))
    print(f"计数器值: {counter_value}")
    
    # 4. 行前缀扫描
    print("\n行前缀扫描:")
    print("扫描所有'row'开头的行:")
    for key, data in table.scan(row_prefix=b'row'):
        print_row(key, data)
    
    # 5. 列前缀扫描
    print("\n列前缀扫描:")
    print(f"获取row1中{cf1}列族数据:")
    row = table.row(b'row1', columns=[f"{cf1}".encode('utf-8')])
    print_row(b'row1', row)

def print_row(row_key, row_data):
    """
    打印一行数据
    """
    # 将行键转换为字符串
    row_key_str = row_key.decode('utf-8') if isinstance(row_key, bytes) else row_key
    print(f"Row: {row_key_str}")
    
    if not row_data:
        print("  - 无数据")
        return
    
    # 处理行数据
    for column, value in row_data.items():
        # 将列名和值转换为字符串
        column_str = column.decode('utf-8') if isinstance(column, bytes) else column
        
        # 尝试将值解码为UTF-8字符串，如果失败则保持原样
        try:
            value_str = value.decode('utf-8') if isinstance(value, bytes) else value
        except UnicodeDecodeError:
            # 如果无法解码为UTF-8，使用原始字节表示
            value_str = f"[二进制数据，长度: {len(value)} 字节]"
        
        print(f"  - {column_str} = {value_str}")

if __name__ == '__main__':
    main() 