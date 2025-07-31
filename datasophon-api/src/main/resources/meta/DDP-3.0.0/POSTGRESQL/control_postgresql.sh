#!/bin/bash

PG_CTL=/opt/datasophon/postgresql-16.1/bin/pg_ctl
DATA_DIR=/opt/datasophon/postgresql-16.1/data
LOG_FILE=logfile

start_postgres() {
    echo "Starting PostgreSQL..."
    $PG_CTL -D $DATA_DIR -l $LOG_FILE start
}

stop_postgres() {
    echo "Stopping PostgreSQL..."
    $PG_CTL -D $DATA_DIR -l $LOG_FILE stop
}

check_status() {
    echo "Checking PostgreSQL status..."
    $PG_CTL -D $DATA_DIR -l $LOG_FILE status
    status_code=$?

    if [ $status_code -eq 0 ]; then
        echo "PostgreSQL is running."
        exit 0
    else
        echo "PostgreSQL is not running."
        exit 1
    fi
}

case "$1" in
    start)
        start_postgres
        ;;
    stop)
        stop_postgres
        ;;
    status)
        check_status
        ;;
    *)
        echo "Usage: $0 {start|stop|status}"
        exit 1
        ;;
esac

exit 0