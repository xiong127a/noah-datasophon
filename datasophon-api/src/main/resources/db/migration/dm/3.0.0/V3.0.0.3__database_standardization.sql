-- V3.0.0.3 数据库表结构标准化重构 (DM数据库版本)
-- 作者：任相鹏
-- 邮箱：635887935@qq.com  
-- 日期：2025-08-13
-- 描述：统一数据库表结构设计，实现标准化主键和审计字段，建立规范的逻辑外键关系

-- =============================================================================
-- DDL部分：数据库结构变更 (适配DM数据库语法)
-- =============================================================================

-- 1. 处理VARCHAR主键表的重构
-- 1.1 t_ddh_cluster_service_command表重构
ALTER TABLE "t_ddh_cluster_service_command" ADD COLUMN "id" BIGINT IDENTITY(1,1);
ALTER TABLE "t_ddh_cluster_service_command" DROP CONSTRAINT PRIMARY KEY;
ALTER TABLE "t_ddh_cluster_service_command" ADD CONSTRAINT PK_cluster_service_command PRIMARY KEY ("id");
ALTER TABLE "t_ddh_cluster_service_command" ALTER COLUMN "command_id" VARCHAR(128) NOT NULL COMMENT '命令标识（保留业务字段）';
ALTER TABLE "t_ddh_cluster_service_command" ADD CONSTRAINT UK_command_id UNIQUE ("command_id");

-- 添加缺失的审计字段
ALTER TABLE "t_ddh_cluster_service_command" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_service_command" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 1.2 t_ddh_cluster_service_command_host表重构
ALTER TABLE "t_ddh_cluster_service_command_host" ADD COLUMN "id" BIGINT IDENTITY(1,1);
ALTER TABLE "t_ddh_cluster_service_command_host" DROP CONSTRAINT PRIMARY KEY;
ALTER TABLE "t_ddh_cluster_service_command_host" ADD CONSTRAINT PK_cluster_service_command_host PRIMARY KEY ("id");
ALTER TABLE "t_ddh_cluster_service_command_host" ALTER COLUMN "command_host_id" VARCHAR(128) NOT NULL COMMENT '主机命令标识（保留业务字段）';
ALTER TABLE "t_ddh_cluster_service_command_host" ADD CONSTRAINT UK_command_host_id UNIQUE ("command_host_id");

-- 添加缺失的审计字段
ALTER TABLE "t_ddh_cluster_service_command_host" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_service_command_host" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_service_command_host" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 1.3 t_ddh_cluster_service_command_host_command表重构
ALTER TABLE "t_ddh_cluster_service_command_host_command" ADD COLUMN "id" BIGINT IDENTITY(1,1);
ALTER TABLE "t_ddh_cluster_service_command_host_command" DROP CONSTRAINT PRIMARY KEY;
ALTER TABLE "t_ddh_cluster_service_command_host_command" ADD CONSTRAINT PK_cluster_service_command_host_command PRIMARY KEY ("id");
ALTER TABLE "t_ddh_cluster_service_command_host_command" ALTER COLUMN "host_command_id" VARCHAR(128) NOT NULL COMMENT '主机命令标识（保留业务字段）';
ALTER TABLE "t_ddh_cluster_service_command_host_command" ADD CONSTRAINT UK_host_command_id UNIQUE ("host_command_id");

-- 添加缺失的审计字段
ALTER TABLE "t_ddh_cluster_service_command_host_command" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_service_command_host_command" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_service_command_host_command" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 2. 处理复合主键表重构
-- 2.1 t_ddh_config_version_info表重构
ALTER TABLE "t_ddh_config_version_info" DROP CONSTRAINT PRIMARY KEY;
ALTER TABLE "t_ddh_config_version_info" ADD COLUMN "id" BIGINT IDENTITY(1,1);
ALTER TABLE "t_ddh_config_version_info" ADD CONSTRAINT PK_config_version_info PRIMARY KEY ("id");
ALTER TABLE "t_ddh_config_version_info" ADD CONSTRAINT UK_version_ref UNIQUE ("version", "ref_type", "ref_id");

-- 添加审计字段
ALTER TABLE "t_ddh_config_version_info" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_config_version_info" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_config_version_info" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_config_version_info" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 3. 统一所有表主键类型为BIGINT
-- 3.1 修改现有INT主键为BIGINT (DM语法)
ALTER TABLE "t_ddh_alert_group" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_alert_group_map" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_alert_history" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_alert_quota" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_group" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_host" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_info" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_node_label" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_queue_capacity" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_rack" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_role_user" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_service_dashboard" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_service_instance" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_service_instance_role_group" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_service_role_group_config" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_service_role_instance" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_service_role_instance_webuis" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_tenant" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_user" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_user_group" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_user_tenant" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_variable" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_yarn_queue" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_yarn_scheduler" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_cluster_zk" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_command" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_frame_info" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_frame_service" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_frame_service_role" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_install_step" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_notice_group" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_notice_group_user" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_operation_log" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_role_info" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "t_ddh_user_info" ALTER COLUMN "id" BIGINT IDENTITY(1,1) COMMENT '主键';

