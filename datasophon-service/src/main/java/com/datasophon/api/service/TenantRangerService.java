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

import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.model.tenant.resource.TenantResource;
import com.datasophon.common.utils.ExecResult;

/**
 * 租户Ranger服务
 * 用于操作Apache Ranger权限管理系统
 */
public interface TenantRangerService {
    
    /**
     * 处理租户Ranger命令
     */
    ExecResult handleTenantRangerCommand(TenantRangerCommand rangerCommand);
    
    /**
     * 处理租户资源
     */
    ExecResult handleTenantResource(TenantResource resource);
}

