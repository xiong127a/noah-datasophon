# NOAHJOB 用户指南

## 概述

本指南将帮助您在大数据平台中部署、配置和使用NOAHJOB组件。NOAHJOB是北京中兵数科公司基于Apache DolphinScheduler深度定制改造的企业级工作流调度平台，专为军工行业、政府和大型企业设计，提供安全可靠的任务调度解决方案。

## 安装与部署

### 环境准备

在安装NOAHJOB之前，请确保您的环境满足以下条件：

* **操作系统**：
  * 支持CentOS 7.x/8.x、RedHat 7.x/8.x、统信UOS、麒麟OS等
  * 国产化环境建议使用统信UOS V20或银河麒麟V10
* **Java环境**：JDK 1.8+（推荐使用OpenJDK 8 或华为毕昇JDK）
* **数据库**：
  * PostgreSQL 9.4+
  * MySQL 5.7+
  * 国产数据库如达梦、人大金仓等
* **注册中心**：ZooKeeper 3.5.8+
* **硬件配置**：
  * Master节点：4核CPU、8GB内存、100GB磁盘
  * Worker节点：8核CPU、16GB内存、200GB磁盘（根据业务负载调整）
* **网络环境**：
  * 所有节点间网络互通
  * 支持主机名互相解析
  * 防火墙开放相关端口（Master: 5678、Worker: 1234、API: 12345等）

### 通过DataSophon平台部署

DataSophon平台提供了便捷的方式部署NOAHJOB：

1. 登录DataSophon管理界面
2. 导航至"集群管理" > "添加服务"
3. 在可用组件列表中选择"NOAHJOB"
4. 按照向导指引配置相关参数：
   * 选择安装节点（Master节点、Worker节点、API节点等）
   * 配置数据库连接信息
   * 配置ZooKeeper连接信息
   * 设置管理员账户信息
   * 配置资源和任务相关参数
5. 提交并等待部署完成
6. 部署完成后，点击"服务详情"查看各组件状态，确保所有组件正常运行

### 手动安装步骤

如需手动安装NOAHJOB，请按照以下步骤操作：

1. 下载安装包：

```bash
# 从指定地址下载NOAHJOB安装包
wget https://datasophon.com/downloads/noahjob/noahjob-1.0.0.tar.gz

# 解压安装包
tar -zxvf noahjob-1.0.0.tar.gz
cd noahjob-1.0.0
```

2. 修改配置文件：

```bash
# 编辑安装配置文件
vi conf/config/install_config.conf

# 设置以下关键参数：
# - 数据库连接信息
# - ZooKeeper连接信息
# - Master、Worker节点信息
# - 安装路径和日志路径
# - 安全认证相关配置
```

3. 修改环境变量配置：

```bash
# 编辑环境变量配置
vi conf/env/dolphinscheduler_env.sh

# 设置JDK路径、HADOOP_HOME、SPARK_HOME等环境变量
export JAVA_HOME=/path/to/jdk
export PATH=$JAVA_HOME/bin:$PATH
```

4. 执行安装脚本：

```bash
# 安装所有组件
./bin/install.sh all

# 或者分步安装各组件
./bin/install.sh zookeeper
./bin/install.sh master
./bin/install.sh worker
./bin/install.sh api
```

5. 启动服务：

```bash
# 启动所有服务
./bin/start-all.sh

# 或者分别启动各服务
./bin/start-master.sh
./bin/start-worker.sh
./bin/start-api.sh
```

6. 验证安装：

```bash
# 检查进程状态
jps

# 或使用状态检查脚本
./bin/status-all.sh
```

### 高可用部署

NOAHJOB支持高可用部署，建议生产环境采用以下部署方式：

1. 多Master节点部署：

```bash
# 编辑安装配置文件，设置多个Master节点
vi conf/config/install_config.conf

# 示例配置
masterServers="master1,master2,master3"
```

2. 多Worker节点部署：

```bash
# 编辑安装配置文件，设置多个Worker节点
vi conf/config/install_config.conf

# 示例配置
workerServers="worker1,worker2,worker3,worker4"
```

3. API服务负载均衡：

