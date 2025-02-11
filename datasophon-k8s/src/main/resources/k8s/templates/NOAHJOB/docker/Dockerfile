# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD noah-job-3.0.0.tar.gz /opt/datasophon

RUN chmod -R 775 /opt/datasophon/noah-job-3.0.0 \
    && ln -s /opt/datasophon/noah-job-3.0.0 /opt/datasophon/noahjob