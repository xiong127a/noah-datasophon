#!/bin/bash

MYSQL_ROOT_PASSWORD="$1"
if [ -z "$MYSQL_ROOT_PASSWORD" ]; then
  echo "No password provided. Using default password."
  MYSQL_ROOT_PASSWORD="123456"
fi
if [ $UID -ne 0 ]; then
  echo Non root user. Please run as root.
  exit 1
fi
if [ -L $0 ]; then
  BASE_DIR=$(dirname $(readlink $0))
else
  BASE_DIR=$(dirname $0)
fi
BASE_PATH=$(
  cd ${BASE_DIR}
  pwd
)
INIT_PATH=$(dirname "${BASE_PATH}")
echo "INIT_PATH: ${INIT_PATH}"
INIT_BIN_PATH=${INIT_PATH}/bin
echo "INIT_BIN_PATH: ${INIT_BIN_PATH}"
INIT_SBIN_PATH=${INIT_PATH}/sbin
echo "INIT_SBIN_PATH: ${INIT_SBIN_PATH}"
PACKAGES_PATH=${INIT_PATH}/packages
echo "PACKAGES_PATH: ${PACKAGES_PATH}"
YAML_PATH=${INIT_PATH}/yaml
echo "YAML_PATH: ${YAML_PATH}"
MYSQL_CONTAINER_TAR_NAME=mysql_5.7.tar

arch=$(uname -m)
if [[ "$arch" != "x86_64" ]]; then
  echo "The architecture is: $arch"
else
  cat >/data/mysql/my.cnf <<EOF
[mysqld]

datadir=/var/lib/mysql
socket=/var/lib/mysql/mysql.sock
symbolic-links=0
log-error=/var/log/mysqld.log
pid-file=/var/run/mysqld/mysqld.pid
skip_ssl
server-id = 1
log-bin=mysql-bin
binlog_format=row
binlog-do-db=financial_lease
binlog-do-db=financial_lease_config

EOF
  NODE_NAME="${NODE_NAME:-$(hostname)}"
  docker load -i ${PACKAGES_PATH}/${MYSQL_CONTAINER_TAR_NAME}
  nerdctl load -i ${PACKAGES_PATH}/${MYSQL_CONTAINER_TAR_NAME} --namespace k8s.io
  echo $MYSQL_ROOT_PASSWORD
  cd ${YAML_PATH} 
  sed "s/kubernetes.io\/hostname: .*/kubernetes.io\/hostname: $NODE_NAME/" \
      mysql-pod-tmp.yaml | \
  sed 's/value: "PASSWORD"/value: "'$MYSQL_ROOT_PASSWORD'"/' > mysql-pod.yaml 
  kubectl apply -f mysql-pod.yaml
fi








