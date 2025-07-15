# 第一阶段
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk AS builder

RUN mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

# 从远程URL下载Grafana包
RUN curl -O http://192.168.1.30/BDP/packages/1.2.1/grafana-9.1.6.tar.gz && \
    tar -xzf grafana-9.1.6.tar.gz && \
    rm -f grafana-9.1.6.tar.gz

RUN chmod -R 755 /opt/datasophon/grafana-9.1.6

# 第二阶段
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk

WORKDIR /opt/datasophon

COPY --from=builder /opt/datasophon/grafana-9.1.6 /opt/datasophon/grafana-9.1.6