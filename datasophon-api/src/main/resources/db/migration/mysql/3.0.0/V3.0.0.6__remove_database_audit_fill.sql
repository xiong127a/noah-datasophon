/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- 移除数据库级别的审计字段自动填充，改为程序控制
-- 作者：任相鹏
-- 邮箱：635887935@qq.com
-- 日期：2025-01-15

-- 修改集群服务命令表，移除数据库级别的自动填充
ALTER TABLE `t_ddh_cluster_service_command` 
MODIFY COLUMN `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `update_time` DATETIME DEFAULT NULL COMMENT '更新时间';

-- 修改其他相关表，移除数据库级别的自动填充（如果存在）
-- t_ddh_cluster_service_command_host表
ALTER TABLE `t_ddh_cluster_service_command_host` 
MODIFY COLUMN `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `update_time` DATETIME DEFAULT NULL COMMENT '更新时间';

-- t_ddh_cluster_service_command_host_command表  
ALTER TABLE `t_ddh_cluster_service_command_host_command` 
MODIFY COLUMN `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `update_time` DATETIME DEFAULT NULL COMMENT '更新时间';

-- 注释：现在所有审计字段（create_time、update_time、create_by、update_by）
-- 都将由MyBatis-Flex的审计监听器自动填充，确保程序完全控制审计信息
