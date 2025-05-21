# Apache ZooKeeper 用户指南

本指南旨在帮助用户理解如何在 DataSophon 平台部署、配置、管理和使用 Apache ZooKeeper 服务。

## 1. 服务部署

ZooKeeper 是许多大数据组件 (如 HDFS HA, YARNResourceManager HA, HBase, Kafka, Flink HA 等) 正常运行所必需的核心协调服务。

通过 DataSophon 的服务管理界面：
1.  选择 "添加服务"。
2.  从服务列表中选择 "ZooKeeper"。
3.  根据集群规划，选择 ZooKeeper Server 角色需要部署的节点。为了保证高可用和数据一致性，ZooKeeper 集群通常需要部署奇数个 (至少3个) ZooKeeper Server 实例 (例如，3个、5个或7个节点)。
4.  DataSophon 会自动处理依赖关系并推荐配置。用户可以根据实际需求调整配置参数，特别是 `myid` 的配置需要确保每个节点的唯一性 (DataSophon 会自动处理)。
5.  确认配置后，点击 "部署"。DataSophon 将自动完成 ZooKeeper 服务的安装、配置和启动。

## 2. 服务配置

ZooKeeper 的核心配置文件是 `zoo.cfg`。通过 DataSophon 的配置管理界面，可以方便地修改这些参数。

### 关键配置参数

*   **`tickTime`**: ZooKeeper 中最基本的时间单元 (毫秒)。用于控制心跳间隔和会话超时。例如，会话超时通常是 `tickTime` 的倍数。
    *   默认值: `2000` (2秒)。
*   **`initLimit`**: Leader 和 Follower 初始连接时能容忍的最多心跳数 (tickTime 的倍数)。如果在此时间内 Follower 未能成功连接并同步到 Leader，则连接失败。
    *   默认值: `10` (即 `10 * tickTime`)。
*   **`syncLimit`**: Leader 和 Follower 之间发送消息，请求和应答时间长度，最长不能超过多少个 tickTime 的时间长度。
    *   默认值: `5` (即 `5 * tickTime`)。
*   **`dataDir`**: ZooKeeper 存储其数据快照 (snapshot) 的目录。**重要**: 每个 ZooKeeper Server 的 `dataDir` 必须是独立的本地目录，不能共享。
*   **`dataLogDir`**: (可选) ZooKeeper 存储其事务日志 (WAL - Write Ahead Log) 的目录。为了获得最佳性能和可靠性，建议将 `dataLogDir` 配置在与 `dataDir` 不同的物理磁盘上。
*   **`clientPort`**: ZooKeeper 服务器监听客户端连接的 TCP 端口。
    *   默认值: `2181`。
*   **`maxClientCnxns`**: 单个客户端 IP 地址允许连接到单个 ZooKeeper 服务器的最大并发连接数。`0` 表示不限制。谨慎设置此值以防止滥用。
    *   默认值: `60`。
*   **`autopurge.snapRetainCount`**: 在 `dataDir` 中保留的快照文件数量。旧的快照会被自动清理。
    *   默认值: `3`。
*   **`autopurge.purgeInterval`**: 自动清理任务的执行间隔 (小时)。`0` 表示禁用自动清理。
    *   默认值: `1` (每小时执行一次)。
*   **`server.X=hostname:peerPort:electionPort`**: 定义集群中每个服务器的配置。其中：
    *   `X`: 是服务器的 ID (即 `myid`)，必须是 1 到 255 之间的整数，并且在集群中唯一。
    *   `hostname`: 服务器的主机名或 IP 地址。
    *   `peerPort`: 服务器之间用于通信和数据同步的端口 (Follower 连接 Leader)。
    *   `electionPort`: 服务器之间用于 Leader 选举的端口。
    *   DataSophon 会根据用户选择的节点自动生成这些配置。

### `myid` 文件
每个 ZooKeeper 服务器在 `dataDir` 目录下都需要一个名为 `myid` 的文件，该文件内容是该服务器的唯一 ID (即上述 `server.X` 中的 `X`)。
DataSophon 在部署时会自动为每个 ZooKeeper Server 节点创建和配置正确的 `myid` 文件。

### 通过 DataSophon 修改配置
1.  进入 "服务管理" -> "ZooKeeper" -> "配置"。
2.  修改需要的参数。可以通过搜索框查找特定参数。
3.  修改完成后，点击 "保存配置"。
4.  根据提示，通常需要滚动重启 ZooKeeper 集群中的每个服务器使配置生效。请注意，ZooKeeper 集群重启需要小心操作，以避免服务中断。

