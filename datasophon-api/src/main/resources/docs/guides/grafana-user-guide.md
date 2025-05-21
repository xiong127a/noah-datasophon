# Grafana 用户指南

本指南将引导您完成 Grafana 的安装、基本配置、仪表盘创建、数据源集成、模板变量使用、告警设置以及用户管理等核心功能。

## 1. 安装和部署 Grafana

### 系统要求
-   支持的操作系统: Linux, Windows, macOS, Docker。
-   硬件: 至少 1 CPU, 256MB RAM (推荐 1GB+ RAM 以获得更好性能)，足够的磁盘空间用于 Grafana 二进制文件和日志。
-   数据库 (可选，用于存储 Grafana 配置): SQLite (默认), MySQL, PostgreSQL。

### 不同安装方式

#### Docker (推荐)
```bash
docker run -d -p 3000:3000 --name=grafana grafana/grafana
```
数据会存储在 Docker 卷中。如需持久化数据，请挂载本地目录：
```bash
docker run -d -p 3000:3000 --name=grafana -v /path/to/your/grafana-data:/var/lib/grafana grafana/grafana
```

#### 二进制包 (Linux)
1.  访问 [Grafana 官网下载页面](https://grafana.com/grafana/download) 下载对应系统的二进制包 (通常是 .tar.gz)。
2.  解压文件：
    ```bash
    tar -zxvf grafana-<version>.linux-amd64.tar.gz
    sudo mv grafana-<version> /usr/share/grafana
    ```
3.  (可选) 创建符号链接：
    ```bash
    sudo ln -s /usr/share/grafana/bin/grafana-server /usr/sbin/grafana-server
    sudo ln -s /usr/share/grafana/bin/grafana-cli /usr/sbin/grafana-cli
    ```

#### 包管理器 (Debian/Ubuntu)
```bash
sudo apt-get install -y apt-transport-https software-properties-common wget
wget -q -O - https://packages.grafana.com/gpg.key | sudo apt-key add -
sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
sudo apt-get update
sudo apt-get install grafana
```

#### 包管理器 (RHEL/CentOS/Fedora)
创建一个 `.repo` 文件，例如 `/etc/yum.repos.d/grafana.repo`：
```ini
[grafana]
name=grafana
baseurl=https://packages.grafana.com/oss/rpm
repo_gpgcheck=1
gpgcheck=1
gpgkey=https://packages.grafana.com/gpg.key
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
```
然后安装：
```bash
sudo yum install grafana
# 或者对于较新的 Fedora 版本
sudo dnf install grafana
```

### 初始配置 (`grafana.ini`)
Grafana 的主配置文件通常位于：
-   Linux (二进制包): `/usr/share/grafana/conf/defaults.ini` (默认值)，自定义配置应放在 `/usr/share/grafana/conf/custom.ini` 或 `/etc/grafana/grafana.ini`。
-   Linux (包管理器): `/etc/grafana/grafana.ini`。
-   Docker: 可以通过环境变量覆盖配置，或挂载自定义的 `grafana.ini`。

常用配置项：
-   `http_port`: Grafana Web 服务器监听的端口 (默认: `3000`)。
-   `[database]`: 配置外部数据库 (MySQL, PostgreSQL) 代替默认的 SQLite。
    -   `type = mysql` / `postgres`
    -   `host = 127.0.0.1:3306`
    -   `name = grafana`
    -   `user = grafana_user`
    -   `password = your_password`
-   `[auth.anonymous]`: 配置匿名访问。
    -   `enabled = false` (默认禁用匿名访问)
    -   `org_name = Main Org.` (匿名用户所属组织)
    -   `org_role = Viewer` (匿名用户角色)
-   `[log]`: 配置日志级别和输出模式 (默认: `console` 和 `file`)。
    -   `mode = console file`
    -   `level = info`

### 启动和访问 Grafana

#### 使用 systemd (包管理器安装)
```bash
sudo systemctl daemon-reload
sudo systemctl enable grafana-server.service
sudo systemctl start grafana-server.service
sudo systemctl status grafana-server.service
```

#### 直接运行 (二进制包)
```bash
cd /usr/share/grafana
./bin/grafana-server web
```

启动后，通过浏览器访问 `http://<your-grafana-host>:3000`。
默认管理员用户名和密码是 `admin` / `admin`。首次登录后会提示修改密码。

## 2. Grafana 基础入门

### 用户界面概览
-   **侧边栏导航 (Sidebar)**: 左侧的主要导航菜单，包含仪表盘 (Dashboards)、探索 (Explore)、告警 (Alerting)、配置 (Configuration)、服务器管理 (Server Admin) 等。
-   **仪表盘选择器**: 顶部用于搜索和选择仪表盘。
-   **用户设置**: 右上角用户头像处，包含个人偏好设置、切换组织、登出等。

### 创建第一个数据源
1.  点击侧边栏的 "Configuration" (齿轮图标) -> "Data Sources"。
2.  点击 "Add data source"。
3.  从列表中选择一个数据源类型，例如 "Prometheus"。
4.  配置数据源设置：
    -   **Name**: 数据源的名称 (例如, `My Prometheus`)。
    -   **URL**: Prometheus 服务器的地址 (例如, `http://localhost:9090`)。
    -   **Access**: `Server` (由 Grafana 后端代理访问) 或 `Browser` (由用户的浏览器直接访问，可能涉及 CORS)。通常选择 `Server`。
    -   其他特定于数据源的设置 (如认证、HTTP 方法等)。
5.  点击 "Save & Test"。如果配置正确，会显示 "Data source is working" 的提示。

**以 InfluxDB 为例:**
-   URL: InfluxDB 服务器地址 (例如, `http://localhost:8086`)。
-   Database: 要查询的 InfluxDB 数据库名称。
-   User/Password: InfluxDB 的认证凭据 (如果启用)。
-   HTTP Method: `GET` 或 `POST`。

### 创建第一个仪表盘和面板
1.  点击侧边栏的 "+" 图标 -> "Dashboard"。
2.  点击 "Add new panel" (或者仪表盘中间的 "Add Panel" 按钮)。
3.  **面板编辑器视图**: 
    -   **右上角**: 选择面板的可视化类型 (例如, `Graph`, `Stat`, `Table`)。
    -   **底部**: 查询编辑器区域。
        -   **Data source**: 选择之前配置好的数据源 (例如, `My Prometheus`)。
        -   **Query**: 输入查询语句。对于 Prometheus，输入 PromQL 表达式，例如 `node_load1`。
        -   可以添加多个查询 (Query A, Query B ...)。
    -   **右侧边栏**: 面板配置选项。
        -   **Panel**: 标题 (Title), 描述 (Description)。
        -   **Axes (坐标轴)**: (对于图形面板) 配置 Y 轴单位 (Unit), 标签 (Label), 最小值/最大值, 小数位数 (Decimals) 等。
        -   **Legend (图例)**: 是否显示图例，图例格式，显示统计值 (Min, Max, Avg, Current) 等。
        -   **Display / Visualization options**: 特定于可视化类型的选项 (例如, Graph 的线条样式、点、填充等)。
        -   **Thresholds (阈值)**: (对于 Stat, Gauge, Table 等) 设置阈值，当数值超过阈值时改变颜色。
4.  配置完成后，点击右上角的 "Apply" 返回仪表盘视图。
5.  在仪表盘视图中，可以拖动和调整面板的大小和位置。
6.  点击仪表盘顶部的保存图标 (软盘形状)，为仪表盘命名并保存。

## 3. 深入理解仪表盘和面板

### 常用面板类型详解
-   **Graph (时间序列图)**: 最常用的，用于绘制时间序列数据。支持多条线、堆叠、百分比模式等。
-   **Stat (单一统计值)**: 以醒目方式显示单个数值，如最新值、平均值、总和。支持背景颜色根据阈值变化。
-   **Gauge (仪表盘图)**: 类似汽车仪表盘，显示一个值在最小值和最大值之间的位置，并可通过颜色表示状态。
-   **Bar gauge (条形仪表盘图)**: 一组水平或垂直的条，每个条代表一个序列的当前值。
-   **Table (表格)**: 以表格形式展示数据。支持排序、列格式化、聚合等。
-   **Text (文本)**: 显示静态文本、Markdown 或 HTML。可以用于添加说明、标题或嵌入其他内容。
-   **Heatmap (热力图)**: 用颜色强度表示数据在 Y 轴分桶和 X 轴时间上的分布，适合观察模式和趋势。
-   **Logs (日志面板)**: 需要 Loki, Elasticsearch 或 InfluxDB (with Flux log support) 等日志数据源。显示日志行，支持搜索和过滤。

### 面板编辑器高级功能
-   **Overrides (覆盖)**: 针对特定序列或匹配特定名称的序列，覆盖其默认的显示设置 (如颜色、Y 轴、线条样式等)。
-   **Transformations (数据转换)**: 在数据可视化之前对查询结果进行转换。常用的转换有：
    -   `Filter data by values`: 根据字段值过滤数据。
    -   `Reduce`: 计算每个序列的单个值 (如总和、平均值)。
    -   `Organize fields`: 重命名、隐藏或重新排序字段。
    -   `Merge`: 合并多个查询结果。
    -   `Sort by`: 按字段值排序。
-   **Data links (数据链接)**: 在可视化中创建链接，允许用户点击图表上的点或表格中的值跳转到其他仪表盘、URL，并传递上下文信息 (如时间范围、序列名称)。

### 仪表盘设置
点击仪表盘顶部的齿轮图标进入仪表盘设置：
-   **General**: 名称, 描述, 标签 (Tags - 用于组织和搜索), 文件夹 (Folder)。
-   **Time options**: 设置仪表盘的默认时区 (Local browser time, UTC, Default)。
-   **Auto refresh**: 配置仪表盘自动刷新的频率。
-   **JSON Model**: 查看和编辑仪表盘的底层 JSON 定义 (高级用户)。
-   **Versions**: 查看和回滚到仪表盘的历史版本。
-   **Permissions**: (如果启用了文件夹/仪表盘权限) 设置哪些用户或团队可以查看或编辑此仪表盘。
-   **Links**: 添加自定义链接到仪表盘顶部 (例如, 链接到相关文档或系统)。
-   **Annotations (注释)**: 在图表上标记特定时间点或时间范围的事件。可以手动添加，也可以从数据源 (如 Elasticsearch, InfluxDB) 查询。

**共享仪表盘**:
-   点击仪表盘顶部的共享图标。
-   **Link**: 生成一个直接链接到当前仪表盘和时间范围。
-   **Snapshot**: 创建一个交互式的、公开可访问的仪表盘快照 (存储在 raintank.io 或本地)。数据是嵌入的，不会实时更新。
-   **Export**: 导出仪表盘的 JSON 定义，方便在其他 Grafana 实例中导入。
-   **Embed**: 生成 iframe 代码将面板嵌入到其他网页 (需配置 `allow_embedding = true`)。

## 4. 使用模板变量 (Templating)

模板变量使得仪表盘更具交互性和复用性。
1.  进入仪表盘设置 (顶部齿轮图标) -> "Variables"。
2.  点击 "Add variable"。
3.  配置变量：
 