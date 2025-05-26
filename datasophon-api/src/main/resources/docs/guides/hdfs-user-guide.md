# HDFS 用户指南

## 快速入门
HDFS是Hadoop分布式文件系统的核心组件，本指南将帮助您快速上手使用HDFS的基本功能。

### 基本命令示例
```bash
# 列出文件
hdfs dfs -ls /

# 创建目录
hdfs dfs -mkdir /user/example

# 上传文件
hdfs dfs -put localfile.txt /user/example/

# 下载文件
hdfs dfs -get /user/example/file.txt local-file.txt
```

### 常用命令速查表

| 命令 | 描述 | 示例 |
|------|------|------|
| `-ls` | 列出目录内容 | `hdfs dfs -ls /user` |
| `-mkdir` | 创建目录 | `hdfs dfs -mkdir -p /user/example/data` |
| `-put` | 上传文件 | `hdfs dfs -put localfile.txt /user/example/` |
| `-get` | 下载文件 | `hdfs dfs -get /user/example/file.txt local-file.txt` |
| `-cp` | 复制文件 | `hdfs dfs -cp /source/file.txt /dest/` |
| `-mv` | 移动文件 | `hdfs dfs -mv /source/file.txt /dest/` |
| `-rm` | 删除文件 | `hdfs dfs -rm /user/example/file.txt` |
| `-rmdir` | 删除空目录 | `hdfs dfs -rmdir /user/example/emptydir` |
| `-cat` | 查看文件内容 | `hdfs dfs -cat /user/example/file.txt` |
| `-tail` | 查看文件尾部 | `hdfs dfs -tail /user/example/logfile.log` |
| `-du` | 查看文件/目录大小 | `hdfs dfs -du -h /user/example/` |

相关链接：[HDFS 官方文档](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsUserGuide.html)

## 文件系统操作

### 文件与目录操作

#### 基本操作

```bash
# 创建多级目录
hdfs dfs -mkdir -p /user/example/data/year=2023/month=06

# 复制文件
hdfs dfs -cp /source/file /dest/

# 移动文件
hdfs dfs -mv /source/file /dest/

# 删除文件
hdfs dfs -rm /user/example/file.txt

# 递归删除目录
hdfs dfs -rm -r /user/example/directory

# 删除空目录
hdfs dfs -rmdir /user/example/emptydir
```

> ⚠️ **警告**：删除操作无法撤销，请谨慎使用

#### 检查文件内容

```bash
# 查看文件内容
hdfs dfs -cat /user/example/file.txt

# 查看大文件的前10行
hdfs dfs -cat /user/example/bigfile.txt | head -10

# 查看文件尾部
hdfs dfs -tail /user/example/logfile.log

# 查看文件统计信息
hdfs dfs -stat "%n %o %r %F %u:%g %b %y" /user/example/file.txt
```

#### 文件拷贝和合并

```bash
# 将多个本地文件上传到HDFS
hdfs dfs -put file1.txt file2.txt /user/example/

# 将多个HDFS文件下载到本地
hdfs dfs -get /user/example/file1.txt /user/example/file2.txt ./

# 合并下载多个文件到一个本地文件
hdfs dfs -getmerge /user/example/data/ ./merged-data.txt

# 将本地目录上传到HDFS
hdfs dfs -put ./local-dir /user/example/
```

### 存储空间管理

```bash
# 查看目录大小
hdfs dfs -du -h /user/example/

# 查看总体使用情况
hdfs dfs -du -s -h /user/

# 检查HDFS总体使用情况
hdfs dfsadmin -report

# 查看配额使用情况
hdfs dfs -count -q /user/example/
```

## 权限管理

HDFS提供了类似于POSIX的文件权限模型，可以控制对文件和目录的访问。

### 权限解释

HDFS权限由三部分组成：

- 所有者权限：文件/目录所有者的访问权限
- 组权限：所有者所在组的访问权限
- 其他用户权限：不属于所有者和组的用户的访问权限

每种权限可以包含以下几种：

- r (读取, 值为4): 允许读取文件内容或列出目录
- w (写入, 值为2): 允许修改文件或在目录中创建、删除文件
- x (执行, 值为1): 允许执行文件或访问目录

### 权限管理命令

```bash
# 查看文件权限
hdfs dfs -ls /user/example/file.txt

# 更改文件权限 (读、写、执行)
hdfs dfs -chmod 755 /user/example/file.txt

# 更改文件所有者
hdfs dfs -chown user:group /user/example/file.txt

# 递归更改目录权限
hdfs dfs -chmod -R 755 /user/example/

# 增加特定权限 (只增加执行权限)
hdfs dfs -chmod +x /user/example/script.sh

# 移除特定权限 (移除写入权限)
hdfs dfs -chmod -w /user/example/readonly.txt
```