## 3. 服务管理与监控

### 服务启停
通过 DataSophon 的服务管理界面，可以方便地启动、停止或滚动重启 ZooKeeper 服务。
*   **启动/停止**: 适用于整个集群或单个节点。
*   **滚动重启**: DataSophon 会逐个重启 ZooKeeper Server 节点，确保在重启过程中集群的可用性 (只要剩余的存活节点数仍满足 Quorum)。

### 监控指标
DataSophon 会集成 ZooKeeper 的关键监控指标，并在仪表盘中展示，帮助用户实时了解服务健康状况和性能表现。关键指标包括：
*   **集群状态**: Leader 是哪个节点，Follower 列表。
*   **节点存活状态**: 每个 ZooKeeper Server 是否在线。
*   **ZNode 数量**: 总的 ZNode 数量。
*   **Watch 数量**: 当前活动的 Watcher 数量。
*   **连接数**: 当前客户端连接总数。
*   **平均延迟 (Avg Latency)**: 处理客户端请求的平均时间。
*   **最小/最大延迟 (Min/Max Latency)**。
*   **未完成请求数 (Outstanding Requests)**: 等待处理的请求队列长度。
*   **收发数据包数量 (Packets Received/Sent)**。
*   **模式 (Mode)**: 每个节点是 Leader, Follower, 还是 Observer (如果有配置)。

### 四字命令 (Four Letter Words)
ZooKeeper 提供了一组简单的四字命令，可以通过 `nc` (netcat) 或 `telnet` 发送到 ZooKeeper 服务器的 `clientPort` 来获取状态信息。这些命令对于快速诊断和监控非常有用。DataSophon 的监控系统可能会利用这些命令收集信息。
常用四字命令包括：
*   `stat`: 输出服务器的详细状态信息，包括版本、模式、ZNode 数量、连接数、延迟等。
    ```bash
    echo stat | nc localhost 2181
    ```
*   `ruok`: 测试服务器是否正在以非错误状态运行。如果正常，服务器响应 "imok"。
    ```bash
    echo ruok | nc localhost 2181
    ```
*   `conf`: 输出服务器的基本配置信息。
    ```bash
    echo conf | nc localhost 2181
    ```
*   `cons`: 列出所有连接到此服务器的客户端的详细信息。
    ```bash
    echo cons | nc localhost 2181
    ```
*   `mntr`: (推荐使用) 输出比 `stat` 更结构化和更丰富的监控指标，适合机器解析。
    ```bash
    echo mntr | nc localhost 2181
    ```
*   `wchs`: 列出服务器上活动的 Watcher 的简要信息。
*   `wchc`: 按会话列出活动的 Watcher 的详细信息。
*   `wchp`: 按路径列出活动的 Watcher 的详细信息。

注意: 某些命令可能需要在 `zoo.cfg` 中通过 `4lw.commands.whitelist` (ZooKeeper 3.5.3+) 或默认启用。为了安全，生产环境中可以配置白名单，只允许必要的命令。

## 4. ZooKeeper CLI 使用

ZooKeeper 自带一个命令行客户端 (`zkCli.sh`)，可以用于连接到 ZooKeeper 集群并执行各种操作，如创建、读取、更新、删除 ZNode，设置 Watch 等。

### 连接到集群
在安装了 ZooKeeper 客户端的节点上执行：
```bash
zkCli.sh -server <zk_server_host>:<client_port>
# 例如:
zkCli.sh -server zkhost1:2181
# 如果 ZooKeeper 集群有多个服务器，可以指定一个列表:
zkCli.sh -server zkhost1:2181,zkhost2:2181,zkhost3:2181
```
连接成功后，会进入交互式 Shell。

### 基本操作
*   **列出子节点 (ls)**:
    ```zkcli
    ls /path/to/znode
    ls / # 列出根路径下的子节点
    ls /path/to/znode true # 同时获取该 ZNode 的 stat 信息
    ```
*   **获取 ZNode 数据 (get)**:
    ```zkcli
    get /path/to/znode
    get /path/to/znode true # 同时获取该 ZNode 的 stat 信息并注册一个 Watch
    ```
*   **创建 ZNode (create)**:
    ```zkcli
    # 创建持久节点
    create /path/to/znode "some data"
    # 创建临时节点 (-e)
    create -e /path/to/ephemeral_node "ephemeral data"
    # 创建顺序节点 (-s)
    create -s /path/to/sequential_node_ "sequential data"
    # 创建持久顺序节点 (-s)
    create -s /persistent_sequential_ "data"
    # 创建临时顺序节点 (-e -s)
    create -e -s /ephemeral_sequential_ "data"
    ```
