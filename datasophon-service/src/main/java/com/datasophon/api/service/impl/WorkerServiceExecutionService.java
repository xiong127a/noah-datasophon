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

package com.datasophon.api.service.impl;

import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;

import java.util.List;
import java.util.Map;

/**
 * Worker服务执行服务
 * 替代WorkerServiceActor，使用Spring Service实现
 */
public interface WorkerServiceExecutionService {
    
    /**
     * 执行Worker服务角色
     */
    void executeWorkerServiceRole(
            Long clusterId,
            Integer commandType,
            String clusterCode,
            DAGGraph<String, ServiceNode, String> dag,
            Map<String, ServiceExecuteState> activeTaskList,
            Map<String, String> errorTaskList,
            Map<String, String> readyToSubmitTaskList,
            Map<String, String> completeTaskList,
            String node,
            List<ServiceRoleInfo> elseRoles,
            ServiceRoleInfo elseRole,
            ServiceRoleType serviceRoleType);
}

