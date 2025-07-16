# =================================================================
#  阶段一：工作台 (Builder) - 使用CentOS 7作为基础
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk AS builder

# 设置工作目录
WORKDIR /opt/datasophon

# 设置基础URL环境变量
ENV BASE_URL=http://192.168.1.30

# --- 在单个RUN指令中完成所有下载、解压和清理操作 ---
RUN \
    # 下载软件包
    echo "====> 下载软件包..." && \
    curl -L -s -o hbase-2.2.7.tar.gz ${BASE_URL}/BDP/packages/1.2.1/hbase-2.2.7.tar.gz && \
    curl -L -s -o hadoop-3.3.3-mini.tar.gz ${BASE_URL}/BDP/packages/1.2.1/hadoop-3.3.3-mini.tar.gz && \
    \
    # 解压
    echo "====> 解压软件包..." && \
    tar -zxf hbase-2.2.7.tar.gz && \
    tar -zxf hadoop-3.3.3-mini.tar.gz && \
    \
    # 删除原始压缩包
    echo "====> 清理压缩包..." && \
    rm -f hbase-2.2.7.tar.gz hadoop-3.3.3-mini.tar.gz && \
    \
    # 清理不必要的目录和文件
    echo "====> 优化并删除不必要的文件..." && \
    rm -rf \
        hadoop-3.3.3/share/doc \
        hadoop-3.3.3/share/hadoop/tools \
        hadoop-3.3.3/src \
        hbase-2.2.7/docs \
    && \
    find . -type f -name "*.cmd" -delete && \
    echo "====> 构建阶段清理完成."


# =================================================================
#  阶段二：最终产物 (Final Image) - 基于CentOS 7
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk

WORKDIR /opt/datasophon

# --- 精确拷贝，只从builder阶段复制清理后的目录 ---
COPY --from=builder /opt/datasophon/hadoop-3.3.3 /opt/datasophon/hadoop-3.3.3
COPY --from=builder /opt/datasophon/hbase-2.2.7 /opt/datasophon/hbase-2.2.7

# --- 设置用户、权限和软链接 ---
RUN \
    # 创建用户和组
    echo "====> 正在创建用户和组..." && \
    groupadd -r -g 2001 hadoop && \
    useradd -r -u 2001 -g hadoop -m -s /bin/bash hdfs && \
    useradd -r -u 2005 -g hadoop -m -s /bin/bash hbase && \
    \
    # 创建软链接
    echo "====> 正在创建软链接..." && \
    ln -s hadoop-3.3.3 hadoop && \
    ln -s hbase-2.2.7 hbase && \
    \
    # 更改权限
    echo "====> 正在更改文件权限..." && \
    chown -R hdfs:hadoop /opt/datasophon/hadoop-3.3.3 && \
    chown -R hbase:hadoop /opt/datasophon/hbase-2.2.7 && \
    chown -h hdfs:hadoop /opt/datasophon/hadoop && \
    chown -h hbase:hadoop /opt/datasophon/hbase && \
    chmod -R 755 /opt/datasophon/hadoop-3.3.3 /opt/datasophon/hbase-2.2.7

# 设置环境变量
# 基础镜像已包含JAVA_HOME
ENV HADOOP_HOME=/opt/datasophon/hadoop \
    HADOOP_CONF_DIR=/opt/datasophon/hadoop/etc/hadoop \
    HBASE_HOME=/opt/datasophon/hbase \
    PATH=$PATH:$HADOOP_HOME/bin:$HBASE_HOME/bin \
    HADOOP_USER_NAME=hdfs




