CREATE TABLE `t_ddh_cluster_tenant` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(10) DEFAULT NULL,
  `tenant_name` varchar(255) DEFAULT NULL COMMENT '租户名称',
  `hdfs_resource_list` text COMMENT 'hdfs资源列表',
  `yarn_resource_list` text COMMENT 'yarn资源列表',
  `hive_resource_list` text COMMENT 'hive资源列表',
  `hbase_resource_list` text COMMENT 'hbase资源列表',
  `kafka_resource_list` text COMMENT 'kafka资源列表',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=82 DEFAULT CHARSET=utf8 COMMENT='租户表';

CREATE TABLE `t_ddh_cluster_user_tenant` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(10) DEFAULT NULL,
  `user_id` int(10) DEFAULT NULL,
  `tenant_id` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=112 DEFAULT CHARSET=utf8 COMMENT='租户授权表';