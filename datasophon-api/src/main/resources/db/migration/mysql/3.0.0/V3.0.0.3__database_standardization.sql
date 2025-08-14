-- V3.0.0.3 数据库表结构标准化重构
-- 作者：任相鹏
-- 邮箱：635887935@qq.com  
-- 日期：2025-01-31
-- 描述：统一数据库表结构设计，实现标准化主键和审计字段，建立规范的逻辑外键关系
-- 
-- ⚠️ 重要说明：
-- 1. 本迁移移除了所有AUTO_INCREMENT设置，因为系统使用雪花算法生成ID
-- 2. 为保证迁移过程中数据完整性，使用临时递增ID建立映射关系
-- 3. 迁移完成后，应用层将使用雪花算法为新数据生成正确的BIGINT ID
-- 4. 现有数据的临时ID不影响业务逻辑，外键关联关系已正确迁移
-- 5. 整个迁移过程在事务中执行，确保原子性操作

-- 开始事务（确保迁移的原子性）
START TRANSACTION;

-- =============================================================================
-- DDL部分：数据库结构变更
-- =============================================================================

-- 1. VARCHAR主键表重构（保证数据完整性）
-- 1.1 t_ddh_cluster_service_command表重构
-- 第一步：添加新的BIGINT id列（不设置AUTO_INCREMENT）
ALTER TABLE `t_ddh_cluster_service_command` ADD COLUMN `id` BIGINT FIRST;

-- 第二步：创建临时映射表保存新旧ID关系
CREATE TEMPORARY TABLE temp_command_id_mapping (
    old_command_id VARCHAR(128),
    new_id BIGINT,
    INDEX idx_old_id (old_command_id),
    INDEX idx_new_id (new_id)
);

-- 第三步：为现有数据生成临时BIGINT ID用于迁移
SET @row_number = 0;
UPDATE `t_ddh_cluster_service_command` SET `id` = (@row_number:=@row_number+1000000);

-- 第四步：填充映射关系
INSERT INTO temp_command_id_mapping (old_command_id, new_id)
SELECT command_id, id FROM `t_ddh_cluster_service_command`;

-- 第五步：更新关联表中的外键引用
UPDATE `t_ddh_cluster_service_command_host` h
JOIN temp_command_id_mapping m ON h.command_id = m.old_command_id
SET h.command_id = CAST(m.new_id AS CHAR);

UPDATE `t_ddh_cluster_service_command_host_command` hc
JOIN temp_command_id_mapping m ON hc.command_id = m.old_command_id
SET hc.command_id = CAST(m.new_id AS CHAR);

-- 第六步：删除原主键约束并重建
ALTER TABLE `t_ddh_cluster_service_command` DROP INDEX `command_id`;
ALTER TABLE `t_ddh_cluster_service_command` DROP COLUMN `command_id`;
ALTER TABLE `t_ddh_cluster_service_command` ADD PRIMARY KEY (`id`);

-- 添加缺失的审计字段
ALTER TABLE `t_ddh_cluster_service_command` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;
ALTER TABLE `t_ddh_cluster_service_command` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- 1.2 t_ddh_cluster_service_command_host表重构
-- 第一步：添加新的BIGINT id列（不设置AUTO_INCREMENT）
ALTER TABLE `t_ddh_cluster_service_command_host` ADD COLUMN `id` BIGINT FIRST;

-- 第二步：创建临时映射表
CREATE TEMPORARY TABLE temp_command_host_id_mapping (
    old_command_host_id VARCHAR(128),
    new_id BIGINT,
    INDEX idx_old_id (old_command_host_id),
    INDEX idx_new_id (new_id)
);

-- 第三步：为现有数据生成临时BIGINT ID用于迁移
SET @row_number = 0;
UPDATE `t_ddh_cluster_service_command_host` SET `id` = (@row_number:=@row_number+2000000);

-- 第四步：填充映射关系
INSERT INTO temp_command_host_id_mapping (old_command_host_id, new_id)
SELECT command_host_id, id FROM `t_ddh_cluster_service_command_host`;

