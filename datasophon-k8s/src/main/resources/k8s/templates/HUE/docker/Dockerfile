FROM 192.168.5.200:5000/bdp/centos-hue:7.9.2009

# 创建用户和组，并创建目录
RUN groupadd -r -g 2008 hue \
    && useradd -r -u 2008 -g 2008 hue \
    && mkdir -p /opt/datasophon

# 设置工作目录
WORKDIR /opt/datasophon

# 将 tar.gz 文件添加到工作目录
ADD hue-4.11.0.tar.gz /opt/datasophon

# 修改权限
RUN chmod -R 775 /opt/datasophon/hue-4.11.0 \
    && chown -R hue:hue /opt/datasophon/hue-4.11.0
