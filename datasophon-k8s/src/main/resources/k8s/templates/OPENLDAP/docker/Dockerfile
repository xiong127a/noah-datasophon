# 第一阶段
FROM 192.168.5.200:5000/bdp/centos:7.9.2009 AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD openldap-2.4.44-22.el7.tar.gz /opt/datasophon/

RUN chmod -R 755 /opt/datasophon/openldap-2.4.44-22.el7

# 第二阶段
FROM 192.168.5.200:5000/bdp/centos:7.9.2009

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/openldap-2.4.44-22.el7 /opt/datasophon/openldap-2.4.44-22.el7