# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD doris-1.2.6.tar.gz /opt/datasophon

RUN chmod -R 775 /opt/datasophon/doris-1.2.6 \
    && ln -s /opt/datasophon/doris-1.2.6 /opt/datasophon/doris