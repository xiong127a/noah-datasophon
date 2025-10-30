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

import com.datasophon.common.command.GetLogCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志服务
 * 替代原来的LogActor
 */
@Service
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    /**
     * 获取日志
     */
    public String getLog(GetLogCommand command) {
        logger.info("Getting log: {}", command.getLogFile());
        
        try {
            return readLogFile(command.getLogFile(), command.getLines());
        } catch (Exception e) {
            logger.error("Failed to read log file: {}", command.getLogFile(), e);
            return "Failed to read log: " + e.getMessage();
        }
    }

    private String readLogFile(String logFile, int lines) throws Exception {
        List<String> logLines = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logLines.add(line);
                if (lines > 0 && logLines.size() > lines) {
                    logLines.removeFirst();
                }
            }
        }
        
        return String.join("\n", logLines);
    }
}

