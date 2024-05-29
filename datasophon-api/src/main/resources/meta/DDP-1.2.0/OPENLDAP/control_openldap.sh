#!/bin/bash

start_slapd() {
    systemctl start slapd
}

stop_slapd() {
    systemctl stop slapd
}

status_slapd() {
    if systemctl is-active --quiet slapd; then
        echo "OpenLDAP is running."
        exit 0
    else
        echo "OpenLDAP is not running."
        exit 1
    fi
}

case "$1" in
    start)
        start_slapd
        ;;
    stop)
        stop_slapd
        ;;
    status)
        status_slapd
        ;;
    *)
        echo "Usage: $0 {start|stop|status}"
        exit 1
        ;;
esac

exit 0
