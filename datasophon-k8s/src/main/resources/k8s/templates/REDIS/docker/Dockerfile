# =================================================================
#  阶段一：工作台 (Builder) - 使用CentOS 7作为基础
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk AS builder

# 设置工作目录
WORKDIR /opt/datasophon
# --- 在单个RUN指令中完成所有下载、解压和清理操作 ---
RUN \
    # 下载软件包
    echo "====> 下载软件包..." && \
    curl -L -s -o redis-7.2.3.tar.gz http://192.168.1.30/BDP/packages/1.2.1/redis-7.2.3.tar.gz && \
    \
    # 解压
    echo "====> 解压软件包..." && \
    tar -zxf redis-7.2.3.tar.gz && \
    \
    # 删除原始压缩包
    echo "====> 清理压缩包..." && \
    rm -f redis-7.2.3.tar.gz && \
    \
    # 清理不必要的目录和文件
    echo "====> 优化并删除不必要的文件..." && \
    rm -rf \
        redis-7.2.3/deps/jemalloc \
        redis-7.2.3/deps/lua \
        redis-7.2.3/tests \
    && \
    echo "====> 构建阶段清理完成."


# =================================================================
#  阶段二：最终产物 (Final Image) - 基于CentOS 7
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk

WORKDIR /opt/datasophon

# --- 精确拷贝，只从builder阶段复制清理后的目录 ---
COPY --from=builder /opt/datasophon/redis-7.2.3 /opt/datasophon/redis-7.2.3

# --- 设置用户、权限和软链接 (CentOS版本) ---
RUN \
    # 创建用户和组
    echo "====> 正在创建用户和组..." && \
    groupadd -r -g 6379 redis && \
    useradd -r -u 6379 -g redis -d /home/redis -m -s /bin/bash redis && \
    \
    # 创建软链接
    echo "====> 正在创建软链接..." && \
    ln -s /opt/datasophon/redis-7.2.3 /opt/datasophon/redis && \
    \
    # 更改权限
    echo "====> 正在更改文件权限..." && \
    chown -R redis:redis /opt/datasophon/redis-7.2.3 && \
    chown -h redis:redis /opt/datasophon/redis && \
    chmod -R 755 /opt/datasophon/redis-7.2.3

# 设置环境变量
ENV REDIS_HOME=/opt/datasophon/redis \
    PATH=$PATH:$REDIS_HOME
