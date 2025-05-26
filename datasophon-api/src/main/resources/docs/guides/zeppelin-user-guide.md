# Apache Zeppelin 用户指南

## 环境准备

在开始使用Apache Zeppelin之前，请确保您的环境满足以下基本要求：

### 硬件要求

**最低配置**：
- **处理器**：双核CPU
- **内存**：4GB RAM（建议8GB以上）
- **存储**：至少10GB可用磁盘空间
- **网络**：稳定的网络连接

**推荐配置**（对于生产环境）：
- **处理器**：8核或更多
- **内存**：16GB以上RAM
- **存储**：50GB以上SSD存储
- **网络**：高速网络连接

### 软件依赖

- **Java**：JDK 1.8或更高版本（不推荐JDK 9或更高版本）
- **Web浏览器**：最新版Chrome、Firefox或Safari
- **操作系统**：
  - Linux（推荐CentOS 7+、Ubuntu 16.04+）
  - macOS 10.12+
  - Windows 10/Server 2016+

### 可选前置条件

根据需要使用的解释器，可能还需要以下组件：

- **Spark**：Apache Spark 1.6.x, 2.x 或 3.x
- **Hadoop**：Hadoop 2.6+ 或 3.x
- **Python**：Python 2.7 或 3.6+
- **R**：R 3.1+

## 安装与配置

### 下载与安装

有多种方式可以获取和安装Apache Zeppelin：

#### 方式一：二进制包安装

