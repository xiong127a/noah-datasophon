# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ENV PROSTO_VERSION 0.287

ADD presto-${PROSTO_VERSION}.tar.gz /opt/datasophon

RUN chmod -R 775 /opt/datasophon/presto-${PROSTO_VERSION}
# 第二阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333

ENV PROSTO_VERSION 0.287

WORKDIR /opt/datasophon

RUN ln -s /opt/datasophon/presto-${PROSTO_VERSION} /opt/datasophon/presto

COPY --from=builder /opt/datasophon/presto-${PROSTO_VERSION} /opt/datasophon/presto-${PROSTO_VERSION}