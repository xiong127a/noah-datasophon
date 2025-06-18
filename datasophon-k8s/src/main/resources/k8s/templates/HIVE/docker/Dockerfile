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
    curl -L -s -o hadoop-3.3.3.tar.gz http://192.168.1.30/BDP/packages/1.2.1/hadoop-3.3.3.tar.gz && \
    curl -L -s -o hive-3.1.0.tar.gz http://192.168.1.30/BDP/packages/1.2.1/hive-3.1.0.tar.gz && \
    \
    # 解压
    echo "====> 解压软件包..." && \
    tar -zxf hadoop-3.3.3.tar.gz && \
    tar -zxf hive-3.1.0.tar.gz && \
    \
    # 删除原始压缩包
    echo "====> 清理压缩包..." && \
    rm -f hadoop-3.3.3.tar.gz hive-3.1.0.tar.gz && \
    \
    # 清理不必要的目录和文件
    echo "====> 优化并删除不必要的文件..." && \
    rm -rf \
        hadoop-3.3.3/share/doc \
        hadoop-3.3.3/share/hadoop/tools \
        hadoop-3.3.3/src \
        hive-3.1.0/doc \
        hive-3.1.0/examples \
        hive-3.1.0/src \
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
COPY --from=builder /opt/datasophon/hive-3.1.0 /opt/datasophon/hive-3.1.0

# --- 设置用户、权限和软链接 (CentOS版本) ---
RUN \
    # 安装运行时依赖
    echo "====> 正在安装运行时依赖..." && \
    yum install -y mysql libxml2 which && \
    yum clean all && \
    \
    # 创建用户和组
    echo "====> 正在创建用户和组..." && \
    groupadd -r -g 2001 hadoop && \
    useradd -r -u 2003 -g hadoop -d /home/hive -m -s /bin/bash hive && \
    \
    # 创建软链接
    echo "====> 正在创建软链接..." && \
    ln -s hadoop-3.3.3 hadoop && \
    ln -s hive-3.1.0 hive && \
    \
    # 更改权限
    echo "====> 正在更改文件权限..." && \
    chown -R hive:hadoop /opt/datasophon/hadoop-3.3.3 /opt/datasophon/hive-3.1.0 && \
    chown -h hive:hadoop /opt/datasophon/hadoop /opt/datasophon/hive && \
    chmod -R 755 /opt/datasophon/hadoop

# 设置环境变量
# 基础镜像已包含JAVA_HOME
ENV HADOOP_HOME=/opt/datasophon/hadoop \
    HADOOP_CONF_DIR=/opt/datasophon/hadoop/etc/hadoop \
    HIVE_HOME=/opt/datasophon/hive \
    PATH=$PATH:$HADOOP_HOME/bin:$HIVE_HOME/bin \
    HADOOP_USER_NAME=hive
