# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

WORKDIR /opt/datasophon
ADD http://192.168.1.30/BDP/packages/1.2.1/juicefs-1.2.2.tar.gz /opt/datasophon/

RUN tar -zxvf juicefs-1.2.2.tar.gz && \
    rm -f juicefs-1.2.2.tar.gz && \
    ln -s juicefs-1.2.2 juicefs

