# DataSophon 简化架构方案

## 🎯 **简化原因**

原方案将common拆分成3个模块过于复杂：
- ❌ `datasophon-common-core` + `datasophon-common-domain` + `datasophon-common-dto`
- ❌ 模块太多，管理复杂
- ❌ 依赖链过长，开发体验差

## 💡 **简化后的模块结构**

### **推荐方案：两个核心模块**

```
datasophon (父项目)
├── datasophon-common           # 公共模块（工具类、常量、DTO、异常）
├── datasophon-domain          # 领域模型（数据库实体）
├── datasophon-dao             # 数据访问层
├── datasophon-service         # 业务逻辑层
├── datasophon-api             # Web接口层（统一返回类）
├── datasophon-worker          # 独立工作节点
├── datasophon-kubernetes      # K8s业务模块
└── datasophon-ui              # 前端模块
```

### **模块职责重新划分**

#### 📦 **datasophon-common**
```java
com.datasophon.common
├── constants/        # 系统常量
├── utils/           # 工具类（PropertyUtils、IOUtils等）
├── enums/           # 枚举类型
├── exceptions/      # 基础异常类
├── dto/            # 数据传输对象
│   ├── request/    # 请求DTO
│   ├── response/   # 响应DTO
│   └── query/      # 查询DTO
├── annotations/    # 通用注解
└── cache/          # 缓存相关
```
**职责**：通用工具、DTO、常量、枚举等，被所有模块依赖

#### 📦 **datasophon-domain**
```java
com.datasophon.domain
├── entity/         # 数据库实体类
├── vo/            # 值对象
└── command/       # 命令对象
```
**职责**：数据库实体和领域模型，只被dao/service依赖

## 🔗 **简化的依赖关系**

```
datasophon-api
    ↓
datasophon-service ←→ datasophon-kubernetes
    ↓
datasophon-dao
    ↓
datasophon-domain

datasophon-worker → datasophon-common + datasophon-domain

所有模块 → datasophon-common
```

## 📋 **对比分析**

### **复杂方案 vs 简化方案**

| 对比项目 | 复杂方案 | 简化方案 | 
|---------|---------|---------|
| 模块数量 | 3个common模块 | 1个common + 1个domain |
| 依赖复杂度 | core→domain→dto | common, domain独立 |
| 开发体验 | 需要跨3个模块 | 主要在2个模块 |
| 维护成本 | 高 | 低 |
| 理解难度 | 高 | 低 |

### **简化方案优势**

✅ **降低复杂度**：模块数量减少，依赖关系清晰
✅ **提升开发效率**：修改DTO不需要跨太多模块  
✅ **保持核心原则**：统一返回类仍在API层，实体类独立管理
✅ **便于维护**：模块职责清晰但不过度拆分

## 🔧 **迁移建议**

### **第一步：合并common模块**
```bash
# 将现有的3个模块内容合并到 datasophon-common
datasophon-common-core/src/main/java/* → datasophon-common/src/main/java/
datasophon-common-dto/src/main/java/*  → datasophon-common/src/main/java/
```

### **第二步：重命名domain模块**
```bash
# 将 datasophon-common-domain 重命名为 datasophon-domain
mv datasophon-common-domain datasophon-domain
```

### **第三步：更新依赖**
```xml
<!-- 各模块pom.xml只依赖两个模块 -->
<dependency>
    <groupId>com.datasophon</groupId>
    <artifactId>datasophon-common</artifactId>
</dependency>
<dependency>
    <groupId>com.datasophon</groupId>
    <artifactId>datasophon-domain</artifactId>
</dependency>
```

## 🎯 **核心规范不变**

即使简化了模块结构，核心规范依然保持：

1. ✅ **统一返回类只在API层**：`Result<T>`仍在`datasophon-api/vo/`
2. ✅ **实体类独立管理**：Entity在`datasophon-domain`中
3. ✅ **分层架构清晰**：API→Service→DAO→Domain
4. ✅ **Worker独立运行**：最小依赖原则
5. ✅ **K8s模块独立**：被Service调用，不被其他模块依赖

## 💡 **总结**

**简化不是妥协，而是更好的设计！**

- 🎯 保持了核心架构原则
- 🔧 降低了实施复杂度  
- 🚀 提升了开发效率
- 📈 便于后续维护和扩展

**建议采用简化方案：`datasophon-common` + `datasophon-domain`**