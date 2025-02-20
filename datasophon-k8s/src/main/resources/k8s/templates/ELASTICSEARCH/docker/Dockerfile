# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

RUN groupadd -r -g 2007 elastic \
    && useradd -r -u 2007 -g 2007 elastic \
    && mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ENV ELASTICSEARCH_VERSION 7.16.2

ADD elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz /opt/datasophon/

RUN chmod -R 755 /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION} \
    && chown -R elastic:elastic /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION}

# 第二阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

RUN groupadd -r -g 2007 elastic \
    && useradd -r -u 2007 -g 2007 elastic

ENV ELASTICSEARCH_VERSION 7.16.2

ENV JAVA_HOME=/opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION}/jdk

ENV PATH $JAVA_HOME/bin:$PATH

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION} /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION}