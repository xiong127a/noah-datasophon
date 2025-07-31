# DataSophon 架构重构规范方案

## 🎯 重构目标
- 实现严格的分层架构
- 明确各模块职责边界  
- 统一返回类只在API层
- 规范依赖关系

## 📁 新模块结构

### 1. datasophon-common-core (核心工具模块)
```
com.datasophon.common.core
├── constants/          # 常量定义
├── enums/             # 枚举类型
├── utils/             # 工具类(不含业务逻辑)
├── exceptions/        # 基础异常类
└── annotations/       # 通用注解
```
**职责**: 纯粹的工具类和常量，无业务逻辑，可被所有模块依赖

### 2. datasophon-common-domain (领域模型模块)  
```
com.datasophon.common.domain
├── entity/            # 数据库实体类
├── vo/               # 值对象
└── command/          # 命令对象
```
**职责**: 领域模型，数据库实体类，只能被dao/service层依赖

### 3. datasophon-common-dto (数据传输对象模块)
```
com.datasophon.common.dto
├── request/          # 请求DTO
├── response/         # 响应DTO(不含统一返回类)
└── query/           # 查询DTO
```
**职责**: 跨模块数据传输，service层与其他层交互使用

### 4. datasophon-dao (数据访问层)
```
com.datasophon.dao
├── mapper/           # MyBatis Mapper接口
└── config/          # 数据源配置
```
**依赖**: common-core + common-domain
**职责**: 纯数据访问，不包含业务逻辑

### 5. datasophon-service (业务逻辑层)
```
com.datasophon.service
├── impl/            # Service实现类
├── strategy/        # 策略模式相关
├── checker/         # 检查器相关
├── load/           # 负载相关
├── master/         # 主节点相关
├── alert/          # 告警相关
└── kubernetes/     # K8s业务逻辑
```
**依赖**: dao + common-core + common-domain + common-dto
**职责**: 业务逻辑处理，返回DTO对象

### 6. datasophon-api (Web接口层)
```
com.datasophon.api
├── controller/      # REST控制器
├── dto/            # API专用DTO
├── vo/             # 统一返回类(Result,PageResult等)
├── interceptor/    # 拦截器
├── config/         # Web配置
├── security/       # 安全配置
└── exceptions/     # API异常处理
```
**依赖**: service + common-core + common-dto
**职责**: HTTP接口，参数校验，统一返回格式

### 7. datasophon-worker (独立工作节点)
```
com.datasophon.worker
├── actor/          # Akka Actor
├── handler/        # 任务处理器
└── executor/       # 执行器
```
**依赖**: 仅 common-core + common-domain(entity)
**职责**: 独立运行的工作节点

### 8. datasophon-kubernetes (K8s业务模块)
```
com.datasophon.kubernetes
├── client/         # K8s客户端
├── resource/       # K8s资源定义
└── operator/       # K8s操作器
```
**依赖**: common-core + common-dto
**职责**: K8s独立业务逻辑，被service层调用，不被其他模块依赖

## 🔗 严格的依赖层次

```
datasophon-api
    ↓
datasophon-service ←→ datasophon-kubernetes (service调用k8s)
    ↓
datasophon-dao
    ↓
datasophon-common-domain

datasophon-worker → datasophon-common-core (独立运行)

所有模块 → datasophon-common-core (工具类)

注意：kubernetes模块不被其他模块依赖，只被service调用
```

## 📋 具体改造任务

### 阶段1: 模块拆分
1. ✅ 创建 datasophon-common-core 模块
2. ✅ 创建 datasophon-common-domain 模块  
3. ✅ 创建 datasophon-common-dto 模块
4. ✅ 迁移现有 common 模块内容

### 阶段2: 统一返回类迁移
1. ✅ 移动 Result.java 到 datasophon-api/vo/
2. ✅ 移动 PageResult.java 到 datasophon-api/vo/
3. ✅ 更新所有引用

### 阶段3: 依赖关系调整  
1. ✅ 调整模块间pom依赖
2. ✅ 修复循环依赖
3. ✅ 验证编译通过

### 阶段4: 包结构重组
1. ✅ 按新规范重组包结构
2. ✅ 更新import语句
3. ✅ 验证功能正常

## 📏 规范约定

### 命名规范
- Controller: xxxController
- Service接口: xxxService  
- Service实现: xxxServiceImpl
- DTO: xxxDto/xxxRequest/xxxResponse
- VO: xxxVo (仅API层使用)
- Entity: 直接使用业务名称

### 依赖原则
1. **上层可依赖下层，下层不可依赖上层**
2. **API层统一返回格式，其他层返回DTO**
3. **Worker独立运行，最小依赖**
4. **Common-core无业务逻辑，纯工具类**

### 数据流向
```
Request → Controller → Service → Dao → Database
Response ← Result<DTO> ← DTO ← Entity ←
```

## 🚦 重构检查清单

- [ ] 模块依赖层次正确
- [ ] 统一返回类仅在API层
- [ ] Entity类仅在domain层
- [ ] DTO在service层间传输
- [ ] 无循环依赖
- [ ] Worker模块独立
- [ ] 版本统一管理
- [ ] 编译测试通过

## 📈 改造收益

1. **职责明确**: 每个模块职责单一清晰
2. **依赖清晰**: 严格的分层架构
3. **可维护**: 便于理解和维护  
4. **可扩展**: 易于添加新功能
5. **可测试**: 模块间解耦便于测试