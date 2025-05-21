# Apache HBase 用户指南

本指南旨在帮助用户理解如何在 DataSophon 平台部署、配置、管理和使用 Apache HBase 服务。

## 1. 服务部署

HBase 服务通常依赖于 HDFS 和 ZooKeeper。在 DataSophon 平台部署 HBase 前，请确保 HDFS 和 ZooKeeper 服务已经成功部署并处于健康状态。

通过 DataSophon 的服务管理界面：
1.  选择 "添加服务"。
2.  从服务列表中选择 "HBase"。
3.  根据集群规划，选择 HBase Master (HMaster) 和 RegionServer 角色需要部署的节点。
    *   **HMaster**: 建议至少部署2个节点以实现高可用 (一个 Active, 一个 Standby)。
    *   **RegionServer**: 根据数据存储需求和并发访问量选择合适的节点数量。
4.  DataSophon 会自动处理依赖关系并推荐配置。用户可以根据实际需求调整配置参数。
5.  确认配置后，点击 "部署"。DataSophon 将自动完成 HBase 服务的安装、配置和启动。

## 2. 服务配置

HBase 的核心配置文件是 `hbase-site.xml`。通过 DataSophon 的配置管理界面，可以方便地修改这些参数。

### 关键配置参数

#### HBase Master (HMaster) 相关
*   `hbase.master.port`: HMaster 服务端口，默认为 16000。
*   `hbase.master.info.port`: HMaster Web UI 端口，默认为 16010。
*   `hbase.master.logcleaner.plugins`: HMaster 日志清理插件。
*   `hbase.master.logcleaner.ttl`: WAL (Write-Ahead Log) 在 HDFS 上的保留时间。

#### RegionServer 相关
*   `hbase.regionserver.port`: RegionServer 服务端口，默认为 16020。
*   `hbase.regionserver.info.port`: RegionServer Web UI 端口，默认为 16030。
*   `hbase.regionserver.handler.count`: RegionServer 用于处理客户端请求的 RPC 线程数。根据并发量和节点配置调整。
*   `hbase.hregion.memstore.flush.size`: MemStore 刷写到磁盘的阈值。当一个 MemStore 的大小达到此值时，会触发 flush 操作。
*   `hbase.hregion.max.filesize`: 一个 Region 分裂 (split) 前的最大 HFile 大小。当 Region 中的 HFile 总大小超过此值时，Region 会分裂。
*   `hbase.regionserver.global.memstore.size`: 单个 RegionServer 上所有 MemStore 占用的总内存上限比例 (相对于 RegionServer 堆内存)。
*   `hfile.block.cache.size`: BlockCache (HBase 读缓存) 占 RegionServer 堆内存的比例。

#### ZooKeeper 相关
*   `hbase.zookeeper.quorum`: ZooKeeper 集群的地址列表 (例如: `zkhost1,zkhost2,zkhost3`)。DataSophon 会根据 ZooKeeper 服务部署自动配置。
*   `hbase.zookeeper.property.clientPort`: ZooKeeper 客户端端口，默认为 2181。
*   `zookeeper.session.timeout`: ZooKeeper 会话超时时间。

#### HDFS 相关
*   `hbase.rootdir`: HBase 在 HDFS 上的根目录 (例如: `hdfs://namenode-host:8020/hbase`)。DataSophon 会根据 HDFS 服务部署自动配置。

#### 性能与调优
*   **MemStore 大小**: `hbase.hregion.memstore.flush.size`。增大此值可以减少 flush 次数，提高写性能，但会增加 flush 时的 I/O 压力和恢复时间。
*   **BlockCache 大小**: `hfile.block.cache.size`。增大此值可以缓存更多的数据块，提高读性能，但会减少可用于 MemStore 和其他操作的内存。
*   **Compaction 配置**:
    *   `hbase.hstore.compactionThreshold`: 一个 Store (列族在一个 Region 中的存储) 中允许的最小 HFile 数量。当 HFile 数量超过此阈值时，会触发 Minor Compaction。
    *   `hbase.hstore.blockingStoreFiles`: 当一个 Store 中的 HFile 数量达到此值时，会阻塞写操作，直到 Compaction 完成。
    *   `hbase.majorcompaction.period`: 自动执行 Major Compaction 的周期 (毫秒)。默认为7天。`0` 表示禁用自动 Major Compaction。
*   **WAL 配置**:
    *   `hbase.wal.provider`: WAL 的提供者，可以是 `filesystem` (默认) 或 `multiwal` (允许多个 WAL 实例，提高并发写吞吐量)。

