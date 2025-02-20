# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ENV KAFKA_VERSION 2.4.1

ADD kafka-${KAFKA_VERSION}.tar.gz /opt/datasophon

RUN chmod -R 775 /opt/datasophon/kafka-${KAFKA_VERSION}
# 第二阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

ENV KAFKA_VERSION 2.4.1

WORKDIR /opt/datasophon

RUN ln -s /opt/datasophon/kafka-${KAFKA_VERSION} /opt/datasophon/kafka

COPY --from=builder /opt/datasophon/kafka-${KAFKA_VERSION} /opt/datasophon/kafka-${KAFKA_VERSION}