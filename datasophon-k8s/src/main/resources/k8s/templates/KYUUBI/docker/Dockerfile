# 第一阶段
FROM harbor.norintech.com/bdp/jdk:1.8.0_333 AS builder

# 创建 kyuubi 用户，并将其加入 hadoop 组
RUN groupadd -g 1001 hadoop && useradd -u 1001 -g hadoop -m kyuubi

# 下载并解压 kyuubi 包，删除 tar.gz 文件并创建软链接
ADD --checksum=sha256:3cbd1583b9df0aa78827cc8bb5902e03fde2d339d6d15917f290ec80ba1aeb90 \
    --chmod=755 --chown=kyuubi:hadoop http://192.168.1.30/BDP/packages/1.2.1/kyuubi-1.7.3.tar.gz /opt/datasophon/

WORKDIR /opt/datasophon

# 切换到 kyuubi 用户
USER kyuubi

# 解压并赋予权限，避免临时文件留存
RUN tar -zxvf kyuubi-1.7.3.tar.gz && \
    rm -f kyuubi-1.7.3.tar.gz && \
    ln -s kyuubi-1.7.3 kyuubi

USER root
