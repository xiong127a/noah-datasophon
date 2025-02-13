# 第一阶段：构建阶段（包含所有构建工具）
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

# 创建用户和目录（保持最小权限）
RUN groupadd -r -g 2002 ranger && \
    useradd -r -u 2011 -g ranger -m ranger && \
    useradd -r -u 2012 -g ranger -m rangerusersync && \
    useradd -r -u 2013 -g ranger -m rangerkms

ADD --checksum=sha256:f30b4e7eeb536ef0e2f22d489db8bf38144c231cbeeb3df8ce112ec6c4f47893 --chmod=755 http://192.168.1.30/BDP/packages/1.2.1/ranger-2.1.0.tar.gz /opt/datasophon/
WORKDIR /opt/datasophon
RUN tar -zxvf ranger-2.1.0.tar.gz  && \
    chown -R ranger:ranger /opt/datasophon/ranger-2.1.0.tar.gz && \
    rm -f ranger-2.1.0.tar.gz && \
    ln -s ranger-2.1.0 ranger