# API-Worker 通信架构改造总结

## 📋 改造背景

**原问题**：由于防火墙限制，Worker 节点无法访问 API 节点的端口，但 API 可以访问 Worker 的端口。而原架构中存在 Worker 主动连接 API 的双向通信模式，导致部分功能无法正常工作。

**改造目标**：将双向通信改造为单向通信，确保只有 API 主动连接 Worker，消除 Worker 到 API 的所有网络连接需求。

## ✅ 已完成的改造

### 阶段一：模板下发机制改造

#### 1.1 命令对象增强
- **文件**：`datasophon-common/src/main/java/com/datasophon/common/command/GenerateServiceConfigCommand.java`
- **改动**：新增 `templateContents` 字段（Map<String, String>），用于携带模板内容
- **目的**：避免 Worker 回连 API 获取模板

#### 1.2 API 端模板打包
- **文件**：`datasophon-service/src/main/java/com/datasophon/api/master/handler/service/ServiceConfigureHandler.java`
- **改动**：
  - 新增 `packTemplateContents()` 方法
  - 在发送配置命令前，将所需模板内容打包到命令对象中
- **效果**：Worker 收到的命令中已包含所有需要的模板数据

#### 1.3 Worker 端模板使用
- **文件**：
  - `datasophon-worker/src/main/java/com/datasophon/worker/utils/WorkerFreemarkerUtils.java`
  - `datasophon-worker/src/main/java/com/datasophon/worker/handler/ConfigureServiceHandler.java`
  - `datasophon-worker/src/main/java/com/datasophon/worker/actor/ConfigureServiceActor.java`
- **改动**：
  - 修改 `generateConfigFile()` 和 `generatePromAlertFile()` 方法，从命令对象获取模板
  - 移除 `AkkaUtils.getTemplateContent()` 调用
  - 在 Actor 和 Handler 间传递 templateContents

### 阶段二：OLAP 节点自动添加机制

#### 2.1 创建监控 Actor
- **文件**：`datasophon-service/src/main/java/com/datasophon/api/master/OlapNodeMonitorActor.java`
- **功能**：
  - 定时（30秒）检查运行中但未添加到集群的 OLAP 节点
  - 自动调用 API 端的 OlapUtils 将节点添加到集群
  - 支持 Doris/StarRocks 的 BE/FE/FEObserver/CN 节点类型

#### 2.2 数据库增强
- **文件**：
  - `datasophon-dao/src/main/java/com/datasophon/dao/entity/ClusterServiceRoleInstanceEntity.java`
  - `datasophon-api/src/main/resources/db/migration/mysql/3.0.0/V3.0.0.7__olap_node_cluster_management.sql`
  - `datasophon-api/src/main/resources/db/migration/dm/3.0.0/V3.0.0.7__olap_node_cluster_management.sql`
- **新增字段**：
  - `added_to_cluster`: 标记节点是否已添加到集群
  - `add_to_cluster_time`: 记录添加时间
  - 索引优化：`idx_added_to_cluster`

#### 2.3 Actor 注册与调度
- **文件**：`datasophon-service/src/main/java/com/datasophon/api/master/ActorUtils.java`
- **改动**：
  - 注册 `OlapNodeMonitorActor`
  - 配置定时调度：初始延迟 10秒，间隔 30秒

### 阶段三：移除 Worker 到 API 的通信代码

#### 3.1 Strategy 类清理
清理了 8 个 Worker Strategy 类中向 API 发送消息的代码：
1. **BEHandlerStrategy.java** - Doris BE 节点
2. **FEHandlerStrategy.java** - Doris FE 节点
3. **FEObserverHandlerStrategy.java** - Doris FE Observer 节点
4. **SRBEHandlerStrategy.java** - StarRocks BE 节点
5. **SRFEHandlerStrategy.java** - StarRocks FE 节点
6. **SRFEObserverHandlerStrategy.java** - StarRocks FE Observer 节点
7. **SRCNHandlerStrategy.java** - StarRocks CN 节点
8. **GrafanaHandlerStrategy.java** - Grafana 节点

