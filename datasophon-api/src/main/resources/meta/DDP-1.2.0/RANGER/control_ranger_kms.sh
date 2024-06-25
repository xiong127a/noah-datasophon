#!/bin/bash

# 获取当前目录
CURRENT_DIR=$(dirname "$0")

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
    if [ -n "$(ls -A ${CURRENT_DIR}/pid)" ]; then
        echo "服务正在运行."
        exit 0
    else
        echo "服务未运行."
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