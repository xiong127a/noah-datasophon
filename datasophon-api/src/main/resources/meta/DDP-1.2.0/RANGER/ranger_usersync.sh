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
    echo "ranger userSync status"
    pid=$(jps | grep -iw UnixAuthenticationService | grep -v grep | awk '{print $1}')
    echo "pid is: $pid"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        echo "$startStop is running"
    else
        echo "$startStop is not running"
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

echo "End $startStop ranger"
