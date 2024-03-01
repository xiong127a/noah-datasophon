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

CREATE TABLE `t_ddh_operation_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `url` varchar(128) DEFAULT NULL COMMENT '请求地址',
  `ip` varchar(128) DEFAULT NULL COMMENT '客户端ip',
  `operation_module` varchar(128) DEFAULT NULL COMMENT '操作模块',
  `operation_type` varchar(128) DEFAULT NULL COMMENT '操作类型',
  `cluster_id` int(10) DEFAULT NULL COMMENT '返回状态码',
  `host_ids` varchar(30) DEFAULT NULL COMMENT '主机',
  `service_name` varchar(100) DEFAULT NULL,
  `service_role_instances_ids` varchar(100) DEFAULT NULL,
  `return_msg` varchar(128) DEFAULT NULL COMMENT '返回说明',
  `operate_user` varchar(128) DEFAULT NULL COMMENT '操作人',
  `start_time` datetime DEFAULT NULL COMMENT '操作开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '操作结束时间',
  `param` text COMMENT '请求数据',
  `return_code` int(10) DEFAULT NULL COMMENT '返回状态码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=432 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='操作日志表';