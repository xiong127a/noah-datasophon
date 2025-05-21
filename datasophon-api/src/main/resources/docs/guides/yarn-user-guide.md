# YARN 用户指南

## 什么是YARN
YARN (Yet Another Resource Negotiator) 是Hadoop的核心组件之一，负责集群资源管理和作业调度。YARN将资源管理功能从MapReduce中分离出来，使Hadoop能够支持更多类型的分布式应用程序。

### YARN的主要优势
- 多种计算框架支持：不仅支持MapReduce，还支持Spark、Flink、Tez等
- 资源利用率提高：动态分配资源，避免资源浪费
- 可扩展性增强：支持数千节点的大规模集群
- 多租户环境支持：不同用户和应用程序可以共享集群资源
- 向后兼容：现有MapReduce应用无需修改即可运行

## YARN架构组件

### ResourceManager (RM)
- **功能**：全局资源管理器，负责整个集群的资源分配
- **位置**：通常部署在专用的管理节点上
- **访问方式**：Web UI通常在`http://<rm-host>:8088`

### NodeManager (NM)
- **功能**：每个节点上的资源管理代理，负责容器管理和资源监控
- **位置**：集群中的每个工作节点
- **日志位置**：`<hadoop-log-dir>/yarn/nodemanager/`

### ApplicationMaster (AM)
- **功能**：每个应用程序的管理者，负责与RM协商资源并监控任务执行
- **特点**：每个应用有一个独立的AM实例

### Container
- **概念**：YARN中的资源抽象单位，包含特定数量的CPU、内存等资源
- **生命周期**：由AM申请，RM分配，NM启动和监控

## 常用命令速查表

以下是YARN最常用的命令汇总，方便快速查阅和日常操作：

| 命令 | 描述 | 示例 |
|------|------|------|
| `application -list` | 列出应用 | `yarn application -list -appStates RUNNING` |
| `application -kill` | 终止应用 | `yarn application -kill application_1625123456789_0001` |
| `logs` | 查看应用日志 | `yarn logs -applicationId application_1625123456789_0001` |
| `node -list` | 列出节点 | `yarn node -list -all` |
| `rmadmin` | RM管理 | `yarn rmadmin -refreshQueues` |
| `queue` | 队列信息 | `yarn queue -status root.default` |
| `top` | 资源使用情况 | `yarn top` |
| `mapred job` | 管理MR作业 | `mapred job -list` |

## 提交应用到YARN

### 提交MapReduce作业
```bash
# 基本语法
hadoop jar <jar-file> <main-class> [args...]

# 示例：字数统计
hadoop jar hadoop-mapreduce-examples.jar wordcount /input /output

# 指定队列提交作业
hadoop jar hadoop-mapreduce-examples.jar wordcount -Dmapreduce.job.queuename=high /input /output

# 指定资源需求
hadoop jar hadoop-mapreduce-examples.jar wordcount \
  -Dmapreduce.map.memory.mb=2048 \
  -Dmapreduce.reduce.memory.mb=4096 \
  -Dmapreduce.map.java.opts=-Xmx1638m \
  -Dmapreduce.reduce.java.opts=-Xmx3686m \
  /input /output
```

### 提交Spark作业
```bash
# 基本语法
spark-submit --master yarn [options] --class <main-class> <app-jar> [app-args]

# 集群模式（AM在集群中运行）
spark-submit --master yarn --deploy-mode cluster --class org.example.SparkApp app.jar

# 客户端模式（AM在提交客户端运行）
spark-submit --master yarn --deploy-mode client --class org.example.SparkApp app.jar

# 指定资源配置
spark-submit --master yarn \
  --deploy-mode cluster \
  --driver-memory 4g \
  --executor-memory 2g \
  --executor-cores 2 \
  --num-executors 10 \
  --queue production \
  --class org.example.SparkApp app.jar
```

### 提交Flink作业
```bash
# 基本语法
flink run -m yarn-cluster [options] <jar-file> [arguments]

# 示例
flink run -m yarn-cluster \
  -yn 4 \
  -yjm 1024m \
  -ytm 4096m \
  ./examples/streaming/WindowJoin.jar
```

## 监控和管理YARN应用

### Web UI访问
YARN ResourceManager提供了功能强大的Web界面，可用于监控和管理应用：
- **应用列表**：`http://<rm-host>:8088/cluster/apps`
- **集群指标**：`http://<rm-host>:8088/cluster/metrics`
- **节点列表**：`http://<rm-host>:8088/cluster/nodes`
- **调度器**：`http://<rm-host>:8088/cluster/scheduler`

### 命令行工具
YARN提供了丰富的命令行工具用于管理和监控：

