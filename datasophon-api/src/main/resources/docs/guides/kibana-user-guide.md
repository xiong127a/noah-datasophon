# Kibana 用户指南

## 开始使用

本指南将帮助您快速入门 Kibana，包括基本配置、界面导航和核心功能的使用方法。

### 安装配置

Kibana 需要与 Elasticsearch 配合使用，确保您已经有一个正在运行的 Elasticsearch 实例。

**基本配置步骤：**

1. **配置 Elasticsearch 连接**
   编辑 `config/kibana.yml`：
   ```yaml
   server.port: 5601
   server.host: "0.0.0.0"
   elasticsearch.hosts: ["http://localhost:9200"]
   ```

2. **启动 Kibana**
   ```bash
   ./bin/kibana
   ```
   
3. **访问 Kibana**
   打开浏览器访问：`http://localhost:5601`

### 界面导航

Kibana 界面主要包含以下核心部分：

- **导航菜单**：位于左侧，包含所有主要功能入口
- **工作区**：中央主要内容区域
- **工具栏**：顶部，包含全局设置和用户选项

## 数据探索

### 创建索引模式

1. **进入管理页面**
   - 点击左侧菜单的 "Stack Management"
   - 选择 "Index Patterns"

2. **创建新的索引模式**
   - 点击 "Create index pattern"
   - 输入索引模式（如 "logstash-*"）
   - 选择时间字段（如 "@timestamp"）
   - 点击 "Create index pattern"

### 使用 Discover

Discover 页面用于搜索和浏览数据：

1. **基本搜索**
   - 使用 KQL（Kibana Query Language）或 Lucene 语法
   - 示例：`status:error AND service:api`

2. **时间范围选择**
   - 使用顶部时间选择器
   - 支持相对时间和绝对时间范围

3. **字段选择**
   - 在左侧字段列表中选择要显示的字段
   - 可以添加或删除列

## 可视化创建

### 创建可视化

1. **选择可视化类型**
   - 进入 Visualize
   - 点击 "Create visualization"
   - 选择合适的图表类型

2. **配置数据源**
   - 选择索引模式
   - 配置指标（Metrics）
   - 设置分组（Buckets）

### 常用图表类型

1. **折线图配置**
   ```json
   {
     "aggs": {
       "time_buckets": {
         "date_histogram": {
           "field": "@timestamp",
           "interval": "1h"
         }
       },
       "total_requests": {
         "sum": {
           "field": "bytes"
         }
       }
     }
   }
   ```

2. **饼图配置**
   ```json
   {
     "aggs": {
       "status_codes": {
         "terms": {
           "field": "response.status_code",
           "size": 10
         }
       }
     }
   }
   ```

## 仪表板创建

### 创建新仪表板

1. **基本步骤**
   - 进入 Dashboard
   - 点击 "Create dashboard"
   - 添加已有可视化或创建新的可视化

2. **布局调整**
   - 拖拽调整面板大小
   - 重新排列面板位置
   - 设置刷新间隔

### 仪表板优化

1. **性能优化**
   - 限制时间范围
   - 减少面板数量
   - 优化查询语句

2. **外观定制**
   - 设置主题
   - 调整颜色方案
   - 配置字体大小

## 开发工具使用

### Console 工具

1. **基本用法**
   ```json
   GET _search
   {
     "query": {
       "match_all": {}
     }
   }
   ```

2. **自动补全**
   - 支持 API 补全
   - 支持字段名补全
   - 支持查询语法补全

### 其他开发工具

1. **Grok Debugger**
   - 用于测试和调试 Logstash 的 Grok 模式
   - 实时验证解析结果

2. **Search Profiler**
   - 分析搜索性能
   - 识别查询瓶颈

## 高级功能

### Canvas

1. **创建演示文稿**
   - 选择模板
   - 添加数据源
   - 配置元素样式

2. **数据源配置**
   ```json
   {
     "type": "elasticsearch",
     "index": "my-index",
     "query": {
       "bool": {
         "must": [
           {"match": {"field": "value"}}
         ]
       }
     }
   }
   ```

