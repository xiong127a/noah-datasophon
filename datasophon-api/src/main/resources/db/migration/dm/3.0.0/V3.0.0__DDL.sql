-- 删除旧表
DROP TABLE IF EXISTS t_ddh_session;
DROP TABLE IF EXISTS t_ddh_access_token;

-- 创建新表
CREATE TABLE t_ddh_auth_token (
  id bigint NOT NULL, -- '主键，使用雪花算法生成'
  user_id int NOT NULL, -- '关联的用户ID'
  token varchar(2048) NOT NULL, -- 'JWT访问令牌'
  refresh_token varchar(2048) DEFAULT NULL, -- '刷新令牌'
  token_type varchar(50) DEFAULT 'Bearer', -- '令牌类型，默认为Bearer'
  client_ip varchar(128) DEFAULT NULL, -- '客户端IP地址'
  user_agent varchar(512) DEFAULT NULL, -- '客户端浏览器信息'
  issued_at datetime NOT NULL, -- '令牌颁发时间'
  expires_at datetime NOT NULL, -- '令牌过期时间'
  last_access_time datetime DEFAULT NULL, -- '最后访问时间'
  is_revoked tinyint DEFAULT 0, -- '是否已被撤销，0-有效，1-已撤销'
  revoked_reason varchar(128) DEFAULT NULL, -- '撤销原因'
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, -- '记录创建时间'
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, -- '记录更新时间'
  PRIMARY KEY (id)
);

-- 添加索引
CREATE INDEX idx_user_id ON t_ddh_auth_token (user_id);
CREATE INDEX idx_expires_at ON t_ddh_auth_token (expires_at);
