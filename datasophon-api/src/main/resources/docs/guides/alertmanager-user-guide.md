# Alertmanager 用户指南

## 快速入门

Alertmanager 是处理由 Prometheus 服务器发送的告警的工具。本指南将帮助您快速上手使用 Alertmanager 的基本功能。

### 基本概念

在开始使用 Alertmanager 之前，了解以下核心概念非常重要：

- **告警（Alert）**：由 Prometheus 生成的告警事件
- **告警组（Group）**：相似告警的集合，减少告警风暴
- **路由（Route）**：决定告警发送到哪些接收器的规则
- **接收器（Receiver）**：告警通知的目标，如邮件、Slack 等
- **静默（Silence）**：临时禁用特定告警的通知
- **抑制（Inhibition）**：当某些告警触发时阻止其他告警通知

### 常用命令速查表

| 命令 | 描述 | 示例 |
|------|------|------|
| `amtool alert` | 查询当前活动告警 | `amtool alert` |
| `amtool silence add` | 添加静默规则 | `amtool silence add alertname=HighCpuLoad duration=1h` |
| `amtool silence query` | 查询静默规则 | `amtool silence query` |
| `amtool silence expire` | 使静默规则过期 | `amtool silence expire <silence-id>` |
| `amtool config routes show` | 显示路由配置 | `amtool config routes show` |
| `amtool check-config` | 检查配置文件 | `amtool check-config alertmanager.yml` |
| `curl -X POST <webhook>` | 手动触发告警 | `curl -XPOST -d@alert.json http://localhost:9093/api/v1/alerts` |

## 配置指南

### 基本配置文件结构

Alertmanager 使用 YAML 格式的配置文件。以下是一个基本配置示例：

```yaml
global:
  resolve_timeout: 5m
  smtp_smarthost: 'smtp.example.org:587'
  smtp_from: 'alertmanager@example.org'
  smtp_auth_username: 'alertmanager'
  smtp_auth_password: 'password'

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 3h
  receiver: 'team-emails'
  routes:
  - match:
      severity: critical
    receiver: 'team-pager'

receivers:
- name: 'team-emails'
  email_configs:
  - to: 'team@example.org'
- name: 'team-pager'
  email_configs:
  - to: 'oncall@example.org'
  webhook_configs:
  - url: 'http://pagerduty.example.org'
```

### 配置节点说明

#### 全局配置（global）

全局配置定义了适用于所有其他配置部分的设置：

```yaml
global:
  # 解析超时：当告警从活动状态变为已解决状态时等待的时间
  resolve_timeout: 5m
  
  # SMTP 设置（用于邮件通知）
  smtp_smarthost: 'smtp.example.org:587'
  smtp_from: 'alertmanager@example.org'
  smtp_auth_username: 'alertmanager'
  smtp_auth_password: 'password'
  
  # HTTP 客户端设置（用于 webhooks 等）
  http_config:
    proxy_url: 'http://proxy.example.org'
    basic_auth:
      username: 'admin'
      password: 'password'
```

#### 路由配置（route）

路由树定义了告警如何被分组和发送到接收器：

```yaml
route:
  # 按照哪些标签对告警进行分组
  group_by: ['alertname', 'cluster', 'service']
  
  # 初始等待时间，收集更多可能同时触发的告警
  group_wait: 30s
  
  # 同一组的后续告警发送通知的间隔时间
  group_interval: 5m
  
  # 如果告警持续存在，重复发送通知的间隔时间
  repeat_interval: 3h
  
  # 默认接收器
  receiver: 'team-emails'
  
  # 子路由
  routes:
  - match:
      severity: critical
    receiver: 'team-pager'
  
  - match_re:
      service: ^(frontend|backend)$
    receiver: 'service-team'
    
  # 特殊路由：继续评估其他兄弟节点的告警
  - match:
      network: 'production'
    continue: true
    receiver: 'network-team'
```

#### 接收器配置（receivers）

