-- Create Parcel Repository table
CREATE TABLE `t_ddh_parcel_repository` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `repo_name` varchar(128) NOT NULL COMMENT '存储库名称',
  `repo_type` varchar(32) NOT NULL DEFAULT 'http' COMMENT '存储库类型：local/http',
  `repo_url` varchar(512) NOT NULL COMMENT '存储库地址',
  `frame_code` varchar(64) DEFAULT NULL COMMENT '框架代码（如 DDP-1.2.1）',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `is_default` tinyint DEFAULT 0 COMMENT '是否默认存储库：0-否，1-是',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repo_name` (`repo_name`),
  KEY `idx_repo_type` (`repo_type`),
  KEY `idx_frame_code` (`frame_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Parcel存储库配置表';

-- Insert default local repository (replaces hardcoded Constants.MASTER_MANAGE_PACKAGE_PATH)
INSERT INTO `t_ddh_parcel_repository` 
  (`repo_name`, `repo_type`, `repo_url`, `description`, `is_default`, `status`)
VALUES 
  ('本地存储库', 'local', '/opt/datasophon/DDP/packages', 'Master节点本地存储库', 1, 1);

