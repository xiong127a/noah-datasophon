# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

ADD --checksum=sha256:e1f90c3e2cbd2db452717400183199aa022880cd051d18128b779531a5357a14 --chmod=755 http://192.168.1.30/BDP/packages/1.2.1/streampark-2.1.1.tar.gz /opt/datasophon/
WORKDIR /opt/datasophon
RUN tar -zxvf streampark-2.1.1.tar.gz  && \
    rm -f streampark-2.1.1.tar.gz && \
    ln -s streampark-2.1.1 streampark