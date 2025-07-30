-- 用户表增强 - 添加个人简介、最后登录时间、头像字段 (达梦数据库版本)
-- Version: 3.0.1
-- Description: 增强用户管理功能，添加个人简介、最后登录时间和头像字段

-- 添加个人简介字段
ALTER TABLE "DATASOPHON"."t_ddh_user_info" 
ADD "bio" CLOB NULL;

-- 添加字段注释
COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."bio" IS '个人简介';

-- 添加最后登录时间字段
ALTER TABLE "DATASOPHON"."t_ddh_user_info" 
ADD "last_login_time" DATETIME NULL;

-- 添加字段注释
COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."last_login_time" IS '最后登录时间';

-- 添加用户头像字段（存储Base64编码的图片数据）
ALTER TABLE "DATASOPHON"."t_ddh_user_info" 
ADD "avatar" CLOB NULL;

-- 添加字段注释
COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."avatar" IS '用户头像（Base64编码）';

-- 更新用户类型字段注释（规范化）
COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."user_type" IS '用户类型: 1-管理员, 2-普通用户';

-- 为性能优化添加索引
CREATE INDEX "idx_user_type" ON "DATASOPHON"."t_ddh_user_info"("user_type");
CREATE INDEX "idx_last_login_time" ON "DATASOPHON"."t_ddh_user_info"("last_login_time");

-- 为admin用户设置个人简介和头像
UPDATE "DATASOPHON"."t_ddh_user_info" 
SET 
    "bio" = '系统超级管理员，负责Noah大数据基础平台的整体管理和维护工作',
    "avatar" = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQxKSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMjggMjEgTDMwIDI0IEwzNiAyMSBMMzQgMjMgTDMyIDI2IFoiIGZpbGw9ImdvbGQiLz4KPGRlZnM+CjxsaW5lYXJHcmFkaWVudCBpZD0iZ3JhZGllbnQxIiB4MT0iMCIgeTE9IjAiIHgyPSI2NCIgeTI9IjY0IiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+CjxzdG9wIHN0b3AtY29sb3I9IiNmYmI0MjYiLz4KPHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjZjU5ZTBiIi8+CjwvbGluZWFyR3JhZGllbnQ+CjwvZGVmcz4KPC9zdmc+Cg=='
WHERE "username" = 'admin' AND "id" = 1;

-- 提交事务
COMMIT;