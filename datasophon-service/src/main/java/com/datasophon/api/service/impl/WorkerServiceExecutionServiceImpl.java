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

import com.datasophon.api.service.CommandExecutionService;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Worker服务执行服务实现
 * 替代WorkerServiceActor，处理Worker节点的服务执行
 */
@Service
public class WorkerServiceExecutionServiceImpl implements WorkerServiceExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerServiceExecutionServiceImpl.class);

    @Autowired
    private CommandExecutionService commandExecutionService;

    @Override
    @Async("taskExecutor")
    public void executeWorkerServiceRole(
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
            ServiceRoleType serviceRoleType) {
        
        logger.info("执行Worker服务角色: {}, 主机: {}", node, elseRole.getHostname());
        
        // 使用CommandExecutionService执行命令
        // 注意：buildExecuteServiceRoleCommand已被标记为deprecated
        // 但这里仍需调用以保持功能完整性
        commandExecutionService.buildExecuteServiceRoleCommand(
                clusterId,
                commandType,
                clusterCode,
                dag,
                activeTaskList,
                errorTaskList,
                readyToSubmitTaskList,
                completeTaskList,
                node,
                elseRoles,
                elseRole,
                null, // ActorRef已废弃
                serviceRoleType);
    }
}

