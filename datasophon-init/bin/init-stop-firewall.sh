#!/bin/bash
# example: sh init-stop-firewall.sh
if [ $UID -ne 0 ]; then
  echo Non root user. Please run as root.
  exit 1
fi
if [ -L $0 ]
then
    BASE_DIR=`dirname $(readlink $0)`
else
    BASE_DIR=`dirname $0`
fi
BASE_PATH=$(cd ${BASE_DIR}; pwd)
INIT_PATH=$(dirname "${BASE_PATH}")
echo "INIT_PATH: ${INIT_PATH}"
DATASOPHON_PATH=$(dirname "${INIT_PATH}")
echo "DATASOPHON_PATH: ${DATASOPHON_PATH}"
INIT_BIN_PATH=${INIT_PATH}/bin
echo "INIT_BIN_PATH: ${INIT_BIN_PATH}"
INIT_SBIN_PATH=${INIT_PATH}/sbin
echo "INIT_SBIN_PATH: ${INIT_SBIN_PATH}"
PACKAGES_PATH=${INIT_PATH}/packages
echo "PACKAGES_PATH: ${PACKAGES_PATH}"
# 检查 firewalld (CentOS/RHEL)
if command -v firewall-cmd &>/dev/null; then
    FIREWALL_STATUS=$(firewall-cmd --state)
    if [ "${FIREWALL_STATUS}" == "running" ]; then
        echo "Closing firewall (firewalld)."
        systemctl stop firewalld.service
        systemctl disable firewalld.service
        echo "Firewall closed."
    else
        echo "Firewall (firewalld) already closed."
    fi

# 检查 ufw (Ubuntu/Debian)
elif command -v ufw &>/dev/null; then
    FIREWALL_STATUS=$(ufw status | grep -o 'active')
    if [ "${FIREWALL_STATUS}" == "active" ]; then
        echo "Closing firewall (ufw)."
        ufw disable
        echo "Firewall closed."
    else
        echo "Firewall (ufw) already closed."
    fi

else
    echo "No supported firewall found (either firewalld or ufw)."
fi

echo "init-stop-firewall.sh finished."
echo "Done."