```bash
# 部署多个API服务节点
apiServers="api1,api2"

# 配置负载均衡器（如Nginx）
upstream noahjob_api {
    server api1:12345;
    server api2:12345;
}
```

4. 监控和故障转移配置：

```bash
# 编辑Master配置
vi conf/master.properties

# 设置失败检测间隔和超时时间
master.heartbeat.interval=10
master.task.commit.retryTimes=5
```

## 基本配置

### 安全与认证配置

NOAHJOB提供了增强的安全认证机制，配置如下：

#### 多因素认证

```properties
# 编辑security.properties文件
vi conf/security.properties

# 启用多因素认证
security.authentication.type=MULTI_FACTOR
security.authentication.mfa.enabled=true

# 配置认证方式（支持SMS、EMAIL、TOTP）
security.authentication.mfa.methods=SMS,TOTP
```

#### 密码策略

```properties
# 设置密码策略
security.password.min-length=12
security.password.require-numbers=true
security.password.require-uppercase=true
security.password.require-lowercase=true
security.password.require-special-chars=true
security.password.history-count=5
security.password.max-age-days=90
```

#### 数据传输加密

```properties
# 启用TLS/SSL
security.ssl.enabled=true
security.ssl.keystore.path=/path/to/keystore.jks
security.ssl.keystore.password=keystore_password
security.ssl.truststore.path=/path/to/truststore.jks
security.ssl.truststore.password=truststore_password
```

### 数据库配置

#### PostgreSQL配置

```properties
# 编辑application.properties文件
vi conf/application.properties

# PostgreSQL连接配置
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://postgresql-host:5432/noahjob
spring.datasource.username=noahjob
spring.datasource.password=password

# 连接池配置
spring.datasource.hikari.maximum-pool-size=100
spring.datasource.hikari.minimum-idle=10
```

#### 国产数据库配置（以达梦为例）

```properties
# 达梦数据库配置
spring.datasource.driver-class-name=dm.jdbc.driver.DmDriver
spring.datasource.url=jdbc:dm://dm-host:5236/NOAHJOB
spring.datasource.username=NOAHJOB
spring.datasource.password=password
```

### 资源配置

#### Worker资源配置

```properties
# 编辑worker.properties文件
vi conf/worker.properties

# 设置Worker资源限制
worker.max.concurrent.tasks=100
worker.exec.threads=100
worker.memory.limit=8G
worker.cpu.limit=8

# 设置预留资源
worker.reserved.memory=2G
```

#### 任务执行环境配置

```properties
# 任务执行环境隔离
worker.isolation.enabled=true
worker.isolation.type=CONTAINER

# 容器配置（使用Docker）
worker.docker.image=noahjob/worker-exec:1.0.0
worker.docker.network=host
worker.docker.volumes=/data:/data
```

## 用户与权限管理

### 用户管理

NOAHJOB提供了强大的用户管理功能，支持用户创建、角色分配和权限控制。

#### 用户创建

1. 登录NOAHJOB Web界面
2. 进入"安全中心" -> "用户管理"
3. 点击"创建用户"按钮
4. 填写用户信息：
   * 用户名
   * 密码（符合密码策略）
   * 邮箱
   * 手机号（用于短信验证）
   * 选择租户
   * 分配角色

#### 角色管理

1. 进入"安全中心" -> "角色管理"
2. 点击"创建角色"按钮
3. 设置角色名称和描述
4. 分配权限：
   * 项目创建权限
   * 工作流定义权限
   * 工作流实例权限
   * 任务实例权限
   * 资源中心权限
   * 数据源管理权限
   * 告警组管理权限

### 军工特色权限控制

#### 多级权限管理

NOAHJOB提供了军工特色的多级权限管理：

1. 进入"安全中心" -> "多级权限"
2. 配置以下权限级别：
   * 系统级权限
   * 项目级权限
   * 工作流级权限
   * 任务级权限
   * 数据级权限

#### 密级管理

针对军工行业的特殊需求，NOAHJOB支持数据密级管理：

1. 进入"安全中心" -> "密级管理"
2. 创建密级标签：
   * 公开级
   * 内部级
   * 秘密级
   * 机密级
   * 绝密级
3. 为资源分配密级：
   * 工作流密级标记
   * 资源文件密级标记
   * 数据源密级标记

