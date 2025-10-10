# StreamPark 用户指南

## 概述

本指南将帮助您在大数据平台中部署、配置和使用StreamPark组件。StreamPark是一个现代化的流处理应用开发与管理平台，围绕Apache Flink和Spark Streaming构建，专注于流式数据处理领域，旨在帮助您更高效地开发、部署和管理流处理应用。

## 安装与部署

### 环境准备

在安装StreamPark之前，请确保您的环境满足以下条件：

* **Java环境**：JDK 8或JDK 11（推荐JDK 8）
* **数据库**：MySQL 5.7+或PostgreSQL 9.6+
* **Web服务器**：Nginx或Apache HTTP Server（可选，用于反向代理）
* **计算引擎**：
  * Apache Flink 1.12.x/1.13.x/1.14.x/1.15.x
  * Apache Spark 2.4.x/3.x（如需支持Spark Streaming）
* **资源管理**：YARN、Kubernetes或独立集群
* **资源要求**：
  * 最低配置：2核CPU、4GB内存、20GB磁盘空间
  * 推荐配置：4核以上CPU、8GB以上内存、50GB以上磁盘空间

### 通过DataSophon平台部署

DataSophon平台提供了便捷的方式部署StreamPark：

1. 登录DataSophon管理界面
2. 导航至"集群管理" > "添加服务"
3. 在可用组件列表中选择"StreamPark"
4. 按照向导指引配置相关参数：
   * 选择版本
   * 选择安装节点
   * 配置数据库连接
   * 配置Flink环境
   * 设置管理员账户
5. 提交并等待部署完成
6. 部署完成后，访问StreamPark Web控制台（默认端口10000）

### 手动安装步骤

如需手动安装StreamPark，请按照以下步骤操作：

1. 下载安装包：

```bash
# 下载最新版本的StreamPark
wget https://dlcdn.apache.org/incubator/streampark/x.y.z/apache-streampark-x.y.z-incubating-bin.tar.gz

# 解压安装包
tar -xzvf apache-streampark-x.y.z-incubating-bin.tar.gz
cd apache-streampark-x.y.z-incubating
```

2. 配置数据库：

```bash
# 创建数据库
mysql -uroot -p -e "CREATE DATABASE streampark DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 初始化数据库架构
mysql -uroot -p streampark < script/mysql/streampark.sql
```

3. 修改配置文件：

```bash
# 编辑配置文件
vi conf/application.yaml

# 配置数据库连接
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/streampark?useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    
# 配置Flink和Spark路径
streampark:
  flink:
    home: /path/to/flink
  spark:
    home: /path/to/spark
```

4. 启动服务：

```bash
# 启动StreamPark服务
bin/startup.sh

# 检查服务状态
bin/status.sh
```

5. 访问Web界面：

打开浏览器，访问 `http://服务器IP:10000`，默认管理员账户为 `admin/streampark`。

### 高可用部署

对于生产环境，建议采用高可用部署方式：

1. 多节点部署：

```bash
# 在多个节点上部署StreamPark
# 节点1
bin/startup.sh

# 节点2
bin/startup.sh
```

2. 配置负载均衡：

```nginx
# Nginx配置示例
upstream streampark {
    server streampark-node1:10000;
    server streampark-node2:10000;
}

server {
    listen 80;
    server_name streampark.example.com;
    
    location / {
        proxy_pass http://streampark;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

3. 配置共享会话：

```yaml
# application.yaml 配置
spring:
  session:
    store-type: redis
    redis:
      host: redis-server
      port: 6379
```

## 基本配置

### 系统配置

StreamPark的系统配置项主要包括：

#### 基础设置

通过管理界面进行配置：

1. 登录StreamPark管理界面
2. 进入"系统设置" > "系统参数"
3. 配置以下参数：
   * 系统名称
   * 系统Logo
   * 系统主题
   * 登录页面背景
   * 系统语言

#### 邮件通知配置

```yaml
# 编辑 application.yaml
spring:
  mail:
    host: smtp.example.com
    port: 465
    username: notification@example.com
    password: your_password
    properties:
      mail.smtp.ssl.enable: true
