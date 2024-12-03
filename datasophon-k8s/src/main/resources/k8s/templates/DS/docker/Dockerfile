# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

ENV DOLPHINSCHEDULER_DIR_NAME dolphinscheduler-3.1.8

ADD http://192.168.1.30/BDP/packages/1.2.1/${DOLPHINSCHEDULER_DIR_NAME}.tar.gz /opt/datasophon/

RUN tar -zxvf /opt/datasophon/${DOLPHINSCHEDULER_DIR_NAME}.tar.gz -C /opt/datasophon/ \
    && rm -f /opt/datasophon/${DOLPHINSCHEDULER_DIR_NAME}.tar.gz  \
    && ln -s /opt/datasophon/${DOLPHINSCHEDULER_DIR_NAME} /opt/datasophon/dolphinscheduler \
    && chmod -R 755 /opt/datasophon/${DOLPHINSCHEDULER_DIR_NAME}

WORKDIR /opt/datasophon
