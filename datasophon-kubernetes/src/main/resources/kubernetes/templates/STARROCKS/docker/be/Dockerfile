# =================================================================
#  阶段一：工作台 (Builder) - 使用CentOS 7作为基础
# =================================================================
FROM harbor.norintech.com/bdp/centos:7.9.2009-tools AS builder

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
    # 删除原始压缩包
    echo "====> 清理压缩包..." && \
    rm -f starRocks-3.3.2.tar.gz && \
    \
    # 只保留BE组件
    echo "====> 提取BE组件..." && \
    mkdir -p /opt/datasophon/starRocks-3.3.2-be && \
    mv /opt/datasophon/starRocks-3.3.2/be /opt/datasophon/starRocks-3.3.2-be/ && \
    cp /opt/datasophon/starRocks-3.3.2/LICENSE.txt /opt/datasophon/starRocks-3.3.2-be/ && \
    cp /opt/datasophon/starRocks-3.3.2/NOTICE.txt /opt/datasophon/starRocks-3.3.2-be/ && \
    echo "====> 构建阶段清理完成."

# =================================================================
#  阶段二：最终产物 (Final Image) - 基于CentOS 7
# =================================================================
FROM harbor.norintech.com/bdp/centos:7.9.2009-tools

LABEL maintainer="DataSophon Team" \
      description="StarRocks BE image with netcat tools"

WORKDIR /opt/datasophon

# --- 精确拷贝，只从builder阶段复制BE目录 ---
COPY --from=builder /opt/datasophon/starRocks-3.3.2-be /opt/datasophon/starRocks-3.3.2

# --- 安装依赖、设置用户、权限和软链接 ---
RUN \
    # 安装必要工具
    echo "====> 安装必要工具..." && \
    yum install -y nc && \
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

# 设置环境变量
ENV STARROCKS_HOME=/opt/datasophon/starrocks \
    PATH=/opt/datasophon/starrocks/be/bin:/usr/bin:$PATH