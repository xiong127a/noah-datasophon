# 元数据目录

此目录用于存放系统运行所需的元数据文件，包括检查项配置、服务定义等。

## 目录结构

元数据目录应包含以下结构：

```
meta/
└── DDP-1.2.1/
    └── services/
        └── [service_name]/
            └── service_ddl.json
```

## 版本说明

目前系统支持的版本为`DDP-1.2.1`，可在`checker-config.yml`中配置。

## 开发环境配置

在IDEA开发环境中，系统会自动搜索以下路径查找元数据文件：

1. `项目根目录/datasophon-api/src/main/resources/meta`
2. `项目根目录/datasophon-service/src/main/resources/meta`
3. `项目根目录/src/main/resources/meta`
4. `项目根目录/conf/meta`
5. `项目根目录/meta`

如果找不到对应的元数据目录，系统将使用默认的用户组映射配置。

## 手动设置

可以通过设置环境变量`META_BASE_DIR`来指定元数据目录的位置：

```bash
# Linux/Mac
export META_BASE_DIR=/path/to/your/meta/directory

# Windows
set META_BASE_DIR=E:\path\to\your\meta\directory
``` 