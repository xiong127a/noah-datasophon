#!/bin/bash

# 获取脚本当前目录
current_path=$(dirname "$0")

# 使用说明
usage="Usage: $0 {start|stop|status|restart}"

start(){
    echo "ranger userSync start"
    sh "$current_path/ranger-usersync" start
    if [ $? -eq 0 ]; then
        echo "ranger userSync start success"
    else
        echo "ranger userSync start failed"
        exit 1
    fi
}

stop(){
    echo "ranger userSync stop"
    sh "$current_path/ranger-usersync" stop
    if [ $? -eq 0 ]; then
        echo "ranger userSync stop success"
    else
        echo "ranger userSync stop failed"
        exit 1
    fi
}

status(){
    process_name="UnixAuthenticationService"

    # 使用 pgrep 命令检测进程是否存在
    pgrep -f "$process_name" > /dev/null

    if [ $? -eq 0 ]; then
        echo "进程 $process_name 存在"
        exit 0
    else
        echo "进程 $process_name 不存在"
        exit 1
    fi
}

restart(){
    echo "ranger userSync restart"
    sh "$current_path/ranger-usersync" restart
    if [ $? -eq 0 ]; then
        echo "ranger userSync restart success"
    else
        echo "ranger userSync restart failed"
        exit 1
    fi
}

# 处理参数
startStop=$1

case $startStop in
    start)
        start
        ;;
    stop)
        stop
        ;;
    status)
        status
        ;;
    restart)
        restart
        ;;
    *)
        echo "$usage"
        exit 1
        ;;
esac

echo "End $startStop ranger userSync"
