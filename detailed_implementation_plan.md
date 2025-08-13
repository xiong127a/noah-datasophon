# Datasophon数据库标准化重构详细实施计划

## 文件路径和组件关系

### 1. 基础实体类设计
**文件路径**: `datasophon-dao/src/main/java/com/datasophon/dao/entity/base/BaseEntity.java`
**功能**: 统一的审计字段基础实体类

```java
@Data
@MappedSuperclass
public abstract class BaseEntity implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键 - 统一使用Long类型
     */
    @Id
    @Column("id")
    private Long id;
    
    /**
     * 创建时间
     */
    @Column("create_time")
    private Date createTime;
    
    /**
     * 更新时间  
     */
    @Column("update_time")
    private Date updateTime;
    
    /**
     * 创建人
     */
    @Column("create_by")
    private String createBy;
    
    /**
     * 更新人
     */
    @Column("update_by") 
    private String updateBy;
}
```

### 2. 特殊基础实体类设计
**文件路径**: `datasophon-dao/src/main/java/com/datasophon/dao/entity/base/BusinessKeyEntity.java`
**功能**: 用于VARCHAR主键表的特殊基础实体类

```java
@Data
@MappedSuperclass  
public abstract class BusinessKeyEntity extends BaseEntity {
    
    /**
     * 业务主键字段（原VARCHAR主键保留作为业务标识）
     */
    @Column("business_key")
    private String businessKey;
}
```

### 3. 数据库更新脚本文件
**MySQL脚本路径**: `datasophon-api/src/main/resources/db/migration/mysql/3.0.0/V3.0.0.3__database_standardization.sql`
**DM脚本路径**: `datasophon-api/src/main/resources/db/migration/dm/3.0.0/V3.0.0.3__database_standardization.sql`

### 4. 实体类重构范围
**需要修改的实体类文件**（42个）：
- `datasophon-dao/src/main/java/com/datasophon/dao/entity/*.java`
- 所有实体类继承BaseEntity或BusinessKeyEntity
- 主键类型统一修改为Long

## 数据库表结构修改规范

### 1. 标准审计字段添加
所有表统一添加以下字段：
```sql
-- 如果表还没有id字段，则添加
ALTER TABLE {table_name} ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- 添加缺失的审计字段
ALTER TABLE {table_name} ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE {table_name} ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';  
ALTER TABLE {table_name} ADD COLUMN create_by VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE {table_name} ADD COLUMN update_by VARCHAR(128) DEFAULT NULL COMMENT '更新人';
```

### 2. 特殊主键表处理策略

#### VARCHAR主键表重构方案
以`t_ddh_cluster_service_command`为例：
```sql
-- 1. 添加新的BIGINT主键
ALTER TABLE t_ddh_cluster_service_command ADD COLUMN id BIGINT AUTO_INCREMENT FIRST;
ALTER TABLE t_ddh_cluster_service_command ADD PRIMARY KEY (id);

-- 2. 将原主键改为业务标识字段
ALTER TABLE t_ddh_cluster_service_command CHANGE COLUMN command_id business_key VARCHAR(128) NOT NULL COMMENT '业务标识';
ALTER TABLE t_ddh_cluster_service_command ADD UNIQUE KEY uk_business_key (business_key);

-- 3. 添加审计字段（已有create_time和create_by，只需添加缺失的）
ALTER TABLE t_ddh_cluster_service_command ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE t_ddh_cluster_service_command ADD COLUMN update_by VARCHAR(128) DEFAULT NULL COMMENT '更新人';
```

#### 复合主键表重构方案
`t_ddh_config_version_info`表处理：
```sql
-- 1. 删除现有主键约束
ALTER TABLE t_ddh_config_version_info DROP PRIMARY KEY;

-- 2. 添加新的BIGINT主键
ALTER TABLE t_ddh_config_version_info ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- 3. 为原复合主键字段添加唯一约束
ALTER TABLE t_ddh_config_version_info ADD UNIQUE KEY uk_version_ref (version, ref_type, ref_id);

-- 4. 添加审计字段
ALTER TABLE t_ddh_config_version_info ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE t_ddh_config_version_info ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE t_ddh_config_version_info ADD COLUMN create_by VARCHAR(128) DEFAULT NULL COMMENT '创建人';  
ALTER TABLE t_ddh_config_version_info ADD COLUMN update_by VARCHAR(128) DEFAULT NULL COMMENT '更新人';
```

## 实体类重构规范

### 1. 标准实体类重构模板
以`ClusterInfoEntity`为例：

**重构前**:
```java
@Id
private Integer id;
private String createBy;
private Date createTime;
// ... 业务字段
```

**重构后**:
```java
@Table("t_ddh_cluster_info")
@Data
public class ClusterInfoEntity extends BaseEntity {
    
    // BaseEntity中已包含id、createTime、updateTime、createBy、updateBy
    
    // 业务字段
    private String clusterName;
    private String clusterCode;
    // ... 其他业务字段
}
```

### 2. VARCHAR主键实体类重构模板
以`ClusterServiceCommandEntity`为例：

**重构前**:
```java
@Id
private String commandId;
private String createBy;
private Date createTime;
```

**重构后**:
```java
@Table("t_ddh_cluster_service_command")
@Data  
public class ClusterServiceCommandEntity extends BusinessKeyEntity {
    
    // BusinessKeyEntity继承BaseEntity，包含id和所有审计字段
    // businessKey字段映射原command_id
    
    // 业务字段
    private String commandName;
    private CommandState commandState;
    // ... 其他业务字段
}
```

## 错误处理策略

### 1. 数据迁移安全保障
- 执行前自动备份相关表数据
- 使用事务确保原子性操作
- 提供回滚脚本应对异常情况

### 2. 兼容性处理
- 保留原有业务字段，确保应用层兼容
- 渐进式字段迁移，避免业务中断
- 提供数据一致性验证脚本

### 3. 索引优化
- 为新增的审计字段添加合适的索引
- 保留原有业务索引的有效性
- 优化查询性能

## 测试验证方法

### 1. 数据完整性验证
```sql
-- 验证所有表都有标准审计字段
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'datasophon2' 
  AND TABLE_NAME LIKE 't_ddh_%'
  AND TABLE_NAME NOT IN (
    SELECT DISTINCT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'datasophon2' 
      AND COLUMN_NAME IN ('id', 'create_time', 'update_time', 'create_by', 'update_by')
  );
```

### 2. 主键类型一致性验证
```sql
-- 验证所有主键都是BIGINT类型
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'datasophon2' 
  AND COLUMN_KEY = 'PRI' 
  AND DATA_TYPE != 'bigint';
```

### 3. 实体类映射验证
- 编译所有实体类确保语法正确
- 运行单元测试验证映射关系
- 执行集成测试确保业务功能正常