#### 数据隔离

配置数据隔离策略，确保不同安全域的数据不会混合：

1. 进入"安全中心" -> "数据隔离"
2. 创建隔离域
3. 设置隔离规则
4. 配置访问控制策略

## 工作流设计与管理

### 创建项目

1. 登录NOAHJOB Web界面
2. 点击顶部菜单"项目管理"
3. 点击"创建项目"按钮
4. 填写项目信息：
   * 项目名称
   * 项目描述
   * 项目负责人
   * 项目密级（军工特色）
   * 选择项目所属部门
5. 点击"提交"创建项目

### 创建工作流

1. 进入项目详情页面
2. 点击"工作流定义"标签
3. 点击"创建工作流"按钮
4. 进入工作流设计器
5. 设置工作流属性：
   * 工作流名称
   * 工作流描述
   * 工作流优先级
   * 工作流密级（军工特色）
   * 超时告警设置
   * 重试策略

### 任务节点配置

NOAHJOB支持多种任务类型，以下是常用任务节点的配置方法：

#### Shell任务

1. 从左侧工具栏拖拽"Shell"任务到画布
2. 双击节点，打开配置面板
3. 设置任务属性：
   * 节点名称
   * 运行标志（正常/禁止执行）
   * 描述信息
   * 任务优先级
4. 编写Shell脚本或上传脚本文件
5. 设置环境变量
6. 配置资源文件引用
7. 设置失败策略和重试参数

#### SQL任务

1. 从左侧工具栏拖拽"SQL"任务到画布
2. 双击节点，打开配置面板
3. 设置任务属性
4. 选择数据源类型和数据源
5. 编写SQL语句
6. 设置SQL执行模式（单条/批量）
7. 设置SQL结果处理方式

#### Python任务

1. 从左侧工具栏拖拽"Python"任务到画布
2. 双击节点，打开配置面板
3. 设置任务属性
4. 选择Python版本
5. 编写Python脚本或上传脚本文件
6. 配置依赖包管理
7. 设置运行参数

#### 安全任务（军工特色）

NOAHJOB提供了特有的安全任务类型：

1. 从左侧工具栏拖拽"安全执行"任务到画布
2. 双击节点，打开配置面板
3. 设置任务属性
4. 选择安全执行模式：
   * 隔离环境执行
   * 加密执行
   * 安全审计执行
5. 配置任务内容和参数
6. 设置数据脱敏规则（如有需要）

### 任务依赖关系配置

1. 在画布中选择源任务节点
2. 拖拽连接线到目标任务节点
3. 配置依赖策略：
   * 成功后继续
   * 失败后继续
   * 条件判断依赖
4. 设置数据传递参数（如需要）

### 全局参数设置

1. 在工作流设计器中，点击"全局参数"标签
2. 添加全局参数，格式为`key=value`
3. 支持的参数类型：
   * 字符串
   * 数字
   * 日期
   * JSON对象
   * 表达式

### 调度配置

1. 在工作流定义页面，选择工作流，点击"定时"按钮
2. 创建定时调度：
   * 开始时间和结束时间
   * 时区设置
   * 定时周期（Cron表达式）
   * 失败策略
   * 优先级设置
   * Worker分组分配
3. 设置依赖调度：
   * 依赖工作流
   * 依赖任务
   * 依赖条件
4. 设置数据触发调度（基于数据就绪情况）

### 工作流版本管理

NOAHJOB提供了完善的版本管理功能：

1. 在工作流定义页面，点击工作流版本号
2. 查看历史版本列表
3. 对比不同版本的差异
4. 切换到历史版本
5. 从历史版本创建新版本
6. 版本回滚操作

## 工作流运行与监控

### 运行工作流

可以通过以下方式运行工作流：

1. **手动触发**：
   * 在工作流定义页面，选择工作流，点击"运行"按钮
   * 设置启动参数
   * 选择Worker组
   * 配置通知方式
   * 点击"确认"开始运行

2. **定时调度**：
   * 工作流会根据定时配置自动触发
   * 可在工作流实例页面查看调度执行情况

