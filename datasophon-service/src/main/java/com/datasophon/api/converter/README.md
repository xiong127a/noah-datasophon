# API层转换器说明

## 📍 为什么转换器放在API层？

### 正确的依赖关系
```
common(基础) ← dao ← service ← api(应用层)
```

### 关键原因

1. **依赖方向正确**
   - API层可以访问所有下层的类（Entity、DTO、VO）
   - 不会造成依赖倒置或循环依赖

2. **职责单一**
   - Common层：基础工具和接口
   - DAO层：数据访问
   - Service层：业务逻辑  
   - API层：接口和转换

3. **实际需要**
   - 转换器主要在Controller中使用
   - 需要同时访问Entity、DTO、VO三种类型

## 🎯 使用方式

```java
@RestController
public class UserController {
    
    @Autowired
    private UserInfoConverter converter;
    
    @GetMapping("/user/{id}")
    public UserInfoVO getUser(@PathVariable Integer id) {
        UserInfoEntity entity = userService.findById(id);
        return converter.entityToVo(entity);
    }
}
```

## 📦 包结构

```
datasophon-api/
└── src/main/java/com/datasophon/api/
    ├── converter/          # ✅ 转换器实现
    │   ├── UserInfoConverter.java
    │   └── ClusterInfoConverter.java
    ├── controller/         # 控制器
    ├── vo/                # 视图对象
    └── dto/               # API层DTO
```

这种设计既保持了清晰的分层架构，又避免了复杂的依赖关系问题。