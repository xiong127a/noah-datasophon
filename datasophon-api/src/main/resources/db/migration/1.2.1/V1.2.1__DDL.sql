CREATE TABLE `t_ddh_cluster_tenant` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(10) DEFAULT NULL,
  `tenant_name` varchar(255) DEFAULT NULL COMMENT '租户名称',
  `hdfs_path` varchar(255) DEFAULT NULL COMMENT 'hdfs存储路径',
  `hdfs_quota` varchar(255) DEFAULT NULL COMMENT 'hdfs文件数配额',
  `hdfs_space_quota` varchar(255) DEFAULT NULL COMMENT 'hdfs空间配额',
  `yarn_memory` varchar(255) DEFAULT NULL COMMENT 'yarn可用内存大小',
  `yarn_cpu` varchar(255) DEFAULT NULL COMMENT 'yarn可用cpu核数',
  `hive_database` varchar(255) DEFAULT NULL COMMENT 'hive数据库名称',
  `hive_database_capacity` varchar(255) DEFAULT NULL COMMENT 'hive数据库容量',
  `kafka_topics_config` varchar(255) DEFAULT NULL COMMENT 'kafka的主题配置，topic、容量、副本，支持多个',
  `hbase_namespace` varchar(255) DEFAULT NULL COMMENT 'hbase命名空间',
  `hbase_capacity` varchar(255) DEFAULT NULL COMMENT 'hbase容量',
  `hbase_regionServer_num` varchar(255) DEFAULT NULL COMMENT 'regionServer数量',
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8mb4 ROW_FORMAT = DYNAMIC COMMENT='租户表';