1. **下载二进制发行版**

   从[Apache Zeppelin下载页面](https://zeppelin.apache.org/download.html)获取最新稳定版本：

   ```bash
   # 下载最新版本(例如0.10.1)
   wget https://dlcdn.apache.org/zeppelin/zeppelin-0.10.1/zeppelin-0.10.1-bin-all.tgz
   
   # 解压文件
   tar -xzf zeppelin-0.10.1-bin-all.tgz
   
   # 进入Zeppelin目录
   cd zeppelin-0.10.1-bin-all
   ```

2. **启动Zeppelin服务**

   ```bash
   # 启动Zeppelin
   bin/zeppelin-daemon.sh start
   
   # 查看状态
   bin/zeppelin-daemon.sh status
   
   # 停止服务
   bin/zeppelin-daemon.sh stop
   ```

#### 方式二：源码编译安装

如果您需要定制或使用最新开发版本，可以从源码编译：

1. **获取源码**

   ```bash
   # 使用Git克隆仓库
   git clone https://github.com/apache/zeppelin.git
   
   # 进入项目目录
   cd zeppelin
   ```

2. **编译源码**

   ```bash
   # 使用Maven编译
   mvn clean package -DskipTests -Pspark-3.1 -Pscala-2.12
   
   # 编译所有解释器
   mvn clean package -DskipTests -Pscala-2.12 -Pspark-3.1 -Phadoop3 -Pr -Ppyspark -Psparkr
   ```

   编译选项说明：
   - `-Pspark-3.1`: 指定Spark版本
   - `-Pscala-2.12`: 指定Scala版本
   - `-Phadoop3`: 支持Hadoop 3.x
   - `-Pr`, `-Ppyspark`, `-Psparkr`: 添加R、PySpark和SparkR支持

3. **启动编译后的Zeppelin**

   ```bash
   bin/zeppelin-daemon.sh start
   ```

#### 方式三：Docker部署

使用Docker可以快速部署Zeppelin：

```bash
# 拉取官方镜像
docker pull apache/zeppelin:0.10.1

# 运行容器
docker run -p 8080:8080 -p 8443:8443 --rm --name zeppelin apache/zeppelin:0.10.1

# 后台运行
docker run -d -p 8080:8080 -p 8443:8443 --name zeppelin apache/zeppelin:0.10.1
```

要持久化数据，可以挂载卷：

```bash
# 挂载笔记本和解释器设置
docker run -d -p 8080:8080 -p 8443:8443 \
  -v $PWD/logs:/logs \
  -v $PWD/notebook:/notebook \
  -v $PWD/conf:/conf \
  -e ZEPPELIN_LOG_DIR='/logs' \
  -e ZEPPELIN_NOTEBOOK_DIR='/notebook' \
  --name zeppelin apache/zeppelin:0.10.1
```

### 基本配置

Zeppelin的主要配置文件位于`conf/`目录中：

1. **创建配置文件**

   首次运行需要从模板创建配置文件：

   ```bash
   cp conf/zeppelin-site.xml.template conf/zeppelin-site.xml
   cp conf/zeppelin-env.sh.template conf/zeppelin-env.sh
   cp conf/shiro.ini.template conf/shiro.ini
   ```

2. **配置服务器设置**

   编辑`conf/zeppelin-site.xml`文件，设置关键参数：

   ```xml
   <!-- 设置Zeppelin服务器端口 -->
   <property>
     <name>zeppelin.server.port</name>
     <value>8080</value>
   </property>
   
   <!-- 设置Zeppelin服务器地址 -->
   <property>
     <name>zeppelin.server.addr</name>
     <value>0.0.0.0</value>
   </property>
   
   <!-- 设置笔记本保存目录 -->
   <property>
     <name>zeppelin.notebook.dir</name>
     <value>notebook</value>
   </property>
   ```

3. **环境变量配置**

   编辑`conf/zeppelin-env.sh`设置环境变量：

   ```bash
   # JVM选项
   export ZEPPELIN_JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"
   
   # 设置Spark相关选项（如果使用Spark）
   export SPARK_HOME=/path/to/spark
   export HADOOP_CONF_DIR=/path/to/hadoop/conf
   
   # 设置Python（如果使用PySpark或Python解释器）
   export PYSPARK_PYTHON=/path/to/python
   export PYSPARK_DRIVER_PYTHON=/path/to/python
   ```

4. **设置安全认证**

   如果需要开启身份验证，编辑`conf/shiro.ini`：

   ```ini
   [users]
   # 格式: 用户名 = 密码, 角色1, 角色2, ...
   admin = admin123, admin
   user1 = password1, role1
   
   [roles]
   role1 = *
   admin = *
   
   [urls]
   # 启用身份验证
   /api/version = anon
   /api/interpreter/** = authc
   /api/configurations/** = authc
   /api/credential/** = authc
   /** = authc
   ```

5. **日志配置**

   可以通过编辑`conf/log4j.properties`调整日志级别：

   ```properties
   log4j.rootLogger = INFO, dailyfile
   log4j.appender.stdout = org.apache.log4j.ConsoleAppender
   log4j.appender.stdout.layout = org.apache.log4j.PatternLayout
   log4j.appender.stdout.layout.ConversionPattern=%5p [%d] ({%t} %F[%M]:%L) - %m%n
   ```

### 解释器配置

Zeppelin支持多种解释器，需根据使用需求进行配置：

#### Spark解释器配置

1. **基本配置**

   在Web界面中，点击右上角"匿名" → "解释器" → 找到"spark"解释器，进行以下设置：

   - **SPARK_HOME**: Spark安装目录
   - **master**: Spark集群地址（例如`local[*]`、`yarn`或`spark://host:port`）
   - **spark.executor.memory**: 执行器内存（例如`1g`）
   - **spark.driver.memory**: 驱动器内存（例如`1g`）

2. **依赖管理**

   您可以添加外部依赖包：
   
   - **Dependencies**: 添加需要的JAR包路径或Maven坐标
   - 例如：`org.apache.hadoop:hadoop-aws:3.3.1`

#### JDBC解释器配置

配置JDBC解释器连接数据库：

- **default.driver**: JDBC驱动类（例如`com.mysql.jdbc.Driver`）
- **default.url**: 数据库连接URL（例如`jdbc:mysql://localhost:3306/db`）
- **default.user**: 数据库用户名
- **default.password**: 数据库密码

#### Python解释器配置

配置Python环境：

- **zeppelin.python**: Python解释器路径（例如`/usr/bin/python3`）
- **zeppelin.python.maxResult**: 最大结果行数

## 基本操作指南

### 访问Zeppelin

启动Zeppelin服务后，可以通过浏览器访问Web界面：

- 默认URL: `http://localhost:8080`

### 笔记本管理

#### 创建笔记本

1. 点击主页"Create new note"或顶部导航栏的"Notebook" → "Create new note"
2. 输入笔记本名称
3. 选择默认解释器（例如spark、md、python等）
4. 点击"Create"完成创建

#### 导入笔记本

1. 点击"Import note"
2. 选择本地笔记本文件(.json)或输入URL
3. 点击"Import Note"

#### 导出笔记本

1. 打开笔记本
2. 点击右上角的菜单图标
3. 选择"Export note"
4. 选择导出格式（.json或其他支持的格式）

#### 组织笔记本

可以通过文件夹组织笔记本：

1. 在主页，点击笔记本右侧的菜单图标
2. 选择"Move"
3. 输入或选择目标文件夹路径

### 基本编辑操作

Zeppelin笔记本由多个段落(Paragraph)组成，每个段落可以使用不同的解释器：

#### 创建段落

- 点击已有段落之间的"➕"图标
- 或使用快捷键`Shift+Enter`在当前段落之后创建新段落

#### 设置解释器

每个段落可以使用不同的解释器：

1. 在段落开头指定解释器，格式为`%解释器名`
   ```
   %spark
   println("Hello Spark")
   
   %python
   print("Hello Python")
   
   %md
   # Markdown标题
   ```

2. 支持多解释器段落，使用`%解释器名.子解释器`：
   ```
   %spark.pyspark
   print("This is PySpark")
   
   %spark.sql
   SELECT * FROM my_table
   ```

#### 运行段落

- 点击段落右上角的"▶️"图标
- 或使用快捷键`Shift+Enter`运行当前段落

#### 运行所有段落

- 点击顶部工具栏中的"运行所有段落"图标
- 或使用笔记本菜单选择"Run all paragraphs"

#### 段落快捷键

- `Ctrl+Enter`: 运行当前段落
- `Shift+Enter`: 运行当前段落并移至下一个
- `Ctrl+;`: 添加/删除注释
- `Ctrl+/`: 在代码编辑器中打开帮助

### 数据可视化

Zeppelin提供多种数据可视化选项：

#### 表格视图

SQL或DataFrame查询结果会自动以表格形式显示：

```
%spark.sql
SELECT * FROM people LIMIT 10
```

#### 切换图表类型

1. 在结果区域底部，点击图表图标
2. 选择所需图表类型（饼图、柱状图、折线图等）
3. 配置图表参数（键、值、组等）

#### 自定义图表

可以使用Zeppelin内置的可视化系统或JavaScript库创建自定义图表：

```
%spark.sql
SELECT age, count(*) as count FROM people GROUP BY age ORDER BY age
```

结果可以切换为柱状图、折线图等。

#### 多图表视图

可以在一个段落中显示多种图表视图：

1. 运行返回表格数据的段落
2. 点击"多图表视图"按钮
3. 选择不同图表类型并配置

### 动态表单

Zeppelin支持动态表单，让笔记本具有交互性：

#### 创建基本表单

在段落中使用`${变量名}`或`${变量名=默认值}`语法：

```
%spark
println("Hello ${name=world}")

%spark.sql
SELECT * FROM people WHERE age > ${min_age=18} LIMIT ${limit=10}
```

#### 选择表单

使用`${变量名=默认值,值1|值2|值3...}`创建下拉选择：

```
%spark.sql
SELECT * FROM people 
WHERE gender = "${gender=all,male|female|all}"
LIMIT 10
```

#### 表单类型

支持多种表单类型：

- **文本框**: `${var=默认值}`
- **选择框**: `${var=默认值,选项1|选项2|选项3}`
- **复选框**: `${var=true}`

## 高级功能

### 解释器绑定与隔离

#### 笔记本级解释器绑定

可以为每个笔记本绑定特定的解释器实例：

1. 打开笔记本
2. 点击右上角的齿轮图标
3. 在"解释器绑定"中选择或取消选择解释器
4. 点击"Save"保存设置

#### 动态解释器设置

可以在笔记本中动态配置解释器：

```
%spark(master=local[4],spark.executor.memory=4g)
// 此段落使用特定配置的Spark
```

### 段落依赖与执行顺序

#### 设置依赖关系

可以设置段落之间的依赖关系，确保按特定顺序执行：

1. 点击段落右上角的设置图标
2. 选择"Enable Dependency Management"
3. 选择此段落依赖的其他段落ID

#### 自动运行

配置段落自动运行：

1. 点击段落右上角的设置图标
2. 选择"Run paragraph automatically when other paragraphs changed"

### 笔记本版本控制

Zeppelin提供内置的笔记本版本管理：

#### 查看修订历史

1. 打开笔记本
2. 点击右上角菜单
3. 选择"Revision history"
4. 浏览不同版本

#### 恢复版本

1. 在修订历史页面选择要恢复的版本
2. 点击"Set Current"将所选版本设为当前版本

### 共享与协作

#### 公开分享

可以创建笔记本公开链接：

1. 点击右上角菜单
2. 选择"Share"
3. 复制生成的URL（可以设置所有人可访问）

#### 多用户协作

多用户可以同时编辑同一笔记本：

1. 确保已启用认证
2. 不同用户可以同时编辑笔记本
3. 实时查看他人的编辑

### 调度与任务

Zeppelin支持定时运行笔记本：

#### 设置任务调度

1. 打开笔记本
2. 点击右上角菜单
3. 选择"Job scheduler"
4. 设置定时表达式（Cron表达式）
5. 选择"Enable"启用调度

#### 常用Cron表达式

- 每小时: `0 0 * * * ?`
- 每天午夜: `0 0 0 * * ?`
- 每周一上午8点: `0 0 8 ? * MON`
- 每月1号: `0 0 0 1 * ?`

## 解释器使用指南

### Spark解释器

Spark是Zeppelin最常用的解释器之一：

#### Scala代码

```
%spark
val data = Seq(1, 2, 3, 4, 5)
val rdd = sc.parallelize(data)
println(s"Sum: ${rdd.sum()}")
```

#### SparkSQL

```
%spark.sql
-- 创建临时视图
CREATE OR REPLACE TEMPORARY VIEW people AS
SELECT * FROM json.`/path/to/people.json`;

-- 查询数据
SELECT age, count(*) as count FROM people GROUP BY age ORDER BY age
```

#### PySpark

```
%spark.pyspark
data = [1, 2, 3, 4, 5]
rdd = sc.parallelize(data)
print("Sum:", rdd.sum())
```

#### SparkR

```
%spark.r
data <- c(1, 2, 3, 4, 5)
rdd <- parallelize(sc, data)
print(paste("Sum:", sum(collect(rdd))))
```

### Python解释器

Zeppelin提供独立的Python解释器：

```
%python
import numpy as np
import matplotlib.pyplot as plt

x = np.linspace(0, 10, 100)
y = np.sin(x)

plt.figure(figsize=(10, 4))
plt.plot(x, y)
plt.title("Sine Wave")
plt.show()
```

### Markdown解释器

用于创建格式化文档：

```
%md
# 文档标题

## 小标题

这是一个**粗体**文本和*斜体*文本。

- 列表项1
- 列表项2

> 引用文本

[链接文本](http://example.com)

![图片说明](http://example.com/image.jpg)

```

### JDBC解释器

连接并查询各种数据库：

```
%jdbc
SELECT * FROM customers LIMIT 10
```

也可以指定不同的JDBC连接：

```
%jdbc(mysql)
SELECT * FROM orders WHERE order_date > '2022-01-01'

%jdbc(postgres)
SELECT * FROM products WHERE price > 100
```

## 常见问题排查

### 启动问题

**问题**：Zeppelin无法启动
**排查**：
- 检查日志文件`logs/zeppelin-*.log`
- 确认Java版本与安装匹配
- 检查端口是否被占用：`netstat -tulpn | grep 8080`
- 确保有足够的磁盘空间和权限

**问题**：Web界面打不开
**排查**：
- 检查防火墙设置
- 确认服务器地址配置正确
- 尝试使用`localhost`代替IP地址
- 检查浏览器控制台是否有错误

### 解释器问题

**问题**：Spark解释器启动失败
**排查**：
- 确认SPARK_HOME路径正确
- 检查Spark是否能独立运行
- 查看Zeppelin日志中与Spark相关的错误
- 尝试重启解释器

**问题**：JDBC连接失败
**排查**：
- 验证数据库凭据
- 确认JDBC驱动已正确加载
- 检查网络连接和防火墙设置
- 尝试使用其他工具验证连接

### 性能问题

**问题**：笔记本执行缓慢
**排查**：
- 查看解释器资源设置
- 减少一次性处理的数据量
- 检查集群资源利用率
- 优化查询和处理逻辑
- 增加JVM内存分配

**问题**：内存溢出错误
**排查**：
- 增加JVM堆内存：编辑`zeppelin-env.sh`中的`ZEPPELIN_JAVA_OPTS`
- 限制结果集大小
- 优化数据处理逻辑
- 使用采样或聚合减少数据量

### 数据可视化问题

**问题**：图表显示不正确
**排查**：
- 检查数据格式是否符合要求
- 尝试使用不同的图表类型
- 使用表格视图验证原始数据
- 确认浏览器是否支持所选可视化类型

## 最佳实践

### 笔记本组织

1. **结构化笔记本**
   - 使用Markdown段落创建标题和说明
   - 将相关代码分组到相邻段落
   - 提供足够的注释和文档

2. **文件夹组织**
   - 按项目或团队创建文件夹
   - 为长期和临时笔记本创建不同的路径
   - 使用命名约定保持一致性

3. **版本管理**
   - 定期保存重要的笔记本状态
   - 考虑将笔记本导出并存储在Git中
   - 记录重大更改和实验

### 性能优化

1. **解释器优化**
   - 为不同用例使用单独的解释器实例
   - 在笔记本不活跃时关闭解释器
   - 为大型操作配置适当的资源

2. **数据处理**
   - 处理前先对数据进行采样
   - 使用分区和过滤减少数据量
   - 缓存频繁使用的中间结果
   - 避免在单个段落中处理过多数据

3. **可视化优化**
   - 限制可视化中的数据点数量
   - 对于大型数据集，考虑聚合后再可视化
   - 使用合适的图表类型展示数据

### 安全最佳实践

1. **身份验证**
   - 始终在生产环境中启用身份验证
   - 使用强密码和适当的角色分配
   - 考虑与企业LDAP/AD集成

2. **数据安全**
   - 避免在笔记本中硬编码敏感凭据
   - 使用环境变量或凭据提供者
   - 考虑加密敏感数据

3. **网络安全**
   - 配置HTTPS以加密网络流量
   - 限制解释器的网络访问
   - 使用防火墙限制Zeppelin的访问

### 协作实践

1. **共享标准**
   - 创建团队笔记本模板
   - 制定命名和文档标准
   - 清晰记录数据源和处理步骤

2. **知识分享**
   - 创建常用分析的示例笔记本
   - 记录数据结构和业务规则
   - 分享发现和见解

## 参考资源

- [Apache Zeppelin官方文档](https://zeppelin.apache.org/docs/latest/)
- [Apache Zeppelin GitHub仓库](https://github.com/apache/zeppelin)
- [Apache Zeppelin Wiki](https://cwiki.apache.org/confluence/display/ZEPPELIN/Zeppelin+Home)
- [Zeppelin用户邮件列表](https://zeppelin.apache.org/community.html)
- [StackOverflow上的Zeppelin问题](https://stackoverflow.com/questions/tagged/apache-zeppelin)
- [Zeppelin教程和示例](https://zeppelin.apache.org/docs/latest/quickstart/tutorial.html) 