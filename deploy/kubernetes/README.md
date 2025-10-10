# Datasophon Kubernetes部署指南

本文档提供了如何在Kubernetes环境中部署Datasophon的详细说明，采用前后端分离架构。

## 前提条件

1. 已安装Maven 3.6+
2. 已安装Docker
3. 已配置Kubernetes集群
4. 已创建Harbor镜像仓库账号 (harbor.norintech.com)

## 构建流程

### 1. 构建与推送镜像

#### 后端API镜像

```bash
# 构建API应用
cd noah-bigdata-platform
mvn clean package -DskipTests

# 构建并推送API镜像
cd datasophon-api
mvn docker:build docker:push
```

#### 前端UI镜像

```bash
# 构建前端UI并创建镜像
cd datasophon-ui
mvn clean install -Pfrontend-build
mvn docker:build docker:push
```

### 2. 修改配置

根据实际环境修改 `deploy/kubernetes/datasophon-deployment.yaml` 中的配置信息：

- 数据库连接信息
- 存储配置
- 服务域名

## 部署到Kubernetes

```bash
# 创建namespace（如果需要）
kubectl create namespace datasophon

# 应用部署配置
kubectl apply -f deploy/kubernetes/datasophon-deployment.yaml -n datasophon
```

## 验证部署

```bash
# 检查所有Pod是否正常运行
kubectl get pods -n datasophon

# 检查服务
kubectl get svc -n datasophon

# 检查Ingress
kubectl get ing -n datasophon
```

## 访问应用

应用可通过配置的Ingress域名访问：`http://datasophon.example.com`

## 故障排除

如果遇到问题，可以检查Pod日志：

```bash
kubectl logs -f [pod-name] -n datasophon
```

## 注意事项

1. 确保Harbor镜像仓库凭证已配置到Kubernetes中
2. 默认配置适用于测试环境，生产环境可能需要调整资源配置
3. 前端UI访问API的配置在nginx.conf中，如API地址变更需相应修改 