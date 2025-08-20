# ID类型迁移指南

## 问题背景

后端使用20位LONG类型的ID，但JavaScript的Number类型只能安全表示到2^53-1（约16位数字），超出此范围会丢失精度。

## 解决方案

### 1. 统一ID类型规范

所有ID字段统一使用`string`类型，包括但不限于：
- `clusterId: string`
- `serviceId: string` 
- `hostId: string`
- `userId: string`
- `rackId: string`
- `labelId: string`
- 等等...

### 2. 已修复的文件

#### `lib/id-types.ts` (新增)
- 定义了统一的ID类型系统
- 提供ID验证和转换工具类
- 检测长整型ID的工具方法

#### `lib/api/doc-service.ts`
```typescript
// 修复前
export interface ServiceDocVO {
  clusterId: number  // ❌ 会丢失精度
  serviceId: number  // ❌ 会丢失精度
}

// 修复后  
export interface ServiceDocVO {
  clusterId: string  // ✅ 保持精度
  serviceId: string  // ✅ 保持精度
}
```

#### `lib/api-utils-v1.ts`
```typescript
// 修复前
clusterId: parseInt(params.clusterId),  // ❌ 强制转number
serviceId: parseInt(params.serviceId),  // ❌ 强制转number

// 修复后
clusterId: params.clusterId,  // ✅ 保持string
serviceId: params.serviceId,  // ✅ 保持string
```

### 3. API调用规范

#### 正确的做法 ✅
```typescript
// 保持string格式传递给后端
const params = {
  clusterId: "1234567890123456789",  // 20位ID
  serviceId: "9876543210987654321",   // 20位ID
  type: "component"
}

const response = await clusterApiV1.doc.getServiceDoc(params)
```

#### 错误的做法 ❌
```typescript
// 不要转换为number
const params = {
  clusterId: parseInt("1234567890123456789"),  // ❌ 精度丢失
  serviceId: Number("9876543210987654321"),    // ❌ 精度丢失
}
```

### 4. 类型检查工具

使用新增的ID验证工具：

```typescript
import { IDValidator, toStringId } from '@/lib/id-types'

// 验证ID格式
if (IDValidator.isValidId(someId)) {
  // 安全使用
}

// 确保ID为string格式
const safeId = toStringId(maybeNumberId)

// 检查是否为长整型
if (IDValidator.isPotentialLongId(id)) {
  console.warn('检测到长整型ID，请确保以string格式处理')
}
```

### 5. 前端组件使用

```typescript
// React组件props
interface ServiceComponentProps {
  serviceId: string    // ✅ 使用string
  clusterId?: string   // ✅ 使用string
}

// 事件处理
const handleServiceClick = (serviceId: string) => {
  // 直接使用string格式的ID
  navigate(`/service/${serviceId}`)
}
```

### 6. 后端兼容性

后端接口已支持接收string格式的ID：

```java
// 后端自动转换
@PostMapping("/getServiceDoc")
public Result<ServiceDocVO> getServiceDoc(@RequestBody ServiceDocParams params) {
    // params.clusterId 可以是 "1234567890123456789" 
    // params.serviceId 可以是 "9876543210987654321"
    // 后端会自动转换为Long类型
}
```

### 7. 迁移检查清单

- [ ] ✅ 所有API调用移除`parseInt()`和`Number()`转换
- [ ] ✅ 所有接口定义的ID字段改为`string`类型
- [ ] ✅ 组件props中的ID参数使用`string`类型
- [ ] ✅ 状态管理中的ID使用`string`类型
- [ ] ✅ 路由参数中的ID保持`string`格式
- [ ] ✅ 本地存储的ID以`string`格式保存

### 8. 注意事项

1. **数组中的ID**：确保ID数组中的每个元素都是string
   ```typescript
   const serviceIds: string[] = ["123456789012345678", "987654321098765432"]
   ```

2. **URL参数**：URL参数天然是string，无需转换
   ```typescript
   const serviceId = useParams<{serviceId: string}>().serviceId
   ```

3. **表单处理**：确保表单提交时ID保持string格式
   ```typescript
   const formData = {
     serviceId: formValues.serviceId.toString()
   }
   ```

4. **比较操作**：字符串比较而非数值比较
   ```typescript
   // 正确
   if (currentServiceId === targetServiceId) { }
   
   // 错误 
   if (Number(currentServiceId) === Number(targetServiceId)) { }
   ```

通过以上修复，确保了20位LONG类型的ID在前后端传输过程中不会丢失精度，保证了数据的准确性和系统的稳定性。
