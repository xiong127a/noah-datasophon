-- V3.0.0.2 主机管理增强
-- 作者：任相鹏
-- 邮箱：635887935@qq.com  
-- 日期：2025-01-31
-- 描述：为集群主机表添加K8s节点信息字段和主机管理状态，提升主机管理能力

-- =============================================================================
-- DDL部分：数据库结构变更
-- =============================================================================

-- 1. 为 t_ddh_cluster_host 表添加 K8s 节点信息字段
ALTER TABLE `t_ddh_cluster_host`
ADD COLUMN `k8s_node_name` varchar(255) DEFAULT NULL COMMENT 'Kubernetes节点名称',
ADD COLUMN `k8s_node_version` varchar(64) DEFAULT NULL COMMENT 'Kubernetes节点版本',
ADD COLUMN `k8s_node_age` varchar(32) DEFAULT NULL COMMENT 'Kubernetes节点运行时长',
ADD COLUMN `management_status` int(2) DEFAULT 2 COMMENT '主机管理状态：1-受管，2-未受管，3-配置中';

-- 2. 更新现有字段注释，明确 node_label 的正确用途
ALTER TABLE `t_ddh_cluster_host`
MODIFY COLUMN `node_label` varchar(255) DEFAULT NULL COMMENT '主机标签（用户自定义标签）';

-- 3. 为新字段添加索引以提高查询性能
CREATE INDEX `idx_cluster_host_management_status` ON `t_ddh_cluster_host` (`management_status`);
CREATE INDEX `idx_cluster_host_k8s_node` ON `t_ddh_cluster_host` (`k8s_node_name`);

-- =============================================================================
-- DML部分：数据迁移和处理
-- =============================================================================

-- 4. 更新现有数据：将原 managed 字段映射到新的 management_status 字段
-- 保持值不变：1->1(受管), 2->2(未受管)
UPDATE `t_ddh_cluster_host` 
SET `management_status` = CASE 
    WHEN `managed` = 1 THEN 1  -- YES -> MANAGED (保持值1)
    WHEN `managed` = 2 THEN 2  -- NO -> UNMANAGED (保持值2)
    ELSE 2  -- 默认为 UNMANAGED
END;

-- 5. 清理 node_label 字段中错误存储的 K8s 节点信息
-- 将类似 "kubernetes-node|<none>|v1.28.9|43d" 格式的数据拆分到对应字段
UPDATE `t_ddh_cluster_host`
SET 
    `k8s_node_name` = CASE 
        WHEN `node_label` LIKE 'kubernetes-node|%' 
        THEN SUBSTRING_INDEX(`node_label`, '|', 1)
        ELSE NULL
    END,
    `k8s_node_version` = CASE 
        WHEN `node_label` LIKE 'kubernetes-node|%' 
        THEN SUBSTRING_INDEX(SUBSTRING_INDEX(`node_label`, '|', 3), '|', -1)
        ELSE NULL
    END,
    `k8s_node_age` = CASE 
        WHEN `node_label` LIKE 'kubernetes-node|%' 
        THEN SUBSTRING_INDEX(`node_label`, '|', -1)
        ELSE NULL
    END,
    `node_label` = CASE 
        WHEN `node_label` LIKE 'kubernetes-node|%' 
        THEN NULL  -- 清空错误数据
        ELSE `node_label`  -- 保留真正的标签数据
    END
WHERE `node_label` IS NOT NULL;

-- 6. 验证和修正 management_status 字段数据
-- 确保所有记录都有有效的管理状态
UPDATE `t_ddh_cluster_host` 
SET `management_status` = 2 
WHERE `management_status` IS NULL OR `management_status` NOT IN (1, 2, 3);

-- =============================================================================
-- 数据完整性检查（可选，用于验证迁移结果）
-- =============================================================================

-- 检查是否有遗漏的K8s节点信息（仅在需要时取消注释执行）
-- SELECT 
--     COUNT(*) as total_hosts,
--     SUM(CASE WHEN k8s_node_name IS NOT NULL THEN 1 ELSE 0 END) as k8s_hosts,
--     SUM(CASE WHEN management_status = 1 THEN 1 ELSE 0 END) as unmanaged_hosts,
--     SUM(CASE WHEN management_status = 2 THEN 1 ELSE 0 END) as managed_hosts,
--     SUM(CASE WHEN management_status = 3 THEN 1 ELSE 0 END) as configuring_hosts
-- FROM `t_ddh_cluster_host`;

-- 检查是否有不匹配的K8s数据（仅在需要时取消注释执行）
-- SELECT COUNT(*) as orphaned_k8s_data 
-- FROM `t_ddh_cluster_host` h
-- JOIN `t_ddh_cluster_info` c ON h.cluster_id = c.id
-- WHERE (h.k8s_node_name IS NOT NULL OR h.k8s_node_version IS NOT NULL OR h.k8s_node_age IS NOT NULL)
--   AND c.dep_type != 'Kubernetes';