```

#### 告警配置

可以配置多种告警方式：

1. 进入"系统设置" > "告警配置"
2. 添加告警通道：
   * 邮件告警
   * 钉钉告警
   * 企业微信告警
   * 短信告警
   * Slack告警

### 用户与权限

#### 用户管理

1. 进入"系统设置" > "用户管理"
2. 点击"添加用户"按钮
3. 填写用户信息：
   * 登录账号
   * 用户名称
   * 密码
   * 邮箱
   * 手机号
   * 分配角色

#### 角色管理

StreamPark使用基于角色的访问控制（RBAC）：

1. 进入"系统设置" > "角色管理"
2. 点击"添加角色"按钮
3. 设置角色信息：
   * 角色名称
   * 角色描述
   * 权限分配
     * 应用管理权限
     * 项目管理权限
     * 集群管理权限
     * 系统设置权限

#### 团队管理

1. 进入"系统设置" > "团队管理"
2. 点击"添加团队"按钮
3. 设置团队信息：
   * 团队名称
   * 团队描述
   * 团队成员
   * 资源配额

### 集群管理

#### Flink集群管理

1. 进入"集群管理" > "Flink集群"
2. 点击"添加集群"按钮
3. 配置集群信息：
   * 集群名称
   * 集群地址
   * 集群类型（Standalone、YARN、Kubernetes）
   * 版本信息
   * 集群配置

#### YARN队列配置

如果使用YARN作为资源管理器：

1. 进入"集群管理" > "YARN队列"
2. 点击"添加队列"按钮
3. 配置队列信息：
   * 队列名称
   * 队列标签
   * 队列描述
   * 队列优先级
   * 所属集群

#### Kubernetes命名空间

如果使用Kubernetes作为资源管理器：

1. 进入"集群管理" > "Kubernetes命名空间"
2. 点击"添加命名空间"按钮
3. 配置命名空间信息：
   * 命名空间名称
   * Kubernetes集群
   * 资源配额

## 应用开发与管理

### 项目管理

在StreamPark中，项目是应用的逻辑分组：

#### 创建项目

1. 进入"应用管理" > "项目管理"
2. 点击"添加项目"按钮
3. 填写项目信息：
   * 项目名称
   * 项目描述
   * 项目负责人
   * 项目成员

#### 项目资源管理

1. 进入项目详情页
2. 添加项目资源：
   * 配置文件
   * 依赖包
   * UDF函数
   * 参考文档

### 应用开发

StreamPark支持两种应用开发方式：直接开发和使用脚手架。

#### 使用StreamPark脚手架

1. 从Maven原型创建项目：

```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.apache.streampark \
  -DarchetypeArtifactId=streampark-flink-quickstart \
  -DarchetypeVersion=x.y.z \
  -DgroupId=com.example \
  -DartifactId=my-streampark-app \
  -Dversion=1.0-SNAPSHOT
```

2. 项目结构：

```
my-streampark-app/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   ├── resources/
│   │   └── scala/
│   │   └── com/example/
│   │   └── jobs/          # 作业主类
│   │   └── functions/     # 自定义函数
│   │   └── models/        # 数据模型
│   │   └── utils/         # 工具类
│   │   └── resources/
│   │       └── application.conf   # 主配置文件
│   │       └── dev.conf           # 开发环境配置
│   │       └── test.conf          # 测试环境配置
│   │       └── prod.conf          # 生产环境配置
│   └── test/
├── pom.xml
└── README.md
```

3. 编写流处理作业：

```scala
// Scala示例
package com.example

import org.apache.streampark.flink.core.scala.FlinkStreaming
import org.apache.flink.streaming.api.scala._

