-- =============================================
-- db-scheduler 调度框架表创建脚本 (达梦数据库版本)
-- 版本: 3.0.0.6  
-- 作者: 任相鹏 <635887935@qq.com>
-- 日期: 2025-08-28
-- 描述: 为db-scheduler框架创建必要的数据库表（达梦数据库适配版本）
-- =============================================

-- 1. 主调度任务表 (db-scheduler 达梦数据库标准表结构)
CREATE TABLE scheduled_tasks (
  task_name varchar(100) NOT NULL, -- '任务名称'
  task_instance varchar(100) NOT NULL, -- '任务实例ID'
  task_data blob, -- '任务数据(JSON格式)'
  execution_time datetime NOT NULL, -- '计划执行时间'
  picked tinyint NOT NULL DEFAULT 0, -- '是否被选中执行(0-未选中,1-已选中)'
  picked_by varchar(50), -- '执行器标识'
  last_success datetime, -- '最后成功执行时间'
  last_failure datetime, -- '最后失败执行时间'
  consecutive_failures int DEFAULT 0, -- '连续失败次数'
  last_heartbeat datetime, -- '最后心跳时间'
  version bigint NOT NULL DEFAULT 1, -- '版本号(乐观锁)'
  priority smallint, -- '任务优先级'
  PRIMARY KEY (task_name, task_instance)
);

-- 为 scheduled_tasks 表添加索引
CREATE INDEX execution_time_idx ON scheduled_tasks (execution_time);
CREATE INDEX last_heartbeat_idx ON scheduled_tasks (last_heartbeat);
CREATE INDEX priority_execution_time_idx ON scheduled_tasks (priority DESC, execution_time ASC);

-- 为 scheduled_tasks 表添加注释
COMMENT ON TABLE scheduled_tasks IS 'db-scheduler 主调度任务表';
COMMENT ON COLUMN scheduled_tasks.task_name IS '任务名称';
COMMENT ON COLUMN scheduled_tasks.task_instance IS '任务实例ID';
COMMENT ON COLUMN scheduled_tasks.task_data IS '任务数据(JSON格式)';
COMMENT ON COLUMN scheduled_tasks.execution_time IS '计划执行时间';
COMMENT ON COLUMN scheduled_tasks.picked IS '是否被选中执行(0-未选中,1-已选中)';
COMMENT ON COLUMN scheduled_tasks.picked_by IS '执行器标识';
COMMENT ON COLUMN scheduled_tasks.last_success IS '最后成功执行时间';
COMMENT ON COLUMN scheduled_tasks.last_failure IS '最后失败执行时间';
COMMENT ON COLUMN scheduled_tasks.consecutive_failures IS '连续失败次数';
COMMENT ON COLUMN scheduled_tasks.last_heartbeat IS '最后心跳时间';
COMMENT ON COLUMN scheduled_tasks.version IS '版本号(乐观锁)';
COMMENT ON COLUMN scheduled_tasks.priority IS '任务优先级';

-- 2. 任务执行历史日志表 (db-scheduler-log 达梦数据库扩展表)
CREATE TABLE scheduled_execution_logs (
  id bigint NOT NULL, -- '主键ID'
  task_name varchar(100) NOT NULL, -- '任务名称'
  task_instance varchar(100) NOT NULL, -- '任务实例ID'
  task_data blob, -- '任务数据快照'
  picked_by varchar(50), -- '执行器标识'
  time_started datetime NOT NULL, -- '任务开始执行时间'
  time_finished datetime NOT NULL, -- '任务完成时间'
  succeeded tinyint NOT NULL, -- '执行是否成功(0-失败,1-成功)'
  duration_ms bigint NOT NULL, -- '执行耗时(毫秒)'
  exception_class varchar(1000), -- '异常类名'
  exception_message blob, -- '异常消息'
  exception_stacktrace blob, -- '异常堆栈信息'
  PRIMARY KEY (id)
);

-- 为 scheduled_execution_logs 表添加索引
CREATE INDEX stl_started_idx ON scheduled_execution_logs (time_started);
CREATE INDEX stl_task_name_idx ON scheduled_execution_logs (task_name);
CREATE INDEX stl_exception_class_idx ON scheduled_execution_logs (exception_class);

-- 为 scheduled_execution_logs 表添加注释
COMMENT ON TABLE scheduled_execution_logs IS 'db-scheduler 任务执行历史日志表';
COMMENT ON COLUMN scheduled_execution_logs.id IS '主键ID';
COMMENT ON COLUMN scheduled_execution_logs.task_name IS '任务名称';
COMMENT ON COLUMN scheduled_execution_logs.task_instance IS '任务实例ID';
COMMENT ON COLUMN scheduled_execution_logs.task_data IS '任务数据快照';
COMMENT ON COLUMN scheduled_execution_logs.picked_by IS '执行器标识';
COMMENT ON COLUMN scheduled_execution_logs.time_started IS '任务开始执行时间';
COMMENT ON COLUMN scheduled_execution_logs.time_finished IS '任务完成时间';
COMMENT ON COLUMN scheduled_execution_logs.succeeded IS '执行是否成功(0-失败,1-成功)';
COMMENT ON COLUMN scheduled_execution_logs.duration_ms IS '执行耗时(毫秒)';
COMMENT ON COLUMN scheduled_execution_logs.exception_class IS '异常类名';
COMMENT ON COLUMN scheduled_execution_logs.exception_message IS '异常消息';
COMMENT ON COLUMN scheduled_execution_logs.exception_stacktrace IS '异常堆栈信息';

-- =============================================
-- 创建序列用于生成主键ID (达梦数据库特有)
-- =============================================

-- 创建序列用于scheduled_execution_logs表的主键
CREATE SEQUENCE seq_scheduled_execution_logs_id
START WITH 1
INCREMENT BY 1
CACHE 20;

-- =============================================
-- 验证脚本执行结果
-- =============================================

-- 验证表是否创建成功 (达梦数据库语法)
SELECT 
    TABLE_NAME as "表名",
    TABLE_COMMENT as "表注释"
FROM 
    USER_TAB_COMMENTS 
WHERE 
    TABLE_NAME IN ('SCHEDULED_TASKS', 'SCHEDULED_EXECUTION_LOGS');

-- 验证索引是否创建成功 (达梦数据库语法)
SELECT 
    INDEX_NAME as "索引名",
    TABLE_NAME as "表名",
    COLUMN_NAME as "索引字段",
    COLUMN_POSITION as "字段位置"
FROM 
    USER_IND_COLUMNS 
WHERE 
    TABLE_NAME IN ('SCHEDULED_TASKS', 'SCHEDULED_EXECUTION_LOGS')
ORDER BY TABLE_NAME, INDEX_NAME, COLUMN_POSITION;

-- 验证序列是否创建成功
SELECT 
    SEQUENCE_NAME as "序列名",
    MIN_VALUE as "最小值",
    MAX_VALUE as "最大值",
    INCREMENT_BY as "递增步长",
    CACHE_SIZE as "缓存大小"
FROM 
    USER_SEQUENCES 
WHERE 
    SEQUENCE_NAME = 'SEQ_SCHEDULED_EXECUTION_LOGS_ID';
