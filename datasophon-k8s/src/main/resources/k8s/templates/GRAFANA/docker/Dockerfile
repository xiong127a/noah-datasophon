# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD grafana-9.1.6.tar.gz /opt/datasophon/

RUN chmod -R 755 /opt/datasophon/grafana-9.1.6

# 第二阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/grafana-9.1.6 /opt/datasophon/grafana-9.1.6