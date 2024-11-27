#!/bin/bash

# 配置包路径和安装目录
PACKAGE_DIR="/data/datasophon-init/packages"
INSTALL_DIR="/usr/local"
JDK_ARCH=""

# 获取系统架构
ARCH=$(uname -m)

# 获取当前用户
USER=$(whoami)

# 检查是否是 root 用户
if [[ "$USER" != "root" ]]; then
    echo "This script must be run as root. Please use sudo."
    exit 1
fi

# 根据系统架构设置相应的 JDK 安装包名称
if [[ "$ARCH" == "x86_64" ]]; then
    JDK_ARCH="jdk-8u333-linux-x64.tar.gz"
elif [[ "$ARCH" == "aarch64" ]]; then
    JDK_ARCH="jdk-8u333-linux-aarch64.tar.gz"
else
    echo "Unsupported architecture: $ARCH"
    exit 1
fi

# 检查 JDK 安装包是否存在
PACKAGE_PATH="$PACKAGE_DIR/$JDK_ARCH"
if [[ ! -f "$PACKAGE_PATH" ]]; then
    echo "JDK package $JDK_ARCH not found in $PACKAGE_DIR"
    exit 1
fi



# 清理已有的 JAVA_HOME 环境变量
echo "Cleaning up old JAVA_HOME environment variable..."

# 备份 /etc/profile 文件
cp /etc/profile /etc/profile.bak

# 删除旧的 JAVA_HOME 配置

sed -i '/JAVA_HOME/d' /etc/profile
sed -i '/JRE_HOME/d' /etc/profile

# 解压 JDK 包到 /usr/local 目录
echo "Installing JDK from $PACKAGE_PATH to $INSTALL_DIR..."
tar -zxf "$PACKAGE_PATH" -C "$INSTALL_DIR"

# 获取解压后的 JDK 目录（假设 JDK 包名结构是固定的）
JDK_DIR=$(tar -tzf "$PACKAGE_PATH" | head -n 1 | cut -f1 -d"/")

# 确保解压路径存在并且是 JDK 文件夹
if [[ ! -d "$INSTALL_DIR/$JDK_DIR" ]]; then
    echo "Error: JDK directory $INSTALL_DIR/$JDK_DIR not found."
    exit 1
fi

# 设置 JAVA_HOME, JRE_HOME 和 PATH 环境变量
echo "Setting up JDK environment variables..."

# 设置新的 JAVA_HOME
echo "export JAVA_HOME=$INSTALL_DIR/$JDK_DIR" >> /etc/profile
echo "export JRE_HOME=$INSTALL_DIR/$JDK_DIR/jre" >> /etc/profile

# 刷新环境变量
source /etc/profile
rm /usr/bin/java
echo "export PATH=$JAVA_HOME/bin:$(echo $PATH | tr ':' '\n' | sort | uniq | tr '\n' ':')" >> /etc/profile
# 刷新环境变量
source /etc/profile


# 输出新的 PATH
echo "The updated PATH is:"
echo "$PATH"

# 验证 JDK 安装是否成功
echo "Verifying JDK installation..."
java -version
if [[ $? -eq 0 ]]; then
    echo "JDK installation successful!"
else
    echo "JDK installation failed."
    exit 1
fi


echo "Installation complete. Please ensure the new environment variables are loaded."

