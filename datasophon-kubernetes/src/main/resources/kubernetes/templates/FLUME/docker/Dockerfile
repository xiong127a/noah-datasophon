FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

# 创建用户和组，并创建目录
RUN mkdir -p /opt/datasophon

# 设置工作目录
WORKDIR /opt/datasophon

# 将 tar.gz 文件添加到工作目录
ADD flume-1.11.0.tar.gz /opt/datasophon

# 修改权限
RUN chmod -R 775 /opt/datasophon/flume-1.11.0 \
    && ln -s /opt/datasophon/flume-1.11.0 /opt/datasophon/flume
