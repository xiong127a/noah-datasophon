#!/bin/bash

HUE_HOME="/opt/datasophon/hue"
SUPERVISOR_CMD="${HUE_HOME}/build/env/bin/supervisor"

start_hue() {
    echo "Starting Apache Hue..."
    nohup ${SUPERVISOR_CMD} > /dev/null 2>&1 &
    echo "Apache Hue started successfully."
}

stop_hue() {
    echo "Stopping Apache Hue..."
    pkill -f "supervisor"
    pkill -f "hue runcherrypyserver"
    echo "Apache Hue stopped successfully."
}

is_hue_running() {
    if pgrep -f "supervisor" > /dev/null; then
        return 0
    else
        return 1
    fi
}

case "$1" in
    start)
        start_hue
        ;;
    stop)
        stop_hue
        ;;
    status)
        if is_hue_running; then
            echo "Apache Hue is running."
            exit 0
        else
            echo "Apache Hue is not running."
            exit 1
        fi
        ;;
    *)
        echo "Usage: $0 {start|stop|status}"
        exit 1
        ;;
esac

exit 0
