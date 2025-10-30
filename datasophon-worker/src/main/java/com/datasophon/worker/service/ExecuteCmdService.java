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

import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 命令执行服务
 * 替代原来的ExecuteCmdActor
 */
@Service
public class ExecuteCmdService {

    private static final Logger logger = LoggerFactory.getLogger(ExecuteCmdService.class);

    /**
     * 执行命令
     */
    public ExecResult executeCmd(ExecuteCmdCommand command) {
        logger.info("Executing command: {}", command.getCommandType());
        
        try {
            ExecResult result = ShellUtils.execWithStatus(
                    command.getCommandId(),
                    command.getCommands(),
                    command.getTimeout(),
                    logger);
            
            logger.info("Command execution completed: {}, success: {}",
                    command.getCommandType(), result.getExecResult());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to execute command: {}", command.getCommandType(), e);
            ExecResult errorResult = new ExecResult();
            errorResult.setExecResult(false);
            errorResult.setExecOut("Command execution failed: " + e.getMessage());
            return errorResult;
        }
    }
}

