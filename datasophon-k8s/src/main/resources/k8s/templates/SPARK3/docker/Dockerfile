# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ENV SPARK_VERSION 3.2.2

ADD spark-${SPARK_VERSION}.tar.gz /opt/datasophon

ADD hadoop-3.3.3.tar.gz /opt/datasophon/


COPY spark-submit-status.sh /opt/datasophon/spark-${SPARK_VERSION}

RUN ln -s /opt/datasophon/spark-${SPARK_VERSION} /opt/datasophon/spark \
    && chmod -R 775 /opt/datasophon/spark-${SPARK_VERSION}
