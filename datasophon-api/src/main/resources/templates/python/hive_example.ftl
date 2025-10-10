DEPENDENCIES_START
# 安装依赖：
# pip install pyhive
# pip install thrift
# pip install pandas
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
# pip install thrift_sasl
# pip install kerberos
# pip install pyspnego
</#if>
DEPENDENCIES_END

#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Hive 连接示例
使用 PyHive 连接 Hive 并执行查询
"""

import pandas as pd
import logging
import sys
from pyhive import hive
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
import os
import kerberos
</#if>
from thrift.transport.TTransport import TTransportException

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger('HiveExample')

# Hive 连接配置
HOST = "${data.getBasicInfoValue('host', 'localhost')}"
PORT = ${data.getBasicInfoValue('port', '10000')}
DATABASE = "${data.getConnectInfoValue('database', 'default')}"
USERNAME = "${data.getSecurityInfoValue('username', '')}"
PASSWORD = "${data.getSecurityInfoValue('password', '')}"

# Kerberos 配置
KERBEROS_ENABLED = ${data.getSecurityInfoValue('kerberos.enabled', 'false')}
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
KRB5_CONF = "${data.getSecurityInfoValue('krb5.conf.path', '/etc/krb5.conf')}"
KEYTAB_PATH = "${data.getSecurityInfoValue('keytab.path', '')}"
PRINCIPAL = "${data.getSecurityInfoValue('principal', '')}"
</#if>

def connect_to_hive():
    """
    连接到 Hive 服务器
    """
    logger.info(f"连接到 Hive 服务器: {HOST}:{PORT}, 数据库: {DATABASE}")
    
    connection_params = {
        'host': HOST,
        'port': PORT,
        'database': DATABASE,
    }
    
    # 添加认证参数
    if KERBEROS_ENABLED:
        logger.info("使用 Kerberos 认证")
        <#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
        # 设置 Kerberos 环境变量
        os.environ['KRB5_CONFIG'] = KRB5_CONF
        
        # 使用 keytab 进行认证 (如果提供)
        if KEYTAB_PATH and PRINCIPAL:
            try:
                logger.info(f"使用 keytab 文件认证: {KEYTAB_PATH}, 主体: {PRINCIPAL}")
                import subprocess
                subprocess.call(['kinit', '-kt', KEYTAB_PATH, PRINCIPAL])
            except Exception as e:
                logger.error(f"Keytab 认证失败: {e}")
        
        connection_params.update({
            'auth': 'KERBEROS',
            'kerberos_service_name': 'hive'
        })
        </#if>
    elif USERNAME:
        logger.info(f"使用用户名密码认证: {USERNAME}")
        connection_params.update({
            'username': USERNAME,
            'password': PASSWORD if PASSWORD else ''
        })
    
    try:
        return hive.Connection(**connection_params)
    except TTransportException as e:
        logger.error(f"连接错误: {e}")
        if KERBEROS_ENABLED:
            logger.error("Kerberos 认证可能失败，请确认 Kerberos 票据有效")
            logger.info("可以通过运行 'klist' 命令检查当前票据状态")
        raise
    except Exception as e:
        logger.error(f"连接失败: {e}")
        raise

def query_with_pandas(connection, sql):
    """
    使用 pandas 执行查询并返回结果 DataFrame
    """
    logger.info(f"执行查询: {sql}")
    try:
        return pd.read_sql(sql, connection)
    except Exception as e:
        logger.error(f"查询失败: {e}")
        return None

def main():
    logger.info("Hive Python 示例程序")
    
    try:
        # 连接到 Hive
        conn = connect_to_hive()
        logger.info("连接成功!")
        
        # 1. 查看所有数据库
        logger.info("查询所有数据库:")
        df_databases = query_with_pandas(conn, "SHOW DATABASES")
        if df_databases is not None:
            for idx, row in df_databases.head(5).iterrows():
                logger.info(f"  数据库: {row[0]}")
            if len(df_databases) > 5:
                logger.info(f"  ... (还有 {len(df_databases) - 5} 个数据库)")
        
        # 2. 切换到指定数据库
        logger.info(f"使用数据库: {DATABASE}")
        with conn.cursor() as cursor:
            cursor.execute(f"USE {DATABASE}")
        
        # 3. 查看所有表
        logger.info("查询所有表:")
        df_tables = query_with_pandas(conn, "SHOW TABLES")
        if df_tables is not None and not df_tables.empty:
            for idx, row in df_tables.head(5).iterrows():
                logger.info(f"  表: {row[0]}")
            if len(df_tables) > 5:
                logger.info(f"  ... (还有 {len(df_tables) - 5} 个表)")
        else:
            logger.info("  没有表，创建示例表")
            with conn.cursor() as cursor:
                # 创建示例表
                cursor.execute("""
                CREATE TABLE IF NOT EXISTS example_table (
                    id INT, 
                    name STRING, 
                    value DOUBLE, 
                    create_time TIMESTAMP
                ) ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
                """)
                logger.info("  创建了示例表: example_table")
                
                # 插入示例数据
                cursor.execute("""
                INSERT INTO example_table VALUES 
                (1, 'Item 1', 10.5, CURRENT_TIMESTAMP), 
                (2, 'Item 2', 20.75, CURRENT_TIMESTAMP), 
                (3, 'Item 3', 30.25, CURRENT_TIMESTAMP)
                """)
                logger.info("  插入了示例数据")
        
        # 4. 查询表数据
        try:
            logger.info("查询示例表数据:")
            df_data = query_with_pandas(conn, "SELECT * FROM example_table LIMIT 10")
            if df_data is not None and not df_data.empty:
                logger.info("\n" + str(df_data))
        except Exception as e:
            logger.warning(f"查询示例表失败: {e}")
        
        # 5. 高级查询示例
        logger.info("高级查询示例和技巧:")
        logger.info("  1. 使用参数化查询 (防止SQL注入):")
        logger.info("     cursor.execute('SELECT * FROM my_table WHERE id = %s', (my_id,))")
        logger.info("  2. Pandas聚合操作:")
        logger.info("     df.groupby('department').agg({'salary': ['mean', 'sum', 'count']})")
        logger.info("  3. 使用事务 (HiveServer2 支持有限):")
        logger.info("     需要使用支持事务的表存储格式如 ORC")
        logger.info("     使用 tez 或 spark 作为执行引擎以获得更好性能")
        
        # 关闭连接
        conn.close()
        logger.info("连接已关闭")
        
    except Exception as e:
        logger.error(f"执行过程中发生错误: {e}")

if __name__ == "__main__":
    main() 