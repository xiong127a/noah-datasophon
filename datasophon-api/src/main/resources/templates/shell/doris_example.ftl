#!/bin/bash

# Doris Shell命令示例脚本
# 演示如何使用MySQL命令行客户端连接Doris并执行常见操作

# =======================
# 连接信息
# =======================
HOST="${data.getBasicInfoValue('host', 'localhost')}"
PORT="${data.getBasicInfoValue('fePort', '9030')}"
USER="${data.getSecurityInfoValue('username', 'root')}"
PASSWORD="${data.getSecurityInfoValue('password', '')}"
DATABASE="example_db"

# =======================
# 颜色定义
# =======================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =======================
# 辅助函数
# =======================
print_section() {
    echo -e "\n${BLUE}=== $1 ===${NC}"
}

print_success() {
    echo -e "${GREEN}$1${NC}"
}

print_error() {
    echo -e "${RED}$1${NC}"
}

print_warning() {
    echo -e "${YELLOW}$1${NC}"
}

execute_sql() {
    local sql="$1"
    local message="$2"
    
    # 构建MySQL命令，根据是否有密码来调整
    if [ -z "$PASSWORD" ]; then
        MYSQL_CMD="mysql -h$HOST -P$PORT -u$USER"
    else
        MYSQL_CMD="mysql -h$HOST -P$PORT -u$USER -p$PASSWORD"
    fi
    
    if [ -n "$DATABASE" ]; then
        MYSQL_CMD="$MYSQL_CMD $DATABASE"
    fi
    
    # 执行SQL并输出结果
    echo -e "${YELLOW}执行: $message${NC}"
    echo -e "${BLUE}SQL: $sql${NC}"
    
    result=$(echo "$sql" | $MYSQL_CMD 2>&1)
    
    if [ $? -eq 0 ]; then
        if [ -n "$result" ]; then
            echo -e "${GREEN}执行成功:${NC}\n$result"
        else
            echo -e "${GREEN}执行成功${NC}"
        fi
        return 0
    else
        echo -e "${RED}执行失败: $result${NC}"
        return 1
    fi
}

# =======================
# 主程序
# =======================
main() {
    print_section "Doris Shell命令示例"
    echo "主机: $HOST"
    echo "端口: $PORT"
    echo "用户: $USER"
    
    # 创建数据库
    print_section "创建数据库"
    execute_sql "DROP DATABASE IF EXISTS $DATABASE; CREATE DATABASE $DATABASE;" "创建数据库"
    
    # 切换到新创建的数据库
    DATABASE="example_db"
    
    # 创建示例表
    print_section "创建示例表"
    create_table_sql="
    CREATE TABLE IF NOT EXISTS example_table (
        id INT,
        name VARCHAR(50),
        value DOUBLE,
        create_time DATETIME
    ) ENGINE=OLAP
    DUPLICATE KEY(id)
    COMMENT 'Doris示例表'
    DISTRIBUTED BY HASH(id) BUCKETS 3
    "
    execute_sql "$create_table_sql" "创建示例表"
    
    # 插入数据
    print_section "插入数据"
    current_time=$(date "+%Y-%m-%d %H:%M:%S")
    insert_sql="
    INSERT INTO example_table VALUES
    (1, '测试1', 10.5, '$current_time'),
    (2, '测试2', 20.5, '$current_time'),
    (3, '测试3', 30.5, '$current_time')
    "
    execute_sql "$insert_sql" "插入数据"
    
    # 查询数据
    print_section "查询数据"
    execute_sql "SELECT * FROM example_table ORDER BY id;" "查询数据"
    
    # 创建分区表
    print_section "创建分区表"
    create_partition_table_sql="
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
    "
    execute_sql "$create_partition_table_sql" "创建分区表"
    
    # 插入分区数据
    print_section "插入分区数据"
    
    # 这里使用循环插入多条数据到不同分区
    for day in "2023-01-01" "2023-01-02" "2023-01-03"; do
        for hour in 0 6 12 18; do
            for type in "click" "view" "purchase"; do
                # 随机生成一个100-999之间的数
                count=$((RANDOM % 900 + 100))
                insert_partition_sql="
                INSERT INTO partition_example VALUES
                ('$day', $hour, '$type', $count)
                "
                execute_sql "$insert_partition_sql" "插入分区数据: $day, $hour, $type"
            done
        done
    done
    
    # 查询分区数据
    print_section "查询特定分区数据"
    execute_sql "
    SELECT event_day, event_hour, event_type, event_count
    FROM partition_example
    WHERE event_day = '2023-01-02'
    ORDER BY event_hour, event_type;
    " "查询2023-01-02分区数据"
    
    # 添加新分区
    print_section "添加新分区"
    execute_sql "
    ALTER TABLE partition_example 
    ADD PARTITION p20230104 VALUES [('2023-01-04'), ('2023-01-05'))
    " "添加新分区p20230104"
    
    # 查看分区信息
    print_section "查看分区信息"
    execute_sql "SHOW PARTITIONS FROM partition_example;" "查看分区信息"
    
    # Doris常用管理命令
    print_section "Doris常用管理命令示例"
    
    # 查看所有数据库
    execute_sql "SHOW DATABASES;" "查看所有数据库"
    
    # 查看表结构
    execute_sql "DESC example_table;" "查看example_table表结构"
    
    # 查看BE节点
    execute_sql "SHOW BACKENDS;" "查看BE节点"
    
    # 查看FE节点
    execute_sql "SHOW FRONTENDS;" "查看FE节点"
    
    # 查看集群信息
    execute_sql "SHOW CLUSTER;" "查看集群信息"
    
    # 查看所有表
    execute_sql "SHOW TABLES;" "查看所有表"
    
    # 删除测试表
    print_section "清理测试数据"
    execute_sql "DROP TABLE IF EXISTS example_table;" "删除example_table"
    execute_sql "DROP TABLE IF EXISTS partition_example;" "删除partition_example"
    
    # 删除数据库
    execute_sql "DROP DATABASE IF EXISTS $DATABASE;" "删除数据库$DATABASE"
    
    print_section "示例完成"
    print_success "所有命令示例已执行完毕"
}

# 执行主程序
main 