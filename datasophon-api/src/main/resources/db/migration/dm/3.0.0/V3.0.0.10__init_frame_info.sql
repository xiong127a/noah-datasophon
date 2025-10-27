-- 初始化框架信息数据
-- 框架版本号从 frame_code 中提取，去掉前面的 "DDP-" 等前缀

-- 插入 DDP-3.0.0 框架信息
MERGE INTO t_ddh_frame_info t
USING (SELECT 'DDP' AS frame_name, 'DDP-3.0.0' AS frame_code, '3.0.0' AS frame_version FROM dual) s
ON (t.frame_code = s.frame_code)
WHEN MATCHED THEN
    UPDATE SET t.frame_name = s.frame_name, t.frame_version = s.frame_version
WHEN NOT MATCHED THEN
    INSERT (frame_name, frame_code, frame_version)
    VALUES (s.frame_name, s.frame_code, s.frame_version);

-- 如果有其他框架版本，在这里添加
-- 例如：
-- MERGE INTO t_ddh_frame_info t
-- USING (SELECT 'DDP' AS frame_name, 'DDP-2.2.0' AS frame_code, '2.2.0' AS frame_version FROM dual) s
-- ON (t.frame_code = s.frame_code)
-- WHEN MATCHED THEN
--     UPDATE SET t.frame_name = s.frame_name, t.frame_version = s.frame_version
-- WHEN NOT MATCHED THEN
--     INSERT (frame_name, frame_code, frame_version)
--     VALUES (s.frame_name, s.frame_code, s.frame_version);

