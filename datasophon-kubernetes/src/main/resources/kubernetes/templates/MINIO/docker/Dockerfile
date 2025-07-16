FROM 192.168.5.200:5000/bdp/centos:7.9.2009

# 创建用户和组，并创建目录
RUN mkdir -p /opt/datasophon

# 设置工作目录
WORKDIR /opt/datasophon

# 将 tar.gz 文件添加到工作目录
ADD minio-20240613.tar.gz /opt/datasophon

# 修改权限
RUN chmod -R 775 /opt/datasophon/minio-20240613 \
    && ln -s /opt/datasophon/minio-20240613 /opt/datasophon/minio
