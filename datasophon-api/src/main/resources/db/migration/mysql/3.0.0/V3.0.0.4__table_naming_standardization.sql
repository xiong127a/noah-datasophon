-- V3.0.0.4 表命名标准化重构
-- 作者：任相鹏
-- 邮箱：635887935@qq.com  
-- 日期：2025-01-31
-- 描述：统一数据库表命名规范，移除冗余的ddh前缀，实现简洁清晰的命名体系
-- 
-- ⚠️ 重要说明：
-- 1. 本迁移将所有表从t_ddh_*命名改为t_*命名
-- 2. 保持所有数据完整性，仅重命名表名
-- 3. 不涉及物理外键，仅使用逻辑外键关系
-- 4. 所有实体类的@Table注解需要同步更新

-- =============================================================================
-- 表命名标准化：按功能模块分组重命名
-- =============================================================================

-- 1. 核心框架表
RENAME TABLE `t_ddh_frame_info` TO `t_frame`;
RENAME TABLE `t_ddh_frame_service` TO `t_frame_service`;
RENAME TABLE `t_ddh_frame_service_role` TO `t_frame_service_role`;

-- 2. 集群管理表
RENAME TABLE `t_ddh_cluster_info` TO `t_cluster`;
RENAME TABLE `t_ddh_cluster_host` TO `t_cluster_host`;
RENAME TABLE `t_ddh_cluster_group` TO `t_cluster_group`;
RENAME TABLE `t_ddh_cluster_rack` TO `t_cluster_rack`;
RENAME TABLE `t_ddh_cluster_node_label` TO `t_cluster_node_label`;
RENAME TABLE `t_ddh_cluster_variable` TO `t_cluster_variable`;
RENAME TABLE `t_ddh_cluster_zk` TO `t_cluster_zk`;

-- 3. 服务管理表
RENAME TABLE `t_ddh_cluster_service_instance` TO `t_service_instance`;
RENAME TABLE `t_ddh_cluster_service_role_instance` TO `t_service_role_instance`;
RENAME TABLE `t_ddh_cluster_service_command` TO `t_service_command`;
RENAME TABLE `t_ddh_cluster_service_command_host` TO `t_service_command_host`;
RENAME TABLE `t_ddh_cluster_service_command_host_command` TO `t_service_command_host_command`;
RENAME TABLE `t_ddh_cluster_service_dashboard` TO `t_service_dashboard`;
RENAME TABLE `t_ddh_cluster_service_instance_role_group` TO `t_service_instance_role_group`;
RENAME TABLE `t_ddh_cluster_service_role_group_config` TO `t_service_role_group_config`;
RENAME TABLE `t_ddh_cluster_service_role_instance_webuis` TO `t_service_role_instance_webuis`;
RENAME TABLE `t_ddh_cluster_service_instance_config` TO `t_service_instance_config`;
RENAME TABLE `t_ddh_cluster_service_role_instance_config` TO `t_service_role_instance_config`;

-- 4. 用户权限表
RENAME TABLE `t_ddh_user_info` TO `t_user`;
RENAME TABLE `t_ddh_role_info` TO `t_role`;
RENAME TABLE `t_ddh_cluster_user` TO `t_cluster_user`;
RENAME TABLE `t_ddh_cluster_user_group` TO `t_cluster_user_group`;
RENAME TABLE `t_ddh_cluster_role_user` TO `t_cluster_role_user`;
RENAME TABLE `t_ddh_cluster_user_tenant` TO `t_cluster_user_tenant`;
RENAME TABLE `t_ddh_cluster_tenant` TO `t_cluster_tenant`;

-- 5. 告警监控表
RENAME TABLE `t_ddh_alert_group` TO `t_alert_group`;
RENAME TABLE `t_ddh_cluster_alert_expression` TO `t_alert_expression`;
RENAME TABLE `t_ddh_cluster_alert_group_map` TO `t_alert_group_map`;
RENAME TABLE `t_ddh_cluster_alert_history` TO `t_alert_history`;
RENAME TABLE `t_ddh_cluster_alert_quota` TO `t_alert_quota`;
RENAME TABLE `t_ddh_cluster_alert_rule` TO `t_alert_rule`;

-- 6. 通知管理表
RENAME TABLE `t_ddh_notice_group` TO `t_notice_group`;
RENAME TABLE `t_ddh_notice_group_user` TO `t_notice_group_user`;

-- 7. YARN资源管理表
RENAME TABLE `t_ddh_cluster_yarn_queue` TO `t_yarn_queue`;
RENAME TABLE `t_ddh_cluster_yarn_scheduler` TO `t_yarn_scheduler`;
RENAME TABLE `t_ddh_cluster_queue_capacity` TO `t_queue_capacity`;

-- 8. 系统管理表
RENAME TABLE `t_ddh_access_token` TO `t_access_token`;
RENAME TABLE `t_ddh_session` TO `t_session`;
RENAME TABLE `t_ddh_operation_log` TO `t_operation_log`;
RENAME TABLE `t_ddh_install_step` TO `t_install_step`;
RENAME TABLE `t_ddh_command` TO `t_command`;

-- 9. 配置管理表
RENAME TABLE `t_ddh_config_version_info` TO `t_config_version`;

-- 10. 特殊表处理
-- misval表保持原名（不是ddh表）

-- =============================================================================
-- 验证重命名结果
-- =============================================================================

-- 统计重命名后的表数量
SELECT 
    CONCAT('数据库标准化完成，共重命名 ', 
           (SELECT COUNT(*) FROM information_schema.tables 
            WHERE table_schema = DATABASE() AND table_name LIKE 't_%' AND table_name NOT LIKE 't_ddh_%'),
           ' 个表') as summary;