3. **API触发**：
   * 通过调用NOAHJOB的REST API触发工作流
   ```bash
   curl -X POST "http://{API服务地址}:12345/api/projects/{项目ID}/workflows/{工作流ID}/instances" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer {TOKEN}" \
     -d '{"paramMap": {"param1": "value1", "param2": "value2"}}'
   ```

### 紧急优先调度（军工特色）

NOAHJOB支持军工行业特有的紧急任务优先调度功能：

1. 在工作流运行页面，勾选"紧急任务"选项
2. 设置紧急级别（1-10，数字越大优先级越高）
3. 设置资源抢占策略：
   * 等待模式：等待资源释放
   * 抢占模式：中断低优先级任务并抢占资源
   * 混合模式：部分等待部分抢占
4. 点击"确认"，系统将按紧急优先策略调度任务

### 监控工作流实例

1. 点击"工作流实例"菜单
2. 查看所有运行的工作流实例列表
3. 使用筛选器按状态、时间、负责人等条件筛选
4. 点击实例ID进入实例详情页，查看：
   * 工作流DAG视图
   * 任务节点运行状态
   * 实时日志
   * 执行历史
   * 运行参数

### 任务实例监控

1. 点击"任务实例"菜单
2. 查看所有任务实例列表
3. 使用筛选器筛选任务
4. 点击任务ID查看任务详情：
   * 任务参数
   * 执行命令
   * 资源使用情况
   * 执行日志
   * 异常信息

### 告警管理

#### 告警配置

1. 进入"安全中心" -> "告警组管理"
2. 创建告警组
3. 配置告警渠道：
   * 邮件告警
   * 短信告警
   * 系统内部消息
   * 企业微信告警
   * 钉钉告警
   * 自定义告警插件
4. 设置告警级别和规则

#### 告警触发条件

可以为以下事件配置告警：

* 工作流超时
* 工作流失败
* 任务超时
* 任务失败
* 资源使用超限
* 安全事件告警（军工特色）
* 数据质量异常
* 系统异常

### 运行监控与报表

NOAHJOB提供了丰富的监控和报表功能：

1. 点击"监控中心"菜单
2. 查看以下监控面板：
   * Master服务状态
   * Worker服务状态
   * 数据库状态
   * Zookeeper状态
   * 任务队列状态
   * 资源使用情况
3. 点击"统计分析"查看报表：
   * 工作流成功率报表
   * 任务分布报表
   * 资源使用趋势
   * 执行时长分析
   * 自定义报表

## 高级功能

### 工作流补数

对历史数据进行回填处理：

1. 在工作流定义页面，选择工作流，点击"补数"
2. 设置补数参数：
   * 日期范围
   * 并行度
   * 依赖模式
   * 优先级
   * 通知策略
3. 点击"确认"，系统将生成对应日期的多个工作流实例

### 工作流重跑

针对失败的工作流实例进行重跑：

1. 在工作流实例页面，选择失败的工作流实例
2. 点击"重跑"按钮
3. 选择重跑方式：
   * 从失败节点开始重跑
   * 从指定节点开始重跑
   * 重跑全部节点
4. 设置重跑参数
5. 点击"确认"开始重跑

### 子工作流

创建和使用子工作流：

1. 首先创建子工作流定义
2. 在父工作流中，从左侧工具栏拖拽"子工作流"任务到画布
3. 双击节点，打开配置面板
4. 选择子工作流
5. 配置参数传递：
   * 父工作流向子工作流传递参数
   * 子工作流结果返回父工作流

### 定时停机与启动（军工特色）

针对军工行业的保密需求，支持定时停机与启动：

1. 进入"系统管理" -> "定时任务"
2. 点击"创建定时任务"
3. 选择任务类型："系统停机"或"系统启动"
4. 设置定时计划（Cron表达式）
5. 配置停机/启动策略：
   * 优雅停机（等待运行中任务完成）
   * 强制停机
   * 数据自动备份

### 数据血缘分析

跟踪和分析数据流转关系：

1. 进入"数据治理" -> "数据血缘"
2. 搜索特定工作流或数据源
3. 查看可视化的血缘关系图
4. 分析上下游依赖关系
5. 评估数据变更影响范围

### 资源文件管理

管理工作流所需的资源文件：