-- 4. 为完全缺失审计字段的表添加审计字段
-- 4.1 misval表
ALTER TABLE "misval" ALTER COLUMN "Id" "id" BIGINT IDENTITY(1,1) COMMENT '主键';
ALTER TABLE "misval" ADD CONSTRAINT PK_misval PRIMARY KEY ("id");
ALTER TABLE "misval" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "misval" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "misval" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "misval" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 4.2 t_ddh_auth_token表（已有BIGINT主键）
ALTER TABLE "t_ddh_auth_token" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_auth_token" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_auth_token" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_auth_token" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 4.3 其他完全缺失审计字段的表
ALTER TABLE "t_ddh_cluster_alert_group_map" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_alert_group_map" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_alert_group_map" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_alert_group_map" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_group" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_group" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_group" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_group" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_node_label" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_node_label" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_node_label" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_node_label" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_queue_capacity" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_queue_capacity" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_queue_capacity" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_queue_capacity" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_rack" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_rack" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_rack" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_rack" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_role_user" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_role_user" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_role_user" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_role_user" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_service_dashboard" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_service_dashboard" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_service_dashboard" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_service_dashboard" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_service_role_instance_webuis" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_service_role_instance_webuis" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_service_role_instance_webuis" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_service_role_instance_webuis" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_tenant" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_tenant" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_tenant" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_tenant" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_user" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_user" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_user" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_user" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_user_group" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_user_group" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_user_group" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_user_group" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_user_tenant" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_user_tenant" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_user_tenant" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_user_tenant" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_variable" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_variable" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_variable" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_variable" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_yarn_scheduler" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_yarn_scheduler" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_yarn_scheduler" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_yarn_scheduler" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_zk" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_cluster_zk" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_zk" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_zk" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_command" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_command" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_command" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_command" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_frame_info" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_frame_info" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_frame_info" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_frame_info" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_frame_service" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_frame_service" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_frame_service" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_frame_service" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_frame_service_role" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_frame_service_role" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_frame_service_role" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_frame_service_role" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_install_step" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_install_step" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_install_step" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_install_step" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_notice_group_user" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_notice_group_user" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_notice_group_user" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_notice_group_user" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_operation_log" ADD COLUMN "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE "t_ddh_operation_log" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_operation_log" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_operation_log" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 5. 为部分缺失审计字段的表补充字段
-- 5.1 只有create_time的表，添加缺失的字段
ALTER TABLE "t_ddh_alert_group" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_alert_group" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_alert_group" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_alert_quota" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_alert_quota" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_alert_quota" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_host" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_host" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_host" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- t_ddh_cluster_info 已有 create_time 和 create_by，只需添加 update_time 和 update_by
ALTER TABLE "t_ddh_cluster_info" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_info" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_service_instance_role_group" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_service_instance_role_group" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_service_instance_role_group" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_yarn_queue" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_cluster_yarn_queue" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_yarn_queue" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_notice_group" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_notice_group" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_notice_group" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_role_info" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_role_info" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_role_info" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_user_info" ADD COLUMN "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE "t_ddh_user_info" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_user_info" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 5.2 已有create_time和update_time但缺少create_by和update_by的表
ALTER TABLE "t_ddh_cluster_alert_expression" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_alert_expression" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_alert_history" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_alert_history" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_alert_rule" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_alert_rule" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_service_instance" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_service_instance" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_service_role_group_config" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_service_role_group_config" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE "t_ddh_cluster_service_role_instance" ADD COLUMN "create_by" VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE "t_ddh_cluster_service_role_instance" ADD COLUMN "update_by" VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 6. 索引优化 - 为新增审计字段添加合适索引
CREATE INDEX IDX_alert_group_create_time ON "t_ddh_alert_group" ("create_time");
CREATE INDEX IDX_alert_group_create_by ON "t_ddh_alert_group" ("create_by");

CREATE INDEX IDX_cluster_host_create_time ON "t_ddh_cluster_host" ("create_time");
CREATE INDEX IDX_cluster_host_create_by ON "t_ddh_cluster_host" ("create_by");

CREATE INDEX IDX_operation_log_create_time ON "t_ddh_operation_log" ("create_time");
CREATE INDEX IDX_operation_log_create_by ON "t_ddh_operation_log" ("create_by");

-- =============================================================================
-- 数据完整性检查查询（用于验证迁移结果）- DM语法版本
-- =============================================================================

-- 注释：DM数据库的验证查询与MySQL略有差异，使用时请取消注释
-- COMMENT '验证所有表都有标准审计字段';
-- COMMENT '验证所有主键都是BIGINT类型';
