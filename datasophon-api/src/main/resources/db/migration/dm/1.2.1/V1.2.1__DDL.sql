CREATE TABLE "DATASOPHON"."t_ddh_access_token"
(
 "id" INT NOT NULL,
 "user_id" INT NULL,
 "token" VARCHAR(255) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "update_time" TIMESTAMP(0) NULL,
 "expire_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_alert_group"
(
 "id" INT IDENTITY(24,1) NOT NULL,
 "alert_group_name" VARCHAR(32) NULL,
 "alert_group_category" VARCHAR(32) NULL,
 "create_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_alert_expression"
(
 "id" BIGINT IDENTITY(134002,1) NOT NULL,
 "name" VARCHAR(255) NULL,
 "expr" VARCHAR(4096) NULL,
 "service_category" VARCHAR(255) NULL,
 "value_type" VARCHAR(255) NULL,
 "is_predefined" VARCHAR(255) NULL,
 "state" VARCHAR(255) NOT NULL,
 "is_delete" VARCHAR(255) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "update_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_alert_group_map"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "cluster_id" INT NULL,
 "alert_group_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_alert_history"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "alert_group_name" VARCHAR(32) NULL,
 "alert_target_name" VARCHAR(32) NULL,
 "alert_info" VARCHAR(1024) NULL,
 "alert_advice" VARCHAR(1024) NULL,
 "hostname" VARCHAR(32) NULL,
 "alert_level" INT NULL,
 "is_enabled" INT NULL,
 "service_role_instance_id" INT NULL,
 "service_instance_id" INT NULL,
 "create_time" TIMESTAMP(0) NULL,
 "update_time" TIMESTAMP(0) NULL,
 "cluster_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_alert_quota"
(
 "id" INT IDENTITY(633,1) NOT NULL,
 "alert_quota_name" VARCHAR(255) NULL,
 "service_category" VARCHAR(32) NULL,
 "alert_expr" VARCHAR(1024) NULL,
 "alert_level" INT NULL,
 "alert_group_id" INT NULL,
 "notice_group_id" INT NULL,
 "alert_advice" VARCHAR(1024) NULL,
 "compare_method" VARCHAR(32) NULL,
 "alert_threshold" BIGINT NULL,
 "alert_tactic" INT NULL,
 "interval_duration" INT NULL,
 "trigger_duration" INT NULL,
 "service_role_name" VARCHAR(255) NULL,
 "quota_state" INT NULL,
 "create_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_alert_rule"
(
 "id" BIGINT IDENTITY(134002,1) NOT NULL,
 "expression_id" BIGINT NOT NULL,
 "is_predefined" VARCHAR(255) NULL,
 "compare_method" VARCHAR(255) NOT NULL,
 "threshold_value" VARCHAR(255) NOT NULL,
 "persistent_time" BIGINT NOT NULL,
 "strategy" VARCHAR(255) NOT NULL,
 "repeat_interval" BIGINT NULL,
 "alert_level" VARCHAR(255) NOT NULL,
 "alert_desc" VARCHAR(4096) NOT NULL,
 "receiver_group_id" BIGINT NULL,
 "state" VARCHAR(255) NOT NULL,
 "is_delete" VARCHAR(255) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "update_time" TIMESTAMP(0) NULL,
 "cluster_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_group"
(
 "id" INT IDENTITY(6,1) NOT NULL,
 "group_name" VARCHAR(255) NULL,
 "cluster_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_host"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "create_time" TIMESTAMP(0) NULL,
 "hostname" VARCHAR(32) NULL,
 "ip" VARCHAR(32) NULL,
 "rack" VARCHAR(32) NULL,
 "core_num" INT NULL,
 "total_mem" INT NULL,
 "total_disk" INT NULL,
 "used_mem" INT NULL,
 "used_disk" INT NULL,
 "average_load" VARCHAR(32) NULL,
 "check_time" TIMESTAMP(0) NULL,
 "cluster_id" VARCHAR(32) NULL,
 "host_state" INT NULL,
 "managed" INT NULL,
 "cpu_architecture" VARCHAR(255) NULL,
 "node_label" VARCHAR(255) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_info"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "create_by" VARCHAR(128) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "cluster_name" VARCHAR(128) NULL,
 "cluster_code" VARCHAR(128) NULL,
 "cluster_frame" VARCHAR(128) NULL,
 "frame_version" VARCHAR(128) NULL,
 "cluster_state" INT NULL,
 "frame_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_node_label"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "cluster_id" INT NULL,
 "node_label" VARCHAR(255) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_queue_capacity"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "cluster_id" INT NULL,
 "queue_name" VARCHAR(255) NULL,
 "capacity" VARCHAR(255) NULL,
 "node_label" VARCHAR(255) NULL,
 "acl_users" VARCHAR(255) NULL,
 "parent" VARCHAR(255) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_rack"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "rack" VARCHAR(255) NULL,
 "cluster_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_role_user"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "cluster_id" INT NULL,
 "user_type" INT NULL,
 "user_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_service_command"
(
 "command_id" VARCHAR(128) NOT NULL,
 "create_by" VARCHAR(32) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "command_name" VARCHAR(256) NULL,
 "command_state" INT NULL,
 "command_progress" INT NULL,
 "cluster_id" INT NULL,
 "service_name" VARCHAR(128) NULL,
 "command_type" INT NULL,
 "end_time" TIMESTAMP(0) NULL,
 "service_instance_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_service_command_host"
(
 "command_host_id" VARCHAR(128) DEFAULT '1'
 NOT NULL,
 "hostname" VARCHAR(32) NULL,
 "command_state" INT NULL,
 "command_progress" INT NULL,
 "command_id" VARCHAR(128) NULL,
 "create_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_service_command_host_command"
(
 "host_command_id" VARCHAR(128) DEFAULT '1'
 NOT NULL,
 "command_name" VARCHAR(256) NULL,
 "command_state" INT NULL,
 "command_progress" INT NULL,
 "command_host_id" VARCHAR(128) NULL,
 "hostname" VARCHAR(128) NULL,
 "service_role_name" VARCHAR(128) NULL,
 "service_role_type" INT NULL,
 "command_id" VARCHAR(128) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "command_type" INT NULL,
 "result_msg" TEXT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_service_dashboard"
(
 "id" INT IDENTITY(22,1) NOT NULL,
 "service_name" VARCHAR(128) NULL,
 "dashboard_url" VARCHAR(256) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_service_instance"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "cluster_id" INT NULL,
 "service_name" VARCHAR(32) NULL,
 "service_state" INT NULL,
 "update_time" TIMESTAMP(0) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "need_restart" INT NULL,
 "frame_service_id" INT NULL,
 "sort_num" INT NULL,
 "label" VARCHAR(255) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_service_instance_role_group"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "role_group_name" VARCHAR(255) NULL,
 "service_instance_id" INT NULL,
 "service_name" VARCHAR(255) NULL,
 "cluster_id" INT NULL,
 "role_group_type" VARCHAR(255) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "need_restart" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_service_role_group_config"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "role_group_id" INT NULL,
 "config_json" TEXT NULL,
 "config_json_md5" VARCHAR(255) NULL,
 "config_version" INT NULL,
 "config_file_json" TEXT NULL,
 "config_file_json_md5" VARCHAR(255) NULL,
 "cluster_id" INT NULL,
 "create_time" TIMESTAMP(0) NULL,
 "update_time" TIMESTAMP(0) NULL,
 "service_name" VARCHAR(255) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_service_role_instance"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "service_role_name" VARCHAR(32) NULL,
 "hostname" VARCHAR(32) NULL,
 "service_role_state" INT NULL,
 "update_time" TIMESTAMP(0) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "service_id" INT NULL,
 "role_type" INT NULL,
 "cluster_id" INT NULL,
 "service_name" VARCHAR(255) NULL,
 "role_group_id" INT NULL,
 "need_restart" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_service_role_instance_webuis"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "service_role_instance_id" INT NULL,
 "web_url" VARCHAR(256) NULL,
 "service_instance_id" INT NULL,
 "name" VARCHAR(255) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_tenant"
(
 "id" INT IDENTITY(82,1) NOT NULL,
 "cluster_id" INT NULL,
 "tenant_name" VARCHAR(255) NULL,
 "hdfs_resource_list" TEXT NULL,
 "yarn_resource_list" TEXT NULL,
 "hive_resource_list" TEXT NULL,
 "hbase_resource_list" TEXT NULL,
 "kafka_resource_list" TEXT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_user"
(
 "id" INT IDENTITY(11,1) NOT NULL,
 "username" VARCHAR(255) NULL,
 "cluster_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_user_group"
(
 "id" INT IDENTITY(11,1) NOT NULL,
 "user_id" INT NULL,
 "group_id" INT NULL,
 "cluster_id" INT NULL,
 "user_group_type" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_user_tenant"
(
 "id" INT IDENTITY(112,1) NOT NULL,
 "cluster_id" INT NULL,
 "user_id" INT NULL,
 "tenant_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_variable"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "cluster_id" INT NULL,
 "variable_name" VARCHAR(255) NULL,
 "variable_value" VARCHAR(255) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_yarn_queue"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "queue_name" VARCHAR(255) NULL,
 "min_core" INT NULL,
 "min_mem" INT NULL,
 "max_core" INT NULL,
 "max_mem" INT NULL,
 "app_num" INT NULL,
 "weight" INT NULL,
 "schedule_policy" VARCHAR(255) NULL,
 "allow_preemption" INT NULL,
 "cluster_id" INT NULL,
 "am_share" VARCHAR(255) NULL,
 "create_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_yarn_scheduler"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "cluster_id" INT NULL,
 "scheduler" VARCHAR(255) NULL,
 "in_use" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_cluster_zk"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "zk_server" VARCHAR(255) NULL,
 "myid" INT NULL,
 "cluster_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_command"
(
 "id" INT NOT NULL,
 "command_type" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_frame_info"
(
 "id" INT IDENTITY(3,1) NOT NULL,
 "frame_name" VARCHAR(128) NULL,
 "frame_code" VARCHAR(128) NULL,
 "frame_version" VARCHAR(128) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_frame_service"
(
 "id" INT IDENTITY(73,1) NOT NULL,
 "frame_id" INT NULL,
 "service_name" VARCHAR(32) NULL,
 "label" VARCHAR(255) NULL,
 "service_version" VARCHAR(32) NULL,
 "service_desc" VARCHAR(1024) NULL,
 "dependencies" VARCHAR(255) NULL,
 "package_name" VARCHAR(255) NULL,
 "service_config" TEXT NULL,
 "service_json" TEXT NULL,
 "service_json_md5" VARCHAR(255) NULL,
 "frame_code" VARCHAR(255) NULL,
 "config_file_json" TEXT NULL,
 "config_file_json_md5" VARCHAR(255) NULL,
 "decompress_package_name" VARCHAR(255) NULL,
 "sort_num" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_frame_service_role"
(
 "id" INT IDENTITY(137,1) NOT NULL,
 "service_id" INT NULL,
 "service_role_name" VARCHAR(32) NULL,
 "service_role_type" INT NULL,
 "cardinality" VARCHAR(32) NULL,
 "service_role_json" TEXT NULL,
 "service_role_json_md5" VARCHAR(255) NULL,
 "frame_code" VARCHAR(255) NULL,
 "jmx_port" VARCHAR(255) NULL,
 "log_file" VARCHAR(255) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_install_step"
(
 "id" INT IDENTITY(10,1) NOT NULL,
 "step_name" VARCHAR(128) NULL,
 "step_desc" VARCHAR(256) NULL,
 "install_type" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_notice_group"
(
 "id" INT IDENTITY(2,1) NOT NULL,
 "notice_group_name" VARCHAR(32) NULL,
 "create_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_notice_group_user"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "notice_group_id" INT NULL,
 "user_id" INT NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_operation_log"
(
 "id" INT IDENTITY(2,1) NOT NULL,
 "url" VARCHAR(128) NULL,
 "ip" VARCHAR(128) NULL,
 "operation_module" VARCHAR(128) NULL,
 "operation_type" VARCHAR(128) NULL,
 "cluster_id" INT NULL,
 "host_ids" VARCHAR(30) NULL,
 "service_name" VARCHAR(30) NULL,
 "service_role_instances_ids" VARCHAR(30) NULL,
 "param" TEXT NULL,
 "return_code" INT NULL,
 "return_msg" TEXT NULL,
 "operate_user" VARCHAR(128) NULL,
 "start_time" TIMESTAMP(0) NULL,
 "end_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_role_info"
(
 "id" INT IDENTITY(1,1) NOT NULL,
 "role_name" VARCHAR(128) NULL,
 "role_code" VARCHAR(128) NULL,
 "create_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_session"
(
 "id" VARCHAR(128) NOT NULL,
 "user_id" INT NULL,
 "ip" VARCHAR(128) NULL,
 "last_login_time" TIMESTAMP(0) NULL
);
CREATE TABLE "DATASOPHON"."t_ddh_user_info"
(
 "id" INT IDENTITY(2,1) NOT NULL,
 "username" VARCHAR(128) NULL,
 "password" VARCHAR(128) NULL,
 "email" VARCHAR(128) NULL,
 "phone" VARCHAR(128) NULL,
 "create_time" TIMESTAMP(0) NULL,
 "user_type" INT NULL
);

ALTER TABLE "DATASOPHON"."t_ddh_access_token" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_alert_group" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_alert_expression" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_alert_group_map" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_alert_history" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_alert_quota" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_alert_rule" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_group" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_host" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_info" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_node_label" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_queue_capacity" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_rack" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_role_user" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_service_dashboard" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_service_instance" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_service_instance_role_group" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_service_role_group_config" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_service_role_instance" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_service_role_instance_webuis" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_tenant" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_user" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_user_group" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_user_tenant" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_variable" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_yarn_queue" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_yarn_scheduler" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_zk" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_command" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_frame_info" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_frame_service" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_frame_service_role" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_install_step" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_notice_group" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_notice_group_user" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_operation_log" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_role_info" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_session" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_user_info" ADD CONSTRAINT  PRIMARY KEY("id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_service_command" ADD CONSTRAINT "command_id" UNIQUE("command_id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_service_command_host" ADD CONSTRAINT "command_host_id" UNIQUE("command_host_id") ;

ALTER TABLE "DATASOPHON"."t_ddh_cluster_service_command_host_command" ADD CONSTRAINT "command_host_command_id" UNIQUE("host_command_id") ;

COMMENT ON TABLE "DATASOPHON"."t_ddh_alert_group" IS '告警组表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_alert_group"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_alert_group"."alert_group_name" IS '告警组名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_alert_group"."alert_group_category" IS '告警组类别';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_alert_group"."create_time" IS '创建时间';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_alert_expression" IS '表达式常量表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."id" IS '自增 ID';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."name" IS '指标名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."expr" IS '监控指标表达式';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."service_category" IS '服务类别';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."value_type" IS '阈值类型  BOOL  INT  FLOAT  ';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."is_predefined" IS '是否预定义';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."state" IS '表达式状态';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."is_delete" IS '是否删除';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_expression"."update_time" IS '修改时间';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_alert_history" IS '集群告警历史表 ';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."alert_group_name" IS '告警组';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."alert_target_name" IS '告警指标';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."alert_info" IS '告警详情';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."alert_advice" IS '告警建议';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."hostname" IS '主机';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."alert_level" IS '告警级别 1：警告2：异常';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."is_enabled" IS '是否处理 1:未处理2：已处理';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."service_role_instance_id" IS '集群服务角色实例id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."service_instance_id" IS '集群服务实例id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."update_time" IS '更新时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_history"."cluster_id" IS '集群id';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_alert_quota" IS '集群告警指标表 ';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."alert_quota_name" IS '告警指标名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."service_category" IS '服务分类';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."alert_expr" IS '告警指标表达式';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."alert_level" IS '告警级别 1:警告2：异常';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."alert_group_id" IS '告警组';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."notice_group_id" IS '通知组';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."alert_advice" IS '告警建议';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."compare_method" IS '比较方式 != > <';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."alert_threshold" IS '告警阀值';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."alert_tactic" IS '告警策略 1:单次2：连续';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."interval_duration" IS '间隔时长 单位分钟';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."trigger_duration" IS '触发时长 单位秒';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."service_role_name" IS '服务角色名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."quota_state" IS '1: 启用  2：未启用';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_quota"."create_time" IS '创建时间';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_alert_rule" IS '规则表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."id" IS '自增 ID';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."expression_id" IS '表达式 ID';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."is_predefined" IS '是否预定义';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."compare_method" IS '比较方式 如 大于 小于 等于 等';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."threshold_value" IS '阈值';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."persistent_time" IS '持续时长';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."strategy" IS '告警策略：单次，连续';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."repeat_interval" IS '连续告警时 间隔时长';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."alert_level" IS '告警级别';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."alert_desc" IS '告警描述';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."receiver_group_id" IS '接收组 ID';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."state" IS '状态';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."is_delete" IS '是否删除';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."update_time" IS '修改时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_alert_rule"."cluster_id" IS '集群id';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_host" IS '集群主机表 ';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."hostname" IS '主机名';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."ip" IS 'IP';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."rack" IS '机架';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."core_num" IS '核数';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."total_mem" IS '总内存';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."total_disk" IS '总磁盘';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."used_mem" IS '已用内存';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."used_disk" IS '已用磁盘';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."average_load" IS '平均负载';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."check_time" IS '检测时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."cluster_id" IS '集群id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."host_state" IS '1:健康 2、有一个角色异常3、有多个角色异常';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."managed" IS '1:受管 2：断线';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."cpu_architecture" IS 'cpu架构';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_host"."node_label" IS '节点标签';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_info" IS '集群信息表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_info"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_info"."create_by" IS '创建人';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_info"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_info"."cluster_name" IS '集群名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_info"."cluster_code" IS '集群编码';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_info"."cluster_frame" IS '集群框架';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_info"."frame_version" IS '集群版本';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_info"."cluster_state" IS '集群状态 1:待配置2：正在运行';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_role_user" IS '集群角色用户中间表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_role_user"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_role_user"."cluster_id" IS '集群id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_role_user"."user_type" IS '集群用户类型1：管理员2：普通用户';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_role_user"."user_id" IS '用户id';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_service_command" IS '集群服务操作指令表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command"."command_id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command"."create_by" IS '创建人';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command"."command_name" IS '命令名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command"."command_state" IS '命令状态 0：待运行 1：正在运行2：成功3：失败4、取消';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command"."command_progress" IS '命令进度';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command"."command_type" IS '命令类型1：安装服务 2：启动服务 3：停止服务 4：重启服务 5：更新配置后启动 6：更新配置后重启';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command"."end_time" IS '结束时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command"."service_instance_id" IS '服务实例id';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_service_command_host" IS '集群服务操作指令主机表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host"."command_host_id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host"."hostname" IS '主机';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host"."command_state" IS '命令状态 1：正在运行2：成功3：失败4、取消';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host"."command_progress" IS '命令进度';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host"."command_id" IS '操作指令id';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_service_command_host_command" IS '集群服务操作指令主机指令表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."host_command_id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."command_name" IS '指令名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."command_state" IS '指令状态';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."command_progress" IS '指令进度';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."command_host_id" IS '主机id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."hostname" IS '主机';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."service_role_name" IS '服务角色名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."service_role_type" IS '服务角色类型';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."command_id" IS '指令id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_command_host_command"."command_type" IS '1：安装服务 2：启动服务 3：停止服务 4：重启服务 5：更新配置后启动 6：更新配置后重启';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_service_dashboard" IS '集群服务总览仪表盘';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_dashboard"."id" IS '主机';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_dashboard"."service_name" IS '服务名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_dashboard"."dashboard_url" IS '总览页面地址';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_service_instance" IS '集群服务表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_instance"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_instance"."cluster_id" IS '集群id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_instance"."service_name" IS '服务名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_instance"."service_state" IS '服务状态 1、待安装 2：正在运行 3：存在告警 4：存在异常';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_instance"."update_time" IS '更新时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_instance"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_instance"."need_restart" IS '是否需要重启 1：正常 2：需要重启';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_instance"."frame_service_id" IS '框架服务id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_instance"."sort_num" IS '排序字段';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_service_role_instance" IS '集群服务角色实例表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."service_role_name" IS '服务角色名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."hostname" IS '主机';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."service_role_state" IS '服务角色状态 1:正在运行2：停止';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."update_time" IS '更新时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."service_id" IS '服务id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."role_type" IS '角色类型 1:master2:worker3:client';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."cluster_id" IS '集群id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."service_name" IS '服务名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."role_group_id" IS '角色组id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance"."need_restart" IS '是否需要重启 1：正常 2：需要重启';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_service_role_instance_webuis" IS '集群服务角色对应web ui表 ';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance_webuis"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance_webuis"."service_role_instance_id" IS '服务角色id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_service_role_instance_webuis"."web_url" IS 'URL地址';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_tenant" IS '租户表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_tenant"."tenant_name" IS '租户名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_tenant"."hdfs_resource_list" IS 'hdfs资源列表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_tenant"."yarn_resource_list" IS 'yarn资源列表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_tenant"."hive_resource_list" IS 'hive资源列表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_tenant"."hbase_resource_list" IS 'hbase资源列表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_tenant"."kafka_resource_list" IS 'kafka资源列表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_user_group"."user_group_type" IS '1:主用户组 2：附加组';

COMMENT ON TABLE "DATASOPHON"."t_ddh_cluster_user_tenant" IS '租户授权表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_yarn_queue"."schedule_policy" IS 'fifo ,fair ,drf';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_yarn_queue"."allow_preemption" IS '1: true 2:false';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_cluster_yarn_scheduler"."in_use" IS '1: 是  2：否';

COMMENT ON TABLE "DATASOPHON"."t_ddh_frame_info" IS '集群框架表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_info"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_info"."frame_name" IS '框架名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_info"."frame_code" IS '框架编码';

COMMENT ON TABLE "DATASOPHON"."t_ddh_frame_service" IS '集群框架版本服务表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service"."frame_id" IS '版本id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service"."service_name" IS '服务名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service"."service_version" IS '服务版本';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service"."service_desc" IS '服务描述';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service"."dependencies" IS '服务依赖';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service"."package_name" IS '安装包名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service"."sort_num" IS '排序字段';

COMMENT ON TABLE "DATASOPHON"."t_ddh_frame_service_role" IS '框架服务角色表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service_role"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service_role"."service_id" IS '服务id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service_role"."service_role_name" IS '角色名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_frame_service_role"."service_role_type" IS '角色类型 1:master2:worker3:client';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_install_step"."install_type" IS '1:集群配置2：添加服务3：添加主机';

COMMENT ON TABLE "DATASOPHON"."t_ddh_notice_group" IS '通知组表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_notice_group"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_notice_group"."notice_group_name" IS '通知组名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_notice_group"."create_time" IS '创建时间';

COMMENT ON TABLE "DATASOPHON"."t_ddh_notice_group_user" IS '通知组-用户中间表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_notice_group_user"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_notice_group_user"."notice_group_id" IS '通知组id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_notice_group_user"."user_id" IS '用户id';

COMMENT ON TABLE "DATASOPHON"."t_ddh_operation_log" IS '操作日志表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."url" IS '请求地址';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."ip" IS '客户端ip';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."operation_module" IS '操作模块';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."operation_type" IS '操作类型';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."cluster_id" IS '集群id';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."host_ids" IS '主机';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."service_name" IS '服务名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."service_role_instances_ids" IS '服务实例';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."param" IS '请求数据';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."return_code" IS '返回状态码';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."return_msg" IS '返回说明';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."operate_user" IS '操作人';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."start_time" IS '操作开始时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_operation_log"."end_time" IS '操作结束时间';

COMMENT ON TABLE "DATASOPHON"."t_ddh_role_info" IS '角色信息表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_role_info"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_role_info"."role_name" IS '角色名称';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_role_info"."role_code" IS '角色编码';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_role_info"."create_time" IS '创建时间';

COMMENT ON TABLE "DATASOPHON"."t_ddh_user_info" IS '用户信息表';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."id" IS '主键';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."username" IS '用户名';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."password" IS '密码';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."email" IS '邮箱';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."phone" IS '手机号';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."create_time" IS '创建时间';

COMMENT ON COLUMN "DATASOPHON"."t_ddh_user_info"."user_type" IS '1：超级管理员 2：普通用户';

COMMIT;