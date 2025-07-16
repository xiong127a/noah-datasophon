# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ENV TRINO_VERSION 367

ADD trino-${TRINO_VERSION}.tar.gz /opt/datasophon

RUN chmod -R 775 /opt/datasophon/trino-${TRINO_VERSION}

# 第二阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

ENV TRINO_VERSION 367

WORKDIR /opt/datasophon

RUN ln -s /opt/datasophon/trino-${TRINO_VERSION} /opt/datasophon/trino

COPY --from=builder /opt/datasophon/trino-${TRINO_VERSION} /opt/datasophon/trino-${TRINO_VERSION}