# Kubernetes模块架构重构说明

## 🏗️ 重构目标

移除Kubernetes模块对DAO层的直接依赖，实现模块间的解耦，符合分层架构设计原则。

## 📊 重构前后对比

### ❌ 重构前的问题
```java
// KubeUtil.java 直接依赖DAO层
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.enums.HostState;
import com.datasophon.dao.enums.MANAGED;

// 直接返回DAO实体
public static List<ClusterHostDO> getHostListByConfig(String kubeConfig)
```

**问题**：
- 违反分层架构原则
- kubernetes模块与dao模块耦合
- 模块职责不清晰

### ✅ 重构后的架构

```
datasophon-kubernetes (基础设施层)
├── model/
│   ├── K8sNodeInfo.java       # K8S节点信息模型
│   └── K8sResourceInfo.java   # K8S资源信息模型
└── util/
    └── KubeUtil.java          # 返回K8S领域模型

datasophon-service (业务逻辑层)
├── converter/
│   └── K8sToClusterHostConverter.java  # K8S模型→DAO实体转换器
└── 使用转换器进行数据转换

datasophon-dao (数据访问层)
└── entity/
    └── ClusterHostDO.java     # 数据库实体
```

## 🎯 核心设计原则

### 1. 单一职责原则
- **kubernetes模块**: 只负责与K8S API交互，使用自己的领域模型
- **service模块**: 负责业务逻辑和数据转换
- **dao模块**: 只负责数据库访问

### 2. 依赖倒置原则
- kubernetes模块不依赖任何其他业务模块
- service模块协调kubernetes模块和dao模块
- 通过转换器实现模型间的转换

### 3. 开放封闭原则
- K8S领域模型可独立演进
- 新增K8S功能不影响其他模块
- 转换逻辑集中在转换器中，易于维护

## 📋 新增文件说明

### K8sNodeInfo.java
```java
@Data
@Builder
public class K8sNodeInfo {
    private String ip;
    private String hostname;
    private Integer coreNum;
    private Integer totalMem;
    private String status;
    private String cpuArchitecture;
    // ... 其他K8S特有字段
}
```
- K8S节点的领域模型
- 包含K8S特有的信息（如节点状态、可分配资源等）
- 独立于数据库实体设计

### K8sResourceInfo.java
```java
@Data
@Builder
public class K8sResourceInfo {
    private String resourceName;
    private Long capacity;
    private Long allocatable;
    private Long used;
    // ... 资源相关字段
}
```
- K8S资源信息模型
- 用于表示CPU、内存、存储等资源

### K8sToClusterHostConverter.java
```java
@Component
public class K8sToClusterHostConverter {
    public ClusterHostDO convertToClusterHost(K8sNodeInfo k8sNodeInfo, Integer clusterId) {
        return ClusterHostDO.builder()
                .clusterId(clusterId)
                .ip(k8sNodeInfo.getIp())
                .hostname(k8sNodeInfo.getHostname())
                .hostState(convertToHostState(k8sNodeInfo.getStatus()))
                .managed(MANAGED.YES)
                .build();
    }
}
```
- 负责K8S领域模型到DAO实体的转换
- 处理K8S状态到业务状态的映射
- 设置业务默认值（如机架、节点标签等）

## 🔄 使用方式

### 重构前
```java
// 直接返回DAO实体
List<ClusterHostDO> hosts = KubeUtil.getHostListByConfig(kubeConfig);
```

### 重构后
```java
// 1. 获取K8S领域模型
List<K8sNodeInfo> k8sNodes = KubeUtil.getHostListByConfig(kubeConfig);

// 2. 转换为DAO实体
List<ClusterHostDO> hosts = k8sToClusterHostConverter
    .convertToClusterHostList(k8sNodes, clusterId);
```

## 🎉 重构收益

### 1. 架构清晰
- 每个模块职责明确
- 依赖关系清晰，符合分层原则
- 模块间解耦，便于独立演进

### 2. 可维护性提升
- K8S相关逻辑集中在kubernetes模块
- 转换逻辑统一在转换器中
- 业务逻辑与基础设施分离

### 3. 可扩展性增强
- 新增K8S功能不影响其他模块
- 转换逻辑可灵活调整
- 支持多种K8S集群类型

### 4. 可测试性改善
- 各模块可独立测试
- Mock容易实现
- 单元测试更精确

## 🔄 KubernetesUtil重构完成

### ✅ 已解决的架构问题

#### **移除DAO依赖**
```java
// ❌ 重构前 - 直接依赖DAO层
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterInfoMapper;

public static ExecResult exec(ClusterServiceRoleInstanceEntity entity, ...) {
    // 直接使用DAO实体
}

// ✅ 重构后 - 使用K8S专用模型
import com.datasophon.kubernetes.model.K8sServiceRoleInfo;

public static ExecResult exec(K8sServiceRoleInfo serviceRoleInfo, ...) {
    // 使用K8S领域模型
}
```

#### **创建了专用数据模型**
- `K8sServiceRoleInfo` - 服务角色信息模型
- `ServiceRoleToK8sConverter` - Service层转换器

#### **修改了调用方式**
```java
// Service层使用转换器
ServiceRoleToK8sConverter converter = SpringUtil.getBean(ServiceRoleToK8sConverter.class);
K8sServiceRoleInfo k8sInfo = converter.convertToK8sServiceRoleInfo(entity, clusterInfo);
ExecResult result = KubernetesUtil.exec(k8sInfo, kubeConfig, cmdCommand);
```

### ⚠️ 待迁移的使用点

项目中仍有多处使用已删除的`KubernetesUtil.getKubernetesNamespace()`方法，建议后续迁移到`ClusterInfoUtils.getKubernetesNamespace()`。

## 🔧 后续优化建议

1. **完成namespace方法迁移**: 将所有`KubernetesUtil.getKubernetesNamespace()`替换为`ClusterInfoUtils.getKubernetesNamespace()`
2. **缓存优化**: 考虑在service层添加K8S节点信息缓存
3. **异常处理**: 完善K8S连接异常的处理机制  
4. **监控指标**: 添加K8S模块的监控和指标收集
5. **配置管理**: 支持多K8S集群配置管理