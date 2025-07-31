# Kubernetes 模块架构指南

## 🎯 **Kubernetes 模块定位**

`datasophon-kubernetes` 是一个**独立的业务模块**，有以下特点：

### ✅ **正确的使用方式**
- **被Service层调用**：只有 `datasophon-service` 可以调用K8s模块
- **处理K8s业务**：专门处理Kubernetes相关的业务逻辑
- **运行在项目中**：不是独立部署的模块，和其他模块一起运行
- **业务独立性**：K8s业务逻辑与其他业务隔离

### ❌ **错误的使用方式**
- ~~API层直接调用K8s模块~~
- ~~其他模块依赖K8s模块~~
- ~~K8s模块直接依赖DAO层~~

## 🏗️ **架构设计**

```
┌─────────────────┐
│  datasophon-api │ (不直接依赖K8s)
└─────────┬───────┘
          ↓
┌─────────┴───────┐    ┌──────────────────────┐
│datasophon-service│←──→│datasophon-kubernetes │
└─────────┬───────┘    └──────────────────────┘
          ↓              (K8s独立业务模块)
┌─────────┴───────┐
│ datasophon-dao  │
└─────────────────┘
```

## 📋 **调用模式**

### **Service层调用K8s模块**
```java
@Service
public class ServiceRoleServiceImpl implements ServiceRoleService {
    
    @Autowired
    private KubernetesService kubernetesService; // 注入K8s服务
    
    public Result deployService(DeployRequest request) {
        // 1. 处理通用业务逻辑
        ServiceRoleEntity entity = processCommonLogic(request);
        
        // 2. 如果是K8s模式，调用K8s模块
        if (isKubernetesMode(request)) {
            KubernetesDeployResult k8sResult = kubernetesService.deployToK8s(entity);
            return handleK8sResult(k8sResult);
        }
        
        // 3. 其他模式的处理逻辑
        return handleOtherModes(entity);
    }
}
```

### **API层不直接调用K8s**
```java
@RestController
public class ServiceRoleController {
    
    @Autowired
    private ServiceRoleService serviceRoleService; // 只依赖Service
    
    @PostMapping("/deploy")
    public Result<DeployResponse> deploy(@RequestBody DeployRequest request) {
        // ✅ 通过Service层处理，Service内部决定是否调用K8s模块
        DeployResponse response = serviceRoleService.deployService(request);
        return Result.success(response);
    }
}
```

## 🔧 **K8s模块内部结构**

```java
com.datasophon.kubernetes
├── service/                 # K8s业务服务
│   ├── KubernetesService.java
│   ├── K8sDeployService.java
│   └── K8sMonitorService.java
├── client/                  # K8s客户端封装
│   ├── K8sClientManager.java
│   └── K8sResourceManager.java
├── strategy/                # K8s策略模式
│   ├── K8sDeployStrategy.java
│   └── K8sServiceStrategy.java
├── dto/                     # K8s专用DTO
│   ├── K8sDeployRequest.java
│   └── K8sDeployResponse.java
└── config/                  # K8s配置
    └── KubernetesConfig.java
```

## 📊 **数据访问模式**

### **K8s模块需要数据时的正确方式**

```java
@Service
public class KubernetesServiceImpl implements KubernetesService {
    
    // ❌ 错误：不直接依赖DAO
    // @Autowired
    // private ServiceRoleDao serviceRoleDao;
    
    // ✅ 正确：通过Service层接口获取数据
    public KubernetesDeployResult deployToK8s(ServiceRoleEntity entity) {
        // 1. 使用传入的entity数据
        String serviceName = entity.getServiceName();
        
        // 2. 专注K8s部署逻辑
        return deployToKubernetesCluster(serviceName, entity.getConfig());
    }
    
    // 如果需要查询数据，通过回调或接口方式
    public KubernetesDeployResult deployWithData(String serviceId, 
                                                DataProvider dataProvider) {
        // 通过回调获取数据，而不是直接查询
        ServiceRoleEntity entity = dataProvider.getServiceRole(serviceId);
        return deployToK8s(entity);
    }
}
```

## 🎯 **设计原则**

### **1. 单一职责**
- K8s模块只处理Kubernetes相关业务
- 不处理数据库操作
- 不处理通用业务逻辑

### **2. 依赖倒置**
- K8s模块不依赖具体的DAO实现
- 通过接口或回调获取需要的数据
- Service层协调数据和K8s操作

### **3. 业务隔离**
- K8s业务逻辑独立
- 可以独立测试和维护
- 不影响其他业务模块

## ✅ **检查清单**

- [ ] K8s模块只被Service层依赖
- [ ] API层不直接依赖K8s模块
- [ ] K8s模块不直接依赖DAO层
- [ ] 数据访问通过Service层协调
- [ ] K8s业务逻辑独立清晰
- [ ] 模块间接口设计合理

---

🎯 **记住：Kubernetes模块是独立的业务模块，被Service调用，不被其他模块依赖！**