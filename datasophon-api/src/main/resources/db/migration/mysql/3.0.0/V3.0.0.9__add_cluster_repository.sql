-- Add repository_id column to cluster_info table (if not exists)
SET @column_exists = (
  SELECT COUNT(*) 
  FROM INFORMATION_SCHEMA.COLUMNS 
  WHERE table_schema = DATABASE() 
    AND table_name = 't_ddh_cluster_info' 
    AND column_name = 'repository_id'
);

SET @add_column_sql = IF(
  @column_exists = 0,
  'ALTER TABLE `t_ddh_cluster_info` 
     ADD COLUMN `repository_id` bigint DEFAULT NULL COMMENT ''关联的存储库ID'' AFTER `namespace`,
     ADD KEY `idx_repository_id` (`repository_id`);',
  'SELECT ''Column repository_id already exists'' AS info;'
);

PREPARE stmt FROM @add_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Associate existing clusters with the default local repository (backward compatibility)
UPDATE `t_ddh_cluster_info` 
SET `repository_id` = (SELECT `id` FROM `t_ddh_parcel_repository` WHERE `is_default` = 1 LIMIT 1)
WHERE `repository_id` IS NULL;

