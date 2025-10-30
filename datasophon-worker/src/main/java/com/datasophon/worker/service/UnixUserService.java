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

import com.datasophon.common.command.remote.CreateUnixUserCommand;
import com.datasophon.common.command.remote.DelUnixUserCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.utils.UnixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Unix用户服务
 * 替代原来的UnixUserActor
 */
@Service
public class UnixUserService {

    private static final Logger logger = LoggerFactory.getLogger(UnixUserService.class);

    /**
     * 创建Unix用户
     */
    public ExecResult createUser(CreateUnixUserCommand command) {
        logger.info("Creating Unix user: {}", command.getUsername());
        
        ExecResult result = new ExecResult();
        
        try {
            UnixUtils.createUnixUser(
                    command.getUsername(),
                    command.getMainGroup(),
                    command.getOtherGroups());
            
            result.setExecResult(true);
            result.setExecOut("User created successfully");
            logger.info("User created successfully: {}", command.getUsername());
            
        } catch (Exception e) {
            logger.error("Failed to create user: {}", command.getUsername(), e);
            result.setExecResult(false);
            result.setExecOut("Failed to create user: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 删除Unix用户
     */
    public ExecResult deleteUser(DelUnixUserCommand command) {
        logger.info("Deleting Unix user: {}", command.getUsername());
        
        ExecResult result = new ExecResult();
        
        try {
            UnixUtils.deleteUnixUser(command.getUsername());
            
            result.setExecResult(true);
            result.setExecOut("User deleted successfully");
            logger.info("User deleted successfully: {}", command.getUsername());
            
        } catch (Exception e) {
            logger.error("Failed to delete user: {}", command.getUsername(), e);
            result.setExecResult(false);
            result.setExecOut("Failed to delete user: " + e.getMessage());
        }
        
        return result;
    }
}