-- 第五步：更新关联表中的外键引用
UPDATE `t_ddh_cluster_service_command_host_command` hc
JOIN temp_command_host_id_mapping m ON hc.command_host_id = m.old_command_host_id
SET hc.command_host_id = CAST(m.new_id AS CHAR);

-- 第六步：删除原主键约束并重建
ALTER TABLE `t_ddh_cluster_service_command_host` DROP INDEX `command_host_id`;
ALTER TABLE `t_ddh_cluster_service_command_host` DROP COLUMN `command_host_id`;
ALTER TABLE `t_ddh_cluster_service_command_host` ADD PRIMARY KEY (`id`);

-- 添加缺失的审计字段
ALTER TABLE `t_ddh_cluster_service_command_host` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;
ALTER TABLE `t_ddh_cluster_service_command_host` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `t_ddh_cluster_service_command_host` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- 修正外键字段类型为BIGINT
ALTER TABLE `t_ddh_cluster_service_command_host` MODIFY COLUMN `command_id` BIGINT COMMENT '操作指令id（逻辑外键，关联t_ddh_cluster_service_command表的id）';

-- 1.3 t_ddh_cluster_service_command_host_command表重构
-- 第一步：添加新的BIGINT id列（不设置AUTO_INCREMENT）
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD COLUMN `id` BIGINT FIRST;

-- 第二步：为现有数据生成临时BIGINT ID用于迁移
SET @row_number = 0;
UPDATE `t_ddh_cluster_service_command_host_command` SET `id` = (@row_number:=@row_number+3000000);

-- 第三步：删除原主键约束并重建
ALTER TABLE `t_ddh_cluster_service_command_host_command` DROP INDEX `command_host_command_id`;
ALTER TABLE `t_ddh_cluster_service_command_host_command` DROP COLUMN `host_command_id`;
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD PRIMARY KEY (`id`);

-- 添加缺失的审计字段（该表已有create_time，只需添加缺失字段）
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `t_ddh_cluster_service_command_host_command` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- 修正外键字段类型为BIGINT
ALTER TABLE `t_ddh_cluster_service_command_host_command` MODIFY COLUMN `command_host_id` BIGINT COMMENT '主机id（逻辑外键，关联t_ddh_cluster_service_command_host表的id）';
ALTER TABLE `t_ddh_cluster_service_command_host_command` MODIFY COLUMN `command_id` BIGINT COMMENT '命令id（逻辑外键，关联t_ddh_cluster_service_command表的id）';

-- 清理临时映射表
DROP TEMPORARY TABLE temp_command_id_mapping;
DROP TEMPORARY TABLE temp_command_host_id_mapping;

-- 2. 复合主键表重构
-- 2.1 t_ddh_config_version_info表重构
-- 第一步：添加新的BIGINT id列（不设置AUTO_INCREMENT）
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `id` BIGINT FIRST;

-- 第二步：为现有数据生成临时BIGINT ID用于迁移
SET @row_number = 0;
UPDATE `t_ddh_config_version_info` SET `id` = (@row_number:=@row_number+4000000);

-- 第三步：删除原复合主键并重建
ALTER TABLE `t_ddh_config_version_info` DROP PRIMARY KEY;
ALTER TABLE `t_ddh_config_version_info` ADD PRIMARY KEY (`id`);
ALTER TABLE `t_ddh_config_version_info` ADD UNIQUE KEY `uk_version_ref` (`version`, `ref_type`, `ref_id`);

-- 添加审计字段
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `user_id`;
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `t_ddh_config_version_info` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- 3. 特殊表处理
-- 3.1 misval表重构
-- 重命名Id列为id并修改类型
ALTER TABLE `misval` CHANGE COLUMN `Id` `id` BIGINT COMMENT '主键';
-- 为id列填充临时BIGINT ID用于迁移（如果有空值或重复值）
SET @row_number = 0;
UPDATE `misval` SET `id` = (@row_number:=@row_number+5000000) WHERE `id` IS NULL OR `id` = 0;
-- 添加主键约束
ALTER TABLE `misval` ADD PRIMARY KEY (`id`);