1. 进入"资源中心"
2. 创建资源文件目录
3. 上传资源文件（脚本、JAR包、配置文件等）
4. 为资源文件设置密级（军工特色）
5. 管理资源文件版本
6. 在任务中引用资源文件

## 与其他系统集成

### 与NOAHSYNC集成

NOAHJOB可以与NOAHSYNC数据同步平台无缝集成：

1. 进入"系统管理" -> "集成配置"
2. 点击"NOAHSYNC集成"
3. 配置连接参数：
   * NOAHSYNC服务地址
   * API密钥
   * 认证方式
4. 在工作流中使用NOAHSYNC任务：
   * 从左侧工具栏拖拽"NOAHSYNC"任务到画布
   * 选择NOAHSYNC同步作业
   * 配置参数和运行模式

### 与认证系统集成

支持与企业认证系统集成：

1. 进入"安全中心" -> "认证设置"
2. 选择认证类型：
   * LDAP/AD集成
   * OAuth2集成
   * SAML集成
   * 自定义SSO集成
3. 配置认证参数
4. 设置用户同步策略
5. 设置权限映射规则

### API集成

通过REST API与其他系统集成：

```bash
# 获取认证Token
curl -X POST "http://{API服务地址}:12345/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'

# 创建项目
curl -X POST "http://{API服务地址}:12345/api/projects" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{"name": "测试项目", "description": "API创建的项目"}'

# 获取工作流列表
curl -X GET "http://{API服务地址}:12345/api/projects/{项目ID}/workflows" \
  -H "Authorization: Bearer {TOKEN}"
```

## 国产化环境适配

### 国产操作系统适配

NOAHJOB针对国产操作系统进行了全面适配：

1. 统信UOS适配：
   * 适配UOS V20系统
   * 支持ARM和x86架构
   * 优化系统调用和资源管理

2. 银河麒麟适配：
   * 适配银河麒麟V10系统
   * 兼容特定内核版本
   * 针对性能调优

### 国产中间件适配

支持国产中间件替代方案：

1. 数据库适配：
   * 达梦数据库
   * 人大金仓
   * 神通数据库
   * GaussDB

2. 消息中间件适配：
   * TongLINK/Q
   * TurboMQ
   * openMQ

3. 分布式协调适配：
   * DCache
   * TongLINK/RV

### 国密算法支持

支持国家密码局认证的密码算法：

1. 对称加密：SM1、SM4
2. 非对称加密：SM2
3. 哈希算法：SM3
4. 数字签名：SM2DSA
5. 证书体系：SM2PKI

## 故障排查与维护

### 常见问题排查

#### Master无法启动

可能的原因和解决方案：

1. 数据库连接问题：
   * 检查数据库连接配置
   * 验证数据库服务状态
   * 确认网络连通性

2. ZooKeeper连接问题：
   * 检查ZooKeeper连接配置
   * 验证ZooKeeper服务状态
   * 检查Master在ZooKeeper中的注册信息

3. 端口冲突：
   * 检查5678端口是否被占用
   * 使用`netstat -anp | grep 5678`查看

4. 权限问题：
   * 检查安装目录权限
   * 确认运行用户权限

#### Worker无法启动

排查步骤：

1. 检查Worker配置：
   * Master地址配置
   * Worker监听端口
   * Worker组配置

2. 网络连接问题：
   * 确认Worker能够连接Master
   * 检查防火墙设置

3. 资源不足：
   * 检查系统内存和CPU资源
   * 查看磁盘空间是否充足

#### 任务执行失败

常见原因与排查：

1. 脚本错误：
   * 检查任务脚本语法
   * 手动执行脚本验证

2. 资源不足：
   * 检查Worker资源使用情况
   * 适当调整任务资源配额

3. 依赖服务异常：
   * 检查外部依赖服务状态
   * 验证网络连接和配置

4. 权限问题：
   * 检查任务执行用户权限
   * 验证资源文件访问权限

### 日志管理

NOAHJOB的日志目录结构：

* **Master日志**：`{NOAHJOB_HOME}/logs/master`
* **Worker日志**：`{NOAHJOB_HOME}/logs/worker`
* **API日志**：`{NOAHJOB_HOME}/logs/api-server`
* **任务日志**：`{NOAHJOB_HOME}/logs/workflow-task/{工作流ID}/{任务ID}`

