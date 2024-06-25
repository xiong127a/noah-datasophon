#!/bin/bash

operation=$1
node_type=$2

alluxio_start="./bin/alluxio-start.sh"
alluxio_stop="./bin/alluxio-stop.sh"

check_process() {
    if ps -ef | grep -v grep | grep -q "$1"; then
        return 0  # Process exists
    else
        return 1  # Process doesn't exist
    fi
}

start_master() {
    if ! check_process "AlluxioMaster"; then
        $alluxio_start master
    fi
    if ! check_process "AlluxioJobMaster"; then
        $alluxio_start job_master
    fi
    if ! check_process "AlluxioProxy"; then
        $alluxio_start proxy
    fi
}

start_worker() {
    if ! check_process "AlluxioWorker"; then
        $alluxio_start worker SudoMount
    fi
    if ! check_process "AlluxioJobWorker"; then
        $alluxio_start job_worker
    fi
    if ! check_process "AlluxioProxy"; then
        $alluxio_start proxy
    fi
}

stop_master() {
    if check_process "AlluxioProxy"; then
        $alluxio_stop proxy
    fi
    if check_process "AlluxioJobMaster"; then
        $alluxio_stop job_master
    fi
    if check_process "AlluxioMaster"; then
        $alluxio_stop master
    fi
}

stop_worker() {
    if check_process "AlluxioProxy"; then
        $alluxio_stop proxy
    fi
    if check_process "AlluxioJobWorker"; then
        $alluxio_stop job_worker
    fi
    if check_process "AlluxioWorker"; then
        $alluxio_stop worker SudoMount
    fi
}

if [ "$operation" == "start" ]; then
    case "$node_type" in
        "master")
            start_master
            ;;
        "worker")
            start_worker
            ;;
        *)
            echo "Invalid node type. Please use 'master' or 'worker'."
            ;;
    esac
elif [ "$operation" == "stop" ]; then
    case "$node_type" in
        "master")
            stop_master
            ;;
        "worker")
            stop_worker
            ;;
        *)
            echo "Invalid node type. Please use 'master' or 'worker'."
            ;;
    esac
elif [ "$operation" == "status" ]; then
    case "$node_type" in
        "master")
            if check_process "AlluxioMaster"; then
                exit 0
            else
                exit 1
            fi
            ;;
        "worker")
            if check_process "AlluxioWorker"; then
                exit 0
            else
                exit 1
            fi
            ;;
        *)
            echo "Invalid node type. Please use 'master' or 'worker'."
            ;;
    esac
else
    echo "Invalid operation. Please use 'start', 'stop', or 'status'."
fi
