# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

RUN groupadd -r hadoop && useradd -r -g hadoop yarn && useradd -r -g hadoop hdfs && mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD hadoop-3.3.3.tar.gz /opt/datasophon/

RUN chown -R root:hadoop /opt/datasophon/hadoop-3.3.3 \
    && chmod 755 /opt/datasophon/hadoop-3.3.3 \
    && chmod -R 755 /opt/datasophon/hadoop-3.3.3/etc \
    && chmod 6050 /opt/datasophon/hadoop-3.3.3/bin/container-executor \
    && chmod 400 /opt/datasophon/hadoop-3.3.3/etc/hadoop/container-executor.cfg \
    && chown -R yarn:hadoop /opt/datasophon/hadoop-3.3.3/logs/userlogs \
    && chmod 775 /opt/datasophon/hadoop-3.3.3/logs/userlogs

# 第二阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

RUN groupadd -r hadoop && useradd -r -g hadoop yarn && useradd -r -g hadoop hdfs

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/hadoop-3.3.3 /opt/datasophon/hadoop-3.3.3