接收器定义了告警通知的发送目标：

```yaml
receivers:
- name: 'team-emails'
  email_configs:
  - to: 'team@example.org'
    send_resolved: true
    
- name: 'team-pager'
  email_configs:
  - to: 'oncall@example.org'
  webhook_configs:
  - url: 'http://pagerduty.example.org'
  
- name: 'slack-notifications'
  slack_configs:
  - api_url: 'https://hooks.slack.com/services/...'
    channel: '#alerts'
    title: '{{ template "slack.default.title" . }}'
    text: '{{ template "slack.default.text" . }}'
```

#### 抑制规则（inhibit_rules）

抑制规则用于在某些告警活动时阻止其他告警的通知：

```yaml
inhibit_rules:
  # 当标签 'alertname' 为 'NodeDown' 的告警触发时
  # 抑制来自同一 'cluster' 的所有严重性为 'warning' 的告警
- source_match:
    alertname: 'NodeDown'
  target_match:
    severity: 'warning'
  # 确保这些源告警和目标告警属于同一集群
  equal: ['cluster']
```

### 模板配置

Alertmanager 支持自定义通知模板：

```yaml
templates:
  - 'templates/email.tmpl'
  - 'templates/slack.tmpl'
```

模板文件示例（email.tmpl）：

```
{{ define "email.subject" }}
[{{ .Status | toUpper }}] {{ .GroupLabels.SortedPairs.Values | join " " }} - {{ .CommonAnnotations.summary }}
{{ end }}

{{ define "email.text" }}
告警状态: {{ .Status | toUpper }}

告警详情:
{{ range .Alerts }}
  描述: {{ .Annotations.description }}
  开始时间: {{ .StartsAt }}
  {{ if ne .Status "firing" }}
  结束时间: {{ .EndsAt }}
  {{ end }}
  标签:
  {{ range .Labels.SortedPairs }}
    {{ .Name }} = {{ .Value }}
  {{ end }}
{{ end }}
{{ end }}
```

## 静默和告警管理

### 创建静默

通过 Web UI 或 API 创建静默规则：

1. 访问 Alertmanager Web UI（默认 `http://<alertmanager-host>:9093`）
2. 点击 "Silences" 标签页
3. 点击 "New Silence"
4. 填写静默规则的匹配条件、注释和持续时间
5. 点击 "Create" 按钮

使用 amtool 创建静默：

```bash
# 创建一个持续1小时的针对 HighCpuLoad 告警的静默
amtool silence add alertname=HighCpuLoad --comment="Maintenance window" --duration=1h

# 使用多个匹配条件
amtool silence add alertname=HighCpuLoad environment=production --duration=2h
```

### 管理静默

```bash
# 查看所有当前的静默
amtool silence query

# 查看特定匹配条件的静默
amtool silence query alertname=HighCpuLoad

# 删除/使静默过期
amtool silence expire <silence-id>
```

### 手动触发告警测试

您可以通过 API 手动发送告警到 Alertmanager 进行测试：

```bash
cat <<EOF > alert.json
[
  {
    "labels": {
      "alertname": "TestAlert",
      "service": "test-service",
      "severity": "warning",
      "instance": "test-instance"
    },
    "annotations": {
      "summary": "测试告警通知",
      "description": "这是一个测试告警，请忽略"
    }
  }
]
EOF

curl -H "Content-Type: application/json" -X POST --data @alert.json http://localhost:9093/api/v1/alerts
```

## 高可用部署

### 集群配置

Alertmanager 支持高可用集群部署，确保在单个实例失败时不会丢失告警：

```yaml
# alertmanager1.yml
global:
  # ...

cluster:
  listen-address: "0.0.0.0:9094"
  peers:
    - "alertmanager1:9094"
    - "alertmanager2:9094"
    - "alertmanager3:9094"

# ...
```

### 启动集群节点

```bash
# 节点 1
alertmanager --config.file=alertmanager1.yml --cluster.peer=alertmanager2:9094 --cluster.peer=alertmanager3:9094 --web.listen-address=:9093 --cluster.listen-address=:9094 --storage.path=/data1

# 节点 2
alertmanager --config.file=alertmanager2.yml --cluster.peer=alertmanager1:9094 --cluster.peer=alertmanager3:9094 --web.listen-address=:9093 --cluster.listen-address=:9094 --storage.path=/data2

# 节点 3
alertmanager --config.file=alertmanager3.yml --cluster.peer=alertmanager1:9094 --cluster.peer=alertmanager2:9094 --web.listen-address=:9093 --cluster.listen-address=:9094 --storage.path=/data3
```

### 配置 Prometheus 发送到多个 Alertmanager

```yaml
# prometheus.yml
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - 'alertmanager1:9093'
          - 'alertmanager2:9093'
          - 'alertmanager3:9093'
```

## 常用通知集成配置

### 邮件通知

```yaml
receivers:
- name: 'email-notifications'
  email_configs:
  - to: 'team@example.org'
    from: 'alertmanager@example.org'
    smarthost: 'smtp.example.org:587'
    auth_username: 'alertmanager'
    auth_password: 'password'
    auth_identity: 'alertmanager@example.org'
    auth_secret: 'password'
    send_resolved: true
    html: '{{ template "email.html" . }}'
    headers:
      Subject: '{{ template "email.subject" . }}'
```

### Slack 通知

```yaml
receivers:
- name: 'slack-notifications'
  slack_configs:
  - api_url: 'https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXX'
    channel: '#alerts'
    username: 'Alertmanager'
    icon_emoji: ':warning:'
    title: '{{ template "slack.title" . }}'
    text: '{{ template "slack.text" . }}'
    actions:
    - type: button
      text: 'View in Grafana'
      url: 'https://grafana.example.org/d/abc123/dashboard?orgId=1'
```

### 钉钉通知

```yaml
receivers:
- name: 'dingtalk-notifications'
  webhook_configs:
  - url: 'https://oapi.dingtalk.com/robot/send?access_token=xxxxxxxxxxxx'
    send_resolved: true
    http_config:
      bearer_token: 'xxxxxx'
    max_alerts: 5
```

### 微信通知

```yaml
receivers:
- name: 'wechat-notifications'
  wechat_configs:
  - corp_id: 'ww93920e1cxxxx'
    api_url: 'https://qyapi.weixin.qq.com/cgi-bin/'
    to_party: '1'
    agent_id: '1000002'
    api_secret: 'Frhk************'
    send_resolved: true
```

### 自定义 Webhook

```yaml
receivers:
- name: 'custom-webhook'
  webhook_configs:
  - url: 'http://custom-webhook.example.org/alert'
    send_resolved: true
    http_config:
      basic_auth:
        username: 'user'
        password: 'password'
    max_alerts: 100
```

## 常见问题排查

### 告警没有收到通知

可能的原因与解决方法：

1. **路由配置问题**
   - 检查告警标签是否与路由匹配条件一致
   - 使用 `amtool config routes show` 查看路由树

2. **接收器配置错误**
   - 检查接收器配置（邮箱地址、webhook URL 等）
   - 检查网络连通性（如 SMTP 服务器可达性）

3. **静默规则生效**
   - 检查是否有静默规则匹配了该告警
   - 使用 `amtool silence query` 查看所有静默规则

4. **抑制规则生效**
   - 检查是否有抑制规则匹配了该告警
   - 检查抑制规则的源告警是否已触发

### 查看告警处理状态

```bash
# 查看当前活动告警
curl -s http://localhost:9093/api/v1/alerts | jq .

# 查看静默规则
curl -s http://localhost:9093/api/v1/silences | jq .

# 查看接收器状态
curl -s http://localhost:9093/api/v1/status | jq .receivers
```

### 日志分析

检查 Alertmanager 日志以获取更多信息：

```bash
# 查看 Alertmanager 日志
tail -f /var/log/alertmanager/alertmanager.log

# 使用更高的日志级别启动 Alertmanager
alertmanager --log.level=debug
```

### 配置验证

验证配置文件语法：

```bash
# 验证配置文件
amtool check-config /etc/alertmanager/alertmanager.yml

# 或使用 Alertmanager 二进制文件验证
alertmanager --config.file=/etc/alertmanager/alertmanager.yml --check-config
```

## 最佳实践

### 路由和分组策略

- **使用有意义的分组**：根据服务、集群和告警类型进行分组，减少通知风暴
- **合理设置时间间隔**：根据紧急程度设置 `group_wait`、`group_interval` 和 `repeat_interval`
- **构建层次化路由树**：从具体到一般，使用 `continue: true` 实现多接收器通知

示例：

```yaml
route:
  receiver: 'default-receiver'
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  routes:
  # 关键服务路由
  - match_re:
      service: ^(database|authentication)$
    receiver: 'critical-services'
    group_wait: 10s
    repeat_interval: 1h
    
  # 特定环境路由
  - match:
      environment: production
    routes:
    - match:
        severity: critical
      receiver: 'production-critical'
      group_wait: 0s  # 立即通知
      repeat_interval: 30m
    - match:
        severity: warning
      receiver: 'production-warnings'
      group_wait: 1m
      repeat_interval: 2h
```

### 通知模板优化

- **使用清晰的标题**：包含告警名称、状态和关键标签
- **提供详细的告警信息**：描述、影响、可能原因和建议操作
- **添加关联链接**：包含 Grafana 仪表板、文档或 runbook 链接
- **为不同通知渠道定制模板**：邮件可以详细，Slack 或短信简洁

示例模板（templates/custom.tmpl）：

```
{{ define "custom.title" }}
[{{ .Status | toUpper }}] {{ .GroupLabels.SortedPairs.Values | join " " }}
{{ end }}

{{ define "custom.text" }}
{{ if eq .Status "firing" }}🔥 告警触发{{ else }}✅ 告警已解决{{ end }}

{{ if gt (len .Alerts.Firing) 0 }}
**触发告警:**
{{ range .Alerts.Firing }}
- {{ .Labels.alertname }}: {{ .Annotations.summary }}
  - 描述: {{ .Annotations.description }}
  - 开始时间: {{ .StartsAt | since }}
  - 标签: {{ range .Labels.SortedPairs }}{{ .Name }}={{ .Value }} {{ end }}
  {{ if .Annotations.runbook }}
  - Runbook: {{ .Annotations.runbook }}
  {{ end }}
{{ end }}
{{ end }}

{{ if gt (len .Alerts.Resolved) 0 }}
**已解决告警:**
{{ range .Alerts.Resolved }}
- {{ .Labels.alertname }}: {{ .Annotations.summary }}
{{ end }}
{{ end }}

详情链接: {{ template "custom.dashboard" . }}
{{ end }}

{{ define "custom.dashboard" }}
https://grafana.example.org/d/alerting/alerts-overview
{{ end }}
```

### 维护窗口管理

在计划维护期间，使用静默规则避免无用告警：

```bash
# 创建维护窗口静默
amtool silence add \
  environment=production \
  service=database \
  --comment="Database maintenance window" \
  --author="ops-team" \
  --start="2023-07-10T22:00:00Z" \
  --end="2023-07-11T02:00:00Z"
```

自动化维护窗口脚本：

```bash
#!/bin/bash
# 维护窗口管理脚本

ALERTMANAGER_URL="http://localhost:9093"
LABELS="environment=production,service=frontend"
DURATION="4h"
COMMENT="Scheduled maintenance $(date +%Y-%m-%d)"
AUTHOR="automation"

# 创建静默
SILENCE_ID=$(amtool --alertmanager.url=$ALERTMANAGER_URL silence add $LABELS --duration=$DURATION --comment="$COMMENT" --author="$AUTHOR")

echo "创建静默: $SILENCE_ID"
echo "将在维护窗口结束后自动过期"

# 可选: 在脚本末尾添加维护任务
# ...

# 可选:
# read -p "维护是否完成? (y/n): " DONE
# if [[ $DONE == "y" ]]; then
#   amtool --alertmanager.url=$ALERTMANAGER_URL silence expire $SILENCE_ID
#   echo "静默已手动终止"
# fi
```

### 告警质量改进

- **减少噪音**：调整阈值减少误报，使用更精确的规则
- **富有上下文的告警**：在告警规则中添加有用的注释，如问题描述、影响和可能的解决方案
- **分级告警**：使用标签（如 `severity: critical/warning/info`）区分不同紧急程度
- **关联告警**：通过抑制规则关联相关告警，避免过多通知

## 与其他系统集成

### 与 Prometheus 集成

```yaml
# prometheus.yml
alerting:
  alertmanagers:
  - static_configs:
    - targets:
      - 'alertmanager:9093'

rule_files:
  - 'alert_rules.yml'

# alert_rules.yml
groups:
- name: example
  rules:
  - alert: HighCpuLoad
    expr: cpu_load_avg{interval="5m"} > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High CPU load on {{ $labels.instance }}"
      description: "CPU load is above 80% for 5 minutes (current value: {{ $value }})"
      runbook: "https://wiki.example.org/runbooks/high-cpu-load"
```

### 与 Grafana 集成

配置 Grafana 使用 Alertmanager 作为告警通知渠道：

1. 登录 Grafana 管理界面
2. 导航到 "Alerting" > "Notification channels"
3. 点击 "New channel"
4. 选择类型为 "Prometheus Alertmanager"
5. 配置 Alertmanager URL
6. 保存配置

### 与监控系统集成

使用 webhook 接收器将告警转发到其他监控系统：

```yaml
receivers:
- name: 'monitoring-system'
  webhook_configs:
  - url: 'http://monitoring-system.example.org/api/alert'
    send_resolved: true
    http_config:
      basic_auth:
        username: 'alertmanager'
        password: 'secret'
    max_alerts: 100
```

## 扩展与高级配置

### 自定义通知渠道

通过自定义 HTTP 模板创建新的通知渠道：

1. 创建自定义模板文件 `custom_webhook.tmpl`：

```
{{ define "custom_webhook.message" }}
{
  "alert": "{{ .GroupLabels.alertname }}",
  "status": "{{ .Status }}",
  "environment": "{{ .GroupLabels.environment }}",
  "service": "{{ .CommonLabels.service }}",
  "severity": "{{ .CommonLabels.severity }}",
  "summary": "{{ .CommonAnnotations.summary }}",
  "description": "{{ .CommonAnnotations.description }}",
  "started": "{{ .CommonLabels.started }}",
  "alerts": [
    {{ range $i, $alert := .Alerts -}}
      {{- if $i }}, {{ end -}}
      {
        "status": "{{ $alert.Status }}",
        "labels": {{ $alert.Labels | toJSON }},
        "annotations": {{ $alert.Annotations | toJSON }},
        "startsAt": "{{ $alert.StartsAt }}",
        "endsAt": "{{ $alert.EndsAt }}",
        "fingerprint": "{{ $alert.Fingerprint }}"
      }
    {{- end }}
  ]
}
{{ end }}
```

2. 配置接收器使用该模板：

```yaml
templates:
  - 'custom_webhook.tmpl'

receivers:
- name: 'custom-system'
  webhook_configs:
  - url: 'http://custom-system.example.org/api/alert'
    send_resolved: true
    http_config:
      tls_config:
        ca_file: '/etc/alertmanager/ca.crt'
        cert_file: '/etc/alertmanager/client.crt'
        key_file: '/etc/alertmanager/client.key'
    max_alerts: 100
    template: 'custom_webhook.message'
```

### 动态配置重载

Alertmanager 支持动态重载配置，无需重启服务：

```bash
# 发送 SIGHUP 信号触发配置重载
kill -HUP $(pidof alertmanager)

# 或使用 HTTP API
curl -X POST http://localhost:9093/-/reload
```

### 使用 amtool 进行远程管理

配置 amtool 连接到远程 Alertmanager：

```bash
# ~/.config/amtool/config.yml
alertmanager.url: "https://alertmanager.example.org"
output: extended
tls.cert_file: "/path/to/client.crt"
tls.key_file: "/path/to/client.key"
```

示例用法：

```bash
# 远程查询告警
amtool alert query

# 在远程服务器添加静默
amtool silence add severity=warning --duration=2h --comment="Investigating issue"
```

## 附录

### 配置文件参考

完整的 Alertmanager 配置示例：

```yaml
global:
  # 全局 SMTP 设置
  smtp_smarthost: 'smtp.example.org:587'
  smtp_from: 'alertmanager@example.org'
  smtp_auth_username: 'alertmanager'
  smtp_auth_password: 'password'
  
  # 全局 HTTP 设置
  http_config:
    tls_config:
      cert_file: '/etc/alertmanager/certificate.pem'
      key_file: '/etc/alertmanager/key.pem'
  
  # 全局通知选项
  resolve_timeout: 5m

# 模板文件列表
templates:
  - '/etc/alertmanager/templates/*.tmpl'

# 路由配置
route:
  receiver: 'default-receiver'
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  
  routes:
  - match:
      severity: critical
    receiver: 'critical-receiver'
    group_wait: 0s
    repeat_interval: 1h
  
  - match_re:
      service: ^(database|api|auth)$
    receiver: 'core-services'
    routes:
    - match:
        severity: critical
      receiver: 'core-services-critical'
  
  - match:
      team: network
    receiver: 'network-team'
    continue: true

# 抑制规则
inhibit_rules:
- source_match:
    severity: 'critical'
  target_match:
    severity: 'warning'
  equal: ['alertname', 'cluster', 'service']

# 接收器配置
receivers:
- name: 'default-receiver'
  email_configs:
  - to: 'team@example.org'
  
- name: 'critical-receiver'
  email_configs:
  - to: 'team-oncall@example.org'
  pagerduty_configs:
  - service_key: '0dbaf1a24f29c902ea07751d48063039'
  
- name: 'core-services'
  email_configs:
  - to: 'core-services@example.org'
  
- name: 'core-services-critical'
  email_configs:
  - to: 'core-services-oncall@example.org'
  slack_configs:
  - api_url: 'https://hooks.slack.com/services/XXXX/YYYY/ZZZZ'
    channel: '#core-alerts'
  
- name: 'network-team'
  email_configs:
  - to: 'network-team@example.org'
```

### API 参考

Alertmanager 提供 REST API 进行交互：

| 端点 | 描述 | 示例 |
|------|------|------|
| `/api/v1/alerts` | 查询和创建告警 | `GET /api/v1/alerts` |
| `/api/v1/silences` | 管理静默规则 | `POST /api/v1/silences` |
| `/api/v1/silence/{silenceID}` | 获取/删除特定静默 | `DELETE /api/v1/silence/{silenceID}` |
| `/api/v1/status` | 获取 Alertmanager 状态 | `GET /api/v1/status` |
| `/-/reload` | 重新加载配置 | `POST /-/reload` |
| `/-/healthy` | 健康检查 | `GET /-/healthy` |
| `/-/ready` | 就绪检查 | `GET /-/ready` |

### 官方文档链接

- [Prometheus Alertmanager 官方文档](https://prometheus.io/docs/alerting/latest/alertmanager/)
- [Alertmanager 配置文档](https://prometheus.io/docs/alerting/latest/configuration/)
- [Alertmanager API 文档](https://prometheus.io/docs/alerting/latest/management_api/)
- [Alerting Rules 文档](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/) 