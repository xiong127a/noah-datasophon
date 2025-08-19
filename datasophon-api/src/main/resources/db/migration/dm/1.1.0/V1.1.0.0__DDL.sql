-- ----------------------------
-- Table structure for t_ddh_access_token
-- ----------------------------

CREATE TABLE t_ddh_access_token  (
  id int NOT NULL,
  user_id int DEFAULT NULL,
  token varchar(255)  DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  expire_time datetime DEFAULT NULL,
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_alert_group
-- ----------------------------

CREATE TABLE t_ddh_alert_group  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  alert_group_name varchar(32)  DEFAULT NULL, -- '告警组名称'
  alert_group_category varchar(32)  DEFAULT NULL, -- '告警组类别'
  create_time datetime DEFAULT NULL, -- '创建时间'
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_cluster_alert_expression
-- ----------------------------

CREATE TABLE t_ddh_cluster_alert_expression  (
  id bigint NOT NULL IDENTITY(1,1), -- '自增 ID'
  name varchar(255)  DEFAULT NULL, -- '指标名称'
  expr varchar(4096)  DEFAULT NULL, -- '监控指标表达式'
  service_category varchar(255)  DEFAULT NULL, -- '服务类别'
  value_type varchar(255)  DEFAULT NULL, -- '阈值类型  BOOL  INT  FLOAT  '
  is_predefined varchar(255)  DEFAULT NULL, -- '是否预定义'
  state varchar(255)  NOT NULL, -- '表达式状态'
  is_delete varchar(255)  DEFAULT NULL, -- '是否删除'
  create_time datetime DEFAULT NULL, -- '创建时间'
  update_time datetime DEFAULT NULL, -- '修改时间'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_group_map
-- ----------------------------

CREATE TABLE t_ddh_cluster_alert_group_map  (
  id int NOT NULL IDENTITY(1,1),
  cluster_id int DEFAULT NULL,
  alert_group_id int DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_history
-- ----------------------------

CREATE TABLE t_ddh_cluster_alert_history  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  alert_group_name varchar(32)  DEFAULT NULL, -- '告警组'
  alert_target_name varchar(32)  DEFAULT NULL, -- '告警指标'
  alert_info varchar(1024)  DEFAULT NULL, -- '告警详情'
  alert_advice varchar(1024)  DEFAULT NULL, -- '告警建议'
  hostname varchar(32)  DEFAULT NULL, -- '主机'
  alert_level int DEFAULT NULL, -- '告警级别 1：警告2：异常'
  is_enabled int DEFAULT NULL, -- '是否处理 1:未处理2：已处理'
  service_role_instance_id int DEFAULT NULL, -- '集群服务角色实例id'
  service_instance_id int DEFAULT NULL, -- '集群服务实例id'
  create_time datetime DEFAULT NULL, -- '创建时间'
  update_time datetime DEFAULT NULL, -- '更新时间'
  cluster_id int DEFAULT NULL, -- '集群id'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_quota
-- ----------------------------

CREATE TABLE t_ddh_cluster_alert_quota  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  alert_quota_name varchar(255)  DEFAULT NULL, -- '告警指标名称'
  service_category varchar(32)  DEFAULT NULL, -- '服务分类'
  alert_expr varchar(1024)  DEFAULT NULL, -- '告警指标表达式'
  alert_level int DEFAULT NULL, -- '告警级别 1:警告2：异常'
  alert_group_id int DEFAULT NULL, -- '告警组'
  notice_group_id int DEFAULT NULL, -- '通知组'
  alert_advice varchar(1024)  DEFAULT NULL, -- '告警建议'
  compare_method varchar(32)  DEFAULT NULL, -- '比较方式 != > <'
  alert_threshold bigint DEFAULT NULL, -- '告警阀值'
  alert_tactic int DEFAULT NULL, -- '告警策略 1:单次2：连续'
  interval_duration int DEFAULT NULL, -- '间隔时长 单位分钟'
  trigger_duration int DEFAULT NULL, -- '触发时长 单位秒'
  service_role_name varchar(255)  DEFAULT NULL, -- '服务角色名称'
  quota_state int DEFAULT NULL, -- '1: 启用  2：未启用'
  create_time datetime DEFAULT NULL, -- '创建时间'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_alert_rule
-- ----------------------------

CREATE TABLE t_ddh_cluster_alert_rule  (
  id bigint NOT NULL IDENTITY(1,1), -- '自增 ID'
  expression_id bigint NOT NULL, -- '表达式 ID'
  is_predefined varchar(255)  DEFAULT NULL, -- '是否预定义'
  compare_method varchar(255)  NOT NULL, -- '比较方式 如 大于 小于 等于 等'
  threshold_value varchar(255)  NOT NULL, -- '阈值'
  persistent_time bigint NOT NULL, -- '持续时长'
  strategy varchar(255)  NOT NULL, -- '告警策略：单次，连续'
  repeat_interval bigint DEFAULT NULL, -- '连续告警时 间隔时长'
  alert_level varchar(255)  NOT NULL, -- '告警级别'
  alert_desc varchar(4096)  NOT NULL, -- '告警描述'
  receiver_group_id bigint DEFAULT NULL, -- '接收组 ID'
  state varchar(255)  NOT NULL, -- '状态'
  is_delete varchar(255)  DEFAULT NULL, -- '是否删除'
  create_time datetime DEFAULT NULL, -- '创建时间'
  update_time datetime DEFAULT NULL, -- '修改时间'
  cluster_id int DEFAULT NULL, -- '集群id'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_group
-- ----------------------------

CREATE TABLE t_ddh_cluster_group  (
  id int NOT NULL IDENTITY(1,1),
  group_name varchar(255)  DEFAULT NULL,
  cluster_id int DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_host
-- ----------------------------

CREATE TABLE t_ddh_cluster_host  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  create_time datetime DEFAULT NULL, -- '创建时间'
  hostname varchar(32)  DEFAULT NULL, -- '主机名'
  ip varchar(32)  DEFAULT NULL, -- 'IP'
  rack varchar(32)  DEFAULT NULL, -- '机架'
  core_num int DEFAULT NULL, -- '核数'
  total_mem int DEFAULT NULL, -- '总内存'
  total_disk int DEFAULT NULL, -- '总磁盘'
  average_load varchar(32)  DEFAULT NULL, -- '平均负载'
  check_time datetime DEFAULT NULL, -- '检测时间'
  cluster_id varchar(32)  DEFAULT NULL, -- '集群id'
  host_state int DEFAULT NULL, -- '1:健康 2、有一个角色异常3、有多个角色异常'
  managed int DEFAULT NULL, -- '1:受管 2：断线'
  cpu_architecture varchar(255)  DEFAULT NULL, -- 'cpu架构'
  node_label varchar(255)  DEFAULT NULL, -- '节点标签'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_info
-- ----------------------------

CREATE TABLE t_ddh_cluster_info  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  create_by varchar(128)  DEFAULT NULL, -- '创建人'
  create_time datetime DEFAULT NULL, -- '创建时间'
  cluster_name varchar(128)  DEFAULT NULL, -- '集群名称'
  cluster_code varchar(128)  DEFAULT NULL, -- '集群编码'
  cluster_frame varchar(128)  DEFAULT NULL, -- '集群框架'
  frame_version varchar(128)  DEFAULT NULL, -- '集群版本'
  cluster_state int DEFAULT NULL, -- '集群状态 1:待配置2：正在运行'
  frame_id int DEFAULT NULL,
  dep_type VARCHAR(128) NULL,
  kube_config TEXT,
  namespace VARCHAR(128) NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_node_label
-- ----------------------------

CREATE TABLE t_ddh_cluster_node_label  (
  id int NOT NULL IDENTITY(1,1),
  cluster_id int DEFAULT NULL,
  node_label varchar(255)  DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_queue_capacity
-- ----------------------------

CREATE TABLE t_ddh_cluster_queue_capacity  (
  id int NOT NULL IDENTITY(1,1),
  cluster_id int DEFAULT NULL,
  queue_name varchar(255)  DEFAULT NULL,
  capacity varchar(255)  DEFAULT NULL,
  node_label varchar(255)  DEFAULT NULL,
  acl_users varchar(255)  DEFAULT NULL,
  parent varchar(255)  DEFAULT NULL,
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_cluster_rack
-- ----------------------------

CREATE TABLE t_ddh_cluster_rack  (
  id int NOT NULL IDENTITY(1,1),
  rack varchar(255)  DEFAULT NULL,
  cluster_id int DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_role_user
-- ----------------------------

CREATE TABLE t_ddh_cluster_role_user  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  cluster_id int DEFAULT NULL, -- '集群id'
  user_type int DEFAULT NULL, -- '集群用户类型1：管理员2：普通用户'
  user_id int DEFAULT NULL, -- '用户id'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_service_command
-- ----------------------------

CREATE TABLE t_ddh_cluster_service_command  (
  command_id varchar(128)  NOT NULL, -- '主键'
  create_by varchar(32)  DEFAULT NULL, -- '创建人'
  create_time datetime DEFAULT NULL, -- '创建时间'
  command_name varchar(256)  DEFAULT NULL, -- '命令名称'
  command_state int DEFAULT NULL, -- '命令状态 0：待运行 1：正在运行2：成功3：失败4、取消'
  command_progress int DEFAULT NULL, -- '命令进度'
  cluster_id int DEFAULT NULL,
  service_name varchar(128)  DEFAULT NULL,
  command_type int DEFAULT NULL, -- '命令类型1：安装服务 2：启动服务 3：停止服务 4：重启服务 5：更新配置后启动 6：更新配置后重启'
  end_time datetime DEFAULT NULL, -- '结束时间'
  service_instance_id int DEFAULT NULL, -- '服务实例id'
  CONSTRAINT UQ_command_id UNIQUE (command_id)
);


-- ----------------------------
-- Table structure for t_ddh_cluster_service_command_host
-- ----------------------------
CREATE TABLE t_ddh_cluster_service_command_host  (
  command_host_id varchar(128)  NOT NULL DEFAULT '1', -- '主键'
  hostname varchar(32)  DEFAULT NULL, -- '主机'
  command_state int DEFAULT NULL, -- '命令状态 1：正在运行2：成功3：失败4、取消'
  command_progress int DEFAULT NULL, -- '命令进度'
  command_id varchar(128)  DEFAULT NULL, -- '操作指令id'
  create_time datetime DEFAULT NULL,
  CONSTRAINT UQ_command_host_id UNIQUE (command_host_id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_service_command_host_command
-- ----------------------------

CREATE TABLE t_ddh_cluster_service_command_host_command  (
  host_command_id varchar(128)  NOT NULL DEFAULT '1', -- '主键'
  command_name varchar(256)  DEFAULT NULL, -- '指令名称'
  command_state int DEFAULT NULL, -- '指令状态'
  command_progress int DEFAULT NULL, -- '指令进度'
  command_host_id varchar(128)  DEFAULT NULL, -- '主机id'
  hostname varchar(128)  DEFAULT NULL, -- '主机'
  service_role_name varchar(128)  DEFAULT NULL, -- '服务角色名称'
  service_role_type int DEFAULT NULL, -- '服务角色类型'
  command_id varchar(128)  DEFAULT NULL, -- '指令id'
  create_time datetime DEFAULT NULL, -- '创建时间'
  command_type int DEFAULT NULL, -- '1：安装服务 2：启动服务 3：停止服务 4：重启服务 5：更新配置后启动 6：更新配置后重启'
  result_msg text  NULL,
  CONSTRAINT UQ_command_host_command_id UNIQUE (host_command_id)
);


-- ----------------------------
-- Table structure for t_ddh_cluster_service_dashboard
-- ----------------------------

CREATE TABLE t_ddh_cluster_service_dashboard  (
  id int NOT NULL IDENTITY(1,1), -- '主机'
  service_name varchar(128)  DEFAULT NULL, -- '服务名称'
  dashboard_url varchar(256)  DEFAULT NULL, -- '总览页面地址'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_service_instance
-- ----------------------------

CREATE TABLE t_ddh_cluster_service_instance  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  cluster_id int DEFAULT NULL, -- '集群id'
  service_name varchar(32)  DEFAULT NULL, -- '服务名称'
  service_state int DEFAULT NULL, -- '服务状态 1、待安装 2：正在运行 3：存在告警 4：存在异常'
  update_time datetime DEFAULT NULL, -- '更新时间'
  create_time datetime DEFAULT NULL, -- '创建时间'
  need_restart int DEFAULT NULL, -- '是否需要重启 1：正常 2：需要重启'
  frame_service_id int DEFAULT NULL, -- '框架服务id'
  sort_num int DEFAULT NULL, -- '排序字段'
  label varchar(255)  DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_service_instance_role_group
-- ----------------------------

CREATE TABLE t_ddh_cluster_service_instance_role_group  (
  id int NOT NULL IDENTITY(1,1),
  role_group_name varchar(255)  DEFAULT NULL,
  service_instance_id int DEFAULT NULL,
  service_name varchar(255)  DEFAULT NULL,
  cluster_id int DEFAULT NULL,
  role_group_type varchar(255)  DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_service_role_group_config
-- ----------------------------

CREATE TABLE t_ddh_cluster_service_role_group_config  (
  id int NOT NULL IDENTITY(1,1),
  role_group_id int DEFAULT NULL,
  config_json text  NULL,
  config_json_md5 varchar(255)  DEFAULT NULL,
  config_version int DEFAULT NULL,
  config_file_json text  NULL,
  config_file_json_md5 varchar(255)  DEFAULT NULL,
  cluster_id int DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  service_name varchar(255)  DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_service_role_instance
-- ----------------------------

CREATE TABLE t_ddh_cluster_service_role_instance  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  service_role_name varchar(32)  DEFAULT NULL, -- '服务角色名称'
  hostname varchar(32)  DEFAULT NULL, -- '主机'
  service_role_state int DEFAULT NULL, -- '服务角色状态 1:正在运行2：停止'
  update_time datetime DEFAULT NULL, -- '更新时间'
  create_time datetime DEFAULT NULL, -- '创建时间'
  service_id int DEFAULT NULL, -- '服务id'
  role_type int DEFAULT NULL, -- '角色类型 1:master2:worker3:client'
  cluster_id int DEFAULT NULL, -- '集群id'
  service_name varchar(255)  DEFAULT NULL, -- '服务名称'
  role_group_id int DEFAULT NULL, -- '角色组id'
  need_restart int DEFAULT NULL, -- '是否需要重启 1：正常 2：需要重启'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_service_role_instance_webuis
-- ----------------------------

CREATE TABLE t_ddh_cluster_service_role_instance_webuis  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  service_role_instance_id int DEFAULT NULL, -- '服务角色id'
  web_url varchar(256)  DEFAULT NULL, -- 'URL地址'
  service_instance_id int DEFAULT NULL,
  name varchar(255)  DEFAULT NULL,
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_cluster_user
-- ----------------------------

CREATE TABLE t_ddh_cluster_user  (
  id int NOT NULL IDENTITY(1,1),
  username varchar(255)  DEFAULT NULL,
  cluster_id int DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_user_group
-- ----------------------------

CREATE TABLE t_ddh_cluster_user_group  (
  id int NOT NULL IDENTITY(1,1),
  user_id int DEFAULT NULL,
  group_id int DEFAULT NULL,
  cluster_id int DEFAULT NULL,
  user_group_type int DEFAULT NULL, -- '1:主用户组 2：附加组'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_variable
-- ----------------------------

CREATE TABLE t_ddh_cluster_variable  (
  id int NOT NULL IDENTITY(1,1),
  cluster_id int DEFAULT NULL,
  variable_name varchar(255)  DEFAULT NULL,
  variable_value CLOB  DEFAULT NULL,
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_cluster_yarn_queue
-- ----------------------------

CREATE TABLE t_ddh_cluster_yarn_queue  (
  id int NOT NULL IDENTITY(1,1),
  queue_name varchar(255)  DEFAULT NULL,
  min_core int DEFAULT NULL,
  min_mem int DEFAULT NULL,
  max_core int DEFAULT NULL,
  max_mem int DEFAULT NULL,
  app_num int DEFAULT NULL,
  weight int DEFAULT NULL,
  schedule_policy varchar(255)  DEFAULT NULL, -- 'fifo ,fair ,drf'
  allow_preemption int DEFAULT NULL, -- '1: true 2:false'
  cluster_id int DEFAULT NULL,
  am_share varchar(255)  DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_yarn_scheduler
-- ----------------------------

CREATE TABLE t_ddh_cluster_yarn_scheduler  (
  id int NOT NULL IDENTITY(1,1),
  cluster_id int DEFAULT NULL,
  scheduler varchar(255)  DEFAULT NULL,
  in_use int DEFAULT NULL, -- '1: 是  2：否'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_cluster_zk
-- ----------------------------

CREATE TABLE t_ddh_cluster_zk  (
  id int NOT NULL IDENTITY(1,1),
  zk_server varchar(255)  DEFAULT NULL,
  myid int DEFAULT NULL,
  cluster_id int DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_command
-- ----------------------------

CREATE TABLE t_ddh_command  (
  id int NOT NULL,
  command_type int DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_frame_info
-- ----------------------------

CREATE TABLE t_ddh_frame_info  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  frame_name varchar(128)  DEFAULT NULL, -- '框架名称'
  frame_code varchar(128)  DEFAULT NULL, -- '框架编码'
  frame_version varchar(128)  DEFAULT NULL,
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_frame_service
-- ----------------------------

CREATE TABLE t_ddh_frame_service  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  frame_id int DEFAULT NULL, -- '版本id'
  service_name varchar(32)  DEFAULT NULL, -- '服务名称'
  label varchar(255)  DEFAULT NULL,
  service_version varchar(32)  DEFAULT NULL, -- '服务版本'
  service_desc varchar(1024)  DEFAULT NULL, -- '服务描述'
  dependencies varchar(255)  DEFAULT NULL, -- '服务依赖'
  package_name varchar(255)  DEFAULT NULL, -- '安装包名称'
  service_config text  NULL,
  service_json text  NULL,
  service_json_md5 varchar(255)  DEFAULT NULL,
  frame_code varchar(255)  DEFAULT NULL,
  config_file_json text  NULL,
  config_file_json_md5 varchar(255)  DEFAULT NULL,
  decompress_package_name varchar(255)  DEFAULT NULL,
  sort_num int DEFAULT NULL, -- '排序字段'
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_frame_service_role
-- ----------------------------

CREATE TABLE t_ddh_frame_service_role  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  service_id int DEFAULT NULL, -- '服务id'
  service_role_name varchar(32)  DEFAULT NULL, -- '角色名称'
  service_role_type int DEFAULT NULL, -- '角色类型 1:master2:worker3:client'
  cardinality varchar(32)  DEFAULT NULL,
  service_role_json text  NULL,
  service_role_json_md5 varchar(255)  DEFAULT NULL,
  frame_code varchar(255)  DEFAULT NULL,
  jmx_port varchar(255)  DEFAULT NULL,
  log_file varchar(255)  DEFAULT NULL,
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_install_step
-- ----------------------------

CREATE TABLE t_ddh_install_step  (
  id int NOT NULL IDENTITY(1,1),
  step_name varchar(128)  DEFAULT NULL,
  step_desc varchar(256)  DEFAULT NULL,
  install_type int DEFAULT NULL, -- '1:集群配置2：添加服务3：添加主机'
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_notice_group
-- ----------------------------

CREATE TABLE t_ddh_notice_group  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  notice_group_name varchar(32)  DEFAULT NULL, -- '通知组名称'
  create_time datetime DEFAULT NULL, -- '创建时间'
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_notice_group_user
-- ----------------------------

CREATE TABLE t_ddh_notice_group_user  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  notice_group_id int DEFAULT NULL, -- '通知组id'
  user_id int DEFAULT NULL, -- '用户id'
  PRIMARY KEY (id)
);


-- ----------------------------
-- Table structure for t_ddh_role_info
-- ----------------------------

CREATE TABLE t_ddh_role_info  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  role_name varchar(128)  DEFAULT NULL, -- '角色名称'
  role_code varchar(128)  DEFAULT NULL, -- '角色编码'
  create_time datetime DEFAULT NULL, -- '创建时间'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_session
-- ----------------------------

CREATE TABLE t_ddh_session  (
  id varchar(128)  NOT NULL,
  user_id int DEFAULT NULL,
  ip varchar(128)  DEFAULT NULL,
  last_login_time datetime DEFAULT NULL,
  PRIMARY KEY (id)
);



-- ----------------------------
-- Table structure for t_ddh_user_info
-- ----------------------------

CREATE TABLE t_ddh_user_info  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  username varchar(128)  DEFAULT NULL, -- '用户名'
  password varchar(128)  DEFAULT NULL, -- '密码'
  email varchar(128)  DEFAULT NULL, -- '邮箱'
  phone varchar(128)  DEFAULT NULL, -- '手机号'
  create_time datetime DEFAULT NULL, -- '创建时间'
  user_type int DEFAULT NULL, -- '1：超级管理员 2：普通用户'
  PRIMARY KEY (id)
);

-- ----------------------------
-- Table structure for t_ddh_operation_log
-- ----------------------------

CREATE TABLE t_ddh_operation_log  (
  id int NOT NULL IDENTITY(1,1), -- '主键'
  url varchar(128)  DEFAULT NULL, -- '请求地址'
  ip varchar(128)  DEFAULT NULL, -- '客户端ip'
  operation_module varchar(128)  DEFAULT NULL, -- '操作模块'
  operation_type varchar(128)  DEFAULT NULL, -- '操作类型'
  cluster_id int  DEFAULT NULL, -- '集群id'
  host_ids varchar(30)  DEFAULT NULL, -- '主机'
  service_name varchar(30)  DEFAULT NULL, -- '服务名称'
  service_role_instances_ids varchar(30)  DEFAULT NULL, -- '服务实例'
  param CLOB  DEFAULT NULL, -- '请求数据'
  return_code int  DEFAULT NULL, -- '返回状态码'
  return_msg text  DEFAULT NULL, -- '返回说明'
  operate_user varchar(128)  DEFAULT NULL, -- '操作人'
  start_time datetime DEFAULT NULL, -- '操作开始时间'
  end_time datetime DEFAULT NULL, -- '操作结束时间'
  PRIMARY KEY (id)
);

-- ========================================================================
-- 添加表注释（达梦数据库语法）
-- ========================================================================
COMMENT ON TABLE t_ddh_alert_group IS '告警组表';
COMMENT ON TABLE t_ddh_cluster_alert_expression IS '表达式常量表';
COMMENT ON TABLE t_ddh_cluster_alert_group_map IS '告警组关联主机组表';
COMMENT ON TABLE t_ddh_cluster_alert_history IS '告警历史表';
COMMENT ON TABLE t_ddh_cluster_alert_quota IS '告警配额表';
COMMENT ON TABLE t_ddh_cluster_alert_rule IS '告警规则表';
COMMENT ON TABLE t_ddh_cluster_group IS '主机组表';
COMMENT ON TABLE t_ddh_cluster_host IS '集群主机表';
COMMENT ON TABLE t_ddh_cluster_host_command IS '主机操作指令表';
COMMENT ON TABLE t_ddh_cluster_info IS '集群信息表';
COMMENT ON TABLE t_ddh_cluster_kafka_acl IS 'kafka acl表';
COMMENT ON TABLE t_ddh_cluster_kerberos IS 'kerberos表';
COMMENT ON TABLE t_ddh_cluster_node_label IS '集群主机节点标签表';
COMMENT ON TABLE t_ddh_cluster_queue_capacity IS '集群队列容量表';
COMMENT ON TABLE t_ddh_cluster_role_user_map IS '集群角色用户关联表';
COMMENT ON TABLE t_ddh_cluster_service_command IS '集群服务操作指令表';
COMMENT ON TABLE t_ddh_cluster_service_command_host_command IS '集群服务指令主机指令表';
COMMENT ON TABLE t_ddh_cluster_service_dashboard IS '集群服务仪表盘表';
COMMENT ON TABLE t_ddh_cluster_service_instance IS '集群服务实例表';
COMMENT ON TABLE t_ddh_cluster_service_instance_config IS '集群服务实例配置表';
COMMENT ON TABLE t_ddh_cluster_service_instance_role_group IS '角色组表';
COMMENT ON TABLE t_ddh_cluster_service_instance_role_group_config IS '角色组配置表';
COMMENT ON TABLE t_ddh_cluster_service_instance_web_uis IS '集群服务实例WEB UI表';
COMMENT ON TABLE t_ddh_cluster_service_role_instance_webs IS '集群服务角色实例WEB表';
COMMENT ON TABLE t_ddh_cluster_user IS '集群用户表';
COMMENT ON TABLE t_ddh_cluster_user_group IS '集群用户组表';
COMMENT ON TABLE t_ddh_cluster_variable IS '集群全局变量表';
COMMENT ON TABLE t_ddh_frame_info IS '框架信息表';
COMMENT ON TABLE t_ddh_frame_service IS '框架服务表';
COMMENT ON TABLE t_ddh_frame_service_role IS '框架服务角色表';
COMMENT ON TABLE t_ddh_master_role_group_config IS '主角色组配置表';
COMMENT ON TABLE t_ddh_master_service_config IS '主服务配置表';
COMMENT ON TABLE t_ddh_notice_group IS '通知组表';
COMMENT ON TABLE t_ddh_notice_group_user IS '通知组用户表';
COMMENT ON TABLE t_ddh_cluster_service_role_instance_webuis IS '集群服务角色实例WEBUI表';
COMMENT ON TABLE t_ddh_session IS 'session表';
COMMENT ON TABLE t_ddh_user_info IS '用户信息表';
COMMENT ON TABLE t_ddh_operation_log IS '操作日志表';
