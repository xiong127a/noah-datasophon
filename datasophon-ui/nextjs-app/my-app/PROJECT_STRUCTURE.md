# 项目结构文档

## 📁 标准化Next.js项目结构

### 🛣️ 路由结构 (app/)
```
app/
├── page.tsx                    # 主页 "/"
├── login/page.tsx             # 登录页 "/login"  
├── profile/page.tsx           # 个人资料 "/profile"
├── hosts/page.tsx             # 主机管理 "/hosts"
├── system/                     # 系统管理模块
│   ├── users/page.tsx         # 用户管理 "/system/users"
│   ├── racks/page.tsx         # 机架管理 "/system/racks"
│   ├── tags/page.tsx          # 标签管理 "/system/tags"
│   └── audit/page.tsx         # 审计日志 "/system/audit"
└── clusters/
    ├── list/page.tsx          # 集群列表 "/clusters/list"
    ├── framework/page.tsx     # 集群框架 "/clusters/framework"
    └── storage/page.tsx       # 集群存储 "/clusters/storage"
```

### 🧩 组件结构 (components/)
```
components/
├── layout/                    # 布局组件
│   ├── navbar-final.tsx      # 统一导航栏 (所有页面使用)
│   └── index.ts              # 导出索引
├── auth/                      # 认证相关组件
│   └── login-page.tsx         # 登录页面组件
├── cluster/                   # 集群相关组件
│   ├── cluster-list.tsx       # 集群列表组件
│   ├── cluster-framework.tsx  # 集群框架组件
│   ├── cluster-storage.tsx    # 集群存储组件
│   ├── authorization-dialog.tsx # 集群授权对话框
│   ├── create-dialog.tsx      # 创建集群对话框
│   └── index.ts              # 导出索引
├── host/                      # 主机相关组件
│   └── host-management.tsx    # 主机管理组件
├── user/                      # 用户相关组件
│   └── user-management.tsx    # 用户管理组件
├── tag/                       # 标签相关组件
│   ├── tag-management.tsx     # 标签管理组件
│   ├── add-tag-dialog.tsx     # 添加标签对话框
│   ├── delete-tag-dialog.tsx  # 删除标签对话框
│   └── index.ts              # 导出索引
├── rack/                      # 机架相关组件
│   ├── rack-management.tsx    # 机架管理组件
│   ├── add-rack-dialog.tsx    # 添加机架对话框
│   ├── delete-rack-dialog.tsx # 删除机架对话框
│   └── index.ts              # 导出索引
├── profile/                   # 个人资料组件
│   └── profile-page.tsx       # 个人资料页面组件
├── login/                     # 登录辅助组件
│   ├── LoginBackground.tsx    # 登录背景组件
│   └── ParticleCanvas.tsx     # 粒子画布组件
├── system/                    # 系统管理组件
│   ├── audit-log-management.tsx # 日志审计管理组件
│   └── index.ts              # 导出索引
└── ui/                        # 基础UI组件 (shadcn/ui)
    ├── alert-dialog.tsx
    ├── button.tsx
    ├── card.tsx
    ├── dialog.tsx
    └── ...
```

### 📦 其他目录
```
lib/                           # 工具函数和配置
├── api-config.ts             # API配置
└── utils.ts                  # 工具函数

hooks/                         # 自定义React Hooks
types/                         # TypeScript类型定义
public/                        # 静态资源
├── images/
└── icons/
```

## 🔄 导入路径规范

### App路由组件导入
```typescript
// ✅ 正确的导入方式
import NavbarFinal from "../../../components/layout/navbar-final"
import ClusterList from "../../../components/cluster/cluster-list"

// 或使用index导入 (推荐)
import { NavbarFinal } from "../../../components/layout"
import { ClusterList } from "../../../components/cluster"  
```

### 组件间导入
```typescript
// ✅ 同级组件导入
import AuthorizationDialog from "./authorization-dialog"

// ✅ 跨模块组件导入  
import FinalNavbar from "../layout/navbar-final"
```

## 🎯 设计原则

### 📂 按功能模块组织
- **layout/** - 全局布局组件
- **auth/** - 认证相关功能
- **cluster/** - 集群管理功能
- **host/** - 主机管理功能
- **user/** - 用户管理功能
- **tag/** - 标签管理功能
- **rack/** - 机架管理功能
- **system/** - 系统管理功能
- **profile/** - 用户资料功能

### 🔄 组件复用
- 统一导航栏组件被所有页面复用
- 对话框组件被集群列表页面调用
- UI组件库统一管理

### 📁 清晰的分层
- **app/** - 路由和页面入口
- **components/** - 可复用的业务组件
- **lib/** - 工具函数和配置
- **public/** - 静态资源

## ✅ 优化成果

1. **结构标准化** - 符合Next.js最佳实践
2. **模块化管理** - 按功能清晰分类
3. **导入路径优化** - 相对路径清晰明确
4. **可维护性提升** - 文件定位快速准确           
5. **团队协作友好** - 约定明确，降低沟通成本

## 🚀 后续扩展

新增功能时按以下规范：
- 新页面：添加到 `app/` 对应路由
- 新组件：添加到 `components/` 对应模块
- 新工具：添加到 `lib/`
- 新类型：添加到 `types/`

## 📝 日志审计功能迁移

从Vue2项目成功迁移到Next.js框架：

### 功能特性
- **多维度筛选**：支持按操作模块、服务名称、操作用户筛选
- **分页展示**：完整的分页功能和数据展示
- **实时状态**：操作结果状态实时显示
- **响应式设计**：适配不同屏幕尺寸

### 实现架构
- **路由**：`/system/audit` - 统一的系统管理路由结构
- **组件**：`AuditLogManagement` - 可复用的日志审计组件
- **类型**：完整的TypeScript类型定义
- **API**：保持与后端API接口的兼容性 