-- 添加审计字段
ALTER TABLE `misval` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `misval` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `misval` ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人';
ALTER TABLE `misval` ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 4. 统一所有表主键类型为BIGINT（批量处理，不设置AUTO_INCREMENT，使用雪花算法）
-- 4.1 修改现有INT主键为BIGINT
ALTER TABLE `t_ddh_alert_group` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_alert_group_map` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_alert_history` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_alert_quota` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_group` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_host` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_info` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_node_label` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_queue_capacity` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_rack` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_role_user` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_dashboard` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_instance` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_instance_role_group` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_role_group_config` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_role_instance` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_service_role_instance_webuis` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_tenant` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_user` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_user_group` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_user_tenant` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_variable` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_yarn_queue` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_yarn_scheduler` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_cluster_zk` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_command` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_frame_info` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_frame_service` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_frame_service_role` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_install_step` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_notice_group` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_notice_group_user` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_operation_log` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_role_info` MODIFY COLUMN `id` BIGINT COMMENT '主键';
ALTER TABLE `t_ddh_user_info` MODIFY COLUMN `id` BIGINT COMMENT '主键';

-- 4.2 【关键修复】统一所有外键字段类型为BIGINT
-- 修复frame_id字段（框架ID外键）
ALTER TABLE `t_ddh_cluster_info` MODIFY COLUMN `frame_id` BIGINT COMMENT '框架ID';
ALTER TABLE `t_ddh_frame_service` MODIFY COLUMN `frame_id` BIGINT COMMENT '框架ID';

-- 修复service_id字段（服务ID外键）
ALTER TABLE `t_ddh_cluster_service_role_instance` MODIFY COLUMN `service_id` BIGINT COMMENT '服务ID';
ALTER TABLE `t_ddh_frame_service_role` MODIFY COLUMN `service_id` BIGINT COMMENT '服务ID';

-- 修复cluster_id字段（集群ID外键）
ALTER TABLE `t_ddh_cluster_alert_group_map` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_alert_history` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_group` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_host` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_node_label` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_queue_capacity` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_rack` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_role_user` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_service_instance` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_service_instance_role_group` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_service_role_group_config` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_service_role_instance` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_user` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_user_group` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_user_tenant` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_variable` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_yarn_queue` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_yarn_scheduler` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_cluster_zk` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';
ALTER TABLE `t_ddh_operation_log` MODIFY COLUMN `cluster_id` BIGINT COMMENT '集群ID';

-- 修复user_id字段（用户ID外键）
-- 注意：t_ddh_access_token表已在V3.0.0中删除并重建为t_ddh_auth_token，跳过此操作
-- ALTER TABLE `t_ddh_access_token` MODIFY COLUMN `user_id` BIGINT COMMENT '用户ID';
ALTER TABLE `t_ddh_cluster_role_user` MODIFY COLUMN `user_id` BIGINT COMMENT '用户ID';
ALTER TABLE `t_ddh_cluster_user_group` MODIFY COLUMN `user_id` BIGINT COMMENT '用户ID';
ALTER TABLE `t_ddh_cluster_user_tenant` MODIFY COLUMN `user_id` BIGINT COMMENT '用户ID';
ALTER TABLE `t_ddh_notice_group_user` MODIFY COLUMN `user_id` BIGINT COMMENT '用户ID';
-- 注意：t_ddh_operation_log表使用operate_user字段而非user_id字段
-- ALTER TABLE `t_ddh_operation_log` MODIFY COLUMN `user_id` BIGINT COMMENT '用户ID';

-- 修复alert_group_id字段（告警组ID外键）
ALTER TABLE `t_ddh_cluster_alert_group_map` MODIFY COLUMN `alert_group_id` BIGINT COMMENT '告警组ID';
ALTER TABLE `t_ddh_cluster_alert_quota` MODIFY COLUMN `alert_group_id` BIGINT COMMENT '告警组ID';

-- 修复notice_group_id字段（通知组ID外键）
ALTER TABLE `t_ddh_cluster_alert_quota` MODIFY COLUMN `notice_group_id` BIGINT COMMENT '通知组ID';
ALTER TABLE `t_ddh_notice_group_user` MODIFY COLUMN `notice_group_id` BIGINT COMMENT '通知组ID';

-- 修复service_instance_id字段（服务实例ID外键）
ALTER TABLE `t_ddh_cluster_service_command` MODIFY COLUMN `service_instance_id` BIGINT COMMENT '服务实例ID';
ALTER TABLE `t_ddh_cluster_alert_history` MODIFY COLUMN `service_instance_id` BIGINT COMMENT '服务实例ID';
ALTER TABLE `t_ddh_cluster_service_instance_role_group` MODIFY COLUMN `service_instance_id` BIGINT COMMENT '服务实例ID';
ALTER TABLE `t_ddh_cluster_service_role_instance_webuis` MODIFY COLUMN `service_instance_id` BIGINT COMMENT '服务实例ID';

-- 修复service_role_instance_id字段（服务角色实例ID外键）
ALTER TABLE `t_ddh_cluster_alert_history` MODIFY COLUMN `service_role_instance_id` BIGINT COMMENT '服务角色实例ID';
ALTER TABLE `t_ddh_cluster_service_role_instance_webuis` MODIFY COLUMN `service_role_instance_id` BIGINT COMMENT '服务角色实例ID';

-- 修复role_group_id字段（角色组ID外键）
ALTER TABLE `t_ddh_cluster_service_role_group_config` MODIFY COLUMN `role_group_id` BIGINT COMMENT '角色组ID';
ALTER TABLE `t_ddh_cluster_service_role_instance` MODIFY COLUMN `role_group_id` BIGINT COMMENT '角色组ID';

-- 修复frame_service_id字段（框架服务ID外键）
ALTER TABLE `t_ddh_cluster_service_instance` MODIFY COLUMN `frame_service_id` BIGINT COMMENT '框架服务ID';

-- 修复group_id字段（组ID外键）
ALTER TABLE `t_ddh_cluster_user_group` MODIFY COLUMN `group_id` BIGINT COMMENT '组ID';

-- 修复tenant_id字段（租户ID外键）
ALTER TABLE `t_ddh_cluster_user_tenant` MODIFY COLUMN `tenant_id` BIGINT COMMENT '租户ID';

-- 修复expression_id字段（表达式ID外键）
ALTER TABLE `t_ddh_cluster_alert_rule` MODIFY COLUMN `expression_id` BIGINT COMMENT '表达式ID';

-- 修复receiver_group_id字段（接收组ID外键）
ALTER TABLE `t_ddh_cluster_alert_rule` MODIFY COLUMN `receiver_group_id` BIGINT COMMENT '接收组ID';

-- 为t_ddh_operation_log表添加标准审计字段（该表当前有操作相关时间字段，需要添加标准审计字段）
ALTER TABLE `t_ddh_operation_log` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 5. 批量添加缺失的审计字段
-- 5.1 完全缺失审计字段的表（批量添加全部4个字段）
ALTER TABLE `t_ddh_cluster_alert_group_map` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_group` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_node_label` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_queue_capacity` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_rack` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_role_user` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_service_dashboard` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_service_role_instance_webuis` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_tenant` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_user` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_user_group` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_user_tenant` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_variable` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_yarn_scheduler` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_cluster_zk` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_command` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_frame_info` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_frame_service` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_frame_service_role` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_install_step` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

