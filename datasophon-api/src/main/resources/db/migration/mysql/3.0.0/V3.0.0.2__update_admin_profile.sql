-- 更新admin用户信息 - 添加头像、简介和更新创建时间
-- Version: 3.0.0.2
-- Description: 为内置admin账号设置头像、个人简介，并优化创建时间

-- 更新admin用户的头像、简介和创建时间
UPDATE `t_ddh_user_info` 
SET 
    `bio` = '系统超级管理员，负责平台整体管理和维护工作',
    `avatar` = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQxKSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMjggMjEgTDMwIDI0IEwzNiAyMSBMMzQgMjMgTDMyIDI2IFoiIGZpbGw9ImdvbGQiLz4KPGRlZnM+CjxsaW5lYXJHcmFkaWVudCBpZD0iZ3JhZGllbnQxIiB4MT0iMCIgeTE9IjAiIHgyPSI2NCIgeTI9IjY0IiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+CjxzdG9wIHN0b3AtY29sb3I9IiNmYmI0MjYiLz4KPHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjZjU5ZTBiIi8+CjwvbGluZWFyR3JhZGllbnQ+CjwvZGVmcz4KPC9zdmc+Cg==',
    `create_time` = NOW(),
    `user_type` = 1
WHERE `username` = 'admin' AND `id` = 1;

-- 确保admin用户邮箱和手机号有合理的默认值
UPDATE `t_ddh_user_info` 
SET 
    `email` = 'admin@datasophon.com',
    `phone` = '18600000000'
WHERE `username` = 'admin' AND `id` = 1 AND (`email` = 'xxx@163.com' OR `phone` = '1865xx');

-- 验证更新结果（注释掉的查询语句，用于调试）
-- SELECT id, username, email, phone, bio, user_type, create_time, 
--        CASE WHEN avatar IS NOT NULL THEN '头像已设置' ELSE '头像未设置' END as avatar_status
-- FROM `t_ddh_user_info` WHERE username = 'admin';