### 权限最佳实践

1. **最小权限原则**：只授予完成任务所需的最小权限
2. **使用组**：通过组管理权限，而不是单独授权每个用户
3. **目录权限**：设置合适的目录权限来控制新建文件的访问
4. **定期审计**：定期检查权限设置，确保安全

## 配额管理

HDFS允许管理员设置目录的配额，限制可以存储的文件数量和空间大小。

### 配额类型

- **名称配额**：限制目录中的文件和子目录数量
- **空间配额**：限制目录及其子目录使用的总磁盘空间

### 配额命令

```bash
# 设置目录的命名配额（文件数量限制）
hdfs dfsadmin -setQuota 1000 /user/example/

# 设置目录的空间配额
hdfs dfsadmin -setSpaceQuota 10g /user/example/

# 移除命名配额
hdfs dfsadmin -clrQuota /user/example/

# 移除空间配额
hdfs dfsadmin -clrSpaceQuota /user/example/

# 查看配额使用情况
hdfs dfs -count -q /user/example/
```

### 配额使用场景

- **资源隔离**：防止单个用户或应用使用过多存储资源
- **成本控制**：限制特定项目或部门的存储使用量
- **防止滥用**：防止误操作导致存储空间耗尽

## 数据管理最佳实践

### 文件大小建议

- **大文件优于小文件**：HDFS针对大文件优化，建议文件大小为HDFS块大小(默认128MB)的倍数
- **小文件处理**：对于大量小文件，考虑使用HAR(Hadoop Archive)、SequenceFile或其他文件压缩/合并方案
- **文件拆分**：超大文件应考虑合理拆分，便于并行处理

### 数据组织

- **分区存储**：按日期、地区等关键维度组织数据目录结构，如 `/data/year=2023/month=06/day=15/`
- **命名规范**：采用一致的文件命名规范，包含日期、数据源等信息
- **元数据管理**：保留数据描述文件，记录字段含义、来源等信息

### 压缩策略

```bash
# 上传压缩文件(HDFS会保持压缩状态)
hdfs dfs -put compressed-data.gz /user/example/

# 创建压缩文件
hdfs dfs -cat /user/example/data.txt | gzip | hdfs dfs -put - /user/example/data.txt.gz
```

| 压缩格式 | 适用场景 | 特点 |
|---------|---------|------|
| Gzip | 单文件压缩 | 压缩率高，不支持切分 |
| LZO | MapReduce处理 | 压缩率中，支持切分 |
| Snappy | 临时数据 | 压缩速度快，压缩率中 |
| Bzip2 | 长期存储 | 压缩率最高，速度最慢 |

## 文件系统检查与维护

### 检查文件系统

```bash
# 基本文件系统检查
hdfs fsck /

# 详细检查指定路径
hdfs fsck /user/example -files -blocks -locations

# 查找损坏的文件
hdfs fsck / -list-corruptfileblocks

# 检查文件块副本
hdfs fsck /user/example/file.txt -files -blocks -locations

# 修复丢失的块(删除)
hdfs fsck / -delete
```

### 平衡数据块分布

```bash
# 启动平衡器
hdfs balancer -threshold 10

# 查看数据节点的块分布
hdfs dfsadmin -report
```

## 命令行技巧

### 批量操作

```bash
# 批量删除文件(使用通配符)
hdfs dfs -rm /user/example/data/tmp/*.tmp

# 使用find结合xargs批量操作
hdfs dfs -ls -R /user/example/ | grep "\.log$" | awk '{print $8}' | xargs -I {} hdfs dfs -rm {}

# 并行上传多个文件
find ./logs/ -name "*.log" | xargs -P 5 -I {} hdfs dfs -put {} /user/example/logs/
```

### 数据传输优化

```bash
# 使用distcp在HDFS集群间复制数据
hadoop distcp hdfs://cluster1/user/example/ hdfs://cluster2/backup/

# 设置增量复制
hadoop distcp -update hdfs://cluster1/user/example/ hdfs://cluster2/backup/

# 设置带宽限制
hadoop distcp -bandwidth 10 hdfs://cluster1/user/example/ hdfs://cluster2/backup/

# 跳过CRC检查(当源文件可信时可用于提速)
hadoop distcp -skipcrccheck hdfs://cluster1/user/example/ hdfs://cluster2/backup/
```

## WebHDFS REST API使用

除了命令行工具，HDFS还提供了基于HTTP的REST API，可以通过标准HTTP请求访问HDFS。

### 常用REST操作

