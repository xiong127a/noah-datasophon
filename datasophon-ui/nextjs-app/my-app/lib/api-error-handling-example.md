# API错误处理机制使用说明

## 📋 **功能概述**

本项目现在具备了完整的API错误处理机制，能够自动捕获和显示各种类型的错误，提供苹果样式的美观通知。

## 🎯 **主要特性**

### **1. 统一错误处理**
- ✅ **业务错误**: 自动检测后端返回的 `code !== 200` 或 `success: false`
- ✅ **网络错误**: 处理各种HTTP状态码（400, 401, 403, 404, 500等）
- ✅ **认证错误**: 自动清理token并提示重新登录
- ✅ **权限错误**: 友好的权限提示

### **2. 苹果样式设计**
- 🎨 **现代化UI**: 毛玻璃效果、圆角、阴影
- 🌈 **语义化颜色**: 不同错误类型使用不同颜色
- 📱 **响应式设计**: 适配桌面和移动设备
- 🌙 **暗色模式**: 自动适配系统主题

## 🚀 **使用方式**

### **自动错误处理**

现在所有通过 `apiV1` 发起的请求都会自动处理错误：

```typescript
// 这个请求如果失败，会自动显示错误toast
const response = await apiV1.post('/api/v1/some-endpoint', data, { headers })

// 不需要手动检查错误，拦截器会处理
// ❌ 不再需要这样做：
// if (response.data.code !== 200) {
//   toast.error(response.data.msg)
// }
```

### **手动使用苹果样式Toast**

如果需要手动显示通知：

```typescript
import { appleToast, operationToast, apiErrorToast } from '@/lib/apple-toast'

// 成功通知
appleToast.success('操作成功！')

// 错误通知
appleToast.error('操作失败，请重试')

// 警告通知
appleToast.warning('请注意检查配置')

// 信息通知
appleToast.info('正在处理您的请求...')

// 加载状态
const loadingToast = appleToast.loading('正在保存...')

// 操作反馈
operationToast.success('保存', '配置已成功保存')
operationToast.failed('删除', '权限不足')

// Promise状态处理
appleToast.promise(
  fetch('/api/data'),
  {
    loading: '正在加载数据...',
    success: '数据加载成功',
    error: '数据加载失败'
  }
)
```

## 🛠️ **错误类型说明**

### **业务错误** (Business Error)
```json
{
  "code": 500,
  "success": false,
  "msg": "生成服务命令失败: 未选择需要安装的服务实例"
}
```
**效果**: 显示红色错误toast，内容为 `msg` 字段

### **网络错误** (Network Error)
- **401**: 认证过期，自动清理token
- **403**: 权限不足
- **404**: 资源不存在
- **500**: 服务器错误
- **502/503**: 服务不可用

### **连接错误** (Connection Error)
- 网络断开
- 请求超时
- DNS解析失败

## 🎨 **样式展示**

### **成功通知**
```
✅ 操作成功
绿色边框，毛玻璃背景，圆角设计
```

### **错误通知**
```
❌ 生成服务命令失败: 未选择需要安装的服务实例
红色边框，详细错误信息，持续6秒
```

### **警告通知**
```
⚠️ 请注意检查配置
橙色边框，中等持续时间
```

### **信息通知**
```
📡 正在处理您的请求...
蓝色边框，加载动画效果
```

## 📱 **响应式特性**

- **桌面端**: 最小宽度320px，最大宽度480px
- **移动端**: 自动适配屏幕宽度，左右留16px边距
- **高对比度**: 支持高对比度模式
- **减少动画**: 支持用户偏好设置

## 🌙 **暗色模式**

自动检测系统主题：
- **亮色模式**: 白色背景，黑色文字
- **暗色模式**: 深色背景，白色文字

## ⚙️ **配置选项**

### **默认配置**
```typescript
{
  duration: 4000,        // 显示时长（毫秒）
  position: 'top-right', // 显示位置
  dismissible: true,     // 可手动关闭
  closeButton: true,     // 显示关闭按钮
}
```

### **错误类型持续时间**
- **成功**: 3秒
- **信息**: 4秒
- **警告**: 5秒
- **错误**: 6秒
- **认证错误**: 8秒

## 🔧 **自定义配置**

如果需要自定义某个通知的配置：

```typescript
appleToast.error('自定义错误', {
  duration: 10000,           // 自定义持续时间
  position: 'bottom-center', // 自定义位置
  description: '详细描述',    // 添加描述
  action: {                  // 添加操作按钮
    label: '重试',
    onClick: () => console.log('重试')
  }
})
```

## 🚨 **常见问题**

### **Q: 为什么有些错误没有显示toast？**
A: 检查API调用是否使用了 `apiV1`，只有通过统一API客户端的请求才会自动处理错误。

### **Q: 如何禁用某个请求的自动错误处理？**
A: 目前所有请求都会自动处理错误，如果需要特殊处理，可以在catch块中自定义逻辑。

### **Q: Toast显示位置可以调整吗？**
A: 可以在 `app/layout.tsx` 中的 `<Toaster>` 组件修改 `position` 属性。

### **Q: 如何修改默认样式？**
A: 在 `styles/apple-toast.css` 中修改相应的CSS类。

## 📊 **测试验证**

1. **触发500错误**: 访问不存在的API端点
2. **触发业务错误**: 提交无效数据
3. **触发网络错误**: 断开网络后发起请求
4. **触发认证错误**: 使用过期token

## 🎉 **效果展示**

现在当您遇到像 `http://localhost:8081/ddh/api/v1/cluster/service/command/generate?commandType=INSTALL_SERVICE` 返回500错误时，会自动显示：

```
❌ 生成服务命令失败: 未选择需要安装的服务实例
```

美观的苹果样式错误通知，无需手动处理！

---

> **作者**: 任相鹏  
> **邮箱**: 635887935@qq.com  
> **日期**: 2025-01-20