### 通过 DataSophon 修改配置
1.  进入 "服务管理" -> "HBase" -> "配置"。
2.  修改需要的参数。可以通过搜索框查找特定参数。
3.  修改完成后，点击 "保存配置"。
4.  根据提示，可能需要重启 HBase 服务或相关组件使配置生效。

## 3. 服务管理与监控

### 服务启停
通过 DataSophon 的服务管理界面，可以方便地启动、停止 HBase 服务及其组件 (HMaster, RegionServer)。

### Web UI
HBase 提供了 Web UI 来监控集群状态：
*   **HMaster UI**: `http://<active-hmaster-host>:16010`
    *   显示集群概览、表信息、RegionServer 列表、任务信息等。
    *   可以查看表的 Region 分布、执行 DDL 操作 (如 enable/disable 表)。
*   **RegionServer UI**: `http://<regionserver-host>:16030`
    *   显示该 RegionServer 管理的 Region 列表、请求统计、BlockCache 统计、MemStore 大小、WAL 信息等。

DataSophon 通常会在 HBase 服务页面提供这些 UI 的快捷链接。

### 监控指标
DataSophon 会集成 HBase 的关键监控指标，并在仪表盘中展示，帮助用户实时了解服务健康状况和性能表现。关键指标包括：
*   **集群层面**:
    *   Region 总数、在线 RegionServer 数量。
    *   读写请求速率 (QPS)、平均延迟。
*   **HMaster 层面**:
    *   Active HMaster、Standby HMaster 状态。
    *   Region In Transition (RIT) 数量。
*   **RegionServer 层面**:
    *   每个 RegionServer 的 Region 数量、读写请求数、队列长度。
    *   MemStore 大小、BlockCache 命中率、Compaction 队列长度。
    *   GC 时间和频率。

## 4. HBase Shell 使用

HBase Shell 是与 HBase 交互的主要命令行工具。可以通过在安装了 HBase 客户端的节点上执行 `hbase shell` 命令启动。

### 基本操作

#### 连接到集群
通常情况下，`hbase shell` 会自动通过 `hbase-site.xml` 中的 ZooKeeper 地址连接到集群。

#### 命名空间 (Namespace)
*   创建命名空间:
    ```hbase
    create_namespace 'my_namespace'
    ```
*   列出命名空间:
    ```hbase
    list_namespace
    ```
*   删除命名空间 (必须为空):
    ```hbase
    drop_namespace 'my_namespace'
    ```

#### 表 (Table)
*   创建表:
    ```hbase
    # 语法: create 'table_name', 'column_family1', 'column_family2', ...
    # 或 create 'table_name', {NAME => 'cf1', VERSIONS => 3}, {NAME => 'cf2', TTL => 86400}
    create 'my_namespace:my_table', 'cf_info', 'cf_data'
    create 'test_table', {NAME => 'cf1', VERSIONS => 5} 
    ```
*   列出表:
    ```hbase
    list
    list 'my_namespace:.*' # 列出特定命名空间下的表
    ```
*   描述表结构:
    ```hbase
    describe 'my_namespace:my_table'
    ```
*   禁用表 (修改或删除表前需要先禁用):
    ```hbase
    disable 'my_namespace:my_table'
    ```
*   检查表是否已禁用:
    ```hbase
    is_disabled 'my_namespace:my_table'
    ```
*   启用表:
    ```hbase
    enable 'my_namespace:my_table'
    ```
*   检查表是否已启用:
    ```hbase
    is_enabled 'my_namespace:my_table'
    ```
*   删除表 (必须先禁用):
    ```hbase
    drop 'my_namespace:my_table'
    ```
*   修改表结构 (添加/删除列族，修改列族属性):
    ```hbase
    # 必须先禁用表
    disable 'my_namespace:my_table'
    # 添加列族
    alter 'my_namespace:my_table', NAME => 'cf_new'
    # 修改列族版本数
    alter 'my_namespace:my_table', NAME => 'cf_info', VERSIONS => 5
    # 删除列族
    alter 'my_namespace:my_table', NAME => 'cf_data', METHOD => 'delete' 
    # 或者 alter 'my_namespace:my_table', 'delete' => 'cf_data'
    enable 'my_namespace:my_table'
    ```