object MyStreamingJob extends FlinkStreaming {

  override def handle(): Unit = {
    // 设置检查点
    checkpoint.interval = 60000
    
    // 创建数据源
    val source = kafka.getSource[String]("kafka_source")
    
    // 处理逻辑
    val result = source
      .map(x => {
        // 处理逻辑
        x
      })
    
    // 输出到目标
    result.addSink(kafka.sink[String]("kafka_sink"))
  }
}
```

4. 配置文件（`application.conf`）：

```hocon
# 公共配置
env {
  parallelism = 4
  job.name = "MyStreamingJob"
}

# Kafka源配置
kafka {
  kafka_source {
    bootstrap.servers = "kafka1:9092,kafka2:9092"
    topic = "input-topic"
    group.id = "my-group"
    auto.offset.reset = "latest"
  }
  
  kafka_sink {
    bootstrap.servers = "kafka1:9092,kafka2:9092"
    topic = "output-topic"
  }
}
```

5. 构建项目：

```bash
mvn clean package
```

#### 在StreamPark UI中开发应用

1. 进入"应用管理" > "创建应用"
2. 选择应用类型："Flink SQL"或"Flink JAR"
3. 填写基本信息：
   * 应用名称
   * 应用版本
   * 项目归属
   * 应用标签
   * 应用描述
4. 对于Flink SQL应用，直接在UI中编写SQL：

```sql
-- 创建源表
CREATE TABLE source_table (
  id STRING,
  name STRING,
  age INT,
  event_time TIMESTAMP(3),
  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
  'connector' = 'kafka',
  'topic' = 'input-topic',
  'properties.bootstrap.servers' = 'kafka1:9092,kafka2:9092',
  'properties.group.id' = 'my-group',
  'scan.startup.mode' = 'latest-offset',
  'format' = 'json'
);

-- 创建目标表
CREATE TABLE sink_table (
  id STRING,
  name STRING,
  age INT,
  event_time TIMESTAMP(3)
) WITH (
  'connector' = 'kafka',
  'topic' = 'output-topic',
  'properties.bootstrap.servers' = 'kafka1:9092,kafka2:9092',
  'format' = 'json'
);

-- 处理逻辑
INSERT INTO sink_table
SELECT 
  id,
  name,
  age,
  event_time
