-- 集群配置进度管理表
-- 用于记录集群配置过程中的状态和数据，支持断点续传
-- Version: 3.0.1
-- Author: DataSophon Team

CREATE TABLE IF NOT EXISTS `t_ddh_cluster_config_progress` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `cluster_id` INT NOT NULL COMMENT '集群ID，关联t_ddh_cluster_info表',
    `config_status` VARCHAR(20) NOT NULL DEFAULT 'UNCONFIGURED' COMMENT '配置状态：UNCONFIGURED-未配置，CONFIGURING-配置中，COMPLETED-配置完成',
    `completed_step` TINYINT NOT NULL DEFAULT 0 COMMENT '已完成步骤：0-未开始，1-安装主机，2-环境校验，3-Agent分发，4-选择服务，5-分配Master，6-分配Worker，7-服务配置，8-全部完成',
    
    -- 各步骤配置数据（JSON格式存储）
    `step1_data` TEXT COMMENT 'Step1数据：安装主机 - 主机列表、SSH配置等',
    `step2_data` TEXT COMMENT 'Step2数据：主机环境校验 - 校验结果、环境信息等',
    `step3_data` TEXT COMMENT 'Step3数据：主机Agent分发 - Agent状态、分发进度等',
    `step4_data` TEXT COMMENT 'Step4数据：选择服务 - 服务列表、框架信息等',
    `step5_data` TEXT COMMENT 'Step5数据：分配服务Master角色 - Master节点分配等',
    `step6_data` TEXT COMMENT 'Step6数据：分配服务Worker与Client角色 - Worker节点分配等',
    `step7_data` TEXT COMMENT 'Step7数据：服务配置 - 服务参数配置等',
    `step8_data` TEXT COMMENT 'Step8数据：安装并启动服务 - 安装进度、服务状态等',
    
    -- 配置过程元数据
    `started_time` DATETIME NULL COMMENT '配置开始时间',
    `completed_time` DATETIME NULL COMMENT '配置完成时间',
    `last_step_time` DATETIME NULL COMMENT '最后步骤操作时间',
    
    -- 审计字段
    `created_by` VARCHAR(50) COMMENT '创建人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(50) COMMENT '最后更新人', 
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引和约束
    UNIQUE KEY `uk_cluster_id` (`cluster_id`) COMMENT '集群ID唯一约束',
    INDEX `idx_config_status` (`config_status`) COMMENT '配置状态索引',
    INDEX `idx_completed_step` (`completed_step`) COMMENT '完成步骤索引',
    INDEX `idx_created_time` (`created_time`) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='集群配置进度表' ROW_FORMAT=DYNAMIC;

-- 添加外键约束（确保t_ddh_cluster_info表存在）
ALTER TABLE `t_ddh_cluster_config_progress` 
ADD CONSTRAINT `fk_cluster_config_progress_cluster_id` 
FOREIGN KEY (`cluster_id`) REFERENCES `t_ddh_cluster_info`(`id`) 
ON DELETE CASCADE ON UPDATE CASCADE;