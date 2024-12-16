#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

usage="Usage: control_logstash (start|stop|restart|status)"

if [ $# -ne 1 ]; then
    echo $usage
    exit 1
fi

action=$1

SH_DIR=$(dirname "$0")

export LOG_DIR=$SH_DIR/logs
export PID_DIR=$SH_DIR/pid

log=$LOG_DIR/logstash.log
pid=$PID_DIR/logstash.pid

echo "Log file path: $log"
echo "PID file path: $pid"

# Create directories if they don't exist
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi
if [ ! -d "$PID_DIR" ]; then
    mkdir -p "$PID_DIR"
fi

# Start Logstash
start() {
    if [ -f "$pid" ]; then
        echo "Logstash is already running."
        exit 1
    fi
    echo "Starting Logstash..."
    nohup "$SH_DIR/bin/logstash" > "$log" 2>&1 &
    echo $! > "$pid"  # Save the PID of the Logstash process
    echo "Logstash started."
}

# Stop Logstash
stop() {
    if [ -f "$pid" ]; then
        TARGET_PID=$(cat "$pid")
        if kill -0 "$TARGET_PID" > /dev/null 2>&1; then
            echo "Stopping Logstash..."
            kill "$TARGET_PID"
            sleep 3s
            if kill -0 "$TARGET_PID" > /dev/null 2>&1; then
                echo "Logstash did not stop gracefully after 3 seconds: killing with kill -9"
                kill -9 "$TARGET_PID"
            fi
        else
            echo "Logstash is not running, but pid file exists. Removing stale pid file."
        fi
        rm -f "$pid"
        echo "Logstash stopped."
    else
        echo "Logstash is not running."
    fi
}

# Check the status of Logstash
status() {
    if [ -f "$pid" ]; then
        TARGET_PID=$(cat "$pid")
        if kill -0 "$TARGET_PID" > /dev/null 2>&1; then
            echo "Logstash is running."
        else
            echo "Logstash is not running, but pid file exists."
            exit 1
        fi
    else
        echo "Logstash is not running. No pid file found."
        exit 1
    fi
}

# Restart Logstash
restart() {
    stop
    sleep 2s
    start
}

# Handle the passed arguments
case $action in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    *)
        echo $usage
        exit 1
        ;;
esac

echo "End $action Logstash."
