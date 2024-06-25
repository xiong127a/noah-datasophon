#!/bin/bash

# 获取脚本当前目录
current_path=`dirname $0`

# Minio 启动命令
start_command="sh $current_path/bin/start.sh"

# Minio 停止命令
stop_command="sh $current_path/bin/stop.sh"

# Minio 状态命令
status_command="sh $current_path/bin/status.sh"

# 检查Minio服务状态
function check_minio_status() {
    if $status_command | grep -q "active (running)"; then
        echo "Minio is running."
        return 0
    else
        echo "Minio is not running."
        return 1
    fi
}

# 启动Minio
function start_minio() {
    check_minio_status
    if [ $? -eq 0 ]; then
        echo "Minio is already running."
        exit 1
    else
        $start_command
        echo "Starting Minio..."
    fi
}

# 停止Minio
function stop_minio() {
    $stop_command
    echo "Stopping Minio..."
}

# 重启Minio
function restart_minio() {
    echo "Stopping Minio..."
    stop_minio
    echo "Starting Minio..."
    start_minio
    check_minio_status
}

# 显示Minio状态
function show_minio_status() {
    $status_command
}

# 主函数，根据传入的命令执行对应操作
command="$1"
case $command in
"start")
    start_minio
    ;;
"stop")
    stop_minio
    ;;
"restart")
    restart_minio
    ;;
"status")
    show_minio_status
    ;;
*)
    echo "Usage: $0 {start|stop|status}"
    exit 1
    ;;
esac
