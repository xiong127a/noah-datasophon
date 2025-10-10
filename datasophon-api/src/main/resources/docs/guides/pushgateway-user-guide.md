# Pushgateway 用户指南

本指南将详细介绍如何在大数据平台中使用 Prometheus Pushgateway，包括安装配置、数据推送方法、最佳实践以及常见问题解决方案。

## 准备工作

在开始使用 Pushgateway 之前，您需要了解以下基本信息：

- Pushgateway 服务的地址和端口
- 推送指标的格式和命名规范
- 指标标签的使用策略
- 批处理任务的监控需求

## 安装与配置

Pushgateway 的部署非常灵活，可以根据实际需求选择适合的安装方式。

### 二进制包安装

1. 从 Prometheus 官网下载适合您操作系统的 Pushgateway 二进制包：

```bash
# Linux 64位系统示例
wget https://github.com/prometheus/pushgateway/releases/download/v1.6.0/pushgateway-1.6.0.linux-amd64.tar.gz
```

2. 解压安装包：

```bash
tar -xzf pushgateway-1.6.0.linux-amd64.tar.gz
cd pushgateway-1.6.0.linux-amd64/
```

3. 启动 Pushgateway 服务：

```bash
./pushgateway
```

默认情况下，Pushgateway 将监听在 9091 端口。

### Docker 安装

使用 Docker 运行 Pushgateway：

```bash
docker run -d -p 9091:9091 --name pushgateway prom/pushgateway
```

如果需要持久化存储：

```bash
docker run -d -p 9091:9091 \
  -v /path/to/pushgateway/data:/pushgateway \
  --name pushgateway \
  prom/pushgateway \
  --persistence.file=/pushgateway/metrics.data \
  --persistence.interval=5m
```

### Kubernetes 安装

在 Kubernetes 环境中，可以使用以下 YAML 定义创建 Pushgateway 部署：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pushgateway
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: pushgateway
  template:
    metadata:
      labels:
        app: pushgateway
    spec:
      containers:
      - name: pushgateway
        image: prom/pushgateway:v1.6.0
        args:
          - "--persistence.file=/data/metrics.data"
          - "--persistence.interval=5m"
        ports:
        - containerPort: 9091
        volumeMounts:
        - name: data
          mountPath: /data
      volumes:
      - name: data
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: pushgateway
  namespace: monitoring
spec:
  selector:
    app: pushgateway
  ports:
  - port: 9091
    targetPort: 9091
  type: ClusterIP
```

## 启动参数与配置

Pushgateway 提供了多种命令行参数来自定义其行为：

### 核心参数

| 参数 | 描述 | 默认值 |
|------|------|--------|
| `--web.listen-address` | 监听地址和端口 | `:9091` |
| `--web.telemetry-path` | 暴露指标的路径 | `/metrics` |
| `--persistence.file` | 持久化存储文件路径 | 空（不持久化） |
| `--persistence.interval` | 持久化间隔 | `5m` |
| `--log.level` | 日志级别 | `info` |

### 安全参数

| 参数 | 描述 | 默认值 |
|------|------|--------|
| `--web.enable-admin-api` | 启用管理 API | `true` |
| `--web.enable-lifecycle` | 启用生命周期管理 API | `false` |
| `--web.config.file` | Web 服务器配置文件（TLS 和认证） | 空 |

### 示例启动命令

```bash
./pushgateway \
  --web.listen-address=":9091" \
  --persistence.file="/var/lib/pushgateway/metrics.data" \
  --persistence.interval=5m \
  --web.enable-admin-api=true \
  --log.level=info
```

## 配置 Prometheus 抓取 Pushgateway

要使 Prometheus 能够从 Pushgateway 抓取指标，需要在 Prometheus 配置文件中添加相应的抓取配置：

```yaml
scrape_configs:
  - job_name: 'pushgateway'
    honor_labels: true  # 保留推送时的原始标签
    static_configs:
      - targets: ['pushgateway:9091']
