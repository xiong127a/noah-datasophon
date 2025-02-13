# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ENV FLINK_VERSION 1.16.2

ADD flink-${FLINK_VERSION}.tar.gz /opt/datasophon

ADD hadoop-3.3.3.tar.gz /opt/datasophon/

RUN ln -s /opt/datasophon/flink-${FLINK_VERSION} /opt/datasophon/flink \
    && chmod -R 775 /opt/datasophon/flink-${FLINK_VERSION}
