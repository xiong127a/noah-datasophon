# 实体类分层与转换器使用指南

## 📋 分层架构说明

### 正确的分层结构

```
datasophon-dao/
├── entity/              # ✅ 数据库实体 (XxxEntity)
└── mapper/             # ✅ 数据访问接口

datasophon-service/  
├── impl/               # ✅ 业务服务实现
└── 业务逻辑处理

datasophon-api/
├── controller/         # ✅ 控制器
├── dto/               # ✅ API层DTO  
│   ├── request/       # 请求DTO
│   └── response/      # 响应DTO
└── vo/                # ✅ 视图对象 (正确位置!)

datasophon-common/
├── dto/               # ✅ 跨模块通用DTO
├── converter/         # ✅ 转换器基础接口（不包含具体实现）
└── model/            # ✅ 通用模型类

📊 正确的依赖关系：
common(基础) ← dao ← service ← api(应用层)

🎯 转换器位置：
- BaseConverter接口 → common模块 (通用接口)
- UserInfoConverter实现 → api模块 (具体实现)
```

### ❌ 错误的设计
- VO放在DAO层 (`datasophon-dao/model/XxxVO.java`) - **违反分层原则**
- 转换器放在Common层 (`common/converter/UserInfoConverter.java`) - **依赖方向颠倒**
- Entity直接暴露给前端 - **安全风险**
- 所有层共用同一个对象 - **耦合过高**

### ✅ 正确的转换器位置
- **Common层**: 只放`BaseConverter`接口等通用组件
- **API层**: 放具体的转换器实现，如`UserInfoConverter`
- **原因**: API层可以访问所有下层的Entity、DTO、VO类

## 🎯 实体类命名规范

| 层次 | 位置 | 命名规范 | 示例 | 职责 |
|------|------|----------|------|------|
| **Entity** | `dao/entity/` | `XxxEntity.java` | `UserInfoEntity` | 数据库映射 |
| **DTO** | `common/dto/` | `XxxDTO.java` | `UserInfoDTO` | 数据传输 |
| **VO** | `api/vo/` | `XxxVO.java` | `UserInfoVO` | 前端展示 |
| **Request** | `api/dto/request/` | `XxxRequest.java` | `CreateUserRequest` | 接口请求 |
| **Response** | `api/dto/response/` | `XxxResponse.java` | `UserDetailResponse` | 接口响应 |
| **Converter** | `api/converter/` | `XxxConverter.java` | `UserInfoConverter` | 对象转换 |

## 🔧 MapStruct转换器使用

### 1. 基本用法

```java
// ✅ 正确的包导入
import com.datasophon.api.converter.UserInfoConverter;

@RestController
public class UserController {
    
    @Autowired
    private UserInfoConverter userConverter;
    
    @Autowired  
    private UserService userService;
    
    @GetMapping("/user/{id}")
    public UserInfoVO getUserInfo(@PathVariable Integer id) {
        // 从数据库获取Entity
        UserInfoEntity entity = userService.findById(id);
        
        // 转换为VO返回给前端
        return userConverter.entityToVo(entity);
    }
    
    @PostMapping("/user")
    public UserInfoVO createUser(@RequestBody CreateUserRequest request) {
        // Request转DTO
        UserInfoDTO dto = UserInfoDTO.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .build();
            
        // DTO转Entity保存
        UserInfoEntity entity = userConverter.dtoToEntity(dto);
        UserInfoEntity saved = userService.save(entity);
        
        // Entity转VO返回
        return userConverter.entityToVo(saved);
    }
}
```

### 2. Service层使用

```java
// ✅ Service层也可以使用API层的转换器
import com.datasophon.api.converter.UserInfoConverter;

@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserInfoConverter userConverter;
    
    @Autowired
    private UserMapper userMapper;
    
    public UserInfoDTO getUserInfo(Integer id) {
        UserInfoEntity entity = userMapper.selectById(id);
        return userConverter.entityToDto(entity);
    }
    
    public List<UserInfoVO> getUserList() {
        List<UserInfoEntity> entities = userMapper.selectAll();
        return userConverter.entityListToVoList(entities);
    }
}
```

## 🎨 转换器特性展示

### 字段映射与转换

```java
// Entity -> VO 转换示例
UserInfoEntity entity = UserInfoEntity.builder()
    .id(1)
    .username("admin")
    .email("admin@example.com")
    .phone("13800138000")
    .userType(1)
    .createTime(new Date())
    .build();

UserInfoVO vo = userConverter.entityToVo(entity);

// 转换结果:
// vo.getEmail() -> "a***@example.com"     (脱敏)
// vo.getPhone() -> "138****8000"          (脱敏)  
// vo.getUserTypeDesc() -> "管理员"         (类型转换)
// vo.getCreateTime() -> "2024-01-01 10:00:00" (格式化)
```

### 批量转换

```java
// 批量Entity转VO
List<UserInfoEntity> entities = userMapper.selectAll();
List<UserInfoVO> vos = userConverter.entityListToVoList(entities);

// 批量DTO转Entity
List<UserInfoDTO> dtos = getUserDTOList();
List<UserInfoEntity> entities = userConverter.dtoListToEntityList(dtos);
```

## 📚 最佳实践

### ✅ 推荐做法

1. **严格分层**: Entity只在DAO层使用，VO只在API层使用
2. **转换隔离**: 使用转换器进行层间数据转换，避免直接传递
3. **敏感数据**: VO中不包含密码等敏感信息，必要时进行脱敏
4. **类型安全**: 利用MapStruct的编译时检查，避免运行时错误
5. **性能优化**: MapStruct生成的代码无反射，性能最佳

### ❌ 避免做法

1. **直接暴露Entity**: 不要将Entity直接返回给前端
2. **层级混乱**: 不要在DAO层使用VO，不要在API层使用Entity
3. **字段冗余**: 避免在VO中包含前端不需要的字段
4. **硬编码转换**: 避免手写setter/getter进行对象转换
5. **忽略null处理**: 转换时要考虑null值的处理

## 🚀 性能优化建议

### Spring Boot 3 + JDK 21 优化

```java
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    builder = @Builder(disableBuilder = true) // JDK 21 Records支持
)
public interface UserInfoConverter extends BaseConverter<UserInfoEntity, UserInfoDTO, UserInfoVO> {
    // 利用JDK 21的模式匹配
    @Named("userTypeToDesc")
    default String userTypeToDesc(Integer userType) {
        return switch (userType) {
            case 1 -> "管理员";
            case 2 -> "普通用户"; 
            case null, default -> "未知";
        };
    }
}
```

## 📖 扩展阅读

- [MapStruct官方文档](https://mapstruct.org/documentation/stable/reference/html/)
- [Spring Boot 3最佳实践](https://spring.io/projects/spring-boot)
- [JDK 21新特性应用](https://openjdk.org/projects/jdk/21/)