```

`honor_labels: true` 参数非常重要，它确保 Prometheus 保留由推送任务设置的作业名称和实例名称标签，而不是用 Prometheus 自己的标签覆盖它们。

## 推送指标到 Pushgateway

Pushgateway 支持多种方式推送指标数据。

### 使用命令行工具

#### 使用 curl 命令推送

最简单的方式是使用 curl 命令推送文本格式的指标：

```bash
echo "batch_job_duration_seconds 42.5" | curl --data-binary @- http://pushgateway:9091/metrics/job/batch_job
```

带有标签的指标：

```bash
cat <<EOF | curl --data-binary @- http://pushgateway:9091/metrics/job/batch_job/instance/batch-instance
# TYPE batch_job_duration_seconds gauge
# HELP batch_job_duration_seconds Duration of batch job in seconds
batch_job_duration_seconds 42.5
# TYPE batch_job_records_processed counter
# HELP batch_job_records_processed Number of records processed by the batch job
batch_job_records_processed 10000
EOF
```

#### 使用 Prometheus 官方工具 pushgateway_cli

```bash
# 安装工具
go get github.com/prometheus/pushgateway_cli

# 推送指标
pushgateway_cli --gateway=http://pushgateway:9091 \
  --job=batch_job \
  --instance=batch-instance \
  gauge batch_job_duration_seconds 42.5 \
  counter batch_job_records_processed 10000
```

### 使用编程语言客户端库

#### Java 客户端示例

使用 Prometheus Java 客户端库：

```java
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;

public class PushgatewayExample {
    public static void main(String[] args) throws Exception {
        // 创建注册表
        CollectorRegistry registry = new CollectorRegistry();
        
        // 创建指标
        Gauge duration = Gauge.build()
            .name("batch_job_duration_seconds")
            .help("Duration of batch job in seconds")
            .register(registry);
            
        Counter recordsProcessed = Counter.build()
            .name("batch_job_records_processed")
            .help("Number of records processed by the batch job")
            .register(registry);
            
        // 设置指标值
        duration.set(42.5);
        recordsProcessed.inc(10000);
        
        // 推送到 Pushgateway
        PushGateway pg = new PushGateway("pushgateway:9091");
        pg.pushAdd(registry, "batch_job", "batch-instance");
    }
}
```

#### Python 客户端示例

使用 Prometheus Python 客户端库：

```python
from prometheus_client import CollectorRegistry, Gauge, Counter, push_to_gateway

# 创建注册表
registry = CollectorRegistry()

# 创建指标
duration = Gauge('batch_job_duration_seconds', 
                'Duration of batch job in seconds', 
                registry=registry)
records_processed = Counter('batch_job_records_processed', 
                          'Number of records processed by the batch job', 
                          registry=registry)

# 设置指标值
duration.set(42.5)
records_processed.inc(10000)

# 推送到 Pushgateway
push_to_gateway('pushgateway:9091', job='batch_job', 
               registry=registry, grouping_key={'instance': 'batch-instance'})
```

#### Node.js 客户端示例

使用 prom-client 库：

```javascript
const client = require('prom-client');
const gateway = new client.Pushgateway('http://pushgateway:9091');

// 创建注册表
const registry = new client.Registry();

// 创建指标
const duration = new client.Gauge({
  name: 'batch_job_duration_seconds',
  help: 'Duration of batch job in seconds',
  registers: [registry]
});

const recordsProcessed = new client.Counter({
  name: 'batch_job_records_processed',
  help: 'Number of records processed by the batch job',
  registers: [registry]
});

// 设置指标值
duration.set(42.5);
recordsProcessed.inc(10000);

// 推送到 Pushgateway
gateway.pushAdd({ jobName: 'batch_job', groupings: { instance: 'batch-instance' } }, registry)
  .then(() => console.log('Metrics pushed to Pushgateway'))
  .catch((err) => console.error('Error pushing metrics:', err));
```

## HTTP API 详解

Pushgateway 提供了多个 HTTP 端点用于不同的操作。

### 推送指标 API

- **PUT** `/metrics/job/<JOB_NAME>/instance/<INSTANCE_NAME>`: 替换指定作业和实例的所有指标
- **POST** `/metrics/job/<JOB_NAME>/instance/<INSTANCE_NAME>`: 更新/新增指定作业和实例的指标
- **DELETE** `/metrics/job/<JOB_NAME>/instance/<INSTANCE_NAME>`: 删除指定作业和实例的所有指标

URL 路径包含标签，格式为 `/metrics/{label_name}/{label_value}...`，必须至少包含 `job` 标签。

### 查询指标 API

- **GET** `/metrics`: 返回所有推送的指标和 Pushgateway 自身的指标
- **GET** `/api/v1/metrics`: 返回 JSON 格式的所有推送指标

### 管理 API

- **DELETE** `/api/v1/admin/wipe`: 删除所有指标数据（需启用 `--web.enable-admin-api`）
- **PUT** `/api/v1/admin/shutdown`: 触发优雅关闭（需启用 `--web.enable-lifecycle`）

## 指标设计最佳实践

### 命名规范

遵循 Prometheus 命名规范：

- 使用小写字母和下划线（如 `batch_job_duration_seconds`）
- 使用 `_total` 后缀表示计数器（如 `records_processed_total`）
- 使用适当的单位后缀（如 `_seconds`, `_bytes`, `_count`）
- 使用命名空间前缀避免冲突（如 `myapp_batch_job_duration_seconds`）

### 标签使用策略

- **必要标签**：每个指标至少应包含 `job` 和 `instance` 标签
- **基数控制**：避免使用高基数标签（如用户 ID、会话 ID）
- **一致性**：在相关指标间保持标签一致性
- **有意义的值**：使用有意义的标签值，避免空字符串或通配符

### 指标类型选择

选择适当的指标类型：

- **计数器（Counter）**：对于只增不减的累积值（如处理的记录数）
- **仪表（Gauge）**：对于可上可下的值（如内存使用、队列长度）
- **直方图（Histogram）**：对于需要分布分析的值（如请求持续时间）
- **摘要（Summary）**：对于需要服务端计算分位数的值

## 最佳实践与使用场景

### 常见使用场景及实现方式

#### 批处理任务监控

对于批处理任务，记录执行时间、处理数量、成功率等指标：

```python
import time
from prometheus_client import CollectorRegistry, Gauge, Counter, push_to_gateway

def run_batch_job():
    # 初始化注册表和指标
    registry = CollectorRegistry()
    duration = Gauge('batch_job_duration_seconds', 'Duration of batch job', registry=registry)
    records_processed = Counter('batch_job_records_processed_total', 'Records processed', registry=registry)
    failures = Counter('batch_job_failures_total', 'Processing failures', registry=registry)
    
    # 记录开始时间
    start_time = time.time()
    
    try:
        # 执行批处理任务
        processed = process_data()
        records_processed.inc(processed)
        
    except Exception as e:
        # 记录故障
        failures.inc()
        raise
    
    finally:
        # 记录持续时间并推送指标
        duration.set(time.time() - start_time)
        push_to_gateway('pushgateway:9091', job='data_processor', registry=registry)
```

#### 定时任务监控

对于 cron 作业，记录上次执行成功时间和状态：

```bash
#!/bin/bash

# 记录开始时间
START_TIME=$(date +%s)

# 执行备份
backup_result=0
/usr/local/bin/backup.sh || backup_result=1

# 计算执行时间
DURATION=$(($(date +%s) - $START_TIME))

# 推送指标到 Pushgateway
cat <<EOF | curl -s --data-binary @- http://pushgateway:9091/metrics/job/backup/instance/$(hostname)
# TYPE backup_last_execution_timestamp gauge
# HELP backup_last_execution_timestamp Timestamp of the last backup job execution
backup_last_execution_timestamp $(date +%s)

# TYPE backup_duration_seconds gauge
# HELP backup_duration_seconds Duration of backup job in seconds
backup_duration_seconds $DURATION

# TYPE backup_success gauge
# HELP backup_success Whether the backup job was successful (1 for success, 0 for failure)
backup_success $((1 - $backup_result))
EOF

