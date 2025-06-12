# YARN 组件介绍

## 基本介绍
YARN（Yet Another Resource Negotiator）是Hadoop生态系统中的资源管理和作业调度框架。作为Hadoop 2.0的核心组件，YARN将资源管理功能从MapReduce框架中分离出来，成为一个独立的通用资源管理系统。

YARN的引入解决了Hadoop 1.0中JobTracker的单点故障和可扩展性问题，同时使Hadoop能够支持更多种类的分布式计算模型，不再局限于MapReduce。这使得Hadoop从一个单一的批处理系统演变为一个多用途的分布式处理平台。

## 主要功能
- **资源抽象和统一管理** - 将计算资源（CPU、内存等）抽象为容器，统一管理和分配
- **多租户支持** - 能够同时运行多种不同的计算框架，如MapReduce、Spark、Flink、Hive等
- **高可扩展性** - 支持扩展到数千个节点的大规模集群
- **资源利用率优化** - 动态分配集群资源，提高利用率
- **安全性** - 通过Kerberos身份验证和访问控制提供强大的安全保障
- **高可用性** - 支持ResourceManager的高可用配置，消除单点故障
- **兼容性** - 完全向后兼容已有的MapReduce应用
- **资源隔离** - 通过Linux CGroups等技术实现更好的资源隔离

## 架构说明
YARN采用主从式架构，包含以下主要组件：

### ResourceManager (RM)
集群的主控节点，负责整个集群的资源管理和分配。ResourceManager有两个主要组件：
- **Scheduler** - 纯粹的调度器，根据应用程序的资源需求进行资源分配，但不执行监控或跟踪应用程序状态
- **ApplicationsManager (ASM)** - 管理集群中运行的应用，负责接收作业提交、协商启动ApplicationMaster以及在失败时重启ApplicationMaster

ResourceManager的主要职责包括：
1. 仲裁来自应用程序的资源请求
2. 根据约束条件（如队列容量、用户限制等）安全地分配资源
3. 管理应用程序的提交和监控
4. 在必要时重启ApplicationMaster容器

### NodeManager (NM)
集群中每个节点上运行的代理，负责容器的管理、监控资源使用情况并汇报给ResourceManager。NodeManager主要职责包括：
- 接收来自ResourceManager的容器启动/停止请求
- 管理容器的生命周期
- 监控和报告节点资源使用情况
- 运行Node Labels，允许应用程序请求特定类型的资源

NodeManager通过定期的心跳和ResourceManager通信，报告当前节点的资源使用情况和容器状态。如果NodeManager与ResourceManager断开连接，相应节点上的所有容器会被标记为不健康。

### ApplicationMaster (AM)
每个应用程序一个，负责与ResourceManager协商资源并与NodeManager协作执行和监控任务。ApplicationMaster职责包括：
- 向ResourceManager请求适当的资源容器
- 在分配的容器上启动进程
- 监控应用程序状态和进度
- 处理应用程序内的任务失败
- 根据应用需求动态调整资源

ApplicationMaster提供了各种计算框架（如MapReduce、Spark、Flink等）与YARN集成的连接点。每个框架可以实现自己特定的ApplicationMaster，以根据其需求管理资源和任务执行。

### 容器 (Container)
YARN的资源抽象单位，包含特定数量的CPU、内存等资源，是应用程序运行的基本单位。容器代表分配给应用程序的资源，具有以下特性：
- 资源边界明确（CPU、内存、网络带宽等）
- 支持资源隔离（通过Linux CGroups）
- 分配和回收由YARN管理
- 容器内运行特定的任务或进程

![YARN架构详细图](../images/1747818637707-0.png)

## YARN工作流程
1. **作业提交** - 客户端向ResourceManager提交应用程序
2. **启动ApplicationMaster** - ResourceManager为应用程序分配第一个容器来运行ApplicationMaster
3. **资源申请** - ApplicationMaster向ResourceManager注册并请求资源
4. **资源分配** - ResourceManager根据调度策略向ApplicationMaster分配容器
5. **容器启动** - ApplicationMaster要求NodeManager启动分配的容器
6. **应用执行** - 容器执行应用程序的计算任务
7. **进度监控** - ApplicationMaster监控作业进度，必要时请求或释放容器
8. **作业完成** - 应用程序完成后，ApplicationMaster向ResourceManager注销并释放资源

