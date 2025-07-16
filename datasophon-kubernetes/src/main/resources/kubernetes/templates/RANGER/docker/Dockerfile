# 第一阶段
FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333 AS builder

RUN groupadd -r -g 2002 ranger \
    && useradd -r -u 2011 -g 2002 -m ranger \
    && useradd -r -u 2012 -g 2002 -m rangerusersync \
    && useradd -r -u 2013 -g 2002 -m rangerkms \
    && mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD ranger-2.1.0.tar.gz /opt/datasophon/

COPY setup-ranger.sh /opt/datasophon/

RUN chmod -R 775 /opt/datasophon/ranger-2.1.0 \
    && rm -rf /opt/datasophon/ranger-2.1.0/ranger-2.1.0-usersync/install.properties \
    && ln -s /opt/datasophon/ranger-2.1.0/ranger-2.1.0-usersync/install.properties1 /opt/datasophon/ranger-2.1.0/ranger-2.1.0-usersync/install.properties \
    && rm -rf /opt/datasophon/ranger-2.1.0/ranger-2.1.0-kms/install.properties \
    && ln -s /opt/datasophon/ranger-2.1.0/ranger-2.1.0-kms/install.properties2 /opt/datasophon/ranger-2.1.0/ranger-2.1.0-kms/install.properties \
    && chown -R ranger:ranger /opt/datasophon/ranger-2.1.0 \
    && chmod 775 /opt/datasophon/setup-ranger.sh