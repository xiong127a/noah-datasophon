# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

RUN groupadd -r -g 2007 elastic \
    && useradd -r -u 2007 -g 2007 elastic \
    && mkdir -p /opt/datasophon

ENV ELASTICSEARCH_VERSION 7.16.2

ADD http://192.168.1.30/BDP/packages/1.2.1/elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz /opt/datasophon/

WORKDIR /opt/datasophon

RUN tar -zxvf elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz \
    && rm -rf elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz \
    && ln -s elasticsearch-${ELASTICSEARCH_VERSION} elasticsearch \
    && chmod -R 755 /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION} \
    && chown -R elastic:elastic /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION}

# 第二阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333

RUN groupadd -r -g 2007 elastic \
    && useradd -r -u 2007 -g 2007 elastic

ENV ELASTICSEARCH_VERSION 7.16.2

ENV JAVA_HOME=/opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION}/jdk

ENV PATH $JAVA_HOME/bin:$PATH

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION} /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION}