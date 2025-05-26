# Prometheus 用户指南

本指南将详细介绍如何在大数据平台中使用 Prometheus 监控系统，包括安装配置、数据收集、查询分析和告警管理，以及开发过程中的最佳实践。

## 准备工作

在开始使用 Prometheus 之前，您需要了解以下基本信息：

- Prometheus 服务的运行环境要求
- 需要监控的目标系统和指标
- 数据存储和保留策略
- 告警和通知要求

## 安装与配置

Prometheus 的部署非常灵活，可以根据实际需求选择适合的安装方式。

### 二进制包安装

1. 从 Prometheus 官网下载适合您操作系统的二进制包：

```bash
# Linux 64位系统示例
wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz
```

2. 解压安装包：

```bash
tar -xzf prometheus-2.45.0.linux-amd64.tar.gz
cd prometheus-2.45.0.linux-amd64/
```

3. 修改配置文件 `prometheus.yml`：

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

4. 启动 Prometheus 服务：

```bash
./prometheus --config.file=prometheus.yml
```

### Docker 安装

使用 Docker 运行 Prometheus：

```bash
# 创建配置文件目录
mkdir -p /opt/prometheus/config

# 创建配置文件
cat > /opt/prometheus/config/prometheus.yml << EOF
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
EOF

# 启动 Prometheus 容器
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v /opt/prometheus/config:/etc/prometheus \
  prom/prometheus
```

### Kubernetes 安装

在 Kubernetes 环境中，建议使用 Prometheus Operator 或 Helm Chart 安装：

```bash
# 使用 Helm 安装 Prometheus
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install prometheus prometheus-community/prometheus \
  --namespace monitoring \
  --create-namespace
```

## 基本配置详解

Prometheus 的配置主要通过 YAML 文件进行，以下是主要配置部分的详解。

### 全局配置

```yaml
global:
  # 默认抓取间隔
  scrape_interval: 15s
  # 规则评估间隔
  evaluation_interval: 15s
  # 抓取超时
  scrape_timeout: 10s
  # 外部标签，添加到所有时间序列和告警
  external_labels:
    cluster: 'production'
    environment: 'staging'
```

### 数据抓取配置

```yaml
scrape_configs:
  - job_name: 'node_exporter'
    # 覆盖全局抓取间隔
    scrape_interval: 10s
    # 静态目标
    static_configs:
      - targets: ['node1:9100', 'node2:9100']
        labels:
          group: 'production'
    
  - job_name: 'api_servers'
    # 基于文件的服务发现
    file_sd_configs:
      - files:
        - 'targets/*.json'
        refresh_interval: 5m
```

### 告警配置

```yaml
# 告警规则文件
rule_files:
  - 'alert_rules.yml'

# Alertmanager 配置
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

### 存储配置

```yaml
# 本地存储配置
storage:
  tsdb:
    # 数据保留时间
    retention.time: 15d
    # 最大存储空间
    retention.size: 50GB
    # 数据目录
    path: /data
```

### 远程存储配置

```yaml
# 远程写入配置
remote_write:
  - url: "https://remote-storage-server/api/v1/write"
    basic_auth:
      username: "username"
      password: "password"

# 远程读取配置
remote_read:
  - url: "https://remote-storage-server/api/v1/read"
    read_recent: true
```

## 添加监控目标

Prometheus 可以通过多种方式发现和添加监控目标。

### 静态配置

直接在配置文件中指定目标：

```yaml
scrape_configs:
  - job_name: 'static_targets'
    static_configs:
      - targets: ['server1:9100', 'server2:9100']
        labels:
          env: 'production'
```

### 基于文件的服务发现

使用 JSON 文件动态配置目标：

```yaml
scrape_configs:
  - job_name: 'file_targets'
    file_sd_configs:
      - files:
          - '/etc/prometheus/targets/*.json'
        refresh_interval: 5m
```

JSON 文件格式：

```json
[
  {
    "targets": ["host1:9100", "host2:9100"],
    "labels": {
      "env": "production",
      "job": "node"
    }
  }
]
```

### Kubernetes 服务发现

监控 Kubernetes 集群内的服务：

```yaml
scrape_configs:
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
```

### DNS 服务发现

基于 DNS SRV 记录发现目标：

```yaml
scrape_configs:
  - job_name: 'dns_targets'
    dns_sd_configs:
      - names:
          - 'service.consul'
        type: 'SRV'
        refresh_interval: 30s
```

## 部署常用 Exporter

Exporter 是 Prometheus 生态系统中用于暴露特定系统或服务指标的组件。

### Node Exporter

监控主机系统指标：

```bash
# 下载并安装 Node Exporter
wget https://github.com/prometheus/node_exporter/releases/download/v1.5.0/node_exporter-1.5.0.linux-amd64.tar.gz
tar -xzf node_exporter-1.5.0.linux-amd64.tar.gz
cd node_exporter-1.5.0.linux-amd64

# 启动 Node Exporter
./node_exporter
```

添加到 Prometheus 配置：

```yaml
scrape_configs:
  - job_name: 'node'
    static_configs:
      - targets: ['node1:9100', 'node2:9100']
```

### MySQL Exporter

监控 MySQL 数据库：

```bash
# 下载并安装 MySQL Exporter
wget https://github.com/prometheus/mysqld_exporter/releases/download/v0.14.0/mysqld_exporter-0.14.0.linux-amd64.tar.gz
tar -xzf mysqld_exporter-0.14.0.linux-amd64.tar.gz
cd mysqld_exporter-0.14.0.linux-amd64

# 创建配置文件
cat > .my.cnf << EOF
[client]
user=exporter
password=password
host=localhost
EOF

# 启动 MySQL Exporter
./mysqld_exporter --config.my-cnf=.my.cnf
```

添加到 Prometheus 配置：

```yaml
scrape_configs:
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-server:9104']
```

### JMX Exporter

监控 Java 应用程序：

1. 下载 JMX Exporter JAR 文件：

```bash
wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.17.0/jmx_prometheus_javaagent-0.17.0.jar
```

2. 创建配置文件 `config.yaml`：

```yaml
---
lowercaseOutputName: true
lowercaseOutputLabelNames: true
rules:
  - pattern: ".*"
```

3. 将 JMX Exporter 作为 Java 代理添加到 Java 应用：

```bash
java -javaagent:./jmx_prometheus_javaagent-0.17.0.jar=8080:config.yaml -jar your-application.jar
```

### Blackbox Exporter

进行黑盒监控（如 HTTP 探针）：

```bash
# 下载并安装 Blackbox Exporter
wget https://github.com/prometheus/blackbox_exporter/releases/download/v0.23.0/blackbox_exporter-0.23.0.linux-amd64.tar.gz
tar -xzf blackbox_exporter-0.23.0.linux-amd64.tar.gz
cd blackbox_exporter-0.23.0.linux-amd64

# 创建配置文件
cat > blackbox.yml << EOF
modules:
  http_2xx:
    prober: http
    timeout: 5s
    http:
      preferred_ip_protocol: "ip4"
EOF

# 启动 Blackbox Exporter
./blackbox_exporter --config.file=blackbox.yml
```

Prometheus 配置：

```yaml
scrape_configs:
  - job_name: 'blackbox'
    metrics_path: /probe
    params:
      module: [http_2xx]
    static_configs:
      - targets:
        - https://example.com
        - https://prometheus.io
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: 127.0.0.1:9115  # Blackbox exporter.
```

## 使用 Pushgateway

对于短期批处理作业，可以使用 Pushgateway 推送指标。

### 安装 Pushgateway

```bash
# 下载并安装 Pushgateway
wget https://github.com/prometheus/pushgateway/releases/download/v1.5.1/pushgateway-1.5.1.linux-amd64.tar.gz
tar -xzf pushgateway-1.5.1.linux-amd64.tar.gz
cd pushgateway-1.5.1.linux-amd64

# 启动 Pushgateway
./pushgateway
```

### 推送指标到 Pushgateway

使用命令行工具：

```bash
echo "batch_job_status{job=\"batch_job\", instance=\"batch-instance\"} 1" | curl --data-binary @- http://localhost:9091/metrics/job/batch_job
```

使用 Python 客户端：

```python
from prometheus_client import CollectorRegistry, Gauge, push_to_gateway

registry = CollectorRegistry()
g = Gauge('job_last_success_unixtime', 'Last time a batch job successfully finished',
          registry=registry)
g.set_to_current_time()
push_to_gateway('localhost:9091', job='batch_job', registry=registry)
```

### 配置 Prometheus 抓取 Pushgateway

```yaml
scrape_configs:
  - job_name: 'pushgateway'
    honor_labels: true
    static_configs:
      - targets: ['localhost:9091']
```

## 查询和分析数据

Prometheus 提供了强大的查询语言 PromQL，用于分析时间序列数据。

### 基本查询

访问 Prometheus Web UI，通常在 `http://<prometheus-host>:9090`。

查询示例：

- 获取单个指标：
  ```
  node_cpu_seconds_total
  ```

- 带标签过滤的查询：
  ```
  node_cpu_seconds_total{mode="idle"}
  ```

- 范围向量（过去5分钟的数据）：
  ```
  node_cpu_seconds_total{mode="idle"}[5m]
  ```

### 常用 PromQL 函数和操作符

- 计算速率：
  ```
  rate(node_cpu_seconds_total{mode="idle"}[5m])
  ```

- 求和聚合：
  ```
  sum(rate(node_cpu_seconds_total{mode!="idle"}[5m])) by (instance)
  ```

- 计算 CPU 使用率：
  ```
  100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
  ```

- 预测磁盘将满的时间：
  ```
  predict_linear(node_filesystem_free_bytes{mountpoint="/"}[1h], 24 * 3600) < 0
  ```

- 计算请求延迟的 95 百分位：
  ```
  histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))
  ```

### 使用控制台模板

创建自定义控制台模板：

1. 创建模板文件 `console_templates/node.html`：

```html
{{template "head" .}}
<h1>Node Exporter</h1>

<h3>CPU Usage</h3>
<div id="cpuGraph"></div>
<script>
new PromConsole.Graph({
  node: document.querySelector("#cpuGraph"),
  expr: 'sum by(mode) (rate(node_cpu_seconds_total{instance="{{.Params.instance}}",mode!="idle"}[5m]))',
  renderer: 'area',
  height: 300
});
</script>

{{template "tail" .}}
```

2. 在 Prometheus 配置中启用控制台：

```yaml
web:
  console:
    templates: 'console_templates'
    libraries: 'console_libraries'
```

## 配置告警规则

告警规则允许您定义条件，当满足这些条件时将触发告警。

### 创建告警规则文件

创建文件 `alert_rules.yml`：

```yaml
groups:
  - name: example
    rules:
      - alert: HighCpuLoad
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU load (instance {{ $labels.instance }})"
          description: "CPU load is > 80%\n  VALUE = {{ $value }}\n  LABELS = {{ $labels }}"
          
      - alert: HostOutOfMemory
        expr: node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes * 100 < 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Host out of memory (instance {{ $labels.instance }})"
          description: "Node memory is filling up (< 10% left)\n  VALUE = {{ $value }}\n  LABELS = {{ $labels }}"
          
      - alert: HostOutOfDiskSpace
        expr: (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"} * 100) < 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Host out of disk space (instance {{ $labels.instance }})"
          description: "Disk is almost full (< 10% left)\n  VALUE = {{ $value }}\n  LABELS = {{ $labels }}"
```

### 在 Prometheus 配置中引用告警规则

更新 `prometheus.yml`：

```yaml
rule_files:
  - "alert_rules.yml"
```

### 配置 Alertmanager

1. 下载并安装 Alertmanager：

```bash
wget https://github.com/prometheus/alertmanager/releases/download/v0.25.0/alertmanager-0.25.0.linux-amd64.tar.gz
tar -xzf alertmanager-0.25.0.linux-amd64.tar.gz
cd alertmanager-0.25.0.linux-amd64
```

2. 创建配置文件 `alertmanager.yml`：

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'email-notifications'

receivers:
- name: 'email-notifications'
  email_configs:
  - to: 'alerts@example.com'
    from: 'prometheus@example.com'
    smarthost: 'smtp.example.com:587'
    auth_username: 'alerts@example.com'
    auth_password: 'password'
    send_resolved: true
```

3. 启动 Alertmanager：

```bash
./alertmanager --config.file=alertmanager.yml
```

4. 在 Prometheus 配置中添加 Alertmanager：

```yaml
alerting:
  alertmanagers:
  - static_configs:
    - targets:
      - localhost:9093
```

## 与应用程序集成

### 在 Java 应用中集成

使用 `micrometer-registry-prometheus` 库：

1. 添加依赖：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>1.10.5</version>
</dependency>
```

2. 配置 Prometheus 注册表：

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

// 创建 Prometheus 注册表
PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

// 创建并递增计数器
Counter counter = registry.counter("my_counter", "tag1", "value1");
counter.increment();

// 暴露 /metrics 端点
// 使用 Spring Boot 或其他 Web 框架
@GetMapping("/metrics")
public String metrics() {
    return registry.scrape();
}
```

### 在 Python 应用中集成

使用 `prometheus_client` 库：

1. 安装库：

```bash
pip install prometheus_client
```

2. 在应用中使用：

```python
from prometheus_client import start_http_server, Counter, Gauge, Summary

# 创建指标
REQUEST_COUNT = Counter('app_requests_total', 'Total app HTTP requests')
REQUEST_LATENCY = Summary('app_request_latency_seconds', 'Request latency in seconds')
INPROGRESS = Gauge('app_requests_inprogress', 'Number of in-progress requests')

# 启动 metrics 服务器
start_http_server(8000)

# 在应用代码中使用指标
@REQUEST_LATENCY.time()
def process_request():
    INPROGRESS.inc()
    REQUEST_COUNT.inc()
    # 处理请求
    INPROGRESS.dec()
```

### 在 Node.js 应用中集成

使用 `prom-client` 库：

1. 安装库：

```bash
npm install prom-client
```

2. 在应用中使用：

```javascript
const express = require('express');
const app = express();
const client = require('prom-client');

// 创建注册表
const register = new client.Registry();
client.collectDefaultMetrics({ register });

// 创建自定义指标
const httpRequestCounter = new client.Counter({
  name: 'http_requests_total',
  help: 'Total number of HTTP requests',
  labelNames: ['method', 'route', 'status_code'],
  registers: [register]
});

// 暴露 metrics 端点
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});

// 使用中间件记录请求
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    httpRequestCounter.inc({
      method: req.method,
      route: req.route ? req.route.path : req.path,
      status_code: res.statusCode
    });
  });
  next();
});

app.listen(3000);
```

## 可视化与仪表板

### 与 Grafana 集成

1. 安装 Grafana：

```bash
# 使用 Docker 安装
docker run -d -p 3000:3000 --name grafana grafana/grafana
```

2. 添加 Prometheus 数据源：

- 访问 Grafana 界面（默认 http://localhost:3000，用户名/密码：admin/admin）
- 点击 Configuration > Data Sources > Add data source
- 选择 Prometheus 类型
- 设置 URL 为 Prometheus 服务器地址（如 http://prometheus:9090）
- 点击 Save & Test

3. 导入预配置仪表板：

- 点击 + > Import
- 输入仪表板 ID（如 Node Exporter 的 1860）
- 选择 Prometheus 数据源
- 点击 Import

### 创建自定义仪表板

1. 点击 + > Dashboard > Add new panel
2. 在 Query 选项卡中输入 PromQL 查询：
   ```
   100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
   ```
3. 在 Panel 选项卡中设置标题为 "CPU Usage"
4. 在 Field 选项卡中设置单位为 "Percent (0-100)"
5. 点击 Apply 保存面板

## 性能优化

### 存储优化

- **调整保留策略**：根据实际需求设置数据保留时间
  ```yaml
  storage:
    tsdb:
      retention.time: 15d
      retention.size: 30GB
  ```

- **使用压缩**：对长期存储数据启用压缩
  ```yaml
  storage:
    tsdb:
      min_block_duration: 2h
      max_block_duration: 6h
  ```

- **使用远程存储**：对历史数据使用长期存储方案

### 查询优化

- **使用记录规则**：预计算常用查询，创建 `recording_rules.yml`：
  ```yaml
  groups:
    - name: cpu_rules
      interval: 5m
      rules:
        - record: instance:node_cpu_utilisation:rate5m
          expr: 1 - avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m]))
  ```

- **限制时间范围**：查询时指定合适的时间范围
- **避免使用高基数标签**：防止查询性能下降和存储膨胀

### 抓取优化

- **调整抓取间隔**：根据指标变化频率和重要性设置合适的抓取间隔
  ```yaml
  scrape_configs:
    - job_name: 'critical_service'
      scrape_interval: 10s
    - job_name: 'non_critical_service'
      scrape_interval: 1m
  ```

- **使用过滤器**：只收集必要的指标
  ```yaml
  scrape_configs:
    - job_name: 'filtered_job'
      metric_relabel_configs:
        - source_labels: [__name__]
          regex: 'go_.*'
          action: drop
  ```

## 安全最佳实践

### 认证与授权

使用基本认证保护 Prometheus API：

```yaml
# prometheus.yml
web:
  basic_auth_users:
    admin: $2y$12$...  # 使用 bcrypt 算法加密的密码
```

使用 TLS 加密：

```yaml
web:
  tls_server_config:
    cert_file: server.crt
    key_file: server.key
```

### 网络安全

- **使用防火墙**：限制对 Prometheus 端口的访问
- **使用反向代理**：通过 Nginx 或 Apache 提供额外的安全层
- **使用 VPN**：将监控系统放在专用 VPN 中

### 安全检查清单

- [ ] 使用最新版本的 Prometheus 和组件
- [ ] 启用认证和 TLS
- [ ] 限制网络访问
- [ ] 定期备份配置和数据
- [ ] 使用最小权限原则配置 Exporters
- [ ] 审核 web 访问日志

## 排障指南

### 常见问题

1. **指标不可用**：
   - 检查目标是否可访问：`curl http://target:port/metrics`
   - 验证 Prometheus 配置是否正确
   - 检查防火墙设置
   - 查看 Prometheus 日志

2. **告警不触发**：
   - 验证告警规则语法
   - 检查告警条件是否满足
   - 验证 Alertmanager 连接
   - 查看告警状态页面

3. **查询性能低**：
   - 检查查询复杂度
   - 考虑使用记录规则
   - 检查时间范围
   - 减少高基数标签

### 诊断工具

- **状态页面**：访问 `/status` 页面查看 Prometheus 运行状态
- **目标页面**：访问 `/targets` 检查目标抓取状态
- **服务发现页面**：访问 `/service-discovery` 检查服务发现结果
- **配置页面**：访问 `/config` 查看当前配置
- **日志**：检查 Prometheus 日志文件或容器日志

## 最佳实践清单

### 命名和标签约定

- 使用有意义的指标名称，格式如 `namespace_subsystem_name_unit`
- 使用统一的标签命名，如 `environment`, `service`, `instance`
- 避免使用过多的标签，特别是高基数标签

### 监控覆盖

确保监控覆盖所有关键组件：

- [ ] 主机基础指标（CPU、内存、磁盘、网络）
- [ ] 中间件（数据库、消息队列、缓存）
- [ ] 应用程序（请求率、错误率、延迟）
- [ ] 业务指标（用户活动、交易量）
- [ ] 依赖服务（外部 API、第三方服务）

### 告警策略

- 设置有意义的告警阈值，避免噪音
- 使用 `for` 子句防止抖动触发告警
- 按严重性分类告警
- 提供清晰的告警描述和解决方案
- 定期审核和改进告警规则

## 总结

Prometheus 是一个功能强大的监控系统，适用于各种规模的应用和基础设施监控。本指南介绍了从安装配置到高级用法的各个方面，帮助您充分利用 Prometheus 提供的功能。

通过遵循本指南中的最佳实践，您可以：

- 有效收集和分析关键指标
- 设置有意义的告警
- 创建直观的可视化仪表板
- 优化 Prometheus 性能
- 确保监控系统的安全性

随着对 Prometheus 的深入了解和使用，您将能够构建一个全面、高效的监控系统，为您的应用和基础设施提供可靠的可观测性支持。 