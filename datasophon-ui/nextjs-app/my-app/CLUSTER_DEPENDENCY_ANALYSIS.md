# Noah大数据基础平台集群依赖性分析文档

## 📋 概述

本文档基于Vue2老项目和后端API接口，分析了哪些页面/功能需要集群上下文（clusterId），以及用户登录后的权限控制策略。

---

## 🎯 分析方法

1. **前端路由分析**：检查Vue2项目路由配置中 `meta.isCluster` 标记
2. **后端接口分析**：检查Controller中需要 `clusterId` 参数的接口
3. **页面组件分析**：查看具体页面是否使用集群相关功能

---

## 🔍 Vue2项目路由分析

### ✅ 无需集群上下文的页面

| 路由路径 | 页面名称 | 说明 |
|---------|---------|------|
| `/login` | 登录页 | 用户认证入口 |
| `/colony-manage/colony-list` | 集群管理 | 管理所有集群，用于选择集群 |
| `/colony-manage/colony-parcel` | 存储库管理 | 全局存储库配置 |
| `/colony-manage/colony-frame` | 集群框架 | 全局框架配置 |
| `/security-center` | 用户管理 | 系统级用户管理 |

### 🔒 需要集群上下文的页面（标记为 `isCluster: 'isCluster'`）

| 路由路径 | 页面名称 | 集群依赖程度 | 说明 |
|---------|---------|-------------|------|
| `/overview` | 集群总览 | **强依赖** | 显示当前集群的监控数据 |
| `/service-manage` | 服务管理 | **强依赖** | 管理当前集群的服务 |
| `/host-manage` | 主机管理 | **强依赖** | 管理当前集群的主机 |
| `/alarm-manage/notice` | 通知组管理 | **强依赖** | 当前集群的告警通知 |
| `/alarm-manage/group` | 告警组管理 | **强依赖** | 当前集群的告警组 |
| `/alarm-manage/metric` | 告警指标管理 | **强依赖** | 当前集群的监控指标 |
| `/system-center/tenant` | 租户管理 | **强依赖** | 当前集群的租户 |
| `/system-center/user` | 用户管理 | **强依赖** | 当前集群的用户 |
| `/system-center/frame` | 机架管理 | **强依赖** | 当前集群的机架配置 |
| `/system-center/tag` | 标签管理 | **强依赖** | 当前集群的节点标签 |
| `/system-center/log` | 日志审计 | **强依赖** | 当前集群的操作日志 |

---

## 🔧 后端API接口分析

### 需要 clusterId 参数的主要接口

#### 1️⃣ **机架管理** (`ClusterRackController`)
```java
@RequestMapping("cluster/rack")
- /list: 需要 clusterId
- /save: 需要 clusterId + rack
- /delete: 需要 clusterId + rackId
```

#### 2️⃣ **主机管理** (`ClusterHostController`)
```java
@RequestMapping("api/cluster/host")
- /all: 需要 clusterId
- /list: 需要 clusterId + 分页参数
- /getRoleListByHostname: 需要 clusterId + hostname
- /getRack: 需要 clusterId
- /assignRack: 需要 clusterId + rack + hostIds
- /saveKubernetesHost: 需要 clusterId
```

#### 3️⃣ **标签管理** (`ClusterNodeLabelController`)
```java
@RequestMapping("cluster/node/label")
- /list: 需要 clusterId
- /save: 需要 clusterId + nodeLabel
- /assign: 需要 nodeLabelId + hostIds
```

#### 4️⃣ **告警组管理** (`AlertGroupController`)
```java
@RequestMapping("alert/group")
- /list: 需要 clusterId + 分页参数
```

#### 5️⃣ **日志审计** (`OperationLogController`)
```java
@RequestMapping("api/log")
- /serviceNameList: 需要 clusterId
```

#### 6️⃣ **集群信息** (`ClusterInfoController`)
```java
@RequestMapping("api/cluster")
- /info/{id}: 获取指定集群信息
- /detail/{clusterId}: 获取集群详细信息
- /runningClusterList: 获取运行中的集群列表（无需clusterId）
```

---

## 🚦 权限控制策略

