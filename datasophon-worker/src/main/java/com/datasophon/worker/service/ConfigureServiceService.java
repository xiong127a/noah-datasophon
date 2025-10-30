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

import com.datasophon.common.command.GenerateServiceConfigCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.handler.ConfigureServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 服务配置服务
 * 替代原来的ConfigureServiceActor
 */
@Service
public class ConfigureServiceService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigureServiceService.class);

    /**
     * 配置服务
     */
    public ExecResult configure(GenerateServiceConfigCommand command) {
        logger.info("start configure {}", command.getServiceName());
        
        ConfigureServiceHandler serviceHandler = new ConfigureServiceHandler(
                command.getServiceName(),
                command.getServiceRoleName());
                
        // 设置集群ID到handler，更新logger路径
        serviceHandler.setClusterId(command.getClusterId());
        
        ExecResult configureResult = serviceHandler.configure(
                command.getCofigFileMap(),
                command.getDecompressPackageName(),
                command.getMyid(),
                command.getServiceRoleName(),
                command.getRunAs(),
                command.getTemplateContents());

        logger.info("{} configure result {}", command.getServiceName(),
                configureResult.getExecResult() ? "success" : "failed");
        return configureResult;
    }
}