#### 应用管理
```bash
# 列出所有应用
yarn application -list

# 列出特定状态的应用
yarn application -list -appStates RUNNING

# 查看应用详情
yarn application -status <application_id>

# 终止应用
yarn application -kill <application_id>

# 查看应用日志
yarn logs -applicationId <application_id>
```

#### 节点管理
```bash
# 查看所有节点状态
yarn node -list -all

# 查看特定节点信息
yarn node -status <node_id>
```

#### 队列管理
```bash
# 查看队列信息
yarn queue -status <queue_name>
```

#### 资源使用情况
```bash
# 类似top命令，查看YARN应用资源使用情况
yarn top
```

### 日志收集与分析
YARN提供了日志聚合功能，可以将所有容器的日志集中收集：

#### 启用日志聚合
在`yarn-site.xml`中配置：
```xml
<property>
  <name>yarn.log-aggregation-enable</name>
  <value>true</value>
</property>
<property>
  <name>yarn.nodemanager.remote-app-log-dir</name>
  <value>/tmp/logs</value>
</property>
<property>
  <name>yarn.log-aggregation.retain-seconds</name>
  <value>604800</value>
</property>
```

#### 查看聚合日志
```bash
# 使用yarn logs命令查看特定应用的聚合日志
yarn logs -applicationId <application_id>

# 查看特定容器的日志
yarn logs -applicationId <application_id> -containerId <container_id>

# 查看特定节点上的应用日志
yarn logs -applicationId <application_id> -nodeAddress <node_address>
```

## 资源调度和队列管理

### 调度器类型

#### 容量调度器 (Capacity Scheduler)
默认调度器，提供层次化队列结构和容量保证：

```xml
<!-- capacity-scheduler.xml 配置示例 -->
<property>
  <name>yarn.scheduler.capacity.root.queues</name>
  <value>default,production,development</value>
</property>

<property>
  <name>yarn.scheduler.capacity.root.production.capacity</name>
  <value>60</value>
</property>

<property>
  <name>yarn.scheduler.capacity.root.development.capacity</name>
  <value>20</value>
</property>

<property>
  <name>yarn.scheduler.capacity.root.default.capacity</name>
  <value>20</value>
</property>

<!-- 启用队列间资源抢占 -->
<property>
  <name>yarn.scheduler.capacity.root.production.user-limit-factor</name>
  <value>1</value>
</property>

<!-- 最大应用数限制 -->
<property>
  <name>yarn.scheduler.capacity.root.production.maximum-applications</name>
  <value>50</value>
</property>

<!-- 最大资源百分比 -->
<property>
  <name>yarn.scheduler.capacity.root.production.maximum-capacity</name>
  <value>80</value>
</property>
```

#### 公平调度器 (Fair Scheduler)
为应用提供公平资源分配，适合多用户共享环境：

```xml
<!-- fair-scheduler.xml 配置示例 -->
<allocations>
  <queue name="root">
    <queue name="production">
      <minResources>10000 mb, 10 vcores</minResources>
      <maxResources>50000 mb, 50 vcores</maxResources>
      <weight>4.0</weight>
      <schedulingPolicy>fair</schedulingPolicy>
    </queue>
    
    <queue name="development">
      <minResources>5000 mb, 5 vcores</minResources>
      <maxResources>30000 mb, 30 vcores</maxResources>
      <weight>2.0</weight>
      <schedulingPolicy>fair</schedulingPolicy>
    </queue>
    
    <queue name="default">
      <weight>1.0</weight>
      <schedulingPolicy>fair</schedulingPolicy>
    </queue>
    
    <queuePlacementPolicy>
      <rule name="specified" create="false" />
      <rule name="primaryGroup" create="false" />
      <rule name="default" />
    </queuePlacementPolicy>
  </queue>
</allocations>
```

### 队列使用和管理

#### 指定队列提交作业
```bash
# MapReduce作业
hadoop jar example.jar WordCount -Dmapreduce.job.queuename=production input output

# Spark作业
spark-submit --queue production --class MyApp app.jar

# 通过环境变量
export HADOOP_CONF_DIR=/etc/hadoop/conf
export YARN_CONF_DIR=/etc/hadoop/conf
export YARN_QUEUE_NAME=production
```

#### 动态刷新队列配置
管理员可以在不重启ResourceManager的情况下更新队列配置：
```bash
# 刷新容量调度器队列
yarn rmadmin -refreshQueues
```

## 高级功能配置

### 资源配置