*   **设置 ZNode 数据 (set)**:
    ```zkcli
    set /path/to/znode "new data"
    set /path/to/znode "newer data" <expected_version_number> # 带版本号的条件更新
    ```
*   **删除 ZNode (delete)**:
    ```zkcli
    # 只能删除没有子节点的 ZNode
    delete /path/to/znode
    delete /path/to/znode <expected_version_number> # 带版本号的条件删除
    ```
*   **递归删除 ZNode (deleteall / rmr - 老版本)**:
    ```zkcli
    # 新版本 (推荐)
    deleteall /path/to/znode_with_children
    # 老版本 (某些客户端可能仍使用 rmr)
    # rmr /path/to/znode_with_children
    ```
*   **获取 ZNode 元数据 (stat)**:
    ```zkcli
    stat /path/to/znode
    ```
    输出信息包括: cZxid, ctime, mZxid, mtime, pZxid, cversion, dataVersion, aclVersion, ephemeralOwner, dataLength, numChildren。

*   **退出 CLI**: `quit` 或 `exit`。

### Watch 使用示例
```zkcli
# 获取 ZNode 数据并注册 Watch
get /my_app/config true
```
当 `/my_app/config` 的数据发生变化或被删除时，客户端会收到一个通知。要持续监控，需要在收到通知后再次执行 `get /my_app/config true`。

```zkcli
# 列出子节点并注册 Watch
ls /my_app/workers true
```
当 `/my_app/workers` 的子节点列表发生变化 (有新的 worker 加入或离开) 时，客户端会收到通知。

## 5. 备份与恢复

ZooKeeper 的数据主要包括：
1.  **数据快照 (Snapshots)**: 存储在 `dataDir` 目录下，文件名类似 `snapshot.<zxid>`。
2.  **事务日志 (Transaction Logs / WAL)**: 存储在 `dataLogDir` (如果配置了) 或 `dataDir/version-2` 目录下，文件名类似 `log.<zxid>`。

### 备份策略
*   **定期备份 `dataDir` 和 `dataLogDir`**: 这是最直接的方法。可以使用文件系统级别的备份工具 (如 `rsync`, `tar`) 定期将这两个目录的内容完整备份到安全的存储位置。
*   **确保一致性**: 在备份之前，最好能确保 ZooKeeper 服务处于稳定状态，或者如果条件允许，可以短暂停止单个节点进行备份 (如果其他节点能维持 Quorum)。但通常在线备份是可行的，因为 ZooKeeper 本身设计有容错性。
*   **备份频率**: 取决于数据的重要性和变更频率。

### 恢复策略
恢复 ZooKeeper 集群通常是在灾难性故障 (例如，多数节点数据损坏或丢失) 的情况下进行。
1.  **停止所有 ZooKeeper 服务节点**。
2.  **清理所有节点的 `dataDir` 和 `dataLogDir` 中的内容** (或将其移动到备份位置)。
3.  **选择一个一致的备份集**: 从备份中选择最近的一个完整、一致的快照和事务日志备份集。
4.  **恢复数据到每个节点**: 将选定的备份数据恢复到每个 ZooKeeper 节点的 `dataDir` 和 (如果分离的) `dataLogDir`。
    *   **重要**: 确保所有要恢复的节点都使用来自同一时间点的同一份备份数据。数据不一致会导致集群无法正常启动或形成 Quorum。
5.  **检查 `myid` 文件**: 确保每个节点的 `dataDir/myid` 文件内容正确且唯一。
6.  **启动 ZooKeeper 服务节点**: 逐个启动或全部启动。集群会自动进行 Leader 选举并恢复服务。

**注意**: ZooKeeper 本身的高可用设计旨在通过冗余来避免单点故障。上述备份和恢复更多是针对整个集群数据丢失或损坏的极端情况。在正常运维中，如果少数节点故障，集群通常能自动恢复或通过替换故障节点来解决。

## 6. 安全配置

### 网络隔离
*   将 ZooKeeper 集群部署在受信任的网络环境中，使用防火墙限制对 ZooKeeper 服务器端口 (clientPort, peerPort, electionPort) 的访问，只允许必要的应用服务器和集群内部节点访问。

