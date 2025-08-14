-- 修复cluster_id字段类型问题
-- 将确实需要修改的表中的cluster_id字段从INT类型改为BIGINT以支持雪花算法生成的20位Long类型集群ID
-- 适用于达梦数据库
-- 注意：基于实际数据库检查，大部分表的cluster_id字段已经是BIGINT类型，只修改确实需要的表

-- 修改集群服务命令表的cluster_id字段类型（当前是int，需要改为bigint）
ALTER TABLE t_ddh_cluster_service_command 
MODIFY cluster_id bigint DEFAULT NULL; -- '集群ID（支持雪花算法生成的Long类型ID）'

-- 注意：t_ddh_cluster_service_command表的service_instance_id字段已经是bigint类型，无需修改
