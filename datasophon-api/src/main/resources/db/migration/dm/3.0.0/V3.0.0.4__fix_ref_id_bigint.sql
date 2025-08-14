-- 将ref_id字段类型从int修改为bigint，以支持更大的ID值范围
-- Version: 3.0.4
-- Description: 修复配置版本信息表中ref_id字段的数据类型，从int扩展为bigint

-- 修改t_ddh_config_version_info表的ref_id字段类型
ALTER TABLE t_ddh_config_version_info 
MODIFY COLUMN ref_id bigint NOT NULL; -- '关联对象ID'
