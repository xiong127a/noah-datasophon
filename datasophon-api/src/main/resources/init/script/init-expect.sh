#!/bin/bash

# example: sh init-expect.sh
. /etc/os-release   # 使用 . 代替 source，确保兼容性

# 检查是否为 root 用户
if [ "$UID" -ne 0 ]; then
  echo "Non root user. Please run as root."
  exit 1
fi

# 获取脚本的基础路径
if [ -L "$0" ]; then
  BASE_DIR=$(dirname $(readlink -f "$0"))
else
  BASE_DIR=$(dirname "$0")
fi
BASE_PATH=$(cd "$BASE_DIR" && pwd)

INIT_PATH=$(dirname "${BASE_PATH}")
echo "INIT_PATH: ${INIT_PATH}"
INIT_BIN_PATH=${INIT_PATH}/bin
echo "INIT_BIN_PATH: ${INIT_BIN_PATH}"
INIT_SBIN_PATH=${INIT_PATH}/sbin
echo "INIT_SBIN_PATH: ${INIT_SBIN_PATH}"
PACKAGES_PATH=${INIT_PATH}/packages
echo "PACKAGES_PATH: ${PACKAGES_PATH}"

# 安装 expect 软件包
if [[ "$ID" == "ubuntu" || "$ID" == "debian" ]]; then
  apt-get -y install expect
else
  yum -y install expect
fi

echo "init-expect.sh finished."
echo "Done."