### Maps

1. **地理可视化**
   - 添加图层
   - 配置数据源
   - 设置样式

2. **图层类型**
   - 文档图层
   - 热力图层
   - 边界图层

## 安全配置

### 认证设置

1. **配置基本认证**
   ```yaml
   xpack.security.enabled: true
   elasticsearch.username: "kibana_system"
   elasticsearch.password: "password"
   ```

2. **配置 SSL/TLS**
   ```yaml
   server.ssl.enabled: true
   server.ssl.certificate: "/path/to/cert.pem"
   server.ssl.key: "/path/to/key.pem"
   ```

### 角色管理

1. **创建角色**
   - 定义索引权限
   - 设置功能权限
   - 分配空间权限

2. **用户管理**
   - 创建用户
   - 分配角色
   - 设置密码策略

## 监控与维护

### 性能监控

1. **监控指标**
   - 响应时间
   - 请求率
   - 错误率

2. **日志分析**
   - 查看 Kibana 日志
   - 排查常见问题

### 备份恢复

1. **保存对象**
   - 导出仪表板
   - 导出可视化
   - 导出索引模式

2. **恢复配置**
   - 导入已保存对象
   - 验证恢复结果

## 最佳实践

### 查询优化

1. **使用高效的查询**
   ```json
   {
     "query": {
       "bool": {
         "filter": [
           {"term": {"status": "error"}},
           {"range": {"@timestamp": {"gte": "now-1h"}}}
         ]
       }
     }
   }
   ```

2. **避免常见陷阱**
   - 限制返回字段
   - 使用合适的时间范围
   - 优化聚合查询

### 可视化建议

1. **选择合适的图表**
   - 时序数据用折线图
   - 分布数据用柱状图
   - 占比数据用饼图

2. **设计原则**
   - 保持简洁
   - 确保可读性
   - 注重用户体验

### 仪表板设计

1. **布局优化**
   - 重要信息放在顶部
   - 相关信息分组展示
   - 保持视觉平衡

2. **性能考虑**
   - 控制面板数量
   - 优化刷新间隔
   - 使用过滤器减少数据量

## 故障排除

### 常见问题

1. **连接问题**
   - 检查 Elasticsearch 状态
   - 验证配置文件
   - 检查网络连接

2. **性能问题**
   - 优化查询
   - 调整刷新间隔
   - 检查资源使用

### 诊断工具

1. **状态页面**
   - 查看系统状态
   - 监控性能指标
   - 检查插件状态

2. **日志分析**
   - 查看错误日志
   - 分析性能问题
   - 追踪请求链路

## 插件开发

### 开发环境设置

1. **准备工作**
   ```bash
   git clone https://github.com/elastic/kibana.git
   cd kibana
   yarn install
   ```

2. **创建插件**
   ```bash
   node scripts/generate_plugin.js my-plugin
   ```

### 插件结构

1. **基本文件**
   ```
   my-plugin/
   ├── kibana.json
   ├── package.json
   ├── public/
   │   ├── index.ts
   │   └── plugin.ts
   └── server/
       ├── index.ts
       └── plugin.ts
   ```

2. **配置文件**
   ```json
   {
     "id": "myPlugin",
     "version": "1.0.0",
     "kibanaVersion": "8.0.0",
     "server": true,
     "ui": true
   }
   ```

### API 使用

1. **客户端 API**
   ```typescript
   import { CoreStart } from 'kibana/public';
   
   export class MyPlugin {
     constructor(private readonly core: CoreStart) {}
     
     public async mount() {
       // 实现插件功能
     }
   }
   ```

2. **服务器 API**
   ```typescript
   import { CoreSetup } from 'kibana/server';
   
   export class MyPlugin {
     constructor(private readonly core: CoreSetup) {}
     
     public async setup() {
       // 设置路由和服务
     }
   }
   ```

本指南涵盖了 Kibana 的主要使用方法和开发指南。随着 Kibana 的不断发展，建议定期查看官方文档以获取最新信息。