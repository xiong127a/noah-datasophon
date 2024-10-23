# 第一阶段 需要安装sudo
FROM 192.168.5.200:5000/bdp/centos:7.9.2009

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD clickhouse-23.9.1.1854.tar.gz /opt/datasophon

RUN chmod -R 775 /opt/datasophon/clickhouse-23.9.1.1854 \
    && ln -s /opt/datasophon/clickhouse-23.9.1.1854 /opt/datasophon/clickhouse