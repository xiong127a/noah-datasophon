CREATE TABLE misval  (
                                Id bigint NULL DEFAULT NULL,
                                "Sepal.Length" double NULL DEFAULT NULL,
                                "Sepal.Width" double NULL DEFAULT NULL,
                                "Petal.Length" double NULL DEFAULT NULL,
                                "Petal.Width" double NULL DEFAULT NULL,
                                Species text NULL
);

CREATE TABLE t_ddh_config_version_info  (
                                                   version int NOT NULL, -- '版本号'
                                                   ref_type varchar(20) NOT NULL, -- '引用类型(SERVICE/ROLE_GROUP)'
                                                   ref_id int NOT NULL, -- '关联对象ID'
                                                   description varchar(255) NULL DEFAULT NULL, -- '版本描述'
                                                   editor varchar(50) NULL DEFAULT NULL, -- '编辑者'
                                                   edit_time datetime NULL DEFAULT NULL, -- '编辑时间'
                                                   is_current tinyint NULL DEFAULT 0, -- '是否当前使用版本'
                                                   service_code varchar(50) NULL DEFAULT NULL, -- '服务代码'
                                                   user_id int NULL DEFAULT NULL,
                                                   PRIMARY KEY (version, ref_type, ref_id)
);

ALTER TABLE t_ddh_frame_service MODIFY COLUMN service_config CLOB NULL;

ALTER TABLE t_ddh_frame_service MODIFY COLUMN service_json CLOB NULL;

ALTER TABLE t_ddh_frame_service MODIFY COLUMN config_file_json CLOB NULL;

ALTER TABLE t_ddh_operation_log MODIFY COLUMN param CLOB NULL;
