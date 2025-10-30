/*
 *
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
 *
 */

package com.datasophon.worker.service;

import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.handler.ServiceHandler;
import com.datasophon.worker.strategy.ServiceRoleStrategy;
import com.datasophon.worker.strategy.ServiceRoleStrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 服务操作服务
 * 统一处理Start、Stop、Restart等操作
 * 替代StartServiceActor、StopServiceActor、RestartServiceActor
 */
@Service
public class ServiceOperateService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceOperateService.class);

    /**
     * 启动服务
     */
    public ExecResult start(ServiceRoleOperateCommand command) {
        logger.info("start to start service role {}", command.getServiceRoleName());
        
        ServiceHandler serviceHandler = new ServiceHandler(
                command.getServiceName(),
                command.getServiceRoleName());

        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext
                .getServiceRoleHandler(command.getServiceRoleName());
                
        ExecResult startResult;
        if (Objects.nonNull(serviceRoleHandler)) {
            startResult = serviceRoleHandler.handler(command);
        } else {
            startResult = serviceHandler.start(
                    command.getStartRunner(),
                    command.getStatusRunner(),
                    command.getDecompressPackageName(),
                    command.getRunAs());
        }

        logger.info("service role {} start result {}", command.getServiceRoleName(),
                startResult.getExecResult() ? "success" : "failed");
        return startResult;
    }

    /**
     * 停止服务
     */
    public ExecResult stop(ServiceRoleOperateCommand command) {
        logger.info("start to stop service role {}", command.getServiceRoleName());
        
        ServiceHandler serviceHandler = new ServiceHandler(
                command.getServiceName(),
                command.getServiceRoleName());

        ExecResult stopResult = serviceHandler.stop(
                command.getStopRunner(),
                command.getStatusRunner(),
                command.getDecompressPackageName(),
                command.getRunAs());

        logger.info("service role {} stop result {}", command.getServiceRoleName(),
                stopResult.getExecResult() ? "success" : "failed");
        return stopResult;
    }

    /**
     * 重启服务
     */
    public ExecResult restart(ServiceRoleOperateCommand command) {
        logger.info("start to restart service role {}", command.getServiceRoleName());
        
        ServiceHandler serviceHandler = new ServiceHandler(
                command.getServiceName(),
                command.getServiceRoleName());

        ExecResult restartResult = serviceHandler.restart(
                command.getRestartRunner(),
                command.getStatusRunner(),
                command.getDecompressPackageName(),
                command.getRunAs());

        logger.info("service role {} restart result {}", command.getServiceRoleName(),
                restartResult.getExecResult() ? "success" : "failed");
        return restartResult;
    }
}

