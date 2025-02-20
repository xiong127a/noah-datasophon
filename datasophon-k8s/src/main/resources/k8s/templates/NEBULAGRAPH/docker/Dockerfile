FROM 192.168.5.200:5000/bdp/centos:7.9.2009 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD nebulagraph-3.6.0.tar.gz /opt/datasophon/

COPY lsof-4.87-6.el7.x86_64.rpm /opt/datasophon/

RUN chmod -R 755 /opt/datasophon/nebulagraph-3.6.0 \
    && ln -s /opt/datasophon/nebulagraph-3.6.0 /opt/datasophon/nebulagraph \
    && rpm -ivh lsof-4.87-6.el7.x86_64.rpm