FROM source_table
WHERE age > 18;
```

5. 对于Flink JAR应用，上传构建好的JAR包

### 应用配置

StreamPark支持集中化的应用配置管理：

#### 环境变量配置

1. 在应用详情页，进入"环境变量"标签
2. 添加环境变量：
   * 变量名
   * 变量值
   * 描述

#### 依赖管理

1. 在应用详情页，进入"依赖管理"标签
2. 上传应用依赖的JAR包或配置文件

#### 资源配置

1. 在应用详情页，进入"资源配置"标签
2. 配置应用资源：
   * JobManager内存
   * TaskManager内存
   * 并行度
   * 槽位数
   * 重启策略

### 应用部署

StreamPark支持多种部署模式：

#### Session模式

1. 在应用详情页，选择"部署"
2. 部署模式选择"Session"
3. 选择目标集群和会话
4. 配置部署参数
5. 点击"部署"按钮

#### Per-Job模式

1. 在应用详情页，选择"部署"
2. 部署模式选择"Per-Job"
3. 选择目标集群和YARN队列
4. 配置部署参数
5. 点击"部署"按钮

#### Application模式

1. 在应用详情页，选择"部署"
2. 部署模式选择"Application"
3. 选择目标集群
4. 配置部署参数
5. 点击"部署"按钮

#### Kubernetes部署

1. 在应用详情页，选择"部署"
2. 部署模式选择"Kubernetes"
3. 选择Kubernetes集群和命名空间
4. 配置部署参数
5. 点击"部署"按钮

### 版本管理

StreamPark支持应用的版本管理：

1. 在应用详情页，进入"版本"标签
2. 查看历史版本列表
3. 可以执行以下操作：
   * 查看版本详情
   * 比较版本差异
   * 回滚到历史版本
   * 基于历史版本创建新版本

## 运行与监控

### 应用启动

可以通过以下方式启动应用：

1. 在应用列表页，找到目标应用，点击"启动"按钮
2. 在应用详情页，点击"操作" > "启动"

### 应用停止

可以通过以下方式停止应用：

1. 在应用列表页，找到目标应用，点击"停止"按钮
2. 在应用详情页，点击"操作" > "停止"

### 故障重启

当应用出现故障时：

1. 在应用列表页，找到故障应用，点击"重启"按钮
2. 配置重启参数：
   * 是否从保存点恢复
   * 选择保存点路径
   * 重启策略

### 作业监控

StreamPark提供了全面的作业监控功能：

#### 实时状态监控

1. 在应用详情页，进入"监控"标签
2. 查看实时状态信息：
   * 作业状态
   * 运行时间
   * 处理记录数
   * 吞吐量
   * 延迟指标

#### 作业拓扑图

1. 在应用详情页，进入"Flink UI"标签
2. 查看作业的DAG拓扑图
3. 查看各算子的执行情况和性能指标

#### 异常监控

1. 进入"运维中心" > "异常监控"
2. 查看所有作业的异常情况
3. 设置异常告警规则

### 告警配置

可以为应用配置各类告警：

1. 在应用详情页，进入"告警"标签
2. 配置告警规则：
   * 状态变更告警
   * 延迟阈值告警
   * 吞吐量告警
   * 失败重试告警
   * 资源使用告警
3. 配置告警接收方式和接收人

## 高级功能

### 保存点管理

StreamPark提供完善的保存点管理功能：

1. 在应用详情页，进入"保存点"标签
2. 手动触发保存点：
   * 点击"创建保存点"按钮
   * 指定保存点路径（可选）
   * 选择触发模式
3. 查看保存点历史记录
4. 基于保存点恢复应用：
   * 在应用重启时选择保存点
   * 指定恢复策略

### 自动化运维

StreamPark支持自动化运维功能：

#### 自动重启策略

1. 在应用详情页，进入"配置" > "重启策略"
2. 设置自动重启策略：
   * 固定延迟重启
   * 失败率重启
   * 指数延迟重启
   * 不重启（禁用）

#### 定时启停策略

1. 在应用详情页，进入"配置" > "调度策略"
2. 配置定时调度：
   * 定时启动时间
   * 定时停止时间
   * 周期设置
   * 是否启用

#### 资源自动扩缩容

如果在Kubernetes环境中部署：

1. 在应用详情页，进入"配置" > "扩缩容策略"
2. 配置自动扩缩容规则：
   * 触发条件
   * 最小/最大副本数
   * 冷却时间
   * 扩缩容步长

### 日志管理

StreamPark提供集中化的日志管理功能：

1. 在应用详情页，进入"日志"标签
2. 查看各类日志：
   * 作业运行日志
   * TaskManager日志
   * JobManager日志
   * 历史日志
3. 日志检索和分析：
   * 关键字搜索
   * 时间范围过滤
   * 日志级别过滤
   * 日志下载

### API集成

StreamPark提供RESTful API，用于与其他系统集成：

```bash
# 获取Token
curl -X POST "http://{StreamPark地址}/api/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "streampark"}'

# 获取应用列表
curl -X GET "http://{StreamPark地址}/api/flink/app/list" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {Token}"

# 启动应用
curl -X POST "http://{StreamPark地址}/api/flink/app/{appId}/start" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {Token}"

