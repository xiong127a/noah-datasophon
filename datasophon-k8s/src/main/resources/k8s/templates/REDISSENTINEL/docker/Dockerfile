FROM harbor.norintech.com/bdp/centos:7.9.2009

ENV REDIS_SENTINEL_NAME redis-sentinel-7.4.0

ADD http://192.168.1.30/BDP/packages/1.2.1/${REDIS_SENTINEL_NAME}.tar.gz /opt/datasophon/

RUN tar -zxvf /opt/datasophon/${REDIS_SENTINEL_NAME}.tar.gz -C /opt/datasophon/ \
    && rm -f /opt/datasophon/${REDIS_SENTINEL_NAME}.tar.gz  \
    && ln -s /opt/datasophon/${REDIS_SENTINEL_NAME} /opt/datasophon/redissentinel \
    && chmod -R 755 /opt/datasophon/${REDIS_SENTINEL_NAME}

WORKDIR /opt/datasophon
