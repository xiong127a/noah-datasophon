# 模板管理说明

## 概述

为了方便管理模板文件，我们将模板文件统一放在API服务端管理，Worker端通过Akka Actor获取模板内容。

## 目录结构

```
datasophon-api/src/main/resources/worker/templates/ - 存放所有模板文件
```

## 通信方式

系统使用Akka Actor模式进行通信：

1. Master端提供`TemplateServiceActor`，负责响应模板请求
2. Worker端通过Akka远程调用获取模板内容

## Worker端使用

Worker端需要在配置文件中设置如下参数：

```properties
# 是否从Akka获取模板文件
template.from.akka=true
```

## 添加新模板

1. 将新模板文件放到`datasophon-api/src/main/resources/worker/templates/`目录下
2. 重启API服务

## 备注

如果从Akka获取模板失败，系统会自动回退到本地模板文件。 