**改动内容**：
- 移除 `ActorUtils.getRemoteActor()` 调用
- 移除 `OlapSqlExecCommand` 和 `Sqlite3ExecCommand` 的发送逻辑
- 保留日志记录，说明节点将由 API 自动添加

#### 3.2 工具类删除
删除了 Worker 端不再需要的工具类：
1. **`datasophon-worker/src/main/java/com/datasophon/worker/utils/AkkaUtils.java`**
   - 原用途：Worker 向 API 请求模板内容
   - 删除原因：模板已由 API 推送

2. **`datasophon-worker/src/main/java/com/datasophon/worker/utils/ActorUtils.java`**
   - 原用途：Worker 连接 API 的 Actor
   - 删除原因：Worker 不再需要连接 API

#### 3.3 废弃代码清理
- **文件**：`datasophon-worker/src/main/java/com/datasophon/worker/WorkerApplicationServer.java`
- **删除内容**：`tellToMaster()` 废弃方法（85行代码）
- **原用途**：Worker 启动时向 API 报告状态
- **删除原因**：已改为 API 主动连接 Worker

### 阶段四：API 端清理与配置优化

#### 4.1 删除 Worker 面向的 Actor
删除了 API 端用于接收 Worker 请求的 Actor：
1. **`TemplateServiceActor.java`** - 处理 Worker 的模板请求
2. **`WorkerStartActor.java`** - 处理 Worker 的启动注册

#### 4.2 删除消息类
删除了双向通信相关的消息类：
1. **`TemplateRequestMessage.java`** - 模板请求消息
2. **`TemplateResponseMessage.java`** - 模板响应消息

#### 4.3 ActorUtils 清理
- **文件**：`datasophon-service/src/main/java/com/datasophon/api/master/ActorUtils.java`
- **改动**：
  - 移除 `WorkerStartActor` 注册
  - 移除 `TemplateServiceActor` 注册

#### 4.4 API 配置优化
- **文件**：`datasophon-api/src/main/resources/application.conf`
- **改动**：
  - API 的 Pekko Remote 配置改为仅作客户端模式
  - 使用动态端口（0）避免监听固定端口
  - 绑定到 127.0.0.1，不接受外部入站连接
- **效果**：API 不再监听 2551 端口等待 Worker 连接

## 📊 架构对比

### 改造前
```
┌─────────┐          ┌─────────┐
│   API   │◄─────────┤ Worker  │  ❌ Worker 主动连接 API:2551
│  :2551  │          │  :2552  │     - 获取模板内容
└────┬────┘          └────┬────┘     - 上报节点状态
     │                    │           - 请求添加到集群
     └────────────────────┘
```

### 改造后
```
┌─────────┐          ┌─────────┐
│   API   │──────────►│ Worker  │  ✅ 仅 API 主动连接 Worker:2552
│ (动态端口)│          │  :2552  │     - 下发配置命令（含模板）
└────┬────┘          └─────────┘     - 启动/停止服务
     │                                - 查询服务状态
     │
     └──► OlapNodeMonitorActor      ✅ API 端定时轮询
           自动添加 OLAP 节点到集群
```

## 🔄 通信流程变化

### 1. 模板配置流程

#### 改造前
```
API → Worker: 发送配置命令（不含模板）
Worker → API: 请求模板内容（TemplateRequestMessage）
API → Worker: 返回模板（TemplateResponseMessage）
Worker: 使用模板生成配置文件
```

#### 改造后
```
API: 准备配置命令 + 打包所需模板
API → Worker: 发送配置命令（含模板内容）
Worker: 直接使用命令中的模板生成配置文件
```

### 2. OLAP 节点添加流程

#### 改造前
```
Worker: 启动 BE/FE 节点
Worker → API: 发送 OlapSqlExecCommand
API → MasterNodeProcessingActor: 处理添加请求
API: 执行 SQL 添加节点到集群
```

#### 改造后
```
Worker: 启动 BE/FE 节点
Worker: 更新数据库状态为 RUNNING
...
API: OlapNodeMonitorActor 定时检查（30秒）
API: 发现新的 RUNNING 节点
API: 自动执行 SQL 添加节点到集群
API: 更新数据库 added_to_cluster 字段
```

## 📁 涉及的提交

