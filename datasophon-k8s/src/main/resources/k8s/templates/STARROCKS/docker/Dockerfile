# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

ADD starRocks-3.3.2.tar.gz /opt/datasophon

RUN chmod -R 775 /opt/datasophon/starRocks-3.3.2 \
    && ln -s /opt/datasophon/starRocks-3.3.2 /opt/datasophon/starrocks


FROM 192.168.5.200:5000/bdp/jdk-openssl:1.8.0_333

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon /opt/datasophon