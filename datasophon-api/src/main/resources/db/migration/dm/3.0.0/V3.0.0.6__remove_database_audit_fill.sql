-- 移除数据库级别的审计字段自动填充，改为程序控制
-- 适用于达梦数据库
-- 作者：任相鹏
-- 邮箱：635887935@qq.com
-- 日期：2025-01-15

-- 修改集群服务命令表，移除数据库级别的自动填充
ALTER TABLE t_ddh_cluster_service_command 
MODIFY create_time datetime DEFAULT NULL;
-- 注释：'创建时间'

ALTER TABLE t_ddh_cluster_service_command 
MODIFY update_time datetime DEFAULT NULL;
-- 注释：'更新时间'

-- 修改其他相关表，移除数据库级别的自动填充（如果存在）
-- t_ddh_cluster_service_command_host表
ALTER TABLE t_ddh_cluster_service_command_host 
MODIFY create_time datetime DEFAULT NULL;
-- 注释：'创建时间'

ALTER TABLE t_ddh_cluster_service_command_host 
MODIFY update_time datetime DEFAULT NULL;
-- 注释：'更新时间'

-- t_ddh_cluster_service_command_host_command表  
ALTER TABLE t_ddh_cluster_service_command_host_command 
MODIFY create_time datetime DEFAULT NULL;
-- 注释：'创建时间'

ALTER TABLE t_ddh_cluster_service_command_host_command 
MODIFY update_time datetime DEFAULT NULL;
-- 注释：'更新时间'

-- 注释：现在所有审计字段（create_time、update_time、create_by、update_by）
-- 都将由MyBatis-Flex的审计监听器自动填充，确保程序完全控制审计信息
