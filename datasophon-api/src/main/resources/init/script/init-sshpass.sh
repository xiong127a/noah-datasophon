#!/bin/bash

# 定义源码包的路径
SOURCE_DIR="/data/datasophon-init/packages"
SOURCE_FILE="sshpass-1.10.tar.gz"
SOURCE_PATH="$SOURCE_DIR/$SOURCE_FILE"
INSTALL_DIR="/opt/sshpass"


# 检查源码包是否存在
if [ ! -f "$SOURCE_PATH" ]; then
  echo "错误: 找不到源代码包 $SOURCE_PATH"
  exit 1
fi

# 创建安装目录
if [ ! -d "$INSTALL_DIR" ]; then
  echo "创建安装目录 $INSTALL_DIR"
  sudo mkdir -p "$INSTALL_DIR" || { echo "创建目录失败"; exit 1; }
fi

# 进入存放源码包的目录
cd "$SOURCE_DIR" || { echo "无法进入目录 $SOURCE_DIR"; exit 1; }

# 解压源码包
echo "解压源码包..."
tar xvzf "$SOURCE_FILE" || { echo "解压失败"; exit 1; }

# 进入解压后的目录
cd sshpass-1.10 || { echo "无法进入源码目录 sshpass-1.10"; exit 1; }

# 配置源码，指定安装目录
echo "配置源码..."
./configure --prefix="$INSTALL_DIR" || { echo "配置失败"; exit 1; }

# 编译源码
echo "开始编译..."
make || { echo "编译失败"; exit 1; }

# 安装
echo "安装到 $INSTALL_DIR..."
sudo make install || { echo "安装失败"; exit 1; }

# 检查 /usr/bin/sshpass 是否存在，若存在则备份
if [ -e "/usr/bin/sshpass" ]; then
    echo "/usr/bin/sshpass 已存在，进行备份..."
    sudo mv /usr/bin/sshpass "/usr/bin/sshpass_$(date +%Y%m%d%H%M%S)bak"
    echo "现有文件已备份为 /usr/bin/sshpass_$(date +%Y%m%d%H%M%S)bak"
fi

# 创建软链接到 /usr/bin
echo "创建软链接到 /usr/bin..."
sudo ln -s "$INSTALL_DIR/bin/sshpass" /usr/bin/sshpass || { echo "创建软链接失败"; exit 1; }

# 完成
echo "sshpass 安装完成！安装目录：$INSTALL_DIR，软链接已创建到 /usr/bin/sshpass"

