#!/bin/bash
start_time=$(date +%s)
# 设置源码包和安装路径
SOURCE_DIR="/data/datasophon-init/packages"
OPENSSL_TAR="openssl-1.1.1.tar.gz"
OPENSSL_DIR="/opt/openssl-1.1.1"

# 检查并安装所需的依赖包
echo "检查并安装编译 OpenSSL 所需的依赖包..."
sudo yum install -y gcc make perl zlib-devel

# 检查依赖包是否成功安装
for package in gcc make perl zlib-devel; do
    rpm -q $package > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "错误：依赖包 $package 安装失败。请检查安装日志。"
        exit 1
    fi
done

# 检查 OpenSSL 是否已安装，并获取其版本
installed_version=$(openssl version 2>/dev/null)

if [ $? -eq 0 ]; then
    # 获取当前 OpenSSL 版本的数字部分
    current_version=$(echo "$installed_version" | awk '{print $2}')
    
    # 比较版本号
    if [[ $(echo -e "$current_version\n1.1.1" | sort -V | head -n1) == "1.1.1" ]]; then
        echo "OpenSSL 已安装且版本大于或等于 1.1.1，跳过安装。当前版本：$current_version"
        exit 0
    else
        echo "OpenSSL 版本小于 1.1.1，继续安装新的版本。当前版本：$current_version"
    fi
else
    echo "OpenSSL 未安装，开始安装。"
fi

# 检查源码包是否存在
if [ ! -f "${SOURCE_DIR}/${OPENSSL_TAR}" ]; then
    echo "错误：找不到 OpenSSL 源码包 ${SOURCE_DIR}/${OPENSSL_TAR}"
    exit 1
fi

# 解压源码包
cd ${SOURCE_DIR}
tar -zxvf ${OPENSSL_TAR}
if [ $? -ne 0 ]; then
    echo "解压 OpenSSL 源码包失败。"
    exit 1
fi

cd openssl-1.1.1
mkdir -p ${OPENSSL_DIR}

# 编译并安装 OpenSSL
./config --prefix=${OPENSSL_DIR}
if [ $? -ne 0 ]; then
    echo "配置 OpenSSL 失败。"
    exit 1
fi

make
if [ $? -ne 0 ]; then
    echo "编译 OpenSSL 失败。"
    exit 1
fi

make install
if [ $? -ne 0 ]; then
    echo "安装 OpenSSL 失败。"
    exit 1
fi

# 添加环境变量
if ! grep -q "export PATH=${OPENSSL_DIR}/bin" /etc/profile; then
    echo 'export PATH=${OPENSSL_DIR}/bin:$PATH' | sudo tee -a /etc/profile
fi

if ! grep -q "export LD_LIBRARY_PATH=${OPENSSL_DIR}/lib" /etc/profile; then
    echo 'export LD_LIBRARY_PATH=${OPENSSL_DIR}/lib:$LD_LIBRARY_PATH' | sudo tee -a /etc/profile
fi

# 更新环境变量
source /etc/profile

# 检查安装的 OpenSSL 版本
openssl version

# 创建软连接
echo "创建软连接到系统路径..."
if [ ! -e "/usr/lib64/libssl.so.1.1" ]; then
    sudo ln -sf ${OPENSSL_DIR}/lib/libssl.so.1.1 /usr/lib64/libssl.so.1.1
fi

if [ ! -e "/usr/lib64/libcrypto.so.1.1" ]; then
    sudo ln -sf ${OPENSSL_DIR}/lib/libcrypto.so.1.1 /usr/lib64/libcrypto.so.1.1
fi

# 备份并替换系统 OpenSSL 软连接
echo "替换系统中现有的 OpenSSL 软连接..."

# 备份原有的 OpenSSL
if [ -e "/usr/bin/openssl" ]; then
    sudo mv /usr/bin/openssl /usr/bin/openssl_$(date +%Y%m%d%H%M%S)bak
fi

if [ -e "/usr/lib64/openssl" ]; then
    sudo mv /usr/lib64/openssl /usr/lib64/openssl_$(date +%Y%m%d%H%M%S)bak
fi

# 创建新的软连接
sudo ln -sf ${OPENSSL_DIR}/bin/openssl /usr/bin/openssl
sudo ln -sf ${OPENSSL_DIR}/lib /usr/lib64/openssl

# 验证 OpenSSL 版本是否已经更新
openssl version
end_time=$(date +%s)
execution_time=$((end_time - start_time))
echo "脚本执行时长: $execution_time 秒"
echo "OpenSSL 安装和软连接配置完成！"
