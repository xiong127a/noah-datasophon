-- 用户表增强 - 添加个人简介、最后登录时间、头像字段
-- Version: 3.0.1
-- Description: 增强用户管理功能，添加个人简介、最后登录时间和头像字段

-- 添加个人简介字段
ALTER TABLE `t_ddh_user_info` 
ADD COLUMN `bio` TEXT COMMENT '个人简介' AFTER `userType`;

-- 添加最后登录时间字段
ALTER TABLE `t_ddh_user_info` 
ADD COLUMN `last_login_time` DATETIME COMMENT '最后登录时间' AFTER `bio`;

-- 添加用户头像字段（存储Base64编码的图片数据）
ALTER TABLE `t_ddh_user_info` 
ADD COLUMN `avatar` LONGTEXT COMMENT '用户头像（Base64编码）' AFTER `last_login_time`;

-- 更新用户类型字段注释（规范化）
ALTER TABLE `t_ddh_user_info` 
MODIFY COLUMN `userType` INT COMMENT '用户类型: 1-管理员, 2-普通用户';

-- 为性能优化添加索引
CREATE INDEX `idx_user_type` ON `t_ddh_user_info`(`userType`);
CREATE INDEX `idx_last_login_time` ON `t_ddh_user_info`(`last_login_time`);

-- 为已有用户设置默认值（可选）
-- UPDATE `t_ddh_user_info` SET `bio` = '暂无简介' WHERE `bio` IS NULL;
-- UPDATE `t_ddh_user_info` SET `userType` = 2 WHERE `userType` IS NULL;