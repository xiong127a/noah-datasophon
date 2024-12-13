# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

WORKDIR /opt/datasophon
ADD http://192.168.1.30/BDP/packages/1.2.1/logstash-7.16.2.tar.gz /opt/datasophon/

RUN tar -zxvf logstash-7.16.2.tar.gz && \
    rm -f logstash-7.16.2.tar.gz && \
    ln -s logstash-7.16.2 logstash

