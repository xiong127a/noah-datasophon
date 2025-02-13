# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD noah-sync-2.3.5.tar.gz /opt/datasophon

RUN chmod -R 775 /opt/datasophon/noah-sync-2.3.5 \
    && ln -s /opt/datasophon/noah-sync-2.3.5 /opt/datasophon/noahsync