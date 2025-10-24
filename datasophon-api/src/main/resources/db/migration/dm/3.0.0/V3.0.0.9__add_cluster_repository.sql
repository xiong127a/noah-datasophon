-- Add repository_id column to cluster_info table for DM database
ALTER TABLE t_ddh_cluster_info 
  ADD repository_id bigint DEFAULT NULL COMMENT '关联的存储库ID';

CREATE INDEX idx_repository_id ON t_ddh_cluster_info(repository_id);

-- Associate existing clusters with the default local repository (backward compatibility)
UPDATE t_ddh_cluster_info 
SET repository_id = (SELECT id FROM t_ddh_parcel_repository WHERE is_default = 1 LIMIT 1)
WHERE repository_id IS NULL;