详细工作流程解析：
- **客户端提交阶段**：客户端准备应用程序资源（JAR文件、配置文件等），提交给ResourceManager
- **ApplicationMaster启动阶段**：ResourceManager分配第一个容器用于启动ApplicationMaster，并将应用元数据传递给ApplicationMaster
- **资源协商阶段**：ApplicationMaster分析应用需求，向ResourceManager申请资源，并接收分配的容器资源
- **任务执行阶段**：ApplicationMaster与NodeManager通信，在分配的容器上启动任务
- **监控与动态调整阶段**：ApplicationMaster持续监控应用程序进度，并可能根据需求调整资源请求
- **完成与清理阶段**：应用完成后，ApplicationMaster停止所有容器，向ResourceManager注销，释放资源

## 核心组件详述

| 组件名称 | 功能描述 | 运行模式 | 关键配置属性 |
| --- | --- | --- | --- |
| ResourceManager | 全局资源管理器，负责整个集群的资源调度与分配 | 主节点（支持HA） | yarn.resourcemanager.hostname<br>yarn.resourcemanager.scheduler.class |
| NodeManager | 负责单个节点上的资源管理和容器生命周期 | 每个工作节点 | yarn.nodemanager.resource.memory-mb<br>yarn.nodemanager.resource.cpu-vcores |
| ApplicationMaster | 应用程序级别的资源协调和进程管理 | 应用程序特定 | 根据应用类型不同而异 |
| Scheduler | 负责将集群资源分配给各个队列和应用程序 | ResourceManager内部组件 | yarn.scheduler.minimum-allocation-mb<br>yarn.scheduler.maximum-allocation-mb |
| Timeline Server | 存储和检索应用程序的当前和历史信息 | 独立服务 | yarn.timeline-service.enabled<br>yarn.timeline-service.hostname |
| Resource Proxy | 处理自定义资源类型（如GPU）的代理 | NodeManager插件 | yarn.nodemanager.resource-plugins<br>yarn.nodemanager.resource-plugins.gpu.path-to-discovery-executables |
| Sharedcache Manager | 管理应用程序间共享的缓存资源 | 独立服务 | yarn.sharedcache.enabled<br>yarn.sharedcache.root-dir |
| Federation StateStore | 集群联邦状态存储 | 高级功能 | yarn.federation.enabled<br>yarn.federation.state-store.class |

## 关键进程和服务

ResourceManager和NodeManager是YARN的核心进程，它们具有以下特性：

### ResourceManager进程特性
- 默认端口：8088（Web UI）、8032（客户端提交作业）
- 内存要求：通常需要4-8GB内存
- JVM配置：推荐使用CMS或G1垃圾收集器
- 高可用：可配置多个RM实例通过ZooKeeper进行主备切换
- 扩展点：调度器插件、安全插件、恢复机制等

### NodeManager进程特性
- 默认端口：8042（Web UI）、8041（本地化服务）
- 内存监控：可配置物理内存和虚拟内存限制
- 心跳机制：默认每1000ms向ResourceManager报告状态
- 健康检查：可配置执行自定义脚本检查节点健康状态
- 资源限制：支持通过CGroups限制CPU和内存使用

### Timeline Server特性
- 功能：存储和提供应用历史数据
- 版本：
  - Timeline Server v1：基础应用历史服务
  - Timeline Server v2：改进的横向可扩展历史服务
- 存储选项：LevelDB（默认）、HBase、内存存储等
- 访问控制：支持用户级别的历史数据访问权限管理

## 调度器类型

YARN提供了三种主要的调度器实现：

### 1. 容量调度器 (Capacity Scheduler)
默认调度器，专为多租户环境设计，提供了队列层次结构，允许组织共享集群资源。

**主要特点**：
- 层次化队列结构
- 容量保证和弹性
- 安全限制与队列访问控制
- 运行时队列管理
- 资源基于权重的分配

