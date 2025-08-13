-- V3.0.0.3 数据库表结构标准化重构
-- 作者：任相鹏
-- 邮箱：635887935@qq.com  
-- 日期：2025-08-13
-- 描述：统一数据库表结构设计，实现标准化主键和审计字段，建立规范的逻辑外键关系

-- =============================================================================
-- DDL部分：数据库结构变更
-- =============================================================================

-- 1. 处理VARCHAR主键表的重构
-- 1.1 t_ddh_cluster_service_command表重构
ALTER TABLE `t_ddh_cluster_service_command` ADD COLUMN `id` BIGINT AUTO_INCREMENT FIRST;
ALTER TABLE `t_ddh_cluster_service_command` DROP PRIMARY KEY;
ALTER TABLE `t_ddh_cluster_service_command` ADD PRIMARY KEY (`id`);
ALTER TABLE `t_ddh_cluster_service_command` CHANGE COLUMN `command_id` `command_id` VARCHAR(128) NOT NULL COMMENT '命令标识（保留业务字段）';
ALTER TABLE `t_ddh_cluster_service_command` ADD UNIQUE KEY `uk_command_id` (`command_id`);

-- 添加缺失的审计字段
ALTER TABLE `t_ddh_cluster_service_command` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_service_command` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 1.2 t_ddh_cluster_service_command_host表重构
ALTER TABLE `t_ddh_cluster_service_command_host` ADD COLUMN `id` BIGINT AUTO_INCREMENT FIRST;
ALTER TABLE `t_ddh_cluster_service_command_host` DROP PRIMARY KEY;
ALTER TABLE `t_ddh_cluster_service_command_host` ADD PRIMARY KEY (`id`);
ALTER TABLE `t_ddh_cluster_service_command_host` CHANGE COLUMN `command_host_id` `command_host_id` VARCHAR(128) NOT NULL COMMENT '主机命令标识（保留业务字段）';
ALTER TABLE `t_ddh_cluster_service_command_host` ADD UNIQUE KEY `uk_command_host_id` (`command_host_id`);

-- 添加缺失的审计字段
ALTER TABLE `t_ddh_cluster_service_command_host` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_service_command_host` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_service_command_host` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 1.3 t_ddh_cluster_service_command_host_command表重构
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD COLUMN `id` BIGINT AUTO_INCREMENT FIRST;
ALTER TABLE `t_ddh_cluster_service_command_host_command` DROP PRIMARY KEY;
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD PRIMARY KEY (`id`);
ALTER TABLE `t_ddh_cluster_service_command_host_command` CHANGE COLUMN `host_command_id` `host_command_id` VARCHAR(128) NOT NULL COMMENT '主机命令标识（保留业务字段）';
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD UNIQUE KEY `uk_host_command_id` (`host_command_id`);

-- 添加缺失的审计字段
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 2. 处理复合主键表重构
-- 2.1 t_ddh_config_version_info表重构
ALTER TABLE `t_ddh_config_version_info` DROP PRIMARY KEY;
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `id` BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE `t_ddh_config_version_info` ADD UNIQUE KEY `uk_version_ref` (`version`, `ref_type`, `ref_id`);

-- 添加审计字段
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';  
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 3. 统一所有表主键类型为BIGINT
-- 3.1 修改现有INT主键为BIGINT
ALTER TABLE `t_ddh_alert_group` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_alert_group_map` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_alert_history` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_alert_quota` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_group` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_host` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_info` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_node_label` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_queue_capacity` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_rack` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_role_user` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_dashboard` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_instance` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_instance_role_group` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_role_group_config` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_role_instance` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_role_instance_webuis` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_tenant` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_user` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_user_group` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_user_tenant` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_variable` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_yarn_queue` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_yarn_scheduler` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_zk` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_command` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_frame_info` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_frame_service` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_frame_service_role` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_install_step` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_notice_group` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_notice_group_user` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_operation_log` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_role_info` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `t_ddh_user_info` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';

