-- 初始化框架信息数据
-- 框架版本号从 frame_code 中提取，去掉前面的 "DDP-" 等前缀

-- 插入 DDP-3.0.0 框架信息
INSERT INTO `t_ddh_frame_info` (`frame_name`, `frame_code`, `frame_version`) 
VALUES ('DDP', 'DDP-3.0.0', '3.0.0')
ON DUPLICATE KEY UPDATE 
    `frame_name` = VALUES(`frame_name`),
    `frame_version` = VALUES(`frame_version`);

-- 如果有其他框架版本，在这里添加
-- 例如：
-- INSERT INTO `t_ddh_frame_info` (`frame_name`, `frame_code`, `frame_version`) 
-- VALUES ('DDP', 'DDP-2.2.0', '2.2.0')
-- ON DUPLICATE KEY UPDATE 
--     `frame_name` = VALUES(`frame_name`),
--     `frame_version` = VALUES(`frame_version`);

