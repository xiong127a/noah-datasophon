# =================================================================
#  阶段一：工作台 (Builder) - 使用CentOS 7作为基础
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk AS builder

# 设置工作目录
WORKDIR /opt/datasophon

# 设置Elasticsearch版本
ENV ELASTICSEARCH_VERSION 7.16.2

# --- 在单个RUN指令中完成所有下载、解压和清理操作 ---
RUN \
    # 创建elastic用户和组
    groupadd -r -g 2007 elastic && \
    useradd -r -u 2007 -g 2007 elastic && \
    \
    # 下载软件包
    echo "====> 下载软件包..." && \
    curl -L -s -o elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz http://192.168.1.30/BDP/packages/1.2.1/elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz && \
    \
    # 解压软件包
    echo "====> 解压软件包..." && \
    tar -zxf elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz && \
    \
    # 删除原始压缩包
    echo "====> 清理文件..." && \
    rm -f elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz && \
    \
    # 设置权限
    echo "====> 设置权限..." && \
    chmod -R 755 /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION} && \
    chown -R elastic:elastic /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION} && \
    echo "====> 构建阶段完成."

# =================================================================
#  阶段二：最终产物 (Final Image) - 基于CentOS 7
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk

WORKDIR /opt/datasophon

# 设置Elasticsearch版本
ENV ELASTICSEARCH_VERSION 7.16.2

# 创建elastic用户和组
RUN groupadd -r -g 2007 elastic && \
    useradd -r -u 2007 -g 2007 elastic

# --- 精确拷贝，只从builder阶段复制清理后的目录 ---
COPY --from=builder /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION} /opt/datasophon/elasticsearch-${ELASTICSEARCH_VERSION}

# --- 设置软链接 ---
RUN ln -s elasticsearch-${ELASTICSEARCH_VERSION} elasticsearch

# 设置环境变量
ENV JAVA_HOME=/opt/datasophon/elasticsearch/jdk \
    ELASTICSEARCH_HOME=/opt/datasophon/elasticsearch \
    PATH=$JAVA_HOME/bin:$PATH:/opt/datasophon/elasticsearch/bin