常用日志查看命令：

```bash
# 查看Master日志
tail -f {NOAHJOB_HOME}/logs/master/master.log

# 查看Worker日志
tail -f {NOAHJOB_HOME}/logs/worker/worker.log

# 查看任务执行日志
tail -f {NOAHJOB_HOME}/logs/workflow-task/{工作流ID}/{任务ID}.log

# 使用grep过滤错误信息
grep "ERROR" {NOAHJOB_HOME}/logs/master/master.log
```

### 定期维护

建议的维护操作：

1. 数据库维护：
   * 定期执行`VACUUM ANALYZE`（PostgreSQL）
   * 优化表和索引
   * 清理过期数据

2. 日志管理：
   * 配置日志轮转策略
   * 定期归档或清理过期日志
   * 监控日志存储空间

3. 资源文件清理：
   * 定期清理临时文件
   * 归档历史版本资源文件
   * 监控存储空间使用

4. 系统更新：
   * 定期更新NOAHJOB版本
   * 应用安全补丁
   * 更新依赖组件

## 安全建议

### 系统加固建议

1. 操作系统加固：
   * 禁用不必要的服务
   * 定期应用安全补丁
   * 启用防火墙和SELinux
   * 实施最小权限原则

2. 网络安全加固：
   * 配置防火墙，只开放必要端口
   * 使用VPN或专用网络隔离
   * 实施网络访问控制
   * 启用网络流量监控

3. 应用安全加固：
   * 启用TLS/SSL加密
   * 配置严格的访问控制策略
   * 实施IP白名单策略
   * 启用安全审计功能

### 数据保护建议

1. 敏感数据加密：
   * 使用国密算法加密存储敏感数据
   * 加密传输中的数据
   * 实施密钥管理策略

2. 数据备份策略：
   * 定期备份数据库和配置
   * 存储备份在安全位置
   * 定期测试恢复流程

3. 数据脱敏：
   * 对敏感生产数据进行脱敏
   * 在非生产环境使用脱敏数据
   * 实施数据访问控制

### 审计与合规

1. 操作审计：
   * 启用全面的操作审计日志
   * 定期审查审计日志
   * 设置异常操作告警

2. 合规检查：
   * 定期进行安全合规检查
   * 对照行业标准和法规要求
   * 记录和跟踪合规状态

3. 应急响应：
   * 制定安全事件应急响应计划
   * 定期演练应急响应流程
   * 建立安全事件上报机制

## 最佳实践

### 工作流设计建议

1. 模块化设计：
   * 将复杂工作流拆分为多个子工作流
   * 按功能模块组织任务
   * 提高重用性和可维护性

2. 命名规范：
   * 使用统一的命名约定
   * 名称应反映功能或目的
   * 避免使用特殊字符和中文命名

3. 错误处理：
   * 添加适当的错误处理机制
   * 使用条件分支处理异常情况
   * 设置合理的重试策略

4. 资源管理：
   * 合理分配资源配额
   * 避免资源过度占用
   * 考虑任务优先级设置

### 性能优化建议

1. 并行执行：
   * 合理设计任务依赖关系
   * 最大化并行执行机会
   * 使用fork任务实现并行处理

2. 资源配置：
   * 根据任务特性分配资源
   * 避免资源过度分配
   * 定期监控资源使用情况

3. 任务优化：
   * 优化任务执行逻辑
   * 减少不必要的数据处理
   * 使用缓存机制提高性能

### 稳定性保障建议

1. 高可用部署：
   * 部署多个Master节点
   * 配置Master自动故障转移
   * 使用多个Worker节点分散负载

2. 监控与告警：
   * 配置全面的监控指标
   * 设置合理的告警阈值
   * 实施主动监控策略

3. 灾备策略：
   * 实施跨区域灾备部署
   * 定期测试灾备切换
   * 保持数据一致性机制

## 参考资料

* NOAHJOB官方文档：https://noahjob.datasophon.com/docs/
* NOAHJOB API参考：https://noahjob.datasophon.com/api-docs/
* 中兵数科技术支持：support@zhongbing.tech
* DolphinScheduler官方文档：https://dolphinscheduler.apache.org/ 