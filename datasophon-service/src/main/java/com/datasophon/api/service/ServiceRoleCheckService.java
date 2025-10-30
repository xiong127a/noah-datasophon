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

/**
 * 服务角色检查服务
 * 替代ServiceRoleCheckActor，使用db-scheduler定时调度
 */
public interface ServiceRoleCheckService {
    
    /**
     * 执行服务角色检查
     * 由db-scheduler定时调用
     */
    void performServiceRoleCheck();
    
    /**
     * 检查指定集群的服务角色
     */
    void checkClusterServiceRoles(Long clusterId);
}

