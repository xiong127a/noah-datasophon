/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

-- 修复cluster_id字段类型问题
-- 将确实需要修改的表中的cluster_id字段从INT类型改为BIGINT以支持雪花算法生成的20位Long类型集群ID
-- 注意：基于实际数据库检查，大部分表的cluster_id字段已经是BIGINT类型，只修改确实需要的表

-- 修改集群服务命令表的cluster_id字段类型（当前是int，需要改为bigint）
ALTER TABLE `t_ddh_cluster_service_command` 
MODIFY COLUMN `cluster_id` BIGINT DEFAULT NULL COMMENT '集群ID（支持雪花算法生成的Long类型ID）';

-- 注意：t_ddh_cluster_service_command表的service_instance_id字段已经是bigint类型，无需修改

-- 检查其他可能需要修改的表（如果存在且cluster_id是int类型）
-- 以下语句只会在表存在且字段类型不正确时执行

-- 如果t_ddh_cluster_service_dashboard表存在且cluster_id是int类型
-- ALTER TABLE `t_ddh_cluster_service_dashboard` 
-- MODIFY COLUMN `cluster_id` BIGINT DEFAULT NULL COMMENT '集群ID（支持雪花算法生成的Long类型ID）';

-- 如果t_ddh_cluster_variable表存在且cluster_id是int类型  
-- ALTER TABLE `t_ddh_cluster_variable` 
-- MODIFY COLUMN `cluster_id` BIGINT DEFAULT NULL COMMENT '集群ID（支持雪花算法生成的Long类型ID）';
