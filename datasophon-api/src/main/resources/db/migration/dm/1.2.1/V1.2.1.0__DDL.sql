CREATE TABLE t_ddh_cluster_tenant (
  id int NOT NULL IDENTITY(1,1),
  cluster_id int DEFAULT NULL,
  tenant_name varchar(255) DEFAULT NULL, -- '租户名称'
  hdfs_resource_list text, -- 'hdfs资源列表'
  yarn_resource_list text, -- 'yarn资源列表'
  hive_resource_list text, -- 'hive资源列表'
  hbase_resource_list text, -- 'hbase资源列表'
  kafka_resource_list text, -- 'kafka资源列表'
  PRIMARY KEY (id)
);

CREATE TABLE t_ddh_cluster_user_tenant (
  id int NOT NULL IDENTITY(1,1),
  cluster_id int DEFAULT NULL,
  user_id int DEFAULT NULL,
  tenant_id int DEFAULT NULL,
  PRIMARY KEY (id)
);

-- 添加表注释
COMMENT ON TABLE t_ddh_cluster_tenant IS '租户表';
COMMENT ON TABLE t_ddh_cluster_user_tenant IS '租户授权表';