**关键配置参数**：
```xml
<!-- 容量调度器队列定义 -->
<property>
  <n>yarn.scheduler.capacity.root.queues</n>
  <value>default,prod,dev</value>
</property>

<!-- 队列容量配置 -->
<property>
  <n>yarn.scheduler.capacity.root.prod.capacity</n>
  <value>60</value>
</property>

<!-- 队列用户限制配置 -->
<property>
  <n>yarn.scheduler.capacity.root.prod.user-limit-factor</n>
  <value>1.0</value>
</property>

<!-- 抢占配置 -->
<property>
  <n>yarn.scheduler.capacity.root.prod.disable_preemption</n>
  <value>false</value>
</property>
```

### 2. 公平调度器 (Fair Scheduler)
目标是为所有应用提供平等的资源共享，使小作业不必等待大作业完成。

**主要特点**：
- 动态计算公平共享
- 抢占机制
- 最小共享配置
- 权重调整
- 队列放置规则

**关键配置参数**：
```xml
<!-- 队列策略 -->
<allocations>
  <queue n="root">
    <queue n="default">
      <minResources>1024mb,1vcores</minResources>
      <weight>1.0</weight>
      <schedulingPolicy>fair</schedulingPolicy>
    </queue>
    <queue n="prod">
      <minResources>5120mb,5vcores</minResources>
      <maxResources>20480mb,20vcores</maxResources>
      <weight>4.0</weight>
      <schedulingPolicy>drf</schedulingPolicy>
    </queue>
  </queue>
</allocations>
```

### 3. FIFO调度器 (First In First Out)
最简单的调度器，按作业提交顺序分配资源。仅适用于小型集群或单一用户场景。

**主要特点**：
- 简单的实现机制
- 按照应用提交顺序处理
- 无队列和多租户功能
- 适合单一用户或小规模环境
- 不支持资源抢占

**何时使用**：
- 开发/测试环境
- 单用户集群
- 简单批处理环境
- 不需要复杂资源隔离的场景

## 调度器策略比较

| 特性 | 容量调度器 | 公平调度器 | FIFO调度器 |
| --- | --- | --- | --- |
| 队列支持 | 层次化队列 | 层次化队列 | 不支持队列 |
| 资源保证 | 基于队列容量 | 基于最小资源和权重 | 无保证 |
| 多租户支持 | 强 | 强 | 弱 |
| 弹性 | 支持队列间弹性 | 支持应用间动态平衡 | 不支持 |
| 抢占机制 | 支持 | 支持 | 不支持 |
| 用户限制 | 支持用户和应用限制 | 支持用户和应用限制 | 简单应用限制 |
| 资源调度策略 | DRF、FIFO等 | DRF、Fair、FIFO等 | 仅FIFO |
| 复杂度 | 中等 | 高 | 低 |
| 默认在Hadoop中 | 是 | 否 | 否 |

## 高可用配置

YARN ResourceManager支持高可用配置，通过以下方式实现：

1. **主备RM模式** - 一个活跃的ResourceManager和多个备用ResourceManager
2. **状态存储** - 活跃的RM将状态写入共享存储（ZooKeeper）
3. **自动故障转移** - 当活跃RM失败时，备用RM接管角色

基本配置示例：
```xml
<property>
  <n>yarn.resourcemanager.ha.enabled</n>
  <value>true</value>
</property>
<property>
  <n>yarn.resourcemanager.ha.rm-ids</n>
  <value>rm1,rm2</value>
</property>
<property>
  <n>yarn.resourcemanager.hostname.rm1</n>
  <value>master1</value>
</property>
<property>
  <n>yarn.resourcemanager.hostname.rm2</n>
  <value>master2</value>
</property>
<property>
  <n>yarn.resourcemanager.zk-address</n>
  <value>zk1:2181,zk2:2181,zk3:2181</value>
</property>
```

### 高可用架构详解

YARN ResourceManager高可用模式包含以下关键元素：

1. **活跃/备用RM**：一个活跃的ResourceManager和一个或多个备用ResourceManager
2. **ZooKeeper仲裁**：使用ZooKeeper进行活跃RM选举和状态存储
3. **状态恢复**：当备用RM激活时，从状态存储中恢复应用程序和节点信息
4. **客户端重定向**：客户端通过配置连接到ResourceManager集群，自动处理故障转移
5. **无缝故障转移**：在RM故障时保持应用程序运行，只暂时暂停资源分配

### 状态存储选项
YARN RM高可用支持多种状态存储实现：