exit $backup_result
```

#### CI/CD 流水线指标收集

在 CI/CD 流水线中收集构建和部署指标：

```groovy
// Jenkins Pipeline 示例
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                script {
                    def startTime = new Date().getTime()
                    
                    try {
                        // 执行构建
                        sh 'mvn clean package'
                        
                        // 推送成功指标
                        sh """
                            cat <<EOF | curl -s --data-binary @- http://pushgateway:9091/metrics/job/ci_build/instance/\${JOB_NAME}
                            # TYPE build_duration_seconds gauge
                            # HELP build_duration_seconds Duration of build in seconds
                            build_duration_seconds \$((\$(date +%s) - ${startTime/1000}))
                            
                            # TYPE build_success gauge
                            # HELP build_success Build success status (1=success, 0=failure)
                            build_success 1
                            
                            # TYPE build_last_success_timestamp gauge
                            # HELP build_last_success_timestamp Timestamp of last successful build
                            build_last_success_timestamp \$(date +%s)
                            EOF
                        """
                    } catch (Exception e) {
                        // 推送失败指标
                        sh """
                            cat <<EOF | curl -s --data-binary @- http://pushgateway:9091/metrics/job/ci_build/instance/\${JOB_NAME}
                            # TYPE build_duration_seconds gauge
                            # HELP build_duration_seconds Duration of build in seconds
                            build_duration_seconds \$((\$(date +%s) - ${startTime/1000}))
                            
                            # TYPE build_success gauge
                            # HELP build_success Build success status (1=success, 0=failure)
                            build_success 0
                            EOF
                        """
                        throw e
                    }
                }
            }
        }
    }
}
```

### 最佳实践

#### 指标生命周期管理

Pushgateway 默认会无限期保留推送的指标，这可能导致过时信息影响监控。推荐的做法：

1. **作业开始时清理旧指标**：
   ```bash
   curl -X DELETE http://pushgateway:9091/metrics/job/batch_job/instance/batch-instance
   ```

2. **使用时间戳指标**：
   ```
   job_last_run_timestamp{job="batch_job"} 1635945113
   ```

3. **在 Prometheus 侧使用查询过滤**：
   ```
   job_success{job="batch_job"} and (time() - job_last_run_timestamp{job="batch_job"} < 86400)
   ```

#### 数据分组策略

有效使用标签进行数据分组：

1. **基于任务类型分组**：使用 `job` 标签区分不同类型的任务
2. **基于执行环境分组**：使用 `instance` 或 `environment` 标签区分不同环境
3. **基于重要性分组**：使用 `priority` 或 `criticality` 标签区分关键任务
4. **避免过度分组**：标签组合数量应保持在可管理的范围内

#### 告警设置

针对批处理作业的常见告警：

1. **执行超时告警**：
   ```
   batch_job_duration_seconds{job="critical_etl"} > 3600
   ```

2. **未执行告警**：
   ```
   time() - batch_job_last_success_timestamp{job="daily_backup"} > 86400
   ```

3. **失败率告警**：
   ```
   batch_job_failures_total{job="payment_processing"} / batch_job_runs_total{job="payment_processing"} > 0.01
   ```

## 高级功能与场景

### 集成到大数据处理流程

#### 在 Spark 作业中集成

使用 Spark 的 accumulators 收集指标并推送：

```scala
import org.apache.spark.util.AccumulatorV2
import org.apache.spark.{SparkConf, SparkContext}
import io.prometheus.client.{CollectorRegistry, Counter, Gauge}
import io.prometheus.client.exporter.PushGateway

object SparkPushgatewayExample {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("SparkPushgatewayExample")
    val sc = new SparkContext(conf)
    
    // 创建累加器
    val recordsProcessed = sc.longAccumulator("recordsProcessed")
    val processingErrors = sc.longAccumulator("processingErrors")
    
