# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

ADD --chmod=755 http://192.168.1.30/BDP/packages/1.2.1/kafka-2.4.1.tar.gz /opt/datasophon/

WORKDIR /opt/datasophon
RUN tar -zxvf kafka-2.4.1.tar.gz  && \
    rm -f kafka-2.4.1.tar.gz && \
    ln -s kafka-2.4.1 kafka