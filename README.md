# Noah 大数据平台

Noah 是一个开源的大数据集群管理平台，提供大数据组件的自动化部署、配置管理、服务监控和运维管理能力。

## 技术栈

- **后端**: JDK 21, Spring Boot 3.x, Apache Pekko
- **前端**: Vue 3, Element Plus, TypeScript
- **数据库**: MySQL 8.0+ / DM8
- **消息通信**: Apache Pekko Remote
- **任务调度**: db-scheduler

## 系统架构

Noah 采用分布式主从架构：

- **API 模块**: 管理节点，提供 Web UI、REST API、集群管理、任务调度等功能
- **Worker 模块**: 工作节点，部署在各台服务器上，负责执行服务安装、配置、启停等操作

### 通信机制

- API → Worker: 单向通信，通过 Pekko Remote (端口 2552) 发送命令
- Worker → API: 仅 HTTP 请求用于下载安装包和配置文件
- 架构优势: 防火墙友好，Worker 无需主动连接 API 端口

## 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+ 或 DM8 数据库
- Node.js 18+ (用于前端构建)

## 快速启动

### 1. 数据库初始化

```bash
# MySQL
mysql -u root -p < datasophon-api/src/main/resources/db/migration/mysql/3.0.0/*.sql

# DM
# 使用 DM 提供的工具导入 datasophon-api/src/main/resources/db/migration/dm/3.0.0/*.sql
```

### 2. 配置文件

编辑 `conf/profiles/application-config.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/datasophon?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    
server:
  port: 8081
```

### 3. 启动 API 服务

```bash
# 开发环境
cd datasophon-api
mvn spring-boot:run

# 生产环境
sh bin/datasophon-api.sh start
```

访问 http://localhost:8081 即可使用 Web UI。

### 4. 启动 Worker 服务

在各个工作节点上：

```bash
# 配置 Worker
vi datasophon-worker/conf/common.properties
# 设置 masterHost, masterWebPort, clusterId

# 启动 Worker
sh bin/datasophon-worker.sh start
```

## 部署指南

### Maven 打包

```bash
# 完整打包（包含前端）
mvn clean package -DskipTests

# 仅打包后端
mvn clean package -DskipTests -pl datasophon-api,datasophon-worker -am
```

### 部署结构

```
/opt/datasophon/
├── datasophon-manager/          # API 模块
│   ├── bin/
│   │   └── datasophon-api.sh
│   ├── conf/
│   │   └── application.yml
│   ├── lib/
│   └── static/                  # 前端资源
│
└── datasophon-worker/           # Worker 模块（部署在各节点）
    ├── bin/
    │   └── datasophon-worker.sh
    ├── conf/
    │   └── common.properties
    └── lib/
```

### 部署命令

```bash
# 解压安装包
tar -zxvf datasophon-manager-1.2.1.tar.gz -C /opt/datasophon/

# API 服务
cd /opt/datasophon/datasophon-manager
sh bin/datasophon-api.sh start

# Worker 服务（在各个节点）
cd /opt/datasophon/datasophon-worker
sh bin/datasophon-worker.sh start
```

## 端口说明

| 模块 | 端口 | 用途 |
|------|------|------|
| API | 8081 | Web UI / REST API |
| Worker | 2552 | Pekko Remote (接收 API 命令) |

**注意**: Worker 端口 2552 需要对 API 节点开放，API 端口无需对 Worker 开放。

## Docker 部署

```bash
# 构建镜像
docker build -f Dockerfile-api -t noah-bigdata/api:latest .
docker build -f Dockerfile-ui -t noah-bigdata/ui:latest .

# 使用 Docker Compose
cd deploy/compose
docker-compose up -d
```

## Kubernetes 部署

```bash
# 部署到 Kubernetes
kubectl apply -f deploy/kubernetes/
```

详见 `deploy/kubernetes/README.md`

## 支持的大数据组件

- **计算引擎**: Spark, Flink, Trino, Presto
- **存储**: HDFS, HBase, Doris, StarRocks, ClickHouse
- **调度**: Dolphinscheduler, Airflow
- **消息队列**: Kafka, Pulsar
- **协调服务**: ZooKeeper
- **监控**: Prometheus, Grafana

## 开发指南

### 前端开发

```bash
cd datasophon-ui/nextjs-app
npm install
npm run dev
```

访问 http://localhost:3000

### 后端开发

```bash
cd datasophon-api
mvn spring-boot:run -Dspring.profiles.active=dev
```

## 许可证

本项目采用 Apache License 2.0 许可证，详见 [LICENSE](LICENSE) 文件。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

- 项目主页: https://github.com/datasophon/datasophon
- 文档: https://datasophon.github.io
