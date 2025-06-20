SET FOREIGN_KEY_CHECKS=0;

ALTER TABLE `ddp`.`migration_history` CHARACTER SET = utf8mb3, COLLATE = utf8mb3_general_ci;

ALTER TABLE `ddp`.`migration_history` MODIFY COLUMN `version` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL FIRST;

ALTER TABLE `ddp`.`migration_history` MODIFY COLUMN `execute_user` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL AFTER `version`;

CREATE TABLE `ddp`.`misval`  (
                                 `Id` bigint NULL DEFAULT NULL,
                                 `Sepal.Length` double NULL DEFAULT NULL,
                                 `Sepal.Width` double NULL DEFAULT NULL,
                                 `Petal.Length` double NULL DEFAULT NULL,
                                 `Petal.Width` double NULL DEFAULT NULL,
                                 `Species` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

ALTER TABLE `ddp`.`t_ddh_cluster_service_role_group_config` MODIFY COLUMN `config_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL AFTER `role_group_id`;

ALTER TABLE `ddp`.`t_ddh_cluster_service_role_group_config` MODIFY COLUMN `config_file_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL AFTER `config_version`;

CREATE TABLE `ddp`.`t_ddh_config_version_info`  (
                                                    `version` int NOT NULL COMMENT '版本号',
                                                    `ref_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '引用类型(SERVICE/ROLE_GROUP)',
                                                    `ref_id` int NOT NULL COMMENT '关联对象ID',
                                                    `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本描述',
                                                    `editor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '编辑者',
                                                    `edit_time` datetime NULL DEFAULT NULL COMMENT '编辑时间',
                                                    `is_current` tinyint(1) NULL DEFAULT 0 COMMENT '是否当前使用版本',
                                                    `service_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '服务代码',
                                                    `user_id` int NULL DEFAULT NULL,
                                                    PRIMARY KEY (`version`, `ref_type`, `ref_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '配置版本详情表' ROW_FORMAT = Dynamic;

ALTER TABLE `ddp`.`t_ddh_frame_service` MODIFY COLUMN `service_config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL AFTER `package_name`;

ALTER TABLE `ddp`.`t_ddh_frame_service` MODIFY COLUMN `service_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL AFTER `service_config`;

ALTER TABLE `ddp`.`t_ddh_frame_service` MODIFY COLUMN `config_file_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL AFTER `frame_code`;

ALTER TABLE `ddp`.`t_ddh_operation_log` MODIFY COLUMN `param` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求数据' AFTER `service_role_instances_ids`;

SET FOREIGN_KEY_CHECKS=1;