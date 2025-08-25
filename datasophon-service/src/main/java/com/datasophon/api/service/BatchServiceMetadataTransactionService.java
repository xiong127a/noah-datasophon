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

package com.datasophon.api.service;

import com.datasophon.api.load.model.BatchServiceData;
import com.datasophon.api.load.model.ServiceMetaConfig;
import com.datasophon.dao.entity.FrameInfoEntity;

import java.util.List;

/**
 * 批量服务元数据事务处理服务
 * 用于优化数据库操作，将136个服务的1088次SQL操作优化为6-8次批量操作
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com  
 * 日期：2025-01-25
 */
public interface BatchServiceMetadataTransactionService {
    
    /**
     * 批量处理框架内所有服务的元数据
     * 从1000+次SQL减少到6次批量SQL
     * 
     * @param frameCode 框架代码
     * @param frameInfo 框架信息
     * @param configs 服务配置列表
     * @return 批量处理结果统计
     */
    BatchProcessResult batchProcessFrameServices(String frameCode, FrameInfoEntity frameInfo, 
                                               List<ServiceMetaConfig> configs);
    
    /**
     * 批量处理结果统计
     */
    record BatchProcessResult(
        int totalServices,
        int servicesInserted,
        int servicesUpdated,
        int totalRoles,
        int rolesInserted,
        int rolesUpdated,
        long processingTimeMs
    ) {
        
        /**
         * 创建处理结果
         */
        public static BatchProcessResult of(BatchServiceData batchData, long processingTimeMs) {
            return new BatchProcessResult(
                batchData.getTotalServiceOperations(),
                batchData.servicesToInsert().size(),
                batchData.servicesToUpdate().size(),
                batchData.getTotalRoleOperations(),
                batchData.rolesToInsert().size(),
                batchData.rolesToUpdate().size(),
                processingTimeMs
            );
        }
        
        /**
         * 获取操作汇总信息
         */
        public String getSummary() {
            return String.format(
                "服务操作: %d (新增: %d, 更新: %d), 角色操作: %d (新增: %d, 更新: %d), 耗时: %dms",
                totalServices, servicesInserted, servicesUpdated,
                totalRoles, rolesInserted, rolesUpdated, processingTimeMs
            );
        }
    }
}
