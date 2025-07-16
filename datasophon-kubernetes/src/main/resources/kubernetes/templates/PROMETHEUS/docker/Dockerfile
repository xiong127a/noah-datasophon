# =================================================================
#  阶段一：工作台 (Builder) - 使用CentOS 7作为基础
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk AS builder

# 设置工作目录
WORKDIR /opt/datasophon

# --- 在单个RUN指令中完成所有下载、解压和清理操作 ---
RUN \
    # 下载软件包
    echo "====> 下载软件包..." && \
    curl -L -s -o prometheus-2.17.2.tar.gz http://192.168.1.30/BDP/packages/1.2.1/prometheus-2.17.2.tar.gz && \
    curl -L -s -o datasophon-worker.tar.gz http://192.168.1.30/BDP/packages/1.2.1/datasophon-worker.tar.gz && \
    \
    # 解压软件包
    echo "====> 解压软件包..." && \
    tar -zxf prometheus-2.17.2.tar.gz && \
    tar -zxf datasophon-worker.tar.gz -C /tmp && \
    \
    # 查找文件位置
    echo "====> 查找node_exporter文件位置..." && \
    ls -la /tmp/datasophon-worker/node/x86/ && \
    \
    # 创建node_exporter目录在prometheus-2.17.2下
    mkdir -p /opt/datasophon/prometheus-2.17.2/node_exporter && \
    mkdir -p /opt/datasophon/prometheus-2.17.2/node_exporter/logs && \
    mkdir -p /opt/datasophon/prometheus-2.17.2/node_exporter/pid && \
    \
    # 直接复制正确路径的文件
    echo "====> 复制node_exporter和control.sh文件..." && \
    cp /tmp/datasophon-worker/node/x86/node_exporter /opt/datasophon/prometheus-2.17.2/node_exporter/ && \
    cp /tmp/datasophon-worker/node/x86/control.sh /opt/datasophon/prometheus-2.17.2/node_exporter/ && \
    \
    # 删除原始压缩包和临时文件
    echo "====> 清理文件..." && \
    rm -f prometheus-2.17.2.tar.gz datasophon-worker.tar.gz && \
    rm -rf /tmp/datasophon-worker && \
    \
    # 设置权限
    echo "====> 设置权限..." && \
    chmod -R 755 /opt/datasophon/prometheus-2.17.2 && \
    chmod +x /opt/datasophon/prometheus-2.17.2/node_exporter/node_exporter && \
    chmod +x /opt/datasophon/prometheus-2.17.2/node_exporter/control.sh && \
    echo "====> 构建阶段完成."

# =================================================================
#  阶段二：最终产物 (Final Image) - 基于CentOS 7
# =================================================================
FROM harbor.norintech.com/bdp/centos7:java-1.8.0-openjdk

WORKDIR /opt/datasophon

# --- 精确拷贝，只从builder阶段复制清理后的目录 ---
COPY --from=builder /opt/datasophon/prometheus-2.17.2 /opt/datasophon/prometheus-2.17.2

# --- 设置用户、权限和软链接 ---
RUN ln -s prometheus-2.17.2 prometheus

# 设置环境变量
ENV PROMETHEUS_HOME=/opt/datasophon/prometheus \
    PATH=$PATH:/opt/datasophon/prometheus