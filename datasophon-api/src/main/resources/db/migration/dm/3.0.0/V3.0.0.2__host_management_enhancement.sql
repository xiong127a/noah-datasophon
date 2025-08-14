-- V3.0.0.2 主机管理增强
-- 作者：任相鹏
-- 邮箱：635887935@qq.com  
-- 日期：2025-01-31
-- 描述：为集群主机表添加K8s节点信息字段和主机管理状态，提升主机管理能力

-- =============================================================================
-- DDL部分：数据库结构变更
-- =============================================================================

-- 1. 字段重命名和新增
-- 重命名 managed 字段为 management_status（保持数据不丢失）
ALTER TABLE t_ddh_cluster_host
ALTER COLUMN managed RENAME TO management_status;

-- 达梦数据库中修改列的默认值和注释
ALTER TABLE t_ddh_cluster_host
MODIFY COLUMN management_status int DEFAULT 2; -- '主机管理状态：1-受管，2-未受管，3-配置中'

-- 添加 K8s 节点信息字段（达梦使用TEXT代替JSON）
ALTER TABLE t_ddh_cluster_host
ADD COLUMN k8s_node_info TEXT DEFAULT NULL; -- 'Kubernetes节点扩展信息JSON：{status, roles, age, version}'

-- 2. 更新现有字段注释，明确 node_label 的正确用途
ALTER TABLE t_ddh_cluster_host
MODIFY COLUMN node_label varchar(255) DEFAULT NULL; -- '主机标签（用户自定义标签）'

-- 3. 为新字段添加索引以提高查询性能
CREATE INDEX idx_cluster_host_management_status ON t_ddh_cluster_host (management_status);

-- =============================================================================
-- DML部分：数据迁移和处理
-- =============================================================================

-- 4. 清理 node_label 字段中错误存储的 K8s 节点信息
-- 直接清空所有错误格式的K8s数据，不再提供向后兼容
UPDATE t_ddh_cluster_host 
SET node_label = NULL
WHERE node_label IS NOT NULL AND node_label LIKE 'kubernetes-node|%';

-- 5. 确保所有记录都有有效的管理状态
-- 由于使用RENAME重命名，原有数据已保留，只需修正异常值
UPDATE t_ddh_cluster_host 
SET management_status = 2 
WHERE management_status IS NULL OR management_status NOT IN (1, 2, 3);
