# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

# 使用指定的用户和组 ID 创建用户和组
RUN groupadd -r -g 2001 hadoop \
    && useradd -r -u 2002 -g 2001 yarn \
    && useradd -r -u 2001 -g 2001 hdfs \
    && mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD hadoop-3.3.3.tar.gz /opt/datasophon/

RUN chown -R root:hadoop /opt/datasophon/hadoop-3.3.3 \
    && chmod -R 775 /opt/datasophon/hadoop-3.3.3 \
    && chmod 6050 /opt/datasophon/hadoop-3.3.3/bin/container-executor \
    && chmod 400 /opt/datasophon/hadoop-3.3.3/etc/hadoop/container-executor.cfg \
    && chown -R yarn:hadoop /opt/datasophon/hadoop-3.3.3/logs/userlogs \
    && chmod 775 /opt/datasophon/hadoop-3.3.3/logs/userlogs

# 第二阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

# 使用指定的用户和组 ID 创建用户和组
RUN groupadd -r -g 2001 hadoop \
    && useradd -r -u 2002 -g 2001 -m yarn \
    && useradd -r -u 2001 -g 2001 -m hdfs \
    && useradd -r -u 2004 -g 2001 -m mapred

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/hadoop-3.3.3 /opt/datasophon/hadoop-3.3.3

ENV HADOOP_HOME=/opt/datasophon/hadoop-3.3.3 \
    HADOOP_CONF_DIR=/opt/datasophon/hadoop-3.3.3/etc/hadoop \
    PATH=$PATH:/opt/datasophon/hadoop-3.3.3/bin \
    HADOOP_USER_NAME=hdfs