1. **ZKRMStateStore** (默认)：将状态数据存储在ZooKeeper中
2. **FileSystemRMStateStore**：将状态存储在HDFS或其他文件系统中
3. **LeveldbRMStateStore**：使用LevelDB本地存储状态（不推荐用于HA）
4. **MemoryRMStateStore**：内存状态存储（仅用于测试）

## 与其他组件集成

YARN作为资源管理平台，可以与多种计算框架集成：

| 框架 | 集成方式 | 用途 |
| --- | --- | --- |
| MapReduce | 原生支持 | 批处理计算 |
| Apache Spark | 通过YARN客户端 | 通用数据处理和机器学习 |
| Apache Flink | 通过YARN应用客户端 | 流处理和批处理 |
| Apache Tez | 作为YARN应用运行 | DAG计算框架 |
| Apache Hive | 通过Tez或MR在YARN上运行 | 数据仓库查询 |

### MapReduce在YARN上的运行机制
1. 客户端提交MR作业到YARN
2. YARN启动MRAppMaster作为ApplicationMaster
3. MRAppMaster请求资源并启动Map和Reduce任务
4. Map和Reduce任务运行在分配的容器中
5. MRAppMaster监控任务执行并处理失败

### Spark在YARN上的运行模式
1. **集群模式**：
   - Driver程序运行在ApplicationMaster容器中
   - 适合生产环境
   - 客户端只负责提交应用，可以在提交后退出
2. **客户端模式**：
   - Driver程序运行在客户端
   - 适合交互式和调试场景
   - 客户端需要在整个应用生命周期保持活跃

### 集成优势
- **统一资源管理**：所有框架共享相同的资源池
- **多框架支持**：在同一集群上运行不同计算框架
- **动态资源分配**：根据负载动态调整资源
- **统一安全模型**：所有框架使用相同的安全配置
- **简化运维**：管理单个资源管理系统而非多个独立系统

## 资源模型

YARN采用灵活的资源模型来表示和管理集群资源：

### 基本资源类型
- **内存 (MB)**：容器使用的内存量
- **虚拟核心 (vCores)**：容器使用的CPU资源

### 扩展资源类型
YARN 3.x支持多种自定义资源类型：
- **GPU**：图形处理单元资源
- **FPGA**：现场可编程门阵列
- **磁盘**：本地存储资源
- **网络带宽**：网络资源限制
- **自定义资源**：用户定义的其他资源类型

### 资源配置示例
```xml
<!-- 启用GPU资源支持 -->
<property>
  <n>yarn.nodemanager.resource-plugins</n>
  <value>yarn.io/gpu</value>
</property>

<!-- GPU设备发现 -->
<property>
  <n>yarn.nodemanager.resource-plugins.gpu.path-to-discovery-executables</n>
  <value>/usr/bin/nvidia-smi</value>
</property>

<!-- 自定义资源类型 -->
<property>
  <n>yarn.resource-types</n>
  <value>resource1,resource2</value>
</property>
```

## 安全特性

YARN提供多层安全机制保护集群资源和数据：

### 认证机制
- **简单认证**：基于操作系统用户身份
- **Kerberos认证**：基于票据的强认证系统
- **委托令牌**：允许服务代表用户执行操作

### 授权控制
- **服务级别授权**：控制谁可以访问YARN服务
- **队列访问控制**：限制用户提交应用到特定队列
- **应用管理授权**：控制谁可以查看、修改和终止应用

### 数据安全
- **RPC加密**：组件间通信加密
- **Web界面安全**：HTTPS访问ResourceManager和NodeManager界面
- **本地存储加密**：加密NodeManager本地目录

### 配置示例
```xml
<!-- 启用Kerberos认证 -->
<property>
  <n>hadoop.security.authentication</n>
  <value>kerberos</value>
</property>

<!-- 启用授权 -->
<property>
  <n>hadoop.security.authorization</n>
  <value>true</value>
</property>

<!-- ResourceManager安全配置 -->
<property>
  <n>yarn.resourcemanager.principal</n>
  <value>rm/_HOST@REALM</value>
</property>
<property>
  <n>yarn.resourcemanager.keytab</n>
  <value>/etc/hadoop/conf/rm.keytab</value>
</property>
```

## 性能调优建议

