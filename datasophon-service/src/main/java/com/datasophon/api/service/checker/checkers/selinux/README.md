# SELinux检查器

SELinux检查器用于检查和修复Linux系统上的SELinux配置，确保SELinux处于禁用或宽容模式，避免SELinux影响大数据平台的正常运行。

## 支持的操作系统

- **CentOS**: 7.x, 8.x
- **Ubuntu**: 22.04, 24.04
- **Kylin**: V4, V10
- 其他Linux发行版

## 架构设计

SELinux检查器采用工厂模式和策略模式设计，核心组件如下：

### 核心类

- **SELinuxChecker**: 基础检查器类，实现AbstractItemChecker接口，提供基本的检查和修复功能
- **SELinuxCheckerStrategy**: 策略接口，定义SELinux检查和修复的标准方法
- **SELinuxCheckerFactory**: 工厂类，根据操作系统类型创建对应的SELinux检查器实例

### 具体实现类

- **GenericSELinuxChecker**: 通用SELinux检查器实现，作为各操作系统特定实现的基类
- **CentOSSELinuxChecker**: CentOS系统专用的SELinux检查器实现
- **UbuntuSELinuxChecker**: Ubuntu系统专用的SELinux检查器实现
- **KylinSELinuxChecker**: 麒麟操作系统专用的SELinux检查器实现

## 检查逻辑

1. 根据操作系统类型选择合适的检查器
2. 检查SELinux状态:
   - 对于CentOS/RHEL：使用`sestatus`和`getenforce`命令
   - 对于Ubuntu：先检查是否安装SELinux，再检查模块是否加载
   - 对于Kylin：根据是基于CentOS还是Ubuntu选择相应的检查方法
3. 检查SELinux配置文件`/etc/selinux/config`中的设置
4. 根据检查结果决定是否需要修复:
   - SELinux状态为"disabled"或模式为"permissive"则检查通过
   - 否则检查失败，需要修复

## 修复逻辑

1. 临时设置SELinux为宽容模式:
   - CentOS/RHEL: `setenforce 0`
   - Ubuntu: `echo 0 > /sys/fs/selinux/enforce`（如果文件存在）
2. 永久禁用SELinux:
   - 修改配置文件`/etc/selinux/config`，设置`SELINUX=disabled`
   - 对于Ubuntu系统，还会修改GRUB启动参数，添加`selinux=0`
3. 提示用户需要重启系统才能完全应用永久设置

## 使用方法

SELinux检查器会在系统初始化检查时自动执行，也可以通过API手动触发检查和修复：

```java
// 获取SELinux检查器
SELinuxChecker selinuxChecker = applicationContext.getBean(SELinuxChecker.class);

// 执行检查
CheckItem checkResult = selinuxChecker.check(hostInfo, checkItem);

// 执行修复
boolean fixResult = selinuxChecker.fix(hostInfo, checkItem);
```

## 日志记录

SELinux检查器会将所有的检查和修复操作记录在系统日志和缓存日志中，方便问题排查：

- 系统日志：使用SLF4J记录到系统日志文件
- 缓存日志：使用CheckLogger记录到内存缓存，用于生成实时报告

## 注意事项

1. SELinux修复需要重启系统才能完全生效
2. Ubuntu系统默认使用AppArmor而非SELinux，如果没有安装SELinux，检查会直接通过
3. 麒麟V4基于CentOS，麒麟V10基于Ubuntu，检查方法会根据版本自动选择 