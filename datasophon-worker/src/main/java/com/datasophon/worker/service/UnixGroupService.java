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

import com.datasophon.common.command.remote.CreateUnixGroupCommand;
import com.datasophon.common.command.remote.DelUnixGroupCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.utils.UnixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Unix组服务
 * 替代原来的UnixGroupActor
 */
@Service
public class UnixGroupService {

    private static final Logger logger = LoggerFactory.getLogger(UnixGroupService.class);

    /**
     * 创建Unix组
     */
    public ExecResult createGroup(CreateUnixGroupCommand command) {
        logger.info("Creating Unix group: {}", command.getGroupName());
        
        ExecResult result = new ExecResult();
        
        try {
            UnixUtils.createUnixGroup(command.getGroupName());
            
            result.setExecResult(true);
            result.setExecOut("Group created successfully");
            logger.info("Group created successfully: {}", command.getGroupName());
            
        } catch (Exception e) {
            logger.error("Failed to create group: {}", command.getGroupName(), e);
            result.setExecResult(false);
            result.setExecOut("Failed to create group: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 删除Unix组
     */
    public ExecResult deleteGroup(DelUnixGroupCommand command) {
        logger.info("Deleting Unix group: {}", command.getGroupName());
        
        ExecResult result = new ExecResult();
        
        try {
            UnixUtils.delUnixGroup(command.getGroupName());
            
            result.setExecResult(true);
            result.setExecOut("Group deleted successfully");
            logger.info("Group deleted successfully: {}", command.getGroupName());
            
        } catch (Exception e) {
            logger.error("Failed to delete group: {}", command.getGroupName(), e);
            result.setExecResult(false);
            result.setExecOut("Failed to delete group: " + e.getMessage());
        }
        
        return result;
    }
}

