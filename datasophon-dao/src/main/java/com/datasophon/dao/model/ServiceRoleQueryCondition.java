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

package com.datasophon.dao.model;

/**
 * 服务角色查询条件
 * 用于批量查询服务角色，位于dao层符合架构分层原则
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com  
 * 日期：2025-01-25
 */
public record ServiceRoleQueryCondition(
    Long serviceId,
    String roleName
) {
    
    /**
     * 创建查询条件
     */
    public static ServiceRoleQueryCondition of(Long serviceId, String roleName) {
        return new ServiceRoleQueryCondition(serviceId, roleName);
    }
    
    /**
     * 生成唯一键
     */
    public String getUniqueKey() {
        return serviceId + "_" + roleName;
    }
}
