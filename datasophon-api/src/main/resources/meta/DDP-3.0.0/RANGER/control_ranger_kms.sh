#!/bin/bash

# 获取当前目录
CURRENT_DIR=$(dirname "$0")

# PID文件路径
PID_FILE="${CURRENT_DIR}/pid/rangerkms.pid"

# 启动服务
start_service() {
    echo "正在启动服务..."
    ${CURRENT_DIR}/ranger-kms start
    sleep 2  # 等待一段时间确保服务已经启动
}

# 停止服务
stop_service() {
    echo "正在停止服务..."
    ${CURRENT_DIR}/ranger-kms stop
    sleep 2  # 等待一段时间确保服务已经停止
}

# 检测服务状态
check_service_status() {
    if [ -f "$PID_FILE" ] && [ -s "$PID_FILE" ]; then
        # 读取 PID 文件中的进程 ID
        PID=$(cat "$PID_FILE")

        # 使用 kill -0 来检查进程是否存在
        if kill -0 $PID > /dev/null 2>&1; then
            echo "服务正在运行."
            exit 0
        else
            echo "PID 文件存在，但进程不存在，服务未运行."
            exit 1
        fi
    else
        echo "服务未运行，PID 文件不存在或为空."
        exit 1
    fi
}

case "$1" in
    start)
        start_service
        ;;
    stop)
        stop_service
        ;;
    status)
        check_service_status
        ;;
    *)
        echo "使用方法: $0 {start|stop|status}"
        exit 1
esac

exit 0
