-- V3.0.0.3 数据库表结构标准化重构（达梦数据库版本）
-- 作者：任相鹏
-- 邮箱：635887935@qq.com  
-- 日期：2025-01-31
-- 描述：统一数据库表结构设计，实现标准化主键和审计字段，建立规范的逻辑外键关系
-- 
-- 注意：本文件为达梦数据库版本，语法已针对达梦数据库进行适配

-- 主要变更说明：
-- 1. 移除了所有IDENTITY设置，使用雪花算法生成ID
-- 2. 调整了达梦数据库特有的语法结构（无AUTO_INCREMENT语法）
-- 3. 统一所有主键为BIGINT类型
-- 4. 外键关联逻辑与MySQL版本完全一致

-- =============================================================================
-- DDL部分：数据库结构变更（达梦数据库版本）
-- =============================================================================

-- 1. VARCHAR主键表重构（保证数据完整性）
-- 1.1 t_ddh_cluster_service_command表重构
-- 第一步：添加新的BIGINT id列
ALTER TABLE t_ddh_cluster_service_command ADD id BIGINT;

-- 第二步：创建临时映射表保存新旧ID关系
CREATE GLOBAL TEMPORARY TABLE temp_command_id_mapping (
    old_command_id VARCHAR(128),
    new_id BIGINT
) ON COMMIT DELETE ROWS;

-- 第三步：为现有数据生成临时BIGINT ID用于迁移
UPDATE t_ddh_cluster_service_command SET id = ROWNUM + 1000000;

-- 第四步：填充映射关系
INSERT INTO temp_command_id_mapping (old_command_id, new_id)
SELECT command_id, id FROM t_ddh_cluster_service_command;

-- 第五步：更新关联表中的外键引用
UPDATE t_ddh_cluster_service_command_host h
SET h.command_id = (
    SELECT CAST(m.new_id AS VARCHAR(128))
    FROM temp_command_id_mapping m 
    WHERE h.command_id = m.old_command_id
);

UPDATE t_ddh_cluster_service_command_host_command hc
SET hc.command_id = (
    SELECT CAST(m.new_id AS VARCHAR(128))
    FROM temp_command_id_mapping m 
    WHERE hc.command_id = m.old_command_id
);

-- 第六步：删除原主键约束并重建
ALTER TABLE t_ddh_cluster_service_command DROP CONSTRAINT PK_CLUSTER_SERVICE_COMMAND;
ALTER TABLE t_ddh_cluster_service_command DROP COLUMN command_id;
ALTER TABLE t_ddh_cluster_service_command ADD CONSTRAINT PK_CLUSTER_SERVICE_COMMAND PRIMARY KEY (id);

-- 添加缺失的审计字段
ALTER TABLE t_ddh_cluster_service_command ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_service_command ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 1.2 t_ddh_cluster_service_command_host表重构
-- 第一步：添加新的BIGINT id列
ALTER TABLE t_ddh_cluster_service_command_host ADD id BIGINT;

-- 第二步：创建临时映射表
CREATE GLOBAL TEMPORARY TABLE temp_command_host_id_mapping (
    old_command_host_id VARCHAR(128),
    new_id BIGINT
) ON COMMIT DELETE ROWS;

-- 第三步：为现有数据生成临时BIGINT ID用于迁移
UPDATE t_ddh_cluster_service_command_host SET id = ROWNUM + 2000000;

-- 第四步：填充映射关系
INSERT INTO temp_command_host_id_mapping (old_command_host_id, new_id)
SELECT command_host_id, id FROM t_ddh_cluster_service_command_host;

-- 第五步：更新关联表中的外键引用
UPDATE t_ddh_cluster_service_command_host_command hc
SET hc.command_host_id = (
    SELECT CAST(m.new_id AS VARCHAR(128))
    FROM temp_command_host_id_mapping m 
    WHERE hc.command_host_id = m.old_command_host_id
);

-- 第六步：删除原主键约束并重建
ALTER TABLE t_ddh_cluster_service_command_host DROP CONSTRAINT PK_CLUSTER_SERVICE_COMMAND_HOST;
ALTER TABLE t_ddh_cluster_service_command_host DROP COLUMN command_host_id;
ALTER TABLE t_ddh_cluster_service_command_host ADD CONSTRAINT PK_CLUSTER_SERVICE_COMMAND_HOST PRIMARY KEY (id);

-- 添加缺失的审计字段
ALTER TABLE t_ddh_cluster_service_command_host ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_service_command_host ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_service_command_host ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 修正外键字段类型为BIGINT
ALTER TABLE t_ddh_cluster_service_command_host MODIFY command_id BIGINT; -- 操作指令id（逻辑外键）

