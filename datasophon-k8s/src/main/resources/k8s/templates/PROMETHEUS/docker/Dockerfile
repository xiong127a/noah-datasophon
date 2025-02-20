# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD prometheus-2.17.2.tar.gz /opt/datasophon/

RUN chmod -R 775 /opt/datasophon/prometheus-2.17.2

# 第二阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/prometheus-2.17.2 /opt/datasophon/prometheus-2.17.2