-- 4. 为完全缺失审计字段的表添加审计字段
-- 4.1 misval表
ALTER TABLE `misval` MODIFY COLUMN `Id` BIGINT COMMENT '主键';
ALTER TABLE `misval` CHANGE COLUMN `Id` `id` BIGINT COMMENT '主键';
ALTER TABLE `misval` ADD PRIMARY KEY (`id`);
ALTER TABLE `misval` MODIFY COLUMN `id` BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `misval` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `misval` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `misval` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `misval` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 4.2 t_ddh_auth_token表（已有BIGINT主键）
ALTER TABLE `t_ddh_auth_token` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_auth_token` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_auth_token` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_auth_token` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 4.3 其他完全缺失审计字段的表
ALTER TABLE `t_ddh_cluster_alert_group_map` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_alert_group_map` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_alert_group_map` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_alert_group_map` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_group` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_group` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_group` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_group` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_node_label` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_node_label` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_node_label` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_node_label` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_queue_capacity` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_queue_capacity` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_queue_capacity` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_queue_capacity` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_rack` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_rack` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_rack` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_rack` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_role_user` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_role_user` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_role_user` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_role_user` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_service_dashboard` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_service_dashboard` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_service_dashboard` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_service_dashboard` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_service_role_instance_webuis` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_service_role_instance_webuis` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_service_role_instance_webuis` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_service_role_instance_webuis` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_tenant` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_tenant` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_tenant` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_tenant` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_user` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_user` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_user` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_user` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_user_group` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_user_group` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_user_group` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_user_group` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_user_tenant` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_user_tenant` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_user_tenant` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_user_tenant` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_variable` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_variable` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_variable` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_variable` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_yarn_scheduler` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_yarn_scheduler` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_yarn_scheduler` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_yarn_scheduler` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_zk` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_cluster_zk` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_zk` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_zk` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_command` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_command` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_command` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_command` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_frame_info` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_frame_info` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_frame_info` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_frame_info` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_frame_service` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_frame_service` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_frame_service` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_frame_service` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_frame_service_role` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_frame_service_role` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_frame_service_role` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_frame_service_role` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_install_step` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_install_step` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_install_step` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_install_step` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_notice_group_user` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_notice_group_user` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_notice_group_user` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_notice_group_user` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_operation_log` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_operation_log` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_operation_log` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_operation_log` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 5. 为部分缺失审计字段的表补充字段
-- 5.1 只有create_time的表，添加缺失的字段
ALTER TABLE `t_ddh_alert_group` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_alert_group` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_alert_group` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_alert_quota` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_alert_quota` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_alert_quota` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_host` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_host` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_host` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- t_ddh_cluster_info 已有 create_time 和 create_by，只需添加 update_time 和 update_by
ALTER TABLE `t_ddh_cluster_info` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_info` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- t_ddh_cluster_service_command 已有 create_time 和 create_by，在上面已添加 update_time 和 update_by

ALTER TABLE `t_ddh_cluster_service_instance_role_group` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_service_instance_role_group` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_service_instance_role_group` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_yarn_queue` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_cluster_yarn_queue` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_yarn_queue` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_notice_group` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_notice_group` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_notice_group` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_role_info` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_role_info` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_role_info` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_user_info` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `t_ddh_user_info` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_user_info` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 5.2 已有create_time和update_time但缺少create_by和update_by的表
ALTER TABLE `t_ddh_cluster_alert_expression` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_alert_expression` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_alert_history` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_alert_history` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_alert_rule` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_alert_rule` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_service_instance` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_service_instance` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_service_role_group_config` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_service_role_group_config` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_service_role_instance` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `t_ddh_cluster_service_role_instance` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 6. 索引优化 - 为新增审计字段添加合适索引
CREATE INDEX `idx_create_time` ON `t_ddh_alert_group` (`create_time`);
CREATE INDEX `idx_create_by` ON `t_ddh_alert_group` (`create_by`);

CREATE INDEX `idx_create_time` ON `t_ddh_cluster_host` (`create_time`);
CREATE INDEX `idx_create_by` ON `t_ddh_cluster_host` (`create_by`);

CREATE INDEX `idx_create_time` ON `t_ddh_operation_log` (`create_time`);
CREATE INDEX `idx_create_by` ON `t_ddh_operation_log` (`create_by`);

-- =============================================================================
-- 数据完整性检查查询（用于验证迁移结果）
-- =============================================================================

-- 验证所有表都有标准审计字段的查询（注释掉，需要时可执行）
-- SELECT 'Tables missing standard audit fields:' as verification_type;
-- SELECT TABLE_NAME as missing_audit_fields_table
-- FROM INFORMATION_SCHEMA.TABLES 
-- WHERE TABLE_SCHEMA = 'datasophon2' 
--   AND TABLE_TYPE = 'BASE TABLE'
--   AND TABLE_NAME LIKE 't_ddh_%'
--   AND TABLE_NAME NOT IN (
--     SELECT DISTINCT t1.TABLE_NAME 
--     FROM INFORMATION_SCHEMA.COLUMNS t1
--     JOIN INFORMATION_SCHEMA.COLUMNS t2 ON t1.TABLE_NAME = t2.TABLE_NAME
--     JOIN INFORMATION_SCHEMA.COLUMNS t3 ON t1.TABLE_NAME = t3.TABLE_NAME  
--     JOIN INFORMATION_SCHEMA.COLUMNS t4 ON t1.TABLE_NAME = t4.TABLE_NAME
--     JOIN INFORMATION_SCHEMA.COLUMNS t5 ON t1.TABLE_NAME = t5.TABLE_NAME
--     WHERE t1.TABLE_SCHEMA = 'datasophon2' AND t1.COLUMN_NAME = 'id'
--       AND t2.TABLE_SCHEMA = 'datasophon2' AND t2.COLUMN_NAME = 'create_time'
--       AND t3.TABLE_SCHEMA = 'datasophon2' AND t3.COLUMN_NAME = 'update_time'
--       AND t4.TABLE_SCHEMA = 'datasophon2' AND t4.COLUMN_NAME = 'create_by'
--       AND t5.TABLE_SCHEMA = 'datasophon2' AND t5.COLUMN_NAME = 'update_by'
--   );

-- 验证所有主键都是BIGINT类型的查询（注释掉，需要时可执行）
-- SELECT 'Tables with non-BIGINT primary keys:' as verification_type;
-- SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE as non_bigint_pk_table
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = 'datasophon2' 
--   AND COLUMN_KEY = 'PRI' 
--   AND DATA_TYPE != 'bigint';