#### 内存配置
在`yarn-site.xml`中设置节点内存配置：
```xml
<!-- NodeManager可用内存 -->
<property>
  <name>yarn.nodemanager.resource.memory-mb</name>
  <value>16384</value>
</property>

<!-- 容器最小内存 -->
<property>
  <name>yarn.scheduler.minimum-allocation-mb</name>
  <value>1024</value>
</property>

<!-- 容器最大内存 -->
<property>
  <name>yarn.scheduler.maximum-allocation-mb</name>
  <value>8192</value>
</property>

<!-- ApplicationMaster内存 -->
<property>
  <name>yarn.app.mapreduce.am.resource.mb</name>
  <value>2048</value>
</property>
```

#### CPU配置
```xml
<!-- NodeManager可用CPU核心数 -->
<property>
  <name>yarn.nodemanager.resource.cpu-vcores</name>
  <value>8</value>
</property>

<!-- 容器最小CPU核心数 -->
<property>
  <name>yarn.scheduler.minimum-allocation-vcores</name>
  <value>1</value>
</property>

<!-- 容器最大CPU核心数 -->
<property>
  <name>yarn.scheduler.maximum-allocation-vcores</name>
  <value>4</value>
</property>
```

### 高可用性配置
配置ResourceManager高可用，防止单点故障：

```xml
<!-- yarn-site.xml -->
<property>
  <name>yarn.resourcemanager.ha.enabled</name>
  <value>true</value>
</property>

<property>
  <name>yarn.resourcemanager.ha.rm-ids</name>
  <value>rm1,rm2</value>
</property>

<property>
  <name>yarn.resourcemanager.hostname.rm1</name>
  <value>master1.example.com</value>
</property>

<property>
  <name>yarn.resourcemanager.hostname.rm2</name>
  <value>master2.example.com</value>
</property>

<property>
  <name>yarn.resourcemanager.zk-address</name>
  <value>zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181</value>
</property>

<property>
  <name>yarn.resourcemanager.recovery.enabled</name>
  <value>true</value>
</property>

<property>
  <name>yarn.resourcemanager.store.class</name>
  <value>org.apache.hadoop.yarn.server.resourcemanager.recovery.ZKRMStateStore</value>
</property>
```

### 节点标签管理
YARN支持节点标签功能，可以将特定应用分配到特定节点：

```bash
# 添加节点标签
yarn cluster --add-node-labels "GPU,SSD,MEMORY"

# 将标签分配给节点
yarn rmadmin -addToClusterNodeLabels "GPU,SSD,MEMORY"
yarn rmadmin -replaceLabelsOnNode "node1:8041=GPU node2:8041=SSD node3:8041=MEMORY"

# 指定标签提交作业
hadoop jar example.jar -Dmapreduce.job.node-label-expression=GPU
```

### 资源类型配置
YARN支持自定义资源类型（如GPU、FPGA等）：

```xml
<property>
  <name>yarn.resource-types</name>
  <value>resource1,resource2</value>
</property>

<property>
  <name>yarn.resource-types.resource1.units</name>
  <value>G</value>
</property>
```

## 性能调优

### MapReduce作业调优
```bash
# Map任务内存配置
-Dmapreduce.map.memory.mb=2048
-Dmapreduce.map.java.opts=-Xmx1638m

# Reduce任务内存配置
-Dmapreduce.reduce.memory.mb=4096
-Dmapreduce.reduce.java.opts=-Xmx3686m

# Map/Reduce任务数量
-Dmapreduce.job.maps=10
-Dmapreduce.job.reduces=5

# 中间数据压缩
-Dmapreduce.map.output.compress=true
-Dmapreduce.map.output.compress.codec=org.apache.hadoop.io.compress.SnappyCodec
```

### Spark作业调优
```bash
# 执行器配置
--executor-memory 2g
--executor-cores 2
--num-executors 10

# 动态分配
--conf spark.dynamicAllocation.enabled=true
--conf spark.dynamicAllocation.minExecutors=5
--conf spark.dynamicAllocation.maxExecutors=20

# 序列化
--conf spark.serializer=org.apache.spark.serializer.KryoSerializer

# 内存管理
--conf spark.memory.fraction=0.8
--conf spark.memory.storageFraction=0.3
```

### 集群级别调优
```xml
<!-- 启用内存监控 -->
<property>
  <name>yarn.nodemanager.pmem-check-enabled</name>
  <value>true</value>
</property>

<property>
  <name>yarn.nodemanager.vmem-check-enabled</name>
  <value>true</value>
</property>

<!-- 心跳间隔 -->
<property>
  <name>yarn.resourcemanager.nm.liveness-monitor.interval-ms</name>
  <value>1000</value>
</property>

<!-- 容器分配加速 -->
<property>
  <name>yarn.scheduler.capacity.node-locality-delay</name>
  <value>0</value>
</property>
```

## 故障排除

### 常见问题与解决方案