#### 数据操作 (CRUD)
*   **写入数据 (Put)**:
    ```hbase
    # 语法: put 'table_name', 'row_key', 'column_family:column_qualifier', 'value'
    put 'my_namespace:my_table', 'row1', 'cf_info:name', 'Alice'
    put 'my_namespace:my_table', 'row1', 'cf_info:age', '30'
    put 'my_namespace:my_table', 'row2', 'cf_data:payload', '{"some":"data"}'
    ```
*   **读取数据 (Get)**:
    ```hbase
    # 语法: get 'table_name', 'row_key'
    # 获取特定列: get 'table_name', 'row_key', 'cf:cq'
    # 获取特定列族: get 'table_name', 'row_key', 'cf'
    # 获取多个版本: get 'table_name', 'row_key', {COLUMN => 'cf:cq', VERSIONS => 3}
    get 'my_namespace:my_table', 'row1'
    get 'my_namespace:my_table', 'row1', 'cf_info:name'
    get 'my_namespace:my_table', 'row1', {COLUMN => 'cf_info:age', VERSIONS => 2}
    ```
*   **扫描数据 (Scan)**:
    ```hbase
    # 语法: scan 'table_name'
    # 带过滤器: scan 'table_name', {FILTER => "filter_expression"}
    # 限制行数: scan 'table_name', {LIMIT => 10}
    # 指定列: scan 'table_name', {COLUMNS => ['cf_info:name', 'cf_data']}
    # 指定行键范围: scan 'table_name', {STARTROW => 'row1', ENDROW => 'row3'}
    scan 'my_namespace:my_table'
    scan 'my_namespace:my_table', {LIMIT => 5}
    scan 'my_namespace:my_table', {STARTROW => 'user100', STOPROW => 'user200', COLUMNS => 'cf_info:email'}
    scan 'my_namespace:my_table', {FILTER => "ValueFilter(=, 'binary:Alice')"}
    ```
*   **删除数据 (Delete)**:
    ```hbase
    # 删除特定单元格最新版本: delete 'table_name', 'row_key', 'cf:cq'
    # 删除特定单元格指定版本: delete 'table_name', 'row_key', 'cf:cq', timestamp
    # 删除特定列所有版本: deleteall 'table_name', 'row_key', 'cf:cq'
    # 删除整行: deleteall 'table_name', 'row_key'
    delete 'my_namespace:my_table', 'row1', 'cf_info:age' 
    deleteall 'my_namespace:my_table', 'row2' 
    ```
*   **计数 (Count)**:
    ```hbase
    # 统计表行数 (较慢，会全表扫描)
    count 'my_namespace:my_table'
    count 'my_namespace:my_table', {INTERVAL => 1000, CACHE => 10} # 设置汇报间隔和缓存
    ```

### 常用 Shell 命令
*   `status`: 显示集群状态 (master, backup masters, regionservers, dead servers, regions in transition)。
*   `version`: 显示 HBase 版本。
*   `whoami`: 显示当前 HBase 用户和组。
*   `exit`: 退出 HBase Shell。

## 5. 备份与恢复

HBase 数据的备份通常依赖于 HDFS 的快照功能或者使用 HBase 提供的 `Export` 和 `Import` 工具。

### 快照 (Snapshot)
HBase 提供了表快照功能，可以在不影响在线服务的情况下创建表的某个时间点的一致性副本。
*   创建快照:
    ```hbase
    snapshot 'my_namespace:my_table', 'my_table_snapshot_20231201'
    ```
*   列出快照:
    ```hbase
    list_snapshots
    ```
*   从快照恢复表 (会先禁用原表，然后恢复，原表数据会被覆盖):
    ```hbase
    disable 'my_namespace:my_table'
    restore_snapshot 'my_table_snapshot_20231201'
    enable 'my_namespace:my_table'
    ```
*   克隆快照为新表:
    ```hbase
    clone_snapshot 'my_table_snapshot_20231201', 'my_namespace:my_table_clone'
    ```
*   删除快照:
    ```hbase
    delete_snapshot 'my_table_snapshot_20231201'
    ```

### Export 和 Import 工具
`Export` 工具可以将表数据导出到 HDFS 上的 SequenceFile 文件，`Import` 工具则可以将这些文件导入到 HBase 表中。
*   **Export**:
    ```bash
    hbase org.apache.hadoop.hbase.mapreduce.Export <tablename> <outputdir> [ <versions> [<starttime> [<endtime>]] ]
    # 示例: 导出 my_table 到 /backup/my_table_export
    hbase org.apache.hadoop.hbase.mapreduce.Export 'my_namespace:my_table' /backup/my_table_export_$(date +%F)
    ```
