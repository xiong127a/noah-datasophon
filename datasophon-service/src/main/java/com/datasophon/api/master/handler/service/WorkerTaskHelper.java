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

package com.datasophon.api.master.handler.service;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.client.WorkerHttpClient;
import com.datasophon.common.command.BaseCommand;
import com.datasophon.common.utils.ExecResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Worker任务助手
 * 提供通用的HTTP任务提交和等待方法
 */
public class WorkerTaskHelper {

    private static final Logger logger = LoggerFactory.getLogger(WorkerTaskHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 提交任务并等待完成
     * @param hostname Worker主机名
     * @param command 命令对象
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public static ExecResult submitAndWait(String hostname, BaseCommand command, int timeoutSeconds) {
        WorkerHttpClient workerHttpClient = SpringUtil.getBean(WorkerHttpClient.class);
        
        try {
            logger.info("Submitting task to Worker: {}, command: {}", 
                    hostname, command.getClass().getSimpleName());
            
            String taskId = workerHttpClient.submitTask(hostname, command);
            
            return waitForTaskCompletion(workerHttpClient, hostname, taskId, timeoutSeconds);
            
        } catch (Exception e) {
            logger.error("Failed to execute task on Worker: {}", hostname, e);
            ExecResult errorResult = new ExecResult();
            errorResult.setExecResult(false);
            errorResult.setExecOut("Task execution failed: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 等待任务完成
     */
    private static ExecResult waitForTaskCompletion(WorkerHttpClient client, String hostname, 
                                                   String taskId, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                Map<String, Object> status = client.getTaskStatus(hostname, taskId);
                
                if (status != null && Boolean.TRUE.equals(status.get("success"))) {
                    String taskStatus = (String) status.get("status");
                    
                    if ("COMPLETED".equals(taskStatus)) {
                        Object resultData = status.get("result");
                        
                        // 尝试将result转换为ExecResult
                        if (resultData instanceof Map) {
                            try {
                                String json = objectMapper.writeValueAsString(resultData);
                                return objectMapper.readValue(json, ExecResult.class);
                            } catch (Exception e) {
                                logger.warn("Failed to convert result to ExecResult", e);
                            }
                        } else if (resultData instanceof ExecResult) {
                            return (ExecResult) resultData;
                        }
                        
                        // 默认成功结果
                        ExecResult result = new ExecResult();
                        result.setExecResult(true);
                        result.setExecOut("Task completed");
                        return result;
                        
                    } else if ("FAILED".equals(taskStatus)) {
                        ExecResult result = new ExecResult();
                        result.setExecResult(false);
                        result.setExecOut((String) status.get("errorMessage"));
                        return result;
                    }
                }
                
                Thread.sleep(2000); // 每2秒查询一次
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("Failed to get task status: {}", e.getMessage());
            }
        }
        
        // 超时
        ExecResult result = new ExecResult();
        result.setExecResult(false);
        result.setExecOut("Task timeout after " + timeoutSeconds + " seconds");
        return result;
    }
}

