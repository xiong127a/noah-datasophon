-- Create Parcel Repository table for DM database
CREATE TABLE t_ddh_parcel_repository (
  id bigint NOT NULL IDENTITY(1,1) COMMENT '主键',
  repo_name varchar(128) NOT NULL COMMENT '存储库名称',
  repo_type varchar(32) NOT NULL DEFAULT 'http' COMMENT '存储库类型：local/http',
  repo_url varchar(512) NOT NULL COMMENT '存储库地址',
  frame_code varchar(64) DEFAULT NULL COMMENT '框架代码（如 DDP-1.2.1）',
  description varchar(512) DEFAULT NULL COMMENT '描述',
  is_default int DEFAULT 0 COMMENT '是否默认存储库：0-否，1-是',
  status int DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by varchar(64) DEFAULT NULL COMMENT '创建人',
  update_by varchar(64) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (id),
  CONSTRAINT uk_repo_name UNIQUE (repo_name)
);

CREATE INDEX idx_repo_type ON t_ddh_parcel_repository(repo_type);
CREATE INDEX idx_frame_code ON t_ddh_parcel_repository(frame_code);

-- Insert default local repository (replaces hardcoded Constants.MASTER_MANAGE_PACKAGE_PATH)
INSERT INTO t_ddh_parcel_repository 
  (repo_name, repo_type, repo_url, description, is_default, status)
VALUES 
  ('本地存储库', 'local', '/opt/datasophon/DDP/packages', 'Master节点本地存储库', 1, 1);

