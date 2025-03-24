# 防火墙检查器模块

## 概述

防火墙检查器模块负责检测和修复各种Linux发行版上的防火墙配置，支持多种防火墙类型，包括：

- firewalld (CentOS 7/8, RHEL 7/8, Kylin)
- ufw (Ubuntu)
- iptables (传统Linux发行版)

## 设计思路

该模块采用了工厂模式设计，基于操作系统的类型返回适合的防火墙检查器实现。主要类及其职责如下：

1. `FirewallChecker`: 基础防火墙检查器类，提供通用的防火墙检测和修复能力。
2. `CentOSFirewallChecker`: 特定于CentOS的防火墙检查器实现，继承自FirewallChecker。
3. `FirewallCheckerFactory`: 防火墙检查器工厂类，根据操作系统类型创建适合的检查器实例。

## 支持的操作系统

- CentOS 7/8
- Ubuntu 22.04/24.04
- Kylin V4/V10
- 其他Linux发行版（通用实现）

## 使用方法

```java
// 获取操作系统信息
OsInfo osInfo = getOsInfo(hostInfo);

// 通过工厂获取适合当前操作系统的防火墙检查器
FirewallCheckerFactory factory = new FirewallCheckerFactory();
FirewallChecker checker = factory.getChecker(osInfo);

// 执行检查
CheckItem checkItem = new CheckItem();
checkItem = checker.doCheck(hostInfo, checkItem);

// 如果检查失败，执行修复
if (checkItem.getStatus() == CheckItem.Status.FAILED) {
    boolean success = checker.doFix(hostInfo, checkItem);
    // 处理修复结果
}
```

## 注意事项

1. 防火墙检查器会根据操作系统类型自动检测并使用适合的防火墙管理工具。
2. 对于未明确支持的操作系统，会使用通用的检测和修复逻辑。
3. 提供详细的检查和修复日志，便于问题诊断。
4. 对于不同类型的防火墙，修复策略统一为：停止服务、禁用自启动、清空规则。

## 扩展方式

如需添加对新的操作系统或防火墙类型的支持，可按照以下步骤进行：

1. 创建新的防火墙检查器类，继承自FirewallChecker。
2. 实现特定于该操作系统的检查和修复方法。
3. 在FirewallCheckerFactory中添加对新操作系统的判断和实例创建逻辑。

## 更新记录

- 2025-03-20: 首次实现，支持CentOS、Ubuntu和Kylin系统的防火墙检查和修复。
- 2025-03-21: 添加对iptables的详细规则检查和清空功能。
- 2025-03-24: 优化防火墙类型自动检测算法，提高兼容性。 