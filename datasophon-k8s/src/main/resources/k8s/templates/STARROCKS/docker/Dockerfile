# =================================================================
#  阶段一：工作台 (Builder) - 使用CentOS 7作为基础
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk AS builder

# 设置工作目录
WORKDIR /opt/datasophon

# 设置基础URL环境变量
ENV BASE_URL=http://192.168.1.30
ENV MYSQL_CLIENT_PKG=04_mysql-community-client-5.7.16-1.el7.x86_64.rpm
ENV MYSQL_CLIENT_PATH=BDP/datasophon-init/datasophon-init/packages

# --- 在单个RUN指令中完成所有下载、解压和清理操作 ---
RUN \
    # 下载MySQL客户端
    echo "====> 下载MySQL客户端..." && \
    curl -L -s -o ${MYSQL_CLIENT_PKG} ${BASE_URL}/${MYSQL_CLIENT_PATH}/${MYSQL_CLIENT_PKG} && \
    \
    # 检查下载文件是否为有效RPM包
    echo "====> 验证RPM包..." && \
    du -sh *  && \
    # 强制安装MySQL客户端(忽略依赖)
    echo "====> 强制安装MySQL客户端..." && \
    rpm --nodeps -ivh ${MYSQL_CLIENT_PKG} && \
    rm -f ${MYSQL_CLIENT_PKG} && \
    \
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
    echo "====> 构建阶段清理完成."

# =================================================================
#  阶段二：最终产物 (Final Image) - 基于CentOS 7
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk

WORKDIR /opt/datasophon

# --- 精确拷贝，只从builder阶段复制清理后的目录 ---
COPY --from=builder /opt/datasophon/starRocks-3.3.2 /opt/datasophon/starRocks-3.3.2
COPY --from=builder /usr/bin/mysql /usr/bin/mysql

# --- 设置用户、权限和软链接 ---
RUN \
    # 创建软链接
    echo "====> 正在创建软链接..." && \
    ln -s starRocks-3.3.2 /opt/datasophon/starrocks && \
    echo "====> 正在更改文件权限..." && \
    chmod -R 755 /opt/datasophon/starRocks-3.3.2

# 设置环境变量
ENV STARROCKS_HOME=/opt/datasophon/starrocks \
    PATH=$PATH:$STARROCKS_HOME/bin:/usr/bin \
