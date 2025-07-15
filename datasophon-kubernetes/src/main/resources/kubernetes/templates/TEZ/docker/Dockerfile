# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

# 创建 yarn 用户，并将其加入 hadoop 组
RUN groupadd -g 2001 hadoop && useradd -u 2002 -g hadoop -m yarn

ADD --checksum=sha256:afe8302a5f47fbe859b11acbdd0192601bd3a0729f3b45e667c08d550906b557 --chmod=755 --chown=yarn:hadoop http://192.168.1.30/BDP/packages/1.2.1/tez-0.10.2.tar.gz /opt/datasophon/
WORKDIR /opt/datasophon

ADD --checksum=sha256:5cb42b38d6064b6eaf920fb7d678b5bd3ea5a575210a1044b21543fc1b873548 --chmod=755 --chown=yarn:hadoop http://192.168.1.30/BDP/packages/1.2.1/hadoop-3.3.3-mini.tar.gz /opt/datasophon/
USER yarn
RUN tar -zxvf tez-0.10.2.tar.gz  && \
    rm -f tez-0.10.2.tar.gz && \
    ln -s tez-0.10.2 tez && \
    tar -zxvf hadoop-3.3.3-mini.tar.gz && \
    rm -f hadoop-3.3.3-mini.tar.gz && \
    ln -s hadoop-3.3.3 hadoop

USER root

