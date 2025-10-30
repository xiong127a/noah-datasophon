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

package com.datasophon.worker.http;

import com.datasophon.common.command.BaseCommand;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.Future;

/**
 * 任务信息
 * 记录任务的执行状态
 */
@Data
public class TaskInfo {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 命令对象
     */
    private BaseCommand command;
    
    /**
     * 任务状态：PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
     */
    private String status;
    
    /**
     * 任务结果
     */
    private Object result;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
    
    /**
     * 异步执行的Future
     */
    private Future<?> future;
    
    public TaskInfo(String taskId, BaseCommand command) {
        this.taskId = taskId;
        this.command = command;
        this.status = "PENDING";
        this.createTime = LocalDateTime.now();
    }
}