    try {
      // 处理数据
      val data = sc.textFile("hdfs://path/to/data")
      val processed = data.map(line => {
        try {
          // 处理数据
          recordsProcessed.add(1)
          // 返回处理结果
          true
        } catch {
          case e: Exception =>
            processingErrors.add(1)
            false
        }
      })
      
      // 触发计算
      val successCount = processed.filter(identity).count()
      
      // 创建注册表并推送指标
      val registry = new CollectorRegistry()
      val recordsGauge = Gauge.build()
        .name("spark_job_records_processed")
        .help("Total records processed")
        .register(registry)
      recordsGauge.set(recordsProcessed.value)
      
      val errorsGauge = Gauge.build()
        .name("spark_job_processing_errors")
        .help("Total processing errors")
        .register(registry)
      errorsGauge.set(processingErrors.value)
      
      val pg = new PushGateway("pushgateway:9091")
      pg.pushAdd(registry, "spark_job", 
                java.util.Collections.singletonMap("instance", sc.applicationId))
      
    } finally {
      sc.stop()
    }
  }
}
```

#### 在 Hadoop MapReduce 作业中集成

在 MapReduce 作业中使用计数器收集指标：

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import io.prometheus.client.*;
import io.prometheus.client.exporter.PushGateway;

public class MapReducePushgatewayExample {
    
    public static class MetricsMapper extends Mapper<Object, Text, Text, Text> {
        private enum Counters {
            RECORDS_PROCESSED,
            PROCESSING_ERRORS
        }
        
        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            try {
                // 处理数据
                context.getCounter(Counters.RECORDS_PROCESSED).increment(1);
                // 输出结果
                context.write(new Text("output"), value);
            } catch (Exception e) {
                context.getCounter(Counters.PROCESSING_ERRORS).increment(1);
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "MapReducePushgatewayExample");
        
        // 配置作业
        job.setJarByClass(MapReducePushgatewayExample.class);
        job.setMapperClass(MetricsMapper.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setInputPath(new Path(args[0]));
        job.setOutputPath(new Path(args[1]));
        
        // 运行作业
        long startTime = System.currentTimeMillis();
        boolean success = job.waitForCompletion(true);
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        
        // 收集计数器
        Counter recordsProcessed = job.getCounters().findCounter(
            MetricsMapper.Counters.RECORDS_PROCESSED);
        Counter processingErrors = job.getCounters().findCounter(
            MetricsMapper.Counters.PROCESSING_ERRORS);
        
        // 推送指标
        CollectorRegistry registry = new CollectorRegistry();
        
        Gauge jobDuration = Gauge.build()
            .name("hadoop_job_duration_seconds")
            .help("Duration of Hadoop job in seconds")
            .register(registry);
        jobDuration.set(duration);
        
        Gauge jobSuccess = Gauge.build()
            .name("hadoop_job_success")
            .help("Whether the Hadoop job was successful (1 for success, 0 for failure)")
            .register(registry);
        jobSuccess.set(success ? 1 : 0);
        
        Gauge recordsGauge = Gauge.build()
            .name("hadoop_job_records_processed")
            .help("Total records processed")
            .register(registry);
        recordsGauge.set(recordsProcessed.getValue());
        
        Gauge errorsGauge = Gauge.build()
            .name("hadoop_job_processing_errors")
            .help("Total processing errors")
            .register(registry);
        errorsGauge.set(processingErrors.getValue());
        
        PushGateway pg = new PushGateway("pushgateway:9091");
        pg.pushAdd(registry, "hadoop_job", 
                  Collections.singletonMap("instance", job.getJobID().toString()));
        
        System.exit(success ? 0 : 1);
    }
}
```

### 高可用部署策略

#### 多实例部署

对于高可用性要求，可以部署多个 Pushgateway 实例并使用负载均衡：

```
┌───────────┐     ┌────────────────┐     ┌───────────────┐
│ 批处理任务 │────►│  负载均衡器    │────►│ Pushgateway 1 │
└───────────┘     └────────────────┘     └───────────────┘
                          │                      ▲
                          │                      │
                          │               ┌──────┴──────┐
                          │               │  共享存储   │
                          │               └──────┬──────┘
                          │                      │
                          │                      ▼
                          └─────────────────►│ Pushgateway 2 │
                                             └───────────────┘
```

在客户端实现中，可以配置重试逻辑和故障转移：

```python
def push_with_retry(registry, job, instance, gateways, max_retries=3):
    """尝试推送到多个 Pushgateway 实例，有重试逻辑"""
    for attempt in range(max_retries):
        # 随机选择一个 gateway
        gateway = random.choice(gateways)
        try:
            print(f"Attempting to push to {gateway}, attempt {attempt+1}")
            push_to_gateway(
                gateway, 
                job=job, 
                registry=registry,
                grouping_key={'instance': instance}
            )
            print("Push successful")
            return True
        except Exception as e:
            print(f"Push failed: {e}")
            time.sleep(1)  # 稍等片刻再重试
    
    # 所有尝试都失败
    print("All push attempts failed")
    return False
```

#### 使用消息队列作为缓冲

在高要求场景中，可以使用消息队列作为中间缓冲：

```
┌───────────┐     ┌────────────────┐     ┌───────────────┐
│ 批处理任务 │────►│  消息队列      │────►│ 指标收集服务  │────►│ Pushgateway │
└───────────┘     └────────────────┘     └───────────────┘     └────────────┘
```

这种方式可以在 Pushgateway 不可用时临时存储指标，并在恢复后重新推送。

## 故障排除与常见问题

### 常见错误和解决方案

#### 指标不显示在 Prometheus 中

**症状**：推送到 Pushgateway 的指标在 Prometheus 中不可见。

**可能原因和解决方案**：

1. **Prometheus 未配置抓取 Pushgateway**：
   - 检查 Prometheus 配置是否包含 Pushgateway 抓取配置
   - 验证 `honor_labels: true` 设置是否存在

2. **推送格式不正确**：
   - 验证推送的指标格式是否符合 Prometheus 格式
   - 检查是否提供了所有必需标签

