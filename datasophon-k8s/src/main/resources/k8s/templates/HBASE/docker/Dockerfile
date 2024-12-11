# 使用基础镜像
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

# 添加用户组和用户
RUN groupadd -r -g 2001 hadoop \
    && useradd -r -u 2001 -g 2001 -m hdfs \
    && useradd -r -u 2005 -g 2001 -m hbase


# 设置基础URL环境变量
ENV BASE_URL=http://192.168.1.30
WORKDIR /opt/datasophon

# 下载 Hadoop 和 HBase 压缩包并设置权限
ADD --checksum=sha256:90eed5e6f5a0575f85cd03eb806ef7ceba58286fe99a794bf90bf5bc99bfed86 --chown=hbase:hadoop --chmod=775 ${BASE_URL}/BDP/packages/1.2.1/hbase-2.2.7.tar.gz /opt/datasophon/
ADD --checksum=sha256:5cb42b38d6064b6eaf920fb7d678b5bd3ea5a575210a1044b21543fc1b873548 --chown=hdfs:hadoop --chmod=775 ${BASE_URL}/BDP/packages/1.2.1/hadoop-3.3.3-mini.tar.gz /opt/datasophon/

# 设置工作目录

# 安装 ca-certificates 并下载 gosu 根据系统架构
# 解压 tar 文件并删除压缩包
RUN tar -zxvf hbase-2.2.7.tar.gz \
    && tar -zxvf hadoop-3.3.3-mini.tar.gz \
    && rm hbase-2.2.7.tar.gz hadoop-3.3.3-mini.tar.gz \
    && chmod -R 755 hadoop-3.3.3 hbase-2.2.7 \
    && chown -R hdfs:hadoop hadoop-3.3.3 \
    && chown -R hbase:hadoop hbase-2.2.7 \
    && ln -s hadoop-3.3.3 hadoop \
    && ln -s hbase-2.2.7 hbase

# 设置环境变量
ENV HADOOP_HOME=/opt/datasophon/hadoop-3.3.3 \
    HADOOP_CONF_DIR=/opt/datasophon/hadoop-3.3.3/etc/hadoop \
    HBASE_HOME=/opt/datasophon/hbase-2.2.7 \
    PATH=$PATH:$HADOOP_HOME/bin:$HBASE_HOME/bin \
    HADOOP_USER_NAME=hdfs




