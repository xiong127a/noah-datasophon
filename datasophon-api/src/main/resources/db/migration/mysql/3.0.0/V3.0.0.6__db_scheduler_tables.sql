-- =============================================
-- db-scheduler 调度框架表创建脚本
-- 版本: 3.0.0.6  
-- 作者: 任相鹏 <635887935@qq.com>
-- 日期: 2025-08-28
-- 描述: 为db-scheduler框架创建必要的数据库表
-- =============================================

-- 1. 主调度任务表 (db-scheduler 标准表结构)
CREATE TABLE IF NOT EXISTS `scheduled_tasks` (
  `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
  `task_instance` VARCHAR(100) NOT NULL COMMENT '任务实例ID',
  `task_data` BLOB COMMENT '任务数据(JSON格式)',
  `execution_time` TIMESTAMP(6) NOT NULL COMMENT '计划执行时间',
  `picked` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否被选中执行',
  `picked_by` VARCHAR(50) COMMENT '执行器标识',
  `last_success` TIMESTAMP(6) NULL COMMENT '最后成功执行时间',
  `last_failure` TIMESTAMP(6) NULL COMMENT '最后失败执行时间',
  `consecutive_failures` INT DEFAULT 0 COMMENT '连续失败次数',
  `last_heartbeat` TIMESTAMP(6) NULL COMMENT '最后心跳时间',
  `version` BIGINT NOT NULL DEFAULT 1 COMMENT '版本号(乐观锁)',
  `priority` SMALLINT COMMENT '任务优先级',
  PRIMARY KEY (`task_name`, `task_instance`),
  INDEX `execution_time_idx` (`execution_time`),
  INDEX `last_heartbeat_idx` (`last_heartbeat`),
  INDEX `priority_execution_time_idx` (`priority` DESC, `execution_time` ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='db-scheduler 主调度任务表';

-- 2. 任务执行历史日志表 (db-scheduler-log 扩展表)
CREATE TABLE IF NOT EXISTS `scheduled_execution_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
  `task_instance` VARCHAR(100) NOT NULL COMMENT '任务实例ID',
  `task_data` BLOB COMMENT '任务数据快照',
  `picked_by` VARCHAR(50) COMMENT '执行器标识',
  `time_started` TIMESTAMP(6) NOT NULL COMMENT '任务开始执行时间',
  `time_finished` TIMESTAMP(6) NOT NULL COMMENT '任务完成时间',
  `succeeded` BOOLEAN NOT NULL COMMENT '执行是否成功',
  `duration_ms` BIGINT NOT NULL COMMENT '执行耗时(毫秒)',
  `exception_class` VARCHAR(1000) COMMENT '异常类名',
  `exception_message` BLOB COMMENT '异常消息',
  `exception_stacktrace` BLOB COMMENT '异常堆栈信息',
  INDEX `stl_started_idx` (`time_started`),
  INDEX `stl_task_name_idx` (`task_name`),
  INDEX `stl_exception_class_idx` (`exception_class`(200))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='db-scheduler 任务执行历史日志表';

-- =============================================
-- 初始化示例数据 (由db-scheduler自动管理)
-- =============================================

-- 注意：实际的定时任务实例会在应用启动时由db-scheduler自动创建和管理
-- 这里不需要手动插入数据，因为recurring tasks会自动管理自己的执行计划

-- =============================================
-- 验证脚本执行结果
-- =============================================

-- 验证表是否创建成功
SELECT 
    TABLE_NAME as '表名',
    TABLE_COMMENT as '表注释',
    TABLE_ROWS as '数据行数'
FROM 
    information_schema.TABLES 
WHERE 
    TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME IN ('scheduled_tasks', 'scheduled_execution_logs');

-- 验证索引是否创建成功
SELECT 
    TABLE_NAME as '表名',
    INDEX_NAME as '索引名',
    COLUMN_NAME as '索引字段',
    NON_UNIQUE as '是否唯一'
FROM 
    information_schema.STATISTICS 
WHERE 
    TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME IN ('scheduled_tasks', 'scheduled_execution_logs')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
