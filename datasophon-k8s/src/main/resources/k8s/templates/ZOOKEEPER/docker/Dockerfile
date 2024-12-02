# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

ADD --checksum=sha256:90fc0f9ea07c49b51ca9cb9289bd22c5b97981c4e0bfd57f2f910e899b46ad91 --chmod=755 http://192.168.1.30/BDP/packages/1.2.1/zookeeper-3.5.10.tar.gz /opt/datasophon/
WORKDIR /opt/datasophon
RUN tar -zxvf zookeeper-3.5.10.tar.gz  && \
    rm -f zookeeper-3.5.10.tar.gz && \
    ln -s zookeeper-3.5.10 zookeeper