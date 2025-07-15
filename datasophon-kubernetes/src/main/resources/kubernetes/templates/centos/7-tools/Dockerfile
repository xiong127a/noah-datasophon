# =================================================================
#  CentOS 7基础镜像
# =================================================================
FROM harbor.norintech.com/bdp/centos:7.9.2009

# 作者信息
LABEL maintainer="renxiangpeng"

# 一次性执行所有命令，减少镜像层数
RUN echo "====> 配置YUM源..." && \
    rm -f /etc/yum.repos.d/*.repo && \
    curl -o /etc/yum.repos.d/CentOS-Base.repo https://mirrors.aliyun.com/repo/Centos-7.repo && \
    sed -i -e '/mirrors.cloud.aliyuncs.com/d' -e '/mirrors.aliyuncs.com/d' /etc/yum.repos.d/CentOS-Base.repo && \
    yum clean all && \
    yum makecache && \
    \
    echo "====> 安装基础开发和调试工具..." && \
    yum install -y --setopt=tsflags=nodocs \
        nc \
        telnet \
        lsof \
        iputils \
        net-tools \
        curl \
        wget \
        vim \
        procps-ng \
        tcpdump \
        bind-utils \
        unzip \
        tar \
        which \
        sudo \
        iproute && \
    \
    echo "====> 安装中文支持相关包..." && \
    yum install -y --setopt=tsflags=nodocs \
        glibc-common \
        kde-l10n-Chinese \
        wqy-zenhei-fonts \
        wqy-microhei-fonts && \
    \
    echo "====> 设置中文环境..." && \
    localedef -c -f UTF-8 -i zh_CN zh_CN.UTF-8 && \
    \
    echo "====> 清理YUM缓存和临时文件..." && \
    yum clean all && \
    rm -rf /var/cache/yum && \
    rm -rf /tmp/* /var/tmp/*

# 设置中文环境变量
ENV LANG=zh_CN.UTF-8 \
    LC_ALL=zh_CN.UTF-8

# 设置工作目录
WORKDIR /opt