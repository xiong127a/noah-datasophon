# =================================================================
#  CentOS 7基础镜像
# =================================================================
FROM harbor.norintech.com/bdp/centos:7.9.2009

# 作者信息
LABEL maintainer="renxiangpeng"

# 替换YUM源
RUN echo "====> 配置YUM源..." && \
    rm -f /etc/yum.repos.d/*.repo && \
    curl -o /etc/yum.repos.d/CentOS-Base.repo https://mirrors.aliyun.com/repo/Centos-7.repo && \
    sed -i -e '/mirrors.cloud.aliyuncs.com/d' -e '/mirrors.aliyuncs.com/d' /etc/yum.repos.d/CentOS-Base.repo && \
    yum clean all && \
    yum makecache

# 设置工作目录
WORKDIR /opt