FROM 192.168.5.200:5000/bdp/centos:7.9.2009

# 创建用户和组，并创建目录
RUN mkdir -p /opt/datasophon

# 设置工作目录
WORKDIR /opt/datasophon

# 将 tar.gz 文件添加到工作目录
ADD redis-7.2.3.tar.gz /opt/datasophon

RUN ln -s /opt/datasophon/redis-7.2.3 /opt/datasophon/redis

# 修改权限
RUN chmod -R 775 /opt/datasophon/redis-7.2.3
