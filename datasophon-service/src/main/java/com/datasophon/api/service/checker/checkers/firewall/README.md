# 防火墙检查器分包结构

## 整体结构

```
com.datasophon.api.service.checker.checkers.firewall/
├── FirewallChecker.java                 # 主检查器类
├── FirewallCheckerStrategy.java         # 策略接口
│
├── factory/
│   └── FirewallCheckerFactory.java      # 工厂类
│
├── generic/
│   └── GenericFirewallChecker.java      # 通用实现
│
├── os/
│   ├── centos/
│   │   ├── CentOSFirewallChecker.java     # CentOS基类
│   │   ├── CentOS7FirewallChecker.java    # CentOS 7实现
│   │   └── CentOS8FirewallChecker.java    # CentOS 8实现
│   │
│   ├── ubuntu/
│   │   ├── UbuntuFirewallChecker.java     # Ubuntu基类
│   │   ├── Ubuntu22FirewallChecker.java   # Ubuntu 22实现
│   │   └── Ubuntu24FirewallChecker.java   # Ubuntu 24实现
│   │
│   └── kylin/
│       ├── KylinFirewallChecker.java      # Kylin基类
│       ├── KylinV4FirewallChecker.java    # Kylin V4实现
│       └── KylinV10FirewallChecker.java   # Kylin V10实现
```

## 类职责

### 核心类

1. **FirewallChecker** - 防火墙检查器基类，提供基本的检查和修复方法
2. **FirewallCheckerStrategy** - 策略接口，定义防火墙检查和修复的标准方法
3. **FirewallCheckerFactory** - 工厂类，根据操作系统类型创建适合的检查器实例

### 通用实现

1. **GenericFirewallChecker** - 通用防火墙检查器，实现FirewallCheckerStrategy接口，提供基本的检查逻辑

### 操作系统特定实现

#### CentOS系列

1. **CentOSFirewallChecker** - CentOS基类，提供CentOS通用的防火墙检查和修复方法
2. **CentOS7FirewallChecker** - CentOS 7专用实现，处理CentOS 7特有的防火墙配置
3. **CentOS8FirewallChecker** - CentOS 8专用实现，处理CentOS 8特有的防火墙配置

#### Ubuntu系列

1. **UbuntuFirewallChecker** - Ubuntu基类，提供Ubuntu通用的防火墙检查和修复方法
2. **Ubuntu22FirewallChecker** - Ubuntu 22专用实现，处理Ubuntu 22.04特有的防火墙配置
3. **Ubuntu24FirewallChecker** - Ubuntu 24专用实现，处理Ubuntu 24.04特有的防火墙配置

#### Kylin系列

1. **KylinFirewallChecker** - Kylin基类，提供Kylin通用的防火墙检查和修复方法
2. **KylinV4FirewallChecker** - Kylin V4专用实现，处理Kylin V4特有的防火墙配置
3. **KylinV10FirewallChecker** - Kylin V10专用实现，处理Kylin V10特有的防火墙配置

## 主要功能

每个防火墙检查器将实现以下功能：

1. **检测防火墙类型** - 确定系统使用的防火墙类型（firewalld、ufw、iptables等）
2. **检查防火墙状态** - 检查防火墙是否启用，如启用则标记为失败
3. **检查自启动状态** - 检查防火墙是否设置为开机自启动
4. **修复防火墙配置** - 停止防火墙服务并禁用自启动
5. **验证修复结果** - 确认防火墙已被正确禁用

## 操作系统特点

### CentOS 7/8
- 主要使用firewalld
- 通过systemctl管理服务
- CentOS 7: 可能同时存在iptables和firewalld
- CentOS 8: 主要使用firewalld，使用nftables作为后端

### Ubuntu 22.04/24.04
- 主要使用ufw (Uncomplicated Firewall)
- 通过systemctl管理服务
- Ubuntu 22.04: 基于iptables的ufw
- Ubuntu 24.04: 可能已迁移到nftables后端

### Kylin V4/V10
- Kylin V4 (基于Debian): 可能使用ufw
- Kylin V10 (基于CentOS): 可能使用firewalld
- 需要同时检查iptables和firewalld 