ALTER TABLE `t_ddh_notice_group_user` 
ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人',
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人';

-- 5.2 部分缺失审计字段的表（补充缺失字段）
-- t_ddh_alert_group: 只有create_time，需要添加其他3个字段
ALTER TABLE `t_ddh_alert_group` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- t_ddh_cluster_alert_quota: 只有create_time，需要添加其他3个字段
ALTER TABLE `t_ddh_cluster_alert_quota` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- t_ddh_cluster_host: 只有create_time，需要添加其他3个字段
ALTER TABLE `t_ddh_cluster_host` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- t_ddh_cluster_info: 有create_time和create_by，需要添加update_time和update_by
ALTER TABLE `t_ddh_cluster_info` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- t_ddh_cluster_service_instance_role_group: 只有create_time，需要添加其他3个字段
ALTER TABLE `t_ddh_cluster_service_instance_role_group` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- t_ddh_cluster_yarn_queue: 只有create_time，需要添加其他3个字段
ALTER TABLE `t_ddh_cluster_yarn_queue` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- t_ddh_notice_group: 只有create_time，需要添加其他3个字段
ALTER TABLE `t_ddh_notice_group` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- t_ddh_role_info: 只有create_time，需要添加其他3个字段
ALTER TABLE `t_ddh_role_info` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- t_ddh_user_info: 只有create_time，需要添加其他3个字段
ALTER TABLE `t_ddh_user_info` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- 5.3 已有create_time和update_time但缺少create_by和update_by的表
ALTER TABLE `t_ddh_cluster_alert_expression` 
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

