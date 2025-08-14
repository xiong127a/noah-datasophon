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

-- 修复ref_id字段类型问题
-- 将INT类型改为BIGINT以支持雪花算法生成的Long类型ID

-- 修改t_ddh_config_version_info表的ref_id字段类型
ALTER TABLE `t_ddh_config_version_info` MODIFY COLUMN `ref_id` BIGINT NOT NULL COMMENT '关联对象ID（支持雪花算法生成的Long类型ID）';

-- 同时修改user_id字段类型（确保与雪花算法生成的用户ID兼容）
ALTER TABLE `t_ddh_config_version_info` MODIFY COLUMN `user_id` BIGINT NULL DEFAULT NULL COMMENT '用户ID（支持雪花算法生成的Long类型ID）';

-- 删除并重建唯一索引以适应新的字段类型
ALTER TABLE `t_ddh_config_version_info` DROP INDEX `uk_version_ref`;
ALTER TABLE `t_ddh_config_version_info` ADD UNIQUE KEY `uk_version_ref` (`version`, `ref_type`, `ref_id`);
