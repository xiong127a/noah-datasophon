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

package com.datasophon.api.client;

import com.datasophon.common.command.BaseCommand;
import com.datasophon.common.command.SystemInfoResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Worker HTTP客户端
 * 用于Master向Worker发送HTTP请求
 * 
 * 单向通讯设计：
 * - Master主动发起所有HTTP请求
 * - Worker只被动响应
 */
@Component
public class WorkerHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(WorkerHttpClient.class);

    private static final int WORKER_PORT = 2552;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(180);

    private final WebClient.Builder webClientBuilder;

    public WorkerHttpClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * 提交任务到Worker
     * @param workerHost Worker主机名或IP
     * @param command 命令对象
     * @return 任务ID
     */
    public String submitTask(String workerHost, BaseCommand command) {
        String url = String.format("http://%s:%d/api/tasks", workerHost, WORKER_PORT);
        
        logger.info("Submitting task to worker: {}, command: {}", workerHost, command.getClass().getSimpleName());
        
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(command)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            logger.error("Failed to submit task to worker: {}, status: {}, error: {}", 
                                    workerHost, clientResponse.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("Failed to submit task: " + clientResponse.statusCode()));
                        })
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(DEFAULT_TIMEOUT)
                    .block();
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                String taskId = (String) response.get("taskId");
                logger.info("Task submitted successfully to worker: {}, taskId: {}", workerHost, taskId);
                return taskId;
            } else {
                throw new RuntimeException("Task submission failed: " + response);
            }
            
        } catch (Exception e) {
            logger.error("Failed to submit task to worker: {}", workerHost, e);
            throw new RuntimeException("Failed to submit task to worker: " + workerHost, e);
        }
    }

    /**
     * 查询任务状态
     * @param workerHost Worker主机名或IP
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    public Map<String, Object> getTaskStatus(String workerHost, String taskId) {
        String url = String.format("http://%s:%d/api/tasks/%s", workerHost, WORKER_PORT, taskId);
        
        logger.debug("Getting task status from worker: {}, taskId: {}", workerHost, taskId);
        
        try {
            Map<String, Object> result = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            return result;
        } catch (Exception e) {
            logger.error("Failed to get task status from worker: {}, taskId: {}", workerHost, taskId, e);
            throw new RuntimeException("Failed to get task status", e);
        }
    }

    /**
     * 取消任务
     * @param workerHost Worker主机名或IP
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    public boolean cancelTask(String workerHost, String taskId) {
        String url = String.format("http://%s:%d/api/tasks/%s", workerHost, WORKER_PORT, taskId);
        
        logger.info("Cancelling task on worker: {}, taskId: {}", workerHost, taskId);
        
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .delete()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            logger.error("Failed to cancel task on worker: {}, taskId: {}", workerHost, taskId, e);
            return false;
        }
    }

    /**
     * Ping Worker（健康检查）
     * 替代原来的Pekko Ping
     */
    public boolean ping(String workerHost) {
        String url = String.format("http://%s:%d/api/ping", workerHost, WORKER_PORT);
        
        logger.debug("Pinging worker: {}", workerHost);
        
        try {
            String response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            boolean success = "pong".equals(response);
            if (success) {
                logger.debug("Worker ping successful: {}", workerHost);
            } else {
                logger.warn("Worker ping failed: {}, response: {}", workerHost, response);
            }
            return success;
            
        } catch (Exception e) {
            logger.error("Failed to ping worker: {}", workerHost, e);
            return false;
        }
    }

    /**
     * 获取Worker系统信息
     * 替代原来的SystemInfoActor
     */
    public SystemInfoResult getSystemInfo(String workerHost) {
        String url = String.format("http://%s:%d/api/info", workerHost, WORKER_PORT);
        
        logger.info("Getting system info from worker: {}", workerHost);
        
        try {
            SystemInfoResult result = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(SystemInfoResult.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            if (result != null && result.getExecResult()) {
                logger.info("System info retrieved successfully from worker: {}", workerHost);
            } else {
                logger.warn("Failed to retrieve system info from worker: {}", workerHost);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to get system info from worker: {}", workerHost, e);
            SystemInfoResult errorResult = new SystemInfoResult();
            errorResult.setExecResult(false);
            errorResult.setExecOut("Failed to get system info: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 检查Worker健康状态
     * 替代原来的Pekko健康检查
     */
    public Map<String, Object> checkHealth(String workerHost) {
        String url = String.format("http://%s:%d/api/health", workerHost, WORKER_PORT);
        
        logger.debug("Checking health of worker: {}", workerHost);
        
        try {
            Map<String, Object> result = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();
            return result;
        } catch (Exception e) {
            logger.error("Failed to check health of worker: {}", workerHost, e);
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }
}

