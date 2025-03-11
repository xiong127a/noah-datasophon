#!/bin/bash

# example: sh init-ssh-gen-key.sh

filePath=$1
GROUP="hadoop"
USER="hadoop"
GROUP_ID=2001
USER_ID=2000

# 检查是否以root运行
if [ $(id -u) -ne 0 ]; then
    echo "请以root用户运行此脚本"
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

# 检查组是否存在
if grep -q "^$GROUP" /etc/group; then
    echo "组 $GROUP 已存在"
else
    # 创建组
    groupadd -g $GROUP_ID $GROUP
    if [ $? -eq 0 ]; then
        echo "组 $GROUP 创建成功"
    else
        echo "组 $GROUP 创建失败"
        exit 1
    fi
fi

# 检查用户是否存在
if id "$USER" >/dev/null 2>&1; then
    echo "用户 $USER 已存在"
else
    # 创建用户
    useradd -u $USER_ID -g $GROUP_ID -m $USER
    if [ $? -eq 0 ]; then
        echo "用户 $USER 创建成功"
    else
        echo "用户 $USER 创建失败"
        exit 1
    fi
fi

echo "init-add-hadoop-user.sh."
echo "Done."