*   **Import**:
    ```bash
    hbase org.apache.hadoop.hbase.mapreduce.Import <tablename> <inputdir>
    # 示例: 从 /backup/my_table_export 导入数据到 my_table_restored
    hbase org.apache.hadoop.hbase.mapreduce.Import 'my_namespace:my_table_restored' /backup/my_table_export_YYYY-MM-DD
    ```

## 6. 性能调优与最佳实践

### Row Key 设计
*   **散列与反转**: 避免行键的顺序写入导致热点问题。对于时间序列数据或单调递增的键，可以考虑对行键进行散列 (hashing) 或反转 (reversing) 处理。
*   **长度**: 行键不宜过长，因为它们会存储在每个单元格中，并影响索引大小和内存占用。
*   **可读性与业务相关性**: 在满足散列要求的前提下，尽量使行键具有一定的可读性和业务含义，方便排查问题。

### 列族设计
*   **数量**: 列族不宜过多，通常建议少于3个。过多的列族会影响 Compaction 和查询性能。
*   **命名**: 列族名称尽量简短。
*   **数据访问模式**: 将访问模式相似的列放在同一个列族。例如，经常一起读取的列应放在一个列族，不常用的列可以放在另一个列族。

### 数据压缩
为列族启用数据压缩 (如 SNAPPY, LZO, GZIP) 可以有效减少存储空间和网络 I/O。SNAPPY 通常是性能和压缩率较好的折中选择。
```hbase
alter 'my_table', NAME => 'cf_data', COMPRESSION => 'SNAPPY'
```

### Bloom Filter
为列族启用 Bloom Filter 可以加速某些 Get 和 Scan 操作，通过快速判断一个 HFile 是否可能包含某个行键或行+列的数据。
```hbase
alter 'my_table', NAME => 'cf_info', BLOOMFILTER => 'ROW' # 或 ROWCOL
```

### 预分区 (Pre-splitting)
对于新创建的空表，如果预知数据量会很大且行键分布比较均匀，可以进行预分区。这样可以避免初始阶段所有数据写入单个 Region 造成的热点，以及后续频繁的自动分裂。
```hbase
# 创建表时指定分裂点
create 'my_table', 'cf', SPLITS => ['0100', '0200', '0300', '0400']
# 或者通过 SPLITALGO 指定分裂算法和数量
# create 'my_table', 'cf', {SPLITALGO => 'HexStringSplit', NUMREGIONS => 16}
```

### JVM 调优
*   合理配置 RegionServer 和 HMaster 的 JVM 堆大小 (`-Xms`, `-Xmx`)。
*   选择合适的垃圾收集器 (Garbage Collector)，如 G1GC，并调整其参数。
*   监控 GC 活动，避免长时间的 Full GC。

### 监控与告警
*   持续监控 HBase 集群的关键指标。
*   设置合理的告警阈值，及时发现并处理问题。

## 7. 故障排查

### HMaster 无法启动
*   检查 ZooKeeper 服务是否正常。
*   检查 HDFS 服务是否正常，`hbase.rootdir` 是否可访问。
*   查看 HMaster 日志获取详细错误信息。

### RegionServer 无法启动或频繁宕机
*   检查 ZooKeeper 连接。
*   检查与 HDFS 的连接。
*   检查节点磁盘空间、内存、CPU 资源。
*   查看 RegionServer 日志，特别是 OutOfMemoryError 或 GC 相关问题。

### Region In Transition (RIT)
*   当 Region 处于 RIT 状态过久时，表示 Region 的分配或恢复过程卡住。
*   可以通过 HMaster UI 查看 RIT 详情。
*   尝试使用 HBase HBCK2 (HBase 2.x+) 工具进行修复。
    ```bash
    # HBase 2.x+
    hbase hbck -j <path_to_hbck2_jar> assigns <encoded_region_name>
    hbase hbck -j <path_to_hbck2_jar> unassigns <encoded_region_name>
    ```

### 数据读写慢
*   检查是否存在热点 RegionServer。
*   检查 Compaction 是否过于频繁或积压。
*   检查网络延迟。
*   分析 BlockCache 命中率。
*   检查客户端查询模式是否高效 (如 Scan 是否使用了合适的 Filter)。

通过本指南，您应该能够更好地在 DataSophon 平台上管理和使用 HBase 服务。对于更深入的配置和问题排查，请参考 Apache HBase 官方文档。 