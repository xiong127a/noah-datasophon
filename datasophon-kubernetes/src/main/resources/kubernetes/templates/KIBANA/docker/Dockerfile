# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder


# 创建 elastic 组和用户
RUN useradd elastic

ADD http://192.168.1.30/BDP/packages/1.2.1/kibana-7.16.2.tar.gz /opt/datasophon/

WORKDIR /opt/datasophon
RUN tar -zxvf kibana-7.16.2.tar.gz && \
    rm -f kibana-7.16.2.tar.gz && \
    chown elastic:elastic -R kibana-7.16.2 && \
    chmod 755 -R kibana-7.16.2 && \
    ln -s kibana-7.16.2 kibana

