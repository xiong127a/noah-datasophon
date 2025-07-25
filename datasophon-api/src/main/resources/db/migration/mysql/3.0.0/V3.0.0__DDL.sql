-- 删除旧表
DROP TABLE IF EXISTS `t_ddh_session`;
DROP TABLE IF EXISTS `t_ddh_access_token`;

-- 创建新表
CREATE TABLE `t_ddh_auth_token` (
  `id` bigint NOT NULL COMMENT '主键，使用雪花算法生成',
  `user_id` int NOT NULL COMMENT '关联的用户ID',
  `token` varchar(2048) NOT NULL COMMENT 'JWT访问令牌',
  `refresh_token` varchar(2048) DEFAULT NULL COMMENT '刷新令牌',
  `token_type` varchar(50) DEFAULT 'Bearer' COMMENT '令牌类型，默认为Bearer',
  `client_ip` varchar(128) DEFAULT NULL COMMENT '客户端IP地址',
  `user_agent` varchar(512) DEFAULT NULL COMMENT '客户端浏览器信息',
  `issued_at` datetime NOT NULL COMMENT '令牌颁发时间',
  `expires_at` datetime NOT NULL COMMENT '令牌过期时间',
  `last_access_time` datetime DEFAULT NULL COMMENT '最后访问时间',
  `is_revoked` tinyint DEFAULT '0' COMMENT '是否已被撤销，0-有效，1-已撤销',
  `revoked_reason` varchar(128) DEFAULT NULL COMMENT '撤销原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JWT认证令牌表' ROW_FORMAT=DYNAMIC;

-- 添加外键约束（如果需要）
-- ALTER TABLE `t_ddh_auth_token`
--   ADD CONSTRAINT `fk_auth_token_user_id` FOREIGN KEY (`user_id`) REFERENCES `t_ddh_user_info` (`id`) ON DELETE CASCADE; 