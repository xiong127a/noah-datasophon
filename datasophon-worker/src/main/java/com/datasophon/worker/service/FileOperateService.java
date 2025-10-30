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

import com.datasophon.common.command.FileOperateCommand;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 文件操作服务
 * 替代原来的FileOperateActor
 */
@Service
public class FileOperateService {

    private static final Logger logger = LoggerFactory.getLogger(FileOperateService.class);

    /**
     * 执行文件操作
     */
    public ExecResult operateFile(FileOperateCommand command) {
        logger.info("File operation: {}", command.getOperateType());
        
        ExecResult result = new ExecResult();
        
        try {
            switch (command.getOperateType()) {
                case "COPY":
                    copyFile(command.getSourcePath(), command.getTargetPath());
                    break;
                case "MOVE":
                    moveFile(command.getSourcePath(), command.getTargetPath());
                    break;
                case "DELETE":
                    deleteFile(command.getTargetPath());
                    break;
                case "CREATE_DIR":
                    createDirectory(command.getTargetPath());
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported operation: " + command.getOperateType());
            }
            
            result.setExecResult(true);
            result.setExecOut("File operation completed successfully");
            logger.info("File operation completed: {}", command.getOperateType());
            
        } catch (Exception e) {
            logger.error("File operation failed: {}", command.getOperateType(), e);
            result.setExecResult(false);
            result.setExecOut("File operation failed: " + e.getMessage());
        }
        
        return result;
    }

    private void copyFile(String source, String target) throws Exception {
        Path sourcePath = Paths.get(source);
        Path targetPath = Paths.get(target);
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void moveFile(String source, String target) throws Exception {
        Path sourcePath = Paths.get(source);
        Path targetPath = Paths.get(target);
        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteFile(String path) throws Exception {
        File file = new File(path);
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            Files.deleteIfExists(Paths.get(path));
        }
    }

    private void deleteDirectory(File directory) throws Exception {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    private void createDirectory(String path) throws Exception {
        Files.createDirectories(Paths.get(path));
    }
}

