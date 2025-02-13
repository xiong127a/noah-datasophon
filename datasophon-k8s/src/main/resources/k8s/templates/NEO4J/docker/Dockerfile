# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD neo4j-5.16.0.tar.gz /opt/datasophon

RUN chmod -R 775 /opt/datasophon/neo4j-5.16.0 \
    && ln -s /opt/datasophon/neo4j-5.16.0 /opt/datasophon/neo4j