### 内存配置
- 为ApplicationMaster分配足够的内存 (yarn.app.mapreduce.am.resource.mb)
- 合理设置NodeManager可用内存 (yarn.nodemanager.resource.memory-mb)
- 配置适当的容器内存限制 (yarn.scheduler.minimum/maximum-allocation-mb)
- 调整内存超额使用比例 (yarn.nodemanager.vmem-pmem-ratio)
- 考虑操作系统和其他服务的内存需求

### CPU配置
- 为每个节点配置合适的虚拟CPU核心数 (yarn.nodemanager.resource.cpu-vcores)
- 设置应用的CPU需求 (mapreduce.map.cpu.vcores, mapreduce.reduce.cpu.vcores)
- 平衡CPU与内存资源分配
- 考虑超额订阅策略
- 为应用程序设置适当的CPU限制

### 调度器配置
- 根据工作负载特性选择适当的调度器
- 为重要应用设置资源保证
- 配置队列容量和访问控制策略
- 优化队列结构以反映组织需求
- 设置合理的资源抢占策略

### 其他优化
- 启用内存监控和执行 (yarn.nodemanager.pmem-check-enabled)
- 配置日志聚合 (yarn.log-aggregation-enable)
- 调整应用心跳间隔 (yarn.am.liveness-monitor.expiry-interval-ms)
- 优化节点本地化偏好
- 配置适当的应用优先级

### 高级调优参数
| 参数 | 描述 | 默认值 | 调优建议 |
| --- | --- | --- | --- |
| yarn.resourcemanager.scheduler.client.thread-count | 处理调度请求的线程数 | 50 | 根据集群规模和负载调整 |
| yarn.resourcemanager.scheduler.monitor.enable | 启用调度器监控 | false | 在大型集群中启用 |
| yarn.nodemanager.localizer.client.thread-count | 本地化线程数 | 5 | 调整以处理高并发应用 |
| yarn.nodemanager.recovery.enabled | NodeManager恢复功能 | false | 在生产环境中启用 |
| yarn.nodemanager.resource.detect-hardware-capabilities | 自动检测硬件能力 | false | 在硬件资源不同的节点上启用 |

## 监控与指标

### 关键监控指标
1. **集群级别指标**：
   - 活跃/待处理的应用数量
   - 已分配/可用的内存和CPU
   - 集群资源利用率
   - 应用提交/完成率

2. **节点级别指标**：
   - 节点健康状态
   - 容器数量和资源使用
   - 磁盘和网络I/O
   - 本地化成功率

3. **应用级别指标**：
   - 应用进度和状态
   - 资源请求/使用情况
   - 容器成功/失败率
   - 应用完成时间

### 监控工具
- **ResourceManager Web UI**：提供集群和应用概览
- **NodeManager Web UI**：展示节点资源和容器详情
- **YARN命令行工具**：用于查询状态和资源使用情况
- **Timeline Server**：存储和查询应用历史指标
- **第三方监控系统**：Grafana、Prometheus等与YARN集成

### JMX指标
YARN公开JMX指标，可用于高级监控：
- ResourceManager: `http://<rm-host>:8088/jmx`
- NodeManager: `http://<nm-host>:8042/jmx`

## 日志管理

### 日志目录结构
- **本地日志**：`${HADOOP_LOG_DIR}/yarn-${USER}-${COMPONENT}-${hostname}.log`
- **应用日志**：`${yarn.nodemanager.log-dirs}/application_${appid}/container_${contid}/`
- **聚合日志**：`${yarn.nodemanager.remote-app-log-dir}/${user}/logs/application_${appid}/`

### 日志类型
- **系统日志**：ResourceManager和NodeManager进程日志
- **容器日志**：应用程序容器的stdout、stderr和syslog
- **聚合日志**：将容器日志汇总到HDFS或其他存储系统

### 日志聚合配置
```xml
<!-- 启用日志聚合 -->
<property>
  <n>yarn.log-aggregation-enable</n>
  <value>true</value>
</property>

<!-- 日志保留时间 -->
<property>
  <n>yarn.log-aggregation.retain-seconds</n>
  <value>604800</value>
</property>

<!-- 聚合日志目录 -->
<property>
  <n>yarn.nodemanager.remote-app-log-dir</n>
  <value>/tmp/logs</value>
</property>
```

