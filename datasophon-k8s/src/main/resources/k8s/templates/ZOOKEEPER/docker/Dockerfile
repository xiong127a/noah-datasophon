# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD zookeeper-3.5.10.tar.gz /opt/datasophon/

RUN chmod -R 755 /opt/datasophon/zookeeper-3.5.10

# 第二阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/zookeeper-3.5.10 /opt/datasophon/zookeeper-3.5.10