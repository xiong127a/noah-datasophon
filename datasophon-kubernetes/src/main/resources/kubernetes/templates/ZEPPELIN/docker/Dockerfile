# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

ADD --checksum=sha256:b7ddb3dcdec88ac36ca00bc4c536cafc4724de20e35f014ce10f2299736fafae --chmod=755 http://192.168.1.30/BDP/packages/1.2.1/zeppelin-0.10.1.tar.gz /opt/datasophon/
WORKDIR /opt/datasophon

RUN tar -zxvf zeppelin-0.10.1.tar.gz  && \
    rm -f zeppelin-0.10.1.tar.gz && \
    ln -s zeppelin-0.10.1 zeppelin

