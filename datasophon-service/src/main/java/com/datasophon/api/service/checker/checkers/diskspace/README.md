# 磁盘空间检查器模块

## 概述

磁盘空间检查器模块负责检测主机系统的磁盘空间使用情况，并在磁盘空间不足时发出警告。该模块支持多种Linux发行版，能够识别不同的文件系统和挂载点，过滤掉不需要检查的系统分区，并针对不同的操作系统提供专门的清理建议。

## 设计思路

该模块采用了工厂模式设计，基于操作系统的类型返回适合的磁盘空间检查器实现。主要类及其职责如下：

1. `DiskSpaceChecker`: 基础磁盘空间检查器类，提供通用的磁盘空间检测和建议生成功能。
2. `CentOSDiskSpaceChecker`: 特定于CentOS的磁盘空间检查器实现，继承自DiskSpaceChecker。
3. `DiskSpaceCheckerFactory`: 磁盘空间检查器工厂类，根据操作系统类型创建适合的检查器实例。

## 支持的操作系统

- CentOS/RHEL
- Ubuntu/Debian
- Kylin
- 其他Linux发行版（通用实现）

## 检查逻辑

磁盘空间检查器执行以下检查：

1. 获取所有磁盘分区的使用情况
2. 过滤掉不需要监控的文件系统类型（如tmpfs, devtmpfs等）
3. 过滤掉不需要监控的挂载点（如/proc, /sys等）
4. 检查剩余分区的使用率和可用空间
   - 使用率超过80%发出警告
   - 使用率超过90%视为严重问题
   - 大型分区（>20GB）的可用空间小于10GB也会发出警告
5. 针对不同操作系统执行特定检查
   - CentOS: 检查YUM缓存、RPM数据库大小、旧内核等
   - Ubuntu: 检查APT缓存、日志文件等

## 建议生成逻辑

当磁盘空间不足时，检查器**不会执行任何自动清理操作**，而是提供以下清理建议：

1. 识别并报告大文件和占用空间较大的目录
2. 提供针对不同操作系统的具体清理命令建议：
   - CentOS: `yum clean all`、清理旧内核等
   - Ubuntu: `apt-get clean`、移除未使用的软件包等
3. 提供通用清理建议，如清理日志文件、临时文件等
4. 所有建议都通过warn级别日志输出，确保管理员能够注意到

## 使用方法

```java
// 获取操作系统信息
OsInfo osInfo = getOsInfo(hostInfo);

// 通过工厂获取适合当前操作系统的磁盘空间检查器
DiskSpaceCheckerFactory factory = new DiskSpaceCheckerFactory();
DiskSpaceChecker checker = factory.getChecker(osInfo);

// 执行检查
CheckItem checkItem = new CheckItem();
checkItem = checker.doCheck(hostInfo, checkItem);

// 注意：doFix方法不会执行实际清理操作，只会生成清理建议
```

## 配置选项

磁盘空间检查器提供以下可配置选项：

- `WARNING_THRESHOLD`: 警告阈值，默认80%
- `CRITICAL_THRESHOLD`: 严重阈值，默认90%
- `MIN_FREE_SPACE_GB`: 最小要求可用空间，默认10GB
- `IGNORED_FS_TYPES`: 要忽略的文件系统类型列表
- `IGNORED_MOUNT_POINTS`: 要忽略的挂载点列表

## 扩展方式

如需添加对新的操作系统的支持，可按照以下步骤进行：

1. 创建新的磁盘空间检查器类，继承自DiskSpaceChecker
2. 实现特定于该操作系统的检查和建议生成方法
3. 在DiskSpaceCheckerFactory中添加对新操作系统的判断和实例创建逻辑

## 注意事项

1. 磁盘空间检查器**不会执行任何自动清理操作**，所有清理操作必须由管理员手动执行
2. 检查器会提供尽可能详细的建议，包括具体的文件路径和清理命令
3. 所有清理建议都通过warn级别日志输出，确保管理员能够注意到 