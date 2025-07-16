# =================================================================
#  阶段一：工作台 (Builder) - 使用CentOS 7作为基础
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-11.0.21-openjdk AS builder

# 设置工作目录
WORKDIR /opt/datasophon

# 设置基础URL环境变量
ENV BASE_URL=http://192.168.1.30

# --- 在单个RUN指令中完成所有下载、解压和清理操作 ---
RUN \
    # 下载StarRocks软件包
    echo "====> 下载StarRocks软件包..." && \
    curl -L -s -o starRocks-3.3.2.tar.gz ${BASE_URL}/BDP/packages/1.2.1/starRocks-3.3.2.tar.gz && \
    \
    # 解压
    echo "====> 解压软件包..." && \
    tar -zxf starRocks-3.3.2.tar.gz && \
    \
    # 删除StarRocks根目录中的JDK 11
    echo "====> 删除StarRocks根目录中的JDK 11..." && \
    rm -rf /opt/datasophon/starRocks-3.3.2/jdk* && \
    \
    # 删除原始压缩包
    echo "====> 清理压缩包..." && \
    rm -f starRocks-3.3.2.tar.gz && \
    echo "====> 构建阶段清理完成."

# =================================================================
#  阶段二：最终产物 (Final Image) - 基于CentOS 7
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-11.0.21-openjdk

LABEL maintainer="DataSophon Team" \
      description="StarRocks image with MySQL client and netcat tools"

WORKDIR /opt/datasophon

# --- 精确拷贝，只从builder阶段复制清理后的目录 ---
COPY --from=builder /opt/datasophon/starRocks-3.3.2 /opt/datasophon/starRocks-3.3.2

# --- 安装依赖、设置用户、权限和软链接 ---
RUN \
    # 更新yum源
    echo "====> 更新yum源..." && \
    yum clean all && \
    yum makecache && \
    \
    # 安装必要工具
    echo "====> 安装必要工具..." && \
    yum install -y yum-utils nc && \
    \
    # 配置MySQL yum源并安装MySQL客户端
    echo "====> 配置MySQL yum源..." && \
    rpm -Uvh https://repo.mysql.com/mysql80-community-release-el7-3.noarch.rpm && \
    rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-mysql && \
    yum-config-manager --disable mysql80-community && \
    yum-config-manager --enable mysql57-community && \
    echo "====> 安装MySQL客户端..." && \
    yum install -y mysql-community-client --nogpgcheck && \
    \
    # 清理yum缓存
    echo "====> 清理yum缓存..." && \
    yum clean all && \
    rm -rf /var/cache/yum && \
    \
    # 创建软链接和设置权限
    echo "====> 正在创建软链接..." && \
    ln -s starRocks-3.3.2 /opt/datasophon/starrocks && \
    echo "====> 正在更改文件权限..." && \
    chmod -R 755 /opt/datasophon/starRocks-3.3.2

# 设置环境变量 - 使用硬编码路径而不是变量引用
ENV STARROCKS_HOME=/opt/datasophon/starrocks \
    PATH=/opt/datasophon/starrocks/bin:/usr/bin:$PATH