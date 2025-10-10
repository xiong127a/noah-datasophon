#!/bin/bash

# 定义启动和停止命令
START_REDIS_MASTER="/opt/datasophon/redissentinel/redis-server /opt/datasophon/redissentinel/conf/redis-master.conf"
START_REDIS_SLAVE="/opt/datasophon/redissentinel/redis-server /opt/datasophon/redissentinel/conf/redis-slave.conf"
START_SENTINEL="/opt/datasophon/redissentinel/redis-server /opt/datasophon/redissentinel/conf/sentinel.conf --sentinel"
STOP_REDIS_MASTER="/opt/datasophon/redissentinel/redis-cli -p ${redisSentinelMasterPort} shutdown"
STOP_REDIS_SLAVE="/opt/datasophon/redissentinel/redis-cli -p ${redisSentinelSlavePort} shutdown"
STOP_SENTINEL="/opt/datasophon/redissentinel/redis-cli -p ${sentinelServerPort} shutdown"
STATUS_REDIS_MASTER="/opt/datasophon/redissentinel/redis-cli -p ${redisSentinelMasterPort} ping"
STATUS_REDIS_SLAVE="/opt/datasophon/redissentinel/redis-cli -p ${redisSentinelSlavePort} ping"
STATUS_SENTINEL="/opt/datasophon/redissentinel/redis-cli -p ${sentinelServerPort} ping"

# 启动master
start_master() {
    echo "Starting Redis master..."
    $START_REDIS_MASTER
}

# 启动sentinel
start_sentinel() {
    echo "Starting Redis sentinel..."
    $START_SENTINEL
}

# 启动slave
start_slave() {
    echo "Starting Redis slave..."
    $START_REDIS_SLAVE
}

# 停止master
stop_master() {
    echo "Stopping Redis master..."
    $STOP_REDIS_MASTER
}

# 停止sentinel
stop_sentinel() {
    echo "Stopping Redis sentinel..."
    $STOP_SENTINEL
}

# 停止slave
stop_slave() {
    echo "Stopping Redis slave..."
    $STOP_REDIS_SLAVE
}

# 检查状态并根据返回值决定退出码
check_status() {
    echo "Checking Redis status..."
    status=$($1)  # 使用传递的命令获取状态
    if [ "$status" == "PONG" ]; then
        echo "Redis is running."
        exit 0
    else
        echo "Redis is not running."
        exit 1
    fi
}

# 执行操作
case $1 in
    start)
        case $2 in
            master)
                start_master
                ;;
            slave)
                start_slave
                ;;
            sentinel)
                start_sentinel
                ;;
            *)
                echo "Invalid second parameter. Usage: $0 start {server|slave|sentinel}"
                exit 1
                ;;
        esac
        ;;
    stop)
        case $2 in
            master)
                stop_master
                ;;
            slave)
                stop_slave
                ;;
            sentinel)
                stop_sentinel
                ;;
            *)
                echo "Invalid second parameter. Usage: $0 stop {server|slave|sentinel}"
                exit 1
                ;;
        esac
        ;;
    status)
        case $2 in
            master)
                check_status "$STATUS_REDIS_MASTER"
                ;;
            slave)
                check_status "$STATUS_REDIS_SLAVE"
                ;;
            sentinel)
                check_status "$STATUS_SENTINEL"
                ;;
            *)
                echo "Invalid second parameter. Usage: $0 status {server|slave|sentinel}"
                exit 1
                ;;
        esac
        ;;
    *)
        echo "Invalid first parameter. Usage: $0 {start|stop|status} {server|slave|sentinel}"
        exit 1
        ;;
esac