-- 1.3 t_ddh_cluster_service_command_host_command表重构
-- 第一步：添加新的BIGINT id列
ALTER TABLE t_ddh_cluster_service_command_host_command ADD id BIGINT;

-- 第二步：为现有数据生成临时BIGINT ID用于迁移
UPDATE t_ddh_cluster_service_command_host_command SET id = ROWNUM + 3000000;

-- 第三步：删除原主键约束并重建
ALTER TABLE t_ddh_cluster_service_command_host_command DROP CONSTRAINT PK_CLUSTER_SERVICE_COMMAND_HOST_COMMAND;
ALTER TABLE t_ddh_cluster_service_command_host_command DROP COLUMN host_command_id;
ALTER TABLE t_ddh_cluster_service_command_host_command ADD CONSTRAINT PK_CLUSTER_SERVICE_COMMAND_HOST_COMMAND PRIMARY KEY (id);

-- 添加缺失的审计字段
ALTER TABLE t_ddh_cluster_service_command_host_command ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_service_command_host_command ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_service_command_host_command ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 修正外键字段类型为BIGINT
ALTER TABLE t_ddh_cluster_service_command_host_command MODIFY command_host_id BIGINT; -- 主机id（逻辑外键）
ALTER TABLE t_ddh_cluster_service_command_host_command MODIFY command_id BIGINT; -- 命令id（逻辑外键）

-- 清理临时映射表
DROP TABLE temp_command_id_mapping;
DROP TABLE temp_command_host_id_mapping;

-- 2. 复合主键表重构
-- 2.1 t_ddh_config_version_info表重构
-- 第一步：添加新的BIGINT id列
ALTER TABLE t_ddh_config_version_info ADD id BIGINT;

-- 第二步：为现有数据生成临时BIGINT ID用于迁移
UPDATE t_ddh_config_version_info SET id = ROWNUM + 4000000;

-- 第三步：删除原复合主键并重建
ALTER TABLE t_ddh_config_version_info DROP CONSTRAINT PK_CONFIG_VERSION_INFO;
ALTER TABLE t_ddh_config_version_info ADD CONSTRAINT PK_CONFIG_VERSION_INFO PRIMARY KEY (id);
ALTER TABLE t_ddh_config_version_info ADD CONSTRAINT UK_VERSION_REF UNIQUE (version, ref_type, ref_id);

-- 添加审计字段
ALTER TABLE t_ddh_config_version_info ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_config_version_info ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_config_version_info ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_config_version_info ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 3. 特殊表处理
-- 3.1 misval表重构
-- 重命名Id列为id并修改类型
ALTER TABLE misval RENAME COLUMN Id TO id;
ALTER TABLE misval MODIFY id BIGINT; -- 主键
-- 为id列填充临时BIGINT ID用于迁移（如果有空值或重复值）
UPDATE misval SET id = ROWNUM + 5000000 WHERE id IS NULL OR id = 0;
-- 添加主键约束
ALTER TABLE misval ADD CONSTRAINT PK_MISVAL PRIMARY KEY (id);

-- 添加审计字段
ALTER TABLE misval ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE misval ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE misval ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE misval ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 4. 统一所有表主键类型为BIGINT（批量处理，使用雪花算法）
-- 4.1 修改现有INT主键为BIGINT
ALTER TABLE t_ddh_alert_group MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_alert_group_map MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_alert_history MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_alert_quota MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_group MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_host MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_info MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_node_label MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_queue_capacity MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_rack MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_role_user MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_service_dashboard MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_service_instance MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_service_instance_role_group MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_service_role_group_config MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_service_role_instance MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_service_role_instance_webuis MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_tenant MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_user MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_user_group MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_user_tenant MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_variable MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_yarn_queue MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_yarn_scheduler MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_cluster_zk MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_command MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_frame_info MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_frame_service MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_frame_service_role MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_install_step MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_notice_group MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_notice_group_user MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_operation_log MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_role_info MODIFY id BIGINT; -- 主键
ALTER TABLE t_ddh_user_info MODIFY id BIGINT; -- 主键

-- 4.2 【关键修复】统一所有外键字段类型为BIGINT
-- 修复frame_id字段（框架ID外键）
ALTER TABLE t_ddh_cluster_info MODIFY frame_id BIGINT; -- 框架ID
ALTER TABLE t_ddh_frame_service MODIFY frame_id BIGINT; -- 框架ID

-- 修复service_id字段（服务ID外键）
ALTER TABLE t_ddh_cluster_service_role_instance MODIFY service_id BIGINT; -- 服务ID
ALTER TABLE t_ddh_frame_service_role MODIFY service_id BIGINT; -- 服务ID

