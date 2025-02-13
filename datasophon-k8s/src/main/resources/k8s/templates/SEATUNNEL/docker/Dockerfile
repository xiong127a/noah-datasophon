FROM harbor.norintech.com/bdp/jdk:1.8.0_333

# 创建用户和组，并创建目录
RUN mkdir -p /opt/datasophon

# 设置工作目录
WORKDIR /opt/datasophon

# 将 tar.gz 文件添加到工作目录
ADD seatunnel-2.3.3.tar.gz /opt/datasophon

# 修改权限
RUN chmod -R 775 /opt/datasophon/seatunnel-2.3.3 \
    && ln -s /opt/datasophon/seatunnel-2.3.3 /opt/datasophon/seatunnel