3. **网络连接问题**：
   - 确认 Prometheus 可以访问 Pushgateway
   - 检查防火墙和网络策略

#### 旧指标无法清除

**症状**：已停止的作业的指标仍然显示在 Prometheus 中。

**可能原因和解决方案**：

1. **未明确删除指标**：
   - 在作业启动时使用 DELETE 请求清除旧指标
   - 设计作业使其在完成时清理自己的指标

2. **标签组合不唯一**：
   - 确保使用足够的标签组合以唯一标识作业实例
   - 使用更具体的 instance 标签值（如包含时间戳）

3. **Pushgateway 持久化问题**：
   - 检查 Pushgateway 持久化配置
   - 如有必要，重新启动 Pushgateway 实例

### 监控 Pushgateway 本身

为确保 Pushgateway 正常运行，应该监控 Pushgateway 自身的健康状况：

1. **基本健康检查**：
   - 设置对 `/` 或 `/metrics` 端点的定期 HTTP 检查
   - 配置警报以检测服务不可用的情况

2. **关键指标监控**：

   - `pushgateway_http_requests_total`：请求总数，按状态码分类
   - `pushgateway_metrics_pushed_total`：推送的指标总数
   - `pushgateway_build_info`：版本信息
   - `process_cpu_seconds_total`：CPU 使用情况
   - `process_resident_memory_bytes`：内存使用情况

3. **告警规则示例**：

```yaml
groups:
  - name: pushgateway
    rules:
      - alert: PushgatewayDown
        expr: up{job="pushgateway"} == 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Pushgateway down (instance {{ $labels.instance }})"
          description: "Pushgateway instance is down\n  VALUE = {{ $value }}\n  LABELS = {{ $labels }}"
      
      - alert: PushgatewayHighErrorRate
        expr: sum(rate(pushgateway_http_requests_total{code=~"5.."}[5m])) / sum(rate(pushgateway_http_requests_total[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Pushgateway high error rate (instance {{ $labels.instance }})"
          description: "Pushgateway error rate is above 5%\n  VALUE = {{ $value }}\n  LABELS = {{ $labels }}"
```

## 安全最佳实践

保护 Pushgateway 免受未授权访问和滥用非常重要。

### 启用 TLS 和认证

要保护 Pushgateway 服务，创建一个 Web 配置文件（如 `web-config.yml`）：

```yaml
tls_server_config:
  cert_file: server.crt
  key_file: server.key

basic_auth_users:
  prometheus: $2y$10$uWbALleD1jDJzCpWsMRkquVrjMYK7EgdqMEn6hR4lR1jbCki5Y/5.  # 密码：prometheus
  pushclient: $2y$10$FaYxfsfZA/MUbvASDMvbPuP4uxXiOVFKqgj1e/iDY.j6Lb9.pa6fG  # 密码：pushclient
```

启动 Pushgateway 时指定此配置：

```bash
./pushgateway --web.config.file=web-config.yml
```

### 网络安全策略

- **防火墙规则**：限制对 Pushgateway 端口（默认 9091）的访问
- **网络隔离**：将 Pushgateway 部署在内部网络中
- **反向代理**：使用反向代理（如 Nginx）为 Pushgateway 提供额外的安全层
- **IP 白名单**：限制只允许特定 IP 地址访问 Pushgateway

### Docker 安全配置

使用安全配置运行 Docker 容器：

```bash
docker run -d \
  -p 9091:9091 \
  --name pushgateway \
  --user nobody:nogroup \
  --read-only \
  --cap-drop ALL \
  --security-opt no-new-privileges \
  -v /path/to/data:/pushgateway:rw \
  -v /path/to/web-config.yml:/etc/pushgateway/web-config.yml:ro \
  prom/pushgateway \
  --web.config.file=/etc/pushgateway/web-config.yml \
  --persistence.file=/pushgateway/metrics.data
```

## 总结

Pushgateway 是 Prometheus 生态系统中解决短暂作业和批处理任务监控的关键组件。通过本指南，您应该能够：

- 安装和配置 Pushgateway 服务
- 从各种编程语言和脚本中推送指标
- 实施适当的安全性和高可用性措施
- 集成到复杂的数据处理工作流程中
- 设计有效的指标和监控策略

遵循本指南的最佳实践，您可以构建一个强大的批处理任务监控系统，帮助您更好地了解这些关键但短暂的工作负载的性能和健康状况。 