-- 修复cluster_id字段（集群ID外键）
ALTER TABLE t_ddh_cluster_alert_group_map MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_alert_history MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_group MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_host MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_node_label MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_queue_capacity MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_rack MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_role_user MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_service_instance MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_service_instance_role_group MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_service_role_group_config MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_service_role_instance MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_user MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_user_group MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_user_tenant MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_variable MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_yarn_queue MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_yarn_scheduler MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_cluster_zk MODIFY cluster_id BIGINT; -- 集群ID
ALTER TABLE t_ddh_operation_log MODIFY cluster_id BIGINT; -- 集群ID

-- 修复user_id字段（用户ID外键）
ALTER TABLE t_ddh_cluster_role_user MODIFY user_id BIGINT; -- 用户ID
ALTER TABLE t_ddh_cluster_user_group MODIFY user_id BIGINT; -- 用户ID
ALTER TABLE t_ddh_cluster_user_tenant MODIFY user_id BIGINT; -- 用户ID
ALTER TABLE t_ddh_notice_group_user MODIFY user_id BIGINT; -- 用户ID

-- 修复alert_group_id字段（告警组ID外键）
ALTER TABLE t_ddh_cluster_alert_group_map MODIFY alert_group_id BIGINT; -- 告警组ID
ALTER TABLE t_ddh_cluster_alert_quota MODIFY alert_group_id BIGINT; -- 告警组ID

-- 修复notice_group_id字段（通知组ID外键）
ALTER TABLE t_ddh_cluster_alert_quota MODIFY notice_group_id BIGINT; -- 通知组ID
ALTER TABLE t_ddh_notice_group_user MODIFY notice_group_id BIGINT; -- 通知组ID

-- 修复service_instance_id字段（服务实例ID外键）
ALTER TABLE t_ddh_cluster_service_command MODIFY service_instance_id BIGINT; -- 服务实例ID
ALTER TABLE t_ddh_cluster_alert_history MODIFY service_instance_id BIGINT; -- 服务实例ID
ALTER TABLE t_ddh_cluster_service_instance_role_group MODIFY service_instance_id BIGINT; -- 服务实例ID
ALTER TABLE t_ddh_cluster_service_role_instance_webuis MODIFY service_instance_id BIGINT; -- 服务实例ID

-- 修复service_role_instance_id字段（服务角色实例ID外键）
ALTER TABLE t_ddh_cluster_alert_history MODIFY service_role_instance_id BIGINT; -- 服务角色实例ID
ALTER TABLE t_ddh_cluster_service_role_instance_webuis MODIFY service_role_instance_id BIGINT; -- 服务角色实例ID

-- 修复role_group_id字段（角色组ID外键）
ALTER TABLE t_ddh_cluster_service_role_group_config MODIFY role_group_id BIGINT; -- 角色组ID
ALTER TABLE t_ddh_cluster_service_role_instance MODIFY role_group_id BIGINT; -- 角色组ID

-- 修复frame_service_id字段（框架服务ID外键）
ALTER TABLE t_ddh_cluster_service_instance MODIFY frame_service_id BIGINT; -- 框架服务ID

-- 修复group_id字段（组ID外键）
ALTER TABLE t_ddh_cluster_user_group MODIFY group_id BIGINT; -- 组ID

-- 修复tenant_id字段（租户ID外键）
ALTER TABLE t_ddh_cluster_user_tenant MODIFY tenant_id BIGINT; -- 租户ID

-- 修复expression_id字段（表达式ID外键）
ALTER TABLE t_ddh_cluster_alert_rule MODIFY expression_id BIGINT; -- 表达式ID

-- 修复receiver_group_id字段（接收组ID外键）
ALTER TABLE t_ddh_cluster_alert_rule MODIFY receiver_group_id BIGINT; -- 接收组ID

