# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD alertmanager-0.23.0.tar.gz /opt/datasophon/

RUN chmod -R 775 /opt/datasophon/alertmanager-0.23.0

# 第二阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/alertmanager-0.23.0 /opt/datasophon/alertmanager-0.23.0