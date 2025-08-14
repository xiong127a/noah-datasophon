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

-- DM数据库修复ref_id字段类型问题
-- 将INT类型改为BIGINT以支持雪花算法生成的Long类型ID

-- 修改t_ddh_config_version_info表的ref_id字段类型
ALTER TABLE "t_ddh_config_version_info" ALTER COLUMN "ref_id" BIGINT NOT NULL;

-- 同时修改user_id字段类型（确保与雪花算法生成的用户ID兼容）
ALTER TABLE "t_ddh_config_version_info" ALTER COLUMN "user_id" BIGINT;

-- 删除并重建唯一约束以适应新的字段类型
ALTER TABLE "t_ddh_config_version_info" DROP CONSTRAINT "UK_version_ref";
ALTER TABLE "t_ddh_config_version_info" ADD CONSTRAINT "UK_version_ref" UNIQUE ("version", "ref_type", "ref_id");