ALTER TABLE `t_ddh_cluster_alert_history` 
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

ALTER TABLE `t_ddh_cluster_alert_rule` 
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

ALTER TABLE `t_ddh_cluster_service_instance` 
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

ALTER TABLE `t_ddh_cluster_service_role_group_config` 
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

ALTER TABLE `t_ddh_cluster_service_role_instance` 
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- 5.4 t_ddh_auth_token表字段标准化
-- 该表需要将非标准字段名称修改为标准审计字段名称
-- 步骤1：重命名现有时间字段为标准名称
ALTER TABLE `t_ddh_auth_token` CHANGE COLUMN `created_at` `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `t_ddh_auth_token` CHANGE COLUMN `updated_at` `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 步骤2：添加缺失的审计字段
ALTER TABLE `t_ddh_auth_token` 
ADD COLUMN `create_by` VARCHAR(128) DEFAULT NULL COMMENT '创建人' AFTER `update_time`,
ADD COLUMN `update_by` VARCHAR(128) DEFAULT NULL COMMENT '更新人' AFTER `create_by`;

-- 6. 索引优化 - 为重要审计字段添加索引（提升查询性能）
CREATE INDEX `idx_create_time` ON `t_ddh_cluster_alert_history` (`create_time`);
CREATE INDEX `idx_create_by` ON `t_ddh_cluster_alert_history` (`create_by`);
CREATE INDEX `idx_create_time` ON `t_ddh_cluster_host` (`create_time`);
CREATE INDEX `idx_create_by` ON `t_ddh_cluster_host` (`create_by`);
CREATE INDEX `idx_create_time` ON `t_ddh_operation_log` (`create_time`);
CREATE INDEX `idx_create_by` ON `t_ddh_operation_log` (`create_by`);

-- =============================================================================
-- 数据完整性检查查询（注释掉，需要时可执行）
-- =============================================================================

-- 验证所有表都有标准审计字段的查询
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

-- 验证所有主键都是BIGINT类型的查询
-- SELECT 'Tables with non-BIGINT primary keys:' as verification_type;
-- SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE as non_bigint_pk_table
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = 'datasophon2' 
--   AND COLUMN_KEY = 'PRI' 
--   AND DATA_TYPE != 'bigint';

-- 提交事务（确保所有操作都成功完成）
COMMIT;

-- 验证外键关联数据完整性
-- SELECT 'Command tables foreign key integrity check:' as verification_type;
-- SELECT 
--   COUNT(*) as total_commands,
--   COUNT(DISTINCT h.command_id) as referenced_commands,
--   COUNT(DISTINCT hc.command_id) as host_command_refs
-- FROM t_ddh_cluster_service_command c
-- LEFT JOIN t_ddh_cluster_service_command_host h ON c.id = h.command_id
-- LEFT JOIN t_ddh_cluster_service_command_host_command hc ON c.id = hc.command_id;