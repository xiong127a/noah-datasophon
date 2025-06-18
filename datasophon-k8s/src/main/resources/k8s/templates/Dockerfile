# =================================================================
#  第一阶段：JDK准备阶段 (Builder)
#  使用CentOS 7作为基础，下载并清理JDK 1.8.0_333
# =================================================================
FROM harbor.norintech.com/bdp/centos:7 AS builder

# 设置工作目录
WORKDIR /tmp

# 1. 清理默认YUM源，配置阿里云YUM源，并安装下载工具
RUN rm -f /etc/yum.repos.d/*.repo && \
    curl -o /etc/yum.repos.d/CentOS-Base.repo https://mirrors.aliyun.com/repo/Centos-7.repo && \
    sed -i -e '/mirrors.cloud.aliyuncs.com/d' -e '/mirrors.aliyuncs.com/d' /etc/yum.repos.d/CentOS-Base.repo && \
    yum clean all

# 2. 下载并解压指定版本的JDK 1.8.0_333
# 可以替换为您内部的JDK下载URL
RUN curl -L -o jdk-8u333-linux-x64.tar.gz http://192.168.1.30/BDP/packages/jdk/jdk-8u333-linux-x64.tar.gz && \
    mkdir -p /opt/java && \
    tar -xzf jdk-8u333-linux-x64.tar.gz -C /opt/java && \
    rm -f jdk-8u333-linux-x64.tar.gz

# 3. 清理JDK中不必要的文件以减小体积
RUN echo "正在优化JDK..." && \
    find /opt/java -name "*src.zip" -delete && \
    find /opt/java -name "*.diz" -delete && \
    rm -rf /opt/java/jdk1.8.0_333/jre/lib/plugin.jar \
           /opt/java/jdk1.8.0_333/jre/lib/ext/jfxrt.jar \
           /opt/java/jdk1.8.0_333/jre/bin/javaws \
           /opt/java/jdk1.8.0_333/jre/lib/javaws.jar \
           /opt/java/jdk1.8.0_333/jre/lib/desktop \
           /opt/java/jdk1.8.0_333/jre/plugin \
           /opt/java/jdk1.8.0_333/jre/lib/deploy* \
           /opt/java/jdk1.8.0_333/jre/lib/*javafx* \
           /opt/java/jdk1.8.0_333/jre/lib/*jfx* \
           /opt/java/jdk1.8.0_333/jre/lib/amd64/libdecora_sse.so \
           /opt/java/jdk1.8.0_333/jre/lib/amd64/libprism_*.so \
           /opt/java/jdk1.8.0_333/jre/lib/amd64/libfxplugins.so \
           /opt/java/jdk1.8.0_333/jre/lib/amd64/libglass.so \
           /opt/java/jdk1.8.0_333/jre/lib/amd64/libgstreamer-lite.so \
           /opt/java/jdk1.8.0_333/jre/lib/amd64/libjavafx*.so \
           /opt/java/jdk1.8.0_333/jre/lib/amd64/libjfx*.so \
           /opt/java/jdk1.8.0_333/demo \
           /opt/java/jdk1.8.0_333/sample \
           /opt/java/jdk1.8.0_333/src.zip \
           /opt/java/jdk1.8.0_333/man && \
    echo "JDK优化完成."


# =================================================================
#  第二阶段：最终镜像构建
#  使用一个干净的CentOS 7作为基础，只拷贝优化后的JDK
# =================================================================
FROM harbor.norintech.com/bdp/centos:7

# 作者信息
LABEL maintainer="renxiangpeng"

# 1. 清理默认YUM源，并从builder阶段复制已配置好的阿里云YUM源
RUN rm -f /etc/yum.repos.d/*.repo
COPY --from=builder /etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo

# 2. 从builder阶段复制已清理的JDK 1.8.0_333
COPY --from=builder /opt/java/jdk1.8.0_333 /usr/local/jdk1.8.0_333

# 3. 设置JAVA_HOME和相关环境变量
ENV JAVA_HOME=/usr/local/jdk1.8.0_333
ENV JRE_HOME=$JAVA_HOME/jre
ENV CLASSPATH=.:$JAVA_HOME/lib:$JRE_HOME/lib
ENV PATH=$JAVA_HOME/bin:$PATH

# 4. 验证Java安装
RUN java -version