### 禁用不必要的四字命令
*   在 `zoo.cfg` 中配置 `4lw.commands.whitelist` 来明确指定允许执行的四字命令，例如：
    ```properties
    4lw.commands.whitelist=stat, ruok, conf, mntr
    ```
    或者，完全禁用（不推荐，因为某些监控依赖它们）：
    ```properties
    4lw.commands.whitelist=
    ```

### SASL 认证
ZooKeeper 支持通过 SASL (Simple Authentication and Security Layer) 实现客户端和服务端之间的强认证。常用的机制包括 Kerberos 和 DIGEST-MD5。
*   **客户端认证**: 确保只有经过认证的客户端才能连接和操作 ZooKeeper。
*   **服务端认证 (集群内部)**: 确保只有合法的 ZooKeeper 服务器才能加入集群并参与选举和数据同步。

配置 SASL 比较复杂，涉及 Java Security Manager, JAAS 配置文件, Kerberos keytabs (如果使用 Kerberos) 等。具体步骤请参考 Apache ZooKeeper 官方文档关于 SASL认证的部分。
DataSophon 部署时如果集成了 Kerberos 环境，可能会自动处理 ZooKeeper 的 Kerberos 相关配置。

### ACL 控制
即使客户端通过了认证，也应使用 ACL 来细粒度控制其对 ZNode 的访问权限。
*   默认情况下，新创建的 ZNode 具有 world:anyone:cdrwa (任何人都有所有权限) 的 ACL。
*   在生产环境中，应为重要的 ZNode 设置更严格的 ACL，例如只允许特定的用户或 IP 地址进行读写。
    ```zkcli
    # 示例: 仅允许特定 digest 用户读写
    # 首先需要添加认证用户
    # addauth digest myuser:mypassword
    # create /secure_node "data" auth:myuser:mypassword:cdrwa
    # setAcl /secure_node auth:myuser:mypassword:cdrwa
    ```

## 7. 故障排查

### 集群无法选举出 Leader / 无法形成 Quorum
*   **检查网络连接**: 确保所有 ZooKeeper Server 节点之间的 `peerPort` 和 `electionPort` 是互通的。
*   **检查 `myid` 文件**: 确保每个节点的 `dataDir/myid` 文件存在，内容正确且在集群中唯一。
*   **检查 `zoo.cfg` 中 `server.X` 配置**: 确保所有节点上的 `server.X` 配置一致，且指向正确的主机名/IP和端口。
*   **检查存活节点数**: 确保至少有 (N/2 + 1) 个节点正常运行 (N 是集群总节点数)。
*   **查看日志**: ZooKeeper 服务器的日志 (通常是 `zookeeper.out` 或系统日志) 会包含选举过程的详细信息和可能的错误。

### 客户端无法连接到 ZooKeeper
*   检查客户端配置的 ZooKeeper 服务器地址和 `clientPort` 是否正确。
*   检查网络防火墙是否允许从客户端到 ZooKeeper 服务器 `clientPort` 的连接。
*   检查 ZooKeeper 服务是否在服务器上正常运行。
*   检查是否达到了 `maxClientCnxns` 限制。

### 数据不一致或旧数据问题
*   ZooKeeper 正常情况下保证强一致性。如果出现数据不一致，可能是由于：
    *   客户端连接到了一个与集群网络分区且数据陈旧的 ZooKeeper 服务器 (极端情况)。
    *   严重的时钟不同步问题 (ZooKeeper 对时间戳敏感)。
*   检查所有节点的系统时间和 NTP 同步状态。

### 性能问题 (高延迟、低吞吐量)
*   **磁盘 I/O 瓶颈**: 特别是事务日志的写入。考虑使用 SSD 或将 `dataLogDir` 放到独立的快速磁盘上。
*   **CPU 瓶颈**: 大量客户端连接或复杂的 Watch 处理可能消耗 CPU。
*   **网络瓶颈**。
*   **JVM GC 问题**: 监控 ZooKeeper Server 的 GC 活动，调整 JVM 参数 (如堆大小) 以避免长时间的 GC 暂停。
*   **Watch 数量过多**: 大量的 Watch 会增加服务器的负担。优化应用逻辑，避免不必要的 Watch。
*   **请求过于频繁或数据量过大**: 单个 ZNode 数据不宜过大。

通过本指南，您应该能够更好地在 DataSophon 平台上管理和使用 ZooKeeper 服务。ZooKeeper 是一个稳定且成熟的系统，但正确的配置和监控对于其可靠运行至关重要。对于更高级的配置和问题排查，请参考 Apache ZooKeeper 官方文档。 