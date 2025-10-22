-- Add columns for OLAP node cluster management
-- These columns are used to track whether OLAP nodes (StarRocks/Doris BE/FE) have been added to the cluster

ALTER TABLE `t_ddh_cluster_service_role_instance`
    ADD COLUMN `added_to_cluster` TINYINT(1) DEFAULT 0 COMMENT '是否已添加到集群（用于OLAP类服务）0:未添加 1:已添加',
    ADD COLUMN `add_to_cluster_time` DATETIME DEFAULT NULL COMMENT '添加到集群的时间';

-- Create index for efficient querying
CREATE INDEX `idx_added_to_cluster` ON `t_ddh_cluster_service_role_instance`(`added_to_cluster`, `service_role_state`, `service_role_name`);