# 停止应用
curl -X POST "http://{StreamPark地址}/api/flink/app/{appId}/stop" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {Token}"
```

## 最佳实践

### 开发最佳实践

#### 项目结构规范

推荐的项目结构：

```
project/
├── src/
│   ├── main/
│   │   ├── java/ or scala/
│   │   │   └── com/example/
│   │   │       ├── jobs/          # 作业主类
│   │   │       ├── functions/     # 自定义函数
│   │   │       ├── models/        # 数据模型
│   │   │       └── utils/         # 工具类
│   │   └── resources/
│   │       ├── application.conf   # 主配置文件
│   │       ├── dev.conf           # 开发环境配置
│   │       ├── test.conf          # 测试环境配置
│   │       └── prod.conf          # 生产环境配置
│   └── test/
├── pom.xml
└── README.md
```

#### 状态管理建议

1. 合理设计状态：
   * 避免过大的状态
   * 使用TTL管理状态生命周期
   * 考虑增量更新状态

2. 检查点配置：
   * 根据业务需求设置检查点间隔
   * 配置检查点超时时间
   * 选择适合的检查点存储位置
   * 考虑增量检查点

#### 性能优化

1. 并行度调优：
   * 根据数据量和集群资源设置
   * 避免过高或过低的并行度
   * 考虑算子链的影响

2. 资源配置：
   * 合理设置TaskManager和JobManager内存
   * 优化堆内存和堆外内存比例
   * 配置网络缓冲区大小

### 运维最佳实践

#### 高可用配置

1. Flink高可用配置：
   * 配置ZooKeeper高可用
   * 设置JobManager故障转移
   * 配置多个TaskManager

2. StreamPark高可用：
   * 部署多个StreamPark节点
   * 配置负载均衡
   * 使用共享存储保存配置

#### 监控与告警

1. 建立多层次监控：
   * 应用级监控
   * 资源级监控
   * 系统级监控

2. 告警策略：
   * 配置合理的告警阈值
   * 设置告警升级机制
   * 建立告警响应流程

#### 资源规划

1. 集群资源规划：
   * 评估总体资源需求
   * 预留足够的资源缓冲
   * 考虑高峰期资源需求

2. 应用资源分配：
   * 基于数据量估算资源需求
   * 通过测试确定最佳配置
   * 定期评估和调整资源配置

### 安全最佳实践

1. 用户权限管理：
   * 最小权限原则
   * 定期审核用户权限
   * 敏感操作双人确认

2. 数据安全：
   * 敏感数据加密存储
   * 传输数据TLS加密
   * 实施数据访问审计

## 常见问题排查

### 应用提交失败

可能的原因与解决方法：

1. 资源不足：
   * 检查YARN或Kubernetes集群资源
   * 调整应用资源配置
   * 释放未使用的资源

2. 配置错误：
   * 检查集群配置是否正确
   * 验证应用参数是否有误
   * 检查依赖项是否完整

3. 权限问题：
   * 确认用户权限设置
   * 检查文件系统权限
   * 验证集群访问权限

### 应用运行异常

排查步骤：

1. 检查作业日志：
   * 查看异常堆栈信息
   * 分析错误信息和上下文
   * 确定错误发生位置

2. 监控应用指标：
   * 检查资源使用情况
   * 分析处理延迟指标
   * 查看异常处理统计

3. 检查数据源：
   * 验证数据源连接状态
   * 检查数据格式是否符合预期
   * 确认数据延迟情况

### 性能问题

性能优化思路：

1. 反压问题：
   * 识别瓶颈算子
   * 调整并行度
   * 优化算子实现
   * 考虑数据倾斜问题

2. 资源不足：
   * 增加内存配置
   * 调整CPU分配
   * 优化资源使用效率

3. 网络问题：
   * 检查网络带宽使用
   * 调整数据分区策略
   * 优化序列化配置

## 参考资料

* [StreamPark官方文档](https://streampark.apache.org/docs/introduction)
* [StreamPark GitHub仓库](https://github.com/apache/streampark)
* [Apache Flink文档](https://flink.apache.org/docs/stable/)
* [Apache Spark文档](https://spark.apache.org/docs/latest/) 