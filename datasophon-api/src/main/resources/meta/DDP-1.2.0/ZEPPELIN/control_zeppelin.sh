#!/bin/bash

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ZEPPELIN_HOME=$SCRIPT_DIR  # 当前目录即为 Zeppelin 安装目录
ZEPPELIN_DAEMON=$ZEPPELIN_HOME/bin/zeppelin-daemon.sh

start_zeppelin() {
    echo "Starting Zeppelin..."
    $ZEPPELIN_DAEMON start
}

stop_zeppelin() {
    echo "Stopping Zeppelin..."
    $ZEPPELIN_DAEMON stop
}

check_zeppelin_status() {
    echo "Checking Zeppelin status..."
    $ZEPPELIN_DAEMON status
    if [ $? -eq 0 ]; then
        echo "Zeppelin is running."
        exit 0
    else
        echo "Zeppelin is not running."
        exit 1
    fi
}

case "$1" in
    start)
        start_zeppelin
        ;;
    stop)
        stop_zeppelin
        ;;
    restart)
        stop_zeppelin
        sleep 5  # 等待一些时间确保Zeppelin完全停止
        start_zeppelin
        ;;
    status)
        check_zeppelin_status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac

exit 0