1. `47205c55b` - refactor(communication): implement template push mechanism to remove Worker-to-API dependency
2. `85f27d844` - feat(olap): add OlapNodeMonitorActor for automatic node cluster management
3. `5fb6d7aca` - refactor(worker): remove Worker-to-API actor communication from OLAP and Grafana strategies
4. `cd01b5018` - refactor(worker): remove deprecated Worker-to-API utilities and methods
5. `2daa1b72e` - refactor(api): remove Worker-facing Actors and configure API as outbound-only remote client
6. `568c97a43` - fix(olap): convert Java 14+ switch expressions to Java 8 compatible syntax

## 🎯 改造成果

### 消除的 Worker-to-API 连接点
1. ✅ 模板获取（TemplateServiceActor @ 2551）
2. ✅ Worker 启动注册（WorkerStartActor @ 2551）
3. ✅ OLAP 节点添加请求（MasterNodeProcessingActor）
4. ✅ Grafana 配置上报（GrafanaProcessingActor）

### 保留的通信方式
- ✅ API → Worker (Pekko Remote @ 2552)：配置管理、服务控制
- ✅ API → Worker (HTTP)：仅用于文件下载（安装包、keytab）
- ℹ️  注：Worker 到 API 的 HTTP 请求（masterHost + masterWebPort）仍需保留，用于下载安装包等

## 🔍 测试要点

### 1. 模板配置测试
- [ ] 新增服务角色时，配置文件能否正确生成
- [ ] 模板内容是否完整传递到 Worker
- [ ] 配置更新时是否正常工作

### 2. OLAP 节点测试
- [ ] 新启动的 BE/FE 节点是否自动添加到集群
- [ ] 添加失败后是否正确重试
- [ ] 数据库字段 added_to_cluster 是否正确更新
- [ ] 支持的节点类型：Doris BE/FE/FEObserver, StarRocks BE/FE/FEObserver/CN

### 3. 网络连接测试
- [ ] API 是否不再监听 2551 端口
- [ ] Worker 是否不再尝试连接 API:2551
- [ ] 防火墙只开放 Worker:2552 时系统是否正常工作

### 4. 兼容性测试
- [ ] 已有集群升级后是否正常工作
- [ ] 历史数据库记录是否兼容（added_to_cluster 默认值）
- [ ] 非 OLAP 服务是否不受影响

## 📝 注意事项

### 数据库迁移
- 新增字段有默认值，兼容历史数据
- 迁移脚本：`V3.0.0.7__olap_node_cluster_management.sql`
- 支持 MySQL 和 DM 数据库

### Worker 配置保留
- `masterHost` 配置仍需保留，用于 HTTP 文件下载
- `masterWebPort` 配置仍需保留
- `sed_common.sh` 脚本无需修改

### Grafana 特殊处理
- Grafana 数据源配置改为手动或通过其他方式同步
- 不再通过 Worker 向 API 发送配置信息

### Java 版本兼容
- OlapNodeMonitorActor 使用 Java 8 语法
- 避免使用 Java 14+ 的 switch 表达式

## 🚀 后续优化建议

1. **监控增强**
   - 添加指标：OLAP 节点添加成功率、延迟
   - 告警：节点长时间未添加到集群

2. **配置优化**
   - OlapNodeMonitorActor 的检查间隔可配置化
   - 添加节点的重试次数和间隔可配置化

3. **日志增强**
   - 记录每次节点添加的详细过程
   - 便于故障排查和审计

4. **测试覆盖**
   - 添加单元测试覆盖新增的 Actor 和工具类
   - 添加集成测试验证端到端流程

## 📖 相关文档

- [Apache Pekko Remote 文档](https://pekko.apache.org/docs/pekko/current/remoting.html)
- [Doris ADD BACKEND 文档](https://doris.apache.org/docs/sql-manual/sql-statements/Cluster-Management-Statements/ALTER-SYSTEM-ADD-BACKEND/)
- [StarRocks ADD BACKEND 文档](https://docs.starrocks.io/docs/sql-reference/sql-statements/Administration/ALTER_SYSTEM/)

---

**改造完成时间**：2025-10-22  
**改造负责人**：AI Assistant  
**架构评审状态**：待评审