### 📊 Vue2项目的集群状态管理

**Vuex Store 状态：**
- `clusterId`: 当前选择的集群ID（存储在localStorage）
- `isCluster`: 标识是否在集群上下文中（值为'isCluster'）
- `runningCluster`: 运行中的集群列表

**访问控制逻辑：**
```javascript
// 路由守卫检查
const isCluster = localStorage.getItem('isCluster')
const clusterId = localStorage.getItem('clusterId')

// 需要集群上下文的页面
if (route.meta.isCluster === 'isCluster' && !clusterId) {
  // 重定向到集群选择页面
}
```

### 🎯 建议的NextJS权限控制实现

#### 1️⃣ **登录后跳转逻辑**
```typescript
// 当前：router.push("/") ❌
// 建议：router.push("/clusters") ✅
```

#### 2️⃣ **集群选择器组件**
在顶部导航栏添加集群选择器，显示：
- 当前集群名称和类型图标
- 下拉选择其他运行中的集群
- 切换集群时更新localStorage中的clusterId

#### 3️⃣ **页面访问控制**
创建HOC或Hook检查集群上下文：
```typescript
const useClusterRequired = () => {
  const clusterId = localStorage.getItem('clusterId')
  if (!clusterId || clusterId === '-1') {
    // 显示"请选择集群"提示
    return { hasCluster: false, clusterId: null }
  }
  return { hasCluster: true, clusterId: Number(clusterId) }
}
```

---

## 📋 迁移建议

### 🔄 **immediate 需要修改的页面**

1. **登录成功跳转**
   - 从 `/` 改为 `/clusters`

2. **添加集群选择器**
   - 在navbar-final.tsx中添加集群选择器组件
   - 显示当前集群，支持切换

3. **需要集群上下文检查的页面**
   ```
   ✅ 已完成：
   - /system/racks (机架管理)
   - /system/tags (标签管理)  
   - /system/audit (日志审计)
   
   🔄 需要检查：
   - /hosts (主机管理)
   - /alerts/* (告警管理)
   - /clusters/storage (存储库)
   - /clusters/framework (框架)
   ```

### 📝 **数据存储标准化**

统一使用 `clusterId` 而不是混用：
- `clusterId` ✅ (Vue2兼容)
- `current_cluster_id` ❌ (NextJS临时方案)

### 🎛️ **用户体验优化**

1. **未选择集群时**：
   - 显示友好提示"请先选择要管理的集群"
   - 提供跳转到集群列表的按钮

2. **已选择集群时**：
   - 在导航栏清晰显示当前集群名称
   - 所有功能正常使用

3. **集群切换时**：
   - 自动刷新当前页面数据
   - 保持用户在同一功能页面

---

## 🔗 相关文件

### Vue2项目文件
- `src/router/config.js` - 路由配置
- `src/layouts/header/AdminHeader.vue` - 集群选择器实现
- `src/store/modules/setting.js` - 集群状态管理

### 后端API文件
- `ClusterRackController.java` - 机架管理接口
- `ClusterHostController.java` - 主机管理接口
- `ClusterNodeLabelController.java` - 标签管理接口
- `OperationLogController.java` - 日志审计接口

### NextJS项目需要修改的文件
- `components/auth/login-page.tsx` - 登录跳转逻辑
- `components/layout/navbar-final.tsx` - 添加集群选择器
- 各个需要集群上下文的页面组件

---

## ✅ 结论

**强依赖集群的页面（共9个）：**
1. 集群总览
2. 服务管理
3. 主机管理  
4. 通知组管理
5. 告警组管理
6. 告警指标管理
7. 租户管理
8. 机架管理 ✅
9. 标签管理 ✅
10. 日志审计 ✅

**注意**：集群存储库和集群框架是全局配置，不需要集群上下文。

**无需集群的页面（共5个）：**
1. 集群管理（用于选择集群）
2. 存储库管理  
3. 集群框架管理
4. 系统用户管理
5. 登录页面

**关键实现点：**
- 登录后跳转到集群列表
- 添加集群选择器组件
- 实现集群上下文检查
- 统一数据存储标准