-- 为t_ddh_operation_log表添加标准审计字段
ALTER TABLE t_ddh_operation_log ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_operation_log ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_operation_log ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_operation_log ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 5. 批量添加缺失的审计字段（达梦数据库语法）
-- 5.1 完全缺失审计字段的表（批量添加全部4个字段）
ALTER TABLE t_ddh_cluster_alert_group_map ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_alert_group_map ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_alert_group_map ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_alert_group_map ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_group ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_group ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_group ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_group ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_node_label ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_node_label ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_node_label ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_node_label ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_queue_capacity ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_queue_capacity ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_queue_capacity ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_queue_capacity ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_rack ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_rack ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_rack ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_rack ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_role_user ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_role_user ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_role_user ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_role_user ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_service_dashboard ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_service_dashboard ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_service_dashboard ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_service_dashboard ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_service_role_instance_webuis ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_service_role_instance_webuis ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_service_role_instance_webuis ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_service_role_instance_webuis ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_tenant ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_tenant ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_tenant ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_tenant ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_user ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_user ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_user ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_user ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_user_group ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_user_group ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_user_group ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_user_group ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_user_tenant ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_user_tenant ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_user_tenant ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_user_tenant ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_variable ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_variable ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_variable ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_variable ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_yarn_scheduler ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_yarn_scheduler ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_yarn_scheduler ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_yarn_scheduler ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_zk ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_cluster_zk ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_zk ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_zk ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_command ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_command ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_command ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_command ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_frame_info ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_frame_info ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_frame_info ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_frame_info ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_frame_service ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_frame_service ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_frame_service ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_frame_service ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_frame_service_role ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_frame_service_role ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_frame_service_role ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_frame_service_role ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_install_step ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_install_step ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_install_step ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_install_step ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_notice_group_user ADD create_time DATETIME DEFAULT SYSDATE; -- 创建时间
ALTER TABLE t_ddh_notice_group_user ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_notice_group_user ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_notice_group_user ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 5.2 部分缺失审计字段的表（补充缺失字段）
-- t_ddh_alert_group: 只有create_time，需要添加其他3个字段
ALTER TABLE t_ddh_alert_group ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_alert_group ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_alert_group ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- t_ddh_cluster_alert_quota: 只有create_time，需要添加其他3个字段
ALTER TABLE t_ddh_cluster_alert_quota ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_alert_quota ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_alert_quota ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- t_ddh_cluster_host: 只有create_time，需要添加其他3个字段
ALTER TABLE t_ddh_cluster_host ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_host ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_host ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- t_ddh_cluster_info: 有create_time和create_by，需要添加update_time和update_by
ALTER TABLE t_ddh_cluster_info ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_info ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- t_ddh_cluster_service_instance_role_group: 只有create_time，需要添加其他3个字段
ALTER TABLE t_ddh_cluster_service_instance_role_group ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_service_instance_role_group ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_service_instance_role_group ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- t_ddh_cluster_yarn_queue: 只有create_time，需要添加其他3个字段
ALTER TABLE t_ddh_cluster_yarn_queue ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_cluster_yarn_queue ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_yarn_queue ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- t_ddh_notice_group: 只有create_time，需要添加其他3个字段
ALTER TABLE t_ddh_notice_group ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_notice_group ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_notice_group ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- t_ddh_role_info: 只有create_time，需要添加其他3个字段
ALTER TABLE t_ddh_role_info ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_role_info ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_role_info ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- t_ddh_user_info: 只有create_time，需要添加其他3个字段
ALTER TABLE t_ddh_user_info ADD update_time DATETIME DEFAULT SYSDATE; -- 更新时间
ALTER TABLE t_ddh_user_info ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_user_info ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 5.3 已有create_time和update_time但缺少create_by和update_by的表
ALTER TABLE t_ddh_cluster_alert_expression ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_alert_expression ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_alert_history ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_alert_history ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_alert_rule ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_alert_rule ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_service_instance ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_service_instance ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_service_role_group_config ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_service_role_group_config ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

ALTER TABLE t_ddh_cluster_service_role_instance ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_cluster_service_role_instance ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 5.4 t_ddh_auth_token表字段标准化（达梦数据库版本）
-- 该表需要将非标准字段名称修改为标准审计字段名称
-- 步骤1：重命名现有时间字段为标准名称
ALTER TABLE t_ddh_auth_token RENAME COLUMN created_at TO create_time;
ALTER TABLE t_ddh_auth_token RENAME COLUMN updated_at TO update_time;

-- 步骤2：添加缺失的审计字段
ALTER TABLE t_ddh_auth_token ADD create_by VARCHAR(128) DEFAULT NULL; -- 创建人
ALTER TABLE t_ddh_auth_token ADD update_by VARCHAR(128) DEFAULT NULL; -- 更新人

-- 6. 索引优化 - 为重要审计字段添加索引（提升查询性能）
CREATE INDEX idx_cluster_alert_history_create_time ON t_ddh_cluster_alert_history (create_time);
CREATE INDEX idx_cluster_alert_history_create_by ON t_ddh_cluster_alert_history (create_by);
CREATE INDEX idx_cluster_host_create_time ON t_ddh_cluster_host (create_time);
CREATE INDEX idx_cluster_host_create_by ON t_ddh_cluster_host (create_by);
CREATE INDEX idx_operation_log_create_time ON t_ddh_operation_log (create_time);
CREATE INDEX idx_operation_log_create_by ON t_ddh_operation_log (create_by);

-- =============================================================================
-- 数据完整性说明（达梦数据库版本）
-- =============================================================================

-- 注意：由于达梦数据库语法限制，以下验证查询在实际环境中执行：
-- 1. 验证所有表都有标准审计字段
-- 2. 验证所有主键都是BIGINT类型
-- 3. 验证外键关联数据完整性

-- DM数据库标准化完成！