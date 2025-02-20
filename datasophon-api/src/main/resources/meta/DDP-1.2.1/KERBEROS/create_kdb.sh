#!/bin/bash
DB_PATH="/var/kerberos/krb5kdc/principal"

if [ ! -f "$DB_PATH" ]; then
    kdb5_util create -s <<EOF
admin
admin
EOF
fi