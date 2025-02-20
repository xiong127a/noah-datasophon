#!/bin/bash

PRINCIPAL=$1
KEYTAB_FILE_PATH=$2

# List all principals
list_princ_cmd="kadmin.local -q 'listprincs'"
exec_out=$(eval $list_princ_cmd)

# Check if principal exists and add if not
if ! echo "$exec_out" | grep -q "$PRINCIPAL"; then
    add_princ_cmd="kadmin.local -q 'addprinc -randkey ${PRINCIPAL}'"
    echo "Running command: $add_princ_cmd"
    eval $add_princ_cmd
fi

# Create keytab file path if it doesn't exist
if [ ! -f "$KEYTAB_FILE_PATH" ]; then
    mkdir -p "$(dirname "$KEYTAB_FILE_PATH")"
fi

# Generate keytab file
keytab_cmd="kadmin.local -q 'xst -k ${KEYTAB_FILE_PATH} ${PRINCIPAL}'"
echo "Running command: $keytab_cmd"
eval $keytab_cmd
