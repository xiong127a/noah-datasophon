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

package com.datasophon.api.load.model;

import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;

import java.util.List;
import java.util.Map;

/**
 * 批量服务数据处理对象
 * 用于批量优化数据库操作，减少SQL执行次数
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com  
 * 日期：2025-01-25
 */
public record BatchServiceData(
    List<FrameServiceEntity> servicesToInsert,
    List<FrameServiceEntity> servicesToUpdate,
    List<FrameServiceRoleEntity> rolesToInsert, 
    List<FrameServiceRoleEntity> rolesToUpdate,
    Map<String, FrameServiceEntity> existingServices,
    Map<String, FrameServiceRoleEntity> existingRoles,
    Map<String, Long> serviceNameToIdMap
) {
    
    /**
     * 创建空的批量数据对象
     */
    public static BatchServiceData empty() {
        return new BatchServiceData(
            new java.util.ArrayList<>(), 
            new java.util.ArrayList<>(),
            new java.util.ArrayList<>(), 
            new java.util.ArrayList<>(),
            new java.util.HashMap<>(), 
            new java.util.HashMap<>(),
            new java.util.HashMap<>()
        );
    }
    
    /**
     * 获取总的服务操作数量
     */
    public int getTotalServiceOperations() {
        return servicesToInsert.size() + servicesToUpdate.size();
    }
    
    /**
     * 获取总的角色操作数量
     */
    public int getTotalRoleOperations() {
        return rolesToInsert.size() + rolesToUpdate.size();
    }
    
    /**
     * 检查是否有需要执行的操作
     */
    public boolean hasOperations() {
        return getTotalServiceOperations() > 0 || getTotalRoleOperations() > 0;
    }
}
