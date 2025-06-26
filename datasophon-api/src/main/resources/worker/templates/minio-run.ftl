#!/bin/bash

# 设置MinIO的配置参数
export MINIO_ROOT_USER=${minioAccessKey}
export MINIO_ROOT_PASSWORD=${minioSecretKey}
export MINIO_PROMETHEUS_AUTH_TYPE="public"   #加入这行环境变量，“public”表示Prometheus访问minio集群可以不通过身份验证

mv /opt/datasophon/minio/minio.log "/opt/datasophon/minio/minio_$(date +\%Y\%m\%d_\%H\%M\%S).log" 2>/dev/null

/opt/datasophon/minio/minio server --config-dir /opt/datasophon/minio/etc \
        --address "0.0.0.0:${minioApiPort}" --console-address ":${minioConsolePort}" \
        ${minioDataPaths} > /opt/datasophon/minio/minio.log 2>&1 &