```bash
# 列出目录(GET)
curl -i "http://<namenode>:50070/webhdfs/v1/user/example?op=LISTSTATUS"

# 读取文件(GET)
curl -i "http://<namenode>:50070/webhdfs/v1/user/example/file.txt?op=OPEN"

# 创建文件(PUT)
curl -i -X PUT "http://<namenode>:50070/webhdfs/v1/user/example/newfile.txt?op=CREATE&overwrite=true"
# 然后再次PUT到返回的Location

# 追加文件(POST)
curl -i -X POST "http://<namenode>:50070/webhdfs/v1/user/example/file.txt?op=APPEND"
# 然后再次POST到返回的Location

# 删除文件(DELETE)
curl -i -X DELETE "http://<namenode>:50070/webhdfs/v1/user/example/file.txt?op=DELETE"
```

## 常见问题

### 如何检查HDFS的健康状态？

可以使用`hdfs dfsadmin -report`命令查看HDFS集群的基本健康状况，包括NameNode和DataNode的状态、容量使用情况等。

详细检查方法：

```bash
# 检查集群整体状态
hdfs dfsadmin -report

# 检查NameNode状态
hdfs haadmin -getServiceState nn1  # 对于HA集群

# 查看活跃的DataNode
hdfs dfsadmin -report | grep "Live datanodes"

# 检查是否有丢失的数据块
hdfs fsck / | grep "Missing blocks"
```

### 如何处理HDFS的安全模式问题？

HDFS在启动时会进入安全模式进行文件系统检查。此时文件系统是只读的，无法进行写操作。

```bash
# 检查是否处于安全模式
hdfs dfsadmin -safemode get

# 等待安全模式结束
hdfs dfsadmin -safemode wait

# 手动离开安全模式
hdfs dfsadmin -safemode leave

# 强制进入安全模式(维护时使用)
hdfs dfsadmin -safemode enter
```

> ⚠️ **注意**：正常情况下，安全模式会自动退出。强制退出可能导致数据不一致。

### 文件在HDFS中是如何存储的？

HDFS会将大文件分割成块(通常128MB)，并将这些块分布存储在多个DataNode上。默认情况下，每个块会有3个副本分布在不同的机架上，以提高数据可靠性。 

文件存储机制：

1. **文件拆分**：大文件被拆分成固定大小的块
2. **块存储**：每个块作为独立单元存储在DataNode上
3. **副本放置**：基于机架感知策略放置多个副本
4. **元数据管理**：NameNode维护文件名到块映射的元数据

### 如何处理数据倾斜问题？

数据倾斜会导致某些节点存储的数据远多于其他节点，影响集群平衡。

解决方法：

```bash
# 运行HDFS均衡器程序
hdfs balancer -threshold 10

# 检查数据分布
hdfs dfsadmin -report

# 设置新数据块放置策略(修改hdfs-site.xml)
# <property>
#   <name>dfs.datanode.balance.bandwidthPerSec</name>
#   <value>10485760</value> <!-- 10MB/s -->
# </property>
```

### 如何恢复误删除的文件？

HDFS默认没有回收站功能，但可以配置启用：

```xml
<!-- 在hdfs-site.xml中配置 -->
<property>
  <name>fs.trash.interval</name>
  <value>1440</value> <!-- 以分钟为单位，1440=1天 -->
</property>
<property>
  <name>fs.trash.checkpoint.interval</name>
  <value>120</value> <!-- 以分钟为单位 -->
</property>
```

恢复文件：

```bash
# 查看回收站内容
hdfs dfs -ls /user/<username>/.Trash/Current/

# 从回收站恢复文件
hdfs dfs -mv /user/<username>/.Trash/Current/path/to/deleted/file /destination/path/
```

## 高级配置和优化

### HDFS客户端优化

编辑`hdfs-site.xml`或客户端配置：

```xml
<!-- 设置读取缓冲区大小(默认4K) -->
<property>
  <name>dfs.client.read.shortcircuit.buffer.size</name>
  <value>131072</value> <!-- 128K -->
</property>

<!-- 启用本地读取(短路读) -->
<property>
  <name>dfs.client.read.shortcircuit</name>
  <value>true</value>
</property>

<!-- 设置预读缓冲区大小 -->
<property>
  <name>dfs.client.read.prefetch.size</name>
  <value>10485760</value> <!-- 10MB -->
</property>
```

### JVM和操作系统调优

```bash
# 设置DataNode的Java堆大小
export HADOOP_HEAPSIZE=4096  # 4GB

# 调整Linux文件描述符限制
ulimit -n 65536

# 调整vm.swappiness减少交换
sysctl -w vm.swappiness=10
```

### 数据备份策略

```bash
# 使用distcp进行集群间备份
hadoop distcp -update -prbugp hdfs://primary-cluster/data/ hdfs://backup-cluster/data/

# 设置备份作业定时运行
crontab -e 
# 添加: 0 2 * * * /path/to/backup_script.sh

# 增量备份示例
hadoop distcp -update -delete -diff s1 -p hdfs://primary/data/ hdfs://backup/data/
``` 