#### 应用提交失败
- **问题**：`Application submission failed`
- **可能原因**：
  - ResourceManager不可用
  - 队列资源不足
  - 用户权限问题
- **解决方法**：
  - 检查ResourceManager状态：`yarn rmadmin -getServiceState rm1`
  - 查看队列可用资源：`yarn queue -status <queue_name>`
  - 检查用户权限：`yarn queue -status <queue_name> | grep -i user-limit`

#### 容器分配失败
- **问题**：`Container failed to be allocated`
- **可能原因**：
  - 资源不足
  - 节点不健康
  - 内存/CPU配置不合理
- **解决方法**：
  - 检查集群资源：`yarn node -list -all`
  - 查看节点健康状态：`yarn node -status <node_id>`
  - 调整应用资源需求

#### 应用运行缓慢
- **问题**：应用执行速度比预期慢
- **可能原因**：
  - 资源分配不足
  - 数据倾斜
  - 调度器配置不合理
- **解决方法**：
  - 增加资源分配
  - 检查数据分布
  - 优化调度器配置
  - 启用预调度(Uber)模式：`mapreduce.job.ubertask.enable`

### 日志分析技巧
```bash
# 查找应用失败原因
yarn logs -applicationId <app_id> | grep -i "exception\|error\|fail"

# 查看容器退出状态
yarn logs -applicationId <app_id> | grep -i "exit code"

# 分析资源使用情况
yarn logs -applicationId <app_id> | grep -i "resource"
```

### 调试工具
- YARN Timeline Server：`http://<timeline-server-host>:8188`
- YARN Registry：`yarn registry -listApplications`
- YARN健康监测：`yarn node -all -healthReport`

## 安全配置

### Kerberos认证
YARN支持Kerberos认证，需要在`core-site.xml`和`yarn-site.xml`中配置：

```xml
<!-- core-site.xml -->
<property>
  <name>hadoop.security.authentication</name>
  <value>kerberos</value>
</property>
<property>
  <name>hadoop.security.authorization</name>
  <value>true</value>
</property>

<!-- yarn-site.xml -->
<property>
  <name>yarn.resourcemanager.principal</name>
  <value>rm/_HOST@REALM</value>
</property>
<property>
  <name>yarn.resourcemanager.keytab</name>
  <value>/etc/security/keytabs/rm.keytab</value>
</property>
<property>
  <name>yarn.nodemanager.principal</name>
  <value>nm/_HOST@REALM</value>
</property>
<property>
  <name>yarn.nodemanager.keytab</name>
  <value>/etc/security/keytabs/nm.keytab</value>
</property>
```

### 安全访问控制
配置基于队列的访问控制：

```xml
<property>
  <name>yarn.scheduler.capacity.root.production.acl_submit_applications</name>
  <value>user1,user2 group1,group2</value>
</property>
<property>
  <name>yarn.scheduler.capacity.root.production.acl_administer_queue</name>
  <value>admin1,admin2 admingroup</value>
</property>
```

## 最佳实践

### 资源配置最佳实践
- 为每个节点保留10-15%的内存给操作系统和守护进程
- 容器内存设置应考虑JVM开销
- 适当设置虚拟内存比率：`yarn.nodemanager.vmem-pmem-ratio`

### 调度器选择指南
- **容量调度器**：适合多部门共享集群，需要资源隔离和保证
- **公平调度器**：适合资源共享环境，注重公平性
- **FIFO调度器**：仅适合单用户或小规模测试环境

### 生产环境部署建议
- 启用ResourceManager高可用
- 使用日志聚合功能
- 配置适当的资源监控和告警
- 定期进行集群监控和性能优化
- 为重要队列设置资源保证

## 附录

### 重要配置参数参考
| 参数 | 描述 | 默认值 | 推荐值 |
| --- | --- | --- | --- |
| yarn.nodemanager.resource.memory-mb | 节点可用内存 | 8192 | 根据物理内存的80% |
| yarn.nodemanager.resource.cpu-vcores | 节点可用CPU核心 | 8 | 根据物理CPU核心数 |
| yarn.scheduler.minimum-allocation-mb | 最小容器内存 | 1024 | 1024 |
| yarn.scheduler.maximum-allocation-mb | 最大容器内存 | 8192 | 根据节点内存 |
| yarn.scheduler.capacity.maximum-am-resource-percent | AM最大资源百分比 | 0.1 | 0.2-0.3 |
| yarn.resourcemanager.scheduler.class | 调度器类 | CapacityScheduler | 根据需求选择 |
| yarn.log-aggregation-enable | 启用日志聚合 | false | true |
| yarn.nodemanager.vmem-pmem-ratio | 虚拟内存比例 | 2.1 | 1.5-2.5 |

 