## 最新YARN功能

### 容器调度改进
- **弹性资源分配**：允许ApplicationMaster增加或减少运行中容器的资源
- **资源轮廓**：支持为应用程序定义不同的资源需求轮廓
- **放置约束**：支持容器放置位置的硬/软约束条件

### 服务API
- **长期运行服务**：支持将应用作为服务长期运行（不仅是批处理作业）
- **服务注册**：提供服务发现和DNS集成
- **滚动升级**：支持服务组件的无缝升级

### 容器增强
- **Docker容器支持**：允许在YARN容器中运行Docker镜像
- **容器资源隔离增强**：改进的CGroups集成
- **GPU和其他资源支持**：支持分配和隔离GPU等专用资源

### 集群联邦
- **多集群管理**：支持跨YARN集群的资源分配
- **统一应用视图**：对多个YARN集群提供单一视图
- **路由策略**：支持自定义应用路由到特定集群

### 配置示例
```xml
<!-- 启用Docker运行时 -->
<property>
  <n>yarn.nodemanager.container-executor.class</n>
  <value>org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor</value>
</property>
<property>
  <n>yarn.nodemanager.runtime.linux.allowed-runtimes</n>
  <value>default,docker</value>
</property>

<!-- 启用集群联邦 -->
<property>
  <n>yarn.federation.enabled</n>
  <value>true</value>
</property>
```

## 问题排查指南

### 常见问题及解决方案

#### ResourceManager问题
- **无法启动**：检查端口占用、配置文件和权限
- **高内存使用**：调整JVM堆大小和GC参数
- **应用提交失败**：检查队列容量和用户权限
- **响应缓慢**：调整调度器线程池和内存配置

#### NodeManager问题
- **注册失败**：检查与ResourceManager的网络连接
- **容器启动失败**：检查权限、资源限制和磁盘空间
- **磁盘使用过高**：清理旧的应用缓存和本地化资源
- **内存泄漏**：检查日志中的内存警告和配置虚拟内存比例

#### 应用程序问题
- **容器分配慢**：检查资源可用性和调度延迟
- **应用挂起**：分析ResourceManager日志中的调度决策
- **容器失败**：检查容器日志中的错误和异常
- **性能差**：调整容器资源分配和本地化策略

### 有用的调试命令
```bash
# 检查YARN服务状态
yarn rmadmin -getServiceState rm1

# 收集诊断信息
yarn --debug

# 查看详细的应用日志
yarn logs -applicationId <appId> -containerId <containerId>

# 检查节点健康状态
yarn node -list -all -states unhealthy,decommissioned

# 转储ResourceManager调度器状态
curl -s http://<rm-host>:8088/ws/v1/cluster/scheduler > scheduler_state.json
```

## 部署最佳实践

1. **硬件规划**：
   - ResourceManager：8-16GB内存，4-8核CPU，至少200GB磁盘
   - NodeManager：根据工作负载，内存分配给YARN应通常为节点内存的80%

2. **网络配置**：
   - 配置适当的防火墙规则允许YARN端口通信
   - 实现网络质量服务(QoS)以优先处理YARN控制流量
   - 使用可靠的DNS解析和主机名配置

3. **存储配置**：
   - 使用SSD存储ResourceManager状态和本地化资源
   - 为临时数据配置专用磁盘
   - 确保足够的存储空间用于日志和容器工作目录

4. **操作系统调优**：
   - 增加文件描述符限制
   - 调整TCP参数优化网络性能
   - 配置适当的内存页面大小
   - 禁用不必要的系统服务

5. **高可用部署**：
   - 实现ResourceManager HA
   - 使用至少3个ZooKeeper节点
   - 配置自动故障转移
   - 实施定期备份策略

6. **安全性考虑**：
   - 在生产环境中启用Kerberos认证
   - 实施最小权限原则
   - 加密敏感数据
   - 定期审计安全配置

## 参考资源

- [Apache Hadoop YARN官方文档](https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/YARN.html)
- [YARN架构指南](https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/yarn_architecture.gif)
- [YARN API参考](https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/WebServicesIntro.html)
- [YARN命令参考](https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/YarnCommands.html)
- [调度器配置指南](https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/CapacityScheduler.html) 