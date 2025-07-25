SET FOREIGN_KEY_CHECKS=0;

ALTER TABLE `migration_history` MODIFY COLUMN `version` varchar(128) NOT NULL FIRST;

ALTER TABLE `migration_history` MODIFY COLUMN `execute_user` varchar(128) NOT NULL AFTER `version`;

CREATE TABLE `misval`  (
                                 `Id` bigint NULL DEFAULT NULL,
                                 `Sepal.Length` double NULL DEFAULT NULL,
                                 `Sepal.Width` double NULL DEFAULT NULL,
                                 `Petal.Length` double NULL DEFAULT NULL,
                                 `Petal.Width` double NULL DEFAULT NULL,
                                 `Species` text NULL
) ENGINE = InnoDB ROW_FORMAT = Dynamic;

CREATE TABLE `t_ddh_config_version_info`  (
                                                    `version` int NOT NULL COMMENT '版本号',
                                                    `ref_type` varchar(20) NOT NULL COMMENT '引用类型(SERVICE/ROLE_GROUP)',
                                                    `ref_id` int NOT NULL COMMENT '关联对象ID',
                                                    `description` varchar(255) NULL DEFAULT NULL COMMENT '版本描述',
                                                    `editor` varchar(50) NULL DEFAULT NULL COMMENT '编辑者',
                                                    `edit_time` datetime NULL DEFAULT NULL COMMENT '编辑时间',
                                                    `is_current` tinyint(1) NULL DEFAULT 0 COMMENT '是否当前使用版本',
                                                    `service_code` varchar(50) NULL DEFAULT NULL COMMENT '服务代码',
                                                    `user_id` int NULL DEFAULT NULL,
                                                    PRIMARY KEY (`version`, `ref_type`, `ref_id`) USING BTREE
) ENGINE = InnoDB COMMENT = '配置版本详情表' ROW_FORMAT = Dynamic;

ALTER TABLE `t_ddh_frame_service` MODIFY COLUMN `service_config` longtext NULL AFTER `package_name`;

ALTER TABLE `t_ddh_frame_service` MODIFY COLUMN `service_json` longtext NULL AFTER `service_config`;

ALTER TABLE `t_ddh_frame_service` MODIFY COLUMN `config_file_json` longtext NULL AFTER `frame_code`;

ALTER TABLE `t_ddh_operation_log` MODIFY COLUMN `param` longtext NULL COMMENT '请求数据' AFTER `service_role_instances_ids`;

SET FOREIGN_KEY_CHECKS=1;