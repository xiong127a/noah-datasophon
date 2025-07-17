#!/bin/bash

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

if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
else
    echo "无法识别操作系统，无法继续安装"
    exit 1
fi


INIT_PATH=$(dirname "${BASE_PATH}")
echo "INIT_PATH: ${INIT_PATH}"
INIT_BIN_PATH=${INIT_PATH}/bin
echo "INIT_BIN_PATH: ${INIT_BIN_PATH}"
INIT_SBIN_PATH=${INIT_PATH}/sbin
echo "INIT_SBIN_PATH: ${INIT_SBIN_PATH}"
PACKAGES_PATH=${INIT_PATH}/packages
echo "PACKAGES_PATH: ${PACKAGES_PATH}"

SSH_FOLDER_NAME=ssh
SSH_TAR_NAME=ssh.tar.gz


if [ "$OS" == "ubuntu" ] || [ "$OS" == "debian" ]; then
    ssh_installed=$(dpkg-query -W --showformat='${Status}' openssh-server 2>/dev/null | grep "install ok installed")
    if [ "" == "$ssh_installed" ]; then
        echo "OpenSSH server not installed. Installing..."
        sudo apt install -y openssh-server openssh-client
    else
        echo "OpenSSH already installed."
    fi

    # 确保 SSH 服务已启用并启动
    echo "Enabling and starting SSH service..."
    sudo systemctl enable ssh
    sudo systemctl start ssh
    sudo systemctl status ssh
else 
    ssh_rpm=$(rpm -qa | grep openssh)
    if [[ "$?" == "0" ]]; then
      echo "ssh exists"
    else
      yum -y install openssh
      echo "ssh-install finished."
    fi
fi

echo "init-sshpackage.sh finished."
echo "Done."
