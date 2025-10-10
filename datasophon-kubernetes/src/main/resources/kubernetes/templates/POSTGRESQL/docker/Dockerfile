FROM 192.168.5.200:5000/bdp/centos:7.9.2009

# 创建用户和组，并创建目录
RUN groupadd -r -g 2009 postgres \
    && useradd -r -u 2009 -g 2009 postgres \
    && mkdir -p /opt/datasophon

# 设置工作目录
WORKDIR /opt/datasophon

# 将 tar.gz 文件添加到工作目录
ADD postgresql-16.1.tar.gz /opt/datasophon

RUN ln -s /opt/datasophon/postgresql-16.1 /opt/datasophon/postgresql

# 修改权限
RUN chmod -R 775 /opt/datasophon/postgresql-16.1 \
    && chown -R hue:hue /opt/datasophon/postgresql-16.1
