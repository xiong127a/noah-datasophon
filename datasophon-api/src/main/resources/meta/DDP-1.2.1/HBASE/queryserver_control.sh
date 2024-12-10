#!/bin/bash

# Get the directory of the current script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Calculate the path to the bin directory containing hbase-config.sh and hbase-common.sh
BIN_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)/bin"

# Paths to the scripts
PYTHON_SCRIPT="$SCRIPT_DIR/queryserver.py"
HBASE_CONFIG_SCRIPT="$BIN_DIR/hbase-config.sh"
HBASE_COMMON_SCRIPT="$BIN_DIR/hbase-common.sh"

# Function to display usage information
usage() {
    echo "Usage: $0 {start|stop|status|restart}"
    exit 1
}

# Check if a command is provided
if [ $# -ne 1 ]; then
    usage
fi

# Source the hbase-config.sh and hbase-common.sh scripts
if [ ! -f "$HBASE_CONFIG_SCRIPT" ]; then
    echo "Error: hbase-config.sh not found at $HBASE_CONFIG_SCRIPT"
    exit 1
fi

if [ ! -f "$HBASE_COMMON_SCRIPT" ]; then
    echo "Error: hbase-common.sh not found at $HBASE_COMMON_SCRIPT"
    exit 1
fi

source "$HBASE_CONFIG_SCRIPT"
source "$HBASE_COMMON_SCRIPT"

# Execute the corresponding command in the Python script and capture the exit value
case "$1" in
    start)
        "$PYTHON_SCRIPT" start
        EXIT_STATUS=$?
        ;;
    stop)
        "$PYTHON_SCRIPT" stop
        EXIT_STATUS=$?
        ;;
    status)
        "$PYTHON_SCRIPT" status
        EXIT_STATUS=$?
        ;;
    restart)
        "$PYTHON_SCRIPT" restart
        EXIT_STATUS=$?
        ;;
    *)
        usage
        EXIT_STATUS=1
        ;;
esac

# Exit with the captured exit value
exit $EXIT_STATUS



