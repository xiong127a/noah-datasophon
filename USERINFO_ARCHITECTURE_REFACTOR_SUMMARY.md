# UserInfo模块架构重构总结

## 🎯 重构目标

解决了两个关键架构问题：
1. **SQL逻辑错放** - 将Service层的SQL逻辑移到DAO层
2. **返回类型混乱** - Service层不再返回Result，Result仅用于API层

## 📊 重构前后对比

### ❌ 重构前的问题

```java
// 问题1: Service层直接写SQL
@Service
public class UserInfoServiceImpl {
    public Result createUser(UserInfoEntity userInfo) {
        // SQL逻辑写在Service层 ❌
        List<UserInfoEntity> list = QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username)
                .list();
        
        // Service层返回Result ❌
        return Result.error(Status.USER_NAME_EXIST.getCode(), Status.USER_NAME_EXIST.getMsg());
    }
}
```

### ✅ 重构后的正确架构

```java
// DAO层：负责数据访问
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfoEntity> {
    default boolean existsByUsername(String username) {
        return QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username)
                .exists();
    }
}

// Service层：负责业务逻辑，抛出异常
@Service
public class UserInfoServiceImpl implements UserInfoService {
    public UserInfoDTO createUser(UserInfoDTO userInfoDTO) {
        if (userInfoMapper.existsByUsername(userInfoDTO.getUsername())) {
            throw UserBusinessException.usernameExists(userInfoDTO.getUsername());
        }
        // 业务逻辑处理...
        return entityToDto(savedUser);
    }
}

// API层：包装Result，处理异常
@RestController
public class UserController {
    public Result<UserInfoVO> createUser(@RequestBody UserInfoDTO userInfoDTO) {
        UserInfoDTO createdUser = userInfoService.createUser(userInfoDTO);
        UserInfoVO userVO = userInfoConverter.dtoToVo(createdUser);
        return Result.success(userVO);
    }
}

// 全局异常处理：将异常转换为Result
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
}
```

## 🏗️ 完整的分层架构

### 1. DAO层 (数据访问层)
- **文件**: `UserInfoMapper.java`
- **职责**: 所有数据库操作和SQL逻辑
- **特点**: 使用MyBatis-Flex的默认方法实现SQL

```java
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfoEntity> {
    default UserInfoEntity selectByUsernameAndPassword(String username, String password) {
        return QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username)
                .and(UserInfoEntity::getPassword).eq(password)
                .one();
    }
    
    default Page<UserInfoEntity> selectPageByUsername(Page<UserInfoEntity> page, String username) {
        QueryChain<UserInfoEntity> query = QueryChain.of(UserInfoEntity.class);
        if (StringUtils.isNotBlank(username)) {
            query.where(UserInfoEntity::getUsername).like("%" + username + "%");
        }
        return query.page(page);
    }
}
```

### 2. Service层 (业务逻辑层)
- **文件**: `UserInfoService.java`, `UserInfoServiceImpl.java`
- **职责**: 业务逻辑处理，参数校验
- **输入/输出**: 接收和返回DTO
- **异常处理**: 抛出业务异常，不返回Result

```java
public interface UserInfoService {
    UserInfoDTO createUser(UserInfoDTO userInfoDTO);
    PageResult<UserInfoDTO> getUserListByPage(String username, Integer page, Integer pageSize);
}

@Service
public class UserInfoServiceImpl implements UserInfoService {
    public UserInfoDTO createUser(UserInfoDTO userInfoDTO) {
        // 参数校验
        if (StringUtils.isBlank(userInfoDTO.getUsername())) {
            throw UserBusinessException.usernameIsNull();
        }
        
        // 业务规则校验
        if (userInfoMapper.existsByUsername(userInfoDTO.getUsername())) {
            throw UserBusinessException.usernameExists(userInfoDTO.getUsername());
        }
        
        // 业务逻辑处理
        UserInfoEntity userInfo = dtoToEntity(userInfoDTO);
        userInfo.setCreateTime(new Date());
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        
        this.save(userInfo);
        return entityToDto(userInfo);
    }
}
```

### 3. API层 (接口层)
- **文件**: `UserController.java`, `GlobalExceptionHandler.java`
- **职责**: HTTP请求处理，Result包装，异常转换
- **转换**: DTO ↔ VO，PageResult → Result

```java
@RestController
public class UserController {
    @PostMapping
    public Result<UserInfoVO> createUser(@RequestBody UserInfoDTO userInfoDTO) {
        // 调用Service获取业务结果
        UserInfoDTO createdUser = userInfoService.createUser(userInfoDTO);
        
        // 转换为VO并包装Result
        UserInfoVO userVO = userInfoConverter.dtoToVo(createdUser);
        return Result.success(userVO);
    }
}

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
}
```

## 📦 实体类分层设计

| 层次 | 位置 | 命名规范 | 职责 | 示例 |
|------|------|----------|------|------|
| **Entity** | `dao/entity/` | `XxxEntity.java` | 数据库映射 | `UserInfoEntity` |
| **DTO** | `common/dto/` | `XxxDTO.java` | 数据传输 | `UserInfoDTO` |
| **VO** | `api/vo/` | `XxxVO.java` | 前端展示 | `UserInfoVO` |
| **Converter** | `api/converter/` | `XxxConverter.java` | 对象转换 | `UserInfoConverter` |

## 🎁 重构成果

### ✅ 架构优势

1. **职责清晰**
   - DAO层：专注数据访问
   - Service层：专注业务逻辑
   - API层：专注接口交互

2. **依赖方向正确**
   ```
   common(基础) ← dao ← service ← api(应用层)
   ```

3. **异常处理规范**
   - Service层抛出业务异常
   - API层统一异常处理和转换

4. **数据流向清晰**
   ```
   Request → DTO → Service → DTO → Converter → VO → Result → Response
   ```

### 🔧 技术特点

- **SpringBoot 3 + JDK 21**: 充分利用现代Java特性
- **MapStruct**: 编译时转换，性能最佳
- **MyBatis-Flex**: 流式API，代码简洁
- **分层异常**: 业务异常与系统异常分离

### 📈 性能提升

- **编译时转换**: MapStruct无反射，性能优异
- **SQL逻辑优化**: DAO层专门处理，查询更高效
- **内存使用**: 明确的对象生命周期，减少不必要转换

## 🚀 使用示例

```java
// Service层使用
UserInfoDTO userDTO = userInfoService.createUser(dto);
PageResult<UserInfoDTO> pageResult = userInfoService.getUserListByPage("admin", 1, 10);

// Controller层使用
UserInfoVO userVO = userInfoConverter.dtoToVo(userDTO);
return Result.success(userVO);

// 异常处理（自动）
throw UserBusinessException.usernameExists("admin");
// → GlobalExceptionHandler → Result.error(10003, "用户名已存在")
```

这种架构设计完全符合**单一职责原则**和**依赖倒置原则**，为项目提供了清晰、可维护、高性能的分层架构基础。